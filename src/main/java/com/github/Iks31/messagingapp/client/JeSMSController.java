package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import com.github.Iks31.messagingapp.client.scenes.StartMenu;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import com.github.Iks31.messagingapp.common.NetworkMessage;
import com.github.Iks31.messagingapp.common.Protocol;
import com.github.Iks31.messagingapp.common.Protocol.ChatRequest;
import com.github.Iks31.messagingapp.common.Protocol.MemberRequest;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class JeSMSController {
    private final JeSMSView view;
    private final ObservableList<Conversation> conversationsList = FXCollections.observableArrayList();
    private final FilteredList<Conversation> filteredConversations = new FilteredList<>(conversationsList);
    // Id of the conversation currently open, null if none
    private String currentConversationId;
    // True while the controller changes the list selection itself
    private boolean updatingSelection;
    // Ensures every message sent from this client has a unique timestamp
    private long lastSentTimestamp;
    // Open settings dialog, if any, which receives password change results
    private SettingsDialog settingsDialog;

    public JeSMSController(JeSMSView view) {
        this.view = view;
        attachEvents();
    }

    private String me() {
        return ClientApp.getClientNetworking().getUsername();
    }

    private void attachEvents() {
        ClientApp.getClientNetworking().setMessageHandler(this::handleMessage);
        ClientApp.getClientNetworking().conversationsRequest();

        view.getConversationsList().setItems(filteredConversations);
        // Opening is deferred because the selection can change while the list is still processing a change
        view.getConversationsList().getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (!updatingSelection && selected != null && !selected.id.equals(currentConversationId)) {
                String id = selected.id;
                Platform.runLater(() -> openConversation(id));
            }
        });
        view.getCreateConversationButton().setOnAction(e -> createConversation());
        view.getSendMessageButton().setOnAction(e -> sendMsg());
        view.getFilterToggleButton().setOnAction(e -> toggleFilter());
        view.getLogoutButton().setOnAction(e -> logout());
        view.getSettingsButton().setOnAction(e -> openSettings());
        view.getAddMemberButton().setOnAction(e -> addMember());
        view.getLeaveConversationButton().setOnAction(e -> leaveConversation());
        view.setOnEditMessage(this::editMessage);
        view.setOnDeleteMessage(this::deleteMessage);

        // Enter sends the message, Shift+Enter inserts a new line
        view.getMessageTextArea().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendMsg();
            }
        });

        // Messages that arrived while the window was in the background are read once it is focused again
        view.getMessageTextArea().sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getWindow() != null) {
                scene.getWindow().focusedProperty().addListener((o, was, focused) -> {
                    if (focused) {
                        markCurrentAsRead();
                        refresh();
                    }
                });
            }
        });
        setupConversationFiltering();
    }

    // ---- Server messages ----

    @SuppressWarnings("unchecked")
    private void handleMessage(NetworkMessage<?> msg) {
        Object content = msg.getContent();
        switch (msg.getFlag()) {
            case Protocol.CONVERSATIONS_RECEIVED -> setConversations((ArrayList<Conversation>) content);
            case Protocol.CONVERSATIONS_NOT_RECEIVED -> showWarning("Conversation Error", "Failed to Load Conversations", (String) content);
            case Protocol.CREATE_CONVERSATION_SUCCESS -> {
                Conversation conversation = (Conversation) content;
                upsertConversation(conversation);
                openConversation(conversation.id);
            }
            case Protocol.CREATE_CONVERSATION_FAIL -> showWarning("Conversation Error", "Failed to Create Conversation", (String) content);
            case Protocol.REALTIME_CONVERSATION -> {
                upsertConversation((Conversation) content);
                refresh();
            }
            case Protocol.REMOVED_FROM_CONVERSATION -> removeConversation((String) content);
            case Protocol.REALTIME_CHAT -> realTimeMessage((ChatRequest) content);
            case Protocol.REALTIME_EDIT, Protocol.REALTIME_DELETE -> realTimeMessageUpdate((ChatRequest) content);
            case Protocol.REALTIME_READ -> realTimeRead((MemberRequest) content);
            case Protocol.CHANGE_PASSWORD_SUCCESS, Protocol.CHANGE_PASSWORD_FAIL -> {
                if (settingsDialog != null) {
                    settingsDialog.showResult(Protocol.CHANGE_PASSWORD_SUCCESS.equals(msg.getFlag()), (String) content);
                }
            }
            case Protocol.REQUEST_FAIL -> {
                showWarning("Request Failed", "The server could not complete your request", (String) content);
                // Discards any optimistic local changes that the server rejected
                ClientApp.getClientNetworking().conversationsRequest();
            }
            case Protocol.LOGOUT_SUCCESS -> {
                ClientApp.getClientNetworking().setUsername(null);
                view.stage.setScene(new StartMenu().getScene(view.stage));
            }
            default -> {}
        }
    }

    private void setConversations(ArrayList<Conversation> conversations) {
        mutateConversations(() -> conversationsList.setAll(conversations));
        if (findConversation(currentConversationId) == null) {
            currentConversationId = null;
        }
        markCurrentAsRead();
        refresh();
    }

    public void realTimeMessage(ChatRequest request) {
        Conversation conversation = findConversation(request.conversationId());
        if (conversation == null) {
            // Unknown conversation, resynchronise with the server
            ClientApp.getClientNetworking().conversationsRequest();
            return;
        }
        if (conversation.findMessage(request.message()) == null) {
            conversation.messages.add(request.message());
        }
        markCurrentAsRead();
        refresh();
    }

    public void realTimeMessageUpdate(ChatRequest request) {
        Conversation conversation = findConversation(request.conversationId());
        if (conversation == null) return;
        ChatMessage stored = conversation.findMessage(request.message());
        if (stored != null) {
            stored.content = request.message().content;
            stored.edited = request.message().edited;
            stored.isDeleted = request.message().isDeleted;
        }
        refresh();
    }

    public void realTimeRead(MemberRequest request) {
        Conversation conversation = findConversation(request.conversationId());
        if (conversation == null) return;
        conversation.markReadBy(request.username());
        refresh();
    }

    // ---- Conversation state ----

    private Conversation findConversation(String id) {
        if (id == null) return null;
        for (Conversation conversation : conversationsList) {
            if (id.equals(conversation.id)) return conversation;
        }
        return null;
    }

    // Inserts a conversation or replaces the existing copy with the same id
    private void upsertConversation(Conversation conversation) {
        mutateConversations(() -> {
            for (int i = 0; i < conversationsList.size(); i++) {
                if (conversationsList.get(i).id.equals(conversation.id)) {
                    conversationsList.set(i, conversation);
                    return;
                }
            }
            conversationsList.add(conversation);
        });
    }

    private void removeConversation(String id) {
        mutateConversations(() -> conversationsList.removeIf(c -> c.id.equals(id)));
        if (id.equals(currentConversationId)) {
            currentConversationId = null;
        }
        refresh();
    }

    // Opens a conversation and marks its messages as read
    private void openConversation(String id) {
        currentConversationId = id;
        markCurrentAsRead();
        refresh();
        view.getMessageTextArea().requestFocus();
    }

    private void markCurrentAsRead() {
        Conversation current = findConversation(currentConversationId);
        if (current == null || current.unreadCount(me()) == 0) return;
        // Messages are only read while the user can see them
        if (view.stage != null && !view.stage.isFocused()) return;
        current.markReadBy(me());
        ClientApp.getClientNetworking().readConversationRequest(current.id);
    }

    // Re-sorts conversations and redraws the conversation list and open conversation
    private void refresh() {
        Conversation current = findConversation(currentConversationId);
        mutateConversations(() -> {
            sortConversations();
            if (current == null) {
                view.getConversationsList().getSelectionModel().clearSelection();
            } else {
                view.getConversationsList().getSelectionModel().select(current);
            }
        });
        view.getConversationsList().refresh();
        view.showConversation(current);
    }

    // Changes the conversation list without the resulting selection changes opening a conversation
    private void mutateConversations(Runnable change) {
        boolean wasUpdating = updatingSelection;
        updatingSelection = true;
        try {
            change.run();
        } finally {
            updatingSelection = wasUpdating;
        }
    }

    private void sortConversations() {
        // Most recent activity first, empty conversations last
        Comparator<Conversation> byRecent = Comparator.comparingLong(
                c -> c.messages.isEmpty() ? Long.MIN_VALUE : c.lastMessage().timestamp);
        FXCollections.sort(conversationsList, byRecent.reversed());
    }

    private void setupConversationFiltering() {
        filteredConversations.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            String filter = view.getActiveConversationsFilter().getText();
            if (filter == null || filter.isBlank() || !view.isFilteringUsersProperty().get()) {
                return c -> true;
            }
            String lower = filter.toLowerCase();
            return c -> {
                if (c.name != null && c.name.toLowerCase().contains(lower)) {
                    return true;
                }
                for (String user : c.users) {
                    if (user.toLowerCase().contains(lower)) return true;
                }
                return false;
            };
            }, view.getActiveConversationsFilter().textProperty(), view.isFilteringUsersProperty())
        );
    }

    // ---- User actions ----

    public void sendMsg() {
        Conversation currConversation = findConversation(currentConversationId);
        if (currConversation == null) return;
        String enteredText = view.getMessageTextArea().getText().strip();
        if (enteredText.isEmpty()) {
            return;
        }
        if (enteredText.length() > Protocol.MAX_MESSAGE_LENGTH) {
            showWarning("Message Too Long", "Your message could not be sent",
                    "Messages can be at most " + Protocol.MAX_MESSAGE_LENGTH + " characters.");
            return;
        }
        lastSentTimestamp = Math.max(System.currentTimeMillis(), lastSentTimestamp + 1);
        ChatMessage message = new ChatMessage(me(), enteredText, lastSentTimestamp);

        currConversation.messages.add(message);
        ClientApp.getClientNetworking().messageRequest(currConversation.id, message);
        view.getMessageTextArea().clear();
        refresh();
    }

    public void editMessage(ChatMessage message) {
        Conversation conversation = findConversation(currentConversationId);
        if (conversation == null) return;

        TextArea editArea = new TextArea(message.content);
        editArea.setWrapText(true);
        editArea.setPrefRowCount(4);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message");
        dialog.getDialogPane().setContent(new VBox(editArea));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add("style.css");
        dialog.setResultConverter(button -> button == ButtonType.OK ? editArea.getText().strip() : null);
        editArea.requestFocus();

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isEmpty() || result.get().equals(message.content)) return;
        if (result.get().length() > Protocol.MAX_MESSAGE_LENGTH) {
            showWarning("Message Too Long", "Your message could not be edited",
                    "Messages can be at most " + Protocol.MAX_MESSAGE_LENGTH + " characters.");
            return;
        }

        message.content = result.get();
        message.edited = true;
        ClientApp.getClientNetworking().editMessageRequest(conversation.id, message);
        refresh();
    }

    public void deleteMessage(ChatMessage message) {
        Conversation conversation = findConversation(currentConversationId);
        if (conversation == null) return;
        if (!confirm("Delete Message", "Delete this message?", "Everyone in the conversation will see that it was deleted.")) return;

        message.isDeleted = true;
        ClientApp.getClientNetworking().deleteMessageRequest(conversation.id, message);
        refresh();
    }

    public void createConversation() {
        CreateConversationDialog dialog = new CreateConversationDialog(conversationsList);
        dialog.showAndWait();

        ArrayList<String> users = dialog.getConversationUsers();

        if (users != null) {
            Conversation conversation = new Conversation();
            conversation.name = dialog.getConversationName();
            conversation.users = users;
            ClientApp.getClientNetworking().createConversationRequest(conversation);
        }
    }

    public void addMember() {
        Conversation conversation = findConversation(currentConversationId);
        if (conversation == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Add a member to " + conversation.displayName(me()));
        dialog.setContentText("Username:");
        dialog.getDialogPane().getStylesheets().add("style.css");
        dialog.showAndWait()
                .map(String::trim)
                .filter(user -> !user.isEmpty())
                .ifPresent(user -> ClientApp.getClientNetworking().addMemberRequest(conversation.id, user));
    }

    public void leaveConversation() {
        Conversation conversation = findConversation(currentConversationId);
        if (conversation == null) return;
        if (confirm("Leave Conversation", "Leave " + conversation.displayName(me()) + "?",
                "You will no longer receive messages from this conversation.")) {
            ClientApp.getClientNetworking().leaveConversationRequest(conversation.id);
        }
    }

    public void openSettings() {
        settingsDialog = new SettingsDialog(me());
        settingsDialog.showAndWait();
        settingsDialog = null;
    }

    // Toggles the filtering property
    public void toggleFilter() {
        view.isFilteringUsersProperty().set(!view.isFilteringUsersProperty().get());
        if (view.isFilteringUsersProperty().get()) {
            view.getActiveConversationsFilter().requestFocus();
        }
    }

    // Sends logout request
    public void logout() {
        ClientApp.getClientNetworking().logoutRequest();
    }

    // ---- Dialog helpers ----

    private void showWarning(String title, String header, String message) {
        ClientApp.showErrorDialog(Alert.AlertType.WARNING, title, header, message);
    }

    private boolean confirm(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
}
