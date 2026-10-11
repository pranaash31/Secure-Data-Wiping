package com.sanitizer.gui.views;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verification Portal View Unit Tests")
class VerificationPortalViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify VerificationPortalView instantiates cleanly with root container")
    void testVerificationPortalViewInitialization() {
        VerificationPortalView view = new VerificationPortalView();

        assertThat(view).isNotNull();
        assertThat(view.getRoot()).isNotNull();
    }
}
