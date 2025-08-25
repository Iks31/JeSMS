package com.github.Iks31.messagingapp.client;

import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class TextButton extends Button {
    public TextButton(String text, String style) {
        super(text);
        this.getStyleClass().add(style);

        this.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), this);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });

        this.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), this);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }
}
