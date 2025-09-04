package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.ClientApp;
import com.github.Iks31.messagingapp.client.ClientNetworking;
import com.github.Iks31.messagingapp.client.ClientNetworking.*;
import com.github.Iks31.messagingapp.client.UI;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JeSMSView implements UI {

    // Sidebar
    private final Button settingsButton = new Button("Settings");
    private final Button createConversationButton = new Button("Create Conversation");
    private final Button filterToggleButton = new Button("Filter Users");
    private final VBox sidebar = new VBox(createConversationButton, filterToggleButton, settingsButton);

    // List of active conversations
    private final Label activeConversationsLabel = new Label("Active Conversations");
    private final ListView<String> conversationsList = new ListView<>();
    private final TextField activeConversationsFilter = new TextField();
    private final VBox conversationsContainer = new VBox(activeConversationsLabel, conversationsList, activeConversationsFilter);

    // Current conversation
    private final Label currConversationLabel = new Label("Current Conversation");
    private final ListView<String> currMessagesList = new ListView<>();
    private final TextArea messageTextArea = new TextArea();
    private final Button sendMessageButton = new Button("Send Message");
    private final HBox sendMessageContainer = new HBox(messageTextArea, sendMessageButton);
    private final VBox currConversationContainer = new VBox(currConversationLabel, currMessagesList, sendMessageContainer);

    // Bind text field content to displaying of list view items
    // Conversations ordered by most recent message
    // Clicking on a list view item will display a conversation

    // Root
    private final HBox rootPane = new HBox(sidebar, conversationsContainer, currConversationContainer);
    private final Scene scene = new Scene(rootPane, DEFAULT_WIDTH, DEFAULT_HEIGHT);

    public JeSMSView() {
        // Message text area
        messageTextArea.setPromptText("Type your message...");
        messageTextArea.setWrapText(true);
        messageTextArea.setPrefRowCount(2);
        messageTextArea.setMaxHeight(100);
        HBox.setHgrow(messageTextArea, Priority.ALWAYS);

        // Sidebar has small fixed width
        sidebar.setPrefWidth(50);

        // Conversations container takes 1/3
        conversationsContainer.setPrefWidth(DEFAULT_WIDTH / 3.0);
        conversationsContainer.setMinWidth(200);
        conversationsContainer.setMaxWidth(400);
        HBox.setHgrow(conversationsContainer, Priority.SOMETIMES);

        // Current conversation takes 2/3
        HBox.setHgrow(currConversationContainer, Priority.ALWAYS);
        currConversationContainer.setPrefWidth((DEFAULT_WIDTH * 2) / 3.0);
    }

    @Override
    public Scene getScene (Stage stage) {
        return scene;
    }

    public Button getCreateConversationButton () {
        return createConversationButton;
    }
    public Button getSendMessageButton () {
        return sendMessageButton;
    }
    public Button getFilterToggleButton () { return filterToggleButton; }
    public Button getSettingsButton () { return settingsButton; }
    public ListView<String> getConversationsList () { return conversationsList; }
    public ListView<String> getCurrMessagesList () { return currMessagesList; }

}
