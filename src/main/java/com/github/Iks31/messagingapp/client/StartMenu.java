package com.github.Iks31.messagingapp.client;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StartMenu implements UI {
    @Override
    public Scene getScene(Stage stage) {
        VBox vbox = new VBox(10, new Label("Welcome to JeSMS!"));

        TextButton loginBtn = new TextButton("Login", "button-primary");
        loginBtn.setOnAction(e -> {stage.setScene(new LoginScreen().getScene(stage));});

        TextButton registerBtn = new TextButton("Register", "button-primary");
        registerBtn.setOnAction(e -> {stage.setScene(new RegistrationScreen().getScene(stage));});

        TextButton aboutBtn = new TextButton("About", "button-primary");
        aboutBtn.setOnAction(e -> {stage.setScene(new AboutScreen().getScene(stage));});

        Label serverConnectionStatus = new Label("");

        vbox.getChildren().addAll(loginBtn, registerBtn, aboutBtn, serverConnectionStatus);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add("style.css");

        ClientApp.getClientNetworking().setMessageHandler(message -> {
            if ("INIT_SUCCESS".equals(message.getFlag())) {
                Platform.runLater(() -> {serverConnectionStatus.setText((String) message.getContent());});
            }
        });


        return scene;
    }
}
