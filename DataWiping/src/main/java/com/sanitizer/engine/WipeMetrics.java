package com.sanitizer.engine;

import com.sanitizer.detector.SmartDiagnostics;

/**
 * Encapsulates real-time telemetry, thermal status, I/O oscilloscope metrics, and progress
 * for an ongoing storage drive wipe operation.
 */
public record WipeMetrics(
        String systemPath,
        double overallPercent,
        int currentPass,
        int totalPasses,
        String passName,
        long bytesProcessedInPass,
        long totalTargetBytesInPass,
        double speedMBs,
        long etaSeconds,
        int tempCelsius,
        SmartDiagnostics.ThermalStatus thermalStatus,
        boolean isThermalPaused,
        int throttlePercent,
        String throttleState,
        double iops,
        double latencyMs,
        double busSaturationPercent
) {
    public WipeMetrics(
            String systemPath,
            double overallPercent,
            int currentPass,
            int totalPasses,
            String passName,
            long bytesProcessedInPass,
            long totalTargetBytesInPass,
            double speedMBs,
            long etaSeconds,
            int tempCelsius,
            SmartDiagnostics.ThermalStatus thermalStatus,
            boolean isThermalPaused,
            int throttlePercent,
            String throttleState
    ) {
        this(systemPath, overallPercent, currentPass, totalPasses, passName,
             bytesProcessedInPass, totalTargetBytesInPass, speedMBs, etaSeconds,
             tempCelsius, thermalStatus, isThermalPaused,
             throttlePercent, throttleState,
             calculateDefaultIops(speedMBs),
             calculateDefaultLatency(speedMBs),
             calculateDefaultSaturation(speedMBs));
    }

    public WipeMetrics(
            String systemPath,
            double overallPercent,
            int currentPass,
            int totalPasses,
            String passName,
            long bytesProcessedInPass,
            long totalTargetBytesInPass,
            double speedMBs,
            long etaSeconds,
            int tempCelsius,
            SmartDiagnostics.ThermalStatus thermalStatus,
            boolean isThermalPaused
    ) {
        this(systemPath, overallPercent, currentPass, totalPasses, passName,
             bytesProcessedInPass, totalTargetBytesInPass, speedMBs, etaSeconds,
             tempCelsius, thermalStatus, isThermalPaused,
             isThermalPaused ? 100 : 0,
             isThermalPaused ? "PAUSED" : "NORMAL");
    }

    public WipeMetrics(
            String systemPath,
            double overallPercent,
            int currentPass,
            int totalPasses,
            String passName,
            long bytesProcessedInPass,
            long totalTargetBytesInPass,
            double speedMBs,
            long etaSeconds
    ) {
        this(systemPath, overallPercent, currentPass, totalPasses, passName,
             bytesProcessedInPass, totalTargetBytesInPass, speedMBs, etaSeconds,
             35, SmartDiagnostics.ThermalStatus.NORMAL, false, 0, "NORMAL");
    }

    private static double calculateDefaultIops(double speedMBs) {
        if (speedMBs <= 0.0) return 0.0;
        // Standard 4KB sector ops equivalent: (speedMBs * 1024 * 1024) / 4096
        return (speedMBs * 1024.0 * 1024.0) / 4096.0;
    }

    private static double calculateDefaultLatency(double speedMBs) {
        if (speedMBs <= 0.0) return 0.0;
        // 2MB buffer block transfer time: (2MB / speedMBs) * 1000 ms, with realistic controller overhead (0.5ms - 25ms)
        double rawMs = (2.0 / Math.max(0.1, speedMBs)) * 100.0;
        return Math.min(250.0, Math.max(0.8, rawMs));
    }

    private static double calculateDefaultSaturation(double speedMBs) {
        if (speedMBs <= 0.0) return 0.0;
        // Baseline against USB 3.0 / SATA standard ~450 MB/s
        return Math.min(100.0, (speedMBs / 450.0) * 100.0);
    }

    /**
     * Returns a human-friendly string for the live throughput speed (e.g. "45.2 MB/s").
     */
    public String formattedSpeed() {
        if (speedMBs <= 0.0) {
            return "-- MB/s";
        }
        return String.format("%.1f MB/s", speedMBs);
    }

    public String formattedIops() {
        if (iops <= 0.0) return "-- IOPS";
        if (iops >= 1000.0) {
            return String.format("%,.0f IOPS", iops);
        }
        return String.format("%.1f IOPS", iops);
    }

    public String formattedLatency() {
        if (latencyMs <= 0.0) return "-- ms";
        return String.format("%.1f ms", latencyMs);
    }

    public String formattedBusSaturation() {
        if (busSaturationPercent <= 0.0) return "0.0%";
        return String.format("%.1f%%", busSaturationPercent);
    }

    /**
     * Returns a human-friendly formatted ETA (e.g. "01m 24s", "45s", or "--").
     */
    public String formattedEta() {
        if (etaSeconds <= 0) {
            return overallPercent >= 100.0 ? "00:00" : "Estimating...";
        }
        long minutes = etaSeconds / 60;
        long seconds = etaSeconds % 60;
        if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }

    /**
     * Returns a formatted progress percentage string (e.g. "73.5%").
     */
    public String formattedProgress() {
        return String.format("%.1f%%", overallPercent);
    }

    /**
     * Returns a formatted pass summary (e.g. "Pass 1/3: Zero Fill").
     */
    public String formattedPassSummary() {
        if (totalPasses <= 1) {
            return "Pass 1/1: " + passName;
        }
        return String.format("Pass %d/%d: %s", currentPass, totalPasses, passName);
    }

    public String formattedTemp() {
        return tempCelsius + " °C";
    }

    public boolean isThrottled() {
        return throttlePercent > 0 && !isThermalPaused;
    }

    public String formattedThrottle() {
        if (isThermalPaused) return "⏸ Paused (100%)";
        if (throttlePercent > 0) return "⚡ Throttled " + throttlePercent + "%";
        return "100% Throughput";
    }
}
