package com.github.Iks31.messagingapp.client;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DefaultScene extends Scene {
    public DefaultScene(Parent root, Integer width, Integer height) {
        super(root, width, height);
        root.getStyleClass().add("root");
        root.getStyleClass().add("emerald");
        this.getStylesheets().add("style.css");
    }
}
