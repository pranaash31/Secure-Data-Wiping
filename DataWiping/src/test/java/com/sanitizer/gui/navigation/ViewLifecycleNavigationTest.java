package com.sanitizer.gui.navigation;

import javafx.application.Platform;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("View Lifecycle & Dynamic Factory Navigation Tests")
class ViewLifecycleNavigationTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized in test suite
        }
    }

    @Test
    @DisplayName("Verify createViewEntry creates fresh, distinct Node instances (no static node retention)")
    void testCreateViewEntryCreatesFreshInstances() {
        NavigationManager nav = NavigationManager.getInstance();

        String[] viewKeys = {
            "dashboard", "wizard", "wipewizard", "wiping", "batchwipe",
            "diagnostics", "clients", "keyvault", "audit", "verify", "settings"
        };

        for (String key : viewKeys) {
            NavigationManager.ViewEntry entry1 = nav.createViewEntry(key);
            NavigationManager.ViewEntry entry2 = nav.createViewEntry(key);

            assertThat(entry1).isNotNull();
            assertThat(entry1.rootNode()).isNotNull();
            assertThat(entry1.lifecycle()).isNotNull();

            assertThat(entry2).isNotNull();
            assertThat(entry2.rootNode()).isNotNull();
            assertThat(entry2.lifecycle()).isNotNull();

            // Verify instances are distinct objects (not permanently cached in Map<String, Node>)
            assertThat(entry1.rootNode())
                .as("Subsequent invocations for key '%s' must produce fresh Nodes, not stale cached instances", key)
                .isNotSameAs(entry2.rootNode());
            assertThat(entry1.lifecycle())
                .as("Subsequent invocations for key '%s' must produce fresh ViewLifecycle instances", key)
                .isNotSameAs(entry2.lifecycle());
        }
    }

    @Test
    @DisplayName("Verify lifecycle contract methods (onActivated, onDeactivated) execute without error")
    void testViewLifecycleMethods() {
        NavigationManager nav = NavigationManager.getInstance();

        NavigationManager.ViewEntry entry = nav.createViewEntry("diagnostics");
        assertThat(entry.lifecycle()).isNotNull();

        assertDoesNotThrow(() -> entry.lifecycle().onActivated(),
                "onActivated must succeed when view is activated");
        assertDoesNotThrow(() -> entry.lifecycle().onDeactivated(),
                "onDeactivated must succeed when view is deactivated");
    }

    @Test
    @DisplayName("Verify custom ViewLifecycle implementation receives lifecycle events cleanly")
    void testCustomViewLifecycleContract() {
        AtomicInteger activatedCount = new AtomicInteger(0);
        AtomicInteger deactivatedCount = new AtomicInteger(0);

        ViewLifecycle mockLifecycle = new ViewLifecycle() {
            @Override
            public void onActivated() {
                activatedCount.incrementAndGet();
            }

            @Override
            public void onDeactivated() {
                deactivatedCount.incrementAndGet();
            }
        };

        mockLifecycle.onActivated();
        assertThat(activatedCount.get()).isEqualTo(1);
        assertThat(deactivatedCount.get()).isEqualTo(0);

        mockLifecycle.onDeactivated();
        assertThat(activatedCount.get()).isEqualTo(1);
        assertThat(deactivatedCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify clearViewCache handles lifecycle cleanup safely")
    void testClearViewCacheSafe() {
        NavigationManager nav = NavigationManager.getInstance();
        assertDoesNotThrow(nav::clearViewCache);
        assertThat(nav.getActiveViewLifecycle()).isNull();
    }
}
