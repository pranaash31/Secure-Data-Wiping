package com.sanitizer.gui.components;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class ToastNotification {

    public enum ToastType {
        SUCCESS("#10B981", "✓"),
        INFO("#2563EB", "ℹ"),
        WARNING("#F59E0B", "⚠"),
        ERROR("#EF4444", "✕");

        public final String colorHex;
        public final String iconSymbol;

        ToastType(String colorHex, String iconSymbol) {
            this.colorHex = colorHex;
            this.iconSymbol = iconSymbol;
        }
    }

    public static void show(Window ownerWindow, String title, String message, ToastType type) {
        if (ownerWindow == null || !ownerWindow.isShowing()) return;

        Platform.runLater(() -> {
            Popup popup = new Popup();
            popup.setAutoHide(true);

            HBox toastContainer = new HBox(12);
            toastContainer.setAlignment(Pos.CENTER_LEFT);
            toastContainer.setPadding(new Insets(12, 18, 12, 18));
            toastContainer.setStyle(String.format(
                    "-fx-background-color: #0F172A; -fx-background-radius: 10px; " +
                    "-fx-border-color: %s; -fx-border-radius: 10px; -fx-border-width: 1.5px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.35), 12, 0, 0, 4);",
                    type.colorHex
            ));

            Label iconLabel = new Label(type.iconSymbol);
            iconLabel.setStyle(String.format(
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: %s; " +
                    "-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 50%%; " +
                    "-fx-min-width: 28px; -fx-min-height: 28px; -fx-alignment: CENTER;",
                    type.colorHex
            ));

            VBox textContainer = new VBox(2);
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

            Label msgLabel = new Label(message);
            msgLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(300);

            textContainer.getChildren().addAll(titleLabel, msgLabel);
            toastContainer.getChildren().addAll(iconLabel, textContainer);

            popup.getContent().add(toastContainer);

            double x = ownerWindow.getX() + ownerWindow.getWidth() - 360;
            double y = ownerWindow.getY() + 60;
            popup.show(ownerWindow, x, y);

            toastContainer.setOpacity(0);
            toastContainer.setTranslateY(-10);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastContainer);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition transIn = new TranslateTransition(Duration.millis(300), toastContainer);
            transIn.setFromY(-10);
            transIn.setToY(0);

            ParallelTransition showAnim = new ParallelTransition(fadeIn, transIn);
            showAnim.play();

            Thread dismissThread = new Thread(() -> {
                try {
                    Thread.sleep(3500);
                } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(400), toastContainer);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> popup.hide());
                    fadeOut.play();
                });
            });
            dismissThread.setDaemon(true);
            dismissThread.start();
        });
    }
}
