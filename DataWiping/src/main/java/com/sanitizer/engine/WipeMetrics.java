package com.sanitizer.engine;

/**
 * Encapsulates real-time telemetry and progress metrics for an ongoing storage drive wipe operation.
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
        long etaSeconds
) {
    /**
     * Returns a human-friendly string for the live throughput speed (e.g. "45.2 MB/s").
     */
    public String formattedSpeed() {
        if (speedMBs <= 0.0) {
            return "-- MB/s";
        }
        return String.format("%.1f MB/s", speedMBs);
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
}
