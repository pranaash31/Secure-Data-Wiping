package com.sanitizer.gui.navigation;

import com.sanitizer.gui.views.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class NavigationManager {

    private static NavigationManager instance;

    private Stage primaryStage;
    private Scene scene;
    private MainLayout mainLayout;

    // Session State
    private String officerName = "Officer Pranaash";
    private String agencyId   = "GOV-DEF-8942";
    private String role       = "Senior Sanitization Inspector";
    private boolean isAuthenticated = false;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    /** Entry point: show static web-style Hero landing page maximized to screen bounds. */
    public void init(Stage stage) {
        this.primaryStage = stage;
        
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        
        showHeroView();
        primaryStage.setMaximized(true);
    }

    // ── Static Web Hero Landing Page ─────────────────────────────────────────
    public void showHeroView() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        HeroView heroView = new HeroView(this::showLoginView);
        scene = new Scene(heroView, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        primaryStage.setTitle("SecureErase Pro — Enterprise Hardware Sanitization Platform");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // ── Login ────────────────────────────────────────────────────────────────
    public void showLoginView() {
        this.isAuthenticated = false;
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        LoginView loginView = new LoginView(this);
        scene = new Scene(loginView.getRoot(), bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        primaryStage.setTitle("SecureErase Pro — Officer Authentication Portal");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    // ── Auth Success → Main Portal ───────────────────────────────────────────
    public void loginSuccess(String username, String agency) {
        this.officerName = (username != null && !username.isBlank()) ? username : "Officer Pranaash";
        this.agencyId    = (agency != null && !agency.isBlank()) ? agency : "GOV-DEF-8942";
        this.isAuthenticated = true;
        showMainPortal();
    }

    public void showMainPortal() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        mainLayout = new MainLayout(this);
        scene = new Scene(mainLayout.getRoot(), bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        primaryStage.setTitle("SecureErase Pro — Enterprise Suite | " + officerName);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Default landing — Dashboard
        navigateTo("dashboard");
    }

    // ── Router ───────────────────────────────────────────────────────────────
    public void navigateTo(String viewName) {
        if (!isAuthenticated || mainLayout == null) {
            showLoginView();
            return;
        }

        switch (viewName.toLowerCase()) {
            case "dashboard"   -> mainLayout.setContent(new DashboardView().getRoot(), "dashboard");
            case "wiping"      -> mainLayout.setContent(new WipingView().getRoot(), "wiping");
            case "batchWipe"   -> mainLayout.setContent(new BatchWipeView().getRoot(), "batchWipe");
            case "diagnostics" -> mainLayout.setContent(new DiskDiagnosticView().getRoot(), "diagnostics");
            case "clients"     -> mainLayout.setContent(new ClientManagerView().getRoot(), "clients");
            case "keyvault"    -> mainLayout.setContent(new KeyVaultView().getRoot(), "keyvault");
            case "audit"       -> mainLayout.setContent(new AuditView().getRoot(), "audit");
            case "settings"    -> mainLayout.setContent(new SettingsView().getRoot(), "settings");
            default            -> mainLayout.setContent(new DashboardView().getRoot(), "dashboard");
        }
    }

    public void logout() {
        showLoginView();
    }

    // ── CSS ──────────────────────────────────────────────────────────────────
    private void applyCss(Scene scene) {
        URL cssUrl = getClass().getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getOfficerName() { return officerName; }
    public String getAgencyId()    { return agencyId; }
    public String getRole()        { return role; }
}
