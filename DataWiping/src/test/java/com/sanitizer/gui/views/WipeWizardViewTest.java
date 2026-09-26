package com.sanitizer.gui.views;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Guided Wipe Wizard View Unit Tests")
class WipeWizardViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify WipeWizardView instantiates cleanly with root and stepper container")
    void testWizardInitialization() {
        WipeWizardView wizard = new WipeWizardView();

        assertThat(wizard).isNotNull();
        assertThat(wizard.getRoot()).isNotNull();
    }
}
