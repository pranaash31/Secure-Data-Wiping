package com.sanitizer.demo;

import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.engine.WipeMetrics;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.service.SanitizationWorkflowService;
import com.sanitizer.service.WorkflowCallbacks;
import com.sanitizer.service.WorkflowRequest;
import com.sanitizer.service.WorkflowResult;
import com.sanitizer.shield.DiskLockShield;
import com.sanitizer.shield.SystemDiskShield;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DemoSimulatorManager & Presentation Simulator Mode Tests")
class DemoSimulatorManagerTest {

    @BeforeEach
    void setUp() {
        DemoSimulatorManager.disableDemoMode();
    }

    @AfterEach
    void tearDown() {
        DemoSimulatorManager.disableDemoMode();
    }

    @Test
    @DisplayName("Enabling and disabling Demo Simulator Mode updates detector and notifies listeners")
    void testToggleDemoMode() {
        AtomicBoolean listenerNotified = new AtomicBoolean(false);
        DemoSimulatorManager.addDemoModeListener(active -> listenerNotified.set(active));

        // 1. Enable Demo Mode
        DemoSimulatorManager.enableDemoMode();
        assertThat(DemoSimulatorManager.isDemoModeActive()).isTrue();
        assertThat(listenerNotified.get()).isTrue();

        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        assertThat(drives).hasSize(1);
        UsbDetector.UsbDriveInfo demoDrive = drives.get(0);
        assertThat(demoDrive.model()).isEqualTo(DemoSimulatorManager.SIMULATED_MODEL);
        assertThat(demoDrive.serial()).isEqualTo(DemoSimulatorManager.SIMULATED_SERIAL);
        assertThat(demoDrive.formattedSize()).isEqualTo("16 GB");
        assertThat(demoDrive.systemPath()).isEqualTo(DemoSimulatorManager.SIMULATED_DRIVE_PATH);

        // 2. Disable Demo Mode
        DemoSimulatorManager.disableDemoMode();
        assertThat(DemoSimulatorManager.isDemoModeActive()).isFalse();
        assertThat(listenerNotified.get()).isFalse();
    }

    @Test
    @DisplayName("isSimulatedDrive accurately recognizes simulated descriptors and rejects system drives")
    void testIsSimulatedDriveRecognition() {
        assertThat(DemoSimulatorManager.isSimulatedDrive(DemoSimulatorManager.SIMULATED_DRIVE_PATH)).isTrue();
        assertThat(DemoSimulatorManager.isSimulatedDrive("/dev/mock_usb_16gb")).isTrue();
        assertThat(DemoSimulatorManager.isSimulatedDrive(DemoSimulatorManager.SIMULATED_DRIVE)).isTrue();

        // Must reject primary OS disks and external real storage
        assertThat(DemoSimulatorManager.isSimulatedDrive("/dev/disk0")).isFalse();
        assertThat(DemoSimulatorManager.isSimulatedDrive("/dev/rdisk0")).isFalse();
        assertThat(DemoSimulatorManager.isSimulatedDrive("/dev/rdisk4")).isFalse();
        assertThat(DemoSimulatorManager.isSimulatedDrive("C:\\Windows")).isFalse();
    }

    @Test
    @DisplayName("SystemDiskShield and DiskLockShield evaluate simulated demo target as safe")
    void testShieldCompatibility() {
        SystemDiskShield.SafetyVerdict verdict = SystemDiskShield.evaluate(DemoSimulatorManager.SIMULATED_DRIVE_PATH);
        assertThat(verdict.isSafe()).isTrue();

        boolean locked = DiskLockShield.prepareAndLockDisk(DemoSimulatorManager.SIMULATED_DRIVE_PATH);
        assertThat(locked).isTrue();
        DiskLockShield.releaseDiskLock(DemoSimulatorManager.SIMULATED_DRIVE_PATH);
    }

    @Test
    @DisplayName("WipeEngine simulates multi-pass wipe with live telemetry and progress metrics")
    void testSimulatedWipeExecution() {
        List<WipeMetrics> capturedMetrics = new ArrayList<>();
        List<String> capturedLogs = new ArrayList<>();

        boolean success = WipeEngine.executeWipeWithPolicy(
                DemoSimulatorManager.SIMULATED_DRIVE_PATH,
                DemoSimulatorManager.SIMULATED_SIZE_BYTES,
                WipePolicyManager.getInstance().getPolicyById("dod-5220-22-m"), // 3 passes
                true,
                capturedMetrics::add,
                capturedLogs::add
        );

        assertThat(success).isTrue();
        assertThat(capturedMetrics).isNotEmpty();
        assertThat(capturedLogs).isNotEmpty();

        // Check that final metrics reached 100%
        WipeMetrics last = capturedMetrics.get(capturedMetrics.size() - 1);
        assertThat(last.overallPercent()).isEqualTo(100.0);
        assertThat(last.totalPasses()).isEqualTo(3);
    }

    @Test
    @DisplayName("WipeVerifier returns mathematical zero-residual entropy proof for simulated target")
    void testSimulatedVerification() {
        List<Double> progressEvents = new ArrayList<>();
        List<String> logs = new ArrayList<>();

        WipeVerifier.VerificationResult result = WipeVerifier.verifyDrive(
                DemoSimulatorManager.SIMULATED_DRIVE_PATH,
                DemoSimulatorManager.SIMULATED_SIZE_BYTES,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                WipePolicyManager.getInstance().getDefaultPolicy(),
                true,
                progressEvents::add,
                logs::add
        );

        assertThat(result).isNotNull();
        assertThat(result.isClean()).isTrue();
        assertThat(result.entropyScore()).isEqualTo(0.0000);
        assertThat(result.nonZeroBytesFound()).isEqualTo(0);
        assertThat(result.statusSummary()).contains("Zero Residual Data Confirmed");
        assertThat(result.sha256Proof()).contains("SHA256:");
        assertThat(progressEvents).isNotEmpty();
    }

    @Test
    @DisplayName("SanitizationWorkflowService runs full end-to-end simulated wipe, verification, and PDF certificate export")
    void testFullWorkflowOnSimulatedDrive() {
        DemoSimulatorManager.enableDemoMode();

        WorkflowRequest request = WorkflowRequest.of(
                DemoSimulatorManager.SIMULATED_DRIVE,
                WipePolicyManager.getInstance().getDefaultPolicy(),
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                null
        );

        List<String> logMessages = new ArrayList<>();
        WorkflowCallbacks callbacks = WorkflowCallbacks.of(
                null,
                logMessages::add,
                null,
                null
        );

        WorkflowResult result = SanitizationWorkflowService.getInstance().executeWorkflow(request, callbacks);

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.verificationResult()).isNotNull();
        assertThat(result.verificationResult().isClean()).isTrue();
        assertThat(result.certificatePdfPath()).isNotNull();

        File pdfFile = new File(result.certificatePdfPath());
        assertThat(pdfFile.exists()).isTrue();
        assertThat(pdfFile.length()).isGreaterThan(500);

        // Verify record exists in AuditDb
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        assertThat(records).isNotEmpty();
        AuditDb.AuditRecord latest = records.get(0);
        assertThat(latest.driveModel()).isEqualTo(DemoSimulatorManager.SIMULATED_MODEL);
        assertThat(latest.status()).isEqualTo("SUCCESS");
    }
}
