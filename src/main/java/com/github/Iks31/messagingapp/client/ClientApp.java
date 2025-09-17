package com.github.Iks31.messagingapp.client;


import com.github.Iks31.messagingapp.client.scenes.StartMenu;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


/**
 * JavaFX App
 */
public class ClientApp extends Application {
    private static ClientNetworking clientNetworking = new ClientNetworking();
    @Override
    public void start(Stage stage) {
        // Initial connection task
        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clientNetworking.connect("localhost", 9999);
                return null;
            }
        };
        // Shows window and start menu on successful connection
        connectTask.setOnSucceeded(e -> {
            stage.setScene(new StartMenu().getScene(stage));
            stage.setTitle("JeSMS Messaging App");
            stage.show();
        });
        // Shows an error dialog if the server cannot be connected to
        connectTask.setOnFailed(e -> {
            Throwable ex = connectTask.getException();
            showErrorDialog(Alert.AlertType.ERROR,"Connection Error", "Server Connection Problem","Could not connect to server: " + ex.getMessage());
        });

        new Thread(connectTask).start();
    }

    public static ClientNetworking getClientNetworking() {
        return clientNetworking;
    }

    public static void main(String[] args) {
        launch();
    }

    // Ensures the client disconnects if the window is closed
    @Override
    public void stop() throws Exception {
        super.stop();

        clientNetworking.close();
    }

    // Method for displaying an error popup message
    public static void showErrorDialog(Alert.AlertType type, String title, String headerText, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.showAndWait();
    }

}