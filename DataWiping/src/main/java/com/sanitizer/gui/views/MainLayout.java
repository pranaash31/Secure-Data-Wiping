package com.sanitizer.gui.views;

import com.sanitizer.gui.navigation.NavigationManager;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.Map;

public class MainLayout {

    private final BorderPane rootPane = new BorderPane();
    private final NavigationManager navManager;
    private final Map<String, Button> navButtons = new HashMap<>();

    public MainLayout(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();
    }

    public Parent getRoot() { return rootPane; }

    public void setContent(Node content, String activeView) {
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
        // ── TOP BAR ─────────────────────────────────────────────────────
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox brandRow = new HBox(12);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        Label shieldIcon = new Label("[S]");
        shieldIcon.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 8px; -fx-padding: 4 8;");
        Label lblTitle = new Label("SecureErase Pro");
        lblTitle.getStyleClass().add("top-bar-title");
        brandRow.getChildren().addAll(shieldIcon, lblTitle);

        Label lblBadge = new Label("ENTERPRISE v2.0");
        lblBadge.getStyleClass().add("top-bar-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUserInfo = new Label(
                "Officer: " + navManager.getOfficerName() + "   |   Agency: " + navManager.getAgencyId()
        );
        lblUserInfo.getStyleClass().add("top-bar-user");

        Button btnThemeToggle = new Button("🌙 Dark Mode");
        btnThemeToggle.getStyleClass().add("button-theme-toggle");
        btnThemeToggle.setOnAction(e -> {
            boolean isDark = rootPane.getStyleClass().contains("dark-theme");
            if (isDark) {
                rootPane.getStyleClass().remove("dark-theme");
                btnThemeToggle.setText("🌙 Dark Mode");
            } else {
                rootPane.getStyleClass().add("dark-theme");
                btnThemeToggle.setText("☀️ Light Mode");
            }
        });

        Button btnLogoutTop = new Button("Sign Out");
        btnLogoutTop.getStyleClass().add("button-theme-toggle");
        btnLogoutTop.setOnAction(e -> navManager.logout());

        topBar.getChildren().addAll(brandRow, lblBadge, spacer, lblUserInfo, btnThemeToggle, btnLogoutTop);
        rootPane.setTop(topBar);

        // ── SIDEBAR ──────────────────────────────────────────────────────
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");

        // MAIN section
        Label mainLabel = new Label("MAIN");
        mainLabel.getStyleClass().add("sidebar-section-label");
        Button btnDashboard = createNavBtn("  Dashboard", "dashboard");

        // OPERATIONS section
        Label opsLabel = new Label("OPERATIONS");
        opsLabel.getStyleClass().add("sidebar-section-label");
        Button btnWiping    = createNavBtn("  Data Wiping Workplace", "wiping");
        Button btnBatch     = createNavBtn("  Batch Wipe Queue", "batchWipe");
        Button btnDiag      = createNavBtn("  Drive Diagnostics", "diagnostics");

        // MANAGEMENT section
        Label mgmtLabel = new Label("MANAGEMENT");
        mgmtLabel.getStyleClass().add("sidebar-section-label");
        Button btnClients   = createNavBtn("  Client Manager", "clients");
        Button btnKeyVault  = createNavBtn("  Key Vault", "keyvault");

        // COMPLIANCE section
        Label compLabel = new Label("COMPLIANCE");
        compLabel.getStyleClass().add("sidebar-section-label");
        Button btnAudit     = createNavBtn("  Audit Trail", "audit");
        Button btnSettings  = createNavBtn("  Settings", "settings");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        // Officer info card at bottom of sidebar
        VBox officerCard = new VBox(4);
        officerCard.getStyleClass().add("sidebar-officer-card");
        Label officerName = new Label(navManager.getOfficerName());
        officerName.getStyleClass().add("sidebar-officer-name");
        Label officerRole = new Label(navManager.getRole());
        officerRole.getStyleClass().add("sidebar-officer-role");
        Label officerAgency = new Label(navManager.getAgencyId());
        officerAgency.getStyleClass().add("sidebar-officer-agency");
        officerCard.getChildren().addAll(officerName, officerRole, officerAgency);

        sidebar.getChildren().addAll(
                mainLabel, btnDashboard,
                opsLabel, btnWiping, btnBatch, btnDiag,
                mgmtLabel, btnClients, btnKeyVault,
                compLabel, btnAudit, btnSettings,
                sidebarSpacer,
                officerCard
        );

        rootPane.setLeft(sidebar);
    }

    private Button createNavBtn(String text, String viewKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> navManager.navigateTo(viewKey));
        navButtons.put(viewKey, btn);
        return btn;
    }
}
