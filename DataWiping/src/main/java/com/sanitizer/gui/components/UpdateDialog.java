package com.sanitizer.gui.components;

import com.sanitizer.update.UpdateInfo;
import com.sanitizer.update.UpdateManager;
import com.sanitizer.util.AppLogger;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;

/**
 * Enterprise In-App Update Dialog.
 * Shows release details, changelog, download progress, and launches native installer.
 */
public class UpdateDialog {

    private static final String MODULE = "UpdateDialog";

    public static void show(Stage owner, UpdateInfo updateInfo) {
        if (updateInfo == null) return;

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("USB Sanitizer Update Available");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle(
                "-fx-background-color: #0f172a; " +
                "-fx-border-color: #38bdf8; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
        );
        root.setPrefWidth(540);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("🔄");
        iconLabel.setStyle("-fx-font-size: 28px;");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("Software Update Available");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label subLabel = new Label("A new high-assurance release of USB Sanitizer is ready.");
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(titleLabel, subLabel);

        header.getChildren().addAll(iconLabel, titleBox);

        // Version Comparison Box
        GridPane versionGrid = new GridPane();
        versionGrid.setHgap(16);
        versionGrid.setVgap(6);
        versionGrid.setPadding(new Insets(12));
        versionGrid.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px;");

        Label curVerKey = new Label("Installed Version:");
        curVerKey.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label curVerVal = new Label("v" + UpdateManager.getInstance().getCurrentVersion());
        curVerVal.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label newVerKey = new Label("Latest Available:");
        newVerKey.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label newVerVal = new Label("v" + updateInfo.getVersion() + (updateInfo.isPrerelease() ? " (Pre-release)" : " (Certified)"));
        newVerVal.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label assetKey = new Label("Package Target:");
        assetKey.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label assetVal = new Label(updateInfo.getAssetName().isEmpty() ? "Native Installer" : updateInfo.getAssetName());
        assetVal.setStyle("-fx-text-fill: #a7f3d0; -fx-font-size: 12px;");

        versionGrid.add(curVerKey, 0, 0);
        versionGrid.add(curVerVal, 1, 0);
        versionGrid.add(newVerKey, 0, 1);
        versionGrid.add(newVerVal, 1, 1);
        versionGrid.add(assetKey, 0, 2);
        versionGrid.add(assetVal, 1, 2);

        // Changelog / Release Notes
        Label notesHeader = new Label("Release Notes:");
        notesHeader.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 12px;");

        TextArea notesArea = new TextArea(updateInfo.getChangelog().isBlank() ? "Security patches and performance enhancements." : updateInfo.getChangelog());
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(6);
        notesArea.setStyle(
                "-fx-control-inner-background: #1e293b; " +
                "-fx-text-fill: #e2e8f0; " +
                "-fx-font-family: 'Monaco', 'Courier New', monospace; " +
                "-fx-font-size: 11px; " +
                "-fx-border-color: #334155; " +
                "-fx-border-radius: 6px;"
        );

        // Progress Bar (hidden until download starts)
        VBox progressBox = new VBox(6);
        progressBox.setVisible(false);
        progressBox.setManaged(false);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #38bdf8;");

        Label progressStatus = new Label("Downloading update...");
        progressStatus.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
        progressBox.getChildren().addAll(progressStatus, progressBar);

        // Actions
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button laterBtn = new Button("Remind Me Later");
        laterBtn.setStyle(
                "-fx-background-color: #334155; " +
                "-fx-text-fill: #f8fafc; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 8 16; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand;"
        );
        laterBtn.setOnAction(e -> dialog.close());

        Button updateBtn = new Button("Download & Install Update");
        updateBtn.setStyle(
                "-fx-background-color: #0284c7; " +
                "-fx-text-fill: #ffffff; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 8 18; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand;"
        );

        updateBtn.setOnAction(e -> {
            updateBtn.setDisable(true);
            laterBtn.setDisable(true);
            progressBox.setVisible(true);
            progressBox.setManaged(true);

            new Thread(() -> {
                try {
                    File file = UpdateManager.getInstance().downloadAsset(updateInfo, (bytes, total, percent) -> {
                        Platform.runLater(() -> {
                            progressBar.setProgress(percent / 100.0);
                            progressStatus.setText(String.format("Downloading: %.1f%% (%d MB / %d MB)",
                                    percent, bytes / (1024 * 1024), total / (1024 * 1024)));
                        });
                    });

                    Platform.runLater(() -> {
                        progressStatus.setText("Download complete. Launching installer...");
                        boolean launched = UpdateManager.getInstance().launchInstaller(file);
                        if (launched) {
                            dialog.close();
                        } else {
                            progressStatus.setText("Downloaded to: " + file.getAbsolutePath());
                            laterBtn.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    AppLogger.error(MODULE, "Download failed: " + ex.getMessage(), ex);
                    Platform.runLater(() -> {
                        progressStatus.setText("Download failed: " + ex.getMessage());
                        progressStatus.setStyle("-fx-text-fill: #ef4444;");
                        updateBtn.setDisable(false);
                        laterBtn.setDisable(false);
                    });
                }
            }).start();
        });

        buttonBar.getChildren().addAll(laterBtn, updateBtn);

        root.getChildren().addAll(header, versionGrid, notesHeader, notesArea, progressBox, buttonBar);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();
    }
}
