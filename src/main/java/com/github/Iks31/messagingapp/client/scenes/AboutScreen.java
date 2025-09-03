package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.BackButton;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AboutScreen implements UI {
    @Override
    public Scene getScene (Stage stage) {
        Label aboutInfo = new Label("This is a simple messaging app developed by Iker Davis-Zamorano and Christian Gleitzman. Register, login and message your friends!");
        aboutInfo.setWrapText(true);
        VBox vbox = new VBox(10, new Label("About"), aboutInfo);
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);

        BackButton backBtn = new BackButton(stage, new StartMenu().getScene(stage));;

        BorderPane layout = new BorderPane();
        layout.setCenter(vbox);
        layout.setBottom(backBtn);

        Scene scene = new Scene(layout, 600, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }
}
