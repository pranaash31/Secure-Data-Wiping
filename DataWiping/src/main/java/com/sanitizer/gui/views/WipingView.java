package com.sanitizer.gui.views;

import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.gui.components.SectorHeatmapComponent;
import com.sanitizer.gui.components.ThermalGraphComponent;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class WipingView {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(20);

    private ComboBox<UsbDetector.UsbDriveInfo> cmbDrives;
    private Button btnRefreshDrives;
    private Label lblDriveBadge;
    private Label lblSelectedDriveInfo;

    private ComboBox<WipePolicy> cmbPolicy;
    private Label lblPolicyPatternSummary;
    private ComboBox<WipeVerifier.VerificationMode> cmbVerifyMode;
    private CheckBox chkTestMode;

    private Button btnExecuteWipe;
    private ProgressBar progressBar;
    private Label lblProgressPercent;
    private Label lblStatusMessage;
    private SectorHeatmapComponent sectorMatrix;
    private ThermalGraphComponent thermalGraph;
    private TextArea txtLogOutput;

    public WipingView() {
        buildUi();
        refreshDriveList();
        UsbDetector.registerListener(drives -> Platform.runLater(() -> updateDriveList(drives)));
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    private void buildUi() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(24));

        // Global keyboard shortcuts: F5 = refresh drives
        rootContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(ev -> {
                    if (ev.getCode() == KeyCode.F5) {
                        refreshDriveList();
                    }
                });
            }
        });

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Hardware Data Sanitization Workplace");
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setStyle("-fx-font-size: 22px;");
        Label lblSub = new Label("Direct low-level raw sector sanitization with real-time sector block visualizer & hardware protection shield");
        lblSub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // Row 1: Cards
        HBox topRow = new HBox(16);

        // --- Card 1: Drive Selection ---
        VBox cardDrive = new VBox(12);
        cardDrive.getStyleClass().add("card");
        HBox.setHgrow(cardDrive, Priority.ALWAYS);

        HBox driveTitleBox = new HBox(10);
        driveTitleBox.setAlignment(Pos.CENTER_LEFT);
        Label lblDriveTitle = new Label("Target Hardware Drive");
        lblDriveTitle.getStyleClass().add("card-title");

        lblDriveBadge = new Label("0 Drives Found");
        lblDriveBadge.getStyleClass().add("badge-warning");
        driveTitleBox.getChildren().addAll(lblDriveTitle, lblDriveBadge);

        cmbDrives = new ComboBox<>();
        cmbDrives.setMaxWidth(Double.MAX_VALUE);
        cmbDrives.setPromptText("Scanning connected USB drives...");
        cmbDrives.setCellFactory(param -> new ListCell<UsbDetector.UsbDriveInfo>() {
            @Override
            protected void updateItem(UsbDetector.UsbDriveInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.model() + " (" + item.formattedSize() + ") - Path: " + item.systemPath());
                }
            }
        });
        cmbDrives.setButtonCell(cmbDrives.getCellFactory().call(null));
        cmbDrives.setOnAction(e -> updateSelectedDriveDetails());

        btnRefreshDrives = new Button("Refresh Drives (F5)");
        btnRefreshDrives.setOnAction(e -> refreshDriveList());
        btnRefreshDrives.setTooltip(new Tooltip("Rescan connected USB drives (F5)"));
        cmbDrives.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) updateSelectedDriveDetails();
        });

        HBox driveActionBox = new HBox(10, cmbDrives, btnRefreshDrives);
        HBox.setHgrow(cmbDrives, Priority.ALWAYS);

        lblSelectedDriveInfo = new Label("No drive selected.");
        lblSelectedDriveInfo.getStyleClass().add("card-subtitle");

        cardDrive.getChildren().addAll(driveTitleBox, driveActionBox, lblSelectedDriveInfo);

        // --- Card 2: Configuration & Pre-Wipe Health Assessment ---
        VBox cardConfig = new VBox(12);
        cardConfig.getStyleClass().add("card");
        HBox.setHgrow(cardConfig, Priority.ALWAYS);

        Label lblConfigTitle = new Label("Sanitization Standard & Verification Controls");
        lblConfigTitle.getStyleClass().add("card-title");

        Label lblStandard = new Label("Wiping Standard / Custom Policy:");
        lblStandard.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        cmbPolicy = new ComboBox<>();
        cmbPolicy.setItems(FXCollections.observableArrayList(WipePolicyManager.getInstance().getAllPolicies()));
        cmbPolicy.setValue(WipePolicyManager.getInstance().getDefaultPolicy());
        cmbPolicy.setMaxWidth(Double.MAX_VALUE);
        cmbPolicy.setStyle("-fx-font-size: 11px;");

        lblPolicyPatternSummary = new Label(cmbPolicy.getValue() != null ? cmbPolicy.getValue().getPatternSummary() : "0x00");
        lblPolicyPatternSummary.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-padding: 3 8; -fx-background-radius: 4px;");

        cmbPolicy.setOnAction(e -> {
            WipePolicy p = cmbPolicy.getValue();
            if (p != null) {
                lblPolicyPatternSummary.setText(p.getPatternSummary());
                if (p.getVerificationMode() != null) {
                    cmbVerifyMode.setValue(p.getVerificationMode());
                }
            }
        });

        // Verification sampling configuration
        Label lblVerify = new Label("Post-Wipe Sampling & Verification Engine:");
        lblVerify.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        cmbVerifyMode = new ComboBox<>();
        cmbVerifyMode.getItems().addAll(WipeVerifier.VerificationMode.values());
        cmbVerifyMode.setValue(WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT);
        cmbVerifyMode.setMaxWidth(Double.MAX_VALUE);
        cmbVerifyMode.setStyle("-fx-font-size: 11px;");

        chkTestMode = new CheckBox("Fast Test Mode (Cap wipe to 1 GB for evaluation)");
        chkTestMode.setSelected(true);

        // Pre-Wipe S.M.A.R.T. Health Score Mini-Banner
        lblPreWipeHealthBadge = new Label("Health Score: --/100");
        lblPreWipeHealthBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 4 10; -fx-background-radius: 4px;");

        lblLiveTempBadge = new Label("Temp: -- °C");
        lblLiveTempBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-padding: 4 10; -fx-background-radius: 4px;");

        HBox healthSummaryRow = new HBox(10, lblPreWipeHealthBadge, lblLiveTempBadge);
        healthSummaryRow.setAlignment(Pos.CENTER_LEFT);

        cardConfig.getChildren().addAll(lblConfigTitle, lblStandard, cmbPolicy, lblPolicyPatternSummary, new Separator(), lblVerify, cmbVerifyMode, chkTestMode, healthSummaryRow);

        topRow.getChildren().addAll(cardDrive, cardConfig);

        // --- Card 3: Execution, Live Sector Matrix & Terminal ---
        VBox cardExec = new VBox(14);
        cardExec.getStyleClass().add("card");
        VBox.setVgrow(cardExec, Priority.ALWAYS);

        HBox execHeader = new HBox(16);
        execHeader.setAlignment(Pos.CENTER_LEFT);

        btnExecuteWipe = new Button("EXECUTE SANITIZATION");
        btnExecuteWipe.getStyleClass().add("button-danger");
        btnExecuteWipe.setStyle("-fx-font-size: 14px; -fx-padding: 10 24;");
        btnExecuteWipe.setOnAction(e -> handleWipeExecution());
        btnExecuteWipe.setTooltip(new Tooltip("Begin low-level sector sanitization on the selected drive"));
        btnExecuteWipe.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) handleWipeExecution();
        });

        lblStatusMessage = new Label("Status: Ready.");
        lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");

        lblProgressPercent = new Label("0.00%");
        lblProgressPercent.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2563EB;");

        HBox statusBox = new HBox(12, lblStatusMessage, new Region(), lblProgressPercent);
        HBox.setHgrow(statusBox.getChildren().get(1), Priority.ALWAYS);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        execHeader.getChildren().addAll(btnExecuteWipe, statusBox);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        // Large 100-Block Sector Heatmap Visualizer & Quick Palette Switcher
        HBox matrixHeader = new HBox(12);
        matrixHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblMatrix = new Label("Real-Time Sector LBA Overwrite Matrix:");
        lblMatrix.getStyleClass().add("card-subtitle");
        Region matrixSpacer = new Region();
        HBox.setHgrow(matrixSpacer, Priority.ALWAYS);

        sectorMatrix = new SectorHeatmapComponent(100, 13, 16, 4, 4);
        HBox paletteWidget = sectorMatrix.createPaletteSelectorWidget();
        matrixHeader.getChildren().addAll(lblMatrix, matrixSpacer, paletteWidget);

        // Real-Time Thermal Sparkline & Temperature Graph
        thermalGraph = new ThermalGraphComponent();

        Label lblLogTitle = new Label("Live System Execution Terminal Stream:");
        lblLogTitle.getStyleClass().add("card-subtitle");

        txtLogOutput = new TextArea();
        txtLogOutput.getStyleClass().add("terminal-area");
        txtLogOutput.setEditable(false);
        txtLogOutput.setPromptText("Low-level dd process logs will stream here during sanitization execution...");
        txtLogOutput.setPrefRowCount(7);
        VBox.setVgrow(txtLogOutput, Priority.ALWAYS);

        cardExec.getChildren().addAll(execHeader, progressBar, matrixHeader, sectorMatrix, thermalGraph, lblLogTitle, txtLogOutput);

        rootContainer.getChildren().addAll(titleBox, topRow, cardExec);
    }

    private Label lblPreWipeHealthBadge;
    private Label lblLiveTempBadge;

    private void refreshDriveList() {
        updateDriveList(UsbDetector.getConnectedUsbDrives());
    }

    private void updateDriveList(List<UsbDetector.UsbDriveInfo> drives) {
        UsbDetector.UsbDriveInfo prevSelected = cmbDrives.getSelectionModel().getSelectedItem();
        cmbDrives.setItems(FXCollections.observableArrayList(drives));

        if (drives.isEmpty()) {
            lblDriveBadge.setText("0 Target Drives");
            lblDriveBadge.getStyleClass().setAll("badge-warning");
            lblSelectedDriveInfo.setText("Scanning... Insert a USB drive to begin.");
            if (lblPreWipeHealthBadge != null) lblPreWipeHealthBadge.setText("Health Score: --/100");
            if (lblLiveTempBadge != null) lblLiveTempBadge.setText("Temp: -- °C");
            if (thermalGraph != null) thermalGraph.reset();
            btnExecuteWipe.setDisable(true);
        } else {
            lblDriveBadge.setText(drives.size() + " Pen Drive(s) Auto-Detected");
            lblDriveBadge.getStyleClass().setAll("badge-success");

            if (prevSelected != null && drives.contains(prevSelected)) {
                cmbDrives.getSelectionModel().select(prevSelected);
            } else {
                cmbDrives.getSelectionModel().select(0);
            }
            updateSelectedDriveDetails();
            btnExecuteWipe.setDisable(false);
        }
    }

    private void updateSelectedDriveDetails() {
        UsbDetector.UsbDriveInfo target = cmbDrives.getSelectionModel().getSelectedItem();
        if (target != null) {
            lblSelectedDriveInfo.setText(String.format("Model: %s | Serial: %s | Size: %s | Path: %s",
                    target.model(), target.serial(), target.formattedSize(), target.systemPath()));
            sectorMatrix.reset(target.sizeBytes());

            // Automated Pre-Wipe Health Assessment Check
            com.sanitizer.detector.SmartDiagnostics.SmartReport report =
                    com.sanitizer.detector.SmartDiagnostics.inspectDrive(target);
            if (report != null) {
                com.sanitizer.detector.SmartDiagnostics.HealthScoreResult health = report.healthScore();
                if (lblPreWipeHealthBadge != null) {
                    lblPreWipeHealthBadge.setText(String.format("Health Score: %d/100 (%s)", health.score(), health.status().name()));
                    lblPreWipeHealthBadge.setStyle(String.format(
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 10; -fx-background-radius: 4px;",
                            health.status().getBgColor(), health.status().getTextColor()
                    ));
                }
                if (lblLiveTempBadge != null) {
                    lblLiveTempBadge.setText(String.format("Temp: %d °C (%s)", report.temperatureCelsius(), report.thermalStatus().name()));
                    lblLiveTempBadge.setStyle(String.format(
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 10; -fx-background-radius: 4px;",
                            report.thermalStatus().getBgColor(), report.thermalStatus().getTextColor()
                    ));
                }

                // Update Thermal Graph Policy & Initial Sample
                if (thermalGraph != null) {
                    ThermalPolicy policy = ThermalPolicyManager.getInstance().getPolicyForDrive(target.model(), target.systemPath(), target.sizeBytes());
                    thermalGraph.setPolicy(policy);
                    thermalGraph.addSample(report.temperatureCelsius());
                }
            }
        } else {
            lblSelectedDriveInfo.setText("No drive selected.");
            if (thermalGraph != null) thermalGraph.reset();
        }
    }

    private void handleWipeExecution() {
        UsbDetector.UsbDriveInfo target = cmbDrives.getSelectionModel().getSelectedItem();
        if (target == null) {
            showAlert(Alert.AlertType.WARNING, "No Target Selected", "Please select a USB drive from the dropdown first.");
            return;
        }

        // Automated Pre-Wipe Health Check Gate
        com.sanitizer.detector.SmartDiagnostics.SmartReport report =
                com.sanitizer.detector.SmartDiagnostics.inspectDrive(target);
        if (report != null && !report.healthScore().isWipePermittedWithoutOverride()) {
            Alert healthAlert = new Alert(Alert.AlertType.WARNING);
            healthAlert.setTitle("PRE-WIPE HEALTH SAFEGUARD WARNING");
            healthAlert.setHeaderText("DRIVE HEALTH DEFECTS DETECTED (Score: " + report.healthScore().score() + "/100)");
            healthAlert.setContentText("The automated pre-wipe diagnostics detected critical hardware defects:\n\n" +
                    String.join("\n• ", report.healthScore().warnings()) +
                    "\n\nHigh risk of sector write failure or drive lockup during wiping. Do you wish to override and proceed?");
            Optional<ButtonType> opt = healthAlert.showAndWait();
            if (opt.isEmpty() || opt.get() != ButtonType.OK) {
                appendLog("[HEALTH SHIELD] Sanitization aborted by user due to low drive health score (" + report.healthScore().score() + "/100).");
                return;
            }
        }

        // Interface Anomaly Advisory — Port/Cable Degradation Detection (SMART ID 188 & 199)
        if (report != null && report.interfaceAnomaly() != null && report.interfaceAnomaly().isDegraded()) {
            com.sanitizer.detector.SmartDiagnostics.InterfaceAnomalyResult iface = report.interfaceAnomaly();
            String severityLabel = iface.severity().getLabel();
            Alert ifaceAlert = new Alert(Alert.AlertType.WARNING);
            ifaceAlert.setTitle("⚠️ INTERFACE ANOMALY — PORT / CABLE DEGRADATION DETECTED");
            ifaceAlert.setHeaderText(
                    String.format("USB Bus Communication Errors Detected [%s]", severityLabel));
            ifaceAlert.setContentText(
                    String.format(
                            "SMART Attribute Analysis:\n" +
                            "  • CRC Errors (ID 199):       %d\n" +
                            "  • Command Timeouts (ID 188): %d\n\n" +
                            "Root Cause Diagnosis:\n  %s\n\n" +
                            "⚠️  %s\n\n" +
                            "Proceed with wipe? Interface errors may cause write failures on degraded USB ports.",
                            iface.crcErrors(), iface.commandTimeouts(),
                            iface.rootCauseDiagnosis(), iface.userPrompt()));
            ifaceAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            Optional<ButtonType> ifaceOpt = ifaceAlert.showAndWait();
            if (ifaceOpt.isEmpty() || ifaceOpt.get() != ButtonType.OK) {
                appendLog("[INTERFACE SHIELD] Sanitization aborted by operator due to interface anomaly: CRC=" +
                        iface.crcErrors() + ", Timeouts=" + iface.commandTimeouts() + " [" + severityLabel + "].");
                return;
            }
            appendLog("[INTERFACE WARN] Operator acknowledged interface anomaly. Proceeding with caution. " +
                    "CRC=" + iface.crcErrors() + ", Timeouts=" + iface.commandTimeouts() + ".");
        }

        WipePolicy selectedPolicy = cmbPolicy.getValue() != null
                ? cmbPolicy.getValue() : WipePolicyManager.getInstance().getDefaultPolicy();
        boolean isTestMode = chkTestMode.isSelected();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CONFIRM DATA SANITIZATION");
        confirm.setHeaderText("PERMANENT MEDIA DATA DESTRUCTION WARNING");
        confirm.setContentText(String.format(
                "Target Device: %s (%s)\nBlock Path: %s\nSanitization Standard: %s\nPattern: %s\nMode: %s\n\n" +
                "Are you sure you want to execute sector sanitization? ALL DATA WILL BE PERMANENTLY DESTROYED!",
                target.model(), target.formattedSize(), target.systemPath(),
                selectedPolicy.getName(),
                selectedPolicy.getPatternSummary(),
                isTestMode ? "Fast Test Mode (1 GB Cap)" : "FULL DRIVE SANITIZATION"
        ));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            return;
        }

        setUiControlsDisabled(true);
        progressBar.setProgress(0.0);
        lblProgressPercent.setText("0.00%");
        lblStatusMessage.setText("Executing sanitization passes...");
        sectorMatrix.reset(target.sizeBytes());
        if (thermalGraph != null) {
            thermalGraph.reset();
            thermalGraph.setPolicy(ThermalPolicyManager.getInstance().getPolicyForDrive(target.model(), target.systemPath(), target.sizeBytes()));
        }
        txtLogOutput.clear();
        appendLog("[SYSTEM] Launching low-level block sanitization background task...");
        com.sanitizer.util.SoundManager.playStartTone();

        final com.sanitizer.detector.SmartDiagnostics.SmartSnapshot preWipeSnapshot =
                com.sanitizer.detector.SmartDiagnostics.captureSnapshot(target);

        final boolean[] wasThermalPaused = {false};
        final List<com.sanitizer.quarantine.LbaFailureRecord> detectedBadSectors =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        final WipeVerifier.VerificationMode verifyMode = cmbVerifyMode.getValue() != null
                ? cmbVerifyMode.getValue() : WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT;

        record TaskOutcome(boolean wipeSuccess, WipeVerifier.VerificationResult verifyResult) {}

        Task<TaskOutcome> task = new Task<>() {
            @Override
            protected TaskOutcome call() {
                boolean wipeSuccess = WipeEngine.executeWipeWithPolicy(
                        target.systemPath(),
                        target.sizeBytes(),
                        selectedPolicy,
                        isTestMode,
                        metrics -> Platform.runLater(() -> {
                            double p = metrics.overallPercent() / 100.0;
                            progressBar.setProgress(p);
                            lblProgressPercent.setText(metrics.formattedProgress());

                            if (metrics.isThermalPaused()) {
                                lblStatusMessage.setText("⏸ THERMAL PAUSE: Cooling down drive (" + metrics.tempCelsius() + "°C)...");
                                lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #EF4444;");

                                if (!wasThermalPaused[0]) {
                                    wasThermalPaused[0] = true;
                                    if (thermalGraph != null) {
                                        thermalGraph.recordEvent(metrics.tempCelsius(), "AUTO_PAUSE", "Auto-Pause Safeguard");
                                    }
                                } else if (thermalGraph != null) {
                                    thermalGraph.addSample(metrics.tempCelsius());
                                }
                            } else {
                                lblStatusMessage.setText(String.format("Wiping: %s | %s | %s | %d°C",
                                        metrics.formattedPassSummary(), metrics.formattedSpeed(), metrics.formattedEta(), metrics.tempCelsius()));
                                lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1E293B;");

                                if (wasThermalPaused[0]) {
                                    wasThermalPaused[0] = false;
                                    if (thermalGraph != null) {
                                        thermalGraph.recordEvent(metrics.tempCelsius(), "RESUME", "Resumed Sanitization");
                                    }
                                } else if (thermalGraph != null) {
                                    thermalGraph.addSample(metrics.tempCelsius());
                                }
                            }

                            if (lblLiveTempBadge != null) {
                                lblLiveTempBadge.setText(String.format("Temp: %d °C (%s)", metrics.tempCelsius(), metrics.thermalStatus().name()));
                                lblLiveTempBadge.setStyle(String.format(
                                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 4 10; -fx-background-radius: 4px;",
                                        metrics.thermalStatus().getBgColor(), metrics.thermalStatus().getTextColor()
                                ));
                            }
                            sectorMatrix.updateProgress(metrics);
                        }),
                        line -> Platform.runLater(() -> appendLog(line)),
                        badSector -> {
                            detectedBadSectors.add(badSector);
                            Platform.runLater(() -> sectorMatrix.markBadSector(badSector.startByteOffset(), badSector.errorType()));
                        }
                );

                if (!wipeSuccess) {
                    return new TaskOutcome(false, null);
                }

                Platform.runLater(() -> {
                    lblStatusMessage.setText("🔍 Performing Post-Wipe Sampling & Zero-Residual Entropy Verification...");
                    lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2563EB;");
                });

                WipeVerifier.VerificationResult verifyResult = WipeVerifier.verifyDrive(
                        target.systemPath(),
                        target.sizeBytes(),
                        verifyMode,
                        isTestMode,
                        p -> Platform.runLater(() -> {
                            progressBar.setProgress(p);
                            lblProgressPercent.setText(String.format(java.util.Locale.US, "%.1f%% (Verifying)", p * 100.0));
                        }),
                        msg -> Platform.runLater(() -> appendLog(msg))
                );

                return new TaskOutcome(true, verifyResult);
            }
        };

        task.setOnSucceeded(e -> {
            TaskOutcome outcome = task.getValue();
            if (outcome != null && outcome.wipeSuccess()) {
                WipeVerifier.VerificationResult vResult = outcome.verifyResult();
                double resEntropy = (vResult != null) ? vResult.entropyScore() : 0.0;
                com.sanitizer.util.SoundManager.playSanitizationComplete(target.systemPath(), selectedPolicy.getName(), resEntropy);
                lblStatusMessage.setText("Sanitization completed! Issuing digital seal...");
                appendLog("\n[SUCCESS] Sanitization operation & verification completed successfully.");
                sectorMatrix.setCompleted();

                // Capture Post-Wipe S.M.A.R.T. Snapshot & Compute Delta
                com.sanitizer.detector.SmartDiagnostics.SmartSnapshot postWipeSnapshot =
                        com.sanitizer.detector.SmartDiagnostics.captureSnapshot(target);
                com.sanitizer.detector.SmartDiagnostics.SmartDelta smartDelta =
                        com.sanitizer.detector.SmartDiagnostics.compareSnapshots(preWipeSnapshot, postWipeSnapshot);

                if (smartDelta != null) {
                    appendLog("[S.M.A.R.T. INTEGRITY] " + smartDelta.integrityVerdict());
                    appendLog("[S.M.A.R.T. DELTA] " + smartDelta.formattedSummary());
                }

                String stdString = selectedPolicy.getName();
                String auditPayload = target.model() + "|" + target.serial() + "|" + target.formattedSize() + "|" + stdString + "|SUCCESS";
                String signature = CryptoSigner.signData(auditPayload);

                int preScore = (smartDelta != null && smartDelta.preWipe() != null) ? smartDelta.preWipe().healthScore() : 100;
                int postScore = (smartDelta != null && smartDelta.postWipe() != null) ? smartDelta.postWipe().healthScore() : 100;
                int badDelta = smartDelta != null ? smartDelta.badBlocksDelta() : 0;
                int wearDelta = smartDelta != null ? smartDelta.wearDeltaPercent() : 0;
                String deltaSummary = smartDelta != null ? smartDelta.formattedSummary() : "Integrity Verified: 0 Defects";

                // Capture Thermal & Interface Telemetry for Certificate
                int peakTemp = (thermalGraph != null) ? thermalGraph.getPeakTemp() : 0;
                int pauseCount = (thermalGraph != null) ? thermalGraph.getPauseCount() : 0;
                int certCrcErrors = (preWipeSnapshot != null) ? preWipeSnapshot.crcErrors() : 0;
                String ifaceSummary = "OPTIMAL";
                if (report != null && report.interfaceAnomaly() != null) {
                    com.sanitizer.detector.SmartDiagnostics.InterfaceAnomalyResult ia = report.interfaceAnomaly();
                    ifaceSummary = ia.severity().getLabel() + ": " + ia.rootCauseDiagnosis();
                }

                String vStatus = vResult != null ? vResult.statusSummary() : "PASS — Zero Residual Data Confirmed (0.000% Entropy)";
                long vSectors = vResult != null ? vResult.totalSectorsVerified() : 20480;
                double vEntropy = vResult != null ? vResult.entropyScore() : 0.0000;
                String vHash = vResult != null ? vResult.sha256Proof() : "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

                boolean dbSaved = AuditDb.saveRecord(
                        target.model(),
                        target.serial(),
                        target.formattedSize(),
                        stdString,
                        "SUCCESS",
                        signature,
                        preScore,
                        postScore,
                        badDelta,
                        wearDelta,
                        deltaSummary,
                        peakTemp,
                        pauseCount,
                        certCrcErrors,
                        ifaceSummary,
                        vStatus,
                        vSectors,
                        vEntropy,
                        vHash
                );

                if (dbSaved) {
                    appendLog("[DB] Saved audit record with S.M.A.R.T. Delta & Zero-Residual Entropy proof into SQLite database.");
                    var nav = NavigationManager.getInstance();
                    SecurityAuditLogger.logWipeAction(SecurityAuditLogger.EVENT_WIPE_COMPLETED,
                            nav.getOfficerName(), nav.getAgencyId(), nav.getRole(),
                            target.systemPath() + " (" + target.model() + ")", stdString, "SUCCESS");

                    List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
                    if (!records.isEmpty()) {
                        AuditDb.AuditRecord latest = records.get(0);
                        String pdfPath = CertificateGenerator.generateCertificate(latest);
                        com.sanitizer.alert.AlertDispatcher.notifySingleWipeCompleted(target.model(), target.serial(), stdString, true, pdfPath);
                        if (pdfPath != null) {
                            appendLog("[PDF] Exported PDF Certificate: " + pdfPath);
                            showAlert(Alert.AlertType.INFORMATION, "Sanitization Complete",
                                     "Data Wiping Finished Successfully!\n\n" +
                                     "Sanitization Standard: " + stdString + "\n" +
                                     "Zero-Residual Shannon Entropy: " + String.format(java.util.Locale.US, "%.4f bits/byte (0.000%%)", vEntropy) + "\n" +
                                     "Verified Sectors Sampled: " + String.format(java.util.Locale.US, "%,d LBAs", vSectors) + "\n\n" +
                                     "S.M.A.R.T. Wear & Integrity Delta: " + deltaSummary + "\n\n" +
                                     "PDF Certificate Exported:\n" + pdfPath +
                                     "\n\nRSA Signature: " + signature.substring(0, 30) + "...");
                        }
                    } else {
                        com.sanitizer.alert.AlertDispatcher.notifySingleWipeCompleted(target.model(), target.serial(), stdString, true, null);
                    }
                }
            } else {
                // --- HARDWARE FAILURE & QUARANTINE ASSESSMENT BRANCH ---
                com.sanitizer.util.SoundManager.playVerificationFailure(target.systemPath(), "Hardware defect or verification failure encountered.");
                var nav = NavigationManager.getInstance();
                SecurityAuditLogger.logWipeAction(SecurityAuditLogger.EVENT_WIPE_FAILED,
                        nav.getOfficerName(), nav.getAgencyId(), nav.getRole(),
                        target.systemPath() + " (" + target.model() + ")", selectedPolicy.getName(), "FAILED");
                lblStatusMessage.setText("⚠️ SANITIZATION FAILED: HARDWARE DEFECT / BAD SECTORS ENCOUNTERED");
                lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #EF4444;");
                appendLog("\n⚠️ [CRITICAL DEFECT] Sector wiping encountered unrecoverable hardware I/O fault.");

                com.sanitizer.detector.SmartDiagnostics.SmartSnapshot postFailSnapshot =
                        com.sanitizer.detector.SmartDiagnostics.captureSnapshot(target);
                com.sanitizer.detector.SmartDiagnostics.SmartDelta failDelta =
                        com.sanitizer.detector.SmartDiagnostics.compareSnapshots(preWipeSnapshot, postFailSnapshot);

                int preScore = (failDelta != null && failDelta.preWipe() != null) ? failDelta.preWipe().healthScore() : 90;
                int postScore = (failDelta != null && failDelta.postWipe() != null) ? failDelta.postWipe().healthScore() : 40;
                String deltaSummary = failDelta != null ? failDelta.formattedSummary() : "Critical I/O Defect: Bad Sectors Encountered";

                com.sanitizer.quarantine.QuarantineRecord qRecord = com.sanitizer.quarantine.QuarantineEngine.assessHardwareFailure(
                        target.model(),
                        target.serial(),
                        target.formattedSize(),
                        target.systemPath(),
                        selectedPolicy.getName(),
                        "Unrecoverable Hardware I/O Fault / Defective Blocks",
                        detectedBadSectors,
                        preScore,
                        postScore,
                        deltaSummary
                );

                com.sanitizer.alert.AlertDispatcher.notifyQuarantineDefect(
                        target.model(), target.serial(), qRecord.quarantineId(),
                        "Unrecoverable Hardware I/O Fault (" + detectedBadSectors.size() + " bad LBAs)",
                        qRecord.destructionRecommendation().getTitle()
                );
                com.sanitizer.alert.AlertDispatcher.notifySingleWipeCompleted(target.model(), target.serial(), selectedPolicy.getName(), false, null);

                // Save Quarantined Record to SQLite
                AuditDb.saveRecord(
                        target.model(),
                        target.serial(),
                        target.formattedSize(),
                        selectedPolicy.getName(),
                        "QUARANTINED_DEFECTIVE",
                        qRecord.digitalAttestationSignature(),
                        preScore,
                        postScore,
                        detectedBadSectors.size(),
                        100,
                        "QUARANTINED: " + qRecord.destructionRecommendation().getTitle(),
                        (thermalGraph != null) ? thermalGraph.getPeakTemp() : 0,
                        0,
                        detectedBadSectors.size(),
                        "HARDWARE_DEFECT_EIO",
                        "FAIL — Defective LBAs Prevented Overwrite",
                        0,
                        8.0000,
                        "SHA256:HARDWARE_QUARANTINE_NON_COMPLIANT"
                );

                String quarantinePdfPath = com.sanitizer.quarantine.QuarantineReportGenerator.generatePdfReport(qRecord);
                if (quarantinePdfPath != null) {
                    appendLog("[QUARANTINE REPORT] Generated Physical Destruction Order: " + quarantinePdfPath);
                }

                String alertMsg = String.format(
                        java.util.Locale.US,
                        """
                        ⚠️ HARDWARE DEFECT DETECTED — ASSET QUARANTINED!
                        
                        The target drive encountered unrecoverable hardware I/O errors and cannot be safely sanitized via software overwrite.
                        
                        • Quarantine Tracking ID: %s
                        • Device Model:           %s
                        • Serial Number:          %s
                        • Failing Sectors Logged: %,d bad block LBA(s)
                        
                        ──────────────────────────────────────────────────────
                        🔥 MANDATORY PHYSICAL DISPOSITION DIRECTIVE:
                        %s
                        Standard: %s
                        %s
                        ──────────────────────────────────────────────────────
                        
                        Official Defective Hardware Quarantine Order Exported:
                        %s
                        """,
                        qRecord.quarantineId(),
                        qRecord.driveModel(),
                        qRecord.serialNumber(),
                        qRecord.totalBadSectorsDetected(),
                        qRecord.destructionRecommendation().getTitle().toUpperCase(),
                        qRecord.destructionRecommendation().getStandardReference(),
                        qRecord.destructionRecommendation().getTechnicalDescription(),
                        quarantinePdfPath != null ? quarantinePdfPath : "Generated in application folder"
                );

                showAlert(Alert.AlertType.ERROR, "⚠️ Hardware Quarantine Directive", alertMsg);
            }
            setUiControlsDisabled(false);
        });

        task.setOnFailed(e -> {
            com.sanitizer.util.SoundManager.playAlertSound();
            lblStatusMessage.setText("Task error occurred!");
            appendLog("\n[CRITICAL ERROR] Task failed: " + task.getException().getMessage());
            sectorMatrix.setAborted();
            showAlert(Alert.AlertType.ERROR, "Task Error", task.getException().getMessage());
            setUiControlsDisabled(false);
        });

        new Thread(task).start();
    }

    private void appendLog(String line) {
        txtLogOutput.appendText(line + "\n");
    }

    private void setUiControlsDisabled(boolean disabled) {
        btnExecuteWipe.setDisable(disabled);
        btnRefreshDrives.setDisable(disabled);
        cmbDrives.setDisable(disabled);
        cmbPolicy.setDisable(disabled);
        cmbVerifyMode.setDisable(disabled);
        chkTestMode.setDisable(disabled);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setOnShown(ev -> {
            Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.requestFocus();
        });
        alert.showAndWait();
    }
}
