package com.github.Iks31.messagingapp.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("JeSMS Messaging App");
        // Start Menu Object initialised and passed to stage
        stage.setScene(new StartMenu().getScene(stage));
        stage.show();
    }

    public static void main(String[] args) {
        // Connect client application to external server
        launch();
    }

}