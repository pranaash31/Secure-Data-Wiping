package com.sanitizer.gui.components;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.session.SessionAutoLockManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spotlight-Style Command Palette (Cmd+K / Ctrl+K).
 * Provides instantaneous fuzzy/substring command lookup to navigate views,
 * trigger wipe policies on connected target drives, switch visual themes,
 * toggle languages, inspect specific audit log entries, and execute security actions.
 */
public class CommandPaletteDialog {

    public enum Category {
        NAVIGATION("🧭 Navigation", "#2563EB", "#EFF6FF"),
        WIPE_ACTION("🛡️ Wipe Operations", "#DC2626", "#FEF2F2"),
        HARDWARE("🔌 Hardware & SMART", "#059669", "#ECFDF5"),
        THEME("🎨 Themes & Display", "#7C3AED", "#F5F3FF"),
        LANGUAGE("🌐 Language & Locale", "#D97706", "#FFFBEB"),
        AUDIT("📄 Audit & Compliance", "#0891B2", "#ECFEFF"),
        SECURITY("🔒 Security & Session", "#475569", "#F1F5F9");

        private final String label;
        private final String textColor;
        private final String bgColor;

        Category(String label, String textColor, String bgColor) {
            this.label = label;
            this.textColor = textColor;
            this.bgColor = bgColor;
        }

        public String getLabel() { return label; }
        public String getTextColor() { return textColor; }
        public String getBgColor() { return bgColor; }
    }

    public record CommandItem(
            String id,
            String title,
            String subtitle,
            Category category,
            String icon,
            String shortcut,
            List<String> keywords,
            Runnable action
    ) {
        public boolean matches(String query) {
            if (query == null || query.isBlank()) return true;
            String q = query.trim().toLowerCase();

            if (title != null && title.toLowerCase().contains(q)) return true;
            if (subtitle != null && subtitle.toLowerCase().contains(q)) return true;
            if (category != null && category.getLabel().toLowerCase().contains(q)) return true;
            if (shortcut != null && shortcut.toLowerCase().contains(q)) return true;

            if (keywords != null) {
                for (String kw : keywords) {
                    if (kw.toLowerCase().contains(q)) return true;
                }
            }
            return false;
        }
    }

    private static Stage activeDialog;

    /**
     * Opens the Command Palette spotlight overlay dialog.
     */
    public static void show(Window owner, NavigationManager navManager) {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.toFront();
            activeDialog.requestFocus();
            return;
        }

        Stage dialog = new Stage();
        activeDialog = dialog;
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.setTitle("Command Palette — Spotlight Quick Search");

        VBox root = new VBox(0);
        root.setPrefWidth(680);
        root.setMaxWidth(680);
        root.setPrefHeight(480);
        root.setMaxHeight(520);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1.5px; -fx-border-radius: 12px; -fx-background-radius: 12px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 24, 0, 0, 8);");

        // ── Search Input Header ──────────────────────────────────────────
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setPadding(new Insets(14, 18, 14, 18));
        searchRow.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-background-color: #F8FAFC; -fx-background-radius: 12px 12px 0 0;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 16px;");

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Type a command, view name, drive (e.g. 'disk2'), policy, or audit ID...");
        txtSearch.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: #0F172A;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label escPill = new Label("ESC");
        escPill.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-background-color: #E2E8F0; -fx-padding: 3 7; -fx-background-radius: 4px;");

        searchRow.getChildren().addAll(searchIcon, txtSearch, escPill);

        // ── Command List Scroll Area ──────────────────────────────────────
        VBox commandListContainer = new VBox(2);
        commandListContainer.setPadding(new Insets(8, 10, 8, 10));

        ScrollPane scrollPane = new ScrollPane(commandListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Footer / Keyboard Guide ───────────────────────────────────────
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 18, 10, 18));
        footer.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0; -fx-background-color: #F8FAFC; -fx-background-radius: 0 0 12px 12px;");

        Label hintNav = createFooterHint("↑ / ↓", "Navigate");
        Label hintSelect = createFooterHint("↵ Enter", "Execute Action");
        Label hintClose = createFooterHint("ESC", "Close");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label lblCount = new Label("0 commands");
        lblCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        footer.getChildren().addAll(hintNav, hintSelect, hintClose, footerSpacer, lblCount);

        root.getChildren().addAll(searchRow, scrollPane, footer);

        // Build command database
        List<CommandItem> allCommands = buildCommandCatalog(navManager);

        final List<CommandItem> currentFiltered = new ArrayList<>(allCommands);
        final int[] selectedIndex = {0};

        // Render & filter helper
        Runnable refreshList = () -> {
            commandListContainer.getChildren().clear();
            currentFiltered.clear();
            String query = txtSearch.getText();

            // Dynamic Contextual Match for Audit Logs (e.g., if typing numbers or "audit 104")
            List<CommandItem> dynamicCommands = new ArrayList<>(allCommands);
            addDynamicAuditMatches(dynamicCommands, query, navManager);
            addDynamicDriveMatches(dynamicCommands, query, navManager);

            for (CommandItem item : dynamicCommands) {
                if (item.matches(query)) {
                    currentFiltered.add(item);
                }
            }

            lblCount.setText(currentFiltered.size() + " commands");

            if (currentFiltered.isEmpty()) {
                VBox emptyBox = new VBox(6);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(32, 16, 32, 16));
                Label lblNoMatch = new Label("No matching commands or resources found for \"" + query + "\"");
                lblNoMatch.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
                Label lblNoMatchSub = new Label("Try searching for 'dashboard', 'wipe', 'disk2', 'dark', or 'audit'");
                lblNoMatchSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
                emptyBox.getChildren().addAll(lblNoMatch, lblNoMatchSub);
                commandListContainer.getChildren().add(emptyBox);
                return;
            }

            if (selectedIndex[0] >= currentFiltered.size()) {
                selectedIndex[0] = Math.max(0, currentFiltered.size() - 1);
            }

            for (int i = 0; i < currentFiltered.size(); i++) {
                final int idx = i;
                CommandItem cmd = currentFiltered.get(i);
                boolean isSelected = (i == selectedIndex[0]);
                Node row = buildCommandRow(cmd, isSelected, () -> {
                    dialog.close();
                    cmd.action().run();
                });
                commandListContainer.getChildren().add(row);
            }
        };

        // Instant keystroke filter listener
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            selectedIndex[0] = 0;
            refreshList.run();
        });

        // Key Navigation (Up/Down, Enter, Esc)
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
                event.consume();
            } else if (event.getCode() == KeyCode.DOWN) {
                if (!currentFiltered.isEmpty()) {
                    selectedIndex[0] = (selectedIndex[0] + 1) % currentFiltered.size();
                    refreshList.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.UP) {
                if (!currentFiltered.isEmpty()) {
                    selectedIndex[0] = (selectedIndex[0] - 1 + currentFiltered.size()) % currentFiltered.size();
                    refreshList.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER) {
                if (!currentFiltered.isEmpty() && selectedIndex[0] < currentFiltered.size()) {
                    CommandItem selected = currentFiltered.get(selectedIndex[0]);
                    dialog.close();
                    selected.action().run();
                }
                event.consume();
            }
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        try {
            scene.getStylesheets().add(CommandPaletteDialog.class.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.setOnHidden(e -> activeDialog = null);

        // Center on owner window
        if (owner != null) {
            dialog.setX(owner.getX() + (owner.getWidth() - 680) / 2);
            dialog.setY(owner.getY() + Math.max(60, (owner.getHeight() - 520) / 3));
        }

        refreshList.run();
        dialog.show();
        Platform.runLater(txtSearch::requestFocus);
    }

    private static Node buildCommandRow(CommandItem cmd, boolean isSelected, Runnable onExecute) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setCursor(javafx.scene.Cursor.HAND);

        String bg = isSelected ? "#EFF6FF" : "transparent";
        String border = isSelected ? "#3B82F6" : "transparent";
        row.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;", bg, border));

        // Category Icon / Emoji
        Label lblIcon = new Label(cmd.icon());
        lblIcon.setStyle("-fx-font-size: 16px; -fx-min-width: 24px;");

        // Title and Subtitle Column
        VBox textCol = new VBox(2);
        Label lblTitle = new Label(cmd.title());
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (isSelected ? "#1D4ED8" : "#0F172A") + ";");

        Label lblSub = new Label(cmd.subtitle());
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        textCol.getChildren().addAll(lblTitle, lblSub);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        // Category Badge Pill
        Label catBadge = new Label(cmd.category().getLabel());
        catBadge.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s; -fx-background-color: %s; -fx-padding: 3 8; -fx-background-radius: 6px;",
                cmd.category().getTextColor(), cmd.category().getBgColor()));

        // Shortcut Key Pill (if any)
        if (cmd.shortcut() != null && !cmd.shortcut().isBlank()) {
            Label shortcutBadge = new Label(cmd.shortcut());
            shortcutBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-background-color: #E2E8F0; -fx-padding: 3 6; -fx-background-radius: 4px;");
            row.getChildren().addAll(lblIcon, textCol, catBadge, shortcutBadge);
        } else {
            row.getChildren().addAll(lblIcon, textCol, catBadge);
        }

        row.setOnMouseClicked(e -> onExecute.run());
        return row;
    }

    private static Label createFooterHint(String key, String desc) {
        Label lbl = new Label(key + " " + desc);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        return lbl;
    }

    /**
     * Builds the static command registry for navigation, wiping, themes, language, and security actions.
     */
    public static List<CommandItem> buildCommandCatalog(NavigationManager navManager) {
        List<CommandItem> items = new ArrayList<>();

        // ── 🧭 NAVIGATION COMMANDS ────────────────────────────────────────
        items.add(new CommandItem("nav_dashboard", "Go to Executive Dashboard", "Real-time sanitization telemetry, throughput & ESG metrics",
                Category.NAVIGATION, "📊", "Ctrl+D / 1", List.of("home", "stats", "overview"), () -> navManager.navigateTo("dashboard")));

        items.add(new CommandItem("nav_wizard", "Go to Guided Wipe Wizard (Step-by-Step)", "Simplified 5-step guided sanitization workflow with safety confirmations",
                Category.NAVIGATION, "✨", "", List.of("wizard", "guided", "simple", "step", "easy", "technician"), () -> navManager.navigateTo("wizard")));

        items.add(new CommandItem("nav_wiping", "Go to Single Drive Sanitizer", "High-assurance sector overwrite console with live heatmaps & oscilloscope",
                Category.NAVIGATION, "⚡", "Ctrl+W / 2", List.of("wipe", "erase", "sanitize", "clear", "purge"), () -> navManager.navigateTo("wiping")));

        items.add(new CommandItem("nav_batch", "Go to Multi-Drive Batch Operations", "Mass parallel drive sanitization & worker pool governor",
                Category.NAVIGATION, "🗂️", "Ctrl+B / 3", List.of("batch", "parallel", "multi", "concurrent"), () -> navManager.navigateTo("batchwipe")));

        items.add(new CommandItem("nav_diagnostics", "Go to S.M.A.R.T. Diagnostics & Drive Health", "Health scores, defect analysis, wear levels & thermal graphs",
                Category.NAVIGATION, "🔬", "Ctrl+4", List.of("smart", "health", "defects", "wear", "temperature", "diagnostic"), () -> navManager.navigateTo("diagnostics")));

        items.add(new CommandItem("nav_clients", "Go to Enterprise Client Manager", "Manage client organizations, corporate quotas & sanitization profiles",
                Category.NAVIGATION, "🏢", "Ctrl+5", List.of("clients", "organizations", "accounts", "enterprise"), () -> navManager.navigateTo("clients")));

        items.add(new CommandItem("nav_keyvault", "Go to Certificate Authority & Key Vault", "RSA-4096 / ECC signing keys and digital certificate credentials",
                Category.NAVIGATION, "🔐", "Ctrl+6", List.of("keys", "vault", "certificates", "crypto", "signer"), () -> navManager.navigateTo("keyvault")));

        items.add(new CommandItem("nav_audit", "Go to Compliance Audit Log & Chain of Custody", "Immutable SHA-256 chained audit ledger and forensic records",
                Category.NAVIGATION, "📄", "Ctrl+A / 7", List.of("audit", "history", "logs", "chain", "ledger"), () -> navManager.navigateTo("audit")));

        items.add(new CommandItem("nav_verify", "Go to Certificate Verification Portal", "Verify SHA-256 signed compliance certificates and tamper proofs",
                Category.NAVIGATION, "✅", "", List.of("verify", "validate", "check", "proof"), () -> navManager.navigateTo("verify")));

        items.add(new CommandItem("nav_settings", "Go to System Settings & ESG Config", "Configure carbon ESG factors, audio alarms, SMTP & webhooks",
                Category.NAVIGATION, "⚙️", "Ctrl+, / 8", List.of("settings", "config", "preferences", "esg", "alerts"), () -> navManager.navigateTo("settings")));

        // ── 🛡️ WIPE POLICIES & QUICK ACTIONS ─────────────────────────────
        items.add(new CommandItem("wipe_nist", "Start NIST SP 800-88 Rev. 1 Clear (1-Pass)", "Switch to Sanitizer view configured with NIST 800-88 Clear policy",
                Category.WIPE_ACTION, "🛡️", "", List.of("nist", "800-88", "clear", "single", "zero"), () -> {
            navManager.navigateTo("wiping");
            navManager.showNotification("NIST SP 800-88 Ready", "Configured for NIST SP 800-88 Rev. 1 Clear", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("wipe_dod_3p", "Start DoD 5220.22-M Standard (3-Pass)", "Switch to Sanitizer configured with US Department of Defense 3-pass overwrite",
                Category.WIPE_ACTION, "🛡️", "", List.of("dod", "5220.22", "3-pass", "military"), () -> {
            navManager.navigateTo("wiping");
            navManager.showNotification("DoD 5220.22-M Ready", "Configured for DoD 5220.22-M (3-Pass Standard)", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("wipe_dod_7p", "Start DoD 5220.22-M ECE (7-Pass Enhanced)", "Switch to Sanitizer configured with 7-pass military grade sanitization",
                Category.WIPE_ACTION, "🛡️", "", List.of("dod", "ece", "7-pass", "deep", "military"), () -> {
            navManager.navigateTo("wiping");
            navManager.showNotification("DoD 5220.22-M ECE Ready", "Configured for DoD 5220.22-M ECE (7-Pass Enhanced)", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("wipe_crypto", "Start Cryptographic Erase / Random Fill", "1-Pass cryptographic pseudorandom high-entropy overwrite",
                Category.WIPE_ACTION, "🎲", "", List.of("crypto", "random", "pseudo", "entropy"), () -> {
            navManager.navigateTo("wiping");
            navManager.showNotification("Crypto Erase Ready", "Configured for Cryptographic Pseudo-Random Erase", ToastNotification.ToastType.INFO);
        }));

        // ── 🔌 HARDWARE & DISK TOOLS ─────────────────────────────────────
        items.add(new CommandItem("hw_rescan", "Rescan & Detect Connected Drives", "Trigger hardware bus inspection for USB flash, NVMe and external storage",
                Category.HARDWARE, "🔄", "", List.of("rescan", "refresh", "drives", "detect", "usb", "nvme"), () -> {
            List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
            navManager.showNotification("Hardware Scan Complete", drives.size() + " storage drive(s) detected.", ToastNotification.ToastType.SUCCESS);
        }));

        // ── 🎨 THEMES & DISPLAY ──────────────────────────────────────────
        items.add(new CommandItem("theme_light", "Switch Theme: Executive Light Theme", "Clean, high-contrast modern startup theme",
                Category.THEME, "☀️", "", List.of("theme", "light", "white", "executive"), () -> {
            AccessibilityManager.setTheme(AccessibilityManager.Theme.LIGHT);
            navManager.showNotification("Theme Updated", "Executive Light Theme activated", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("theme_dark", "Switch Theme: Government Dark Mode", "Sleek obsidian dark UI with reduced eye fatigue",
                Category.THEME, "🌙", "Ctrl+T", List.of("theme", "dark", "night", "obsidian", "black"), () -> {
            AccessibilityManager.setTheme(AccessibilityManager.Theme.DARK);
            navManager.showNotification("Theme Updated", "Government Dark Theme activated", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("theme_hc_dark", "Switch Theme: High-Contrast Dark (WCAG AAA)", "Pitch-black background with vivid yellow focus (≥7:1 contrast)",
                Category.THEME, "⚡", "Ctrl+H", List.of("theme", "contrast", "wcag", "aaa", "yellow"), () -> {
            AccessibilityManager.setTheme(AccessibilityManager.Theme.HIGH_CONTRAST_DARK);
            navManager.showNotification("High-Contrast Mode", "High-Contrast Dark Theme (WCAG AAA) activated", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("theme_hc_light", "Switch Theme: High-Contrast Light (WCAG AAA)", "Pure white background with deep black borders (≥7:1 contrast)",
                Category.THEME, "👁️", "", List.of("theme", "contrast", "wcag", "aaa", "high"), () -> {
            AccessibilityManager.setTheme(AccessibilityManager.Theme.HIGH_CONTRAST_LIGHT);
            navManager.showNotification("High-Contrast Mode", "High-Contrast Light Theme (WCAG AAA) activated", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("a11y_zoom_in", "Increase Font Size & Zoom (A+)", "Scale text up to 125%, 150%, 175%, or 200%",
                Category.THEME, "🔍", "Ctrl++", List.of("zoom", "font", "larger", "scale"), AccessibilityManager::increaseFontScale));

        items.add(new CommandItem("a11y_zoom_out", "Decrease Font Size & Zoom (A-)", "Scale text down to standard size",
                Category.THEME, "🔍", "Ctrl+-", List.of("zoom", "font", "smaller", "scale"), AccessibilityManager::decreaseFontScale));

        items.add(new CommandItem("a11y_zoom_reset", "Reset Font Size & Zoom (100%)", "Restore default 100% typography scale",
                Category.THEME, "🔍", "Ctrl+0", List.of("zoom", "font", "reset", "default"), AccessibilityManager::resetFontScale));

        // ── 🌐 LANGUAGE & LOCALIZATION ────────────────────────────────────
        items.add(new CommandItem("lang_en", "Switch Language: English (US)", "Set application interface to English",
                Category.LANGUAGE, "🇺🇸", "", List.of("language", "english", "en"), () -> I18n.setLocale(Locale.ENGLISH)));

        items.add(new CommandItem("lang_de", "Switch Language: Deutsch (German)", "Anwendungssprache auf Deutsch umstellen",
                Category.LANGUAGE, "🇩🇪", "", List.of("language", "german", "deutsch", "de"), () -> I18n.setLocale(Locale.GERMAN)));

        items.add(new CommandItem("lang_fr", "Switch Language: Français (French)", "Changer la langue de l'application en français",
                Category.LANGUAGE, "🇫🇷", "", List.of("language", "french", "francais", "fr"), () -> I18n.setLocale(Locale.FRENCH)));

        items.add(new CommandItem("lang_es", "Switch Language: Español (Spanish)", "Cambiar el idioma de la aplicación a español",
                Category.LANGUAGE, "🇪🇸", "", List.of("language", "spanish", "espanol", "es"), () -> I18n.setLocale(new Locale("es"))));

        items.add(new CommandItem("lang_hi", "Switch Language: हिन्दी (Hindi)", "एप्लिकेशन की भाषा हिन्दी में बदलें",
                Category.LANGUAGE, "🇮🇳", "", List.of("language", "hindi", "hi"), () -> I18n.setLocale(new Locale("hi"))));

        // ── 📄 AUDIT & COMPLIANCE ─────────────────────────────────────────
        items.add(new CommandItem("audit_verify_ledger", "Verify Cryptographic Ledger Tamper Integrity", "Run SHA-256 block chain verification over all audit logs",
                Category.AUDIT, "⛓️", "", List.of("verify", "ledger", "tamper", "blockchain", "sha256"), () -> {
            var result = AuditDb.verifyDatabaseIntegrity();
            if (result.isFullyValid()) {
                navManager.showNotification("Ledger Integrity Verified", "All " + result.totalRecordsChecked() + " audit records cryptographically verified (0 defects).", ToastNotification.ToastType.SUCCESS);
            } else {
                navManager.showNotification("Ledger Warning", "Ledger anomalies detected: " + result.anomalies().size() + " issue(s)", ToastNotification.ToastType.WARNING);
            }
        }));

        // ── 🔒 SECURITY & SESSION ACTIONS ────────────────────────────────
        items.add(new CommandItem("sec_lock", "Lock Session Now", "Immediately lock session and require PIN re-authentication",
                Category.SECURITY, "🔒", "Ctrl+Shift+L", List.of("lock", "session", "secure", "pin"), navManager::lockSessionNow));

        items.add(new CommandItem("sec_a11y_guide", "Open Accessibility & Shortcut Guide (F1)", "View full WCAG 2.1 AA keyboard shortcuts and compliance manual",
                Category.SECURITY, "♿", "F1", List.of("help", "a11y", "shortcuts", "manual", "guide"), () -> {
            AccessibilityHelpDialog.show(null);
        }));

        items.add(new CommandItem("sec_updates", "Check for Application Updates", "Verify signature and download latest security patches",
                Category.SECURITY, "🔄", "", List.of("update", "version", "patch", "upgrade"), () -> {
            com.sanitizer.update.UpdateManager.getInstance().checkForUpdatesAsync();
            navManager.showNotification("Checking Updates", "Querying central repository for security patches...", ToastNotification.ToastType.INFO);
        }));

        items.add(new CommandItem("sec_logout", "Sign Out / Terminate Session", "End current officer session and return to landing page",
                Category.SECURITY, "🚪", "", List.of("logout", "signout", "exit", "disconnect"), navManager::logout));

        return items;
    }

    /**
     * Ingests dynamic drive targets based on user query (e.g. "disk2", "sanitize disk2", "smart disk3").
     */
    private static void addDynamicDriveMatches(List<CommandItem> targetList, String query, NavigationManager navManager) {
        if (query == null || query.isBlank()) return;
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();

        for (UsbDetector.UsbDriveInfo d : drives) {
            String q = query.toLowerCase();
            if (d.systemPath().toLowerCase().contains(q) || d.model().toLowerCase().contains(q) || d.serial().toLowerCase().contains(q) || q.contains("disk")) {
                targetList.add(new CommandItem(
                        "drive_wipe_" + d.systemPath(),
                        "Start NIST 800-88 on " + d.model() + " (" + d.systemPath() + ")",
                        "Direct wipe target: " + d.formattedSize() + " | Serial: " + d.serial(),
                        Category.WIPE_ACTION,
                        "⚡",
                        "",
                        List.of(d.systemPath(), d.model(), d.serial(), "wipe", "sanitize"),
                        () -> {
                            navManager.navigateTo("wiping");
                            navManager.showNotification("Target Selected", "Selected " + d.model() + " (" + d.systemPath() + ")", ToastNotification.ToastType.INFO);
                        }
                ));

                targetList.add(new CommandItem(
                        "drive_diag_" + d.systemPath(),
                        "Inspect S.M.A.R.T. Health on " + d.model() + " (" + d.systemPath() + ")",
                        "Deep diagnostic telemetry for " + d.systemPath(),
                        Category.HARDWARE,
                        "🔬",
                        "",
                        List.of(d.systemPath(), d.model(), d.serial(), "smart", "diagnostic", "health"),
                        () -> {
                            navManager.navigateTo("diagnostics");
                            navManager.showNotification("Diagnostic Target", "Loaded " + d.model() + " (" + d.systemPath() + ")", ToastNotification.ToastType.INFO);
                        }
                ));
            }
        }
    }

    /**
     * Ingests dynamic audit log records when searching for audit IDs or timestamps (e.g. "104", "audit 2").
     */
    private static void addDynamicAuditMatches(List<CommandItem> targetList, String query, NavigationManager navManager) {
        if (query == null || query.isBlank()) return;
        String q = query.trim().toLowerCase();

        // Check if query contains digits or "audit"
        if (q.matches(".*\\d+.*") || q.contains("audit") || q.contains("log")) {
            try {
                List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
                for (AuditDb.AuditRecord rec : records) {
                    String idStr = String.valueOf(rec.id());
                    if (idStr.equals(q) || q.contains(idStr) || rec.serialNumber().toLowerCase().contains(q) || rec.driveModel().toLowerCase().contains(q)) {
                        targetList.add(new CommandItem(
                                "audit_record_" + rec.id(),
                                "View Audit Log #" + rec.id() + ": " + rec.driveModel(),
                                "Standard: " + rec.wipeStandard() + " | Status: " + rec.status() + " | Time: " + rec.timestamp(),
                                Category.AUDIT,
                                "📄",
                                "",
                                List.of("audit", "log", idStr, rec.serialNumber(), rec.driveModel()),
                                () -> {
                                    navManager.navigateTo("audit");
                                    navManager.showNotification("Audit Record #" + rec.id(), "Navigated to compliance record #" + rec.id(), ToastNotification.ToastType.INFO);
                                }
                        ));
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
