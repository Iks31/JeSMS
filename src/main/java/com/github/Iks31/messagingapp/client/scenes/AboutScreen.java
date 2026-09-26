package com.github.Iks31.messagingapp.client.scenes;

import com.github.Iks31.messagingapp.client.UI;
import com.github.Iks31.messagingapp.client.ui_components.BackButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// Basic UI that gives information about the messaging app
public class AboutScreen implements UI {
    @Override
    public Scene getScene (Stage stage) {
        Label aboutInfo = new Label("This is a simple messaging app developed by Iker Davis-Zamorano and Christian Gleitzman. Register, login and message your friends! Start direct or group chats, edit or delete your messages, see when your messages have been read, and manage group members.");
        aboutInfo.setWrapText(true);
        Label titleLabel = new Label("About");
        titleLabel.getStyleClass().add("header");
        VBox vbox = new VBox(10, titleLabel, aboutInfo);
        vbox.setPadding(new Insets(20));
        vbox.setSpacing(10);
        vbox.setAlignment(Pos.CENTER);

        BackButton backBtn = new BackButton(stage, new StartMenu().getScene(stage));

        BorderPane layout = new BorderPane();
        layout.setCenter(vbox);
        layout.setBottom(backBtn);

        Scene scene = new Scene(layout, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add("style.css");
        return scene;
    }
}
