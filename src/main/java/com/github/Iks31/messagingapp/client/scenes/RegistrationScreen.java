package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.ClientApp;
import com.github.Iks31.messagingapp.client.ui_components.TextButton;
import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.BackButton;
import com.github.Iks31.messagingapp.client.ui_components.ClearButton;
import com.github.Iks31.messagingapp.common.Protocol;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegistrationScreen implements UI {
    private final Label statusLabel = new Label();

    @Override
    public Scene getScene (Stage stage) {
        Label titleLabel = new Label("Registration");
        titleLabel.getStyleClass().add("header");

        // Registration form components
        TextField usernameField = new TextField();
        usernameField.setPromptText(Protocol.MIN_USERNAME_LENGTH + "-" + Protocol.MAX_USERNAME_LENGTH + " letters, numbers or _");
        TextField passwordField = new PasswordField();
        passwordField.setPromptText("At least " + Protocol.MIN_PASSWORD_LENGTH + " characters");
        TextField confirmPasswordField = new PasswordField();

        // Layout of components in grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.add(new Label("Please Enter A Username: "), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Please Enter A Password: "), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Please Confirm Your Password: "), 0, 2);
        grid.add(confirmPasswordField, 1, 2);
        statusLabel.setWrapText(true);
        grid.add(statusLabel, 0, 3, 2, 1);

        // UI buttons
        TextButton submitBtn = new TextButton("Submit", "button-primary");
        submitBtn.setOnAction(e -> validateRegistration(usernameField.getText().trim(), passwordField.getText(), confirmPasswordField.getText()));
        submitBtn.setDefaultButton(true);
        ClearButton clrBtn = new ClearButton(usernameField, passwordField, confirmPasswordField);
        HBox btnBox = new HBox(15, submitBtn, clrBtn);
        btnBox.setAlignment(Pos.CENTER);
        BackButton backBtn = new BackButton(stage, new StartMenu().getScene(stage));

        // Root container
        VBox centerBox = new VBox(20, titleLabel, grid, btnBox);
        centerBox.setAlignment(Pos.CENTER);
        BorderPane layout = new BorderPane();
        layout.setCenter(centerBox);
        layout.setBottom(backBtn);

        // Handling server responses - success or failure
        ClientApp.getClientNetworking().setMessageHandler(message -> {
            if (Protocol.REGISTER_SUCCESS.equals(message.getFlag())) {
                clrBtn.fire();
                statusLabel.setText((String) message.getContent());
            } else if (Protocol.REGISTER_FAIL.equals(message.getFlag()) || Protocol.REQUEST_FAIL.equals(message.getFlag())) {
                statusLabel.setText((String) message.getContent());
            }
        });

        Scene scene = new Scene(layout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    // Validation of registration details entered, the server repeats these checks
    private void validateRegistration(String username, String password, String confirmedPassword) {
        String error = Protocol.validateUsername(username);
        if (error == null) error = Protocol.validatePassword(password);
        if (error == null && !password.equals(confirmedPassword)) error = "Passwords do not match";

        if (error != null) {
            statusLabel.setText(error);
        } else {
            statusLabel.setText("Registering...");
            ClientApp.getClientNetworking().registrationRequest(username, password);
        }
    }
}
