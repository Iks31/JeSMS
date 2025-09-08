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

    @Override
    public void stop() throws Exception {
        super.stop();

        clientNetworking.close();
    }

    public static void showErrorDialog(Alert.AlertType type, String title, String headerText, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        alert.showAndWait();
    }

}