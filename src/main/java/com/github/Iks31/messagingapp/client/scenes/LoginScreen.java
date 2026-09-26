package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.*;
import com.github.Iks31.messagingapp.client.ui_components.BackButton;
import com.github.Iks31.messagingapp.client.ui_components.ClearButton;
import com.github.Iks31.messagingapp.client.ui_components.TextButton;
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

public class LoginScreen implements UI {
    private final Label statusLabel = new Label("");
    @Override
    public Scene getScene(Stage stage) {
        Label titleLabel = new Label("Login");
        titleLabel.getStyleClass().add("header");

        // Grid layout for UI
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        // Login form components
        Label usernameLabel = new Label("Please Enter Your Username: ");
        TextField usernameField = new TextField();
        Label passwordLabel = new Label("Please Enter Your Password: ");
        TextField passwordField = new PasswordField();

        // Layout of components in grid
        grid.add(usernameLabel, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(statusLabel, 0, 2, 2, 1);

        // UI buttons
        TextButton submitBtn = new TextButton("Submit", "button-primary");
        submitBtn.setOnAction(e -> loginVerification(usernameField.getText().trim(), passwordField.getText()));
        submitBtn.setDefaultButton(true);
        submitBtn.disableProperty().bind(usernameField.textProperty().isEmpty().or(passwordField.textProperty().isEmpty()));
        ClearButton clrBtn = new ClearButton(usernameField, passwordField);
        HBox btnBox = new HBox(15, submitBtn, clrBtn);
        btnBox.setAlignment(Pos.CENTER);

        // Main container for most UI components
        VBox centerBox = new VBox(20, titleLabel, grid, btnBox);
        centerBox.setAlignment(Pos.CENTER);

        // Back Button Box Created
        BackButton backBtn = new BackButton(stage, new StartMenu().getScene(stage));

        // Root Component
        BorderPane layout = new BorderPane();
        layout.setCenter(centerBox);
        layout.setBottom(backBtn);

        // Handling login relevant messages from the server
        ClientApp.getClientNetworking().setMessageHandler(msg -> {
            if (Protocol.LOGIN_SUCCESS.equals(msg.getFlag())) {
                ClientApp.getClientNetworking().setUsername((String) msg.getContent());
                showJeSMS(stage);
            } else if (Protocol.LOGIN_FAIL.equals(msg.getFlag()) || Protocol.REQUEST_FAIL.equals(msg.getFlag())) {
                updateStatus((String) msg.getContent());
                passwordField.clear();
            }
        });

        // Creating and returning the scene
        Scene scene = new Scene(layout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public void loginVerification(String username, String password) {
        updateStatus("Logging in... ");
        ClientApp.getClientNetworking().loginRequest(username, password);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showJeSMS(Stage stage) {
        // Creates main messaging UI on login success
        JeSMSView view = new JeSMSView();
        new JeSMSController(view);
        stage.setScene(view.getScene(stage));
    }
}
