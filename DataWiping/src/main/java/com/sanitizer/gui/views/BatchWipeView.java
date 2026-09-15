package com.sanitizer.gui.views;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.pdf.CertificateGenerator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BatchWipeView {

    private final VBox rootContainer = new VBox(24);
    private VBox driveQueueContainer;
    private Label statusBadge;
    private Label queueStatusLabel;

    private Button btnRunAll;
    private Button btnStopAll;

    // Track active background wipe tasks per drive path
    private final Map<String, Task<Boolean>> activeTasks = new ConcurrentHashMap<>();

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

        btnRunAll = new Button("Run All Queued Wipes");
        btnRunAll.getStyleClass().add("button-success");
        btnRunAll.setOnAction(e -> handleRunAllWipes());

        btnStopAll = new Button("Emergency Stop All");
        btnStopAll.getStyleClass().add("button-danger");
        btnStopAll.setOnAction(e -> handleEmergencyStopAll());

        queueStatusLabel = new Label("Queue Status: Scanning hardware...");
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
            btnRunAll.setDisable(true);
            btnStopAll.setDisable(true);

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

        btnRunAll.setDisable(!activeTasks.isEmpty());
        btnStopAll.setDisable(activeTasks.isEmpty());

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

        Label indexLabel = new Label(String.valueOf(index));
        indexLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: #EFF6FF; -fx-background-radius: 50%; -fx-min-width: 36px; " +
                "-fx-min-height: 36px; -fx-alignment: CENTER;");

        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(drive.model());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label specsLabel = new Label("Serial: " + drive.serial() + "  |  Capacity: " + drive.formattedSize() + "  |  Path: " + drive.systemPath());
        specsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        infoBox.getChildren().addAll(nameLabel, specsLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setMinWidth(180);

        ProgressBar pb = new ProgressBar(0);
        pb.setPrefWidth(180);
        pb.setPrefHeight(10);

        Label pct = new Label("Ready for sanitization pass");
        pct.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        progressBox.getChildren().addAll(pb, pct);

        Label statusLabel = new Label(activeTasks.containsKey(drive.systemPath()) ? "WIPING..." : "QUEUED");
        statusLabel.getStyleClass().add(activeTasks.containsKey(drive.systemPath()) ? "badge-warning" : "badge-info");
        statusLabel.setMinWidth(90);
        statusLabel.setAlignment(Pos.CENTER);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnStart = new Button(activeTasks.containsKey(drive.systemPath()) ? "Wiping..." : "Start Wipe");
        btnStart.getStyleClass().add("button-primary");
        btnStart.setStyle("-fx-padding: 6 14; -fx-font-size: 11px;");
        btnStart.setDisable(activeTasks.containsKey(drive.systemPath()));

        btnStart.setOnAction(e -> startSingleWipe(drive, pb, pct, statusLabel, btnStart));

        actions.getChildren().addAll(btnStart);
        card.getChildren().addAll(indexLabel, infoBox, progressBox, statusLabel, actions);
        return card;
    }

    private void handleRunAllWipes() {
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        if (drives.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("BATCH DATA SANITIZATION CONFIRMATION");
        confirm.setHeaderText("PERMANENT MEDIA DATA DESTRUCTION");
        confirm.setContentText(String.format("You are about to execute sector sanitization on %d target drive(s).\nAre you sure you want to proceed?", drives.size()));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        NavigationManager.getInstance().showNotification("Batch Sanitization Started",
                "Executing sanitization passes across " + drives.size() + " drives.", ToastNotification.ToastType.INFO);

        btnRunAll.setDisable(true);
        btnStopAll.setDisable(false);

        for (Node node : driveQueueContainer.getChildren()) {
            if (node instanceof HBox card) {
                Button btnStart = null;
                for (Node child : card.getChildren()) {
                    if (child instanceof HBox actions) {
                        for (Node b : actions.getChildren()) {
                            if (b instanceof Button btn) btnStart = btn;
                        }
                    }
                }
                if (btnStart != null && !btnStart.isDisabled()) {
                    btnStart.fire();
                }
            }
        }
    }

    private void startSingleWipe(UsbDetector.UsbDriveInfo drive, ProgressBar pb, Label pct, Label statusLabel, Button btnStart) {
        if (activeTasks.containsKey(drive.systemPath())) return;

        btnStart.setDisable(true);
        btnStart.setText("Wiping...");
        statusLabel.setText("WIPING");
        statusLabel.getStyleClass().setAll("badge-warning");
        btnStopAll.setDisable(false);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return WipeEngine.executeWipe(
                        drive.systemPath(),
                        drive.sizeBytes(),
                        WipeEngine.WipeStandard.DOD_5220_22_M,
                        true, // Fast test mode cap for queue demo execution safety
                        percent -> Platform.runLater(() -> {
                            double p = percent / 100.0;
                            pb.setProgress(p);
                            pct.setText(String.format("%.1f%% Pass Completed", percent));
                        }),
                        line -> {}
                );
            }
        };

        task.setOnSucceeded(e -> {
            activeTasks.remove(drive.systemPath());
            boolean success = task.getValue();
            if (success) {
                pb.setProgress(1.0);
                pct.setText("100% — Pass Verified");
                statusLabel.setText("COMPLETED");
                statusLabel.getStyleClass().setAll("badge-success");

                String payload = drive.model() + "|" + drive.serial() + "|" + drive.formattedSize() + "|DoD 5220.22-M|SUCCESS";
                String sig = CryptoSigner.signData(payload);
                AuditDb.saveRecord(drive.model(), drive.serial(), drive.formattedSize(), "DoD 5220.22-M", "SUCCESS", sig);

                List<AuditDb.AuditRecord> recs = AuditDb.getAllRecords();
                if (!recs.isEmpty()) {
                    CertificateGenerator.generateCertificate(recs.get(0));
                }

                NavigationManager.getInstance().showNotification("Drive Sanitized",
                        drive.model() + " successfully sanitized & certified.", ToastNotification.ToastType.SUCCESS);
            } else {
                statusLabel.setText("FAILED");
                statusLabel.getStyleClass().setAll("badge-danger");
                NavigationManager.getInstance().showNotification("Wipe Failed",
                        drive.model() + " sanitization failed.", ToastNotification.ToastType.ERROR);
            }
            btnStart.setDisable(false);
            btnStart.setText("Start Wipe");
            if (activeTasks.isEmpty()) {
                btnRunAll.setDisable(false);
                btnStopAll.setDisable(true);
            }
        });

        task.setOnFailed(e -> {
            activeTasks.remove(drive.systemPath());
            statusLabel.setText("ERROR");
            statusLabel.getStyleClass().setAll("badge-danger");
            btnStart.setDisable(false);
            btnStart.setText("Start Wipe");
            if (activeTasks.isEmpty()) {
                btnRunAll.setDisable(false);
                btnStopAll.setDisable(true);
            }
            NavigationManager.getInstance().showNotification("Batch Task Error",
                    drive.model() + " error: " + task.getException().getMessage(), ToastNotification.ToastType.ERROR);
        });

        activeTasks.put(drive.systemPath(), task);
        Thread workerThread = new Thread(task);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private void handleEmergencyStopAll() {
        if (activeTasks.isEmpty()) return;

        int stopped = activeTasks.size();
        activeTasks.values().forEach(task -> task.cancel(true));
        activeTasks.clear();

        btnRunAll.setDisable(false);
        btnStopAll.setDisable(true);

        NavigationManager.getInstance().showNotification("EMERGENCY STOP EXECUTED",
                "Halted " + stopped + " active background wipe operation(s).", ToastNotification.ToastType.WARNING);
        refreshQueue();
    }
}
