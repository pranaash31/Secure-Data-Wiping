package com.sanitizer.gui.navigation;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.components.AccessibilityHelpDialog;
import com.sanitizer.gui.views.*;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.SessionContext;
import com.sanitizer.util.AppLogger;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    private String currentActiveView = "dashboard";

    // Session State
    private SessionContext sessionContext;
    private boolean isAuthenticated = false;

    // View Node Cache (ViewKey -> Root Node)
    private final Map<String, Node> viewCache = new HashMap<>();

    private NavigationManager() {
        // When locale changes, invalidate cached views and refresh current active view
        I18n.addListener(locale -> {
            if (isAuthenticated && mainLayout != null) {
                viewCache.clear();
                navigateTo(currentActiveView);
                if (primaryStage != null) {
                    primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("topbar.badge") + " | " + getOfficerName());
                }
            }
        });
    }

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
        AccessibilityManager.applyThemeAndScale(heroView);
        scene = new Scene(heroView, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("app.subtitle"));
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
        Parent root = loginView.getRoot();
        AccessibilityManager.applyThemeAndScale(root);
        scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("login.portal_badge"));
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
        Parent root = mainLayout.getRoot();
        AccessibilityManager.applyThemeAndScale(root);
        scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("topbar.badge") + " | " + getOfficerName());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Default landing — Dashboard
        navigateTo("dashboard");
    }

    // ── Global Keyboard Shortcuts & Accessibility Accelerators ─────────────────
    private void attachGlobalShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean ctrlOrMeta = event.isControlDown() || event.isMetaDown();

            // F1: Accessibility Help Dialog
            if (event.getCode() == KeyCode.F1) {
                AccessibilityHelpDialog.show(primaryStage);
                event.consume();
                return;
            }

            if (ctrlOrMeta) {
                // Font Scaling / Zoom: Ctrl/Cmd + Plus / Equals
                if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS || event.getCode() == KeyCode.ADD) {
                    AccessibilityManager.increaseFontScale();
                    event.consume();
                    return;
                }
                // Font Scaling / Zoom: Ctrl/Cmd + Minus
                if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT) {
                    AccessibilityManager.decreaseFontScale();
                    event.consume();
                    return;
                }
                // Font Scaling / Zoom: Ctrl/Cmd + 0
                if (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0) {
                    AccessibilityManager.resetFontScale();
                    event.consume();
                    return;
                }
                // High Contrast Toggle: Ctrl/Cmd + H
                if (event.getCode() == KeyCode.H) {
                    AccessibilityManager.toggleHighContrast();
                    event.consume();
                    return;
                }
                // Dark/Light Theme Toggle: Ctrl/Cmd + T
                if (event.getCode() == KeyCode.T) {
                    AccessibilityManager.toggleDarkLight();
                    event.consume();
                    return;
                }
                // Cycle Language: Ctrl/Cmd + L
                if (event.getCode() == KeyCode.L) {
                    I18n.cycleNextLanguage();
                    event.consume();
                    return;
                }

                // View Navigation Shortcuts (Ctrl/Cmd + Key)
                if (isAuthenticated) {
                    if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.DIGIT1) navigateTo("dashboard");
                    else if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.DIGIT2) navigateTo("wiping");
                    else if (event.getCode() == KeyCode.B || event.getCode() == KeyCode.DIGIT3) navigateTo("batchWipe");
                    else if (event.getCode() == KeyCode.DIGIT4) navigateTo("diagnostics");
                    else if (event.getCode() == KeyCode.DIGIT5) navigateTo("clients");
                    else if (event.getCode() == KeyCode.K || event.getCode() == KeyCode.DIGIT6) navigateTo("keyvault");
                    else if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.DIGIT7) navigateTo("audit");
                    else if (event.getCode() == KeyCode.COMMA || event.getCode() == KeyCode.DIGIT8) navigateTo("settings");
                }
            }
        });
    }

    // ── Router ───────────────────────────────────────────────────────────────
    public void navigateTo(String viewName) {
        if (!isAuthenticated || mainLayout == null) {
            showLoginView();
            return;
        }

        String key = viewName.toLowerCase();
        this.currentActiveView = key;
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
            case "keyvault"     -> new KeyVaultView().getRoot();
            case "audit"        -> new AuditView().getRoot();
            case "verify", "verification" -> new VerificationPortalView().getRoot();
            case "settings"     -> new SettingsView().getRoot();
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
