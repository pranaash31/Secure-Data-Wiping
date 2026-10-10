package com.sanitizer.service;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;

/**
 * Immutable value object encapsulating all parameters and operator context
 * required to execute an end-to-end media sanitization workflow.
 */
public record WorkflowRequest(
        String systemPath,
        String driveModel,
        String serialNumber,
        long sizeBytes,
        String formattedSize,
        WipePolicy policy,
        WipeVerifier.VerificationMode verificationMode,
        boolean isTestMode,
        SessionContext sessionContext,
        int peakTempCelsius,
        int thermalPauseCount,
        int crcErrors,
        String interfaceSummary
) {

    public WorkflowRequest {
        if (systemPath == null || systemPath.isBlank()) {
            systemPath = "/dev/null";
        }
        if (driveModel == null || driveModel.isBlank()) {
            driveModel = "Generic Storage Device";
        }
        if (serialNumber == null || serialNumber.isBlank()) {
            serialNumber = "UNKNOWN-SN";
        }
        if (formattedSize == null || formattedSize.isBlank()) {
            formattedSize = formatBytes(sizeBytes);
        }
        if (policy == null) {
            policy = WipePolicyManager.getInstance().getDefaultPolicy();
        }
        if (verificationMode == null) {
            verificationMode = WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT;
        }
        if (sessionContext == null) {
            sessionContext = new SessionContext("Compliance Officer", "GOV-DEF-8942", UserRole.INSPECTOR);
        }
        if (interfaceSummary == null || interfaceSummary.isBlank()) {
            interfaceSummary = "OPTIMAL";
        }
    }

    public static WorkflowRequest of(UsbDetector.UsbDriveInfo drive, WipePolicy policy, boolean isTestMode, SessionContext sessionContext) {
        return of(drive, policy, WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT, isTestMode, sessionContext);
    }

    public static WorkflowRequest of(
            UsbDetector.UsbDriveInfo drive,
            WipePolicy policy,
            WipeVerifier.VerificationMode mode,
            boolean isTestMode,
            SessionContext sessionContext
    ) {
        if (drive == null) {
            throw new IllegalArgumentException("Target drive cannot be null");
        }
        SmartDiagnostics.SmartReport report = SmartDiagnostics.inspectDrive(drive);
        int crc = (report != null) ? report.crcErrors() : 0;
        String iface = "OPTIMAL";
        if (report != null && report.interfaceAnomaly() != null && report.interfaceAnomaly().isDegraded()) {
            iface = report.interfaceAnomaly().severity().getLabel() + ": " + report.interfaceAnomaly().rootCauseDiagnosis();
        }

        return new WorkflowRequest(
                drive.systemPath(),
                drive.model(),
                drive.serial(),
                drive.sizeBytes(),
                drive.formattedSize(),
                policy,
                mode,
                isTestMode,
                sessionContext,
                0,
                0,
                crc,
                iface
        );
    }

    public WorkflowRequest withThermalTelemetry(int peakTemp, int pauseCount) {
        return new WorkflowRequest(
                systemPath,
                driveModel,
                serialNumber,
                sizeBytes,
                formattedSize,
                policy,
                verificationMode,
                isTestMode,
                sessionContext,
                peakTemp,
                pauseCount,
                crcErrors,
                interfaceSummary
        );
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 GB";
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        return String.format(java.util.Locale.US, "%.1f GB", gb);
    }
}
