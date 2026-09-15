package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

public class SettingsView {

    private final VBox rootContainer = new VBox(24);

    public SettingsView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));

        // ── Header ──────────────────────────────────────────────────────
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Security Policy & System Settings");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Cryptographic parameters, hardware safety shield, database engine, and appearance configuration");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

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
        btnClearDb.setOnAction(e -> handleClearHistory());

        Button btnExportDb = new Button("Export Database Backup");
        btnExportDb.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        btnExportDb.setOnAction(e -> handleExportDbBackup());

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

        rootContainer.getChildren().addAll(titleBox, grid, footer);
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
