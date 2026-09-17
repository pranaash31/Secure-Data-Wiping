package com.sanitizer.gui.views;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.engine.WipeMetrics;
import com.sanitizer.gui.components.SectorHeatmapComponent;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.util.SoundManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class BatchWipeView {

    private final VBox rootContainer = new VBox(18);
    private VBox driveQueueContainer;
    private Label statusBadge;
    private Label queueStatusLabel;
    private Label poolCapacityBadge;

    private ComboBox<WipeEngine.WipeStandard> cmbGlobalStandard;
    private Button btnRunAll;
    private Button btnStopAll;
    private Button btnAudioToggle;

    // Ambient Live Telemetry Dashboard Labels
    private Label lblTotalSpeed;
    private Label lblSpeedSub;
    private Label lblConcurrency;
    private Label lblConcurrencySub;
    private Label lblDataVolume;
    private Label lblVolumeSub;
    private Label lblMasterEta;
    private Label lblEtaSub;

    // Map of active futures by drive system path for granular abort control
    private final Map<String, Future<Boolean>> activeTasks = new ConcurrentHashMap<>();

    // Map of per-drive UI controllers to update live metrics cleanly
    private final Map<String, DriveCardController> driveControllers = new ConcurrentHashMap<>();

    // Real-time telemetry snapshot per drive
    private final Map<String, WipeMetrics> liveMetricsMap = new ConcurrentHashMap<>();

    public BatchWipeView() {
        buildUi();
        refreshQueue();
        UsbDetector.registerListener(drives -> Platform.runLater(this::refreshQueue));
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(24, 28, 28, 28));

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Parallel Multi-Drive Simultaneous Batch Wiping Engine");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Real-time dynamic sector matrix visualizer with ambient aggregate telemetry & granular safety abort");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        poolCapacityBadge = new Label(WipeEngine.getThreadPoolCapacity() + " CONCURRENT SLOTS");
        poolCapacityBadge.getStyleClass().add("badge-info");
        poolCapacityBadge.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");

        statusBadge = new Label("0 TARGET DRIVES");
        statusBadge.getStyleClass().add("badge-warning");
        statusBadge.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");

        header.getChildren().addAll(titleBox, spacer, poolCapacityBadge, statusBadge);

        // ── Master Control Bar ──────────────────────────────────────────
        HBox controlBar = new HBox(14);
        controlBar.getStyleClass().add("card");
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(16, 20, 16, 20));

        Button btnRefresh = new Button("🔄 Scan Hardware");
        btnRefresh.getStyleClass().add("button-primary");
        btnRefresh.setOnAction(e -> refreshQueue());

        Label lblStandard = new Label("Global Standard:");
        lblStandard.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        cmbGlobalStandard = new ComboBox<>();
        cmbGlobalStandard.getItems().addAll(WipeEngine.WipeStandard.DOD_5220_22_M, WipeEngine.WipeStandard.NIST_800_88_CLEAR);
        cmbGlobalStandard.setValue(WipeEngine.WipeStandard.DOD_5220_22_M);
        cmbGlobalStandard.setStyle("-fx-font-size: 12px; -fx-pref-width: 200px;");

        btnRunAll = new Button("▶ Run All Queued Wipes");
        btnRunAll.getStyleClass().add("button-success");
        btnRunAll.setOnAction(e -> handleRunAllWipes());

        btnStopAll = new Button("⏹ Emergency Stop All");
        btnStopAll.getStyleClass().add("button-danger");
        btnStopAll.setOnAction(e -> handleEmergencyStopAll());

        btnAudioToggle = new Button(SoundManager.isMuted() ? "🔇 Sound: OFF" : "🔊 Sound: ON");
        btnAudioToggle.getStyleClass().add("button-secondary");
        btnAudioToggle.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnAudioToggle.setOnAction(e -> {
            boolean isMuted = SoundManager.toggleMute();
            btnAudioToggle.setText(isMuted ? "🔇 Sound: OFF" : "🔊 Sound: ON");
        });

        queueStatusLabel = new Label("Queue Status: Scanning hardware...");
        queueStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        Region ctrlSpacer = new Region();
        HBox.setHgrow(ctrlSpacer, Priority.ALWAYS);

        controlBar.getChildren().addAll(btnRefresh, lblStandard, cmbGlobalStandard, btnRunAll, btnStopAll, btnAudioToggle, ctrlSpacer, queueStatusLabel);

        // ── Ambient Live Telemetry Dashboard (4 Metric Cards) ─────────────────
        HBox telemetryDashboard = createAmbientTelemetryDashboard();

        // ── Drive Cards Queue Grid ─────────────────────────────────────────────
        HBox queueHeader = new HBox(12);
        queueHeader.setAlignment(Pos.CENTER_LEFT);

        Label queueLabel = new Label("DETECTED STORAGE TARGET QUEUE (LIVE SECTOR MATRIX VISUALIZERS)");
        queueLabel.getStyleClass().add("sidebar-section-label");

        Region qSpacer = new Region();
        HBox.setHgrow(qSpacer, Priority.ALWAYS);

        Label liveHint = new Label("🔴 Raw Data  |  🟡 Active Write Head  |  🔵 Pattern Fill  |  🟢 Verified Zeroed");
        liveHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-font-weight: bold;");

        queueHeader.getChildren().addAll(queueLabel, qSpacer, liveHint);

        driveQueueContainer = new VBox(14);

        ScrollPane scrollPane = new ScrollPane(driveQueueContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(header, controlBar, telemetryDashboard, queueHeader, scrollPane);
    }

    private HBox createAmbientTelemetryDashboard() {
        HBox dashboard = new HBox(14);
        dashboard.setAlignment(Pos.CENTER);

        // Card 1: Total Batch Throughput
        VBox cardSpeed = createTelemetryCard("⚡ AGGREGATE BANDWIDTH", "0.0 MB/s", "#2563EB", "#EFF6FF", "Idle | 0 active stream(s)");
        lblTotalSpeed = (Label) cardSpeed.getChildren().get(1);
        lblSpeedSub = (Label) cardSpeed.getChildren().get(2);

        // Card 2: Worker Pool Concurrency
        VBox cardConcurrency = createTelemetryCard("🔄 THREAD POOL UTILIZATION", "0 / " + WipeEngine.getThreadPoolCapacity() + " ACTIVE", "#059669", "#ECFDF5", "Ready for concurrent dispatch");
        lblConcurrency = (Label) cardConcurrency.getChildren().get(1);
        lblConcurrencySub = (Label) cardConcurrency.getChildren().get(2);

        // Card 3: Cumulative Batch Volume
        VBox cardVolume = createTelemetryCard("💾 SESSION DATA VOLUME", "0.0 GB Processed", "#7C3AED", "#F5F3FF", "0.0% of batch queue");
        lblDataVolume = (Label) cardVolume.getChildren().get(1);
        lblVolumeSub = (Label) cardVolume.getChildren().get(2);

        // Card 4: Master Batch ETA
        VBox cardEta = createTelemetryCard("⏱ MASTER BATCH ETA", "--:--", "#D97706", "#FFFBEB", "All threads synchronized");
        lblMasterEta = (Label) cardEta.getChildren().get(1);
        lblEtaSub = (Label) cardEta.getChildren().get(2);

        dashboard.getChildren().addAll(cardSpeed, cardConcurrency, cardVolume, cardEta);
        return dashboard;
    }

    private VBox createTelemetryCard(String headerText, String initialVal, String textColor, String bgColor, String initialSub) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 16, 12, 16));
        HBox.setHgrow(card, Priority.ALWAYS);

        Label title = new Label(headerText);
        title.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        Label value = new Label(initialVal);
        value.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        Label sub = new Label(initialSub);
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        card.getChildren().addAll(title, value, sub);
        return card;
    }

    private void refreshQueue() {
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();

        // Check for premature disconnection while a wipe is active
        Set<String> connectedPaths = new HashSet<>();
        for (UsbDetector.UsbDriveInfo d : drives) {
            connectedPaths.add(d.systemPath());
        }

        for (String activePath : new HashSet<>(activeTasks.keySet())) {
            if (!connectedPaths.contains(activePath)) {
                // Drive pulled out prematurely mid-sanitization!
                SoundManager.playAlertSound();
                WipeEngine.cancelWipeTask(activePath);
                activeTasks.remove(activePath);
                liveMetricsMap.remove(activePath);

                DriveCardController controller = driveControllers.get(activePath);
                if (controller != null) {
                    controller.handleAbortLocal("Premature Physical Disconnect");
                }

                NavigationManager.getInstance().showNotification(
                        "CRITICAL: DRIVE DISCONNECTED MID-WIPE",
                        "Target device (" + activePath + ") was disconnected prematurely during active sector sanitization!",
                        ToastNotification.ToastType.ERROR
                );
            }
        }

        driveQueueContainer.getChildren().clear();
        driveControllers.clear();

        statusBadge.setText(drives.size() + " DRIVES DETECTED");
        statusBadge.getStyleClass().setAll(drives.isEmpty() ? "badge-warning" : "badge-success");
        updateSummaryStatus();

        if (drives.isEmpty()) {
            btnRunAll.setDisable(true);
            btnStopAll.setDisable(true);

            VBox emptyCard = new VBox(16);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(40));

            Label emptyTitle = new Label("No Target USB Storage Drives Connected");
            emptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

            Label emptySub = new Label("Plug in USB storage devices or pen drives. The batch manager will automatically allocate concurrent worker threads.");
            emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

            emptyCard.getChildren().addAll(emptyTitle, emptySub);
            driveQueueContainer.getChildren().add(emptyCard);
            updateAmbientDashboard();
            return;
        }

        btnRunAll.setDisable(false);
        btnStopAll.setDisable(activeTasks.isEmpty());

        int index = 1;
        for (UsbDetector.UsbDriveInfo drive : drives) {
            DriveCardController controller = new DriveCardController(index++, drive);
            driveControllers.put(drive.systemPath(), controller);
            driveQueueContainer.getChildren().add(controller.cardRoot);
        }
        updateAmbientDashboard();
    }

    private void updateSummaryStatus() {
        int total = driveControllers.size();
        int active = activeTasks.size();
        int completed = AuditDb.getAllRecords().size();
        queueStatusLabel.setText(String.format("Queue: %d total  |  %d active wiping  |  %d completed records", total, active, completed));
        btnStopAll.setDisable(activeTasks.isEmpty());
    }

    /**
     * Recalculates ambient aggregate bandwidth, session data volume, pool utilization, and master ETA.
     */
    private void updateAmbientDashboard() {
        int activeCount = activeTasks.size();
        int poolCap = WipeEngine.getThreadPoolCapacity();

        double totalSpeedMBs = 0.0;
        long totalBytesProcessed = 0;
        long maxEtaSeconds = 0;

        for (WipeMetrics m : liveMetricsMap.values()) {
            totalSpeedMBs += Math.max(0.0, m.speedMBs());
            totalBytesProcessed += m.bytesProcessedInPass();
            if (m.etaSeconds() > maxEtaSeconds) {
                maxEtaSeconds = m.etaSeconds();
            }
        }

        long totalQueueSizeBytes = 0;
        for (DriveCardController c : driveControllers.values()) {
            totalQueueSizeBytes += c.drive.sizeBytes();
        }

        // 1. Bandwidth Counter
        if (lblTotalSpeed != null) {
            if (activeCount > 0) {
                lblTotalSpeed.setText(String.format("%.1f MB/s", totalSpeedMBs));
                lblSpeedSub.setText(String.format("⚡ %d active stream(s) writing", activeCount));
            } else {
                lblTotalSpeed.setText("0.0 MB/s");
                lblSpeedSub.setText("Idle | 0 active streams");
            }
        }

        // 2. Concurrency Utilization
        if (lblConcurrency != null) {
            int pct = (int) Math.round(((double) activeCount / poolCap) * 100);
            lblConcurrency.setText(activeCount + " / " + poolCap + " SLOTS");
            lblConcurrencySub.setText(pct > 0 ? pct + "% Concurrency Utilization" : "Pool ready for dispatch");
        }

        // 3. Volume
        if (lblDataVolume != null) {
            double processedGb = totalBytesProcessed / (1024.0 * 1024.0 * 1024.0);
            double totalGb = totalQueueSizeBytes / (1024.0 * 1024.0 * 1024.0);
            double volPct = totalGb > 0 ? (processedGb / totalGb) * 100 : 0.0;
            lblDataVolume.setText(String.format("%.2f GB / %.1f GB", processedGb, totalGb));
            lblVolumeSub.setText(String.format("%.1f%% of queued storage", volPct));
        }

        // 4. Master ETA
        if (lblMasterEta != null) {
            if (activeCount > 0 && maxEtaSeconds > 0) {
                long mins = maxEtaSeconds / 60;
                long secs = maxEtaSeconds % 60;
                lblMasterEta.setText(mins > 0 ? String.format("%02dm %02ds", mins, secs) : String.format("%02ds", secs));
                lblEtaSub.setText("Estimated batch completion");
            } else if (activeCount > 0) {
                lblMasterEta.setText("Calculating...");
                lblEtaSub.setText("Synchronizing write streams");
            } else {
                lblMasterEta.setText("--:--");
                lblEtaSub.setText("All worker threads idle");
            }
        }
    }

    private void handleRunAllWipes() {
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        if (drives.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("BATCH DATA SANITIZATION CONFIRMATION");
        confirm.setHeaderText("CONCURRENT PERMANENT MEDIA SANITIZATION");
        confirm.setContentText(String.format(
                "You are about to launch concurrent multi-threaded sanitization on %d target drive(s) simultaneously using %s.\n\nAre you sure you want to proceed?",
                drives.size(), cmbGlobalStandard.getValue().name()
        ));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        SoundManager.playStartTone();
        NavigationManager.getInstance().showNotification("Batch Sanitization Dispatched",
                "Spawning parallel wiping threads for " + drives.size() + " drives.", ToastNotification.ToastType.INFO);

        btnRunAll.setDisable(true);
        btnStopAll.setDisable(false);

        for (DriveCardController controller : driveControllers.values()) {
            if (!activeTasks.containsKey(controller.drive.systemPath())) {
                controller.cmbStandard.setValue(cmbGlobalStandard.getValue());
                controller.startWipe();
            }
        }
    }

    private void handleEmergencyStopAll() {
        if (activeTasks.isEmpty()) return;

        SoundManager.playAbortTone();
        int stopped = activeTasks.size();
        for (String path : new HashSet<>(activeTasks.keySet())) {
            WipeEngine.cancelWipeTask(path);
            DriveCardController controller = driveControllers.get(path);
            if (controller != null) {
                controller.handleAbortLocal("Emergency Stop triggered");
            }
        }
        activeTasks.clear();
        liveMetricsMap.clear();

        btnRunAll.setDisable(false);
        btnStopAll.setDisable(true);

        NavigationManager.getInstance().showNotification("EMERGENCY STOP EXECUTED",
                "Successfully halted " + stopped + " active background wipe operation(s).", ToastNotification.ToastType.WARNING);
        updateSummaryStatus();
        updateAmbientDashboard();
    }

    /**
     * Inner controller managing state, real-time metrics, live sector heatmap, and granular abort for an individual drive card.
     */
    private class DriveCardController {
        private final int index;
        private final UsbDetector.UsbDriveInfo drive;
        private final VBox cardRoot;

        private final Label statusBadge;
        private final Label passBadge;
        private final Label speedLabel;
        private final Label etaLabel;
        private final Label pctLabel;
        private final ProgressBar progressBar;
        private final SectorHeatmapComponent heatmap;
        private final ComboBox<WipeEngine.WipeStandard> cmbStandard;
        private final Button btnStart;
        private final Button btnAbort;
        private final Button btnInspect;

        private WipeMetrics lastMetrics;

        public DriveCardController(int index, UsbDetector.UsbDriveInfo drive) {
            this.index = index;
            this.drive = drive;

            cardRoot = new VBox(12);
            cardRoot.getStyleClass().add("card");
            cardRoot.setPadding(new Insets(16, 20, 16, 20));

            // Top Row: Index + Drive Info + Status Badges
            HBox topRow = new HBox(14);
            topRow.setAlignment(Pos.CENTER_LEFT);

            Label indexLabel = new Label(String.valueOf(index));
            indexLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                    "-fx-background-color: #EFF6FF; -fx-background-radius: 50%; -fx-min-width: 32px; " +
                    "-fx-min-height: 32px; -fx-alignment: CENTER;");

            VBox infoBox = new VBox(3);
            Label nameLabel = new Label(drive.model());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

            Label specsLabel = new Label("Serial: " + drive.serial() + "  |  Capacity: " + drive.formattedSize() + "  |  Path: " + drive.systemPath());
            specsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            infoBox.getChildren().addAll(nameLabel, specsLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            passBadge = new Label("Ready");
            passBadge.setStyle("-fx-font-size: 11px; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-weight: bold;");

            statusBadge = new Label("QUEUED");
            statusBadge.getStyleClass().add("badge-info");
            statusBadge.setMinWidth(90);
            statusBadge.setAlignment(Pos.CENTER);

            topRow.getChildren().addAll(indexLabel, infoBox, passBadge, statusBadge);

            // Middle Row: Live Real-Time Telemetry Metrics
            HBox metricsRow = new HBox(20);
            metricsRow.setAlignment(Pos.CENTER_LEFT);

            speedLabel = new Label("⚡ Speed: -- MB/s");
            speedLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-background-color: #EFF6FF; -fx-padding: 3 8; -fx-background-radius: 4px;");

            etaLabel = new Label("⏱ ETA: --");
            etaLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #D97706; -fx-background-color: #FEF3C7; -fx-padding: 3 8; -fx-background-radius: 4px;");

            pctLabel = new Label("0.0% Completed");
            pctLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");

            Region mSpacer = new Region();
            HBox.setHgrow(mSpacer, Priority.ALWAYS);

            cmbStandard = new ComboBox<>();
            cmbStandard.getItems().addAll(WipeEngine.WipeStandard.DOD_5220_22_M, WipeEngine.WipeStandard.NIST_800_88_CLEAR);
            cmbStandard.setValue(WipeEngine.WipeStandard.DOD_5220_22_M);
            cmbStandard.setStyle("-fx-font-size: 11px; -fx-pref-width: 170px;");

            metricsRow.getChildren().addAll(speedLabel, etaLabel, pctLabel, mSpacer, cmbStandard);

            // Progress Bar Row
            progressBar = new ProgressBar(0);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(8);

            // Real-Time Interactive Sector Heatmap / Matrix Grid (48 blocks)
            heatmap = new SectorHeatmapComponent(48, 12, 14, 3, 3);
            heatmap.reset(drive.sizeBytes());

            // Bottom Row: Actions (Start vs Granular Abort + Deep Matrix Inspection)
            HBox actionRow = new HBox(10);
            actionRow.setAlignment(Pos.CENTER_RIGHT);

            btnInspect = new Button("🔍 Deep Inspect Matrix");
            btnInspect.getStyleClass().add("button-secondary");
            btnInspect.setStyle("-fx-padding: 6 12; -fx-font-size: 11px;");
            btnInspect.setOnAction(e -> openDetailedInspectionModal());

            Region actSpacer = new Region();
            HBox.setHgrow(actSpacer, Priority.ALWAYS);

            btnStart = new Button("▶ Start Wipe");
            btnStart.getStyleClass().add("button-primary");
            btnStart.setStyle("-fx-padding: 6 16; -fx-font-size: 11px;");
            btnStart.setOnAction(e -> {
                SoundManager.playStartTone();
                startWipe();
            });

            btnAbort = new Button("⏹ Granular Abort");
            btnAbort.getStyleClass().add("button-danger");
            btnAbort.setStyle("-fx-padding: 6 16; -fx-font-size: 11px;");
            btnAbort.setVisible(false);
            btnAbort.setManaged(false);
            btnAbort.setOnAction(e -> handleGranularAbort());

            actionRow.getChildren().addAll(btnInspect, actSpacer, btnStart, btnAbort);

            cardRoot.getChildren().addAll(topRow, metricsRow, progressBar, heatmap, actionRow);
        }

        public void startWipe() {
            if (activeTasks.containsKey(drive.systemPath())) return;

            btnStart.setVisible(false);
            btnStart.setManaged(false);
            btnAbort.setVisible(true);
            btnAbort.setManaged(true);
            btnAbort.setDisable(false);
            cmbStandard.setDisable(true);

            statusBadge.setText("WIPING");
            statusBadge.getStyleClass().setAll("badge-warning");
            passBadge.setText("Initializing Pass...");
            passBadge.setStyle("-fx-font-size: 11px; -fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-weight: bold;");

            heatmap.reset(drive.sizeBytes());

            WipeEngine.WipeStandard selectedStd = cmbStandard.getValue();

            Future<Boolean> future = WipeEngine.submitBatchWipeTaskWithMetrics(
                    drive.systemPath(),
                    drive.sizeBytes(),
                    selectedStd,
                    true, // Fast mode enabled for safe queue testing demo
                    metrics -> Platform.runLater(() -> updateMetricsUi(metrics)),
                    null,
                    success -> Platform.runLater(() -> handleCompletion(success, selectedStd))
            );

            activeTasks.put(drive.systemPath(), future);
            updateSummaryStatus();
            updateAmbientDashboard();
        }

        private void updateMetricsUi(WipeMetrics metrics) {
            this.lastMetrics = metrics;
            liveMetricsMap.put(metrics.systemPath(), metrics);

            progressBar.setProgress(metrics.overallPercent() / 100.0);
            pctLabel.setText(metrics.formattedProgress() + " Completed");
            speedLabel.setText("⚡ Speed: " + metrics.formattedSpeed());
            etaLabel.setText("⏱ ETA: " + metrics.formattedEta());
            passBadge.setText(metrics.formattedPassSummary());
            heatmap.updateProgress(metrics);

            updateAmbientDashboard();
        }

        private void handleGranularAbort() {
            btnAbort.setDisable(true);
            SoundManager.playAbortTone();
            boolean cancelled = WipeEngine.cancelWipeTask(drive.systemPath());
            activeTasks.remove(drive.systemPath());
            liveMetricsMap.remove(drive.systemPath());

            handleAbortLocal("User Abort Action");

            NavigationManager.getInstance().showNotification(
                    "Granular Abort Executed",
                    drive.model() + " (" + drive.systemPath() + ") sanitization halted. Other active wipes remain running.",
                    ToastNotification.ToastType.WARNING
            );
            updateSummaryStatus();
            updateAmbientDashboard();
        }

        public void handleAbortLocal(String reason) {
            statusBadge.setText("ABORTED");
            statusBadge.getStyleClass().setAll("badge-danger");
            passBadge.setText("Aborted: " + reason);
            passBadge.setStyle("-fx-font-size: 11px; -fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-weight: bold;");
            speedLabel.setText("⚡ Speed: 0.0 MB/s");
            etaLabel.setText("⏱ ETA: --");
            heatmap.setAborted();

            btnAbort.setVisible(false);
            btnAbort.setManaged(false);
            btnStart.setVisible(true);
            btnStart.setManaged(true);
            btnStart.setText("🔄 Retry Wipe");
            btnStart.setDisable(false);
            cmbStandard.setDisable(false);
        }

        private void handleCompletion(boolean success, WipeEngine.WipeStandard standard) {
            activeTasks.remove(drive.systemPath());
            liveMetricsMap.remove(drive.systemPath());

            if (success) {
                SoundManager.playSuccessChime();
                progressBar.setProgress(1.0);
                pctLabel.setText("100.0% — Pass Verified");
                speedLabel.setText("⚡ Speed: Done");
                etaLabel.setText("⏱ ETA: 00:00");
                passBadge.setText("Sanitization Certified");
                passBadge.setStyle("-fx-font-size: 11px; -fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-weight: bold;");

                statusBadge.setText("COMPLETED");
                statusBadge.getStyleClass().setAll("badge-success");
                heatmap.setCompleted();

                String payload = drive.model() + "|" + drive.serial() + "|" + drive.formattedSize() + "|" + standard.name() + "|SUCCESS";
                String sig = CryptoSigner.signData(payload);
                AuditDb.saveRecord(drive.model(), drive.serial(), drive.formattedSize(), standard.name(), "SUCCESS", sig);

                List<AuditDb.AuditRecord> recs = AuditDb.getAllRecords();
                if (!recs.isEmpty()) {
                    CertificateGenerator.generateCertificate(recs.get(0));
                }

                NavigationManager.getInstance().showNotification("Drive Sanitized",
                        drive.model() + " successfully sanitized & certified.", ToastNotification.ToastType.SUCCESS);
            } else {
                SoundManager.playAlertSound();
                statusBadge.setText("FAILED");
                statusBadge.getStyleClass().setAll("badge-danger");
                passBadge.setText("Wipe Failed");
                passBadge.setStyle("-fx-font-size: 11px; -fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-weight: bold;");
                heatmap.setAborted();

                NavigationManager.getInstance().showNotification("Wipe Failed",
                        drive.model() + " sanitization failed.", ToastNotification.ToastType.ERROR);
            }

            btnAbort.setVisible(false);
            btnAbort.setManaged(false);
            btnStart.setVisible(true);
            btnStart.setManaged(true);
            btnStart.setText("Start Wipe");
            btnStart.setDisable(false);
            cmbStandard.setDisable(false);

            updateSummaryStatus();
            updateAmbientDashboard();
            if (activeTasks.isEmpty()) {
                btnRunAll.setDisable(false);
            }
        }

        private void openDetailedInspectionModal() {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Deep Sector Inspection — " + drive.model());

            VBox modalRoot = new VBox(16);
            modalRoot.setPadding(new Insets(24));
            modalRoot.setStyle("-fx-background-color: #0F172A;");

            Label title = new Label("High-Density LBA Sector Inspection Matrix: " + drive.model());
            title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

            Label meta = new Label("Device: " + drive.systemPath() + "  |  Serial: " + drive.serial() + "  |  Size: " + drive.formattedSize());
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

            // Large 160-block inspector matrix
            SectorHeatmapComponent detailedHeatmap = new SectorHeatmapComponent(160, 15, 18, 4, 4);
            detailedHeatmap.reset(drive.sizeBytes());
            if (lastMetrics != null) {
                detailedHeatmap.updateProgress(lastMetrics);
            } else if ("COMPLETED".equals(statusBadge.getText())) {
                detailedHeatmap.setCompleted();
            }

            Button btnClose = new Button("Close Inspector");
            btnClose.getStyleClass().add("button-primary");
            btnClose.setOnAction(e -> dialog.close());

            HBox btnBox = new HBox(btnClose);
            btnBox.setAlignment(Pos.CENTER_RIGHT);

            modalRoot.getChildren().addAll(title, meta, detailedHeatmap, btnBox);

            Scene scene = new Scene(modalRoot, 680, 480);
            dialog.setScene(scene);
            dialog.show();
        }
    }
}
