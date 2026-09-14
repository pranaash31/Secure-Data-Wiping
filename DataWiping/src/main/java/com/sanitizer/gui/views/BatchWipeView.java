package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class BatchWipeView {

    private final VBox rootContainer = new VBox(24);
    private VBox driveQueueContainer;
    private Label statusBadge;
    private Label queueStatusLabel;

    public BatchWipeView() {
        buildUi();
        refreshQueue();
        UsbDetector.registerListener(drives -> refreshQueue());
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Batch Wipe Queue Manager");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Manage parallel multi-drive sanitization operations from a single control panel");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusBadge = new Label("0 TARGET DRIVES");
        statusBadge.getStyleClass().add("badge-warning");

        header.getChildren().addAll(titleBox, spacer, statusBadge);

        // ── Master Control Bar ──────────────────────────────────────────
        HBox controlBar = new HBox(12);
        controlBar.getStyleClass().add("card");
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(16, 20, 16, 20));

        Button btnRefresh = new Button("🔄 Scan Target Hardware");
        btnRefresh.getStyleClass().add("button-primary");
        btnRefresh.setOnAction(e -> refreshQueue());

        Button btnRunAll = new Button("Run All Queued Wipes");
        btnRunAll.getStyleClass().add("button-success");

        Button btnStopAll = new Button("Emergency Stop All");
        btnStopAll.getStyleClass().add("button-danger");

        queueStatusLabel = new Label("Queue Status:  Scanning hardware...");
        queueStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);

        controlBar.getChildren().addAll(btnRefresh, btnRunAll, btnStopAll, ctrlSpacer, queueStatusLabel);

        // ── Drive Cards Grid ─────────────────────────────────────────────
        Label queueLabel = new Label("DETECTED TARGET HARDWARE QUEUE");
        queueLabel.getStyleClass().add("sidebar-section-label");

        driveQueueContainer = new VBox(14);

        ScrollPane scrollPane = new ScrollPane(driveQueueContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(header, controlBar, queueLabel, scrollPane);
    }

    private void refreshQueue() {
        driveQueueContainer.getChildren().clear();
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        int completedCount = AuditDb.getAllRecords().size();

        statusBadge.setText(drives.size() + " DRIVES DETECTED");
        statusBadge.getStyleClass().setAll(drives.isEmpty() ? "badge-warning" : "badge-success");
        queueStatusLabel.setText(String.format("Queue Status: %d active target drives  |  %d completed operations in audit log", drives.size(), completedCount));

        if (drives.isEmpty()) {
            VBox emptyCard = new VBox(16);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));

            Label emptyTitle = new Label("No Target USB Storage Drives Connected");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

            Label emptySub = new Label("Plug in USB storage devices or pen drives. The batch manager will automatically detect and queue them.");
            emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

            emptyCard.getChildren().addAll(emptyTitle, emptySub);
            driveQueueContainer.getChildren().add(emptyCard);
            return;
        }

        int index = 1;
        for (UsbDetector.UsbDriveInfo drive : drives) {
            driveQueueContainer.getChildren().add(createDriveCard(index++, drive));
        }
    }

    private HBox createDriveCard(int index, UsbDetector.UsbDriveInfo drive) {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));

        // Index circle
        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: #EFF6FF; -fx-background-radius: 50%; -fx-min-width: 36px; " +
                "-fx-min-height: 36px; -fx-alignment: CENTER;");

        // Drive info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(drive.model());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label specsLabel = new Label("Serial: " + drive.serial() + "  |  Capacity: " + drive.formattedSize() + "  |  Path: " + drive.systemPath());
        specsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        infoBox.getChildren().addAll(nameLabel, specsLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Progress bar
        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinWidth(180);

        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(180);
        pb.setPrefHeight(10);

        Label pct = new Label("Ready for sanitization pass");
        pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        progressBox.getChildren().addAll(pb, pct);

        // Status badge
        Label statusLabel = new Label("QUEUED");
        statusLabel.getStyleClass().add("badge-info");
        statusLabel.setMinWidth(90);
        statusLabel.setAlignment(Pos.CENTER);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnStart = new Button("Start Wipe");
        btnStart.getStyleClass().add("button-primary");
        btnStart.setStyle("-fx-padding: 6 14; -fx-font-size: 11px;");

        actions.getChildren().addAll(btnStart);

        card.getChildren().addAll(indexLabel, infoBox, progressBox, statusLabel, actions);
        return card;
    }
}
