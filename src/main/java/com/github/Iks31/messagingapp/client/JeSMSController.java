package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import javafx.collections.FXCollections;
import org.bson.Document;

import java.util.ArrayList;

public class JeSMSController {
    private final JeSMSView view;
   // private final Conversations conversations;

    public JeSMSController(JeSMSView view) {
        this.view = view;
        ClientApp.getClientNetworking().setMessageHandler(msg -> {
            if ("CONVERSATIONS_RECEIVED".equals(msg.getFlag())) {
                ArrayList<String> conversationsjson = (ArrayList<String>)msg.getContent();
                ArrayList<Document> conversations = new ArrayList<>();
                for(String c : conversationsjson) {
                    conversations.add(Document.parse(c));

                }
                //TODO conversation and messages here
              //  this.conversations = new Conversations();
              //  this.displayedMessages = new Message();
            } else if ("CONVERSATIONS_NOT_RECEIVED".equals(msg.getFlag())) {
                //TODO what happens when the conversations have not been retrieved
            }
        });
        ClientApp.getClientNetworking().conversationsRequest();

        // Update view based on model data gathered from server here and fix events
         attachEvents();
    }

    private void attachEvents() {
        // Add button events
        view.getCreateConversationButton().setOnAction(e -> createConversation());
        view.getSendMessageButton().setOnAction(e -> sendMsg());

        // Update messages based on refresh
        messageDataSetup();
    }

    public void messageDataSetup() {
        // Add relevant data to list views
        view.getConversationsList().setItems(FXCollections.observableArrayList("James", "Ben", "Sammy"));
        view.getCurrMessagesList().setItems(FXCollections.observableArrayList("Hello", "Hi!", "What's Up?"));
    }

    public void sendMsg() {}
    public void createConversation() {}
}
