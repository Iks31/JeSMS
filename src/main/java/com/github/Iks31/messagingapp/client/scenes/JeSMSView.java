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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JeSMSView implements UI {
    public Stage stage;
    // Sidebar
    private final Button settingsButton = new IconButton("/images/settings.png");
    private final Button createConversationButton = new IconButton("/images/create-chat.png");
    private final Button filterToggleButton = new IconButton("/images/filter.png");
    private final SimpleBooleanProperty isFilteringUsers = new SimpleBooleanProperty(false);
    private final Button logoutButton = new IconButton("/images/logout.png");
    private final VBox sidebar = new VBox(createConversationButton, filterToggleButton, settingsButton, logoutButton);

    // List of active conversations
    private final Label activeConversationsLabel = new Label("Active Conversations");
    private final ListView<Conversation> conversationsList = new ListView<>();
    private final TextField activeConversationsFilter = new TextField();
    private final VBox conversationsContainer = new VBox(activeConversationsLabel, conversationsList, activeConversationsFilter);

    // Current conversation
    private final Label currConversationLabel = new Label("Current Conversation");
    private ListView<ChatMessage> currMessagesList = new ListView<>();
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

        // Sidebar
        sidebar.setMinWidth(50);
        sidebar.setPrefWidth(60);
        sidebar.setMaxWidth(80);
        VBox.setVgrow(createConversationButton, Priority.NEVER);
        VBox.setVgrow(filterToggleButton, Priority.NEVER);
        VBox.setVgrow(settingsButton, Priority.NEVER);
        VBox.setVgrow(logoutButton, Priority.ALWAYS);

        // Filter Box with Toggle
        activeConversationsFilter.visibleProperty().bind(isFilteringUsers);
        activeConversationsFilter.setPromptText("Filter by conversation...");
        activeConversationsFilter.setMaxWidth(Double.MAX_VALUE);

        // Conversations container
        conversationsContainer.setMinWidth(200);
        conversationsContainer.setPrefWidth(300);
        conversationsContainer.setMaxWidth(600);
        HBox.setHgrow(conversationsContainer, Priority.SOMETIMES);
        VBox.setVgrow(conversationsList, Priority.ALWAYS);

        // Current conversation
        HBox.setHgrow(currConversationContainer, Priority.ALWAYS);
        VBox.setVgrow(currMessagesList, Priority.ALWAYS);
        VBox.setVgrow(sendMessageContainer, Priority.NEVER);

        // Adding styles to necessary components
        scene.getStylesheets().add("style.css");
        activeConversationsLabel.getStyleClass().add("section-header");
        currConversationLabel.getStyleClass().add("section-header");
        sidebar.getStyleClass().add("sidebar");
        conversationsContainer.getStyleClass().add("conversations-container");
        currConversationContainer.getStyleClass().add("curr-conversation-container");

        setUpMessageCellFactory();
        setUpConversationCellFactory();
    }

    @Override
    public Scene getScene (Stage stage) {
        this.stage = stage;
        return scene;
    }

    public void setUpMessageCellFactory() {
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
                    content.getStyleClass().add("message-content");
                    content.setWrapText(true);

                    VBox bubble = new VBox(meta, content);
                    bubble.getStyleClass().add("message-bubble");
                    bubble.setMinWidth(80);
                    bubble.setMaxWidth(500);
                    bubble.maxWidthProperty().bind(
                            currMessagesList.widthProperty().multiply(0.50)
                    );

                    HBox wrapper = new HBox(bubble);
                    wrapper.setFillHeight(true);
                    boolean sentByMe = msg.sender.equals(ClientApp.getClientNetworking().getUsername());
                    wrapper.setAlignment(sentByMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    wrapper.getStyleClass().add(sentByMe ? "sent-message" : "received-message");
                    setGraphic(wrapper);
                }
            }
        });
    }

    public void setUpConversationCellFactory() {
        conversationsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Conversation conversation, boolean empty) {
                super.updateItem(conversation, empty);
                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                } else {

                    Label conversationNameLabel = new Label();
                    conversationNameLabel.getStyleClass().add("conversation-name");
                    conversationNameLabel.setWrapText(true);

                    if (conversation.name.isEmpty()) {
                        if (conversation.users.getFirst().equals(ClientApp.getClientNetworking().getUsername())) {
                            conversationNameLabel.setText(conversation.users.getLast());
                        } else {
                            conversationNameLabel.setText(conversation.users.getFirst());
                        }
                    } else {
                        conversationNameLabel.setText(conversation.name);
                    }

                    Label recentChatContent = new Label();
                    recentChatContent.getStyleClass().add("conversation-preview");
                    recentChatContent.setWrapText(true);
                    recentChatContent.setMaxHeight(40);
                    recentChatContent.setTextOverrun(OverrunStyle.ELLIPSIS);

                    Label recentChatTime = new Label();
                    recentChatTime.getStyleClass().add("conversation-time");
                    recentChatTime.setWrapText(true);
                    HBox.setHgrow(recentChatTime, Priority.NEVER);

                    if (!conversation.messages.isEmpty()) {
                        ChatMessage recentMessage = conversation.messages.getLast();
                        if (recentMessage.sender.equals(ClientApp.getClientNetworking().getUsername())) {
                            recentChatContent.setText("You: " + recentMessage.content);
                        } else {
                            recentChatContent.setText(recentMessage.sender + ": " + recentMessage.content);
                        }
                        recentChatTime.setText(DATE_TIME_FORMATTER.format(recentMessage.getTimestampInstant()));
                    } else {
                        recentChatContent.setText("No messages yet");
                        recentChatTime.setText("");
                    }

                    HBox topRow = new HBox(conversationNameLabel, recentChatTime);
                    HBox.setHgrow(conversationNameLabel, Priority.ALWAYS);
                    topRow.setAlignment(Pos.CENTER_LEFT);
                    topRow.setSpacing(8);

                    VBox conversationContainer = new VBox(topRow, recentChatContent);
                    conversationContainer.setSpacing(4);
                    conversationContainer.setPadding(new Insets(6));

                    conversationContainer.maxWidthProperty().bind(conversationsList.widthProperty().subtract(10));
                    conversationNameLabel.maxWidthProperty().bind(conversationContainer.maxWidthProperty().subtract(recentChatTime.getWidth()));
                    recentChatContent.maxWidthProperty().bind(conversationContainer.maxWidthProperty());

                    setGraphic(conversationContainer);
                }
            }
        });
    }

    public Button getCreateConversationButton () { return createConversationButton; }
    public Button getSendMessageButton () { return sendMessageButton; }
    public Button getFilterToggleButton () { return filterToggleButton; }
    public Button getLogoutButton () { return logoutButton; }
    public Button getSettingsButton () { return settingsButton; }
    public BooleanProperty isFilteringUsersProperty() { return isFilteringUsers; }
    public TextField getActiveConversationsFilter() { return activeConversationsFilter; }
    public ListView<Conversation> getConversationsList () { return conversationsList; }
    public ListView<ChatMessage> getCurrMessagesList () { return currMessagesList; }
    public Label getCurrConversationLabel () { return currConversationLabel; }
    public TextArea getMessageTextArea () { return messageTextArea; }

}
