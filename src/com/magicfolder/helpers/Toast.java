package com.magicfolder.helpers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {
    public static void showToast(Stage ownerStage, String message, String rgbColor) {
        Label toastLabel = new Label(message);
        String backgroundColorStr = "-fx-background-color: " + rgbColor + "; ";
        toastLabel.setStyle(backgroundColorStr + "-fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 20;");
        toastLabel.setOpacity(0);

        StackPane root = (StackPane) ownerStage.getScene().getRoot();
        root.getChildren().add(toastLabel);
        StackPane.setAlignment(toastLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastLabel, new Insets(0, 0, 50, 0));

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, fadeOut);
        seq.setOnFinished(e -> root.getChildren().remove(toastLabel));
        seq.play();
    }
}
