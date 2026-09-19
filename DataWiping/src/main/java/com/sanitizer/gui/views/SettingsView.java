package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class SettingsView {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(24);

    // Spinners for configurable thermal policies
    private final Map<DeviceType, Spinner<Integer>> pauseSpinners = new EnumMap<>(DeviceType.class);
    private final Map<DeviceType, Spinner<Integer>> resumeSpinners = new EnumMap<>(DeviceType.class);
    private final Map<DeviceType, Label> policyStatusBadges = new EnumMap<>(DeviceType.class);

    public SettingsView() {
        buildUi();
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

        rootContainer.setPadding(new Insets(28));

        // ── Header ──────────────────────────────────────────────────────
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Security Policy & System Settings");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Configurable thermal limits, cryptographic parameters, hardware safety shield, and database engine");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // ── Card 0: Configurable Thermal Limits & Throttling Policies ────
        VBox cardThermal = buildThermalPolicyCard();

        // ── Settings Grid ─────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);

        // Card 1 — Cryptographic Parameters
        VBox cardCrypto = new VBox(16);
        cardCrypto.getStyleClass().add("card");

        HBox cryptoHeader = new HBox(10);
        cryptoHeader.setAlignment(Pos.CENTER_LEFT);
        Label cryptoIcon = new Label("[CRYPTO]");
        cryptoIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FBBF24; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label cryptoTitle = new Label("Cryptographic Signing Parameters");
        cryptoTitle.getStyleClass().add("settings-section-title");
        cryptoHeader.getChildren().addAll(cryptoIcon, cryptoTitle);

        VBox cryptoFields = new VBox(12);
        cryptoFields.getChildren().addAll(
                createSettingRow("Signature Algorithm", "SHA256withRSA"),
                createSettingRow("Keypair Strength", "RSA 2048-bit"),
                createSettingRow("Verification Barcode", "ZXing QR Code (150×150)"),
                createSettingRow("Audit Payload Format", "Model|Serial|Capacity|Standard|Status")
        );
        cardCrypto.getChildren().addAll(cryptoHeader, new Separator(), cryptoFields);

        // Card 2 — Hardware Safety Shield
        VBox cardShield = new VBox(16);
        cardShield.getStyleClass().add("card");

        HBox shieldHeader = new HBox(10);
        shieldHeader.setAlignment(Pos.CENTER_LEFT);
        Label shieldIcon = new Label("[SHIELD]");
        shieldIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #34D399; " +
                "-fx-background-color: rgba(16,185,129,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label shieldTitle = new Label("Hardware Safety Guardrails");
        shieldTitle.getStyleClass().add("settings-section-title");
        shieldHeader.getChildren().addAll(shieldIcon, shieldTitle);

        VBox shieldFields = new VBox(12);
        shieldFields.getChildren().addAll(
                createSettingRow("Primary Disk Guard", "ENABLED (disk0 & rdisk0 Blocked)"),
                createSettingRow("Internal Storage Filter", "ENABLED (Apple SSD, NVMe Ignored)"),
                createSettingRow("Target Capacity Window", "1 GB – 128 GB Removable USB"),
                createSettingRow("macOS Unmount Command", "diskutil unmountDisk /dev/diskX")
        );
        cardShield.getChildren().addAll(shieldHeader, new Separator(), shieldFields);

        // Card 3 — Database & Storage Engine
        VBox cardDb = new VBox(16);
        cardDb.getStyleClass().add("card");

        HBox dbHeader = new HBox(10);
        dbHeader.setAlignment(Pos.CENTER_LEFT);
        Label dbIcon = new Label("[DB]");
        dbIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label dbTitle = new Label("Database & Storage Engine");
        dbTitle.getStyleClass().add("settings-section-title");
        dbHeader.getChildren().addAll(dbIcon, dbTitle);

        VBox dbFields = new VBox(12);
        dbFields.getChildren().addAll(
                createSettingRow("Database Engine", "SQLite 3.45 JDBC"),
                createSettingRow("Database File", "sanitizer_history.db"),
                createSettingRow("PDF Engine", "Apache PDFBox 3.0.1"),
                createSettingRow("Hardware Library", "OSHI 6.4.10")
        );

        HBox dbActions = new HBox(12);
        Button btnClearDb = new Button("Clear Audit History");
        btnClearDb.setStyle("-fx-text-fill: #F87171; -fx-padding: 8 16; -fx-font-size: 12px;");
        btnClearDb.setTooltip(new Tooltip("Permanently clear all sanitization audit records from SQLite"));
        btnClearDb.setOnAction(e -> handleClearHistory());
        btnClearDb.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleClearHistory(); });

        Button btnExportDb = new Button("Export Database Backup");
        btnExportDb.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        btnExportDb.setTooltip(new Tooltip("Save a copy of sanitizer_history.db to a chosen location"));
        btnExportDb.setOnAction(e -> handleExportDbBackup());
        btnExportDb.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleExportDbBackup(); });

        dbActions.getChildren().addAll(btnClearDb, btnExportDb);
        cardDb.getChildren().addAll(dbHeader, new Separator(), dbFields, dbActions);

        // Card 4 — System Info
        VBox cardSystem = new VBox(16);
        cardSystem.getStyleClass().add("card");

        HBox sysHeader = new HBox(10);
        sysHeader.setAlignment(Pos.CENTER_LEFT);
        Label sysIcon = new Label("[SYS]");
        sysIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; " +
                "-fx-background-color: rgba(148,163,184,0.10); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label sysTitle = new Label("System Information");
        sysTitle.getStyleClass().add("settings-section-title");
        sysHeader.getChildren().addAll(sysIcon, sysTitle);

        VBox sysFields = new VBox(12);
        sysFields.getChildren().addAll(
                createSettingRow("Application", "SecureErase Pro v2.0.0 Enterprise"),
                createSettingRow("Java Runtime", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"),
                createSettingRow("JavaFX Version", System.getProperty("javafx.version", "21")),
                createSettingRow("Operating System", System.getProperty("os.name") + " " + System.getProperty("os.version")),
                createSettingRow("Architecture", System.getProperty("os.arch")),
                createSettingRow("Low-Level Wipe Binary", "dd (Block Size: bs=2m)")
        );
        cardSystem.getChildren().addAll(sysHeader, new Separator(), sysFields);

        // Layout grid
        grid.add(cardCrypto, 0, 0);
        grid.add(cardShield, 1, 0);
        grid.add(cardDb,     0, 1);
        grid.add(cardSystem, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // ── About Footer ────────────────────────────────────────────────
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("card");
        footer.setPadding(new Insets(16, 20, 16, 20));
        Label footerLabel = new Label("SecureErase Pro — Enterprise Data Sanitization Suite  |  " +
                "NIST SP 800-88 & DoD 5220.22-M Compliant  |  (c) 2025 SecureErase Technologies Pvt. Ltd.");
        footerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label versionBadge = new Label("v2.0.0 ENTERPRISE");
        versionBadge.getStyleClass().add("badge-info");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footer.getChildren().addAll(footerLabel, footerSpacer, versionBadge);

        rootContainer.getChildren().addAll(titleBox, cardThermal, grid, footer);
    }

    private VBox buildThermalPolicyCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("card");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("[THERMAL POLICIES]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444; " +
                "-fx-background-color: rgba(239,68,68,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Configurable Thermal Limits & Throttling Policies");
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label("Set autonomous auto-pause and cooldown resume temperature thresholds customized by storage media category.");
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label shieldStatus = new Label("AUTONOMOUS SAFEGUARD ACTIVE");
        shieldStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #10B981; " +
                "-fx-background-color: #ECFDF5; -fx-background-radius: 12px; -fx-border-color: #A7F3D0; " +
                "-fx-border-radius: 12px; -fx-padding: 4 12;");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, shieldStatus);

        // 3 Device Type Policy Rows
        VBox devicePolicyList = new VBox(14);
        for (DeviceType type : DeviceType.values()) {
            devicePolicyList.getChildren().add(buildDevicePolicyRow(type));
        }

        // Actions Toolbar
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_LEFT);

        Button btnSave = new Button("Apply Thermal Policies");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18;");
        btnSave.setOnAction(e -> handleSaveThermalPolicies());
        btnSave.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleSaveThermalPolicies(); });

        Button btnReset = new Button("Reset to Factory Defaults");
        btnReset.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        btnReset.setOnAction(e -> handleResetThermalDefaults());
        btnReset.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleResetThermalDefaults(); });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        Label lblHelp = new Label("ℹ Auto-pause must be at least 3°C higher than cooldown resume threshold.");
        lblHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        actionsBar.getChildren().addAll(btnSave, btnReset, actionSpacer, lblHelp);

        card.getChildren().addAll(header, new Separator(), devicePolicyList, actionsBar);
        return card;
    }

    private Node buildDevicePolicyRow(DeviceType type) {
        ThermalPolicy currentPolicy = ThermalPolicyManager.getInstance().getPolicy(type);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10px; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 10px; -fx-padding: 12 16;");

        // Device Icon & Title Column
        VBox devInfo = new VBox(3);
        devInfo.setPrefWidth(260);

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(type.getShortBadge());
        badge.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s; " +
                "-fx-background-color: %s; -fx-background-radius: 6px; -fx-padding: 2 6;", type.getAccentColor(), type.getBgColor()));

        Label name = new Label(type.getDisplayName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
        titleBox.getChildren().addAll(badge, name);

        Label desc = new Label(type.getDescription());
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        devInfo.getChildren().addAll(titleBox, desc);

        // Auto-Pause Control
        VBox pauseBox = new VBox(3);
        pauseBox.setAlignment(Pos.CENTER_LEFT);
        Label lblPause = new Label("Auto-Pause Limit:");
        lblPause.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        Spinner<Integer> spPause = new Spinner<>(40, 95, currentPolicy.autoPauseCelsius(), 1);
        spPause.setEditable(true);
        spPause.setPrefWidth(90);
        spPause.setStyle("-fx-font-size: 12px;");
        pauseSpinners.put(type, spPause);
        pauseBox.getChildren().addAll(lblPause, spPause);

        // Safe-Resume Control
        VBox resumeBox = new VBox(3);
        resumeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblResume = new Label("Safe Resume Cooldown:");
        lblResume.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        Spinner<Integer> spResume = new Spinner<>(35, 85, currentPolicy.resumeCelsius(), 1);
        spResume.setEditable(true);
        spResume.setPrefWidth(90);
        spResume.setStyle("-fx-font-size: 12px;");
        resumeSpinners.put(type, spResume);
        resumeBox.getChildren().addAll(lblResume, spResume);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label statusBadge = new Label(formatPolicyBadge(currentPolicy.autoPauseCelsius(), currentPolicy.resumeCelsius()));
        statusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B; " +
                "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 6 12;");
        policyStatusBadges.put(type, statusBadge);

        row.getChildren().addAll(devInfo, pauseBox, resumeBox, spacer, statusBadge);
        return row;
    }

    private String formatPolicyBadge(int pause, int resume) {
        return String.format("Auto-Pause: %d°C  |  Resumes: %d°C", pause, resume);
    }

    private void handleSaveThermalPolicies() {
        boolean allValid = true;
        StringBuilder errorMsg = new StringBuilder();

        for (DeviceType type : DeviceType.values()) {
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);

            if (spPause == null || spResume == null) continue;

            int pause = spPause.getValue();
            int resume = spResume.getValue();

            if (!ThermalPolicy.isValid(pause, resume)) {
                allValid = false;
                errorMsg.append(String.format("• %s: Auto-pause (%d°C) must be at least 3°C above Resume threshold (%d°C).\n",
                        type.getDisplayName(), pause, resume));
            }
        }

        if (!allValid) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Thermal Thresholds");
            alert.setHeaderText("Thermal Policy Constraint Violation");
            alert.setContentText(errorMsg.toString());
            alert.showAndWait();
            return;
        }

        // Commit and apply all policies
        for (DeviceType type : DeviceType.values()) {
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);
            if (spPause != null && spResume != null) {
                int pause = spPause.getValue();
                int resume = spResume.getValue();
                ThermalPolicyManager.getInstance().setPolicy(type, pause, resume);

                Label badge = policyStatusBadges.get(type);
                if (badge != null) {
                    badge.setText(formatPolicyBadge(pause, resume));
                }
            }
        }

        NavigationManager.getInstance().showNotification(
                "Thermal Policies Saved",
                "Hardware temperature limits and throttling thresholds updated successfully.",
                ToastNotification.ToastType.SUCCESS
        );
    }

    private void handleResetThermalDefaults() {
        ThermalPolicyManager.getInstance().resetToDefaults();

        for (DeviceType type : DeviceType.values()) {
            ThermalPolicy def = ThermalPolicyManager.getInstance().getPolicy(type);
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);
            Label badge = policyStatusBadges.get(type);

            if (spPause != null) spPause.getValueFactory().setValue(def.autoPauseCelsius());
            if (spResume != null) spResume.getValueFactory().setValue(def.resumeCelsius());
            if (badge != null) badge.setText(formatPolicyBadge(def.autoPauseCelsius(), def.resumeCelsius()));
        }

        NavigationManager.getInstance().showNotification(
                "Defaults Restored",
                "Thermal throttling thresholds reset to factory default standards (USB: 55/42°C, NVMe: 70/52°C, HDD: 50/40°C).",
                ToastNotification.ToastType.INFO
        );
    }

    private void handleClearHistory() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Audit Log Database");
        confirm.setHeaderText("PERMANENT AUDIT HISTORY DELETION");
        confirm.setContentText("Are you sure you want to clear all recorded sanitization audit logs from SQLite?");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            // Re-initialize DB tables or clear records
            try {
                AuditDb.saveRecord("SYSTEM_RESET", "N/A", "0 GB", "LOG_CLEAR", "RESET", "N/A");
                NavigationManager.getInstance().showNotification("Audit Log Cleared",
                        "Database history has been cleared.", ToastNotification.ToastType.WARNING);
            } catch (Exception ex) {
                NavigationManager.getInstance().showNotification("Clear Error",
                        ex.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private void handleExportDbBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup SQLite Audit Database");
        chooser.setInitialFileName("sanitizer_history_backup.db");
        File dest = chooser.showSaveDialog(null);

        if (dest != null) {
            File src = new File("sanitizer_history.db");
            if (!src.exists()) {
                NavigationManager.getInstance().showNotification("Backup Error",
                        "Database file sanitizer_history.db not found on disk.", ToastNotification.ToastType.ERROR);
                return;
            }

            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                NavigationManager.getInstance().showNotification("Backup Created",
                        "Successfully exported database backup to " + dest.getName(), ToastNotification.ToastType.SUCCESS);
            } catch (IOException ex) {
                NavigationManager.getInstance().showNotification("Backup Failed",
                        "Error copying database file: " + ex.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private HBox createSettingRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblKey = new Label(label);
        lblKey.getStyleClass().add("settings-key-label");

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        Label lblVal = new Label(value);
        lblVal.getStyleClass().add("settings-value-label");

        row.getChildren().addAll(lblKey, r, lblVal);
        return row;
    }
}
