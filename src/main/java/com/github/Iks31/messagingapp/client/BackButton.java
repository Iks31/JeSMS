package com.github.Iks31.messagingapp.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class BackButton extends HBox {
    public BackButton(Stage stage, Scene prevScene) {
        super();
        ImageView backIcon = new ImageView(new Image(String.valueOf(getClass().getResource("/images/back.png"))));
        backIcon.setFitWidth(20);
        backIcon.setFitHeight(20);

        Button backBtn = new Button();
        backBtn.setGraphic(backIcon);
        backBtn.getStyleClass().add("back-button");

        backBtn.setOnAction(e -> {stage.setScene(prevScene);});
        this.getChildren().add(backBtn);
        this.setAlignment(Pos.BOTTOM_RIGHT);
        this.setPadding(new Insets(10));
    }
}
