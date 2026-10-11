package com.sanitizer.service;

import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.quarantine.LbaFailureRecord;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.shield.SystemDiskShield;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SanitizationWorkflowServiceTest {

    private File tempDbFile;
    private File tempDriveFile;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = File.createTempFile("test_workflow_audit_", ".db");
        AuditDb.setDbUrlForTesting("jdbc:sqlite:" + tempDbFile.getAbsolutePath());

        tempDriveFile = File.createTempFile("test_mock_drive_", ".img");
        byte[] initialData = new byte[1024 * 64];
        Files.write(tempDriveFile.toPath(), initialData);
    }

    @AfterEach
    void tearDown() {
        SystemDiskShield.clearMockBlockedPaths();
        SmartDiagnostics.clearMockReports();
        if (tempDbFile != null && tempDbFile.exists()) tempDbFile.delete();
        if (tempDriveFile != null && tempDriveFile.exists()) tempDriveFile.delete();
    }

    @Test
    void testSuccessfulWorkflowExecution() {
        UsbDetector.UsbDriveInfo mockDrive = new UsbDetector.UsbDriveInfo(
                "SanDisk Extreme 64GB",
                "SD-EXT-998877",
                64L * 1024 * 1024 * 1024,
                "64.0 GB",
                tempDriveFile.getAbsolutePath()
        );

        SessionContext session = new SessionContext("Auditor Test", "DEPT-CYBER-01", UserRole.CHIEF_AUDITOR);
        WorkflowRequest request = WorkflowRequest.of(
                mockDrive,
                WipePolicyManager.getInstance().getPolicyById("nist-800-88"),
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true, // test mode
                session
        );

        List<String> logs = new ArrayList<>();
        AtomicBoolean metricsReceived = new AtomicBoolean(false);

        WorkflowCallbacks callbacks = WorkflowCallbacks.of(
                m -> metricsReceived.set(true),
                logs::add,
                badSector -> {}
        );

        WorkflowResult result = SanitizationWorkflowService.getInstance().finalizeWipe(
                request,
                true,
                List.of(),
                45,
                0,
                callbacks
        );

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.isQuarantined()).isFalse();
        assertThat(result.auditRecord()).isNotNull();
        assertThat(result.auditRecord().status()).isEqualTo("SUCCESS");
        assertThat(result.certificatePdfPath()).isNotNull();
        assertThat(new File(result.certificatePdfPath()).exists()).isTrue();

        // Verify that database has stored the record
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).driveModel()).isEqualTo("SanDisk Extreme 64GB");
        assertThat(records.get(0).digitalSignature()).isNotBlank();
    }

    @Test
    void testSystemShieldViolationTriggersQuarantineOrFailure() {
        // Block the path in the safety shield
        SystemDiskShield.setMockBlockedPath(tempDriveFile.getAbsolutePath());

        UsbDetector.UsbDriveInfo mockDrive = new UsbDetector.UsbDriveInfo(
                "Protected Root Disk",
                "ROOT-SEC-001",
                128L * 1024 * 1024 * 1024,
                "128.0 GB",
                tempDriveFile.getAbsolutePath()
        );

        SessionContext session = new SessionContext("Operator Shield", "SEC-DIV-99", UserRole.INSPECTOR);
        WorkflowRequest request = WorkflowRequest.of(
                mockDrive,
                WipePolicyManager.getInstance().getPolicyById("nist-800-88"),
                true,
                session
        );

        WorkflowResult result = SanitizationWorkflowService.getInstance().executeWorkflow(request, WorkflowCallbacks.none());

        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
        assertThat(result.isQuarantined()).isTrue();
        assertThat(result.quarantineRecord()).isNotNull();
        assertThat(result.quarantinePdfPath()).isNotNull();
    }

    @Test
    void testFinalizeWipeWithDetectedBadSectorsProducesQuarantineOrder() {
        UsbDetector.UsbDriveInfo mockDrive = new UsbDetector.UsbDriveInfo(
                "Failing Seagate 1TB",
                "ST-FAIL-4433",
                1000L * 1024 * 1024 * 1024,
                "1000.0 GB",
                tempDriveFile.getAbsolutePath()
        );

        WorkflowRequest request = WorkflowRequest.of(
                mockDrive,
                WipePolicyManager.getInstance().getPolicyById("dod-5220-22-m"),
                false,
                new SessionContext("Technician", "HW-LAB", UserRole.INSPECTOR)
        );

        List<LbaFailureRecord> badSectors = List.of(
                LbaFailureRecord.ofByteOffset(204800, 4096, 1, "POSIX_EIO", "I/O write error on block 400"),
                LbaFailureRecord.ofByteOffset(409600, 4096, 1, "POSIX_EIO", "I/O write error on block 800")
        );

        WorkflowResult result = SanitizationWorkflowService.getInstance().finalizeWipe(
                request,
                false, // wipe failed due to bad sectors
                badSectors,
                55,
                2,
                WorkflowCallbacks.none()
        );

        assertThat(result.success()).isFalse();
        assertThat(result.isQuarantined()).isTrue();
        assertThat(result.detectedBadSectors()).hasSize(2);
        assertThat(result.quarantineRecord().totalBadSectorsDetected()).isEqualTo(16);
        assertThat(result.quarantinePdfPath()).isNotNull();
        assertThat(new File(result.quarantinePdfPath()).exists()).isTrue();

        // Check SQLite status
        List<AuditDb.AuditRecord> recs = AuditDb.getAllRecords();
        assertThat(recs).isNotEmpty();
        assertThat(recs.get(0).status()).isEqualTo("QUARANTINED_DEFECTIVE");
    }
}
