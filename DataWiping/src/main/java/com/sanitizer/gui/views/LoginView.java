package com.sanitizer.gui.views;

import com.sanitizer.gui.navigation.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
        rootPane.setStyle("-fx-background-color: #F8FAFC;");

        VBox card = new VBox(20);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(460);
        card.setAlignment(Pos.CENTER_LEFT);

        // Header / Seal Badge
        HBox badgeBox = new HBox(8);
        badgeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblBadge = new Label("🏛️ NATIONAL DEFENSE SECURITY PORTAL");
        lblBadge.getStyleClass().add("badge-info");
        badgeBox.getChildren().add(lblBadge);

        Label lblTitle = new Label("Defense USB Data Wiper");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label lblSubtitle = new Label("NIST SP 800-88 & DoD 5220.22-M Compliance Suite");
        lblSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        VBox titleBox = new VBox(4, badgeBox, lblTitle, lblSubtitle);

        // Inputs
        VBox formBox = new VBox(14);

        Label lblAgency = new Label("Agency Identifier / Clearance ID");
        lblAgency.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        TextField txtAgency = new TextField("GOV-DEF-8942");

        Label lblUser = new Label("Officer Username");
        lblUser.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        TextField txtUser = new TextField("Officer Pranaash");

        Label lblPass = new Label("Security PIN / Password");
        lblPass.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        PasswordField txtPass = new PasswordField();
        txtPass.setText("••••••••••••");

        Label lblClearance = new Label("Operation Security Level");
        lblClearance.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        ComboBox<String> cmbClearance = new ComboBox<>();
        cmbClearance.getItems().addAll("TOP SECRET / DEFENSE CLEARANCE", "SECRET / AGENCY LEVEL", "CONFIDENTIAL / INTERNAL AUDIT");
        cmbClearance.getSelectionModel().select(0);
        cmbClearance.setMaxWidth(Double.MAX_VALUE);

        formBox.getChildren().addAll(lblAgency, txtAgency, lblUser, txtUser, lblPass, txtPass, lblClearance, cmbClearance);

        // Buttons
        Button btnLogin = new Button("🔒 AUTHENTICATE & ACCESS SUITE");
        btnLogin.getStyleClass().add("button-primary");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-font-size: 14px; -fx-padding: 12 20;");
        btnLogin.setOnAction(e -> navManager.loginSuccess(txtUser.getText(), txtAgency.getText()));

        Button btnDemo = new Button("⚡ Quick Demo Access");
        btnDemo.setMaxWidth(Double.MAX_VALUE);
        btnDemo.setOnAction(e -> navManager.loginSuccess("Officer Pranaash", "GOV-DEF-8942"));

        VBox buttonBox = new VBox(10, btnLogin, btnDemo);

        // Disclaimer Footer
        Label lblDisclaimer = new Label("Authorized Government Personnel Only. All activities are cryptographically audited.");
        lblDisclaimer.setWrapText(true);
        lblDisclaimer.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-text-alignment: center;");

        card.getChildren().addAll(titleBox, new Separator(), formBox, buttonBox, new Separator(), lblDisclaimer);

        rootPane.getChildren().add(card);
        StackPane.setMargin(card, new Insets(40));
    }
}
