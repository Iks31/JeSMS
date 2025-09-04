package com.github.Iks31.messagingapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.bson.Document;

import java.util.ArrayList;

public class JeSMSController {
    private final JeSMSView view;
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
        view.getSendMessageButton().setOnAction(e -> sendMsg());

        // Update messages based on refresh
        messageDataSetup();
    }

    public void messageDataSetup() {
        // Add relevant data to list views
      //  view.getConversationsList().setItems(FXCollections.observableArrayList("James", "Ben", "Sammy"));
      //  view.getCurrMessagesList().setItems(FXCollections.observableArrayList("Hello", "Hi!", "What's Up?"));
    }

    public void formatConversations(ArrayList<String> jsons) {
        ObjectMapper mapper = new ObjectMapper();
        Conversation currConversation;
        ObservableList<Conversation> conversationsList = FXCollections.observableArrayList();
        for (String json : jsons) {
            try{
                currConversation = mapper.readValue(json, Conversation.class);
                conversationsList.add(currConversation);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println(conversationsList.toString());
        view.getConversationsList().setItems(conversationsList);
    }
    public void sendMsg() {}
    public void createConversation() {}
}
