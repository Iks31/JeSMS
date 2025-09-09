package com.github.Iks31.messagingapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JeSMSController {
    private final JeSMSView view;
    ObservableList<Conversation> conversationsList = FXCollections.observableArrayList();
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
                    // Code here for successful conversation creation
                    // e.g. new conversations request to maintain consistency with server
                    ClientApp.getClientNetworking().conversationsRequest();
                });
            } else if ("CREATE_CONVERSATION_FAIL".equals(msg.getFlag())) {
                Platform.runLater(() -> {
                   // Code for conversation failure
                   // e.g. Alert that informs user that the conversation already exists or that a user entered
                   // does not exist
                    ClientApp.showErrorDialog(Alert.AlertType.WARNING, "Conversation Error", "Failed to Create Conversation", msg.getContent().toString());
                });
            }
        });
        ClientApp.getClientNetworking().conversationsRequest();
        view.getCreateConversationButton().setOnAction(e -> createConversation());
        view.getSendMessageButton().setOnAction(e -> sendMsg());
        view.getConversationsList().setOnMouseClicked(e -> messageDataSetup());
        view.getFilterToggleButton().setOnAction(e -> toggleFilter());
    }

    // Sets up the messages depending on what conversation is being viewed
    public void messageDataSetup() {
        int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
        if (index >= conversationsList.size()) {
            return;
        }
        String currName = conversationsList.get(index).name;
        List<String> currUsers = conversationsList.get(index).users;
        if (currName.isEmpty()) {
            if (currUsers.getFirst().equals(ClientApp.getClientNetworking().getUsername())) {
                view.getCurrConversationLabel().setText(currUsers.getLast());
            } else {
                view.getCurrConversationLabel().setText(currUsers.getFirst());
            }
        } else {
            view.getCurrConversationLabel().setText(currName);
        }
        ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
        messages.addAll(conversationsList.get(index).messages);
        view.getCurrMessagesList().setItems(messages);
        view.getCurrMessagesList().scrollTo(view.getCurrMessagesList().getItems().size() - 1);
    }

    public void realTimeMessage(ArrayList<Object> content){
        ChatMessage message = (ChatMessage)content.get(0);
        ArrayList<String> users = (ArrayList<String>)content.get(1);
        for(Conversation conversation : conversationsList){
            if(users.equals(conversation.users)){
                conversation.messages.add(message);
            }
        }
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
        view.getConversationsList().setItems(conversationsList);
    }
    //new method to send a message after button has been clicked
    //creates a new message and adds it to the observable list and then calls again to reformat messages for user
    public void sendMsg() {
        int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        String enteredText = view.getMessageTextArea().getText();
        if (enteredText.isEmpty()) {
            return;
        }
        Conversation currConversation = conversationsList.get(index);
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
    public void toggleFilter() {
        view.isFilteringUsersProperty().set(!view.isFilteringUsersProperty().get());
    }
}
