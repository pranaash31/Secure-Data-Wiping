package com.sanitizer.service;

import com.sanitizer.quarantine.LbaFailureRecord;

import java.util.List;

/**
 * Enterprise Application Service contract orchestrating the end-to-end media
 * sanitization lifecycle: diagnostics, execution, zero-residual verification,
 * cryptographic signing, audit ledger persistence, certificate generation,
 * alerts, and hardware failure quarantine.
 */
public interface SanitizationService {

    /**
     * Executes the full sanitization workflow synchronously:
     * Pre-wipe snapshot -> Wipe -> Verification -> Post-wipe snapshot -> Digital Seal -> DB -> PDF -> Alert.
     *
     * @param request Target drive, policy, test mode, and operator context.
     * @param callbacks Observers for progress, logs, and bad sector faults.
     * @return Complete workflow result.
     */
    WorkflowResult executeWorkflow(WorkflowRequest request, WorkflowCallbacks callbacks);

    /**
     * Finalizes a completed low-level wipe (e.g. from an asynchronous batch engine or custom runner),
     * applying post-wipe verification, cryptographic attestation, certification, and quarantine assessment.
     *
     * @param request Target drive and configuration.
     * @param wipeSuccess Whether low-level overwriting completed without I/O error.
     * @param badSectors Any failing LBAs recorded during writing.
     * @param peakTemp Peak drive temperature observed.
     * @param pauseCount Number of thermal pauses triggered.
     * @param callbacks Observers for verification progress and logs.
     * @return Complete workflow result.
     */
    WorkflowResult finalizeWipe(
            WorkflowRequest request,
            boolean wipeSuccess,
            List<LbaFailureRecord> badSectors,
            int peakTemp,
            int pauseCount,
            WorkflowCallbacks callbacks
    );
}
