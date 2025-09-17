package com.github.Iks31.messagingapp.client.ui_components;

import javafx.scene.control.TextField;

// A button that clears all the given text fields provided in the constructors parameters
public class ClearButton extends TextButton {
    public ClearButton(TextField... textFields) {
        super("Clear", "button-secondary");
        this.setOnAction(e -> {
            for (TextField textField : textFields) {
                textField.setText("");
            }
        });
    }
}
