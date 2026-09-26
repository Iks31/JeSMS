package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.ClientApp;
import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.TextButton;
import com.github.Iks31.messagingapp.common.Protocol;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StartMenu implements UI {
    @Override
    public Scene getScene(Stage stage) {
        Label titleLabel = new Label("Welcome to JeSMS!");
        titleLabel.getStyleClass().add("header");
        VBox vbox = new VBox(10, titleLabel);

        // Initial navigation buttons
        TextButton loginBtn = new TextButton("Login", "button-primary");
        loginBtn.setOnAction(e -> {stage.setScene(new LoginScreen().getScene(stage));});
        TextButton registerBtn = new TextButton("Register", "button-primary");
        registerBtn.setOnAction(e -> {stage.setScene(new RegistrationScreen().getScene(stage));});
        TextButton aboutBtn = new TextButton("About", "button-primary");
        aboutBtn.setOnAction(e -> {stage.setScene(new AboutScreen().getScene(stage));});

        // Displays server connection status
        String welcome = ClientApp.getClientNetworking().getServerWelcome();
        Label serverConnectionStatus = new Label(welcome == null ? "" : welcome);

        // Root component
        vbox.getChildren().addAll(loginBtn, registerBtn, aboutBtn, serverConnectionStatus);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add("style.css");

        // Handles initial connection to the server
        ClientApp.getClientNetworking().setMessageHandler(message -> {
            if (Protocol.INIT_SUCCESS.equals(message.getFlag())) {
                serverConnectionStatus.setText((String) message.getContent());
            }
        });


        return scene;
    }
}
