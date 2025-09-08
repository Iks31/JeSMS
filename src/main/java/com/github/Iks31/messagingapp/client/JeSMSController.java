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
        // Now event handlers have been added to send the index of the conversation
        view.getSendMessageButton().setOnAction(e -> sendMsg());
        view.getConversationsList().setOnMouseClicked(e -> messageDataSetup());
    }

    // Sets up the messages depending on what conversation is being viewed
    public void messageDataSetup() {
        Conversation currConversation;
        int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
        ObservableList<String> messages = FXCollections.observableArrayList();
        for(ChatMessage message: conversationsList.get(index).messages){
            messages.add(message.sender + ": " + message.content);
        }

        view.getCurrMessagesList().setItems(messages);
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
        ObservableList<String> conversationName = FXCollections.observableArrayList();
        for (String json : jsons) {
            try{
                currConversation = mapper.readValue(json, Conversation.class);
                conversationsList.add(currConversation);
                if (currConversation.name.isEmpty()) {
                    // Temporary to show private DM - will be username after
                    conversationName.add("Private DM");
                } else {
                    conversationName.add(currConversation.name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        view.getConversationsList().setItems(conversationName);
    }
    //new method to send a message after button has been clicked
    //creates a new message and adds it to the observable list and then calls again to reformat messages for user
    public void sendMsg() {
        int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
        Conversation currConversation = conversationsList.get(index);
        ChatMessage message = new ChatMessage();
        {
            message.sender = ClientApp.getClientNetworking().getUsername();
            message.content = view.getMessageTextArea().getText();
            //TODO needs to be updated
            message.timestamp = null;
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
}
