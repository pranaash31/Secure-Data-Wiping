package com.sanitizer.service;

import com.sanitizer.engine.WipeMetrics;
import com.sanitizer.quarantine.LbaFailureRecord;

import java.util.function.Consumer;

/**
 * Callbacks for live telemetry, logs, and fault tracking during sanitization workflow.
 */
public record WorkflowCallbacks(
        Consumer<WipeMetrics> metricsCallback,
        Consumer<String> logCallback,
        Consumer<LbaFailureRecord> badSectorCallback,
        Consumer<Double> verificationProgressCallback
) {

    public WorkflowCallbacks {
        // Safe no-op fallbacks if null
        if (metricsCallback == null) metricsCallback = m -> {};
        if (logCallback == null) logCallback = l -> {};
        if (badSectorCallback == null) badSectorCallback = b -> {};
        if (verificationProgressCallback == null) verificationProgressCallback = p -> {};
    }

    public static WorkflowCallbacks none() {
        return new WorkflowCallbacks(null, null, null, null);
    }

    public static WorkflowCallbacks of(
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback,
            Consumer<LbaFailureRecord> badSectorCallback
    ) {
        return new WorkflowCallbacks(metricsCallback, logCallback, badSectorCallback, null);
    }

    public static WorkflowCallbacks of(
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback,
            Consumer<LbaFailureRecord> badSectorCallback,
            Consumer<Double> verificationProgressCallback
    ) {
        return new WorkflowCallbacks(metricsCallback, logCallback, badSectorCallback, verificationProgressCallback);
    }
}
