package com.github.Iks31.messagingapp.client;


import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class ClientApp extends Application {
    private static ClientNetworking clientNetworking = new ClientNetworking();
    @Override
    public void start(Stage stage) {
        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clientNetworking.connect("localhost", 9999);
                return null;
            }
        };

        connectTask.setOnSucceeded(e -> {
            stage.setScene(new StartMenu().getScene(stage));
            stage.setTitle("JeSMS Messaging App");
            stage.show();
        });

        connectTask.setOnFailed(e -> {
            Throwable ex = connectTask.getException();
            showErrorDialog("Could not connect to server: " + ex.getMessage());
        });

        new Thread(connectTask).start();
    }

    public static ClientNetworking getClientNetworking() {
        return clientNetworking;
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        clientNetworking.close();
    }

    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Connection Error");
        alert.setHeaderText("Server Connection Problem");
        alert.setContentText(message);
        alert.showAndWait();
    }

}