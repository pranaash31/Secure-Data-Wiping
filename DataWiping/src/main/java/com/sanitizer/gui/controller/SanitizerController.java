package com.sanitizer.gui.controller;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.pdf.CertificateGenerator;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class SanitizerController {

    // Main UI root
    private final BorderPane rootPane = new BorderPane();

    // Tab 1 Components: Sanitization Dashboard
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

    // Tab 2 Components: Audit History
    private TableView<AuditDb.AuditRecord> tblAuditHistory;
    private ObservableList<AuditDb.AuditRecord> auditData;

    public SanitizerController() {
        buildUi();
        refreshDriveList();
        loadAuditHistory();
    }

    public Parent getRoot() {
        return rootPane;
    }

    private void buildUi() {
        // Top Header
        VBox headerBox = new VBox(4);
        headerBox.getStyleClass().add("header-box");

        Label lblTitle = new Label("SECURE DATA SANITIZATION SUITE");
        lblTitle.getStyleClass().add("header-title");

        Label lblSubtitle = new Label("Defense-Grade Sector Sanitization, Cryptographic Audit Trail & PDF Certification");
        lblSubtitle.getStyleClass().add("header-subtitle");

        headerBox.getChildren().addAll(lblTitle, lblSubtitle);
        rootPane.setTop(headerBox);

        // Tab Pane Center
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabWipe = new Tab("Sanitization Dashboard", buildWipeDashboardTab());
        Tab tabAudit = new Tab("Audit Log & Certificates", buildAuditHistoryTab());

        tabPane.getTabs().addAll(tabWipe, tabAudit);
        rootPane.setCenter(tabPane);
    }

    private VBox buildWipeDashboardTab() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(20));

        // Row 1: Drive Selection Card & Configuration Card
        HBox topRow = new HBox(16);
        HBox.setHgrow(topRow, Priority.ALWAYS);

        // --- Card 1: USB Drive Selection ---
        VBox cardDrive = new VBox(12);
        cardDrive.getStyleClass().add("card-panel");
        HBox.setHgrow(cardDrive, Priority.ALWAYS);

        HBox driveTitleBox = new HBox(10);
        driveTitleBox.setAlignment(Pos.CENTER_LEFT);
        Label lblDriveTitle = new Label("1. Select Connected Target Drive");
        lblDriveTitle.getStyleClass().add("card-title");

        lblDriveBadge = new Label("0 Drives Found");
        lblDriveBadge.getStyleClass().add("badge-warning");
        driveTitleBox.getChildren().addAll(lblDriveTitle, lblDriveBadge);

        cmbDrives = new ComboBox<>();
        cmbDrives.setMaxWidth(Double.MAX_VALUE);
        cmbDrives.setPromptText("Scanning for USB pen drives...");
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
        lblSelectedDriveInfo.getStyleClass().add("label-muted");

        cardDrive.getChildren().addAll(driveTitleBox, driveActionBox, lblSelectedDriveInfo);

        // --- Card 2: Wipe Configuration ---
        VBox cardConfig = new VBox(12);
        cardConfig.getStyleClass().add("card-panel");
        HBox.setHgrow(cardConfig, Priority.ALWAYS);

        Label lblConfigTitle = new Label("2. Wipe Algorithm & Mode");
        lblConfigTitle.getStyleClass().add("card-title");

        ToggleGroup standardGroup = new ToggleGroup();
        rdoDod = new RadioButton("DoD 5220.22-M (3-Pass Defense Wipe)");
        rdoDod.setToggleGroup(standardGroup);
        rdoDod.setSelected(true);

        rdoNist = new RadioButton("NIST SP 800-88 Clear (Single Pass 0x00)");
        rdoNist.setToggleGroup(standardGroup);

        VBox radioBox = new VBox(8, rdoDod, rdoNist);

        chkTestMode = new CheckBox("Fast Test Mode (Cap wipe to 1 GB for evaluation)");
        chkTestMode.setSelected(true);
        chkTestMode.getStyleClass().add("label");

        cardConfig.getChildren().addAll(lblConfigTitle, radioBox, new Separator(), chkTestMode);

        topRow.getChildren().addAll(cardDrive, cardConfig);

        // --- Card 3: Execution Controls & Progress Monitor ---
        VBox cardExecute = new VBox(14);
        cardExecute.getStyleClass().add("card-panel");
        VBox.setVgrow(cardExecute, Priority.ALWAYS);

        HBox execHeader = new HBox(16);
        execHeader.setAlignment(Pos.CENTER_LEFT);

        btnExecuteWipe = new Button("EXECUTE SANITIZATION");
        btnExecuteWipe.getStyleClass().add("button-danger");
        btnExecuteWipe.setOnAction(e -> handleWipeExecution());

        lblStatusMessage = new Label("Status: Ready.");
        lblStatusMessage.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        lblProgressPercent = new Label("0.00%");
        lblProgressPercent.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #38BDF8;");

        HBox progressInfoBox = new HBox(12, lblStatusMessage, new Region(), lblProgressPercent);
        HBox.setHgrow(progressInfoBox.getChildren().get(1), Priority.ALWAYS);
        progressInfoBox.setAlignment(Pos.CENTER_LEFT);

        execHeader.getChildren().addAll(btnExecuteWipe, progressInfoBox);
        HBox.setHgrow(progressInfoBox, Priority.ALWAYS);

        progressBar = new ProgressBar(0.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label lblLogHeader = new Label("Console Log Stream:");
        lblLogHeader.getStyleClass().add("label-muted");

        txtLogOutput = new TextArea();
        txtLogOutput.setEditable(false);
        txtLogOutput.setPromptText("Sanitization logs will appear here during execution...");
        VBox.setVgrow(txtLogOutput, Priority.ALWAYS);

        cardExecute.getChildren().addAll(execHeader, progressBar, lblLogHeader, txtLogOutput);

        container.getChildren().addAll(topRow, cardExecute);
        return container;
    }

    @SuppressWarnings("unchecked")
    private VBox buildAuditHistoryTab() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(20));

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblHistoryTitle = new Label("SQLite Tamper-Evident Audit Trail");
        lblHistoryTitle.getStyleClass().add("card-title");

        Button btnRefresh = new Button("Refresh Log (F5)");
        btnRefresh.setTooltip(new Tooltip("Reload audit records from SQLite (F5)"));
        btnRefresh.setOnAction(e -> loadAuditHistory());

        Button btnExportPdf = new Button("Export PDF Certificate");
        btnExportPdf.getStyleClass().add("button-primary");
        btnExportPdf.setTooltip(new Tooltip("Generate a PDF sanitization certificate for the selected record"));
        btnExportPdf.setOnAction(e -> handleExportPdf());

        Button btnVerify = new Button("Verify RSA Signature");
        btnVerify.setTooltip(new Tooltip("Verify the RSA-SHA256 digital signature for the selected record"));
        btnVerify.setOnAction(e -> handleVerifySignature());

        toolbar.getChildren().addAll(lblHistoryTitle, new Region(), btnRefresh, btnExportPdf, btnVerify);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        // F5 to refresh audit log
        toolbar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.F5) loadAuditHistory();
                });
            }
        });

        // Table View
        tblAuditHistory = new TableView<>();
        VBox.setVgrow(tblAuditHistory, Priority.ALWAYS);

        TableColumn<AuditDb.AuditRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()).asObject());
        colId.setPrefWidth(50);

        TableColumn<AuditDb.AuditRecord, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp()));
        colTime.setPrefWidth(140);

        TableColumn<AuditDb.AuditRecord, String> colModel = new TableColumn<>("Drive Model");
        colModel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().driveModel()));
        colModel.setPrefWidth(180);

        TableColumn<AuditDb.AuditRecord, String> colSerial = new TableColumn<>("Serial Number");
        colSerial.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().serialNumber()));
        colSerial.setPrefWidth(140);

        TableColumn<AuditDb.AuditRecord, String> colCapacity = new TableColumn<>("Capacity");
        colCapacity.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().capacity()));
        colCapacity.setPrefWidth(80);

        TableColumn<AuditDb.AuditRecord, String> colStandard = new TableColumn<>("Standard");
        colStandard.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().wipeStandard()));
        colStandard.setPrefWidth(120);

        TableColumn<AuditDb.AuditRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colStatus.setPrefWidth(80);

        TableColumn<AuditDb.AuditRecord, String> colSignature = new TableColumn<>("RSA Digital Signature");
        colSignature.setCellValueFactory(data -> {
            String sig = data.getValue().digitalSignature();
            String snippet = sig.length() > 30 ? sig.substring(0, 30) + "..." : sig;
            return new SimpleStringProperty(snippet);
        });
        colSignature.setPrefWidth(220);

        tblAuditHistory.getColumns().addAll(colId, colTime, colModel, colSerial, colCapacity, colStandard, colStatus, colSignature);

        auditData = FXCollections.observableArrayList();
        tblAuditHistory.setItems(auditData);

        // Double-click row → export PDF instantly
        tblAuditHistory.setRowFactory(tv -> {
            TableRow<AuditDb.AuditRecord> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    handleExportPdf();
                }
            });
            return row;
        });
        // Enter key on focused row also triggers PDF export
        tblAuditHistory.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER && tblAuditHistory.getSelectionModel().getSelectedItem() != null) {
                handleExportPdf();
            }
        });

        container.getChildren().addAll(toolbar, tblAuditHistory);
        return container;
    }

    private void refreshDriveList() {
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        cmbDrives.setItems(FXCollections.observableArrayList(drives));

        if (drives.isEmpty()) {
            lblDriveBadge.setText("0 Drives Found");
            lblDriveBadge.getStyleClass().setAll("badge-warning");
            lblSelectedDriveInfo.setText("No USB pen drives detected. Insert a pen drive and click 'Refresh'.");
            btnExecuteWipe.setDisable(true);
        } else {
            lblDriveBadge.setText(drives.size() + " Pen Drive(s) Ready");
            lblDriveBadge.getStyleClass().setAll("badge-success");
            cmbDrives.getSelectionModel().select(0);
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

        // Safety Confirmation Alert
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CONFIRM DATA SANITIZATION");
        confirm.setHeaderText("PERMANENT DATA LOSS WARNING");
        confirm.setContentText(String.format(
                "Target Drive: %s (%s)\nPath: %s\nAlgorithm: %s\nMode: %s\n\nAre you absolutely sure you want to proceed?",
                target.model(), target.formattedSize(), target.systemPath(),
                standard == WipeEngine.WipeStandard.DOD_5220_22_M ? "DoD 5220.22-M (3-Pass)" : "NIST SP 800-88 (1-Pass)",
                isTestMode ? "Fast Test (1 GB Cap)" : "FULL DRIVE SANITIZATION"
        ));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Disable UI during wipe
        setUiControlsDisabled(true);
        progressBar.setProgress(0.0);
        lblProgressPercent.setText("0.00%");
        lblStatusMessage.setText("Starting wipe operation...");
        txtLogOutput.clear();
        appendLog("[GUI] Launching sector sanitization background task...");

        Task<Boolean> wipeTask = new Task<>() {
            @Override
            protected Boolean call() {
                return WipeEngine.executeWipe(
                        target.systemPath(),
                        target.sizeBytes(),
                        standard,
                        isTestMode,
                        percent -> Platform.runLater(() -> {
                            double prog = percent / 100.0;
                            progressBar.setProgress(prog);
                            lblProgressPercent.setText(String.format("%.2f%%", percent));
                        }),
                        line -> Platform.runLater(() -> appendLog(line))
                );
            }
        };

        wipeTask.setOnSucceeded(e -> {
            boolean success = wipeTask.getValue();
            if (success) {
                lblStatusMessage.setText("Wipe completed successfully! Generating digital signature...");
                appendLog("\n[SUCCESS] Wipe Completed Successfully!");

                // Generate signature & save audit log
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
                    appendLog("[DB] Saved audit record to SQLite database.");
                    loadAuditHistory();

                    List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
                    if (!records.isEmpty()) {
                        AuditDb.AuditRecord latest = records.get(0);
                        String pdfPath = CertificateGenerator.generateCertificate(latest);
                        if (pdfPath != null) {
                            appendLog("[PDF] PDF Sanitization Certificate created: " + pdfPath);
                            showAlert(Alert.AlertType.INFORMATION, "Sanitization Complete",
                                    "Data wiping finished successfully!\n\nPDF Certificate Generated:\n" + pdfPath +
                                            "\n\nRSA Signature: " + signature.substring(0, 30) + "...");
                        }
                    }
                }
            } else {
                lblStatusMessage.setText("Wipe execution failed!");
                appendLog("\n[ERROR] Wipe operation failed. Check permissions or disk access.");
                showAlert(Alert.AlertType.ERROR, "Wipe Failed", "Sector wiping failed. Make sure you have administrator privileges.");
            }
            setUiControlsDisabled(false);
        });

        wipeTask.setOnFailed(e -> {
            lblStatusMessage.setText("Wipe execution error!");
            appendLog("\n[CRITICAL ERROR] Task failed: " + wipeTask.getException().getMessage());
            showAlert(Alert.AlertType.ERROR, "Task Error", wipeTask.getException().getMessage());
            setUiControlsDisabled(false);
        });

        new Thread(wipeTask).start();
    }

    private void appendLog(String message) {
        txtLogOutput.appendText(message + "\n");
    }

    private void setUiControlsDisabled(boolean disabled) {
        btnExecuteWipe.setDisable(disabled);
        btnRefreshDrives.setDisable(disabled);
        cmbDrives.setDisable(disabled);
        rdoDod.setDisable(disabled);
        rdoNist.setDisable(disabled);
        chkTestMode.setDisable(disabled);
    }

    private void loadAuditHistory() {
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        auditData.setAll(records);
    }

    private void handleExportPdf() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Log Selected", "Select an audit record from the table to export its PDF Certificate.");
            return;
        }

        // Run PDF generation on a background thread to avoid freezing the UI
        Task<String> pdfTask = new Task<>() {
            @Override
            protected String call() {
                return CertificateGenerator.generateCertificate(selected);
            }
        };
        pdfTask.setOnSucceeded(ev -> {
            String pdfPath = pdfTask.getValue();
            if (pdfPath != null) {
                showAlert(Alert.AlertType.INFORMATION, "PDF Generated",
                        "Sanitization Certificate exported successfully:\n" + pdfPath);
            } else {
                showAlert(Alert.AlertType.ERROR, "PDF Generation Failed",
                        "Could not generate PDF certificate. Check logs for details.");
            }
        });
        pdfTask.setOnFailed(ev -> showAlert(Alert.AlertType.ERROR, "PDF Task Error",
                pdfTask.getException().getMessage()));
        new Thread(pdfTask, "pdf-export-thread").start();
    }

    private void handleVerifySignature() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Log Selected", "Select an audit record from the table to verify signature.");
            return;
        }

        String payload = selected.driveModel() + "|" + selected.serialNumber() + "|" + selected.capacity() + "|" + selected.wipeStandard() + "|" + selected.status();
        boolean valid = CryptoSigner.verifySignature(payload, selected.digitalSignature());

        if (valid) {
            showAlert(Alert.AlertType.INFORMATION, "Signature Verified",
                    "VERIFICATION SUCCESSFUL\n\nThe SHA256withRSA signature matches the record payload.\nThis audit log is authentic and tamper-free!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Verification Failed",
                    "SIGNATURE MISMATCH\n\nThe signature does not match this record payload or keypair has changed.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        // Focus default button immediately so user can press Enter/Space to dismiss
        alert.getDialogPane().setOnShown(ev ->
            alert.getDialogPane().lookupButton(ButtonType.OK) instanceof Button ok && ok.requestFocus());
        alert.showAndWait();
    }
}
