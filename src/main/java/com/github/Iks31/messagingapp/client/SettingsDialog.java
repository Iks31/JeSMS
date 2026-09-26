package com.github.Iks31.messagingapp.client;

import com.github.Iks31.messagingapp.client.ui_components.TextButton;
import com.github.Iks31.messagingapp.common.Protocol;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

// Account settings, currently allows the user to change their password
public class SettingsDialog extends Stage {
    private final PasswordField currentPasswordField = new PasswordField();
    private final PasswordField newPasswordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();
    private final Label statusLabel = new Label();
    private final TextButton changeButton = new TextButton("Change Password", "button-primary");

    public SettingsDialog(String username) {
        // Configures stage
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Settings");

        Label header = new Label("Account");
        header.getStyleClass().add("header");
        Label loggedInAs = new Label("Logged in as " + username);

        // Password form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Current password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm new password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        statusLabel.setWrapText(true);
        changeButton.setDefaultButton(true);
        changeButton.setOnAction(e -> submit());

        // Root component
        VBox layout = new VBox(12, header, loggedInAs, new Label("Change password"), grid, statusLabel, changeButton);
        layout.setPadding(new Insets(16));

        Scene scene = new Scene(layout, 420, 300);
        scene.getStylesheets().add("style.css");
        setScene(scene);
    }

    private void submit() {
        String newPassword = newPasswordField.getText();
        String error = Protocol.validatePassword(newPassword);
        if (currentPasswordField.getText().isEmpty()) {
            error = "Enter your current password.";
        } else if (error == null && !newPassword.equals(confirmPasswordField.getText())) {
            error = "New passwords do not match.";
        }
        if (error != null) {
            statusLabel.setText(error);
            return;
        }
        statusLabel.setText("Changing password...");
        changeButton.setDisable(true);
        ClientApp.getClientNetworking().changePasswordRequest(currentPasswordField.getText(), newPassword);
    }

    // Called by the controller with the server's response
    public void showResult(boolean success, String message) {
        changeButton.setDisable(false);
        statusLabel.setText(message);
        if (success) {
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        }
    }
}
