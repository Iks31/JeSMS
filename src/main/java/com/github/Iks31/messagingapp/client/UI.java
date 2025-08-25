package com.github.Iks31.messagingapp.client;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface UI {
    public static Integer DEFAULT_WIDTH = 600;
    public static Integer DEFAULT_HEIGHT = 400;
    Scene getScene(Stage stage);
}
