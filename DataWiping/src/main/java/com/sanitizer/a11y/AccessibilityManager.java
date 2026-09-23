package com.sanitizer.a11y;

import com.sanitizer.util.AppLogger;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Accessibility (a11y) Manager for WCAG 2.1 AA & Section 508 Compliance.
 * Manages:
 * 1. High-Contrast and Standard Visual Themes (Light, Dark, High-Contrast Dark, High-Contrast Light)
 * 2. Dynamic Font Scaling & Zoom factor (100%, 125%, 150%, 175%, 200%)
 * 3. Assistive Screen Reader metadata bindings (AccessibleRole, AccessibleText, AccessibleHelp)
 * 4. Keyboard Navigation accessibility accelerators
 */
public final class AccessibilityManager {

    private static final String MODULE = "AccessibilityManager";

    public enum Theme {
        LIGHT("Executive Light (Standard AA)", ""),
        DARK("Government Dark Mode (Standard AA)", "dark-theme"),
        HIGH_CONTRAST_DARK("High Contrast Pitch-Black & Vivid Yellow (WCAG AAA ≥7:1)", "high-contrast-dark"),
        HIGH_CONTRAST_LIGHT("High Contrast Pure-White & Deep-Black (WCAG AAA ≥7:1)", "high-contrast-light");

        private final String displayName;
        private final String cssClass;

        Theme(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() { return displayName; }
        public String getCssClass() { return cssClass; }
    }

    public static final double SCALE_100 = 1.0;
    public static final double SCALE_125 = 1.25;
    public static final double SCALE_150 = 1.50;
    public static final double SCALE_175 = 1.75;
    public static final double SCALE_200 = 2.0;

    private static final List<Double> SUPPORTED_SCALES = List.of(
            SCALE_100,
            SCALE_125,
            SCALE_150,
            SCALE_175,
            SCALE_200
    );

    private static Theme currentTheme = Theme.LIGHT;
    private static double currentFontScale = SCALE_100;
    private static com.sanitizer.gui.components.HeatmapPalette currentPalette = com.sanitizer.gui.components.HeatmapPalette.STANDARD;

    private static final List<Consumer<Theme>> themeListeners = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Double>> scaleListeners = new CopyOnWriteArrayList<>();
    private static final List<Consumer<com.sanitizer.gui.components.HeatmapPalette>> paletteListeners = new CopyOnWriteArrayList<>();

    private AccessibilityManager() {}

    /**
     * Set active colorblind-optimized heatmap palette and notify listeners.
     */
    public static synchronized void setHeatmapPalette(com.sanitizer.gui.components.HeatmapPalette palette) {
        if (palette == null) return;
        if (currentPalette != palette) {
            currentPalette = palette;
            AppLogger.info(MODULE, "Sector heatmap palette set to: " + palette.name());
            notifyPaletteListeners(currentPalette);
        }
    }

    public static synchronized com.sanitizer.gui.components.HeatmapPalette getHeatmapPalette() {
        return currentPalette;
    }

    public static void addPaletteListener(Consumer<com.sanitizer.gui.components.HeatmapPalette> listener) {
        if (listener != null) {
            paletteListeners.add(listener);
            listener.accept(currentPalette);
        }
    }

    public static void removePaletteListener(Consumer<com.sanitizer.gui.components.HeatmapPalette> listener) {
        paletteListeners.remove(listener);
    }

    private static void notifyPaletteListeners(com.sanitizer.gui.components.HeatmapPalette palette) {
        for (Consumer<com.sanitizer.gui.components.HeatmapPalette> listener : paletteListeners) {
            try {
                listener.accept(palette);
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error in palette listener: " + e.getMessage());
            }
        }
    }

    /**
     * Set active visual theme and notify listeners.
     */
    public static synchronized void setTheme(Theme theme) {
        if (theme == null) return;
        if (currentTheme != theme) {
            currentTheme = theme;
            AppLogger.info(MODULE, "Visual theme set to: " + theme.name());
            notifyThemeListeners(currentTheme);
        }
    }

    public static synchronized Theme getTheme() {
        return currentTheme;
    }

    /**
     * Toggle or cycle between Light and Dark mode.
     */
    public static synchronized Theme toggleDarkLight() {
        Theme next = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        setTheme(next);
        return next;
    }

    /**
     * Toggle or cycle High Contrast themes.
     */
    public static synchronized Theme toggleHighContrast() {
        Theme next;
        if (currentTheme == Theme.HIGH_CONTRAST_DARK) {
            next = Theme.HIGH_CONTRAST_LIGHT;
        } else if (currentTheme == Theme.HIGH_CONTRAST_LIGHT) {
            next = Theme.LIGHT;
        } else {
            next = Theme.HIGH_CONTRAST_DARK;
        }
        setTheme(next);
        return next;
    }

    /**
     * Set font scaling factor.
     */
    public static synchronized void setFontScale(double scale) {
        double targetScale = scale;
        if (!SUPPORTED_SCALES.contains(targetScale)) {
            // Find closest supported scale
            final double requested = targetScale;
            targetScale = SUPPORTED_SCALES.stream()
                    .min((s1, s2) -> Double.compare(Math.abs(s1 - requested), Math.abs(s2 - requested)))
                    .orElse(SCALE_100);
        }

        if (Double.compare(currentFontScale, targetScale) != 0) {
            currentFontScale = targetScale;
            AppLogger.info(MODULE, "Font scaling updated to: " + (int)(targetScale * 100) + "%");
            notifyScaleListeners(currentFontScale);
        }
    }

    public static synchronized double getFontScale() {
        return currentFontScale;
    }

    public static List<Double> getSupportedScales() {
        return SUPPORTED_SCALES;
    }

    /**
     * Increase font size by one increment up to 200%.
     */
    public static synchronized double increaseFontScale() {
        int idx = SUPPORTED_SCALES.indexOf(currentFontScale);
        if (idx >= 0 && idx < SUPPORTED_SCALES.size() - 1) {
            setFontScale(SUPPORTED_SCALES.get(idx + 1));
        }
        return currentFontScale;
    }

    /**
     * Decrease font size by one increment down to 100%.
     */
    public static synchronized double decreaseFontScale() {
        int idx = SUPPORTED_SCALES.indexOf(currentFontScale);
        if (idx > 0) {
            setFontScale(SUPPORTED_SCALES.get(idx - 1));
        }
        return currentFontScale;
    }

    /**
     * Reset font scaling to default standard (100%).
     */
    public static synchronized void resetFontScale() {
        setFontScale(SCALE_100);
    }

    /**
     * Apply the current Theme and Font Scale CSS classes to a Scene or Root Parent Node.
     */
    public static void applyThemeAndScale(Parent root) {
        if (root == null) return;

        // Clear all theme classes first
        for (Theme t : Theme.values()) {
            if (!t.getCssClass().isEmpty()) {
                root.getStyleClass().remove(t.getCssClass());
            }
        }

        // Clear all font scale classes
        root.getStyleClass().removeIf(c -> c.startsWith("font-scale-"));

        // Add current theme class
        if (!currentTheme.getCssClass().isEmpty()) {
            root.getStyleClass().add(currentTheme.getCssClass());
        }

        // Add current scale class (e.g. font-scale-125)
        int scaleInt = (int) Math.round(currentFontScale * 100);
        root.getStyleClass().add("font-scale-" + scaleInt);
    }

    /**
     * Utility method to configure accessibility parameters on UI components for Screen Readers / Section 508.
     */
    public static void setupAccessible(Node node, String accessibleText, String accessibleHelp, AccessibleRole role) {
        if (node == null) return;
        if (role != null) {
            node.setAccessibleRole(role);
        }
        if (accessibleText != null && !accessibleText.isEmpty()) {
            node.setAccessibleText(accessibleText);
        }
        if (accessibleHelp != null && !accessibleHelp.isEmpty()) {
            node.setAccessibleHelp(accessibleHelp);
        }
    }

    /**
     * Register listeners
     */
    public static void addThemeListener(Consumer<Theme> listener) {
        if (listener != null && !themeListeners.contains(listener)) {
            themeListeners.add(listener);
        }
    }

    public static void removeThemeListener(Consumer<Theme> listener) {
        themeListeners.remove(listener);
    }

    public static void addScaleListener(Consumer<Double> listener) {
        if (listener != null && !scaleListeners.contains(listener)) {
            scaleListeners.add(listener);
        }
    }

    public static void removeScaleListener(Consumer<Double> listener) {
        scaleListeners.remove(listener);
    }

    private static void notifyThemeListeners(Theme theme) {
        for (Consumer<Theme> l : themeListeners) {
            try { l.accept(theme); } catch (Exception e) { AppLogger.warn(MODULE, "Theme listener error: " + e.getMessage()); }
        }
    }

    private static void notifyScaleListeners(Double scale) {
        for (Consumer<Double> l : scaleListeners) {
            try { l.accept(scale); } catch (Exception e) { AppLogger.warn(MODULE, "Scale listener error: " + e.getMessage()); }
        }
    }
}
