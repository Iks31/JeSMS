package com.github.Iks31.messagingapp.client;

import javafx.scene.Scene;
import javafx.stage.Stage;

public interface UI {
    public static Integer DEFAULT_WIDTH = 900;
    public static Integer DEFAULT_HEIGHT = 600;
    public static Integer MIN_WIDTH = 640;
    public static Integer MIN_HEIGHT = 420;
    Scene getScene(Stage stage);
}
