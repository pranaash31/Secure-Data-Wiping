package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

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
        VBox brandPanel = new VBox(32);
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

        // Feature list
        VBox features = new VBox(14);
        String[][] featureData = {
                {I18n.get("login.feat_certificates"), "#34D399"},
                {I18n.get("login.feat_monitoring"), "#60A5FA"},
                {I18n.get("login.feat_standards"), "#FBBF24"},
                {I18n.get("login.feat_fips"), "#34D399"},
                {I18n.get("login.feat_airgap"), "#60A5FA"}
        };
        for (String[] f : featureData) {
            Label feat = new Label(f[0]);
            feat.setStyle("-fx-font-size: 13px; -fx-text-fill: " + f[1] + "; -fx-font-weight: bold;");
            features.getChildren().add(feat);
        }

        // Compliance badge row
        HBox complianceBadges = new HBox(10);
        complianceBadges.setAlignment(Pos.CENTER_LEFT);
        for (String s : new String[]{"NIST 800-88", "DoD 5220.22-M", "FIPS 140-2", "WCAG 2.1 AA"}) {
            Label b = new Label(s);
            b.getStyleClass().add("badge-info");
            complianceBadges.getChildren().add(b);
        }

        brandPanel.getChildren().addAll(logoRow, brandHeadline, brandSub, new Separator(), features, complianceBadges);

        // ── RIGHT LOGIN PANEL ─────────────────────────────────────────────
        VBox formPanel = new VBox();
        formPanel.getStyleClass().add("login-form-panel");
        formPanel.setAlignment(Pos.CENTER);
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        VBox loginCard = new VBox(22);
        loginCard.getStyleClass().add("login-card");
        loginCard.setMaxWidth(420);

        // Card header
        VBox headerBox = new VBox(6);
        Label badgeLabel = new Label(I18n.get("login.portal_badge"));
        badgeLabel.getStyleClass().add("badge-info");
        Label loginTitle = new Label(I18n.get("login.title"));
        loginTitle.getStyleClass().add("login-title");
        Label loginSub = new Label(I18n.get("login.subtitle"));
        loginSub.getStyleClass().add("login-subtitle");
        headerBox.getChildren().addAll(badgeLabel, loginTitle, loginSub);

        // Form fields
        VBox formFields = new VBox(14);

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

        Label lblClearance = new Label(I18n.get("login.security_level"));
        lblClearance.getStyleClass().add("form-label");
        ComboBox<String> cmbClearance = new ComboBox<>();
        cmbClearance.getItems().addAll(
                I18n.get("login.level_top_secret"),
                I18n.get("login.level_secret"),
                I18n.get("login.level_confidential")
        );
        cmbClearance.getSelectionModel().select(0);
        cmbClearance.setMaxWidth(Double.MAX_VALUE);
        AccessibilityManager.setupAccessible(cmbClearance, "Clearance Level", "Select security clearance tier", AccessibleRole.COMBO_BOX);

        txtUser.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText()));
        txtPass.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText()));

        formFields.getChildren().addAll(lblAgency, txtAgency, lblUser, txtUser, lblPass, txtPass, lblClearance, cmbClearance);

        // Buttons
        Button btnLogin = new Button(I18n.get("login.btn_login"));
        btnLogin.getStyleClass().addAll("button-primary");
        btnLogin.setDefaultButton(true);
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-font-size: 14px; -fx-padding: 14 20;");
        btnLogin.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText()));
        AccessibilityManager.setupAccessible(btnLogin, "Authenticate", "Submit credentials and login to suite", AccessibleRole.BUTTON);

        Button btnDemo = new Button(I18n.get("login.btn_demo"));
        btnDemo.setMaxWidth(Double.MAX_VALUE);
        btnDemo.setStyle("-fx-font-size: 12px;");
        btnDemo.setOnAction(e -> navManager.loginSuccess("Officer Pranaash", "GOV-DEF-8942"));
        AccessibilityManager.setupAccessible(btnDemo, "Demo Access", "Instant test login without credentials", AccessibleRole.BUTTON);

        // Footer
        Label disclaimer = new Label(I18n.get("login.disclaimer"));
        disclaimer.setWrapText(true);
        disclaimer.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569; -fx-text-alignment: center;");
        disclaimer.setAlignment(Pos.CENTER);

        loginCard.getChildren().addAll(
                headerBox,
                new Separator(),
                formFields,
                btnLogin,
                btnDemo,
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
