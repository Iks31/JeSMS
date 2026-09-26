package com.github.Iks31.messagingapp.client;


import com.github.Iks31.messagingapp.client.scenes.StartMenu;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;


/**
 * JavaFX App
 * Usage: ClientApp [--host HOST] [--port PORT]
 * Environment variables JESMS_HOST and JESMS_PORT are used as defaults.
 */
public class ClientApp extends Application {
    private static final ClientNetworking clientNetworking = new ClientNetworking();

    @Override
    public void start(Stage stage) {
        String host = Objects.requireNonNullElse(System.getenv("JESMS_HOST"), "localhost");
        int port = Integer.parseInt(Objects.requireNonNullElse(System.getenv("JESMS_PORT"), "9999"));
        List<String> args = getParameters().getRaw();
        for (int i = 0; i + 1 < args.size(); i++) {
            switch (args.get(i)) {
                case "--host" -> host = args.get(++i);
                case "--port" -> port = Integer.parseInt(args.get(++i));
            }
        }
        final String serverHost = host;
        final int serverPort = port;

        // Initial connection task
        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                clientNetworking.connect(serverHost, serverPort);
                return null;
            }
        };
        // Shows window and start menu on successful connection
        connectTask.setOnSucceeded(e -> {
            stage.setScene(new StartMenu().getScene(stage));
            stage.setTitle("JeSMS Messaging App");
            stage.setMinWidth(UI.MIN_WIDTH);
            stage.setMinHeight(UI.MIN_HEIGHT);
            stage.show();
        });
        // Shows an error dialog if the server cannot be connected to
        connectTask.setOnFailed(e -> {
            Throwable ex = connectTask.getException();
            showErrorDialog(Alert.AlertType.ERROR, "Connection Error", "Server Connection Problem",
                    "Could not connect to server at " + serverHost + ":" + serverPort + ": " + ex.getMessage());
            Platform.exit();
        });
        // Closes the app if the server goes away after connecting
        clientNetworking.setOnConnectionLost(() -> {
            showErrorDialog(Alert.AlertType.ERROR, "Connection Error", "Server Connection Problem",
                    "The connection to the server was lost. The app will now close.");
            Platform.exit();
        });

        new Thread(connectTask).start();
    }

    public static ClientNetworking getClientNetworking() {
        return clientNetworking;
    }

    public static void main(String[] args) {
        launch(args);
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
