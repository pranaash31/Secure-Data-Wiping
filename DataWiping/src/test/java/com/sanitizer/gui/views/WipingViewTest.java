package com.sanitizer.gui.views;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hardware Data Sanitization Workplace (WipingView) Unit Tests")
class WipingViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify WipingView instantiates cleanly with scroll root and UI hierarchy")
    void testWipingViewInitialization() {
        WipingView view = new WipingView();

        assertThat(view).isNotNull();
        assertThat(view.getRoot()).isNotNull();
    }
}
