package com.sanitizer.service;

import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.quarantine.LbaFailureRecord;
import com.sanitizer.quarantine.QuarantineRecord;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete outcome of a media sanitization workflow:
 * verification proofs, S.M.A.R.T. delta, cryptographic ledger record,
 * PDF compliance certificate path, or quarantine disposition records.
 */
public record WorkflowResult(
        boolean success,
        String statusMessage,
        WipeVerifier.VerificationResult verificationResult,
        SmartDiagnostics.SmartDelta smartDelta,
        AuditDb.AuditRecord auditRecord,
        String certificatePdfPath,
        QuarantineRecord quarantineRecord,
        String quarantinePdfPath,
        List<LbaFailureRecord> detectedBadSectors,
        int peakTemperatureCelsius,
        int thermalPauseCount
) {

    public WorkflowResult {
        if (detectedBadSectors == null) {
            detectedBadSectors = Collections.emptyList();
        } else {
            detectedBadSectors = List.copyOf(detectedBadSectors);
        }
    }

    public boolean isQuarantined() {
        return quarantineRecord != null;
    }

    public static WorkflowResult success(
            String statusMessage,
            WipeVerifier.VerificationResult verificationResult,
            SmartDiagnostics.SmartDelta smartDelta,
            AuditDb.AuditRecord auditRecord,
            String certificatePdfPath,
            int peakTemp,
            int pauseCount
    ) {
        return new WorkflowResult(
                true,
                statusMessage,
                verificationResult,
                smartDelta,
                auditRecord,
                certificatePdfPath,
                null,
                null,
                Collections.emptyList(),
                peakTemp,
                pauseCount
        );
    }

    public static WorkflowResult failure(
            String statusMessage,
            SmartDiagnostics.SmartDelta smartDelta,
            QuarantineRecord quarantineRecord,
            String quarantinePdfPath,
            List<LbaFailureRecord> badSectors,
            int peakTemp,
            int pauseCount
    ) {
        return new WorkflowResult(
                false,
                statusMessage,
                null,
                smartDelta,
                null,
                null,
                quarantineRecord,
                quarantinePdfPath,
                badSectors,
                peakTemp,
                pauseCount
        );
    }
}
