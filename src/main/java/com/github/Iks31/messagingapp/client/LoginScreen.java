package com.github.Iks31.messagingapp.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
    // Changes needed: scope of UI components to object variables for access within methods
    @Override
    public Scene getScene(Stage stage) {
        Label titleLabel = new Label("Login");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Please Enter Your Username: ");
        TextField usernameField = new TextField();
        Label passwordLabel = new Label("Please Enter Your Password: ");
        TextField passwordField = new PasswordField();


        grid.add(usernameLabel, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(passwordLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(statusLabel, 0, 2, 2, 1);

        TextButton submitBtn = new TextButton("Submit", "button-primary");
        //submitBtn.disableProperty().bind(); Refer to bindings lecture slides
        submitBtn.setOnAction(e -> loginVerification(usernameField.getText(), passwordField.getText()));
        ClearButton clrBtn = new ClearButton(usernameField, passwordField);
        HBox btnBox = new HBox(15, submitBtn, clrBtn);
        btnBox.setAlignment(Pos.CENTER);

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
            if ("LOGIN_SUCCESS".equals(msg.getFlag())) {
                Platform.runLater(() -> showJeSMS(stage));
            } else if ("LOGIN_FAILURE".equals(msg.getFlag())) {
                Platform.runLater(() -> updateStatus((String) msg.getContent()));
            }
        });

        // Creating and returning the scene
        Scene scene = new Scene(layout, 600, 400);
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
        JeSMSView view = new JeSMSView();
        new JeSMSController(view);
        stage.setScene(view.getScene(stage));
    }
}
