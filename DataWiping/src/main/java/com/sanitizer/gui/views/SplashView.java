package com.sanitizer.gui.views;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class SplashView extends StackPane {

    private Runnable onFinished;
    private ProgressBar loadingBar;

    public SplashView(Runnable onFinished) {
        this.onFinished = onFinished;
        getStyleClass().add("splash-root");
        buildUI();
        startAnimation();
    }

    private void buildUI() {
        // Background particle circles
        for (int i = 0; i < 6; i++) {
            Circle particle = new Circle(2 + i * 1.5);
            particle.setFill(Color.rgb(37, 99, 235, 0.08 + i * 0.02));
            particle.setTranslateX(-250 + i * 100);
            particle.setTranslateY(-180 + i * 70);
            getChildren().add(particle);
        }

        // Main content
        VBox content = new VBox(32);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(500);
        content.setPadding(new Insets(0));

        // Logo / Brand card
        VBox logoBox = new VBox(14);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.getStyleClass().add("splash-logo-box");

        // Shield icon with text
        HBox iconRow = new HBox(14);
        iconRow.setAlignment(Pos.CENTER);
        Label shieldIcon = new Label("[S]");
        shieldIcon.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #3B82F6; " +
                "-fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 12px; -fx-padding: 8 14;");
        Label productName = new Label("SecureErase Pro");
        productName.getStyleClass().add("splash-product-name");
        iconRow.getChildren().addAll(shieldIcon, productName);

        Label tagline = new Label("Enterprise-Grade Data Sanitization Suite");
        tagline.getStyleClass().add("splash-tagline");

        Label version = new Label("v2.0.0 ENTERPRISE");
        version.getStyleClass().add("splash-version");

        logoBox.getChildren().addAll(iconRow, tagline, version);

        // Compliance badges
        HBox badges = new HBox(12);
        badges.setAlignment(Pos.CENTER);
        for (String standard : new String[]{"NIST 800-88", "DoD 5220.22-M", "FIPS 140-2", "ISO 27001"}) {
            Label badge = new Label(standard);
            badge.getStyleClass().add("badge-info");
            badges.getChildren().add(badge);
        }

        // Loading section
        VBox loadingSection = new VBox(10);
        loadingSection.setAlignment(Pos.CENTER);
        Label loadingLabel = new Label("Initializing secure environment...");
        loadingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-font-weight: bold;");
        loadingBar = new ProgressBar(0);
        loadingBar.getStyleClass().add("splash-loading-bar");
        loadingBar.setPrefWidth(400);
        loadingBar.setPrefHeight(6);
        loadingSection.getChildren().addAll(loadingLabel, loadingBar);

        // Footer
        Label footer = new Label("Trusted by Government and Defense Organizations Worldwide  |  (c) 2025 SecureErase Technologies");
        footer.getStyleClass().add("splash-footer-text");
        footer.setWrapText(true);
        footer.setAlignment(Pos.CENTER);

        content.getChildren().addAll(logoBox, badges, loadingSection, footer);

        StackPane.setAlignment(content, Pos.CENTER);
        getChildren().add(content);
    }

    private void startAnimation() {
        // Animate loading bar from 0 to 1 over 3 seconds
        Timeline loadTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingBar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(3.2), new KeyValue(loadingBar.progressProperty(), 1, Interpolator.EASE_BOTH))
        );

        // Fade in the whole splash
        setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        loadTimeline.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(600), this);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> onFinished.run());
            fadeOut.play();
        });

        loadTimeline.play();
    }
}
