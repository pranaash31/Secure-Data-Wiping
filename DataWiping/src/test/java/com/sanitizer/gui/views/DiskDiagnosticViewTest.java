package com.sanitizer.gui.views;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.viewmodel.DiskDiagnosticViewModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Hardware Telemetry View (DiskDiagnosticView) Unit Tests")
class DiskDiagnosticViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @AfterEach
    void tearDown() {
        UsbDetector.clearMockDrivesOverride();
        SmartDiagnostics.clearMockReports();
    }

    @Test
    @DisplayName("Verify DiskDiagnosticView instantiates cleanly with ViewModel and root hierarchy")
    void testViewInitialization() {
        DiskDiagnosticView view = new DiskDiagnosticView();

        assertThat(view).isNotNull();
        assertThat(view.getRoot()).isNotNull();
        assertThat(view.getViewModel()).isNotNull();
        assertThat(view.getViewModel().isListening()).isTrue();

        view.dispose();
        assertThat(view.getViewModel().isListening()).isFalse();
    }

    @Test
    @DisplayName("Verify view accepts injected ViewModel and unregisters cleanly on dispose")
    void testInjectedViewModelAndDisposal() {
        DiskDiagnosticViewModel customVm = new DiskDiagnosticViewModel();
        DiskDiagnosticView view = new DiskDiagnosticView(customVm);

        assertThat(view.getViewModel()).isSameAs(customVm);
        assertThat(customVm.isListening()).isTrue();

        view.dispose();
        assertThat(customVm.isListening()).isFalse();
    }
}
