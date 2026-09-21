package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.UserRole;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class LoginView {

    private final StackPane rootPane = new StackPane();
    private final NavigationManager navManager;

    public LoginView(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();
    }

    public Parent getRoot() {
        return rootPane;
    }

    private void buildUi() {
        rootPane.getStyleClass().add("login-root");

        HBox splitLayout = new HBox();
        splitLayout.setFillHeight(true);

        // ── LEFT BRAND PANEL ──────────────────────────────────────────────
        VBox brandPanel = new VBox(28);
        brandPanel.getStyleClass().add("login-brand-panel");
        brandPanel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brandPanel, Priority.ALWAYS);
        brandPanel.setMinWidth(430);
        brandPanel.setMaxWidth(500);

        // Product logo row
        HBox logoRow = new HBox(14);
        logoRow.setAlignment(Pos.CENTER_LEFT);
        Label shieldBadge = new Label("[S]");
        shieldBadge.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 12px; -fx-padding: 8 14;");
        VBox productLabel = new VBox(2);
        Label productName = new Label(I18n.get("app.title"));
        productName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label productVer = new Label(I18n.get("app.edition"));
        productVer.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #3B82F6; -fx-letter-spacing: 1px;");
        productLabel.getChildren().addAll(productName, productVer);
        logoRow.getChildren().addAll(shieldBadge, productLabel);

        // Brand headline
        Label brandHeadline = new Label(I18n.get("login.brand_headline"));
        brandHeadline.getStyleClass().add("brand-headline");
        brandHeadline.setWrapText(true);

        Label brandSub = new Label(I18n.get("login.brand_sub"));
        brandSub.getStyleClass().add("brand-sub");
        brandSub.setWrapText(true);

        // RBAC Tier Overview Box
        VBox rbacOverview = new VBox(10);
        rbacOverview.setStyle("-fx-background-color: rgba(15,23,42,0.6); -fx-padding: 14; -fx-background-radius: 10px; -fx-border-color: rgba(59,130,246,0.2); -fx-border-radius: 10px;");
        Label lblRbacHeader = new Label("Multi-Tier Authorization (RBAC) Active:");
        lblRbacHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        Label tier1 = new Label("• Tier 1: INSPECTOR — Sanitization Execution & Diagnostics");
        tier1.setStyle("-fx-font-size: 11px; -fx-text-fill: #38BDF8;");
        Label tier2 = new Label("• Tier 2: SUPERVISOR — Policy Builder, Thermal & Alert Config");
        tier2.setStyle("-fx-font-size: 11px; -fx-text-fill: #FBBF24;");
        Label tier3 = new Label("• Tier 3: CHIEF AUDITOR — Full Governance, Ledger Proof & Logs");
        tier3.setStyle("-fx-font-size: 11px; -fx-text-fill: #C084FC;");
        rbacOverview.getChildren().addAll(lblRbacHeader, tier1, tier2, tier3);

        // Compliance badge row
        HBox complianceBadges = new HBox(8);
        complianceBadges.setAlignment(Pos.CENTER_LEFT);
        for (String s : new String[]{"NIST 800-88", "FISMA/HIPAA", "FIPS 140-2", "RBAC Tiers"}) {
            Label b = new Label(s);
            b.getStyleClass().add("badge-info");
            b.setStyle("-fx-font-size: 10px;");
            complianceBadges.getChildren().add(b);
        }

        brandPanel.getChildren().addAll(logoRow, brandHeadline, brandSub, new Separator(), rbacOverview, complianceBadges);

        // ── RIGHT LOGIN PANEL ─────────────────────────────────────────────
        VBox formPanel = new VBox();
        formPanel.getStyleClass().add("login-form-panel");
        formPanel.setAlignment(Pos.CENTER);
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox loginCard = new VBox(18);
        loginCard.getStyleClass().add("login-card");
        loginCard.setMaxWidth(440);

        // Card header
        VBox headerBox = new VBox(4);
        Label badgeLabel = new Label(I18n.get("login.portal_badge"));
        badgeLabel.getStyleClass().add("badge-info");
        Label loginTitle = new Label(I18n.get("login.title"));
        loginTitle.getStyleClass().add("login-title");
        Label loginSub = new Label("Select assigned officer role clearance to authenticate");
        loginSub.getStyleClass().add("login-subtitle");
        headerBox.getChildren().addAll(badgeLabel, loginTitle, loginSub);

        // Form fields
        VBox formFields = new VBox(12);

        Label lblRole = new Label("Assigned Role Clearance (RBAC):");
        lblRole.getStyleClass().add("form-label");
        ComboBox<UserRole> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll(UserRole.values());
        cmbRole.setValue(UserRole.INSPECTOR);
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserRole r) {
                return r != null ? r.getTitle() + " (" + r.getTierLabel() + ")" : "";
            }
            @Override
            public UserRole fromString(String s) { return null; }
        });
        AccessibilityManager.setupAccessible(cmbRole, "User Role", "Select assigned clearance level", AccessibleRole.COMBO_BOX);

        Label lblAgency = new Label(I18n.get("login.agency_id"));
        lblAgency.getStyleClass().add("form-label");
        TextField txtAgency = new TextField("GOV-DEF-8942");
        txtAgency.setPromptText(I18n.get("login.agency_placeholder"));
        txtAgency.setMaxWidth(Double.MAX_VALUE);
        AccessibilityManager.setupAccessible(txtAgency, "Agency Identifier", "Input clearance identifier", AccessibleRole.TEXT_FIELD);

        Label lblUser = new Label(I18n.get("login.officer_name"));
        lblUser.getStyleClass().add("form-label");
        TextField txtUser = new TextField("Officer Pranaash");
        txtUser.setPromptText(I18n.get("login.officer_placeholder"));
        txtUser.setMaxWidth(Double.MAX_VALUE);
        AccessibilityManager.setupAccessible(txtUser, "Officer Username", "Input officer username", AccessibleRole.TEXT_FIELD);

        Label lblPass = new Label(I18n.get("login.pin"));
        lblPass.getStyleClass().add("form-label");
        PasswordField txtPass = new PasswordField();
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.setPromptText(I18n.get("login.pin_placeholder"));
        AccessibilityManager.setupAccessible(txtPass, "Security PIN", "Input security PIN or access code", AccessibleRole.PASSWORD_FIELD);

        txtUser.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText(), cmbRole.getValue()));
        txtPass.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText(), cmbRole.getValue()));

        formFields.getChildren().addAll(lblRole, cmbRole, lblAgency, txtAgency, lblUser, txtUser, lblPass, txtPass);

        // Buttons
        Button btnLogin = new Button("AUTHENTICATE & ENTER PORTAL");
        btnLogin.getStyleClass().addAll("button-primary");
        btnLogin.setDefaultButton(true);
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 12 20;");
        btnLogin.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText(), cmbRole.getValue()));
        AccessibilityManager.setupAccessible(btnLogin, "Authenticate", "Submit credentials and login to suite", AccessibleRole.BUTTON);

        // Quick 1-Click Role Switch Demo Buttons
        VBox demoBox = new VBox(6);
        Label lblDemo = new Label("Quick 1-Click Role Authentication:");
        lblDemo.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        HBox demoButtons = new HBox(8);
        demoButtons.setAlignment(Pos.CENTER);

        Button btnInspector = new Button("🛡️ Inspector");
        btnInspector.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #0284C7; -fx-background-color: #E0F2FE; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnInspector.setOnAction(e -> navManager.loginSuccess("Inspector Pranaash", "GOV-DEF-8942", UserRole.INSPECTOR));

        Button btnSupervisor = new Button("⚙️ Supervisor");
        btnSupervisor.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #D97706; -fx-background-color: #FEF3C7; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnSupervisor.setOnAction(e -> navManager.loginSuccess("Supervisor Vance", "GOV-DEF-8942", UserRole.SUPERVISOR));

        Button btnAuditor = new Button("🔒 Chief Auditor");
        btnAuditor.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #9333EA; -fx-background-color: #F3E8FF; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnAuditor.setOnAction(e -> navManager.loginSuccess("Chief Auditor Davis", "GOV-DEF-8942", UserRole.CHIEF_AUDITOR));

        demoButtons.getChildren().addAll(btnInspector, btnSupervisor, btnAuditor);
        demoBox.getChildren().addAll(lblDemo, demoButtons);

        // Footer
        Label disclaimer = new Label(I18n.get("login.disclaimer"));
        disclaimer.setWrapText(true);
        disclaimer.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569; -fx-text-alignment: center;");
        disclaimer.setAlignment(Pos.CENTER);

        loginCard.getChildren().addAll(
                headerBox,
                new Separator(),
                formFields,
                btnLogin,
                demoBox,
                new Separator(),
                disclaimer
        );

        formPanel.getChildren().add(loginCard);

        splitLayout.getChildren().addAll(brandPanel, formPanel);

        // Fade in animation
        rootPane.setOpacity(0);
        rootPane.getChildren().add(splitLayout);

        FadeTransition ft = new FadeTransition(Duration.millis(600), rootPane);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }
}
