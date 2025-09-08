package com.github.Iks31.messagingapp.client;

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
import java.util.List;

public class CreateConversationDialog extends Stage {
    private final ObservableList<String> chatMembers = FXCollections.observableArrayList();
    private final ListView<String> membersList = new ListView<>(chatMembers);
    private final TextField userField = new TextField();
    private final TextField chatNameField = new TextField();
    private final Label statusLabel = new Label();
    private final TextButton createButton = new TextButton("Create", "button-primary");

    private String conversationName= null;
    private ArrayList<String> conversationUsers;

    public CreateConversationDialog() {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Create New Conversation");

        userField.setPromptText("Enter username");
        chatNameField.setPromptText("Enter chat name");
        chatNameField.setVisible(false);

        Button addUserButton = new Button("+");
        addUserButton.setOnAction(e -> addUser());

        membersList.setPrefHeight(100);

        createButton.setOnAction(e -> validateAndCreate());

        VBox layout = new VBox(10,
                new Label("Add users:"),
                new HBox(5, userField, addUserButton),
                membersList,
                chatNameField,
                statusLabel,
                createButton
        );
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 300, 300);
        setScene(scene);
    }

    private void validateAndCreate() {
        if (chatMembers.isEmpty()) {
            statusLabel.setText("Add at least one user.");
            return;
        }

        if (chatMembers.size() > 1 && chatNameField.getText().trim().isEmpty()) {
            statusLabel.setText("Enter a chat name for group chats.");
            return;
        }

        conversationName = chatMembers.size() > 1 ? chatNameField.getText().trim() : chatMembers.get(0);
        conversationUsers = new ArrayList<>(chatMembers);

        close();
    }
    private void addUser() {
        String user = userField.getText().trim();
        if (user.isEmpty()) {
            statusLabel.setText("Enter a username.");
        } else if (chatMembers.contains(user)) {
            statusLabel.setText("User already added.");
        } else {
            chatMembers.add(user);
            userField.clear();
            statusLabel.setText("");
        }
        chatNameField.setVisible(chatMembers.size() > 1);
    }

    public String getConversationName() {
        return conversationName;
    }

    public ArrayList<String> getConversationUsers() {
        return conversationUsers;
    }
}
