package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.client.scenes.JeSMSView;
import javafx.collections.FXCollections;

public class JeSMSController {
    private final JeSMSView view;
    private final Conversations conversations;
    private final Messages displayedMessages;

    public JeSMSController(JeSMSView view) {
        this.view = view;
        this.conversations = new Conversations();
        this.displayedMessages = new Messages();

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
