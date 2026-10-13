package com.sanitizer.gui.navigation;

import com.sanitizer.alert.AlertDispatcher;
import com.sanitizer.audio.TextToSpeechEngine;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.server.WebVerificationServer;
import com.sanitizer.service.AuditExportService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.SessionAutoLockManager;
import com.sanitizer.util.SoundManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Application Lifecycle & Resource Cleanup Test")
class LifecycleShutdownTest {

    @Test
    @DisplayName("Graceful shutdown coordinates all background subsystems cleanly and idempotently")
    void testGracefulShutdown() {
        NavigationManager nav = NavigationManager.getInstance();

        assertDoesNotThrow(() -> {
            nav.shutdownApplication();
        }, "First shutdown call must execute without throwing exceptions");

        // Idempotency check: Calling shutdown a second time must be safe
        assertDoesNotThrow(() -> {
            nav.shutdownApplication();
        }, "Subsequent shutdown calls must be idempotent and safe");

        // Subsystem state checks
        assertFalse(WebVerificationServer.isRunning(), "Web verification server should not be running after shutdown");
    }

    @Test
    @DisplayName("Individual subsystem shutdown hooks are safe to call directly")
    void testIndividualSubsystems() {
        assertDoesNotThrow(UsbDetector::shutdown);
        assertDoesNotThrow(WebVerificationServer::stopServer);
        assertDoesNotThrow(SoundManager::shutdown);
        assertDoesNotThrow(WipeEngine::shutdown);
        assertDoesNotThrow(AlertDispatcher::shutdown);
        assertDoesNotThrow(() -> TextToSpeechEngine.getInstance().shutdown());
        assertDoesNotThrow(() -> AuditExportService.getInstance().shutdown());
        assertDoesNotThrow(() -> SystemConfigurationService.getInstance().shutdown());
        assertDoesNotThrow(() -> SessionAutoLockManager.getInstance().stopInactivityMonitor());
    }
}
