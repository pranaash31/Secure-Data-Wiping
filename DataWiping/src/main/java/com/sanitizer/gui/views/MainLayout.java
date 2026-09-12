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

    public Parent getRoot() {
        return rootPane;
    }

    public void setContent(Node content, String activeView) {
        rootPane.setCenter(content);

        // Update nav active states
        navButtons.forEach((key, btn) -> {
            if (key.equalsIgnoreCase(activeView)) {
                btn.getStyleClass().setAll("nav-button", "nav-button-active");
            } else {
                btn.getStyleClass().setAll("nav-button");
            }
        });
    }

    private void buildUi() {
        // --- Top Bar ---
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");

        Label lblPortalTitle = new Label("🏛️ NATIONAL DEFENSE DATA SANITIZATION SUITE");
        lblPortalTitle.getStyleClass().add("top-bar-title");

        Label lblBadge = new Label("🔒 NIST & DoD 5220.22-M VERIFIED");
        lblBadge.getStyleClass().add("top-bar-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUserInfo = new Label(String.format("Officer: %s  |  Agency: %s", navManager.getOfficerName(), navManager.getAgencyId()));
        lblUserInfo.getStyleClass().add("top-bar-user");

        topBar.getChildren().addAll(lblPortalTitle, lblBadge, spacer, lblUserInfo);
        topBar.setAlignment(Pos.CENTER_LEFT);
        rootPane.setTop(topBar);

        // --- Sidebar Menu ---
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");

        Label lblNavTitle = new Label("PORTAL NAVIGATION");
        lblNavTitle.getStyleClass().add("sidebar-title");

        Button btnHome = createNavBtn("🏠  Home Portal", "home");
        Button btnDashboard = createNavBtn("📊  Analytics Dashboard", "dashboard");
        Button btnWiping = createNavBtn("🛡️  Data Wiping Suite", "wiping");
        Button btnAudit = createNavBtn("📜  Audit & Certificates", "audit");
        Button btnSettings = createNavBtn("⚙️  Security & Settings", "settings");

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);

        Button btnLogout = new Button("🔒  Sign Out");
        btnLogout.getStyleClass().add("nav-button");
        btnLogout.setOnAction(e -> navManager.logout());

        sidebar.getChildren().addAll(
                lblNavTitle,
                btnHome,
                btnDashboard,
                btnWiping,
                btnAudit,
                btnSettings,
                sidebarSpacer,
                btnLogout
        );

        rootPane.setLeft(sidebar);
    }

    private Button createNavBtn(String text, String viewKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> navManager.navigateTo(viewKey));
        navButtons.put(viewKey, btn);
        return btn;
    }
}
