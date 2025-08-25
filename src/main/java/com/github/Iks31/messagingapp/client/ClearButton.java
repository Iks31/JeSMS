package com.github.Iks31.messagingapp.client;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;

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
