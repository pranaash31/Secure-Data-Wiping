package com.sanitizer.gui.views;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.pdf.CertificateGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class WipingView {

    private final VBox rootContainer = new VBox(20);

    private ComboBox<UsbDetector.UsbDriveInfo> cmbDrives;
    private Button btnRefreshDrives;
    private Label lblDriveBadge;
    private Label lblSelectedDriveInfo;

    private RadioButton rdoDod;
    private RadioButton rdoNist;
    private CheckBox chkTestMode;

    private Button btnExecuteWipe;
    private ProgressBar progressBar;
    private Label lblProgressPercent;
    private Label lblStatusMessage;
    private TextArea txtLogOutput;

    public WipingView() {
        buildUi();
        refreshDriveList();
        UsbDetector.registerListener(drives -> updateDriveList(drives));
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(24));

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Hardware Data Sanitization Workplace");
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setStyle("-fx-font-size: 22px;");
        Label lblSub = new Label("Direct low-level raw sector sanitization with hardware protection shield");
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

        btnRefreshDrives = new Button("Refresh Drives");
        btnRefreshDrives.setOnAction(e -> refreshDriveList());

        HBox driveActionBox = new HBox(10, cmbDrives, btnRefreshDrives);
        HBox.setHgrow(cmbDrives, Priority.ALWAYS);

        lblSelectedDriveInfo = new Label("No drive selected.");
        lblSelectedDriveInfo.getStyleClass().add("card-subtitle");

        cardDrive.getChildren().addAll(driveTitleBox, driveActionBox, lblSelectedDriveInfo);

        // --- Card 2: Configuration ---
        VBox cardConfig = new VBox(12);
        cardConfig.getStyleClass().add("card");
        HBox.setHgrow(cardConfig, Priority.ALWAYS);

        Label lblConfigTitle = new Label("Sanitization Standard & Execution Mode");
        lblConfigTitle.getStyleClass().add("card-title");

        ToggleGroup group = new ToggleGroup();
        rdoDod = new RadioButton("DoD 5220.22-M (3-Pass Military Wipe)");
        rdoDod.setToggleGroup(group);
        rdoDod.setSelected(true);

        rdoNist = new RadioButton("NIST SP 800-88 Clear (Single Pass 0x00)");
        rdoNist.setToggleGroup(group);

        VBox radioBox = new VBox(8, rdoDod, rdoNist);

        chkTestMode = new CheckBox("Fast Test Mode (Cap wipe to 1 GB for evaluation)");
        chkTestMode.setSelected(true);

        cardConfig.getChildren().addAll(lblConfigTitle, radioBox, new Separator(), chkTestMode);

        topRow.getChildren().addAll(cardDrive, cardConfig);

        // --- Card 3: Execution & Terminal ---
        VBox cardExec = new VBox(14);
        cardExec.getStyleClass().add("card");
        VBox.setVgrow(cardExec, Priority.ALWAYS);

        HBox execHeader = new HBox(16);
        execHeader.setAlignment(Pos.CENTER_LEFT);

        btnExecuteWipe = new Button("EXECUTE SANITIZATION");
        btnExecuteWipe.getStyleClass().add("button-danger");
        btnExecuteWipe.setStyle("-fx-font-size: 14px; -fx-padding: 10 24;");
        btnExecuteWipe.setOnAction(e -> handleWipeExecution());

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

        Label lblLogTitle = new Label("Live System Execution Terminal Stream:");
        lblLogTitle.getStyleClass().add("card-subtitle");

        txtLogOutput = new TextArea();
        txtLogOutput.getStyleClass().add("terminal-area");
        txtLogOutput.setEditable(false);
        txtLogOutput.setPromptText("Low-level dd process logs will stream here during sanitization execution...");
        VBox.setVgrow(txtLogOutput, Priority.ALWAYS);

        cardExec.getChildren().addAll(execHeader, progressBar, lblLogTitle, txtLogOutput);

        rootContainer.getChildren().addAll(titleBox, topRow, cardExec);
    }

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
        } else {
            lblSelectedDriveInfo.setText("No drive selected.");
        }
    }

    private void handleWipeExecution() {
        UsbDetector.UsbDriveInfo target = cmbDrives.getSelectionModel().getSelectedItem();
        if (target == null) {
            showAlert(Alert.AlertType.WARNING, "No Target Selected", "Please select a USB drive from the dropdown first.");
            return;
        }

        WipeEngine.WipeStandard standard = rdoDod.isSelected() ? WipeEngine.WipeStandard.DOD_5220_22_M : WipeEngine.WipeStandard.NIST_800_88_CLEAR;
        boolean isTestMode = chkTestMode.isSelected();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CONFIRM DATA SANITIZATION");
        confirm.setHeaderText("PERMANENT MEDIA DATA DESTRUCTION WARNING");
        confirm.setContentText(String.format(
                "Target Device: %s (%s)\nBlock Path: %s\nSanitization Standard: %s\nMode: %s\n\n" +
                "Are you sure you want to execute sector sanitization? ALL DATA WILL BE PERMANENTLY DESTROYED!",
                target.model(), target.formattedSize(), target.systemPath(),
                standard == WipeEngine.WipeStandard.DOD_5220_22_M ? "DoD 5220.22-M (3-Pass)" : "NIST SP 800-88 (1-Pass)",
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
        txtLogOutput.clear();
        appendLog("[SYSTEM] Launching low-level block sanitization background task...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return WipeEngine.executeWipe(
                        target.systemPath(),
                        target.sizeBytes(),
                        standard,
                        isTestMode,
                        percent -> Platform.runLater(() -> {
                            double p = percent / 100.0;
                            progressBar.setProgress(p);
                            lblProgressPercent.setText(String.format("%.2f%%", percent));
                        }),
                        line -> Platform.runLater(() -> appendLog(line))
                );
            }
        };

        task.setOnSucceeded(e -> {
            boolean success = task.getValue();
            if (success) {
                lblStatusMessage.setText("Sanitization completed! Issuing digital seal...");
                appendLog("\n[SUCCESS] Sanitization operation completed successfully.");

                String stdString = standard == WipeEngine.WipeStandard.DOD_5220_22_M ? "DoD 5220.22-M" : "NIST SP 800-88";
                String auditPayload = target.model() + "|" + target.serial() + "|" + target.formattedSize() + "|" + stdString + "|SUCCESS";
                String signature = CryptoSigner.signData(auditPayload);

                boolean dbSaved = AuditDb.saveRecord(
                        target.model(),
                        target.serial(),
                        target.formattedSize(),
                        stdString,
                        "SUCCESS",
                        signature
                );

                if (dbSaved) {
                    appendLog("[DB] Saved audit record into SQLite database.");
                    List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
                    if (!records.isEmpty()) {
                        AuditDb.AuditRecord latest = records.get(0);
                        String pdfPath = CertificateGenerator.generateCertificate(latest);
                        if (pdfPath != null) {
                            appendLog("[PDF] Exported PDF Certificate: " + pdfPath);
                            showAlert(Alert.AlertType.INFORMATION, "Sanitization Complete",
                                    "Data Wiping Finished Successfully!\n\nPDF Certificate Exported:\n" + pdfPath +
                                    "\n\nRSA Signature: " + signature.substring(0, 30) + "...");
                        }
                    }
                }
            } else {
                lblStatusMessage.setText("Sanitization failed!");
                appendLog("\n[ERROR] Sector wiping failed. Please check drive permissions.");
                showAlert(Alert.AlertType.ERROR, "Wipe Failed", "Low-level dd operation failed. Make sure you have administrator privileges.");
            }
            setUiControlsDisabled(false);
        });

        task.setOnFailed(e -> {
            lblStatusMessage.setText("Task error occurred!");
            appendLog("\n[CRITICAL ERROR] Task failed: " + task.getException().getMessage());
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
        rdoDod.setDisable(disabled);
        rdoNist.setDisable(disabled);
        chkTestMode.setDisable(disabled);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
