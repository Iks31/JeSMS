package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.client.ui_components.IconButton;
import com.github.Iks31.messagingapp.common.Conversation;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.github.Iks31.messagingapp.client.ui_components.TextButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CreateConversationDialog extends Stage {
    private ObservableList<Conversation> currentConversations;
    private final ObservableList<String> chatMembers = FXCollections.observableArrayList();
    private final ListView<String> membersList = new ListView<>(chatMembers);
    private final TextField userField = new TextField();
    private final TextField chatNameField = new TextField();
    private final Label statusLabel = new Label();
    private final Button addUserButton = new IconButton("/images/plus.png");
    private final Button undoButton = new IconButton("/images/undo.png");
    private final TextButton createButton = new TextButton("Create", "button-primary");

    private String conversationName = "";
    private ArrayList<String> conversationUsers;

    public CreateConversationDialog(ObservableList<Conversation> currentConversations) {
        this.currentConversations = currentConversations;

        initModality(Modality.APPLICATION_MODAL);
        setTitle("Create New Conversation");

        userField.setPromptText("Enter username");
        chatNameField.setPromptText("Enter chat name");
        chatNameField.visibleProperty().bind(Bindings.size(chatMembers).greaterThan(1));
        chatNameField.managedProperty().bind(chatNameField.visibleProperty());

        addUserButton.setOnAction(e -> addUser());
        undoButton.setOnAction(e -> undo());

        membersList.setPrefHeight(100);

        createButton.setOnAction(e -> validateAndCreate());

        VBox layout = new VBox(10,
                new Label("Add users:"),
                new HBox(5, userField, addUserButton, undoButton),
                membersList,
                chatNameField,
                statusLabel,
                createButton
        );
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 300, 300);
        scene.getStylesheets().add("style.css");
        setScene(scene);
    }

    private void validateAndCreate() {
        // Prevents chat without any members
        if (chatMembers.isEmpty()) {
            statusLabel.setText("Add at least one user.");
            return;
        }

        // Always include the current user
        String currentUser = ClientApp.getClientNetworking().getUsername();
        Set<String> users = new HashSet<>(chatMembers);
        users.add(currentUser);

        // Prevents self-chat
        if (users.size() == 1) {
            statusLabel.setText("You cannot create a conversation with only yourself.");
            return;
        }

        // Require a chat name if group (more than 2 users including self)
        if (users.size() > 2 && chatNameField.getText().trim().isEmpty()) {
            statusLabel.setText("Enter a chat name for group chats.");
            return;
        }

        // Check against existing conversations (order-insensitive)
        for (Conversation conversation : currentConversations) {
            Set<String> existingUsers = new HashSet<>(conversation.users);
            if (users.equals(existingUsers)) {
                statusLabel.setText("You are already in a conversation with these members.");
                return;
            }
        }

        conversationName = chatMembers.size() > 1 ? chatNameField.getText().trim() : "";
        conversationUsers = new ArrayList<>(users);

        close();
    }
    private void addUser() {
        String user = userField.getText().trim();
        if (user.isEmpty()) {
            statusLabel.setText("Enter a username.");
        } else if (user.equals(ClientApp.getClientNetworking().getUsername())) {
            statusLabel.setText("You do not need to add yourself.");
        } else if (chatMembers.contains(user)) {
            statusLabel.setText("User already added.");
        } else {
            chatMembers.add(user);
            userField.clear();
            statusLabel.setText("");
        }
    }

    private void undo() {
        if (chatMembers.isEmpty()) {
            return;
        } else {
            chatMembers.removeLast();
        }
    }

    public String getConversationName() {
        return conversationName;
    }

    public ArrayList<String> getConversationUsers() {
        return conversationUsers;
    }
}
