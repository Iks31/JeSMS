package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.ClientApp;
import com.github.Iks31.messagingapp.client.ClientNetworking;
import com.github.Iks31.messagingapp.client.ClientNetworking.*;
import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.IconButton;
import com.github.Iks31.messagingapp.common.ChatMessage;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JeSMSView implements UI {

    // Sidebar
    private final Button settingsButton = new IconButton("/images/settings.png");
    private final Button createConversationButton = new IconButton("/images/create-chat.png");
    private final Button filterToggleButton = new IconButton("/images/filter.png");
    private final SimpleBooleanProperty isFilteringUsers = new SimpleBooleanProperty(false);
    private final VBox sidebar = new VBox(createConversationButton, filterToggleButton, settingsButton);

    // List of active conversations
    private final Label activeConversationsLabel = new Label("Active Conversations");
    private final ListView<String> conversationsList = new ListView<>();
    private final TextField activeConversationsFilter = new TextField();
    private final VBox conversationsContainer = new VBox(activeConversationsLabel, conversationsList, activeConversationsFilter);

    // Current conversation
    private final Label currConversationLabel = new Label("Current Conversation");
    private ListView<ChatMessage> currMessagesList = new ListView<>(); //changed to string as we just want to show the sender and message
    private final TextArea messageTextArea = new TextArea();
    private final Button sendMessageButton = new IconButton("/images/send.png");
    private final HBox sendMessageContainer = new HBox(messageTextArea, sendMessageButton);
    private final VBox currConversationContainer = new VBox(currConversationLabel, currMessagesList, sendMessageContainer);

    // Bind text field content to displaying of list view items
    // Conversations ordered by most recent message
    // Clicking on a list view item will display a conversation

    // Formatter for datetime
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
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
        activeConversationsFilter.visibleProperty().bind(isFilteringUsers);
        activeConversationsFilter.setPromptText("Filter by conversation...");

        // Conversations container takes 1/3
        conversationsContainer.setPrefWidth(DEFAULT_WIDTH / 3.0);
        conversationsContainer.setMinWidth(200);
        conversationsContainer.setMaxWidth(400);
        HBox.setHgrow(conversationsContainer, Priority.SOMETIMES);

        // Current conversation takes 2/3
        HBox.setHgrow(currConversationContainer, Priority.ALWAYS);
        currConversationContainer.setPrefWidth((DEFAULT_WIDTH * 2) / 3.0);

        // Adding styles to necessary components
        scene.getStylesheets().add("style.css");
        activeConversationsLabel.getStyleClass().add("section-header");
        currConversationLabel.getStyleClass().add("section-header");
        sidebar.getStyleClass().add("sidebar");
        conversationsContainer.getStyleClass().add("conversations-container");
        currConversationContainer.getStyleClass().add("curr-conversation-container");

        setUpCellFactory();
    }

    @Override
    public Scene getScene (Stage stage) {
        return scene;
    }

    public void setUpCellFactory() {
        currMessagesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessage msg, boolean empty) {
                super.updateItem(msg, empty);

                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Sender + timestamp
                    Label meta = new Label(msg.sender + " • " +
                            DATE_TIME_FORMATTER.format(msg.getTimestampInstant()));
                    meta.getStyleClass().add("message-meta");

                    // Message content
                    Label content = new Label(msg.content);
                    content.setWrapText(true);
                    content.setMaxWidth(300);
                    content.getStyleClass().add("message-content");

                    VBox bubble = new VBox(meta, content);
                    bubble.getStyleClass().add("message-bubble");

                    HBox wrapper = new HBox(bubble);
                    wrapper.getStyleClass().add(msg.sender.equals(ClientApp.getClientNetworking().getUsername())
                            ? "sent-message"
                            : "received-message"
                    );

                    setGraphic(wrapper);
                }
            }
        });
    }

    public Button getCreateConversationButton () {
        return createConversationButton;
    }
    public Button getSendMessageButton () {
        return sendMessageButton;
    }
    public Button getFilterToggleButton () { return filterToggleButton; }
    public BooleanProperty isFilteringUsersProperty() { return isFilteringUsers; }
    public Button getSettingsButton () { return settingsButton; }
    public ListView<String> getConversationsList () { return conversationsList; }
    public ListView<ChatMessage> getCurrMessagesList () { return currMessagesList; }
    public Label getCurrConversationLabel () { return currConversationLabel; }
    public TextArea getMessageTextArea () { return messageTextArea; }

}
