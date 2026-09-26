package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.components.AccessibilityHelpDialog;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.UserRole;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainLayout {

    private final BorderPane rootPane = new BorderPane();
    private final NavigationManager navManager;
    private final Map<String, Button> navButtons = new HashMap<>();

    // Dynamic UI Elements for Locale Refreshing
    private Label lblTitle;
    private Label lblBadge;
    private Label lblUserInfo;
    private Label lblRoleBadge;
    private Label mainLabel;
    private Label opsLabel;
    private Label mgmtLabel;
    private Label compLabel;
    private Button btnLockSession;
    private Button btnLogoutTop;
    private Button btnThemeToggle;
    private Button btnA11yHelp;
    private ComboBox<Locale> cmbLanguage;
    private Label officerRole;
    private String currentActiveView = "dashboard";

    // Telemetry Status Bar Elements
    private Label lblDrivesStatus;
    private Label lblShieldStatus;
    private Label lblKeyVaultStatus;
    private Label lblDbStatus;
    private Label lblTelemetryInfo;
    private Consumer<List<UsbDetector.UsbDriveInfo>> usbListener;

    public MainLayout(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();

        // Listen for I18n language changes and update text dynamically
        I18n.addListener(locale -> updateLocalizedTexts());

        // Listen for Theme and Scale changes
        AccessibilityManager.addThemeListener(theme -> AccessibilityManager.applyThemeAndScale(rootPane));
        AccessibilityManager.addScaleListener(scale -> AccessibilityManager.applyThemeAndScale(rootPane));
    }

    public Parent getRoot() { return rootPane; }
    public String getCurrentActiveView() { return currentActiveView; }

    public void setContent(Node content, String activeView) {
        this.currentActiveView = activeView;
        rootPane.setCenter(content);
        navButtons.forEach((key, btn) -> {
            if (key.equalsIgnoreCase(activeView)) {
                btn.getStyleClass().setAll("nav-button", "nav-button-active");
            } else {
                btn.getStyleClass().setAll("nav-button");
            }
        });
    }

    private void buildUi() {
        AccessibilityManager.applyThemeAndScale(rootPane);

        // ── TOP BAR ─────────────────────────────────────────────────────
        HBox topBar = new HBox(12);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox(10);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        Label shieldIcon = new Label("[S]");
        shieldIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 8px; -fx-padding: 4 8;");
        lblTitle = new Label(I18n.get("app.title"));
        lblTitle.getStyleClass().add("top-bar-title");
        brandRow.getChildren().addAll(shieldIcon, lblTitle);

        lblBadge = new Label(I18n.get("topbar.badge"));
        lblBadge.getStyleClass().add("top-bar-badge");

        // Role Clearance Badge
        UserRole role = navManager.getUserRole();
        lblRoleBadge = new Label(role.getTierLabel().toUpperCase());
        lblRoleBadge.setStyle(String.format(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 4 10; -fx-background-radius: 6px;",
                role.getAccentColor(), role.getBgColor()
        ));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblUserInfo = new Label(
                I18n.get("app.officer_label") + ": " + navManager.getOfficerName() +
                        "   |   " + I18n.get("app.agency_label") + ": " + navManager.getAgencyId()
        );
        lblUserInfo.getStyleClass().add("top-bar-user");

        // ── Language Selector ComboBox ──
        cmbLanguage = new ComboBox<>();
        cmbLanguage.getItems().addAll(I18n.getSupportedLocales());
        cmbLanguage.setValue(I18n.getLocale());
        cmbLanguage.setStyle("-fx-font-size: 11px; -fx-pref-width: 135px;");
        cmbLanguage.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                return I18n.getLanguageDisplayName(locale);
            }
            @Override
            public Locale fromString(String s) {
                return null;
            }
        });
        cmbLanguage.setOnAction(e -> {
            Locale selected = cmbLanguage.getValue();
            if (selected != null && !selected.equals(I18n.getLocale())) {
                I18n.setLocale(selected);
            }
        });
        cmbLanguage.setTooltip(new Tooltip(I18n.get("topbar.language") + " (Ctrl/Cmd + L)"));
        AccessibilityManager.setupAccessible(cmbLanguage, "Language Selector", "Change application display language", AccessibleRole.COMBO_BOX);

        // ── Font Scaling Zoom Controls ──
        HBox zoomGroup = new HBox(4);
        zoomGroup.setAlignment(Pos.CENTER_LEFT);

        Button btnZoomOut = new Button("A-");
        btnZoomOut.getStyleClass().add("button-theme-toggle");
        btnZoomOut.setStyle("-fx-font-size: 11px; -fx-padding: 5 10; -fx-font-weight: bold;");
        btnZoomOut.setTooltip(new Tooltip(I18n.get("topbar.font_decrease")));
        btnZoomOut.setOnAction(e -> AccessibilityManager.decreaseFontScale());
        AccessibilityManager.setupAccessible(btnZoomOut, "Decrease Font Size", "Scales down application font size (Ctrl/Cmd + -)", AccessibleRole.BUTTON);

        Button btnZoomReset = new Button("100%");
        btnZoomReset.getStyleClass().add("button-theme-toggle");
        btnZoomReset.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        btnZoomReset.setTooltip(new Tooltip(I18n.get("topbar.font_reset")));
        btnZoomReset.setOnAction(e -> AccessibilityManager.resetFontScale());
        AccessibilityManager.setupAccessible(btnZoomReset, "Reset Font Size", "Resets font scaling to standard 100% (Ctrl/Cmd + 0)", AccessibleRole.BUTTON);

        Button btnZoomIn = new Button("A+");
        btnZoomIn.getStyleClass().add("button-theme-toggle");
        btnZoomIn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10; -fx-font-weight: bold;");
        btnZoomIn.setTooltip(new Tooltip(I18n.get("topbar.font_increase")));
        btnZoomIn.setOnAction(e -> AccessibilityManager.increaseFontScale());
        AccessibilityManager.setupAccessible(btnZoomIn, "Increase Font Size", "Scales up application font size (Ctrl/Cmd + +)", AccessibleRole.BUTTON);

        zoomGroup.getChildren().addAll(btnZoomOut, btnZoomReset, btnZoomIn);

        // ── Visual Theme Selector / Toggle ──
        btnThemeToggle = new Button(getThemeToggleLabel());
        btnThemeToggle.getStyleClass().add("button-theme-toggle");
        btnThemeToggle.setOnAction(e -> {
            AccessibilityManager.toggleDarkLight();
            btnThemeToggle.setText(getThemeToggleLabel());
        });
        btnThemeToggle.setTooltip(new Tooltip("Toggle Dark / Light Theme (Ctrl/Cmd + T)"));
        AccessibilityManager.setupAccessible(btnThemeToggle, "Theme Toggle", "Toggles between Light and Dark visual themes", AccessibleRole.BUTTON);

        // ── Accessibility Help Button (F1) ──
        btnA11yHelp = new Button("♿ A11y (F1)");
        btnA11yHelp.getStyleClass().add("button-theme-toggle");
        btnA11yHelp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        btnA11yHelp.setTooltip(new Tooltip(I18n.get("topbar.a11y_help")));
        btnA11yHelp.setOnAction(e -> AccessibilityHelpDialog.show(rootPane.getScene().getWindow()));
        AccessibilityManager.setupAccessible(btnA11yHelp, "Accessibility & Keyboard Navigation Guide", "Opens keyboard navigation and WCAG accessibility guide", AccessibleRole.BUTTON);

        // ── In-App Update Checker Button ──
        Button btnUpdate = new Button("🔄 Updates");
        btnUpdate.getStyleClass().add("button-theme-toggle");
        btnUpdate.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        btnUpdate.setTooltip(new Tooltip("Check for latest updates and security patches"));
        btnUpdate.setOnAction(e -> {
            btnUpdate.setText("Checking...");
            com.sanitizer.update.UpdateManager.getInstance().checkForUpdatesAsync().whenComplete((info, err) -> {
                javafx.application.Platform.runLater(() -> {
                    if (info != null && info.isNewerThan(com.sanitizer.update.UpdateManager.CURRENT_VERSION)) {
                        btnUpdate.setText("🚀 Update v" + info.getVersion());
                        btnUpdate.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #0284c7; -fx-text-fill: #ffffff;");
                        com.sanitizer.gui.components.UpdateDialog.show((Stage) rootPane.getScene().getWindow(), info);
                    } else {
                        btnUpdate.setText("✓ Up to Date");
                        btnUpdate.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
                    }
                });
            });
        });
        AccessibilityManager.setupAccessible(btnUpdate, "Software Updates", "Check for software updates and patches", AccessibleRole.BUTTON);

        // Register UpdateManager listener for automatic background notification
        com.sanitizer.update.UpdateManager.getInstance().addUpdateListener(info -> {
            if (info != null && info.isNewerThan(com.sanitizer.update.UpdateManager.CURRENT_VERSION)) {
                javafx.application.Platform.runLater(() -> {
                    btnUpdate.setText("🚀 Update v" + info.getVersion());
                    btnUpdate.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #0284c7; -fx-text-fill: #ffffff;");
                });
            }
        });

        // ── Spotlight Command Palette Button (Cmd+K) ──
        Button btnSpotlight = new Button("🔍 Search (⌘K)");
        btnSpotlight.getStyleClass().add("button-theme-toggle");
        btnSpotlight.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284C7; -fx-background-color: #F0F9FF; -fx-border-color: #BAE6FD; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        btnSpotlight.setTooltip(new Tooltip("Spotlight Quick Search & Command Palette (Ctrl/Cmd + K)"));
        btnSpotlight.setOnAction(e -> navManager.showCommandPalette());
        AccessibilityManager.setupAccessible(btnSpotlight, "Command Palette Quick Search", "Opens spotlight search and command runner (Ctrl/Cmd + K)", AccessibleRole.BUTTON);

        // ── FISMA Session Lock Button ──
        btnLockSession = new Button("🔒 Lock");
        btnLockSession.getStyleClass().add("button-theme-toggle");
        btnLockSession.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");
        btnLockSession.setTooltip(new Tooltip("Lock session immediately (FISMA / HIPAA Requirement) — Ctrl/Cmd + Alt + L"));
        btnLockSession.setOnAction(e -> navManager.lockSessionNow());
        AccessibilityManager.setupAccessible(btnLockSession, "Lock Session", "Instantly locks the session (Ctrl/Cmd + Alt + L)", AccessibleRole.BUTTON);

        // ── Sign Out ──
        btnLogoutTop = new Button(I18n.get("topbar.sign_out"));
        btnLogoutTop.getStyleClass().add("button-theme-toggle");
        btnLogoutTop.setOnAction(e -> navManager.logout());
        AccessibilityManager.setupAccessible(btnLogoutTop, "Sign Out", "Logs out from current session", AccessibleRole.BUTTON);

        topBar.getChildren().addAll(
                brandRow, lblBadge, lblRoleBadge, spacer,
                btnSpotlight, lblUserInfo, cmbLanguage, zoomGroup, btnThemeToggle, btnA11yHelp, btnUpdate, btnLockSession, btnLogoutTop
        );
        rootPane.setTop(topBar);

        // ── SIDEBAR ──────────────────────────────────────────────────────
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");

        // MAIN section
        mainLabel = new Label(I18n.get("nav.main"));
        mainLabel.getStyleClass().add("sidebar-section-label");
        Button btnDashboard = createNavBtn("  " + I18n.get("nav.dashboard"), "dashboard", "Dashboard View (Ctrl/Cmd + D)");

        // OPERATIONS section
        opsLabel = new Label(I18n.get("nav.operations"));
        opsLabel.getStyleClass().add("sidebar-section-label");
        Button btnWizard    = createNavBtn("  ✨ Guided Wipe Wizard", "wizard", "Simplified 5-step guided sanitization mode for technicians");
        Button btnWiping    = createNavBtn("  " + I18n.get("nav.wiping"), "wiping", "Data Wiping Workplace (Ctrl/Cmd + W)");
        Button btnBatch     = createNavBtn("  " + I18n.get("nav.batch_wipe"), "batchWipe", "Batch Wipe Queue (Ctrl/Cmd + B)");
        Button btnDiag      = createNavBtn("  " + I18n.get("nav.diagnostics"), "diagnostics", "Drive Diagnostics");

        // MANAGEMENT section
        mgmtLabel = new Label(I18n.get("nav.management"));
        mgmtLabel.getStyleClass().add("sidebar-section-label");
        Button btnClients   = createNavBtn("  " + I18n.get("nav.clients"), "clients", "Client Manager");

        String keyVaultLabel = navManager.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)
                ? "  " + I18n.get("nav.key_vault")
                : "  " + I18n.get("nav.key_vault") + " 🔒";
        Button btnKeyVault  = createNavBtn(keyVaultLabel, "keyvault", "Key Vault (Ctrl/Cmd + K)");

        // COMPLIANCE section
        compLabel = new Label(I18n.get("nav.compliance"));
        compLabel.getStyleClass().add("sidebar-section-label");
        Button btnAudit     = createNavBtn("  " + I18n.get("nav.audit"), "audit", "Audit Trail & Security Log (Ctrl/Cmd + A)");
        Button btnVerify    = createNavBtn("  🛡️ " + I18n.get("nav.verify"), "verify", "Verify Certificate");
        Button btnSettings  = createNavBtn("  " + I18n.get("nav.settings"), "settings", "System & Security Settings (Ctrl/Cmd + ,)");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        // Officer info card at bottom of sidebar
        VBox officerCard = new VBox(4);
        officerCard.getStyleClass().add("sidebar-officer-card");
        Label officerName = new Label(navManager.getOfficerName());
        officerName.getStyleClass().add("sidebar-officer-name");
        officerRole = new Label(navManager.getRole() + " (" + role.getTierLabel() + ")");
        officerRole.getStyleClass().add("sidebar-officer-role");
        officerRole.setStyle("-fx-text-fill: " + role.getAccentColor() + "; -fx-font-weight: bold;");
        Label officerAgency = new Label(navManager.getAgencyId());
        officerAgency.getStyleClass().add("sidebar-officer-agency");
        officerCard.getChildren().addAll(officerName, officerRole, officerAgency);

        sidebar.getChildren().addAll(
                mainLabel, btnDashboard,
                opsLabel, btnWizard, btnWiping, btnBatch, btnDiag,
                mgmtLabel, btnClients, btnKeyVault,
                compLabel, btnAudit, btnVerify, btnSettings,
                sidebarSpacer,
                officerCard
        );

        rootPane.setLeft(sidebar);

        // ── BOTTOM TELEMETRY STATUS BAR ─────────────────────────────────
        HBox statusBar = new HBox(12);
        statusBar.getStyleClass().add("bottom-status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        lblDrivesStatus = new Label("⚪ 0 Drives Connected");
        lblDrivesStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-dim");
        lblDrivesStatus.setTooltip(new Tooltip("Scanning for external USB pen drives..."));

        lblShieldStatus = new Label("🛡️ System Shield: ACTIVE");
        lblShieldStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-blue");
        lblShieldStatus.setTooltip(new Tooltip("OS & Internal SSD Write-Protection Active (Non-Pen Drives Blocked)"));

        lblKeyVaultStatus = new Label("🔏 Key Vault: LOADED");
        lblKeyVaultStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-purple");
        lblKeyVaultStatus.setTooltip(new Tooltip("RSA-2048 Cryptographic Hardware Signer Ready"));

        lblDbStatus = new Label("🗄️ Audit DB: ONLINE");
        lblDbStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-green");
        lblDbStatus.setTooltip(new Tooltip("SQLite Audit Trail Active & Integrity Verified"));

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        String osName = System.getProperty("os.name", "macOS");
        lblTelemetryInfo = new Label("⚡ Defense Telemetry: Active | " + osName);
        lblTelemetryInfo.getStyleClass().add("telemetry-info-text");

        statusBar.getChildren().addAll(
                lblDrivesStatus,
                lblShieldStatus,
                lblKeyVaultStatus,
                lblDbStatus,
                statusSpacer,
                lblTelemetryInfo
        );
        rootPane.setBottom(statusBar);

        // Register USB Poller for Live Drive Count & Toast Notifications
        final int[] prevDriveCount = {-1};
        usbListener = drives -> {
            int count = (drives != null) ? drives.size() : 0;
            if (count == 0) {
                lblDrivesStatus.setText("⚪ 0 Drives Connected");
                lblDrivesStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-dim");
                lblDrivesStatus.setTooltip(new Tooltip("No external USB pen drives detected"));

                if (prevDriveCount[0] > 0) {
                    ToastNotification.warning("Drive Disconnected", "Storage device was safely detached.");
                }
            } else {
                String text = (count == 1) ? "🟢 1 Drive Connected" : "🟢 " + count + " Drives Connected";
                lblDrivesStatus.setText(text);
                lblDrivesStatus.getStyleClass().setAll("telemetry-chip", "telemetry-chip-green");
                String driveNames = drives.stream()
                        .map(d -> d.model() + " (" + d.formattedSize() + ")")
                        .collect(Collectors.joining(", "));
                lblDrivesStatus.setTooltip(new Tooltip("Connected Drives: " + driveNames));

                if (prevDriveCount[0] >= 0 && count > prevDriveCount[0]) {
                    var newest = drives.get(drives.size() - 1);
                    ToastNotification.info("Drive Plugged In", newest.model() + " (" + newest.formattedSize() + ") detected.");
                }
            }
            prevDriveCount[0] = count;
        };
        UsbDetector.registerListener(usbListener);
    }

    private Button createNavBtn(String text, String viewKey, String accessibleDesc) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> navManager.navigateTo(viewKey));
        AccessibilityManager.setupAccessible(btn, text.trim(), accessibleDesc, AccessibleRole.BUTTON);
        navButtons.put(viewKey, btn);
        return btn;
    }

    private String getThemeToggleLabel() {
        return switch (AccessibilityManager.getTheme()) {
            case DARK -> "☀️ Light Mode";
            case HIGH_CONTRAST_DARK -> "⚡ HC Dark";
            case HIGH_CONTRAST_LIGHT -> "👁️ HC Light";
            case LIGHT -> "🌙 Dark Mode";
        };
    }

    private void updateLocalizedTexts() {
        lblTitle.setText(I18n.get("app.title"));
        lblBadge.setText(I18n.get("topbar.badge"));
        lblUserInfo.setText(
                I18n.get("app.officer_label") + ": " + navManager.getOfficerName() +
                        "   |   " + I18n.get("app.agency_label") + ": " + navManager.getAgencyId()
        );
        btnLogoutTop.setText(I18n.get("topbar.sign_out"));
        btnA11yHelp.setText("♿ " + I18n.get("a11y.section_title").split(" ")[0] + " (F1)");

        mainLabel.setText(I18n.get("nav.main"));
        opsLabel.setText(I18n.get("nav.operations"));
        mgmtLabel.setText(I18n.get("nav.management"));
        compLabel.setText(I18n.get("nav.compliance"));

        updateNavBtnText("dashboard", "  " + I18n.get("nav.dashboard"));
        updateNavBtnText("wiping", "  " + I18n.get("nav.wiping"));
        updateNavBtnText("batchwipe", "  " + I18n.get("nav.batch_wipe"));
        updateNavBtnText("diagnostics", "  " + I18n.get("nav.diagnostics"));
        updateNavBtnText("clients", "  " + I18n.get("nav.clients"));

        String kvText = navManager.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)
                ? "  " + I18n.get("nav.key_vault")
                : "  " + I18n.get("nav.key_vault") + " 🔒";
        updateNavBtnText("keyvault", kvText);
        updateNavBtnText("audit", "  " + I18n.get("nav.audit"));
        updateNavBtnText("verify", "  🛡️ " + I18n.get("nav.verify"));
        updateNavBtnText("settings", "  " + I18n.get("nav.settings"));

        cmbLanguage.setValue(I18n.getLocale());
    }

    private void updateNavBtnText(String key, String newText) {
        Button btn = navButtons.get(key);
        if (btn != null) {
            btn.setText(newText);
        }
    }
}
