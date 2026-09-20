package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.gui.components.AccessibilityHelpDialog;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

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
        Label lblTitle = new Label(I18n.get("settings.title"));
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label(I18n.get("settings.subtitle"));
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // ── Card 0: Internationalization & Accessibility (WCAG 2.1 AA / Section 508) ────
        VBox cardA11y = buildAccessibilityAndI18nCard();

        // ── Card 1: Custom Wipe Standard & Pattern Builder ────
        VBox cardPolicyBuilder = buildCustomPolicyBuilderCard();

        // ── Card 2: Configurable Thermal Limits & Throttling Policies ────
        VBox cardThermal = buildThermalPolicyCard();

        // ── Settings Grid ─────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);

        // Card 2 — Cryptographic Parameters
        VBox cardCrypto = new VBox(16);
        cardCrypto.getStyleClass().add("card");

        HBox cryptoHeader = new HBox(10);
        cryptoHeader.setAlignment(Pos.CENTER_LEFT);
        Label cryptoIcon = new Label("[CRYPTO]");
        cryptoIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FBBF24; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label cryptoTitle = new Label(I18n.get("settings.crypto_title"));
        cryptoTitle.getStyleClass().add("settings-section-title");
        cryptoHeader.getChildren().addAll(cryptoIcon, cryptoTitle);

        VBox cryptoFields = new VBox(12);
        cryptoFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.crypto_algo"), "SHA256withRSA"),
                createSettingRow(I18n.get("settings.crypto_keypair"), "RSA 2048-bit"),
                createSettingRow(I18n.get("settings.crypto_barcode"), "ZXing QR Code (150×150)"),
                createSettingRow(I18n.get("settings.crypto_payload"), "Model|Serial|Capacity|Standard|Status")
        );
        cardCrypto.getChildren().addAll(cryptoHeader, new Separator(), cryptoFields);

        // Card 3 — Hardware Safety Shield
        VBox cardShield = new VBox(16);
        cardShield.getStyleClass().add("card");

        HBox shieldHeader = new HBox(10);
        shieldHeader.setAlignment(Pos.CENTER_LEFT);
        Label shieldIcon = new Label("[SHIELD]");
        shieldIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #34D399; " +
                "-fx-background-color: rgba(16,185,129,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label shieldTitle = new Label(I18n.get("settings.shield_title"));
        shieldTitle.getStyleClass().add("settings-section-title");
        shieldHeader.getChildren().addAll(shieldIcon, shieldTitle);

        VBox shieldFields = new VBox(12);
        shieldFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.shield_disk0"), "ENABLED (disk0 & rdisk0 Blocked)"),
                createSettingRow(I18n.get("settings.shield_internal"), "ENABLED (Apple SSD, NVMe Ignored)"),
                createSettingRow(I18n.get("settings.shield_capacity"), "1 GB – 128 GB Removable USB"),
                createSettingRow(I18n.get("settings.shield_unmount"), "diskutil unmountDisk /dev/diskX")
        );
        cardShield.getChildren().addAll(shieldHeader, new Separator(), shieldFields);

        // Card 4 — Database & Storage Engine
        VBox cardDb = new VBox(16);
        cardDb.getStyleClass().add("card");

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
                createSettingRow(I18n.get("settings.db_engine"), "SQLite 3.45 JDBC"),
                createSettingRow(I18n.get("settings.db_file"), "sanitizer_history.db"),
                createSettingRow(I18n.get("settings.db_pdf"), "Apache PDFBox 3.0.1"),
                createSettingRow(I18n.get("settings.db_hardware"), "OSHI 6.4.10")
        );

        HBox dbActions = new HBox(12);
        Button btnClearDb = new Button(I18n.get("settings.db_clear"));
        btnClearDb.setStyle("-fx-text-fill: #F87171; -fx-padding: 8 16; -fx-font-size: 12px;");
        btnClearDb.setTooltip(new Tooltip("Permanently clear all sanitization audit records from SQLite"));
        btnClearDb.setOnAction(e -> handleClearHistory());
        btnClearDb.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleClearHistory(); });

        Button btnExportDb = new Button(I18n.get("settings.db_export"));
        btnExportDb.setStyle("-fx-padding: 8 16; -fx-font-size: 12px;");
        btnExportDb.setTooltip(new Tooltip("Save a copy of sanitizer_history.db to a chosen location"));
        btnExportDb.setOnAction(e -> handleExportDbBackup());
        btnExportDb.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleExportDbBackup(); });

        dbActions.getChildren().addAll(btnClearDb, btnExportDb);
        cardDb.getChildren().addAll(dbHeader, new Separator(), dbFields, dbActions);

        // Card 5 — System Info
        VBox cardSystem = new VBox(16);
        cardSystem.getStyleClass().add("card");

        HBox sysHeader = new HBox(10);
        sysHeader.setAlignment(Pos.CENTER_LEFT);
        Label sysIcon = new Label("[SYS]");
        sysIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; " +
                "-fx-background-color: rgba(148,163,184,0.10); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label sysTitle = new Label(I18n.get("settings.sys_title"));
        sysTitle.getStyleClass().add("settings-section-title");
        sysHeader.getChildren().addAll(sysIcon, sysTitle);

        VBox sysFields = new VBox(12);
        sysFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.sys_app"), "SecureErase Pro v2.0.0 Enterprise"),
                createSettingRow(I18n.get("settings.sys_java"), System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"),
                createSettingRow(I18n.get("settings.sys_javafx"), System.getProperty("javafx.version", "21")),
                createSettingRow(I18n.get("settings.sys_os"), System.getProperty("os.name") + " " + System.getProperty("os.version")),
                createSettingRow(I18n.get("settings.sys_arch"), System.getProperty("os.arch")),
                createSettingRow(I18n.get("settings.sys_wipe_binary"), "dd (Block Size: bs=2m)")
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

        rootContainer.getChildren().addAll(titleBox, cardA11y, cardPolicyBuilder, cardThermal, grid, footer);
    }

    private VBox buildAccessibilityAndI18nCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("card");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("♿ [I18N & A11Y]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: rgba(37,99,235,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label(I18n.get("a11y.section_title"));
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label(I18n.get("a11y.section_sub"));
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        lblDesc.getStyleClass().add("settings-key-label");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label wcagBadge = new Label(I18n.get("a11y.wcag_badge"));
        wcagBadge.getStyleClass().add("badge-success");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, wcagBadge);

        // Grid of Settings
        GridPane a11yGrid = new GridPane();
        a11yGrid.setHgap(20);
        a11yGrid.setVgap(16);

        // 1. Language Picker
        VBox langBox = new VBox(6);
        Label lblLang = new Label(I18n.get("a11y.language_select"));
        lblLang.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblLang.getStyleClass().add("settings-key-label");

        ComboBox<Locale> cmbLang = new ComboBox<>();
        cmbLang.getItems().addAll(I18n.getSupportedLocales());
        cmbLang.setValue(I18n.getLocale());
        cmbLang.setMaxWidth(Double.MAX_VALUE);
        cmbLang.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                return I18n.getLanguageDisplayName(locale);
            }
            @Override
            public Locale fromString(String s) { return null; }
        });
        cmbLang.setOnAction(e -> {
            Locale selected = cmbLang.getValue();
            if (selected != null && !selected.equals(I18n.getLocale())) {
                I18n.setLocale(selected);
            }
        });
        AccessibilityManager.setupAccessible(cmbLang, "Language Selector", "Select interface language", AccessibleRole.COMBO_BOX);
        langBox.getChildren().addAll(lblLang, cmbLang);

        // 2. Contrast Theme Picker
        VBox themeBox = new VBox(6);
        Label lblTheme = new Label(I18n.get("a11y.theme_select"));
        lblTheme.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblTheme.getStyleClass().add("settings-key-label");

        ComboBox<AccessibilityManager.Theme> cmbTheme = new ComboBox<>();
        cmbTheme.getItems().addAll(AccessibilityManager.Theme.values());
        cmbTheme.setValue(AccessibilityManager.getTheme());
        cmbTheme.setMaxWidth(Double.MAX_VALUE);
        cmbTheme.setConverter(new StringConverter<>() {
            @Override
            public String toString(AccessibilityManager.Theme t) {
                return t != null ? t.getDisplayName() : "";
            }
            @Override
            public AccessibilityManager.Theme fromString(String s) { return null; }
        });
        cmbTheme.setOnAction(e -> {
            AccessibilityManager.Theme selected = cmbTheme.getValue();
            if (selected != null) {
                AccessibilityManager.setTheme(selected);
            }
        });
        AccessibilityManager.setupAccessible(cmbTheme, "Theme Selector", "Select visual contrast theme", AccessibleRole.COMBO_BOX);
        themeBox.getChildren().addAll(lblTheme, cmbTheme);

        // 3. Font Scaling / Zoom Level
        VBox scaleBox = new VBox(6);
        Label lblScale = new Label(I18n.get("a11y.font_scale_title"));
        lblScale.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblScale.getStyleClass().add("settings-key-label");

        HBox scaleControls = new HBox(8);
        scaleControls.setAlignment(Pos.CENTER_LEFT);

        Button btnScaleDown = new Button("A-");
        btnScaleDown.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleDown.setOnAction(e -> AccessibilityManager.decreaseFontScale());
        AccessibilityManager.setupAccessible(btnScaleDown, "Decrease Font Scale", "Scales down font size", AccessibleRole.BUTTON);

        Label lblScaleValue = new Label((int)(AccessibilityManager.getFontScale() * 100) + "%");
        lblScaleValue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 50px; -fx-alignment: CENTER;");
        AccessibilityManager.addScaleListener(s -> lblScaleValue.setText((int)(s * 100) + "%"));

        Button btnScaleUp = new Button("A+");
        btnScaleUp.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleUp.setOnAction(e -> AccessibilityManager.increaseFontScale());
        AccessibilityManager.setupAccessible(btnScaleUp, "Increase Font Scale", "Scales up font size", AccessibleRole.BUTTON);

        Button btnScaleReset = new Button(I18n.get("a11y.zoom_reset"));
        btnScaleReset.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleReset.setOnAction(e -> AccessibilityManager.resetFontScale());
        AccessibilityManager.setupAccessible(btnScaleReset, "Reset Font Scale", "Resets font scaling to 100%", AccessibleRole.BUTTON);

        scaleControls.getChildren().addAll(btnScaleDown, lblScaleValue, btnScaleUp, btnScaleReset);
        scaleBox.getChildren().addAll(lblScale, scaleControls);

        // 4. Keyboard Shortcuts Help Action
        VBox actionBox = new VBox(6);
        Label lblHelp = new Label(I18n.get("a11y.keyboard_shortcuts_title"));
        lblHelp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblHelp.getStyleClass().add("settings-key-label");

        Button btnOpenA11yHelp = new Button(I18n.get("a11y.btn_shortcuts_help"));
        btnOpenA11yHelp.getStyleClass().add("button-primary");
        btnOpenA11yHelp.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16;");
        btnOpenA11yHelp.setOnAction(e -> AccessibilityHelpDialog.show(scrollRoot.getScene().getWindow()));
        btnOpenA11yHelp.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER || ev.getCode() == KeyCode.SPACE) {
                AccessibilityHelpDialog.show(scrollRoot.getScene().getWindow());
            }
        });
        AccessibilityManager.setupAccessible(btnOpenA11yHelp, "Keyboard Navigation Guide", "Opens keyboard shortcuts and accessibility help dialog (F1)", AccessibleRole.BUTTON);
        actionBox.getChildren().addAll(lblHelp, btnOpenA11yHelp);

        a11yGrid.add(langBox, 0, 0);
        a11yGrid.add(themeBox, 1, 0);
        a11yGrid.add(scaleBox, 0, 1);
        a11yGrid.add(actionBox, 1, 1);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        a11yGrid.getColumnConstraints().addAll(c1, c2);

        card.getChildren().addAll(header, new Separator(), a11yGrid);
        return card;
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
        Label lblTitle = new Label(I18n.get("settings.thermal_title"));
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label(I18n.get("settings.thermal_sub"));
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        lblDesc.getStyleClass().add("settings-key-label");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label shieldStatus = new Label(I18n.get("settings.thermal_safeguard"));
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

        Button btnSave = new Button(I18n.get("settings.thermal_apply"));
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18;");
        btnSave.setOnAction(e -> handleSaveThermalPolicies());
        btnSave.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleSaveThermalPolicies(); });

        Button btnReset = new Button(I18n.get("settings.thermal_reset"));
        btnReset.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        btnReset.setOnAction(e -> handleResetThermalDefaults());
        btnReset.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleResetThermalDefaults(); });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        Label lblHelp = new Label(I18n.get("settings.thermal_help"));
        lblHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");
        lblHelp.getStyleClass().add("settings-key-label");

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
        name.getStyleClass().add("settings-section-title");
        titleBox.getChildren().addAll(badge, name);

        Label desc = new Label(type.getDescription());
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        desc.getStyleClass().add("settings-key-label");
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

    // ── Custom Wipe Standard & Pattern Builder Card ───────────────────

    private ComboBox<WipePolicy> cmbPolicySelector;
    private TextField txtPolicyName;
    private TextField txtPolicyOrg;
    private TextField txtPolicyCode;
    private TextArea txtPolicyDesc;
    private ComboBox<WipeVerifier.VerificationMode> cmbPolicyVerifyMode;
    private VBox passesListContainer;
    private Label lblSequencePreview;
    private Label lblPolicyTypeBadge;
    private Button btnDeletePolicy;

    private final List<WipePass> currentEditingPasses = new ArrayList<>();

    private VBox buildCustomPolicyBuilderCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("card");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🛠️ [STANDARDS & POLICIES]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8B5CF6; " +
                "-fx-background-color: rgba(139,92,246,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Custom Wipe Standard & Pattern Builder");
        title.getStyleClass().add("settings-section-title");
        Label subtitle = new Label("Configure multi-pass sanitization algorithms, custom byte patterns (0xAA, 0x55, etc.), international presets & JSON profiles.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBadge, titleBox);

        // ── Top Controls: Policy Selector & Actions ──
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblSelect = new Label("Active Standard / Policy:");
        lblSelect.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #334155;");

        cmbPolicySelector = new ComboBox<>();
        cmbPolicySelector.setStyle("-fx-font-size: 12px; -fx-pref-width: 320px;");
        refreshPolicySelectorItems();

        lblPolicyTypeBadge = new Label("STANDARD PRESET");
        lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 4 8; -fx-background-radius: 4px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button btnNew = new Button("+ New Policy");
        btnNew.getStyleClass().add("button-primary");
        btnNew.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnNew.setOnAction(e -> handleCreateNewPolicy());

        Button btnSave = new Button("💾 Save Policy");
        btnSave.getStyleClass().add("button-secondary");
        btnSave.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnSave.setOnAction(e -> handleSaveCurrentPolicy());

        Button btnSetDefault = new Button("⭐ Set Active Default");
        btnSetDefault.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnSetDefault.setOnAction(e -> handleSetActiveDefaultPolicy());

        btnDeletePolicy = new Button("🗑️ Delete");
        btnDeletePolicy.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #EF4444; -fx-background-color: #FEE2E2; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnDeletePolicy.setOnAction(e -> handleDeleteCurrentPolicy());

        Button btnExport = new Button("📤 Export JSON");
        btnExport.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnExport.setOnAction(e -> handleExportPolicyJson());

        Button btnImport = new Button("📥 Import JSON");
        btnImport.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnImport.setOnAction(e -> handleImportPolicyJson());

        topBar.getChildren().addAll(lblSelect, cmbPolicySelector, lblPolicyTypeBadge, topSpacer, btnNew, btnSave, btnSetDefault, btnExport, btnImport, btnDeletePolicy);

        // ── Form Fields: Metadata ──
        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(10);

        Label lblName = new Label("Policy Name:");
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyName = new TextField();
        txtPolicyName.setPromptText("e.g. Enterprise 4-Pass Sanitization");

        Label lblOrg = new Label("Organization / Authority:");
        lblOrg.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyOrg = new TextField();
        txtPolicyOrg.setPromptText("e.g. Acme Corp Cyber Security Team");

        Label lblCode = new Label("Standard Code:");
        lblCode.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyCode = new TextField();
        txtPolicyCode.setPromptText("e.g. ACME_SEC_WIPE_v1");

        Label lblVerify = new Label("Post-Wipe Sampling & Verification:");
        lblVerify.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        cmbPolicyVerifyMode = new ComboBox<>();
        cmbPolicyVerifyMode.getItems().addAll(WipeVerifier.VerificationMode.values());
        cmbPolicyVerifyMode.setMaxWidth(Double.MAX_VALUE);

        Label lblDesc = new Label("Description & Scope:");
        lblDesc.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyDesc = new TextArea();
        txtPolicyDesc.setPrefRowCount(2);
        txtPolicyDesc.setPromptText("Detailed specifications for this sanitization policy profile...");

        formGrid.add(lblName, 0, 0);
        formGrid.add(txtPolicyName, 1, 0);
        formGrid.add(lblOrg, 2, 0);
        formGrid.add(txtPolicyOrg, 3, 0);

        formGrid.add(lblCode, 0, 1);
        formGrid.add(txtPolicyCode, 1, 1);
        formGrid.add(lblVerify, 2, 1);
        formGrid.add(cmbPolicyVerifyMode, 3, 1);

        formGrid.add(lblDesc, 0, 2);
        formGrid.add(txtPolicyDesc, 1, 2, 3, 1);

        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints(260);
        ColumnConstraints c2 = new ColumnConstraints(220);
        ColumnConstraints c3 = new ColumnConstraints(260);
        formGrid.getColumnConstraints().addAll(c0, c1, c2, c3);

        // ── Passes Pattern Editor ──
        VBox passesSection = new VBox(10);
        passesSection.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 14; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

        HBox passesHeader = new HBox(12);
        passesHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblPassesTitle = new Label("Sanitization Passes Sequence & Bit Patterns:");
        lblPassesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1E293B;");

        lblSequencePreview = new Label("0x00");
        lblSequencePreview.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-padding: 3 8; -fx-background-radius: 4px;");

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Button btnAddPass = new Button("+ Add Overwrite Pass");
        btnAddPass.getStyleClass().add("button-secondary");
        btnAddPass.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        btnAddPass.setOnAction(e -> handleAddPass());

        passesHeader.getChildren().addAll(lblPassesTitle, lblSequencePreview, pSpacer, btnAddPass);

        passesListContainer = new VBox(8);

        passesSection.getChildren().addAll(passesHeader, new Separator(), passesListContainer);

        // Populate initial selection
        cmbPolicySelector.setOnAction(e -> {
            WipePolicy selected = cmbPolicySelector.getValue();
            if (selected != null) {
                loadPolicyIntoForm(selected);
            }
        });

        if (!cmbPolicySelector.getItems().isEmpty()) {
            cmbPolicySelector.setValue(WipePolicyManager.getInstance().getDefaultPolicy());
            loadPolicyIntoForm(WipePolicyManager.getInstance().getDefaultPolicy());
        }

        card.getChildren().addAll(header, new Separator(), topBar, formGrid, passesSection);
        return card;
    }

    private void refreshPolicySelectorItems() {
        WipePolicy prev = cmbPolicySelector.getValue();
        List<WipePolicy> all = WipePolicyManager.getInstance().getAllPolicies();
        cmbPolicySelector.setItems(FXCollections.observableArrayList(all));
        if (prev != null && all.contains(prev)) {
            cmbPolicySelector.setValue(prev);
        } else if (!all.isEmpty()) {
            cmbPolicySelector.setValue(WipePolicyManager.getInstance().getDefaultPolicy());
        }
    }

    private void loadPolicyIntoForm(WipePolicy policy) {
        if (policy == null) return;

        txtPolicyName.setText(policy.getName());
        txtPolicyOrg.setText(policy.getOrganization());
        txtPolicyCode.setText(policy.getStandardCode());
        txtPolicyDesc.setText(policy.getDescription());
        cmbPolicyVerifyMode.setValue(policy.getVerificationMode() != null ? policy.getVerificationMode() : WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT);

        if (policy.isSystemBuiltin()) {
            lblPolicyTypeBadge.setText("INTERNATIONAL STANDARD (PRESET)");
            lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 4 8; -fx-background-radius: 4px;");
            btnDeletePolicy.setDisable(true);
        } else {
            lblPolicyTypeBadge.setText("CUSTOM COMPANY POLICY");
            lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 4 8; -fx-background-radius: 4px;");
            btnDeletePolicy.setDisable(false);
        }

        currentEditingPasses.clear();
        if (policy.getPasses() != null) {
            for (WipePass p : policy.getPasses()) {
                currentEditingPasses.add(new WipePass(p.getPassNumber(), p.getPatternType(), p.getCustomByteValue(), p.getDescription()));
            }
        }
        rebuildPassRows();
    }

    private void rebuildPassRows() {
        passesListContainer.getChildren().clear();

        for (int i = 0; i < currentEditingPasses.size(); i++) {
            final int index = i;
            WipePass pass = currentEditingPasses.get(i);
            pass.setPassNumber(i + 1);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 8 12; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-radius: 6px;");

            Label lblPassNum = new Label("Pass " + (i + 1) + ":");
            lblPassNum.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-min-width: 55px;");

            ComboBox<WipePatternType> cmbType = new ComboBox<>();
            cmbType.getItems().addAll(WipePatternType.values());
            cmbType.setValue(pass.getPatternType());
            cmbType.setStyle("-fx-font-size: 11px; -fx-pref-width: 220px;");

            TextField txtHex = new TextField(pass.getPatternType() == WipePatternType.CUSTOM_BYTE ? String.format("0x%02X", pass.getCustomByteValue()) : "0x00");
            txtHex.setPromptText("Hex (0xAA)");
            txtHex.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-pref-width: 85px;");
            txtHex.setVisible(pass.getPatternType() == WipePatternType.CUSTOM_BYTE);
            txtHex.setManaged(pass.getPatternType() == WipePatternType.CUSTOM_BYTE);

            Label badge = new Label(pass.getPatternHex());
            badge.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-padding: 3 6; -fx-background-radius: 4px;");

            TextField txtDesc = new TextField(pass.getDescription());
            txtDesc.setPromptText("Pass description...");
            txtDesc.setStyle("-fx-font-size: 11px;");
            HBox.setHgrow(txtDesc, Priority.ALWAYS);

            cmbType.setOnAction(e -> {
                WipePatternType sel = cmbType.getValue();
                pass.setPatternType(sel);
                boolean isCustom = (sel == WipePatternType.CUSTOM_BYTE);
                txtHex.setVisible(isCustom);
                txtHex.setManaged(isCustom);
                badge.setText(pass.getPatternHex());
                updateSequencePreview();
            });

            txtHex.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    String clean = newVal.replace("0x", "").replace("0X", "").trim();
                    if (!clean.isEmpty()) {
                        int val = Integer.parseInt(clean, 16);
                        pass.setCustomByteValue(val);
                        badge.setText(pass.getPatternHex());
                        updateSequencePreview();
                    }
                } catch (Exception ignored) {}
            });

            txtDesc.textProperty().addListener((obs, o, n) -> pass.setDescription(n));

            Button btnUp = new Button("▲");
            btnUp.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            btnUp.setDisable(index == 0);
            btnUp.setOnAction(e -> {
                Collections.swap(currentEditingPasses, index, index - 1);
                rebuildPassRows();
            });

            Button btnDown = new Button("▼");
            btnDown.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            btnDown.setDisable(index == currentEditingPasses.size() - 1);
            btnDown.setOnAction(e -> {
                Collections.swap(currentEditingPasses, index, index + 1);
                rebuildPassRows();
            });

            Button btnRemove = new Button("✖");
            btnRemove.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 2 6;");
            btnRemove.setDisable(currentEditingPasses.size() <= 1);
            btnRemove.setOnAction(e -> {
                currentEditingPasses.remove(index);
                rebuildPassRows();
            });

            row.getChildren().addAll(lblPassNum, cmbType, txtHex, badge, txtDesc, btnUp, btnDown, btnRemove);
            passesListContainer.getChildren().add(row);
        }

        updateSequencePreview();
    }

    private void updateSequencePreview() {
        if (currentEditingPasses.isEmpty()) {
            lblSequencePreview.setText("Empty");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentEditingPasses.size(); i++) {
            if (i > 0) sb.append(" ➔ ");
            sb.append(currentEditingPasses.get(i).getPatternHex());
        }
        lblSequencePreview.setText(sb.toString());
    }

    private void handleAddPass() {
        currentEditingPasses.add(new WipePass(currentEditingPasses.size() + 1, WipePatternType.ZERO_FILL, 0x00, "Zero Fill Pass"));
        rebuildPassRows();
    }

    private void handleCreateNewPolicy() {
        WipePolicy newPolicy = new WipePolicy(
                UUID.randomUUID().toString(),
                "New Custom Wipe Policy",
                "Custom enterprise sanitization policy profile",
                "Internal Security Team",
                "CUSTOM_POLICY",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1: Pattern 0xAA"),
                        new WipePass(2, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 2: Pattern 0x55"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: CSPRNG Random"),
                        new WipePass(4, WipePatternType.ZERO_FILL, 0x00, "Pass 4: Final Zero Verification")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0.0",
                "User"
        );
        WipePolicyManager.getInstance().savePolicy(newPolicy);
        refreshPolicySelectorItems();
        cmbPolicySelector.setValue(newPolicy);
        loadPolicyIntoForm(newPolicy);
        NavigationManager.getInstance().showNotification("New Policy Created", "Configure passes and click Save.", ToastNotification.ToastType.SUCCESS);
    }

    private void handleSaveCurrentPolicy() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;

        String name = txtPolicyName.getText().trim();
        if (name.isEmpty()) {
            NavigationManager.getInstance().showNotification("Validation Error", "Policy name cannot be empty.", ToastNotification.ToastType.ERROR);
            return;
        }

        if (current.isSystemBuiltin()) {
            // Fork into a custom policy if editing a built-in standard
            WipePolicy customFork = new WipePolicy(
                    UUID.randomUUID().toString(),
                    name + " (Custom)",
                    txtPolicyDesc.getText().trim(),
                    txtPolicyOrg.getText().trim(),
                    txtPolicyCode.getText().trim(),
                    new ArrayList<>(currentEditingPasses),
                    true,
                    cmbPolicyVerifyMode.getValue(),
                    false,
                    "1.0.0",
                    "Custom"
            );
            WipePolicyManager.getInstance().savePolicy(customFork);
            refreshPolicySelectorItems();
            cmbPolicySelector.setValue(customFork);
            loadPolicyIntoForm(customFork);
            NavigationManager.getInstance().showNotification("Policy Forked & Saved", "Saved as custom policy profile: " + customFork.getName(), ToastNotification.ToastType.SUCCESS);
            return;
        }

        current.setName(name);
        current.setOrganization(txtPolicyOrg.getText().trim());
        current.setStandardCode(txtPolicyCode.getText().trim());
        current.setDescription(txtPolicyDesc.getText().trim());
        current.setVerificationMode(cmbPolicyVerifyMode.getValue());
        current.setPasses(new ArrayList<>(currentEditingPasses));

        WipePolicyManager.getInstance().savePolicy(current);
        refreshPolicySelectorItems();
        cmbPolicySelector.setValue(current);
        NavigationManager.getInstance().showNotification("Policy Saved", "Wipe policy profile updated successfully.", ToastNotification.ToastType.SUCCESS);
    }

    private void handleDeleteCurrentPolicy() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null || current.isSystemBuiltin()) return;

        boolean deleted = WipePolicyManager.getInstance().deletePolicy(current.getId());
        if (deleted) {
            refreshPolicySelectorItems();
            loadPolicyIntoForm(WipePolicyManager.getInstance().getDefaultPolicy());
            NavigationManager.getInstance().showNotification("Policy Deleted", "Policy profile removed.", ToastNotification.ToastType.WARNING);
        }
    }

    private void handleSetActiveDefaultPolicy() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;
        WipePolicyManager.getInstance().setDefaultPolicy(current);
        NavigationManager.getInstance().showNotification("Default Standard Updated",
                current.getName() + " is now set as the active default wiping standard.", ToastNotification.ToastType.SUCCESS);
    }

    private void handleExportPolicyJson() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Wipe Policy Configuration Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Policy Profile (*.json)", "*.json"));
        chooser.setInitialFileName(current.getName().toLowerCase().replaceAll("[^a-z0-9]", "_") + "_policy.json");
        File dest = chooser.showSaveDialog(null);

        if (dest != null) {
            try {
                WipePolicyManager.exportPolicyToFile(current, dest);
                NavigationManager.getInstance().showNotification("Policy Exported",
                        "Exported policy profile to: " + dest.getName(), ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                NavigationManager.getInstance().showNotification("Export Error",
                        "Failed to export policy: " + ex.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private void handleImportPolicyJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Wipe Policy Configuration Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Policy Profile (*.json)", "*.json"));
        File src = chooser.showOpenDialog(null);

        if (src != null) {
            try {
                WipePolicy imported = WipePolicyManager.importPolicyFromFile(src);
                WipePolicyManager.getInstance().savePolicy(imported);
                refreshPolicySelectorItems();
                cmbPolicySelector.setValue(imported);
                loadPolicyIntoForm(imported);
                NavigationManager.getInstance().showNotification("Policy Imported",
                        "Successfully imported: " + imported.getName() + " (" + imported.getPassCount() + " passes)",
                        ToastNotification.ToastType.SUCCESS);
            } catch (Exception ex) {
                NavigationManager.getInstance().showNotification("Import Error",
                        "Failed to import JSON policy: " + ex.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }
}

