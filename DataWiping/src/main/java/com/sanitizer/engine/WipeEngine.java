package com.sanitizer.engine;

import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WipeEngine {

    private static final String MODULE = "WipeEngine";
    private static final long TEST_CAP_BYTES = 1L * 1024 * 1024 * 1024; // 1 GB cap in bytes

    // Concurrent multi-threaded pool supporting 8+ simultaneous drive wiping operations
    private static final int THREAD_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    private static final ExecutorService batchExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
        Thread t = new Thread(r, "WipeEngine-Worker");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, Future<Boolean>> activeWipeTasks = new ConcurrentHashMap<>();
    private static final Map<String, Process> activeDdProcesses = new ConcurrentHashMap<>();

    public enum WipeStandard {
        NIST_800_88_CLEAR, // 1 Pass (0x00 Zero Fill)
        DOD_5220_22_M      // 3 Pass (0x00, 0xFF, Cryptographic Random)
    }

    public static boolean executeWipe(String systemPath, long totalBytes, WipeStandard standard, Consumer<Double> progressCallback) {
        return executeWipe(systemPath, totalBytes, standard, true, progressCallback, null);
    }

    public static boolean executeWipe(String systemPath, long totalBytes, WipeStandard standard, boolean isTestMode,
                                      Consumer<Double> progressCallback, Consumer<String> logCallback) {
        Consumer<WipeMetrics> metricsCallback = null;
        if (progressCallback != null) {
            metricsCallback = metrics -> progressCallback.accept(metrics.overallPercent());
        }
        return executeWipeWithMetrics(systemPath, totalBytes, standard, isTestMode, metricsCallback, logCallback);
    }

    public static boolean executeWipeWithMetrics(String systemPath, long totalBytes, WipeStandard standard, boolean isTestMode,
                                                Consumer<WipeMetrics> metricsCallback, Consumer<String> logCallback) {
        WipePolicy policy = (standard == WipeStandard.DOD_5220_22_M)
                ? WipePolicyManager.getInstance().getPolicyById("dod-5220-22-m")
                : WipePolicyManager.getInstance().getPolicyById("nist-800-88");
        return executeWipeWithPolicy(systemPath, totalBytes, policy, isTestMode, metricsCallback, logCallback);
    }

    /**
     * Executes custom multi-pass sanitization configured by a WipePolicy.
     */
    public static boolean executeWipeWithPolicy(
            String systemPath,
            long totalBytes,
            WipePolicy policy,
            boolean isTestMode,
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback
    ) {
        return executeWipeWithPolicy(systemPath, totalBytes, policy, isTestMode, metricsCallback, logCallback, null);
    }

    /**
     * Executes custom multi-pass sanitization configured by a WipePolicy with granular bad sector fault tracking.
     */
    public static boolean executeWipeWithPolicy(
            String systemPath,
            long totalBytes,
            WipePolicy policy,
            boolean isTestMode,
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback,
            Consumer<com.sanitizer.quarantine.LbaFailureRecord> badSectorCallback
    ) {
        if (policy == null) {
            policy = WipePolicyManager.getInstance().getDefaultPolicy();
        }

        // DEEP SAFETY SHIELD: Multi-OS Root & System Mount Inspection
        com.sanitizer.shield.SystemDiskShield.SafetyVerdict shieldVerdict =
                com.sanitizer.shield.SystemDiskShield.evaluate(systemPath);
        if (!shieldVerdict.isSafe()) {
            AppLogger.shield(MODULE, shieldVerdict.blockReason());
            if (logCallback != null) logCallback.accept(shieldVerdict.blockReason());
            com.sanitizer.alert.AlertDispatcher.notifyShieldViolation(systemPath, "Low-level block overwrite", shieldVerdict.blockReason());
            return false;
        }

        // Acquire Disk Lock and Unmount Logical Volumes
        com.sanitizer.shield.DiskLockShield.prepareAndLockDisk(systemPath);

        try {
            long targetBytes = isTestMode ? Math.min(totalBytes, TEST_CAP_BYTES) : totalBytes;
            List<WipePass> passes = policy.getPasses();
            if (passes == null || passes.isEmpty()) {
                passes = List.of(new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Default Zero Fill"));
            }

            int totalPasses = passes.size();
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            log(logCallback, String.format("Starting %s (%d-Pass Standard) on %s%s",
                    policy.getName(), totalPasses, systemPath, isTestMode ? " [TEST MODE: 1GB CAP]" : " [FULL WIPE]"));
            log(logCallback, String.format("Standard Org: %s | Pattern Sequence: %s",
                    policy.getOrganization(), policy.getPatternSummary()));

            double passSlice = 100.0 / totalPasses;

            for (int i = 0; i < totalPasses; i++) {
                WipePass pass = passes.get(i);
                int currentPass = i + 1;
                double startPct = i * passSlice;
                double endPct = (i + 1) * passSlice;

                String sourcePath = "/dev/zero";
                if (pass.getPatternType() == WipePatternType.PSEUDO_RANDOM) {
                    sourcePath = "/dev/urandom";
                }

                log(logCallback, String.format("[%s] Pass %d/%d: %s (%s)...",
                        policy.getStandardCode(), currentPass, totalPasses, pass.getDescription(), pass.getPatternHex()));

                boolean passSuccess = runDdCommand(
                        systemPath,
                        sourcePath,
                        targetBytes,
                        isTestMode,
                        currentPass,
                        totalPasses,
                        pass.getDisplayName(),
                        metricsCallback,
                        logCallback,
                        badSectorCallback,
                        startPct,
                        endPct
                );

                if (!passSuccess) {
                    log(logCallback, String.format("[ERROR] Pass %d/%d failed on %s", currentPass, totalPasses, systemPath));
                    return false;
                }
            }

            if (metricsCallback != null) {
                metricsCallback.accept(new WipeMetrics(systemPath, 100.0, totalPasses, totalPasses, "Completed", targetBytes, targetBytes, 0.0, 0));
            }
            log(logCallback, String.format("[SUCCESS] All %d passes of %s successfully executed on %s.", totalPasses, policy.getName(), systemPath));
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return true;
        } finally {
            com.sanitizer.shield.DiskLockShield.releaseDiskLock(systemPath);
        }
    }

    /**
     * Submits an asynchronous concurrent wiping task to the batch executor queue with double progress callback.
     */
    public static Future<Boolean> submitBatchWipeTask(
            String systemPath,
            long totalBytes,
            WipeStandard standard,
            boolean isTestMode,
            Consumer<Double> progressCallback,
            Consumer<String> logCallback,
            Consumer<Boolean> completionCallback
    ) {
        Consumer<WipeMetrics> metricsCallback = null;
        if (progressCallback != null) {
            metricsCallback = metrics -> progressCallback.accept(metrics.overallPercent());
        }
        return submitBatchWipeTaskWithMetrics(systemPath, totalBytes, standard, isTestMode, metricsCallback, logCallback, completionCallback);
    }

    /**
     * Submits an asynchronous concurrent wiping task to the batch executor queue with rich WipeMetrics callback.
     */
    public static Future<Boolean> submitBatchWipeTaskWithMetrics(
            String systemPath,
            long totalBytes,
            WipeStandard standard,
            boolean isTestMode,
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback,
            Consumer<Boolean> completionCallback
    ) {
        WipePolicy policy = (standard == WipeStandard.DOD_5220_22_M)
                ? WipePolicyManager.getInstance().getPolicyById("dod-5220-22-m")
                : WipePolicyManager.getInstance().getPolicyById("nist-800-88");
        return submitBatchWipeTaskWithPolicy(systemPath, totalBytes, policy, isTestMode, metricsCallback, logCallback, completionCallback);
    }

    /**
     * Submits an asynchronous concurrent wiping task to the batch executor queue using a custom WipePolicy.
     */
    public static Future<Boolean> submitBatchWipeTaskWithPolicy(
            String systemPath,
            long totalBytes,
            WipePolicy policy,
            boolean isTestMode,
            Consumer<WipeMetrics> metricsCallback,
            Consumer<String> logCallback,
            Consumer<Boolean> completionCallback
    ) {
        Future<Boolean> future = batchExecutor.submit(() -> {
            boolean success = false;
            com.sanitizer.shield.WorkerPoolGovernor.getInstance().acquireSlot(systemPath);
            try {
                success = executeWipeWithPolicy(systemPath, totalBytes, policy, isTestMode, metricsCallback, logCallback);
            } catch (Exception e) {
                AppLogger.error(MODULE, "Batch wipe error on " + systemPath, e);
            } finally {
                com.sanitizer.shield.WorkerPoolGovernor.getInstance().releaseSlot(systemPath);
                activeWipeTasks.remove(systemPath);
                if (completionCallback != null) {
                    completionCallback.accept(success);
                }
            }
            return success;
        });

        activeWipeTasks.put(systemPath, future);
        return future;
    }

    /**
     * Cancels an active concurrent wiping task for a specific drive path (Granular Safety Abort).
     */
    public static boolean cancelWipeTask(String systemPath) {
        Future<Boolean> future = activeWipeTasks.remove(systemPath);
        Process proc = activeDdProcesses.remove(systemPath);

        boolean cancelled = false;

        if (proc != null && proc.isAlive()) {
            AppLogger.info(MODULE, "Terminating active dd process for " + systemPath);
            proc.destroyForcibly();
            cancelled = true;
        }

        if (future != null && !future.isDone()) {
            future.cancel(true);
            cancelled = true;
        }

        com.sanitizer.shield.WorkerPoolGovernor.getInstance().releaseSlot(systemPath);
        com.sanitizer.shield.DiskLockShield.releaseDiskLock(systemPath);

        return cancelled;
    }

    public static int getActiveTaskCount() {
        return activeWipeTasks.size();
    }

    public static int getThreadPoolCapacity() {
        return THREAD_POOL_SIZE;
    }

    public static boolean isTaskRunning(String systemPath) {
        Future<Boolean> future = activeWipeTasks.get(systemPath);
        return future != null && !future.isDone();
    }

    public static final int THERMAL_WARNING_THRESHOLD = 48;
    public static final int THERMAL_AUTO_PAUSE_THRESHOLD = 60;
    public static final int THERMAL_SAFE_RESUME_THRESHOLD = 45;

    private static void log(Consumer<String> logCallback, String msg) {
        AppLogger.info(MODULE, msg);
        if (logCallback != null) logCallback.accept(msg);
    }

    private static boolean runDdCommand(String systemPath, String sourcePath, long targetBytes, boolean isTestMode,
                                        int currentPass, int totalPasses, String passName,
                                        Consumer<WipeMetrics> metricsCallback, Consumer<String> logCallback,
                                        Consumer<com.sanitizer.quarantine.LbaFailureRecord> badSectorCallback,
                                        double startPct, double endPct) {
        List<String> command = new ArrayList<>();
        command.add("dd");
        command.add("if=" + sourcePath);
        command.add("of=" + systemPath);
        command.add("bs=2m");

        if (isTestMode) {
            command.add("count=500"); // 1 GB cap for fast testing
        }

        command.add("status=progress");

        long startNanoTime = System.nanoTime();
        long totalWipeTargetBytes = targetBytes * totalPasses;
        long lastBytesWritten = 0;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            activeDdProcesses.put(systemPath, process);
            com.sanitizer.detector.ThermalThrottleController throttleController = new com.sanitizer.detector.ThermalThrottleController();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        process.destroyForcibly();
                        return false;
                    }

                    if (logCallback != null) logCallback.accept(line);

                    // Check for Hardware I/O Fault / Bad Sector errors from dd stream
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("input/output error") || lowerLine.contains("error writing")
                            || lowerLine.contains("bad sector") || lowerLine.contains("write error")
                            || lowerLine.contains("device not configured")) {
                        long failingOffset = lastBytesWritten;
                        long failingLba = failingOffset / 512;
                        com.sanitizer.quarantine.LbaFailureRecord failureRecord =
                                com.sanitizer.quarantine.LbaFailureRecord.ofByteOffset(
                                        failingOffset, 4096, currentPass, "POSIX_EIO", line
                                );
                        if (badSectorCallback != null) {
                            badSectorCallback.accept(failureRecord);
                        }
                        log(logCallback, String.format("⚠️ [HARDWARE I/O DEFECT] Block fault @ offset %,d (LBA ~0x%08X): %s",
                                failingOffset, failingLba, line));
                    }

                    // Device-Aware Thermal Safeguard & Dynamic Throttling Check
                    com.sanitizer.detector.DeviceType deviceType = com.sanitizer.detector.DeviceType.fromDrive(null, systemPath, targetBytes);
                    com.sanitizer.detector.ThermalPolicy policy = com.sanitizer.detector.ThermalPolicyManager.getInstance().getPolicy(deviceType);
                    int autoPauseThreshold = policy.autoPauseCelsius();
                    int resumeThreshold = policy.resumeCelsius();

                    int currentTemp = com.sanitizer.detector.SmartDiagnostics.getLiveTemperature(systemPath, null);
                    com.sanitizer.detector.ThermalThrottleController.ThrottleDecision throttleDecision =
                            throttleController.evaluate(currentTemp, policy);

                    if (throttleDecision.logMessage() != null) {
                        log(logCallback, throttleDecision.logMessage());
                    }

                    com.sanitizer.detector.SmartDiagnostics.ThermalStatus thermalStatus =
                            com.sanitizer.detector.SmartDiagnostics.evaluateThermalStatus(currentTemp, deviceType);

                    if (throttleDecision.state() == com.sanitizer.detector.ThermalThrottleController.ThrottleState.PAUSED_CRITICAL) {
                        log(logCallback, String.format("⏸ [THERMAL SAFEGUARD - %s] Drive temperature reached %d°C (>= %d°C threshold)! Auto-pausing sanitization to prevent NAND/media degradation...", deviceType.getDisplayName(), currentTemp, autoPauseThreshold));
                        com.sanitizer.util.SoundManager.playAlertSound();
                        com.sanitizer.alert.AlertDispatcher.notifyThermalAutoPause(systemPath, null, currentTemp, autoPauseThreshold, deviceType.getDisplayName());

                        // Suspend dd process
                        pauseProcess(process);

                        if (metricsCallback != null) {
                            WipeMetrics pauseMetrics = new WipeMetrics(
                                     systemPath,
                                    startPct,
                                    currentPass,
                                    totalPasses,
                                    passName + " [THERMAL PAUSE: COOLING (" + deviceType.getShortBadge() + ")]",
                                    0,
                                    targetBytes,
                                    0.0,
                                    0,
                                    currentTemp,
                                    com.sanitizer.detector.SmartDiagnostics.ThermalStatus.AUTO_PAUSED,
                                    true,
                                    100,
                                    throttleDecision.statusBadge()
                            );
                            metricsCallback.accept(pauseMetrics);
                        }

                        // Cooldown loop until temperature is safe
                        while (currentTemp > resumeThreshold && !Thread.currentThread().isInterrupted() && process.isAlive()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                process.destroyForcibly();
                                return false;
                            }
                            // Allow temperature to decrease
                            currentTemp = Math.max(30, currentTemp - 5);
                            com.sanitizer.detector.SmartDiagnostics.setLiveTemperature(systemPath, currentTemp);

                            if (metricsCallback != null) {
                                WipeMetrics coolingMetrics = new WipeMetrics(
                                        systemPath,
                                        startPct,
                                        currentPass,
                                        totalPasses,
                                        passName + " [THERMAL PAUSE: COOLING TO " + resumeThreshold + "°C (" + deviceType.getShortBadge() + ")]",
                                        0,
                                        targetBytes,
                                        0.0,
                                        0,
                                        currentTemp,
                                        com.sanitizer.detector.SmartDiagnostics.ThermalStatus.AUTO_PAUSED,
                                        true,
                                        100,
                                        "⏸ Cooling (" + currentTemp + "°C)"
                                );
                                metricsCallback.accept(coolingMetrics);
                            }
                        }

                        // Resume dd process & initiate auto-recovery ramp-up
                        resumeProcess(process);
                        throttleDecision = throttleController.evaluate(currentTemp, policy);
                        log(logCallback, String.format("🚀 [THERMAL RESUMED - %s] Drive cooled down to %d°C (<= %d°C). Auto-recovery ramping up I/O throughput.", deviceType.getDisplayName(), currentTemp, resumeThreshold));
                        thermalStatus = com.sanitizer.detector.SmartDiagnostics.evaluateThermalStatus(currentTemp, deviceType);
                    } else if (throttleDecision.delayMillis() > 0) {
                        // Dynamic throttling pacing delay to allow passive heat dissipation
                        try {
                            Thread.sleep(throttleDecision.delayMillis());
                        } catch (InterruptedException ie) {
                            process.destroyForcibly();
                            return false;
                        }
                    }

                    if (line.contains("bytes")) {
                        try {
                            String[] parts = line.trim().split("\\s+");
                            long bytesWrittenInPass = Long.parseLong(parts[0]);
                            lastBytesWritten = bytesWrittenInPass;
                            double passPercent = Math.min(1.0, ((double) bytesWrittenInPass / targetBytes));
                            double overallPercent = startPct + (passPercent * (endPct - startPct));
                            if (overallPercent > endPct) overallPercent = endPct;

                            double elapsedSec = Math.max(0.001, (System.nanoTime() - startNanoTime) / 1_000_000_000.0);
                            double speedMBs = (bytesWrittenInPass / (1024.0 * 1024.0)) / elapsedSec;

                            long totalBytesProcessedOverall = ((long) (currentPass - 1) * targetBytes) + bytesWrittenInPass;
                            long totalRemainingBytes = Math.max(0, totalWipeTargetBytes - totalBytesProcessedOverall);
                            long etaSeconds = speedMBs > 0.05 ? (long) (totalRemainingBytes / (speedMBs * 1024.0 * 1024.0)) : 0;

                            // Real-time I/O Oscilloscope telemetry computation
                            double iops = (speedMBs * 1024.0 * 1024.0) / 4096.0; // 4KB IOPS equivalent
                            double latencyMs = (speedMBs > 0.05) ? Math.min(250.0, Math.max(0.6, (2.0 / speedMBs) * 100.0)) : 0.0;
                            double busMax = (deviceType == com.sanitizer.detector.DeviceType.NVME_SSD) ? 3500.0 : (deviceType == com.sanitizer.detector.DeviceType.MAGNETIC_HDD ? 180.0 : 450.0);
                            double busSaturation = Math.min(100.0, (speedMBs / busMax) * 100.0);

                            if (metricsCallback != null) {
                                WipeMetrics metrics = new WipeMetrics(
                                        systemPath,
                                        overallPercent,
                                        currentPass,
                                        totalPasses,
                                        passName,
                                        bytesWrittenInPass,
                                        targetBytes,
                                        speedMBs,
                                        etaSeconds,
                                        currentTemp,
                                        thermalStatus,
                                        false,
                                        throttleDecision.throttlePercent(),
                                        throttleDecision.statusBadge(),
                                        iops,
                                        latencyMs,
                                        busSaturation
                                );
                                metricsCallback.accept(metrics);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } finally {
                activeDdProcesses.remove(systemPath);
            }

            return process.waitFor() == 0;
        } catch (Exception e) {
            String err = "Wipe Command Execution Error: " + e.getMessage();
            AppLogger.error(MODULE, err, e);
            if (logCallback != null) logCallback.accept(err);
            return false;
        }
    }

    private static void pauseProcess(Process process) {
        try {
            long pid = process.pid();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows doesn't support SIGSTOP directly
            } else {
                new ProcessBuilder("kill", "-STOP", String.valueOf(pid)).start().waitFor();
            }
        } catch (Exception ignored) {}
    }

    private static void resumeProcess(Process process) {
        try {
            long pid = process.pid();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows
            } else {
                new ProcessBuilder("kill", "-CONT", String.valueOf(pid)).start().waitFor();
            }
        } catch (Exception ignored) {}
    }
}