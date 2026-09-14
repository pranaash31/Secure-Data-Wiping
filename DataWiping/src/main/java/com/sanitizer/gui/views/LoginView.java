package com.sanitizer.gui.views;

import com.sanitizer.gui.navigation.NavigationManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
        Label productName = new Label("SecureErase Pro");
        productName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label productVer = new Label("ENTERPRISE EDITION v2.0");
        productVer.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #3B82F6; -fx-letter-spacing: 1px;");
        productLabel.getChildren().addAll(productName, productVer);
        logoRow.getChildren().addAll(shieldBadge, productLabel);

        // Brand headline
        Label brandHeadline = new Label("Military-Grade Data\nSanitization Suite");
        brandHeadline.getStyleClass().add("brand-headline");
        brandHeadline.setWrapText(true);

        Label brandSub = new Label("Trusted by government and defense agencies worldwide. NIST SP 800-88 & DoD 5220.22-M certified.");
        brandSub.getStyleClass().add("brand-sub");
        brandSub.setWrapText(true);

        // Feature list
        VBox features = new VBox(14);
        String[][] featureData = {
                {"✓  Cryptographic Audit Certificates", "#34D399"},
                {"✓  Real-time USB Drive Monitoring", "#60A5FA"},
                {"✓  Multi-standard Sanitization", "#FBBF24"},
                {"✓  FIPS 140-2 Validated Signatures", "#34D399"},
                {"✓  Offline Air-gapped Operation", "#60A5FA"}
        };
        for (String[] f : featureData) {
            Label feat = new Label(f[0]);
            feat.setStyle("-fx-font-size: 13px; -fx-text-fill: " + f[1] + "; -fx-font-weight: bold;");
            features.getChildren().add(feat);
        }

        // Compliance badge row
        HBox complianceBadges = new HBox(10);
        complianceBadges.setAlignment(Pos.CENTER_LEFT);
        for (String s : new String[]{"NIST 800-88", "DoD 5220.22-M", "FIPS 140-2"}) {
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
        Label badgeLabel = new Label("SECURE AUTHENTICATION PORTAL");
        badgeLabel.getStyleClass().add("badge-info");
        Label loginTitle = new Label("Officer Sign-In");
        loginTitle.getStyleClass().add("login-title");
        Label loginSub = new Label("Enter your credentials to access the sanitization suite");
        loginSub.getStyleClass().add("login-subtitle");
        headerBox.getChildren().addAll(badgeLabel, loginTitle, loginSub);

        // Form fields
        VBox formFields = new VBox(14);

        Label lblAgency = new Label("Agency Identifier / Clearance ID");
        lblAgency.getStyleClass().add("form-label");
        TextField txtAgency = new TextField("GOV-DEF-8942");
        txtAgency.setPromptText("e.g. GOV-DEF-XXXX");
        txtAgency.setMaxWidth(Double.MAX_VALUE);

        Label lblUser = new Label("Officer Username");
        lblUser.getStyleClass().add("form-label");
        TextField txtUser = new TextField("Officer Pranaash");
        txtUser.setPromptText("Enter officer name");
        txtUser.setMaxWidth(Double.MAX_VALUE);

        Label lblPass = new Label("Security PIN / Access Code");
        lblPass.getStyleClass().add("form-label");
        PasswordField txtPass = new PasswordField();
        txtPass.setMaxWidth(Double.MAX_VALUE);
        txtPass.setPromptText("Enter access code");

        Label lblClearance = new Label("Operation Security Level");
        lblClearance.getStyleClass().add("form-label");
        ComboBox<String> cmbClearance = new ComboBox<>();
        cmbClearance.getItems().addAll(
                "TOP SECRET / DEFENSE CLEARANCE",
                "SECRET / AGENCY LEVEL",
                "CONFIDENTIAL / INTERNAL AUDIT"
        );
        cmbClearance.getSelectionModel().select(0);
        cmbClearance.setMaxWidth(Double.MAX_VALUE);

        formFields.getChildren().addAll(lblAgency, txtAgency, lblUser, txtUser, lblPass, txtPass, lblClearance, cmbClearance);

        // Buttons
        Button btnLogin = new Button("AUTHENTICATE & ACCESS SUITE");
        btnLogin.getStyleClass().addAll("button-primary");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-font-size: 14px; -fx-padding: 14 20;");
        btnLogin.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText()));

        Button btnDemo = new Button("Quick Demo Access (No Password Required)");
        btnDemo.setMaxWidth(Double.MAX_VALUE);
        btnDemo.setStyle("-fx-font-size: 12px;");
        btnDemo.setOnAction(e -> navManager.loginSuccess("Officer Pranaash", "GOV-DEF-8942"));

        // Footer
        Label disclaimer = new Label("Authorized Government Personnel Only. All sessions are cryptographically signed and audited.");
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
