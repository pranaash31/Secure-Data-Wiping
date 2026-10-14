package com.sanitizer.gui.views;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Batch Wipe View Unit Tests")
class BatchWipeViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify BatchWipeView instantiates cleanly with root container and queue controls")
    void testBatchWipeViewInitialization() {
        BatchWipeView view = new BatchWipeView();

        assertThat(view).isNotNull();
        assertThat(view.getRoot()).isNotNull();
        assertThat(view.isListening()).isTrue();

        view.dispose();
        assertThat(view.isListening()).isFalse();
    }
}
