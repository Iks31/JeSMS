package com.github.Iks31.messagingapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bson.Document;

import java.util.ArrayList;

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
        // Add button events
        ClientApp.getClientNetworking().setMessageHandler(msg -> {
            if ("CONVERSATIONS_RECEIVED".equals(msg.getFlag())) {
                ArrayList<String> conversationsjson = (ArrayList<String>)msg.getContent();
                Platform.runLater(() -> formatConversations(conversationsjson));
                //TODO conversation and messages here
                //  this.conversations = new Conversations();
                //  this.displayedMessages = new Message();
            } else if ("CONVERSATIONS_NOT_RECEIVED".equals(msg.getFlag())) {
                //TODO what happens when the conversations have not been retrieved
            }
        });
        ClientApp.getClientNetworking().conversationsRequest();
        view.getCreateConversationButton().setOnAction(e -> createConversation());
        //now event handlers have been added to to send the index of the conversation
        view.getSendMessageButton().setOnAction(e -> {
            int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
            sendMsg(index);});
        view.getConversationsList().setOnMouseClicked(e -> {
            int index = view.getConversationsList().getSelectionModel().getSelectedIndex();
            messageDataSetup(index);
        });

        // Update messages based on refresh
    }
// Sets up the messages depending on what conversation is being viewed
    public void messageDataSetup(int index) {
        Conversation currConversation;
        ObservableList<String> messages = FXCollections.observableArrayList();
        for(ChatMessage message: conversationsList.get(index).messages){
            messages.add(message.sender + ": " + message.content);
        }

        view.getCurrMessagesList().setItems(messages);
        // Add relevant data to list views
      //  view.getConversationsList().setItems(FXCollections.observableArrayList("James", "Ben", "Sammy"));
      //  view.getCurrMessagesList().setItems(FXCollections.observableArrayList("Hello", "Hi!", "What's Up?"));
    }


    public void formatConversations(ArrayList<String> jsons) {
        ObjectMapper mapper = new ObjectMapper();
        Conversation currConversation;
        ObservableList<String> conversationName = FXCollections.observableArrayList();
        for (String json : jsons) {
            try{
                currConversation = mapper.readValue(json, Conversation.class);
                conversationsList.add(currConversation);
                conversationName.add(currConversation.name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(conversationsList.toString());
        view.getConversationsList().setItems(conversationName);
    }
    //new method to send a message after button has been clicked
    //creates a new message and adds it to the observable list and then calls again to reformat messages for user
    public void sendMsg(int index) {
        Conversation currConversation = conversationsList.get(index);
        ChatMessage message = new ChatMessage();
        {
            message.sender = ClientApp.getClientNetworking().getUsername();
            message.content = view.getMessageTextArea().getText();
            message.timestamp = null;
            message.readBy = new ArrayList<>();
            message.edited = false;
            message.isDeleted = false;
        }
        currConversation.messages.add(message);
        ArrayList<Object> messageData = new ArrayList<>();
        messageData.add(currConversation.name);
        messageData.add(ClientApp.getClientNetworking().getUsername());
        messageData.add(currConversation.users);
        messageData.add(currConversation.messages.getLast().content);
        ClientApp.getClientNetworking().messageRequest(messageData);
        messageDataSetup(index);
    }
    public void createConversation() {}
}
