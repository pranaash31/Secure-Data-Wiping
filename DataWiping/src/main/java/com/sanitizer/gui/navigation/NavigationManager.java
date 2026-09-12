package com.sanitizer.gui.navigation;

import com.sanitizer.gui.views.*;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;

public class NavigationManager {

    private static NavigationManager instance;

    private Stage primaryStage;
    private Scene scene;
    private MainLayout mainLayout;

    // Session State
    private String officerName = "Officer Pranaash";
    private String agencyId = "GOV-DEF-8942";
    private String role = "Senior Sanitization Inspector";
    private boolean isAuthenticated = false;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
        showLoginView();
    }

    public void showLoginView() {
        this.isAuthenticated = false;
        LoginView loginView = new LoginView(this);
        scene = new Scene(loginView.getRoot(), 1040, 740);
        applyCss(scene);

        primaryStage.setTitle("National USB Data Sanitization System - Government Portal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void loginSuccess(String username, String agency) {
        this.officerName = username != null && !username.isBlank() ? username : "Officer Pranaash";
        this.agencyId = agency != null && !agency.isBlank() ? agency : "GOV-DEF-8942";
        this.isAuthenticated = true;
        showMainPortal();
    }

    public void showMainPortal() {
        mainLayout = new MainLayout(this);
        scene = new Scene(mainLayout.getRoot(), 1180, 780);
        applyCss(scene);

        primaryStage.setTitle("National USB Data Sanitization System - Government Defense Suite");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Default landing view
        navigateTo("home");
    }

    public void navigateTo(String viewName) {
        if (!isAuthenticated || mainLayout == null) {
            showLoginView();
            return;
        }

        switch (viewName.toLowerCase()) {
            case "home" -> mainLayout.setContent(new HomeView().getRoot(), "home");
            case "dashboard" -> mainLayout.setContent(new DashboardView().getRoot(), "dashboard");
            case "wiping" -> mainLayout.setContent(new WipingView().getRoot(), "wiping");
            case "audit" -> mainLayout.setContent(new AuditView().getRoot(), "audit");
            case "settings" -> mainLayout.setContent(new SettingsView().getRoot(), "settings");
            default -> mainLayout.setContent(new HomeView().getRoot(), "home");
        }
    }

    public void logout() {
        showLoginView();
    }

    private void applyCss(Scene scene) {
        URL cssUrl = getClass().getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    public String getOfficerName() {
        return officerName;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public String getRole() {
        return role;
    }
}
