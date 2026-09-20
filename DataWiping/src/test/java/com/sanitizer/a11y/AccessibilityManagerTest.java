package com.sanitizer.a11y;

import javafx.application.Platform;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AccessibilityManagerTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await(2, TimeUnit.SECONDS);
        } catch (IllegalStateException ignored) {
            // Platform already initialized
        }
    }

    @BeforeEach
    void resetState() {
        AccessibilityManager.setTheme(AccessibilityManager.Theme.LIGHT);
        AccessibilityManager.resetFontScale();
    }

    @Test
    void testThemeSettingAndToggles() {
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.LIGHT);

        AccessibilityManager.setTheme(AccessibilityManager.Theme.DARK);
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.DARK);

        AccessibilityManager.Theme toggled = AccessibilityManager.toggleDarkLight();
        assertThat(toggled).isEqualTo(AccessibilityManager.Theme.LIGHT);

        AccessibilityManager.setTheme(AccessibilityManager.Theme.HIGH_CONTRAST_DARK);
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.HIGH_CONTRAST_DARK);
        assertThat(AccessibilityManager.getTheme().getCssClass()).isEqualTo("high-contrast-dark");

        AccessibilityManager.setTheme(AccessibilityManager.Theme.HIGH_CONTRAST_LIGHT);
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.HIGH_CONTRAST_LIGHT);
        assertThat(AccessibilityManager.getTheme().getCssClass()).isEqualTo("high-contrast-light");
    }

    @Test
    void testHighContrastToggleCycle() {
        AccessibilityManager.setTheme(AccessibilityManager.Theme.LIGHT);
        AccessibilityManager.Theme t1 = AccessibilityManager.toggleHighContrast();
        assertThat(t1).isEqualTo(AccessibilityManager.Theme.HIGH_CONTRAST_DARK);

        AccessibilityManager.Theme t2 = AccessibilityManager.toggleHighContrast();
        assertThat(t2).isEqualTo(AccessibilityManager.Theme.HIGH_CONTRAST_LIGHT);

        AccessibilityManager.Theme t3 = AccessibilityManager.toggleHighContrast();
        assertThat(t3).isEqualTo(AccessibilityManager.Theme.LIGHT);
    }

    @Test
    void testFontScalingIncrementsAndReset() {
        assertThat(AccessibilityManager.getFontScale()).isEqualTo(1.0);

        double s1 = AccessibilityManager.increaseFontScale();
        assertThat(s1).isEqualTo(1.25);

        double s2 = AccessibilityManager.increaseFontScale();
        assertThat(s2).isEqualTo(1.50);

        double s3 = AccessibilityManager.increaseFontScale();
        assertThat(s3).isEqualTo(1.75);

        double s4 = AccessibilityManager.increaseFontScale();
        assertThat(s4).isEqualTo(2.00);

        // Clamped at 2.0
        double s5 = AccessibilityManager.increaseFontScale();
        assertThat(s5).isEqualTo(2.00);

        double s6 = AccessibilityManager.decreaseFontScale();
        assertThat(s6).isEqualTo(1.75);

        AccessibilityManager.resetFontScale();
        assertThat(AccessibilityManager.getFontScale()).isEqualTo(1.0);
    }

    @Test
    void testThemeAndScaleListeners() {
        AtomicBoolean themeNotified = new AtomicBoolean(false);
        AtomicBoolean scaleNotified = new AtomicBoolean(false);

        AccessibilityManager.addThemeListener(t -> themeNotified.set(true));
        AccessibilityManager.addScaleListener(s -> scaleNotified.set(true));

        AccessibilityManager.setTheme(AccessibilityManager.Theme.DARK);
        AccessibilityManager.setFontScale(1.50);

        assertThat(themeNotified.get()).isTrue();
        assertThat(scaleNotified.get()).isTrue();
    }

    @Test
    void testApplyThemeAndScaleToParent() {
        VBox root = new VBox();

        AccessibilityManager.setTheme(AccessibilityManager.Theme.HIGH_CONTRAST_DARK);
        AccessibilityManager.setFontScale(1.50);
        AccessibilityManager.applyThemeAndScale(root);

        assertThat(root.getStyleClass()).contains("high-contrast-dark");
        assertThat(root.getStyleClass()).contains("font-scale-150");

        // Switch to Light and 100%
        AccessibilityManager.setTheme(AccessibilityManager.Theme.LIGHT);
        AccessibilityManager.resetFontScale();
        AccessibilityManager.applyThemeAndScale(root);

        assertThat(root.getStyleClass()).doesNotContain("high-contrast-dark");
        assertThat(root.getStyleClass()).contains("font-scale-100");
    }

    @Test
    void testSetupAccessible() {
        Button btn = new Button("Wipe Drive");
        AccessibilityManager.setupAccessible(btn, "Wipe Drive Button", "Initiates secure sanitization algorithm on drive", AccessibleRole.BUTTON);

        assertThat(btn.getAccessibleRole()).isEqualTo(AccessibleRole.BUTTON);
        assertThat(btn.getAccessibleText()).isEqualTo("Wipe Drive Button");
        assertThat(btn.getAccessibleHelp()).isEqualTo("Initiates secure sanitization algorithm on drive");
    }
}
