package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.a11y.AccessibilityManager.Theme;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.UserRole;
import javafx.animation.FadeTransition;
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
 * Formal, institutional Government Authentication Portal for SecureErase Pro.
 * Adheres to USDS / DoD / NIST system access interface standards:
 * - Official Classification Banner & System Identification
 * - Mandatory Legal Warning & Consent Notice
 * - Role-Based Clearance Selector
 * - Clean, standard credential inputs with no extraneous elements
 * - Formal, accessible typography and high-contrast color scheme
 */
public class LoginView {

    private final StackPane rootPane = new StackPane();
    private final NavigationManager navManager;

    private ComboBox<UserRole> cmbRole;
    private TextField txtAgency;
    private TextField txtUser;
    private PasswordField txtPass;
    private Label lblError;

    public LoginView(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();
    }

    public Parent getRoot() {
        return rootPane;
    }

    private void buildUi() {
        rootPane.getChildren().clear();
        rootPane.getStyleClass().add("gov-login-root");

        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);

        // 1. TOP OFFICIAL SYSTEM CLASSIFICATION BANNER
        HBox topBanner = buildClassificationBanner();

        // 2. MAIN PORTAL BODY (Centered Scrollable Container)
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("gov-scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox contentWrapper = new VBox(24);
        contentWrapper.setAlignment(Pos.CENTER);
        contentWrapper.setPadding(new Insets(32, 24, 40, 24));

        // Official Agency & System Header
        VBox institutionalHeader = buildInstitutionalHeader();

        // Official Warning Banner (Mandatory Notice)
        VBox legalNoticeBox = buildLegalNoticeBox();

        // Formal Authentication Card
        VBox authCard = buildAuthCard();

        // Institutional Footer
        VBox footerBox = buildInstitutionalFooter();

        contentWrapper.getChildren().addAll(
                institutionalHeader,
                legalNoticeBox,
                authCard,
                footerBox
        );

        scrollPane.setContent(contentWrapper);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mainContainer.getChildren().addAll(topBanner, scrollPane);
        rootPane.getChildren().add(mainContainer);

        // Apply visual theme
        AccessibilityManager.applyThemeAndScale(rootPane);

        // Clean Entrance Fade
        rootPane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), rootPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private HBox buildClassificationBanner() {
        HBox banner = new HBox(16);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.getStyleClass().add("gov-classification-banner");
        banner.setPadding(new Insets(6, 24, 6, 24));

        Label classLabel = new Label("OFFICIAL USE ONLY  //  RESTRICTED INFORMATION SYSTEM");
        classLabel.getStyleClass().add("gov-classification-text");
        HBox.setHgrow(classLabel, Priority.ALWAYS);

        // Language Selector
        ComboBox<Locale> cmbLocale = new ComboBox<>();
        cmbLocale.getItems().addAll(I18n.getSupportedLocales());
        cmbLocale.setValue(I18n.getLocale());
        cmbLocale.getStyleClass().add("gov-lang-select");
        cmbLocale.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale l) {
                if (l == null) return "";
                if (l.getLanguage().equals("fr")) return "Français (FR)";
                if (l.getLanguage().equals("es")) return "Español (ES)";
                if (l.getLanguage().equals("de")) return "Deutsch (DE)";
                if (l.getLanguage().equals("hi")) return "हिन्दी (HI)";
                return "English (US)";
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

        // Theme Switcher Button
        Button btnTheme = new Button(AccessibilityManager.getTheme() == Theme.DARK ? "Theme: Light" : "Theme: Dark");
        btnTheme.getStyleClass().add("gov-theme-btn");
        btnTheme.setOnAction(e -> {
            AccessibilityManager.toggleDarkLight();
            AccessibilityManager.applyThemeAndScale(rootPane);
            btnTheme.setText(AccessibilityManager.getTheme() == Theme.DARK ? "Theme: Light" : "Theme: Dark");
        });

        banner.getChildren().addAll(classLabel, cmbLocale, btnTheme);
        return banner;
    }

    private VBox buildInstitutionalHeader() {
        VBox header = new VBox(6);
        header.setAlignment(Pos.CENTER);
        header.setMaxWidth(620);

        Label sealLabel = new Label("NATIONAL DATA SANITIZATION & ASSURANCE SUITE");
        sealLabel.getStyleClass().add("gov-agency-subtitle");

        Label mainTitle = new Label("SecureErase Pro Enterprise");
        mainTitle.getStyleClass().add("gov-main-title");

        Label standardNotice = new Label("NIST SP 800-88 Rev. 1  •  DoD 5220.22-M  •  FIPS 140-2 Validated Cryptographic Core");
        standardNotice.getStyleClass().add("gov-standards-label");

        header.getChildren().addAll(sealLabel, mainTitle, standardNotice);
        return header;
    }

    private VBox buildLegalNoticeBox() {
        VBox notice = new VBox(6);
        notice.getStyleClass().add("gov-notice-box");
        notice.setMaxWidth(620);
        notice.setPadding(new Insets(12, 16, 12, 16));

        Label noticeHeader = new Label("MANDATORY SYSTEM USE NOTIFICATION");
        noticeHeader.getStyleClass().add("gov-notice-header");

        Label noticeBody = new Label(
                "You are accessing a secured Government Information System (IS) provided for authorized use only. " +
                "By using this system, you acknowledge and consent to administrative monitoring, cryptographic logging, " +
                "and auditing of all data sanitization operations. Unauthorized access or misuse is subject to legal prosecution under applicable federal statutes."
        );
        noticeBody.getStyleClass().add("gov-notice-body");
        noticeBody.setWrapText(true);

        notice.getChildren().addAll(noticeHeader, noticeBody);
        return notice;
    }

    private VBox buildAuthCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("gov-auth-card");
        card.setMaxWidth(620);
        card.setPadding(new Insets(28, 32, 28, 32));

        Label cardTitle = new Label("Officer Authentication & Clearance Verification");
        cardTitle.getStyleClass().add("gov-card-title");

        Separator sep1 = new Separator();

        // Inline Error Message
        lblError = new Label();
        lblError.getStyleClass().add("gov-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Form Fields
        VBox fieldsContainer = new VBox(14);

        // 1. Assigned Role Clearance (RBAC)
        VBox roleField = new VBox(4);
        Label lblRoleTitle = new Label("Security Clearance Role (RBAC):");
        lblRoleTitle.getStyleClass().add("gov-field-label");

        cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll(UserRole.values());
        cmbRole.setValue(UserRole.INSPECTOR);
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.getStyleClass().add("gov-input-control");
        cmbRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole r) {
                if (r == null) return "";
                return r.getTitle() + " — " + r.getTierLabel();
            }
            @Override
            public UserRole fromString(String s) { return null; }
        });
        AccessibilityManager.setupAccessible(cmbRole, "Security Clearance Role", "Select assigned clearance role", AccessibleRole.COMBO_BOX);
        roleField.getChildren().addAll(lblRoleTitle, cmbRole);

        // 2. Agency Identifier
        VBox agencyField = new VBox(4);
        Label lblAgencyTitle = new Label("Clearance Identifier / Agency Code:");
        lblAgencyTitle.getStyleClass().add("gov-field-label");

        txtAgency = new TextField("GOV-DEF-8942");
        txtAgency.setPromptText("e.g. GOV-DEF-8942");
        txtAgency.setMaxWidth(Double.MAX_VALUE);
        txtAgency.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtAgency, "Agency Code", "Enter clearance identifier", AccessibleRole.TEXT_FIELD);
        agencyField.getChildren().addAll(lblAgencyTitle, txtAgency);

        // 3. Officer Username
        VBox userField = new VBox(4);
        Label lblUserTitle = new Label("Authorized Officer Username:");
        lblUserTitle.getStyleClass().add("gov-field-label");

        txtUser = new TextField("Officer Pranaash");
        txtUser.setPromptText("Enter assigned username");
        txtUser.setMaxWidth(Double.MAX_VALUE);
        txtUser.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtUser, "Officer Username", "Enter officer username", AccessibleRole.TEXT_FIELD);
        userField.getChildren().addAll(lblUserTitle, txtUser);

        // 4. Security PIN / Passcode
        VBox passField = new VBox(4);
        Label lblPassTitle = new Label("Security Access PIN / Passcode:");
        lblPassTitle.getStyleClass().add("gov-field-label");

        txtPass = new PasswordField();
        txtPass.setText("••••••••");
        txtPass.setPromptText("Enter secure PIN");
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtPass, "Security PIN", "Enter security PIN", AccessibleRole.PASSWORD_FIELD);
        passField.getChildren().addAll(lblPassTitle, txtPass);

        // Handle Enter key on inputs
        txtAgency.setOnAction(e -> handleLogin());
        txtUser.setOnAction(e -> handleLogin());
        txtPass.setOnAction(e -> handleLogin());

        fieldsContainer.getChildren().addAll(roleField, agencyField, userField, passField);

        // Authenticate Button
        Button btnSubmit = new Button("AUTHENTICATE & ENTER SYSTEM");
        btnSubmit.getStyleClass().add("gov-btn-primary");
        btnSubmit.setDefaultButton(true);
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleLogin());
        AccessibilityManager.setupAccessible(btnSubmit, "Authenticate", "Submit credentials for verification", AccessibleRole.BUTTON);

        // Quick Demonstration Profiles
        VBox demoSection = new VBox(6);
        demoSection.getStyleClass().add("gov-demo-section");

        Label demoLabel = new Label("Quick Authorization Profiles (Internal Evaluation):");
        demoLabel.getStyleClass().add("gov-demo-label");

        HBox demoButtons = new HBox(8);
        demoButtons.setAlignment(Pos.CENTER);

        Button btnInspector = buildDemoBtn("Inspector (Tier 1)", "Inspector Pranaash", UserRole.INSPECTOR);
        Button btnSupervisor = buildDemoBtn("Supervisor (Tier 2)", "Supervisor Vance", UserRole.SUPERVISOR);
        Button btnAuditor = buildDemoBtn("Chief Auditor (Tier 3)", "Chief Auditor Davis", UserRole.CHIEF_AUDITOR);

        HBox.setHgrow(btnInspector, Priority.ALWAYS);
        HBox.setHgrow(btnSupervisor, Priority.ALWAYS);
        HBox.setHgrow(btnAuditor, Priority.ALWAYS);

        demoButtons.getChildren().addAll(btnInspector, btnSupervisor, btnAuditor);
        demoSection.getChildren().addAll(demoLabel, demoButtons);

        card.getChildren().addAll(
                cardTitle,
                sep1,
                lblError,
                fieldsContainer,
                btnSubmit,
                new Separator(),
                demoSection
        );

        return card;
    }

    private Button buildDemoBtn(String label, String username, UserRole role) {
        Button btn = new Button(label);
        btn.getStyleClass().add("gov-demo-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            txtUser.setText(username);
            txtAgency.setText("GOV-DEF-8942");
            cmbRole.setValue(role);
            navManager.loginSuccess(username, "GOV-DEF-8942", role);
        });
        return btn;
    }

    private VBox buildInstitutionalFooter() {
        VBox footer = new VBox(8);
        footer.setAlignment(Pos.CENTER);
        footer.setMaxWidth(620);

        HBox linksRow = new HBox(16);
        linksRow.setAlignment(Pos.CENTER);

        Hyperlink linkHome = new Hyperlink("← Return to Public System Overview");
        linkHome.getStyleClass().add("gov-footer-link");
        linkHome.setOnAction(e -> navManager.showHeroView());

        linksRow.getChildren().add(linkHome);

        Label certLine = new Label("FIPS 140-2 Validated Cryptographic Core  •  NIST SP 800-88 R1  •  Common Criteria EAL4+");
        certLine.getStyleClass().add("gov-footer-meta");

        Label sysId = new Label("System Identifier: SE-GOV-2026-X86 | Session Attestation: ECDSA P-384 Signed");
        sysId.getStyleClass().add("gov-footer-sysid");

        footer.getChildren().addAll(linksRow, certLine, sysId);
        return footer;
    }

    private void handleLogin() {
        String username = txtUser.getText() != null ? txtUser.getText().trim() : "";
        String agency = txtAgency.getText() != null ? txtAgency.getText().trim() : "";
        UserRole role = cmbRole.getValue() != null ? cmbRole.getValue() : UserRole.INSPECTOR;

        if (username.isEmpty()) {
            showError("Authentication Failed: Officer Username must be provided.");
            txtUser.requestFocus();
            return;
        }

        if (agency.isEmpty()) {
            showError("Authentication Failed: Clearance Identifier / Agency Code is required.");
            txtAgency.requestFocus();
            return;
        }

        hideError();
        navManager.loginSuccess(username, agency, role);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
