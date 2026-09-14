package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class BatchWipeView {

    private final VBox rootContainer = new VBox(24);

    public BatchWipeView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));
        rootContainer.setStyle("-fx-background-color: #0B0F19;");

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

        Label statusBadge = new Label("0 / 3 DRIVES ACTIVE");
        statusBadge.getStyleClass().add("badge-warning");

        header.getChildren().addAll(titleBox, spacer, statusBadge);

        // ── Master Control Bar ──────────────────────────────────────────
        HBox controlBar = new HBox(12);
        controlBar.getStyleClass().add("card");
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(16, 20, 16, 20));

        Button btnRunAll = new Button("Run All Queued Wipes");
        btnRunAll.getStyleClass().add("button-primary");
        Button btnStopAll = new Button("Emergency Stop All");
        btnStopAll.getStyleClass().add("button-danger");
        Button btnAddDrive = new Button("+ Add Drive to Queue");
        btnAddDrive.getStyleClass().add("button-success");
        Button btnExport = new Button("Export Batch Report");

        Label queueStatus = new Label("Queue Status:  3 drives pending  |  0 in progress  |  2 completed");
        queueStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);

        controlBar.getChildren().addAll(btnAddDrive, btnRunAll, btnStopAll, btnExport, ctrlSpacer, queueStatus);

        // ── Drive Cards Grid ─────────────────────────────────────────────
        Label queueLabel = new Label("DRIVE QUEUE");
        queueLabel.getStyleClass().add("sidebar-section-label");
        queueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-letter-spacing: 1px;");

        VBox driveQueue = new VBox(14);

        // Drive entry cards
        String[][] drives = {
                {"USB Flash Drive — SanDisk Ultra 64GB", "USB3.0 — 64.0 GB", "DoD 5220.22-M (3-Pass)", "QUEUED", "badge-warning"},
                {"External SSD — Samsung T7 500GB", "USB3.1 — 500.0 GB", "NIST SP 800-88 (1-Pass)", "IN PROGRESS", "badge-info"},
                {"HDD — Seagate Portable 1TB", "USB2.0 — 1,000.0 GB", "Gutmann 35-Pass", "QUEUED", "badge-warning"},
                {"USB Flash Drive — Kingston DataTraveler 32GB", "USB3.0 — 32.0 GB", "NIST SP 800-88 (1-Pass)", "COMPLETED", "badge-success"},
                {"Compact Flash Card — Lexar 16GB", "SD/CF — 16.0 GB", "DoD 5220.22-M (3-Pass)", "COMPLETED", "badge-success"}
        };

        for (int i = 0; i < drives.length; i++) {
            driveQueue.getChildren().add(createDriveCard(i + 1, drives[i][0], drives[i][1], drives[i][2], drives[i][3], drives[i][4]));
        }

        ScrollPane scrollPane = new ScrollPane(driveQueue);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(header, controlBar, queueLabel, scrollPane);
    }

    private HBox createDriveCard(int index, String name, String specs, String standard, String status, String badgeStyle) {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16, 20, 16, 20));

        // Index circle
        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 50%; -fx-min-width: 36px; " +
                "-fx-min-height: 36px; -fx-alignment: CENTER;");

        // Drive info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #E2E8F0;");
        Label specsLabel = new Label(specs + "  |  " + standard);
        specsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        infoBox.getChildren().addAll(nameLabel, specsLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Progress bar (shown only for in-progress)
        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinWidth(180);

        if (status.equals("IN PROGRESS")) {
            ProgressBar pb = new ProgressBar(0.42);
            pb.setPrefWidth(180);
            pb.setPrefHeight(10);
            Label pct = new Label("Pass 2 of 3  —  42%");
            pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #60A5FA; -fx-font-weight: bold;");
            progressBox.getChildren().addAll(pb, pct);
        } else if (status.equals("COMPLETED")) {
            ProgressBar pb = new ProgressBar(1.0);
            pb.setPrefWidth(180);
            pb.setPrefHeight(10);
            pb.setStyle("-fx-accent: #059669;");
            Label pct = new Label("Sanitization Complete");
            pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #34D399; -fx-font-weight: bold;");
            progressBox.getChildren().addAll(pb, pct);
        } else {
            ProgressBar pb = new ProgressBar(0);
            pb.setPrefWidth(180);
            pb.setPrefHeight(10);
            Label pct = new Label("Waiting in queue...");
            pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569;");
            progressBox.getChildren().addAll(pb, pct);
        }

        // Status badge
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add(badgeStyle);
        statusLabel.setMinWidth(100);
        statusLabel.setAlignment(Pos.CENTER);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        Button btnStart = new Button("Start");
        btnStart.getStyleClass().add("button-primary");
        btnStart.setStyle("-fx-padding: 6 14; -fx-font-size: 11px;");
        Button btnRemove = new Button("Remove");
        btnRemove.setStyle("-fx-padding: 6 14; -fx-font-size: 11px;");
        actions.getChildren().addAll(btnStart, btnRemove);

        card.getChildren().addAll(indexLabel, infoBox, progressBox, statusLabel, actions);
        return card;
    }
}
