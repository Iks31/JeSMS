package com.github.Iks31.messagingapp.client;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegistrationScreen implements UI {
    private final Label statusLabel = new Label();
    // Changes needed: scope of UI components to object variables for access within methods
    @Override
    public Scene getScene (Stage stage) {
        VBox vbox = new VBox(10, new Label("Registration "));
        Label usernameLabel = new Label("Please Enter A Username: ");
        TextField usernameField = new TextField();
        Label passwordLabel = new Label("Please Enter A Password: ");
        TextField passwordField = new PasswordField();
        Label confrimPasswordLabel = new Label("Please Confirm Your Password: ");
        TextField confirmPasswordField = new PasswordField();

        TextButton submitBtn = new TextButton("Submit", "button-primary");
        submitBtn.setOnAction(e -> verifyRegistration(usernameField.getText(), passwordField.getText(), confirmPasswordField.getText()));
        ClearButton clrBtn = new ClearButton(usernameField, passwordField, confirmPasswordField);

        BackButton backBtn = new BackButton(stage, new StartMenu().getScene(stage));
        vbox.getChildren().addAll(usernameLabel, usernameField, passwordLabel, passwordField,
                confrimPasswordLabel, confirmPasswordField, statusLabel, submitBtn, clrBtn, backBtn);
        vbox.setAlignment(Pos.CENTER);

        ClientApp.getClientNetworking().setMessageHandler(message -> {
            if ("REGISTER_SUCCESS".equals(message.getFlag())) {
                Platform.runLater(() -> {
                    statusLabel.setText("Registration Successful");
                    clrBtn.fire();
                });
            } else if ("REGISTER_FAILURE".equals(message.getFlag())) {
                Platform.runLater(() -> statusLabel.setText((String) message.getContent()));
            }
        });

        Scene scene = new Scene(vbox, 600, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void verifyRegistration(String username, String password, String confirmedPassword) {
        statusLabel.setText("");
        if (username.length() < 5 || username.length() > 15 || password.length() < 5 || password.length() > 15) {
            statusLabel.setText("Invalid Username/Password");
        } else if (!password.equals(confirmedPassword)) {
            statusLabel.setText("Passwords do not match");
        } else {
            statusLabel.setText("Registration Successful");
            ClientApp.getClientNetworking().registrationRequest(username, password);
        }

    }
}
