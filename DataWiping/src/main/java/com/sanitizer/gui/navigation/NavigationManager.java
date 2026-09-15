package com.sanitizer.gui.navigation;

import com.sanitizer.gui.views.*;
import com.sanitizer.session.SessionContext;
import com.sanitizer.util.AppLogger;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class NavigationManager {

    private static final String MODULE = "NavigationManager";

    private Stage primaryStage;
    private Scene scene;
    private MainLayout mainLayout;

    // Session State
    private SessionContext sessionContext;
    private boolean isAuthenticated = false;

    // View Node Cache (ViewKey -> Root Node)
    private final Map<String, Node> viewCache = new HashMap<>();

    private NavigationManager() {}

    /** Thread-safe Initialization-on-Demand Holder Singleton. */
    private static class InstanceHolder {
        private static final NavigationManager INSTANCE = new NavigationManager();
    }

    public static NavigationManager getInstance() {
        return InstanceHolder.INSTANCE;
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
        this.sessionContext = null;
        this.viewCache.clear();
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
        this.sessionContext = new SessionContext(username, agency, "Senior Sanitization Inspector");
        this.isAuthenticated = true;
        AppLogger.info(MODULE, "Authenticated session established for: " + sessionContext.officerName());
        showMainPortal();
    }

    public void showMainPortal() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        mainLayout = new MainLayout(this);
        scene = new Scene(mainLayout.getRoot(), bounds.getWidth(), bounds.getHeight());
        applyCss(scene);

        // Global Keyboard Shortcuts (Cmd/Ctrl + Key)
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() || event.isMetaDown()) {
                if (event.getCode() == KeyCode.D) navigateTo("dashboard");
                else if (event.getCode() == KeyCode.W) navigateTo("wiping");
                else if (event.getCode() == KeyCode.B) navigateTo("batchWipe");
                else if (event.getCode() == KeyCode.K) navigateTo("keyvault");
                else if (event.getCode() == KeyCode.A) navigateTo("audit");
                else if (event.getCode() == KeyCode.COMMA) navigateTo("settings");
            }
        });

        primaryStage.setTitle("SecureErase Pro — Enterprise Suite | " + getOfficerName());
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

        String key = viewName.toLowerCase();
        Node viewNode = viewCache.get(key);

        if (viewNode == null) {
            AppLogger.info(MODULE, "Instantiating view: " + key);
            viewNode = createViewNode(key);
            viewCache.put(key, viewNode);
        } else {
            AppLogger.info(MODULE, "Loaded view from cache: " + key);
        }

        mainLayout.setContent(viewNode, key);
    }

    private Node createViewNode(String key) {
        return switch (key) {
            case "dashboard"   -> new DashboardView().getRoot();
            case "wiping"      -> new WipingView().getRoot();
            case "batchwipe"   -> new BatchWipeView().getRoot();
            case "diagnostics" -> new DiskDiagnosticView().getRoot();
            case "clients"     -> new ClientManagerView().getRoot();
            case "keyvault"    -> new KeyVaultView().getRoot();
            case "audit"       -> new AuditView().getRoot();
            case "settings"    -> new SettingsView().getRoot();
            default            -> new DashboardView().getRoot();
        };
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

    // ── Toast Notifications ──────────────────────────────────────────────────
    public void showNotification(String title, String message, com.sanitizer.gui.components.ToastNotification.ToastType type) {
        if (primaryStage != null) {
            com.sanitizer.gui.components.ToastNotification.show(primaryStage, title, message, type);
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public SessionContext getSessionContext() {
        return sessionContext != null ? sessionContext : new SessionContext("Officer Pranaash", "GOV-DEF-8942", "Senior Inspector");
    }

    public String getOfficerName() { return getSessionContext().officerName(); }
    public String getAgencyId()    { return getSessionContext().agencyId(); }
    public String getRole()        { return getSessionContext().role(); }
}
