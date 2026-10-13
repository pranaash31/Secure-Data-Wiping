package com.sanitizer.gui.views.settings;

import com.sanitizer.db.AuditDb;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.gui.views.AuditView;
import com.sanitizer.i18n.I18n;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

import static com.sanitizer.gui.views.settings.SettingsRowUtil.createSettingRow;

/**
 * Settings Card encapsulating SQLite Storage Engine, Cryptographic Proof Verification,
 * and Database Backup / Purge Maintenance.
 */
public class DatabaseMaintenanceCard extends VBox {

    private final ConfigurationService configService;

    public DatabaseMaintenanceCard() {
        this(SystemConfigurationService.getInstance());
    }

    public DatabaseMaintenanceCard(ConfigurationService configService) {
        super(16);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private SessionContext currentSession() {
        var nav = NavigationManager.getInstance();
        return new SessionContext(nav.getOfficerName(), nav.getAgencyId(), nav.getRole());
    }

    private void buildUi() {
        HBox dbHeader = new HBox(10);
        dbHeader.setAlignment(Pos.CENTER_LEFT);
        Label dbIcon = new Label("[DB]");
        dbIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label dbTitle = new Label(I18n.get("settings.db_title"));
        dbTitle.getStyleClass().add("settings-section-title");
        dbHeader.getChildren().addAll(dbIcon, dbTitle);

        VBox dbFields = new VBox(12);
        dbFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.db_engine"), "SQLite 3.45 JDBC (WAL Mode)"),
                createSettingRow(I18n.get("settings.db_file"), "sanitizer_history.db"),
                createSettingRow("Tamper Protection", "SHA-256 Cryptographic Block Ledger"),
                createSettingRow(I18n.get("settings.db_pdf"), "Apache PDFBox 3.0.1"),
                createSettingRow(I18n.get("settings.db_hardware"), "OSHI 6.4.10")
        );

        HBox dbActions = new HBox(12);
        Button btnVerifyDb = new Button("🔒 Verify Database Integrity");
        btnVerifyDb.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16; -fx-font-size: 12px; -fx-cursor: hand;");
        btnVerifyDb.setTooltip(new Tooltip("Instant cryptographic proof verifying no SQLite records have been manually modified or injected"));
        btnVerifyDb.setOnAction(e -> handleVerifyIntegrity());
        btnVerifyDb.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                handleVerifyIntegrity();
            }
        });

        Button btnClearDb = new Button(I18n.get("settings.db_clear"));
        btnClearDb.setStyle("-fx-text-fill: #F87171; -fx-padding: 8 16; -fx-font-size: 12px;");
        btnClearDb.setTooltip(new Tooltip("Permanently clear all sanitization audit records from SQLite"));
        btnClearDb.setOnAction(e -> handleClearHistory());
        btnClearDb.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                handleClearHistory();
            }
        });

        Button btnExportDb = new Button(I18n.get("settings.db_export"));
        btnExportDb.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        btnExportDb.setTooltip(new Tooltip("Save a copy of sanitizer_history.db to a chosen location"));
        btnExportDb.setOnAction(e -> handleExportDbBackup());
        btnExportDb.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                handleExportDbBackup();
            }
        });

        dbActions.getChildren().addAll(btnVerifyDb, btnClearDb, btnExportDb);
        getChildren().addAll(dbHeader, new Separator(), dbFields, dbActions);
    }

    private void handleVerifyIntegrity() {
        var res = AuditDb.verifyDatabaseIntegrity();
        AuditView.showLedgerVerificationDialog(res);
    }

    private void handleClearHistory() {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.AUDIT_CLEAR) && !nav.getUserRole().isAtLeast(UserRole.SUPERVISOR)) {
            nav.showAccessDeniedDialog("Audit Ledger Purge", "SUPERVISOR / CHIEF AUDITOR");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("dialog.db.clear.title"));
        confirm.setHeaderText(I18n.get("dialog.db.clear.header"));
        confirm.setContentText(I18n.get("dialog.db.clear.content"));

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean success = configService.clearAuditHistory(currentSession());
            if (success) {
                NavigationManager.getInstance().showNotification("Audit Log Cleared",
                        I18n.get("dialog.db.clear.success"), ToastType.WARNING);
            } else {
                NavigationManager.getInstance().showNotification("Clear Error",
                        I18n.get("dialog.db.clear.error"), ToastType.ERROR);
            }
        }
    }

    private void handleExportDbBackup() {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.AUDIT_EXPORT) && !nav.hasPermission(UserRole.Permission.SETTINGS_MODIFY)) {
            nav.showAccessDeniedDialog("Database Backup Export", "SUPERVISOR / CHIEF AUDITOR");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("dialog.db.backup.title"));
        chooser.setInitialFileName("sanitizer_history_backup.db");
        File dest = chooser.showSaveDialog(null);

        if (dest != null) {
            configService.exportDatabaseBackupAsync(dest)
                    .thenAccept(exportedFile -> Platform.runLater(() ->
                            NavigationManager.getInstance().showNotification(
                                    "Backup Created",
                                    "Successfully exported database backup to " + exportedFile.getName(),
                                    ToastType.SUCCESS
                            )
                    ))
                    .exceptionally(ex -> {
                        Platform.runLater(() ->
                                NavigationManager.getInstance().showNotification(
                                        "Backup Failed",
                                        "Error copying database file: " + ex.getMessage(),
                                        ToastType.ERROR
                                )
                        );
                        return null;
                    });
        }
    }
}
