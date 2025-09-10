package com.github.Iks31.messagingapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import com.github.Iks31.messagingapp.client.scenes.StartMenu;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JeSMSController {
    private final JeSMSView view;
    ObservableList<Conversation> conversationsList = FXCollections.observableArrayList();
    FilteredList<Conversation> filteredConversations = new FilteredList<>(conversationsList);
   // private final Conversations conversations;

    public JeSMSController(JeSMSView view) {
        this.view = view;
        // Update view based on model data gathered from server here and fix events
        attachEvents();
    }

    private void attachEvents() {
        ClientApp.getClientNetworking().setMessageHandler(msg -> {
            if ("CONVERSATIONS_RECEIVED".equals(msg.getFlag())) {
                ArrayList<String> conversationsjson = (ArrayList<String>)msg.getContent();
                Platform.runLater(() -> formatConversations(conversationsjson));
                //TODO conversation and messages here
                //  this.conversations = new Conversations();
                //  this.displayedMessages = new Message();
            } else if ("CONVERSATIONS_NOT_RECEIVED".equals(msg.getFlag())) {
                //TODO what happens when the conversations have not been retrieved
            } else if ("REALTIME_CHAT".equals(msg.getFlag())) {
                Platform.runLater(() -> realTimeMessage((ArrayList<Object>) msg.getContent()));
            } else if ("REALTIME_CONVERSATION".equals(msg.getFlag())) {
                Platform.runLater(() -> realTimeConversation((Conversation) msg.getContent()));
            } else if ("CREATE_CONVERSATION_SUCCESS".equals(msg.getFlag())) {
                Platform.runLater(() -> {
                    ClientApp.getClientNetworking().conversationsRequest();
                });
            } else if ("CREATE_CONVERSATION_FAIL".equals(msg.getFlag())) {
                Platform.runLater(() -> {
                   ClientApp.showErrorDialog(Alert.AlertType.WARNING, "Conversation Error", "Failed to Create Conversation", msg.getContent().toString());
                });
            } else if ("LOGOUT_SUCCESS".equals(msg.getFlag())) {
                Platform.runLater(() -> {
                    ClientApp.getClientNetworking().setUsername(null);
                    view.stage.setScene(new StartMenu().getScene(view.stage));
                });
            }
        });
        ClientApp.getClientNetworking().conversationsRequest();
        view.getCreateConversationButton().setOnAction(e -> createConversation());
        view.getSendMessageButton().setOnAction(e -> sendMsg());
        view.getConversationsList().setOnMouseClicked(e -> messageDataSetup());
        view.getFilterToggleButton().setOnAction(e -> toggleFilter());
        view.getLogoutButton().setOnAction(e -> logout());
        setupConversationFiltering();
    }

    // Sets up the messages depending on what conversation is being viewed
    public void messageDataSetup() {
        Conversation selectedConversation = view.getConversationsList().getSelectionModel().getSelectedItem();
        if (selectedConversation == null) {
            return;
        }
        String currName = selectedConversation.name;
        List<String> currUsers = selectedConversation.users;
        if (currName.isEmpty()) {
            if (currUsers.getFirst().equals(ClientApp.getClientNetworking().getUsername())) {
                view.getCurrConversationLabel().setText(currUsers.getLast());
            } else {
                view.getCurrConversationLabel().setText(currUsers.getFirst());
            }
        } else {
            view.getCurrConversationLabel().setText(currName);
        }

        // Load Messages
        ObservableList<ChatMessage> messages = FXCollections.observableArrayList(selectedConversation.messages);
        view.getCurrMessagesList().setItems(messages);
        view.getCurrMessagesList().scrollTo(messages.size() - 1);
    }

    public void realTimeMessage(ArrayList<Object> content) {
        ChatMessage message = (ChatMessage)content.get(0);
        ArrayList<String> users = (ArrayList<String>) content.get(1);
        for(Conversation conversation : conversationsList){
            if(users.equals(conversation.users)){
                conversation.messages.add(message);
            }
        }
        sortConversations();
        messageDataSetup();
    }

    public void realTimeConversation(Conversation conversation){
        conversationsList.add(conversation);
    }

    public void formatConversations(ArrayList<String> jsons) {
        ObjectMapper mapper = new ObjectMapper();
        conversationsList.clear();
        Conversation currConversation;
        for (String json : jsons) {
            try{
                currConversation = mapper.readValue(json, Conversation.class);
                conversationsList.add(currConversation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sortConversations();
        view.getConversationsList().setItems(filteredConversations);
    }

    private void setupConversationFiltering() {
        filteredConversations.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            String filter = view.getActiveConversationsFilter().getText();
            if (filter == null || filter.isBlank()) {
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
            }, view.getActiveConversationsFilter().textProperty())
        );
    }

    //new method to send a message after button has been clicked
    //creates a new message and adds it to the observable list and then calls again to reformat messages for user
    public void sendMsg() {
        Conversation currConversation = view.getConversationsList().getSelectionModel().getSelectedItem();
        if (currConversation == null) return;
        String enteredText = view.getMessageTextArea().getText();
        if (enteredText.isEmpty()) {
            return;
        }
        ChatMessage message = new ChatMessage();
        {
            message.sender = ClientApp.getClientNetworking().getUsername();
            message.content = enteredText;
            // Creates MongoLong for current timestamp
            ChatMessage.MongoLong mongoLong = new ChatMessage.MongoLong();
            mongoLong.value = String.valueOf(Instant.now().toEpochMilli());
            message.timestamp = mongoLong;

            message.readBy = new ArrayList<>();
            message.edited = false;
            message.isDeleted = false;
        }
        currConversation.messages.add(message);
        ArrayList<Object> conversationData = new ArrayList<>();
        conversationData.add(currConversation.name);
        conversationData.add(currConversation.users);
        conversationData.add(message);
        ClientApp.getClientNetworking().messageRequest(conversationData);
        view.getMessageTextArea().clear();
        sortConversations();
        messageDataSetup();
    }

    public void createConversation() {
        CreateConversationDialog dialog = new CreateConversationDialog(conversationsList);
        dialog.showAndWait();

        ArrayList<String> users = dialog.getConversationUsers();

        if (users != null) {
            Conversation conversation = new Conversation();
            conversation.name = dialog.getConversationName();
            conversation.users = users;
            conversation.messages = new ArrayList<>();
            ClientApp.getClientNetworking().createConversationRequest(conversation);
        }
    }

    public void sortConversations() {
        conversationsList.sort((c1, c2) -> {
            if (c1.messages.isEmpty() && c2.messages.isEmpty()) return 0;
            if (c1.messages.isEmpty()) return 1;
            if (c2.messages.isEmpty()) return -1;

            // Compare most recent timestamps (newest first)
            return c2.messages.getLast().getTimestampInstant()
                    .compareTo(c1.messages.getLast().getTimestampInstant());
        });
    }

    public void toggleFilter() {
        view.isFilteringUsersProperty().set(!view.isFilteringUsersProperty().get());
    }

    public void logout() {
        // Send logout request
        ClientApp.getClientNetworking().logoutRequest();
    }
}
