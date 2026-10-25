package com.sanitizer.gui.navigation;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.alert.AlertDispatcher;
import com.sanitizer.audio.TextToSpeechEngine;
import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.gui.components.AccessibilityHelpDialog;
import com.sanitizer.gui.components.CommandPaletteDialog;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.views.AuditView;
import com.sanitizer.gui.views.BatchWipeView;
import com.sanitizer.gui.views.ClientManagerView;
import com.sanitizer.gui.views.DashboardView;
import com.sanitizer.gui.views.DiskDiagnosticView;
import com.sanitizer.gui.views.HeroView;
import com.sanitizer.gui.views.KeyVaultView;
import com.sanitizer.gui.views.LockScreenOverlay;
import com.sanitizer.gui.views.LoginView;
import com.sanitizer.gui.views.MainLayout;
import com.sanitizer.gui.views.SettingsView;
import com.sanitizer.gui.views.VerificationPortalView;
import com.sanitizer.gui.views.WipeWizardView;
import com.sanitizer.gui.views.WipingView;
import com.sanitizer.i18n.I18n;
import com.sanitizer.server.WebVerificationServer;
import com.sanitizer.service.AuditExportService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.SessionAutoLockManager;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.update.UpdateManager;
import com.sanitizer.util.AppLogger;
import com.sanitizer.util.SoundManager;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class NavigationManager {

    private static final String MODULE = "NavigationManager";

    private Stage primaryStage;
    private Scene scene;
    private MainLayout mainLayout;
    private StackPane portalRootContainer;
    private String currentActiveView = "dashboard";

    // Session State
    private SessionContext sessionContext;
    private boolean isAuthenticated = false;

    // Active View Lifecycle Hook
    private ViewLifecycle activeViewLifecycle;

    /**
     * View entry holding a JavaFX Node scene root and its associated lifecycle hook.
     */
    public record ViewEntry(Node rootNode, ViewLifecycle lifecycle) {}

    private NavigationManager() {
        // When locale changes, refresh current active view with fresh localized components
        I18n.addListener(locale -> {
            if (isAuthenticated && mainLayout != null) {
                navigateTo(currentActiveView);
                if (primaryStage != null) {
                    primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("topbar.badge") + " | " + getOfficerName());
                }
            }
        });

        // Listen for session lock / unlock events from SessionAutoLockManager
        SessionAutoLockManager.getInstance().addListener(new SessionAutoLockManager.LockListener() {
            @Override
            public void onSessionLocked() {
                Platform.runLater(() -> displayLockScreenOverlay());
            }

            @Override
            public void onSessionUnlocked() {
                Platform.runLater(() -> {
                    showNotification("Session Restored", "Welcome back, " + getOfficerName(),
                            ToastType.SUCCESS);
                });
            }
        });

        // Register JVM-level graceful shutdown hook for process termination
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownApplication, "App-Graceful-Shutdown-Hook"));
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

        // Window close handler — cleanly terminates all daemon executors
        primaryStage.setOnCloseRequest(event -> {
            shutdownApplication();
            Platform.exit();
        });

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

        // Set Window Stage Icon
        try {
            URL iconUrl = getClass().getResource("/icons/icon.png");
            if (iconUrl != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
            }
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Could not load application window icon: " + e.getMessage());
        }

        showHeroView();
        primaryStage.setMaximized(true);
    }

    // ── Static Web Hero Landing Page ─────────────────────────────────────────
    public void showHeroView() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        HeroView heroView = new HeroView(this::showLoginView, this::startDemoPresentation);
        AccessibilityManager.applyThemeAndScale(heroView);
        scene = new Scene(heroView, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);
        SessionAutoLockManager.getInstance().attachToScene(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("app.subtitle"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // ── Login ────────────────────────────────────────────────────────────────
    public void showLoginView() {
        if (activeViewLifecycle != null) {
            try {
                activeViewLifecycle.onDeactivated();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error deactivating view on login transition: " + e.getMessage());
            }
            activeViewLifecycle = null;
        }
        this.isAuthenticated = false;
        this.sessionContext = null;
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        LoginView loginView = new LoginView(this);
        Parent root = loginView.getRoot();
        AccessibilityManager.applyThemeAndScale(root);
        scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);
        SessionAutoLockManager.getInstance().attachToScene(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("login.portal_badge"));
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    // ── Auth Success → Main Portal ───────────────────────────────────────────
    public void loginSuccess(String username, String agency) {
        loginSuccess(username, agency, UserRole.INSPECTOR);
    }

    public void loginSuccess(String username, String agency, UserRole role) {
        this.sessionContext = new SessionContext(username, agency, role);
        this.isAuthenticated = true;
        AppLogger.info(MODULE, "Authenticated session established for: " + sessionContext.officerName() + " [" + role.getTitle() + "]");
        SecurityAuditLogger.logLogin(sessionContext.officerName(), sessionContext.agencyId(), role.getTitle(), true);
        showMainPortal();
    }

    public void startDemoPresentation() {
        com.sanitizer.demo.DemoSimulatorManager.enableDemoMode();
        loginSuccess("Presentation Evaluator", "DEMO-US-GOV", UserRole.INSPECTOR);
        navigateTo("wiping");
        showNotification("Presentation Simulator Active",
                "Simulated 16GB Secure USB loaded. Safe for live presentation without physical hardware.",
                ToastType.INFO);
    }

    public void showMainPortal() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        mainLayout = new MainLayout(this);
        Parent root = mainLayout.getRoot();

        portalRootContainer = new StackPane(root);
        AccessibilityManager.applyThemeAndScale(portalRootContainer);
        scene = new Scene(portalRootContainer, bounds.getWidth(), bounds.getHeight());
        applyCss(scene);
        attachGlobalShortcuts(scene);
        SessionAutoLockManager.getInstance().attachToScene(scene);

        primaryStage.setTitle(I18n.get("app.title") + " — " + I18n.get("topbar.badge") + " | " + getOfficerName() + " (" + getUserRole().getTierLabel() + ")");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Default landing — Dashboard
        navigateTo("dashboard");

        // Asynchronous background update verification
        UpdateManager.getInstance().checkForUpdatesAsync();
    }

    // ── Global Keyboard Shortcuts & Accessibility Accelerators ─────────────────
    private void attachGlobalShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean ctrlOrMeta = event.isControlDown() || event.isMetaDown();

            // Manual Lock Shortcut: Ctrl/Cmd + Alt + L or Ctrl/Cmd + Shift + L
            if (ctrlOrMeta && (event.isAltDown() || event.isShiftDown()) && event.getCode() == KeyCode.L) {
                if (isAuthenticated) {
                    SessionAutoLockManager.getInstance().lockSession("Manual Lock (Shortcut)", getOfficerName(), getAgencyId(), getRole());
                    event.consume();
                    return;
                }
            }

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
                // Cycle Language: Ctrl/Cmd + L (without shift/alt)
                if (event.getCode() == KeyCode.L && !event.isShiftDown() && !event.isAltDown()) {
                    I18n.cycleNextLanguage();
                    event.consume();
                    return;
                }

                // Spotlight Command Palette: Ctrl/Cmd + K
                if (event.getCode() == KeyCode.K) {
                    if (isAuthenticated && !SessionAutoLockManager.getInstance().isLocked()) {
                        showCommandPalette();
                        event.consume();
                        return;
                    }
                }

                // View Navigation Shortcuts (Ctrl/Cmd + Key)
                if (isAuthenticated && !SessionAutoLockManager.getInstance().isLocked()) {
                    if (event.getCode() == KeyCode.D || event.getCode() == KeyCode.DIGIT1) navigateTo("dashboard");
                    else if (event.getCode() == KeyCode.W || event.getCode() == KeyCode.DIGIT2) navigateTo("wiping");
                    else if (event.getCode() == KeyCode.B || event.getCode() == KeyCode.DIGIT3) navigateTo("batchWipe");
                    else if (event.getCode() == KeyCode.DIGIT4) navigateTo("diagnostics");
                    else if (event.getCode() == KeyCode.DIGIT5) navigateTo("clients");
                    else if (event.getCode() == KeyCode.DIGIT6) navigateTo("keyvault");
                    else if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.DIGIT7) navigateTo("audit");
                    else if (event.getCode() == KeyCode.COMMA || event.getCode() == KeyCode.DIGIT8) navigateTo("settings");
                }
            }
        });
    }

    public void showCommandPalette() {
        CommandPaletteDialog.show(primaryStage, this);
    }

    // ── Lock Screen Overlay ──────────────────────────────────────────────────
    private void displayLockScreenOverlay() {
        if (portalRootContainer != null) {
            // Avoid duplicate overlays
            for (Node child : portalRootContainer.getChildren()) {
                if (child instanceof LockScreenOverlay) return;
            }
            LockScreenOverlay overlay = new LockScreenOverlay(this);
            portalRootContainer.getChildren().add(overlay);
        }
    }

    public void lockSessionNow() {
        if (isAuthenticated) {
            SessionAutoLockManager.getInstance().lockSession("Manual Lock Button", getOfficerName(), getAgencyId(), getRole());
        }
    }

    // ── Router & RBAC Permission Enforcement ──────────────────────────────────
    public void navigateTo(String viewName) {
        if (!isAuthenticated || mainLayout == null) {
            showLoginView();
            return;
        }

        if (SessionAutoLockManager.getInstance().isLocked()) {
            return;
        }

        String key = viewName.toLowerCase();

        // RBAC Clearance Evaluation
        if ("keyvault".equals(key) && !hasPermission(UserRole.Permission.KEYVAULT_MANAGE)) {
            showAccessDeniedDialog("Key Vault & Certificate Authority", "Tier 3 — CHIEF AUDITOR Clearance");
            SecurityAuditLogger.logAccessDenied(getOfficerName(), getAgencyId(), getRole(), "Key Vault", "KEYVAULT_MANAGE");
            return;
        }

        // Deactivate outgoing view before transitioning to cleanly detach listeners & cancel timers
        if (activeViewLifecycle != null) {
            try {
                activeViewLifecycle.onDeactivated();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error deactivating view [" + currentActiveView + "]: " + e.getMessage());
            }
            activeViewLifecycle = null;
        }

        this.currentActiveView = key;
        AppLogger.info(MODULE, "Instantiating fresh view via ViewLifecycle factory: " + key);
        ViewEntry entry = createViewEntry(key);

        mainLayout.setContent(entry.rootNode(), key);

        if (entry.lifecycle() != null) {
            this.activeViewLifecycle = entry.lifecycle();
            try {
                this.activeViewLifecycle.onActivated();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error activating view [" + key + "]: " + e.getMessage());
            }
        }
    }

    /**
     * View Factory instantiating fresh views with their lifecycle hooks.
     * Eliminates static UI scene graph caching and memory retention.
     */
    public ViewEntry createViewEntry(String key) {
        return switch (key) {
            case "dashboard"   -> {
                DashboardView v = new DashboardView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "wizard", "wipewizard" -> {
                WipeWizardView v = new WipeWizardView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "wiping"      -> {
                WipingView v = new WipingView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "batchwipe"   -> {
                BatchWipeView v = new BatchWipeView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "diagnostics" -> {
                DiskDiagnosticView v = new DiskDiagnosticView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "clients"     -> {
                ClientManagerView v = new ClientManagerView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "keyvault"    -> {
                KeyVaultView v = new KeyVaultView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "audit"       -> {
                AuditView v = new AuditView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "verify", "verification" -> {
                VerificationPortalView v = new VerificationPortalView();
                yield new ViewEntry(v.getRoot(), v);
            }
            case "settings"    -> {
                SettingsView v = new SettingsView();
                yield new ViewEntry(v.getRoot(), v);
            }
            default            -> {
                DashboardView v = new DashboardView();
                yield new ViewEntry(v.getRoot(), v);
            }
        };
    }

    public Node createViewNode(String key) {
        return createViewEntry(key).rootNode();
    }

    public ViewLifecycle getActiveViewLifecycle() {
        return activeViewLifecycle;
    }

    public void clearViewCache() {
        if (activeViewLifecycle != null) {
            try {
                activeViewLifecycle.onDeactivated();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error deactivating view on clearViewCache: " + e.getMessage());
            }
            activeViewLifecycle = null;
        }
    }

    public void showAccessDeniedDialog(String resourceName, String requiredRole) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(I18n.get("dialog.rbac.title"));
        alert.setHeaderText(I18n.get("dialog.rbac.header"));
        alert.setContentText(I18n.get("dialog.rbac.content",
                resourceName, getOfficerName(), getRole(), requiredRole
        ));
        alert.showAndWait();
        showNotification(I18n.get("dialog.rbac.denied"), I18n.get("dialog.rbac.toast", resourceName),
                ToastType.WARNING);
    }

    public void logout() {
        if (sessionContext != null) {
            SecurityAuditLogger.logLogout(getOfficerName(), getAgencyId(), getRole());
        }
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
    public void showNotification(String title, String message, ToastType type) {
        if (primaryStage != null) {
            ToastNotification.show(primaryStage, title, message, type);
        }
    }

    // ── Getters & Permission Evaluator ─────────────────────────────────────────
    public SessionContext getSessionContext() {
        return sessionContext != null ? sessionContext : new SessionContext("Officer Pranaash", "GOV-DEF-8942", UserRole.INSPECTOR);
    }

    public UserRole getUserRole()  { return getSessionContext().userRole(); }
    public String getOfficerName() { return getSessionContext().officerName(); }
    public String getAgencyId()    { return getSessionContext().agencyId(); }
    public String getRole()        { return getSessionContext().role(); }

    public boolean hasPermission(UserRole.Permission permission) {
        return getSessionContext().hasPermission(permission);
    }

    // ── Graceful Lifecycle Shutdown & Daemon Cleanup ─────────────────────────
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Centralized application shutdown coordinator.
     * Safely terminates all background executors, scheduled timers, daemons, and HTTP servers.
     */
    public synchronized void shutdownApplication() {
        if (!isShutdown.compareAndSet(false, true)) {
            return;
        }
        AppLogger.info(MODULE, "=== INITIATING GRACEFUL APPLICATION SHUTDOWN ===");

        // 0. Deactivate active view lifecycle if mounted
        if (activeViewLifecycle != null) {
            try {
                activeViewLifecycle.onDeactivated();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error deactivating view during shutdown: " + e.getMessage());
            }
            activeViewLifecycle = null;
        }

        // 1. Stop UI Inactivity Timeline Monitor
        try {
            SessionAutoLockManager.getInstance().stopInactivityMonitor();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error stopping SessionAutoLockManager: " + e.getMessage());
        }

        // 2. Shut down hardware USB polling executor
        try {
            UsbDetector.shutdown();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error shutting down UsbDetector: " + e.getMessage());
        }

        // 3. Stop embedded HTTP QR verification web server
        try {
            WebVerificationServer.stopServer();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error stopping WebVerificationServer: " + e.getMessage());
        }

        // 4. Shut down audio synthesis and TTS voice executors
        try {
            SoundManager.shutdown();
            TextToSpeechEngine.getInstance().shutdown();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error shutting down audio subsystems: " + e.getMessage());
        }

        // 5. Shut down real-time webhook & email alert dispatcher
        try {
            AlertDispatcher.shutdown();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error shutting down AlertDispatcher: " + e.getMessage());
        }

        // 6. Terminate active wiping operations and thread pool
        try {
            WipeEngine.shutdown();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error shutting down WipeEngine: " + e.getMessage());
        }

        // 7. Shut down service layer async worker pools
        try {
            SystemConfigurationService.getInstance().shutdown();
            AuditExportService.getInstance().shutdown();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Error shutting down service executors: " + e.getMessage());
        }

        AppLogger.info(MODULE, "=== APPLICATION SHUTDOWN COMPLETED CLEANLY ===");
    }
}
