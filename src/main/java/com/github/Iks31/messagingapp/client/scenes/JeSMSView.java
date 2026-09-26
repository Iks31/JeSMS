package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.ClientApp;
import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.IconButton;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class JeSMSView implements UI {
    public Stage stage;
    // Sidebar
    private final Button settingsButton = new IconButton("/images/settings.png");
    private final Button createConversationButton = new IconButton("/images/create-chat.png");
    private final Button filterToggleButton = new IconButton("/images/filter.png");
    private final SimpleBooleanProperty isFilteringUsers = new SimpleBooleanProperty(false);
    private final Button logoutButton = new IconButton("/images/logout.png");
    private final Region sidebarSpacer = new Region();
    private final VBox sidebar = new VBox(createConversationButton, filterToggleButton, settingsButton, sidebarSpacer, logoutButton);

    // List of active conversations
    private final Label activeConversationsLabel = new Label("Active Conversations");
    private final ListView<Conversation> conversationsList = new ListView<>();
    private final TextField activeConversationsFilter = new TextField();
    private final VBox conversationsContainer = new VBox(activeConversationsLabel, activeConversationsFilter, conversationsList);

    // Current conversation header
    private final Label currConversationLabel = new Label("Select a conversation");
    private final Label currMembersLabel = new Label();
    private final Button addMemberButton = new IconButton("/images/plus.png");
    private final Button leaveConversationButton = new IconButton("/images/logout.png");
    private final VBox headerText = new VBox(currConversationLabel, currMembersLabel);
    private final HBox conversationHeader = new HBox(headerText, addMemberButton, leaveConversationButton);

    // Current conversation
    private final ListView<ChatMessage> currMessagesList = new ListView<>();
    private final TextArea messageTextArea = new TextArea();
    private final Button sendMessageButton = new IconButton("/images/send.png");
    private final HBox sendMessageContainer = new HBox(messageTextArea, sendMessageButton);
    private final VBox currConversationContainer = new VBox(conversationHeader, currMessagesList, sendMessageContainer);

    // Conversation currently displayed, null if none
    private Conversation displayedConversation;
    // Actions for the message context menu, set by the controller
    private Consumer<ChatMessage> onEditMessage = m -> {};
    private Consumer<ChatMessage> onDeleteMessage = m -> {};

    // Formatter for datetime
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    // Root
    private final HBox rootPane = new HBox(sidebar, conversationsContainer, currConversationContainer);
    private final Scene scene = new Scene(rootPane, DEFAULT_WIDTH, DEFAULT_HEIGHT);

    public JeSMSView() {
        // Message text area
        messageTextArea.setPromptText("Type your message... (Enter to send, Shift+Enter for a new line)");
        messageTextArea.setWrapText(true);
        messageTextArea.setPrefRowCount(2);
        messageTextArea.setMaxHeight(100);
        HBox.setHgrow(messageTextArea, Priority.ALWAYS);
        sendMessageContainer.setSpacing(6);
        sendMessageContainer.setAlignment(Pos.CENTER);
        sendMessageContainer.setPadding(new Insets(8, 0, 0, 0));

        // Sidebar
        sidebar.setMinWidth(50);
        sidebar.setPrefWidth(60);
        sidebar.setMaxWidth(80);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setSpacing(6);
        sidebar.setPadding(new Insets(8, 0, 8, 0));
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);
        createConversationButton.setTooltip(new Tooltip("New conversation"));
        filterToggleButton.setTooltip(new Tooltip("Filter conversations"));
        settingsButton.setTooltip(new Tooltip("Settings"));
        logoutButton.setTooltip(new Tooltip("Log out"));

        // Filter Box with Toggle
        activeConversationsFilter.visibleProperty().bind(isFilteringUsers);
        activeConversationsFilter.managedProperty().bind(isFilteringUsers);
        activeConversationsFilter.setPromptText("Filter by conversation...");
        activeConversationsFilter.setMaxWidth(Double.MAX_VALUE);
        activeConversationsLabel.setMaxWidth(Double.MAX_VALUE);

        // Conversations container
        conversationsContainer.setMinWidth(200);
        conversationsContainer.setPrefWidth(280);
        conversationsContainer.setMaxWidth(400);
        HBox.setHgrow(conversationsContainer, Priority.SOMETIMES);
        VBox.setVgrow(conversationsList, Priority.ALWAYS);
        conversationsList.setPlaceholder(new Label("No conversations yet.\nStart one with the new chat button."));

        // Conversation header
        HBox.setHgrow(headerText, Priority.ALWAYS);
        headerText.setMaxWidth(Double.MAX_VALUE);
        conversationHeader.setAlignment(Pos.CENTER_LEFT);
        currMembersLabel.getStyleClass().add("conversation-members");
        addMemberButton.setTooltip(new Tooltip("Add member"));
        leaveConversationButton.setTooltip(new Tooltip("Leave conversation"));

        // Current conversation
        HBox.setHgrow(currConversationContainer, Priority.ALWAYS);
        VBox.setVgrow(currMessagesList, Priority.ALWAYS);
        VBox.setVgrow(sendMessageContainer, Priority.NEVER);
        currMessagesList.setPlaceholder(new Label("Select a conversation to start messaging"));
        currMessagesList.setFocusTraversable(false);
        showConversation(null);

        // Adding styles to necessary components
        scene.getStylesheets().add("style.css");
        activeConversationsLabel.getStyleClass().add("section-header");
        conversationHeader.getStyleClass().add("section-header");
        currConversationLabel.getStyleClass().add("conversation-title");
        sidebar.getStyleClass().add("sidebar");
        conversationsContainer.getStyleClass().add("conversations-container");
        currConversationContainer.getStyleClass().add("curr-conversation-container");
        currMessagesList.getStyleClass().add("messages-list");

        // Sets up list view appearances
        setUpMessageCellFactory();
        setUpConversationCellFactory();
    }

    @Override
    public Scene getScene (Stage stage) {
        this.stage = stage;
        return scene;
    }

    // Displays the given conversation, or an empty state if null
    public void showConversation(Conversation conversation) {
        boolean sameConversation = conversation != null && displayedConversation != null
                && conversation.id.equals(displayedConversation.id);
        int previousCount = currMessagesList.getItems().size();
        displayedConversation = conversation;

        boolean empty = conversation == null;
        addMemberButton.setVisible(!empty && conversation.isGroup());
        addMemberButton.setManaged(addMemberButton.isVisible());
        leaveConversationButton.setVisible(!empty);
        leaveConversationButton.setManaged(!empty);
        messageTextArea.setDisable(empty);
        sendMessageButton.setDisable(empty);

        if (empty) {
            currConversationLabel.setText("Select a conversation");
            currMembersLabel.setText("");
            currMessagesList.setItems(FXCollections.observableArrayList());
            return;
        }

        String me = ClientApp.getClientNetworking().getUsername();
        currConversationLabel.setText(conversation.displayName(me));
        currMembersLabel.setText(conversation.isGroup() ? String.join(", ", conversation.users) : "Direct message");
        currMessagesList.setItems(FXCollections.observableArrayList(conversation.messages));
        // Keeps the scroll position when refreshing, but jumps to the latest message on open or new messages
        if (!sameConversation || conversation.messages.size() > previousCount) {
            currMessagesList.scrollTo(conversation.messages.size() - 1);
        }
    }

    public void setUpMessageCellFactory() {
        currMessagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessage msg, boolean empty) {
                super.updateItem(msg, empty);

                // No item edge case
                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    return;
                }
                String me = ClientApp.getClientNetworking().getUsername();
                boolean sentByMe = msg.sender.equals(me);

                // Sender, timestamp, edited flag and read status
                StringBuilder metaText = new StringBuilder(sentByMe ? "You" : msg.sender);
                metaText.append(" • ").append(DATE_TIME_FORMATTER.format(msg.getTimestampInstant()));
                if (msg.edited && !msg.isDeleted) metaText.append(" • edited");
                if (sentByMe && !msg.isDeleted) metaText.append(" • ").append(readStatus(msg));
                Label meta = new Label(metaText.toString());
                meta.getStyleClass().add("message-meta");

                // Message content
                Label content = new Label(msg.displayContent());
                content.getStyleClass().add("message-content");
                if (msg.isDeleted) content.getStyleClass().add("message-deleted");
                content.setWrapText(true);

                // Message bubble
                VBox bubble = new VBox(meta, content);
                bubble.getStyleClass().add("message-bubble");
                bubble.setMinWidth(80);
                bubble.maxWidthProperty().bind(currMessagesList.widthProperty().multiply(0.65));

                // Wrapper container for message needed for styling and alignment
                HBox wrapper = new HBox(bubble);
                wrapper.setFillHeight(true);
                wrapper.setAlignment(sentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                wrapper.getStyleClass().add(sentByMe ? "sent-message" : "received-message");
                setGraphic(wrapper);

                // Own messages can be edited or deleted
                if (sentByMe && !msg.isDeleted) {
                    MenuItem edit = new MenuItem("Edit");
                    edit.setOnAction(e -> onEditMessage.accept(msg));
                    MenuItem delete = new MenuItem("Delete");
                    delete.setOnAction(e -> onDeleteMessage.accept(msg));
                    setContextMenu(new ContextMenu(edit, delete));
                } else {
                    setContextMenu(null);
                }
            }
        });
    }

    // Read receipt text for a message sent by the current user
    private String readStatus(ChatMessage msg) {
        long readers = msg.readBy.stream().filter(u -> !u.equals(msg.sender)).count();
        if (readers == 0) return "Sent";
        if (displayedConversation != null && displayedConversation.users.size() > 2) return "Seen by " + readers;
        return "Seen";
    }

    public void setUpConversationCellFactory() {
        conversationsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation conversation, boolean empty) {
                super.updateItem(conversation, empty);

                // No item edge case
                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String me = ClientApp.getClientNetworking().getUsername();

                // Conversation name is group chat name or the users the current user is messaging
                Label conversationNameLabel = new Label(conversation.displayName(me));
                conversationNameLabel.getStyleClass().add("conversation-name");
                conversationNameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                conversationNameLabel.setMinWidth(0);
                conversationNameLabel.setMaxWidth(Double.MAX_VALUE);

                // Preview of recent chat content
                Label recentChatContent = new Label();
                recentChatContent.getStyleClass().add("conversation-preview");
                recentChatContent.setTextOverrun(OverrunStyle.ELLIPSIS);
                recentChatContent.setMinWidth(0);
                recentChatContent.setMaxWidth(Double.MAX_VALUE);

                // Time of most recent chat
                Label recentChatTime = new Label();
                recentChatTime.getStyleClass().add("conversation-time");
                recentChatTime.setMinWidth(Region.USE_PREF_SIZE);

                // Only adds preview content if there is a recent message
                ChatMessage recentMessage = conversation.lastMessage();
                if (recentMessage != null) {
                    String sender = recentMessage.sender.equals(me) ? "You" : recentMessage.sender;
                    // Only the first line of a multi-line message is previewed
                    recentChatContent.setText(sender + ": " + recentMessage.displayContent().lines().findFirst().orElse(""));
                    recentChatTime.setText(DATE_TIME_FORMATTER.format(recentMessage.getTimestampInstant()));
                } else {
                    recentChatContent.setText("No messages yet");
                }

                // Unread message count
                int unread = conversation.unreadCount(me);
                Label unreadBadge = new Label(String.valueOf(unread));
                unreadBadge.getStyleClass().add("unread-badge");
                unreadBadge.setMinWidth(Region.USE_PREF_SIZE);
                unreadBadge.setVisible(unread > 0);
                unreadBadge.setManaged(unread > 0);

                // Top row displays name and time, bottom row displays preview and unread count
                HBox topRow = new HBox(8, conversationNameLabel, recentChatTime);
                HBox.setHgrow(conversationNameLabel, Priority.ALWAYS);
                topRow.setAlignment(Pos.CENTER_LEFT);
                HBox bottomRow = new HBox(8, recentChatContent, unreadBadge);
                HBox.setHgrow(recentChatContent, Priority.ALWAYS);
                bottomRow.setAlignment(Pos.CENTER_LEFT);

                // Overall container for an individual conversation, sized to the list so labels truncate
                VBox conversationContainer = new VBox(4, topRow, bottomRow);
                conversationContainer.setPadding(new Insets(4));
                conversationContainer.prefWidthProperty().bind(conversationsList.widthProperty().subtract(40));
                conversationContainer.setMaxWidth(Region.USE_PREF_SIZE);

                setText(null);
                setGraphic(conversationContainer);
            }
        });
    }

    public void setOnEditMessage(Consumer<ChatMessage> onEditMessage) { this.onEditMessage = onEditMessage; }
    public void setOnDeleteMessage(Consumer<ChatMessage> onDeleteMessage) { this.onDeleteMessage = onDeleteMessage; }

    public Button getCreateConversationButton () { return createConversationButton; }
    public Button getSendMessageButton () { return sendMessageButton; }
    public Button getFilterToggleButton () { return filterToggleButton; }
    public Button getLogoutButton () { return logoutButton; }
    public Button getSettingsButton () { return settingsButton; }
    public Button getAddMemberButton () { return addMemberButton; }
    public Button getLeaveConversationButton () { return leaveConversationButton; }
    public BooleanProperty isFilteringUsersProperty() { return isFilteringUsers; }
    public TextField getActiveConversationsFilter() { return activeConversationsFilter; }
    public ListView<Conversation> getConversationsList () { return conversationsList; }
    public ListView<ChatMessage> getCurrMessagesList () { return currMessagesList; }
    public Label getCurrConversationLabel () { return currConversationLabel; }
    public TextArea getMessageTextArea () { return messageTextArea; }

}
