package com.github.Iks31.messagingapp.client;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JeSMSView implements UI {
    private final Button createConversationButton = new Button("Create Conversation");
    private final Button sendMessageButton = new Button("Send Message");
    private final VBox vbox = new VBox(10, new Label("JeSMS Main Page"), createConversationButton, sendMessageButton);
    private final Label currentConversationLabel = new Label("");
    private final Label activeConversationsLabel = new Label("Active Conversations");
    // Bind text field content to displaying of list view items
    // Conversations ordered by most recent message
    // Clicking on a list view item will display a conversation
    private final TextField activeConversationsFilter = new TextField();
    private final Scene scene = new Scene(vbox, 600, 400);

    @Override
    public Scene getScene (Stage stage) {
        return scene;
    }

    private Button getCreateConversationButton () {
        return createConversationButton;
    }
    private Button getSendMessageButton () {
        return sendMessageButton;
    }

}
