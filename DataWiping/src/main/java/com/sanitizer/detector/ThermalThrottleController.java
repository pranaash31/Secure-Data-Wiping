package com.sanitizer.detector;

import com.sanitizer.util.AppLogger;

/**
 * Intelligent Thermal Throttling & Auto-Recovery Controller.
 * Dynamically regulates storage I/O queue depth, block size, and chunk pacing micro-delays
 * when storage thermal sensors cross 65°C (or policy throttle threshold) to prevent drive thermal
 * shutdown or NAND wear, and automatically orchestrates graduated ramp-up recovery once cooled.
 */
public class ThermalThrottleController {

    private static final String MODULE = "ThermalThrottle";

    public enum ThrottleState {
        NORMAL("Normal (100% Throughput)", "#10B981", "#ECFDF5", 0),
        THROTTLED("⚡ Throttling Active", "#F59E0B", "#FEF3C7", 50),
        RAMPING_UP("🚀 Auto-Recovery Ramping Up", "#3B82F6", "#EFF6FF", 25),
        PAUSED_CRITICAL("⏸ Auto-Paused (Thermal Limit)", "#EF4444", "#FEE2E2", 100);

        private final String label;
        private final String textColor;
        private final String bgColor;
        private final int defaultThrottlePercent;

        ThrottleState(String label, String textColor, String bgColor, int defaultThrottlePercent) {
            this.label = label;
            this.textColor = textColor;
            this.bgColor = bgColor;
            this.defaultThrottlePercent = defaultThrottlePercent;
        }

        public String getLabel() { return label; }
        public String getTextColor() { return textColor; }
        public String getBgColor() { return bgColor; }
        public int getDefaultThrottlePercent() { return defaultThrottlePercent; }
    }

    public record ThrottleDecision(
            int tempCelsius,
            ThrottleState state,
            int throttlePercent,
            long delayMillis,
            int queueDepthPercent,
            String statusBadge,
            String logMessage
    ) {}

    private ThrottleState currentState = ThrottleState.NORMAL;
    private int currentThrottlePercent = 0;
    private long currentDelayMillis = 0;
    private int rampStage = 0; // 0=None, 1=50%, 2=25%, 3=10%, 4=Complete(0%)
    private int peakRecordedTemp = -1;
    private int throttleEventCount = 0;
    private int recoveryCount = 0;

    public synchronized ThrottleDecision evaluate(int currentTempCelsius, ThermalPolicy policy) {
        if (policy == null) {
            policy = ThermalPolicyManager.getInstance().getPolicy(DeviceType.USB_FLASH);
        }

        if (currentTempCelsius > peakRecordedTemp) {
            peakRecordedTemp = currentTempCelsius;
        }

        int pauseThreshold = policy.autoPauseCelsius();
        int throttleThreshold = policy.throttleCelsius();
        int resumeThreshold = policy.resumeCelsius();

        // 1. Critical Overheat -> Fully Paused Safeguard
        if (currentTempCelsius >= pauseThreshold) {
            boolean wasNotPaused = (currentState != ThrottleState.PAUSED_CRITICAL);
            currentState = ThrottleState.PAUSED_CRITICAL;
            currentThrottlePercent = 100;
            currentDelayMillis = 1000;
            rampStage = 0;

            String logMsg = null;
            if (wasNotPaused) {
                throttleEventCount++;
                logMsg = String.format("⏸ [THERMAL CRITICAL - %s] Drive temperature %d°C reached auto-pause limit (%d°C)! Sanitization paused.",
                        policy.deviceType().getShortBadge(), currentTempCelsius, pauseThreshold);
                AppLogger.warn(MODULE, logMsg);
            }

            return new ThrottleDecision(
                    currentTempCelsius,
                    currentState,
                    100,
                    1000,
                    0,
                    String.format("⏸ PAUSED (%d°C >= %d°C)", currentTempCelsius, pauseThreshold),
                    logMsg
            );
        }

        // 2. Active Thermal Throttling Range [throttleThreshold .. pauseThreshold - 1]
        if (currentTempCelsius >= throttleThreshold) {
            boolean wasNotThrottled = (currentState != ThrottleState.THROTTLED && currentState != ThrottleState.PAUSED_CRITICAL);
            currentState = ThrottleState.THROTTLED;
            rampStage = 0;

            int span = Math.max(1, pauseThreshold - throttleThreshold);
            double ratio = (double) (currentTempCelsius - throttleThreshold) / span;
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            // Throttle between 30% and 90%
            currentThrottlePercent = (int) Math.round(30.0 + (ratio * 60.0));
            // Micro-sleep delay between 15ms and 85ms
            currentDelayMillis = Math.round(15.0 + (ratio * 70.0));
            int queueDepth = Math.max(10, 100 - currentThrottlePercent);

            String logMsg = null;
            if (wasNotThrottled) {
                throttleEventCount++;
                logMsg = String.format("⚡ [THERMAL THROTTLE - %s] Drive temperature %d°C reached throttle threshold (%d°C)! Dynamic I/O rate limiting engaged (%d%% throttle, queue depth %d%%).",
                        policy.deviceType().getShortBadge(), currentTempCelsius, throttleThreshold, currentThrottlePercent, queueDepth);
                AppLogger.info(MODULE, logMsg);
            }

            return new ThrottleDecision(
                    currentTempCelsius,
                    currentState,
                    currentThrottlePercent,
                    currentDelayMillis,
                    queueDepth,
                    String.format("⚡ Throttled %d%% (%d°C)", currentThrottlePercent, currentTempCelsius),
                    logMsg
            );
        }

        // 3. Cooldown & Auto-Recovery Range (currentTemp < throttleThreshold)
        if (currentState == ThrottleState.PAUSED_CRITICAL || currentState == ThrottleState.THROTTLED || currentState == ThrottleState.RAMPING_UP) {
            // Check if drive cooled down to safe resume / recovery temperature
            if (currentTempCelsius <= resumeThreshold || (currentState == ThrottleState.RAMPING_UP && currentTempCelsius < throttleThreshold)) {
                currentState = ThrottleState.RAMPING_UP;
                rampStage++;

                String logMsg = null;
                int queueDepth;

                switch (rampStage) {
                    case 1:
                        currentThrottlePercent = 50;
                        currentDelayMillis = 20;
                        queueDepth = 50;
                        logMsg = String.format("🚀 [THERMAL AUTO-RECOVERY - %s] Drive cooled to %d°C (<= %d°C). Ramp Stage 1/3: 50%% Throughput restored.",
                                policy.deviceType().getShortBadge(), currentTempCelsius, resumeThreshold);
                        break;
                    case 2:
                        currentThrottlePercent = 25;
                        currentDelayMillis = 8;
                        queueDepth = 75;
                        logMsg = String.format("🚀 [THERMAL AUTO-RECOVERY - %s] Drive stable at %d°C. Ramp Stage 2/3: 75%% Throughput restored.",
                                policy.deviceType().getShortBadge(), currentTempCelsius);
                        break;
                    case 3:
                    default:
                        // Fully recovered back to Normal
                        currentState = ThrottleState.NORMAL;
                        currentThrottlePercent = 0;
                        currentDelayMillis = 0;
                        queueDepth = 100;
                        rampStage = 0;
                        recoveryCount++;
                        logMsg = String.format("✨ [THERMAL RECOVERED - %s] Drive temperature stabilized at %d°C. Full 100%% I/O throughput restored.",
                                policy.deviceType().getShortBadge(), currentTempCelsius);
                        break;
                }

                if (logMsg != null) {
                    AppLogger.info(MODULE, logMsg);
                }

                String badge = (currentState == ThrottleState.NORMAL)
                        ? String.format("NORMAL (%d°C)", currentTempCelsius)
                        : String.format("🚀 Recovering %d%% (%d°C)", 100 - currentThrottlePercent, currentTempCelsius);

                return new ThrottleDecision(
                        currentTempCelsius,
                        currentState,
                        currentThrottlePercent,
                        currentDelayMillis,
                        queueDepth,
                        badge,
                        logMsg
                );
            } else {
                // In between resumeThreshold and throttleThreshold, maintain moderate pacing until full cooldown
                currentThrottlePercent = 30;
                currentDelayMillis = 15;
                return new ThrottleDecision(
                        currentTempCelsius,
                        ThrottleState.THROTTLED,
                        30,
                        15,
                        70,
                        String.format("⚡ Cooling %d°C (Target <= %d°C)", currentTempCelsius, resumeThreshold),
                        null
                );
            }
        }

        // 4. Standard Optimal / Normal Operational State
        currentState = ThrottleState.NORMAL;
        currentThrottlePercent = 0;
        currentDelayMillis = 0;
        rampStage = 0;

        return new ThrottleDecision(
                currentTempCelsius,
                currentState,
                0,
                0,
                100,
                String.format("NORMAL (%d°C)", currentTempCelsius),
                null
        );
    }

    /**
     * Calculates the dynamic effective block buffer size during heavy thermal throttling
     * to reduce memory and bus transmission pressure.
     */
    public static int calculateEffectiveBlockSize(int baseBlockSize, int throttlePercent) {
        if (throttlePercent <= 0) return baseBlockSize;
        if (throttlePercent >= 75) return Math.max(64 * 1024, baseBlockSize / 8); // 256KB or 64KB
        if (throttlePercent >= 40) return Math.max(128 * 1024, baseBlockSize / 4); // 512KB
        return Math.max(256 * 1024, baseBlockSize / 2); // 1MB
    }

    public synchronized ThrottleState getCurrentState() { return currentState; }
    public synchronized int getCurrentThrottlePercent() { return currentThrottlePercent; }
    public synchronized long getCurrentDelayMillis() { return currentDelayMillis; }
    public synchronized int getRampStage() { return rampStage; }
    public synchronized int getPeakRecordedTemp() { return peakRecordedTemp; }
    public synchronized int getThrottleEventCount() { return throttleEventCount; }
    public synchronized int getRecoveryCount() { return recoveryCount; }

    public synchronized boolean isThrottled() {
        return currentState == ThrottleState.THROTTLED || currentState == ThrottleState.RAMPING_UP;
    }

    public synchronized boolean isPaused() {
        return currentState == ThrottleState.PAUSED_CRITICAL;
    }

    public synchronized void reset() {
        currentState = ThrottleState.NORMAL;
        currentThrottlePercent = 0;
        currentDelayMillis = 0;
        rampStage = 0;
        peakRecordedTemp = -1;
        throttleEventCount = 0;
        recoveryCount = 0;
    }
}
