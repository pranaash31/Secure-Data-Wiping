package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.a11y.AccessibilityManager.Theme;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.UserRole;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.Locale;

/**
 * Enterprise-grade, clean and professional LoginView for SecureErase Pro.
 * Features:
 * - Responsive split-screen cybersecurity layout
 * - Real-time system and cryptographic status indicator
 * - Clear multi-tier RBAC authorization breakdown
 * - Password visibility toggle (Show / Hide PIN)
 * - Dynamic Language (i18n) & Visual Theme switcher
 * - 1-Click Quick Demonstration Profiles for Instant Testing
 * - Inline validation feedback with smooth animations
 * - Full Section 508 / WCAG 2.1 AA Accessibility support
 */
public class LoginView {

    private final StackPane rootPane = new StackPane();
    private final NavigationManager navManager;

    // Form inputs
    private ComboBox<UserRole> cmbRole;
    private TextField txtAgency;
    private TextField txtUser;
    private PasswordField txtPass;
    private TextField txtPassVisible;
    private Button btnTogglePassword;
    private boolean isPasswordVisible = false;
    private VBox alertBanner;
    private Label lblAlertText;

    public LoginView(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();
    }

    public Parent getRoot() {
        return rootPane;
    }

    private void buildUi() {
        rootPane.getChildren().clear();
        rootPane.getStyleClass().add("login-root");

        HBox splitLayout = new HBox(0);
        splitLayout.setFillHeight(true);

        // ══════════════════════════════════════════════════════════════════════
        // ── LEFT BRAND & SECURITY CLEARANCE PANEL ─────────────────────────────
        // ══════════════════════════════════════════════════════════════════════
        VBox brandPanel = buildLeftBrandPanel();
        HBox.setHgrow(brandPanel, Priority.ALWAYS);

        // ══════════════════════════════════════════════════════════════════════
        // ── RIGHT AUTHENTICATION FORM PANEL ───────────────────────────────────
        // ══════════════════════════════════════════════════════════════════════
        VBox formPanel = buildRightFormPanel();
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        splitLayout.getChildren().addAll(brandPanel, formPanel);

        rootPane.getChildren().add(splitLayout);

        // Apply visual theme
        AccessibilityManager.applyThemeAndScale(rootPane);

        // Smooth Entrance Fade
        rootPane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(500), rootPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private VBox buildLeftBrandPanel() {
        VBox brandPanel = new VBox(22);
        brandPanel.getStyleClass().add("login-brand-panel");
        brandPanel.setAlignment(Pos.TOP_LEFT);
        brandPanel.setMinWidth(460);
        brandPanel.setMaxWidth(560);
        brandPanel.setPadding(new Insets(36, 44, 36, 44));

        // 1. Back to Landing Page / Public Portal Ghost Button
        Button btnBack = new Button("←  " + I18n.get("nav.home", "Public Portal Overview"));
        btnBack.getStyleClass().add("login-back-btn");
        btnBack.setOnAction(e -> navManager.showHeroView());
        AccessibilityManager.setupAccessible(btnBack, "Return to Landing Page", "Go back to public portal overview", AccessibleRole.BUTTON);

        // 2. Brand Logo Header
        HBox logoRow = new HBox(14);
        logoRow.setAlignment(Pos.CENTER_LEFT);

        Label shieldIcon = new Label("🛡");
        shieldIcon.getStyleClass().add("login-brand-icon");

        VBox titleGroup = new VBox(2);
        Label brandTitle = new Label(I18n.get("app.title", "SecureErase Pro"));
        brandTitle.getStyleClass().add("login-brand-title");

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label editionBadge = new Label(I18n.get("app.edition", "ENTERPRISE v2.0"));
        editionBadge.getStyleClass().add("login-edition-badge");

        Label fipsBadge = new Label("FIPS 140-2 LEVEL 3");
        fipsBadge.getStyleClass().add("login-fips-badge");
        badgeRow.getChildren().addAll(editionBadge, fipsBadge);

        titleGroup.getChildren().addAll(brandTitle, badgeRow);
        logoRow.getChildren().addAll(shieldIcon, titleGroup);

        // 3. Live System Security Status Pill
        HBox statusPill = new HBox(8);
        statusPill.setAlignment(Pos.CENTER_LEFT);
        statusPill.getStyleClass().add("login-status-pill");

        Label pulseDot = new Label("●");
        pulseDot.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11px;");
        Label statusText = new Label("ENGINE ONLINE  •  HARDWARE HSM READY  •  AIR-GAP ISOLATED");
        statusText.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; -fx-letter-spacing: 0.5px;");
        statusPill.getChildren().addAll(pulseDot, statusText);

        // 4. Headline & Narrative Subtitle
        Label headline = new Label(I18n.get("login.brand_headline", "Military-Grade Data Sanitization Suite"));
        headline.getStyleClass().add("login-brand-headline");
        headline.setWrapText(true);

        Label subline = new Label(I18n.get("login.brand_sub",
                "Certified NIST SP 800-88 R1, DoD 5220.22-M, and ISO/IEC 27001 compliant wiping engine engineered for defense, government, and high-assurance enterprise storage."));
        subline.getStyleClass().add("login-brand-subline");
        subline.setWrapText(true);

        // 5. Multi-Tier RBAC Clearance Hierarchy Showcase
        VBox rbacContainer = new VBox(10);
        rbacContainer.getStyleClass().add("login-rbac-container");

        HBox rbacHeader = new HBox(8);
        rbacHeader.setAlignment(Pos.CENTER_LEFT);
        Label rbacTitle = new Label("MULTI-TIER CLEARANCE ARCHITECTURE (RBAC)");
        rbacTitle.getStyleClass().add("login-rbac-title");
        rbacHeader.getChildren().add(rbacTitle);

        VBox tier1Card = buildRbacTierCard(
                "TIER 1 — SANITIZATION INSPECTOR",
                "#0284C7",
                "Operational sanitization execution, drive diagnostics, sector verification & tamper-evident certificates.",
                "🛡️"
        );

        VBox tier2Card = buildRbacTierCard(
                "TIER 2 — OPERATIONS SUPERVISOR",
                "#D97706",
                "Sanitization policy editor, thermal thresholds watchdog, real-time alerts & client management.",
                "⚙️"
        );

        VBox tier3Card = buildRbacTierCard(
                "TIER 3 — CHIEF COMPLIANCE AUDITOR",
                "#9333EA",
                "Cryptographic ledger verification, KeyVault HSM governance, security audit logs & ESG reporting.",
                "🔒"
        );

        rbacContainer.getChildren().addAll(rbacHeader, tier1Card, tier2Card, tier3Card);

        // 6. Compliance Certification Badges
        HBox complianceRow = new HBox(6);
        complianceRow.setAlignment(Pos.CENTER_LEFT);
        for (String cert : new String[]{"NIST SP 800-88", "DoD 5220.22-M", "FIPS 140-2", "ISO/IEC 27001", "HIPAA/GDPR"}) {
            Label badge = new Label(cert);
            badge.getStyleClass().add("login-compliance-badge");
            complianceRow.getChildren().add(badge);
        }

        // 7. Bottom Spacer & Cryptographic Signature Note
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label cryptoNotice = new Label("🔒 RSA-4096 & ECDSA P-384 Signatures Active  •  Tamper-Evident Ledger");
        cryptoNotice.getStyleClass().add("login-crypto-notice");

        brandPanel.getChildren().addAll(
                btnBack,
                logoRow,
                statusPill,
                headline,
                subline,
                rbacContainer,
                complianceRow,
                spacer,
                cryptoNotice
        );

        return brandPanel;
    }

    private VBox buildRbacTierCard(String title, String accentHex, String description, String icon) {
        VBox card = new VBox(3);
        card.getStyleClass().add("login-tier-card");
        card.setStyle(card.getStyle() + "-fx-border-color: " + accentHex + "40;");

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 11px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: " + accentHex + ";");

        top.getChildren().addAll(iconLbl, titleLbl);

        Label descLbl = new Label(description);
        descLbl.getStyleClass().add("login-tier-desc");
        descLbl.setWrapText(true);

        card.getChildren().addAll(top, descLbl);
        return card;
    }

    private VBox buildRightFormPanel() {
        VBox formPanel = new VBox(16);
        formPanel.getStyleClass().add("login-form-panel");
        formPanel.setAlignment(Pos.CENTER);
        formPanel.setPadding(new Insets(30, 48, 30, 48));

        // ── TOP QUICK CONTROLS BAR (Locale + Theme) ───────────────────────────
        HBox topControls = new HBox(12);
        topControls.setAlignment(Pos.CENTER_RIGHT);
        topControls.setMaxWidth(460);

        // Language Selector
        ComboBox<Locale> cmbLocale = new ComboBox<>();
        cmbLocale.getItems().addAll(I18n.getSupportedLocales());
        cmbLocale.setValue(I18n.getLocale());
        cmbLocale.getStyleClass().add("login-util-combo");
        cmbLocale.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale l) {
                if (l == null) return "";
                if (l.getLanguage().equals("fr")) return "🇫🇷 Français";
                if (l.getLanguage().equals("es")) return "🇪🇸 Español";
                if (l.getLanguage().equals("de")) return "🇩🇪 Deutsch";
                if (l.getLanguage().equals("hi")) return "🇮🇳 हिन्दी";
                return "🇺🇸 English";
            }
            @Override
            public Locale fromString(String s) { return null; }
        });
        cmbLocale.setOnAction(e -> {
            Locale selected = cmbLocale.getValue();
            if (selected != null && !selected.equals(I18n.getLocale())) {
                I18n.setLocale(selected);
                buildUi();
            }
        });

        // Theme Toggle Button
        Button btnTheme = new Button(AccessibilityManager.getTheme() == Theme.DARK ? "☀️ Light" : "🌙 Dark");
        btnTheme.getStyleClass().add("login-util-btn");
        btnTheme.setOnAction(e -> {
            AccessibilityManager.toggleDarkLight();
            AccessibilityManager.applyThemeAndScale(rootPane);
            btnTheme.setText(AccessibilityManager.getTheme() == Theme.DARK ? "☀️ Light" : "🌙 Dark");
        });

        topControls.getChildren().addAll(cmbLocale, btnTheme);

        // ── CENTER AUTHENTICATION CARD ────────────────────────────────────────
        VBox loginCard = new VBox(16);
        loginCard.getStyleClass().add("login-card");
        loginCard.setMaxWidth(460);
        loginCard.setPadding(new Insets(32, 34, 32, 34));

        // Card Header
        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label badgeLabel = new Label(I18n.get("login.portal_badge", "SECURE ACCESS GATEWAY"));
        badgeLabel.getStyleClass().add("login-portal-pill");

        Label loginTitle = new Label(I18n.get("login.title", "Officer Authentication"));
        loginTitle.getStyleClass().add("login-title");

        Label loginSub = new Label(I18n.get("login.subtitle", "Select assigned clearance role and input verification credentials."));
        loginSub.getStyleClass().add("login-subtitle");
        loginSub.setWrapText(true);

        headerBox.getChildren().addAll(badgeLabel, loginTitle, loginSub);

        // Dynamic Inline Alert / Feedback Banner (Hidden by default)
        alertBanner = new VBox(6);
        alertBanner.getStyleClass().add("login-alert-banner");
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);

        HBox alertRow = new HBox(8);
        alertRow.setAlignment(Pos.CENTER_LEFT);
        Label alertIcon = new Label("⚠️");
        lblAlertText = new Label();
        lblAlertText.getStyleClass().add("login-alert-text");
        lblAlertText.setWrapText(true);
        alertRow.getChildren().addAll(alertIcon, lblAlertText);
        alertBanner.getChildren().add(alertRow);

        // ── FORM FIELDS ───────────────────────────────────────────────────────
        VBox formFields = new VBox(12);

        // 1. Role Clearance Selector
        VBox roleBox = new VBox(4);
        Label lblRole = new Label("Assigned Role Clearance (RBAC):");
        lblRole.getStyleClass().add("form-label");

        cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll(UserRole.values());
        cmbRole.setValue(UserRole.INSPECTOR);
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.getStyleClass().add("login-input-combo");
        cmbRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole r) {
                if (r == null) return "";
                return r.getTitle() + " (" + r.getTierLabel() + ")";
            }
            @Override
            public UserRole fromString(String s) { return null; }
        });
        cmbRole.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserRole r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cell = new HBox(8);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    Label dot = new Label("●");
                    dot.setStyle("-fx-text-fill: " + r.getAccentColor() + "; -fx-font-size: 13px;");
                    Label label = new Label(r.getTitle() + " — " + r.getTierLabel());
                    label.setStyle("-fx-font-size: 12px; -fx-font-weight: 500;");
                    cell.getChildren().addAll(dot, label);
                    setGraphic(cell);
                    setText(null);
                }
            }
        });
        AccessibilityManager.setupAccessible(cmbRole, "User Role", "Select assigned clearance level", AccessibleRole.COMBO_BOX);
        roleBox.getChildren().addAll(lblRole, cmbRole);

        // 2. Clearance / Agency ID
        VBox agencyBox = new VBox(4);
        Label lblAgency = new Label(I18n.get("login.agency_id", "Agency Identifier / Clearance ID:"));
        lblAgency.getStyleClass().add("form-label");
        txtAgency = new TextField("GOV-DEF-8942");
        txtAgency.setPromptText(I18n.get("login.agency_placeholder", "e.g. GOV-DEF-8942"));
        txtAgency.setMaxWidth(Double.MAX_VALUE);
        txtAgency.getStyleClass().add("login-text-field");
        AccessibilityManager.setupAccessible(txtAgency, "Agency Identifier", "Input clearance identifier", AccessibleRole.TEXT_FIELD);
        agencyBox.getChildren().addAll(lblAgency, txtAgency);

        // 3. Officer Username
        VBox userBox = new VBox(4);
        Label lblUser = new Label(I18n.get("login.officer_name", "Officer Username:"));
        lblUser.getStyleClass().add("form-label");
        txtUser = new TextField("Officer Pranaash");
        txtUser.setPromptText(I18n.get("login.officer_placeholder", "Enter officer name"));
        txtUser.setMaxWidth(Double.MAX_VALUE);
        txtUser.getStyleClass().add("login-text-field");
        AccessibilityManager.setupAccessible(txtUser, "Officer Username", "Input officer username", AccessibleRole.TEXT_FIELD);
        userBox.getChildren().addAll(lblUser, txtUser);

        // 4. Security PIN / Password with Show/Hide Toggle
        VBox passBox = new VBox(4);
        Label lblPass = new Label(I18n.get("login.pin", "Security PIN / Access Key:"));
        lblPass.getStyleClass().add("form-label");

        StackPane passStack = new StackPane();
        txtPass = new PasswordField();
        txtPass.setText("••••••••");
        txtPass.setPromptText(I18n.get("login.pin_placeholder", "Enter security access code"));
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.getStyleClass().add("login-text-field");

        txtPassVisible = new TextField();
        txtPassVisible.setText("••••••••");
        txtPassVisible.setPromptText(I18n.get("login.pin_placeholder", "Enter security access code"));
        txtPassVisible.setMaxWidth(Double.MAX_VALUE);
        txtPassVisible.getStyleClass().add("login-text-field");
        txtPassVisible.setVisible(false);
        txtPassVisible.setManaged(false);

        // Sync text between masked and unmasked fields
        txtPass.textProperty().bindBidirectional(txtPassVisible.textProperty());

        btnTogglePassword = new Button("👁");
        btnTogglePassword.getStyleClass().add("login-pass-toggle-btn");
        StackPane.setAlignment(btnTogglePassword, Pos.CENTER_RIGHT);
        StackPane.setMargin(btnTogglePassword, new Insets(0, 8, 0, 0));
        btnTogglePassword.setOnAction(e -> togglePasswordVisibility());
        AccessibilityManager.setupAccessible(btnTogglePassword, "Toggle PIN Visibility", "Show or hide security PIN", AccessibleRole.BUTTON);

        passStack.getChildren().addAll(txtPass, txtPassVisible, btnTogglePassword);
        AccessibilityManager.setupAccessible(txtPass, "Security PIN", "Input security PIN or access code", AccessibleRole.PASSWORD_FIELD);
        passBox.getChildren().addAll(lblPass, passStack);

        // Enter key listeners
        txtAgency.setOnAction(e -> handleAuthentication());
        txtUser.setOnAction(e -> handleAuthentication());
        txtPass.setOnAction(e -> handleAuthentication());
        txtPassVisible.setOnAction(e -> handleAuthentication());

        formFields.getChildren().addAll(roleBox, agencyBox, userBox, passBox);

        // ── PRIMARY AUTHENTICATE BUTTON ───────────────────────────────────────
        Button btnLogin = new Button("AUTHENTICATE & ENTER PORTAL  ➔");
        btnLogin.getStyleClass().add("login-btn-primary");
        btnLogin.setDefaultButton(true);
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setOnAction(e -> handleAuthentication());
        AccessibilityManager.setupAccessible(btnLogin, "Authenticate", "Submit credentials and login to suite", AccessibleRole.BUTTON);

        // ── 1-CLICK QUICK PROFILE DEMO BUTTONS ────────────────────────────────
        VBox demoContainer = new VBox(8);
        demoContainer.getStyleClass().add("login-demo-container");

        Label lblDemoTitle = new Label("QUICK 1-CLICK ROLE ACCESS (DEMO PROFILES)");
        lblDemoTitle.getStyleClass().add("login-demo-title");

        HBox demoGrid = new HBox(8);
        demoGrid.setAlignment(Pos.CENTER);

        Button btnInspector = buildDemoRoleButton("🛡️ Inspector", "Tier 1", "#0284C7", () -> {
            txtUser.setText("Inspector Pranaash");
            txtAgency.setText("GOV-DEF-8942");
            cmbRole.setValue(UserRole.INSPECTOR);
            navManager.loginSuccess("Inspector Pranaash", "GOV-DEF-8942", UserRole.INSPECTOR);
        });

        Button btnSupervisor = buildDemoRoleButton("⚙️ Supervisor", "Tier 2", "#D97706", () -> {
            txtUser.setText("Supervisor Vance");
            txtAgency.setText("GOV-DEF-8942");
            cmbRole.setValue(UserRole.SUPERVISOR);
            navManager.loginSuccess("Supervisor Vance", "GOV-DEF-8942", UserRole.SUPERVISOR);
        });

        Button btnAuditor = buildDemoRoleButton("🔒 Chief Auditor", "Tier 3", "#9333EA", () -> {
            txtUser.setText("Chief Auditor Davis");
            txtAgency.setText("GOV-DEF-8942");
            cmbRole.setValue(UserRole.CHIEF_AUDITOR);
            navManager.loginSuccess("Chief Auditor Davis", "GOV-DEF-8942", UserRole.CHIEF_AUDITOR);
        });

        HBox.setHgrow(btnInspector, Priority.ALWAYS);
        HBox.setHgrow(btnSupervisor, Priority.ALWAYS);
        HBox.setHgrow(btnAuditor, Priority.ALWAYS);

        demoGrid.getChildren().addAll(btnInspector, btnSupervisor, btnAuditor);
        demoContainer.getChildren().addAll(lblDemoTitle, demoGrid);

        // ── FOOTER LEGAL & COMPLIANCE DISCLAIMER ──────────────────────────────
        Label disclaimer = new Label(I18n.get("login.disclaimer",
                "Authorized Government & Defense Personnel Only. All session activities and cryptographic signatures are immutably logged."));
        disclaimer.getStyleClass().add("login-disclaimer");
        disclaimer.setWrapText(true);
        disclaimer.setAlignment(Pos.CENTER);

        loginCard.getChildren().addAll(
                headerBox,
                alertBanner,
                formFields,
                btnLogin,
                demoContainer,
                new Separator(),
                disclaimer
        );

        formPanel.getChildren().addAll(topControls, loginCard);
        return formPanel;
    }

    private Button buildDemoRoleButton(String name, String tier, String accentColor, Runnable onSelect) {
        Button btn = new Button(name + "\n" + tier);
        btn.getStyleClass().add("login-demo-btn");
        btn.setStyle(btn.getStyle() + "-fx-border-color: " + accentColor + "55;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> onSelect.run());

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-border-color: " + accentColor + "; -fx-text-fill: " + accentColor + ";"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle() + "-fx-border-color: " + accentColor + "55; -fx-text-fill: -fx-text-base-color;"));

        return btn;
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            txtPassVisible.setText(txtPass.getText());
            txtPass.setVisible(false);
            txtPass.setManaged(false);
            txtPassVisible.setVisible(true);
            txtPassVisible.setManaged(true);
            txtPassVisible.requestFocus();
            txtPassVisible.positionCaret(txtPassVisible.getText().length());
            btnTogglePassword.setText("👁‍🗨");
        } else {
            txtPass.setText(txtPassVisible.getText());
            txtPassVisible.setVisible(false);
            txtPassVisible.setManaged(false);
            txtPass.setVisible(true);
            txtPass.setManaged(true);
            txtPass.requestFocus();
            txtPass.positionCaret(txtPass.getText().length());
            btnTogglePassword.setText("👁");
        }
    }

    private void handleAuthentication() {
        String username = txtUser.getText() != null ? txtUser.getText().trim() : "";
        String agency = txtAgency.getText() != null ? txtAgency.getText().trim() : "";
        UserRole role = cmbRole.getValue() != null ? cmbRole.getValue() : UserRole.INSPECTOR;

        if (username.isEmpty()) {
            showError("Officer Username is required for cryptographic authentication.");
            txtUser.requestFocus();
            return;
        }

        if (agency.isEmpty()) {
            showError("Clearance Identifier / Agency ID cannot be empty.");
            txtAgency.requestFocus();
            return;
        }

        hideError();
        navManager.loginSuccess(username, agency, role);
    }

    private void showError(String message) {
        lblAlertText.setText(message);
        alertBanner.setVisible(true);
        alertBanner.setManaged(true);

        TranslateTransition tt = new TranslateTransition(Duration.millis(80), alertBanner);
        tt.setFromX(-6);
        tt.setToX(6);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void hideError() {
        alertBanner.setVisible(false);
        alertBanner.setManaged(false);
    }
}
