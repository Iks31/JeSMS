package com.github.Iks31.messagingapp.client.ui_components;


import javafx.animation.ScaleTransition;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class IconButton extends Button {
    public IconButton(String iconPath) {
        super();
        ImageView backIcon = new ImageView(new Image(String.valueOf(getClass().getResource(iconPath))));
        backIcon.setFitWidth(30);
        backIcon.setFitHeight(30);
        this.setGraphic(backIcon);
        this.getStyleClass().add("icon-button");

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