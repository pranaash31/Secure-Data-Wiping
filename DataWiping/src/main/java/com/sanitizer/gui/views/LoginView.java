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
 * Clean, simple, and professional Government Officer Sign-In Portal.
 * Designed for non-technical government personnel:
 * - Clean Federal Blue & White palette
 * - Clear, straightforward 3-step credential layout
 * - Plain-language role descriptions
 * - 1-Click test profile buttons for immediate access
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

        VBox layout = new VBox(0);
        layout.setAlignment(Pos.TOP_CENTER);

        // 1. TOP OFFICIAL HEADER BAR (Federal Blue)
        HBox topBar = buildTopBar();

        // 2. CENTERED CARD CONTAINER
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("gov-scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox contentWrapper = new VBox(20);
        contentWrapper.setAlignment(Pos.CENTER);
        contentWrapper.setPadding(new Insets(36, 24, 40, 24));

        // Heading & Subtitle
        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setMaxWidth(500);

        Label lblTitle = new Label("Officer Sign-In");
        lblTitle.getStyleClass().add("gov-main-title");

        Label lblSubtitle = new Label("Secure Data Sanitization & Assurance System");
        lblSubtitle.getStyleClass().add("gov-standards-label");

        titleBox.getChildren().addAll(lblTitle, lblSubtitle);

        // Sign-In White Card
        VBox card = buildSimpleLoginCard();

        // Bottom Navigation & Info
        VBox footer = buildSimpleFooter();

        contentWrapper.getChildren().addAll(titleBox, card, footer);
        scrollPane.setContent(contentWrapper);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        layout.getChildren().addAll(topBar, scrollPane);
        rootPane.getChildren().add(layout);

        // Apply visual theme
        AccessibilityManager.applyThemeAndScale(rootPane);

        // Smooth fade-in
        rootPane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), rootPane);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("gov-classification-banner");
        topBar.setPadding(new Insets(10, 32, 10, 32));

        Label brand = new Label("SecureErase Pro  |  Official Government Portal");
        brand.getStyleClass().add("gov-classification-text");
        HBox.setHgrow(brand, Priority.ALWAYS);

        // Language Dropdown
        ComboBox<Locale> cmbLocale = new ComboBox<>();
        cmbLocale.getItems().addAll(I18n.getSupportedLocales());
        cmbLocale.setValue(I18n.getLocale());
        cmbLocale.getStyleClass().add("gov-lang-select");
        cmbLocale.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale l) {
                if (l == null) return "";
                if (l.getLanguage().equals("fr")) return "Français";
                if (l.getLanguage().equals("es")) return "Español";
                if (l.getLanguage().equals("de")) return "Deutsch";
                if (l.getLanguage().equals("hi")) return "हिन्दी";
                return "English";
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

        // Theme Toggle
        Button btnTheme = new Button(AccessibilityManager.getTheme() == Theme.DARK ? "Light Mode" : "Dark Mode");
        btnTheme.getStyleClass().add("gov-theme-btn");
        btnTheme.setOnAction(e -> {
            AccessibilityManager.toggleDarkLight();
            AccessibilityManager.applyThemeAndScale(rootPane);
            btnTheme.setText(AccessibilityManager.getTheme() == Theme.DARK ? "Light Mode" : "Dark Mode");
        });

        topBar.getChildren().addAll(brand, cmbLocale, btnTheme);
        return topBar;
    }

    private VBox buildSimpleLoginCard() {
        VBox card = new VBox(18);
        card.getStyleClass().add("gov-auth-card");
        card.setMaxWidth(480);
        card.setPadding(new Insets(32, 36, 32, 36));

        // Error message banner
        lblError = new Label();
        lblError.getStyleClass().add("gov-error-label");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // 1. Role Selection
        VBox roleBox = new VBox(5);
        Label lblRole = new Label("1. Select Your Role:");
        lblRole.getStyleClass().add("gov-field-label");

        cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll(UserRole.values());
        cmbRole.setValue(UserRole.INSPECTOR);
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.getStyleClass().add("gov-input-control");
        cmbRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole r) {
                if (r == null) return "";
                switch (r) {
                    case INSPECTOR:
                        return "Operator / Inspector (Wipe Drives & Diagnostics)";
                    case SUPERVISOR:
                        return "Supervisor (Manage Policies & Settings)";
                    case CHIEF_AUDITOR:
                        return "Compliance Auditor (View Audit Logs & Reports)";
                    default:
                        return r.getTitle();
                }
            }
            @Override
            public UserRole fromString(String s) { return null; }
        });
        AccessibilityManager.setupAccessible(cmbRole, "User Role", "Select assigned clearance role", AccessibleRole.COMBO_BOX);
        roleBox.getChildren().addAll(lblRole, cmbRole);

        // 2. Agency Code
        VBox agencyBox = new VBox(5);
        Label lblAgency = new Label("2. Agency / Department Code:");
        lblAgency.getStyleClass().add("gov-field-label");

        txtAgency = new TextField("GOV-DEF-8942");
        txtAgency.setPromptText("e.g. GOV-DEF-8942");
        txtAgency.setMaxWidth(Double.MAX_VALUE);
        txtAgency.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtAgency, "Agency Code", "Enter agency identifier", AccessibleRole.TEXT_FIELD);
        agencyBox.getChildren().addAll(lblAgency, txtAgency);

        // 3. Officer Name
        VBox userBox = new VBox(5);
        Label lblUser = new Label("3. Officer Username:");
        lblUser.getStyleClass().add("gov-field-label");

        txtUser = new TextField("Officer Pranaash");
        txtUser.setPromptText("Enter your name");
        txtUser.setMaxWidth(Double.MAX_VALUE);
        txtUser.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtUser, "Officer Username", "Enter username", AccessibleRole.TEXT_FIELD);
        userBox.getChildren().addAll(lblUser, txtUser);

        // 4. PIN / Password
        VBox passBox = new VBox(5);
        Label lblPass = new Label("4. Security PIN:");
        lblPass.getStyleClass().add("gov-field-label");

        txtPass = new PasswordField();
        txtPass.setText("••••••••");
        txtPass.setPromptText("Enter PIN");
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.getStyleClass().add("gov-input-control");
        AccessibilityManager.setupAccessible(txtPass, "Security PIN", "Enter PIN", AccessibleRole.PASSWORD_FIELD);
        passBox.getChildren().addAll(lblPass, txtPass);

        // Enter key submits form
        txtAgency.setOnAction(e -> handleLogin());
        txtUser.setOnAction(e -> handleLogin());
        txtPass.setOnAction(e -> handleLogin());

        // Sign In Button
        Button btnSubmit = new Button("Sign In to Portal  ➔");
        btnSubmit.getStyleClass().add("gov-btn-primary");
        btnSubmit.setDefaultButton(true);
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleLogin());
        AccessibilityManager.setupAccessible(btnSubmit, "Sign In", "Submit credentials to log in", AccessibleRole.BUTTON);

        // Quick Demo Profiles (1-Click Login for testing)
        VBox demoBox = new VBox(8);
        demoBox.getStyleClass().add("gov-demo-section");
        demoBox.setAlignment(Pos.CENTER);

        Label lblDemo = new Label("Quick 1-Click Access (Select Role):");
        lblDemo.getStyleClass().add("gov-demo-label");

        HBox demoButtons = new HBox(8);
        demoButtons.setAlignment(Pos.CENTER);

        Button btnOp = buildQuickButton("Operator", "Inspector Pranaash", UserRole.INSPECTOR);
        Button btnSup = buildQuickButton("Supervisor", "Supervisor Vance", UserRole.SUPERVISOR);
        Button btnAud = buildQuickButton("Auditor", "Chief Auditor Davis", UserRole.CHIEF_AUDITOR);

        HBox.setHgrow(btnOp, Priority.ALWAYS);
        HBox.setHgrow(btnSup, Priority.ALWAYS);
        HBox.setHgrow(btnAud, Priority.ALWAYS);

        demoButtons.getChildren().addAll(btnOp, btnSup, btnAud);
        demoBox.getChildren().addAll(lblDemo, demoButtons);

        card.getChildren().addAll(
                lblError,
                roleBox,
                agencyBox,
                userBox,
                passBox,
                btnSubmit,
                new Separator(),
                demoBox
        );

        return card;
    }

    private Button buildQuickButton(String title, String username, UserRole role) {
        Button btn = new Button(title);
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

    private VBox buildSimpleFooter() {
        VBox footer = new VBox(8);
        footer.setAlignment(Pos.CENTER);
        footer.setMaxWidth(480);

        Hyperlink linkHome = new Hyperlink("← Back to Landing Page");
        linkHome.getStyleClass().add("gov-footer-link");
        linkHome.setOnAction(e -> navManager.showHeroView());

        Label notice = new Label("Authorized Use Only  •  NIST SP 800-88 & DoD 5220.22-M Compliant");
        notice.getStyleClass().add("gov-footer-meta");

        footer.getChildren().addAll(linkHome, notice);
        return footer;
    }

    private void handleLogin() {
        String username = txtUser.getText() != null ? txtUser.getText().trim() : "";
        String agency = txtAgency.getText() != null ? txtAgency.getText().trim() : "";
        UserRole role = cmbRole.getValue() != null ? cmbRole.getValue() : UserRole.INSPECTOR;

        if (username.isEmpty()) {
            showError("Please enter your Officer Username.");
            txtUser.requestFocus();
            return;
        }

        if (agency.isEmpty()) {
            showError("Please enter your Agency / Department Code.");
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
