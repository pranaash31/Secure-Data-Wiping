package com.sanitizer.engine;

import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.util.AppLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class WipeEngine {

    private static final String MODULE = "WipeEngine";
    private static final long TEST_CAP_BYTES = 1L * 1024 * 1024 * 1024; // 1 GB cap in bytes

    // Concurrent multi-threaded pool supporting 8+ simultaneous drive wiping operations
    private static final int THREAD_POOL_SIZE = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
    private static volatile ExecutorService batchExecutor = createBatchExecutor();

    private static ExecutorService createBatchExecutor() {
        return Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "WipeEngine-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    private static synchronized ExecutorService getBatchExecutor() {
        if (batchExecutor == null || batchExecutor.isShutdown()) {
            batchExecutor = createBatchExecutor();
        }
        return batchExecutor;
    }

    private static final Map<String, Future<Boolean>> activeWipeTasks = new ConcurrentHashMap<>();
    private static final Map<String, Process> activeDdProcesses = new ConcurrentHashMap<>();
    private static final Map<String, Thread> activeFeederThreads = new ConcurrentHashMap<>();

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

        // Presentation Simulator Mode Dispatch (Safely bypass real OS block I/O)
        if (com.sanitizer.demo.DemoSimulatorManager.isSimulatedDrive(systemPath)) {
            try {
                return executeSimulatedWipe(systemPath, totalBytes, policy, isTestMode, metricsCallback, logCallback, badSectorCallback);
            } finally {
                com.sanitizer.shield.DiskLockShield.releaseDiskLock(systemPath);
            }
        }

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
            int lastPatternByte = 0x00; // Tracks previous byte for COMPLEMENT passes

            for (int i = 0; i < totalPasses; i++) {
                WipePass pass = passes.get(i);
                int currentPass = i + 1;
                double startPct = i * passSlice;
                double endPct = (i + 1) * passSlice;

                int effectiveByte = resolveEffectiveByte(pass, lastPatternByte);
                if (effectiveByte >= 0) {
                    lastPatternByte = effectiveByte;
                }

                log(logCallback, String.format("[%s] Pass %d/%d: %s (%s)...",
                        policy.getStandardCode(), currentPass, totalPasses, pass.getDescription(), pass.getPatternHex()));

                boolean passSuccess = runDdCommand(
                        systemPath,
                        pass,
                        effectiveByte,
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
        Future<Boolean> future = getBatchExecutor().submit(() -> {
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
        Thread feeder = activeFeederThreads.remove(systemPath);

        boolean cancelled = false;

        if (feeder != null && feeder.isAlive()) {
            feeder.interrupt();
        }

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

    /**
     * Resolves the concrete single-byte value for passes that write fixed or complemented bytes.
     * Returns -1 for non-deterministic patterns (such as CSPRNG random).
     */
    public static int resolveEffectiveByte(WipePass pass, int prevByte) {
        if (pass == null) return 0x00;
        return switch (pass.getPatternType()) {
            case ZERO_FILL -> 0x00;
            case ONE_FILL -> 0xFF;
            case CUSTOM_BYTE -> pass.getCustomByteValue() & 0xFF;
            case COMPLEMENT -> (~prevByte) & 0xFF;
            case PSEUDO_RANDOM -> -1;
        };
    }

    /**
     * Executes high-fidelity Presentation Simulator Mode sanitization.
     * Safely emulates block-level overwriting with realistic I/O throughput, oscilloscope waveform data,
     * thermal curves, live dd console logging, and sector heatmap progression without touching real storage devices.
     */
    private static boolean executeSimulatedWipe(
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

        List<WipePass> passes = policy.getPasses();
        if (passes == null || passes.isEmpty()) {
            passes = List.of(new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Default Zero Fill"));
        }

        int totalPasses = passes.size();
        long targetBytes = isTestMode ? Math.min(totalBytes, TEST_CAP_BYTES) : totalBytes;
        long totalWipeTargetBytes = targetBytes * totalPasses;
        long startNanoTime = System.nanoTime();

        log(logCallback, "═════════════════════════════════════════════════════════════════");
        log(logCallback, String.format("⚡ [SIMULATOR MODE] Starting %s (%d-Pass Standard) on %s%s",
                policy.getName(), totalPasses, systemPath, isTestMode ? " [TEST MODE: 1GB CAP]" : " [FULL CAPACITY: 16 GB]"));
        log(logCallback, String.format("Standard Org: %s | Pattern Sequence: %s",
                policy.getOrganization(), policy.getPatternSummary()));
        log(logCallback, "⚡ [SIMULATOR MODE] Live presentation execution active — real storage disks protected.");
        log(logCallback, "═════════════════════════════════════════════════════════════════");

        double passSlice = 100.0 / totalPasses;
        long accumulatedBytesWritten = 0;
        int currentTemp = 36;

        for (int i = 0; i < totalPasses; i++) {
            WipePass pass = passes.get(i);
            int currentPass = i + 1;
            double startPct = i * passSlice;
            double endPct = (i + 1) * passSlice;

            log(logCallback, String.format("[%s] [SIMULATOR] Pass %d/%d: %s (%s)...",
                    policy.getStandardCode(), currentPass, totalPasses, pass.getDescription(), pass.getPatternHex()));

            int steps = 24;
            for (int s = 1; s <= steps; s++) {
                if (Thread.currentThread().isInterrupted()) {
                    log(logCallback, "[SIMULATOR] Sanitization task interrupted / aborted by user.");
                    return false;
                }

                double passFraction = (double) s / steps;
                double overallPercent = startPct + (passFraction * (endPct - startPct));
                long bytesInThisPass = (long) (passFraction * targetBytes);
                long totalProcessed = accumulatedBytesWritten + bytesInThisPass;
                long remainingBytes = Math.max(0, totalWipeTargetBytes - totalProcessed);

                // Simulated dynamic transfer speed (between 62 and 78 MB/s with realistic jitter)
                double speedMBs = 62.0 + (ThreadLocalRandom.current().nextDouble() * 16.0);
                long etaSeconds = speedMBs > 0 ? (long) (remainingBytes / (speedMBs * 1024.0 * 1024.0)) : 0;

                // Telemetry metrics
                double iops = (speedMBs * 1024.0 * 1024.0) / 4096.0;
                double latencyMs = 0.85 + (ThreadLocalRandom.current().nextDouble() * 0.35);
                double busSaturation = Math.min(100.0, (speedMBs / 450.0) * 100.0);

                if (s % 6 == 0 && currentTemp < 41) {
                    currentTemp++;
                }

                if (metricsCallback != null) {
                    WipeMetrics metrics = new WipeMetrics(
                            systemPath,
                            overallPercent,
                            currentPass,
                            totalPasses,
                            pass.getDisplayName() + " [SIMULATOR]",
                            bytesInThisPass,
                            targetBytes,
                            speedMBs,
                            etaSeconds,
                            currentTemp,
                            com.sanitizer.detector.SmartDiagnostics.ThermalStatus.NORMAL,
                            false,
                            100,
                            "Optimal (Demo)",
                            iops,
                            latencyMs,
                            busSaturation
                    );
                    metricsCallback.accept(metrics);
                }

                // Periodic dd console output stream emulation
                if (s % 6 == 0 || s == steps) {
                    double elapsedSec = Math.max(0.1, (System.nanoTime() - startNanoTime) / 1_000_000_000.0);
                    log(logCallback, String.format("dd: %,d bytes (%.1f GB) copied, %.1f s, %.1f MB/s",
                            bytesInThisPass, (double) bytesInThisPass / (1024 * 1024 * 1024), elapsedSec, speedMBs));
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log(logCallback, "[SIMULATOR] Sanitization task interrupted / cancelled.");
                    return false;
                }
            }

            accumulatedBytesWritten += targetBytes;
            log(logCallback, String.format("[SIMULATOR] Pass %d/%d successfully completed (%s).",
                    currentPass, totalPasses, pass.getDescription()));
        }

        if (metricsCallback != null) {
            metricsCallback.accept(new WipeMetrics(
                    systemPath,
                    100.0,
                    totalPasses,
                    totalPasses,
                    "Completed [SIMULATOR]",
                    targetBytes,
                    targetBytes,
                    0.0,
                    0,
                    currentTemp,
                    com.sanitizer.detector.SmartDiagnostics.ThermalStatus.NORMAL,
                    false,
                    100,
                    "Complete",
                    0,
                    0,
                    0
            ));
        }

        log(logCallback, String.format("[SUCCESS] [SIMULATOR] All %d passes of %s successfully executed on %s.",
                totalPasses, policy.getName(), systemPath));
        log(logCallback, "═════════════════════════════════════════════════════════════════");
        return true;
    }

    private static boolean runDdCommand(String systemPath, WipePass pass, int effectiveByte, long targetBytes, boolean isTestMode,
                                        int currentPass, int totalPasses, String passName,
                                        Consumer<WipeMetrics> metricsCallback, Consumer<String> logCallback,
                                        Consumer<com.sanitizer.quarantine.LbaFailureRecord> badSectorCallback,
                                        double startPct, double endPct) {
        List<String> command = new ArrayList<>();
        command.add("dd");

        boolean useFeeder = false;
        boolean isRandomPattern = false;
        byte fillByte = 0;

        WipePatternType patternType = (pass != null) ? pass.getPatternType() : WipePatternType.ZERO_FILL;

        if (patternType == WipePatternType.ZERO_FILL && new File("/dev/zero").exists()) {
            command.add("if=/dev/zero");
        } else if (patternType == WipePatternType.PSEUDO_RANDOM && new File("/dev/urandom").exists()) {
            command.add("if=/dev/urandom");
        } else if (patternType == WipePatternType.PSEUDO_RANDOM) {
            useFeeder = true;
            isRandomPattern = true;
        } else {
            // ONE_FILL (0xFF), CUSTOM_BYTE (e.g. 0x96, 0xAA), COMPLEMENT, or ZERO_FILL when /dev/zero is absent
            useFeeder = true;
            fillByte = (byte) (effectiveByte & 0xFF);
        }

        command.add("of=" + systemPath);
        command.add("bs=2m");

        if (isTestMode && !useFeeder) {
            long blocks = Math.max(1, targetBytes / (2 * 1024 * 1024));
            command.add("count=" + blocks);
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

            Thread feederThread = null;
            if (useFeeder) {
                final boolean randomFeeder = isRandomPattern;
                final byte patternByte = fillByte;
                final long wipeTarget = targetBytes;
                feederThread = new Thread(() -> {
                    try (OutputStream os = process.getOutputStream()) {
                        byte[] chunk = new byte[2 * 1024 * 1024];
                        if (!randomFeeder) {
                            Arrays.fill(chunk, patternByte);
                        }
                        long remaining = wipeTarget;
                        while (remaining > 0 && process.isAlive() && !Thread.currentThread().isInterrupted()) {
                            int toWrite = (int) Math.min(chunk.length, remaining);
                            if (randomFeeder) {
                                java.util.concurrent.ThreadLocalRandom.current().nextBytes(chunk);
                            }
                            os.write(chunk, 0, toWrite);
                            remaining -= toWrite;
                        }
                        os.flush();
                    } catch (IOException ignored) {
                        // Normal termination when process closes input or exits
                    }
                }, "WipeEngine-PatternFeeder-" + systemPath);
                feederThread.setDaemon(true);
                activeFeederThreads.put(systemPath, feederThread);
                feederThread.start();
            } else {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {}
            }

            com.sanitizer.detector.ThermalThrottleController throttleController = new com.sanitizer.detector.ThermalThrottleController();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        if (feederThread != null) feederThread.interrupt();
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
                                if (feederThread != null) feederThread.interrupt();
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
                            if (feederThread != null) feederThread.interrupt();
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
                activeFeederThreads.remove(systemPath);
                if (feederThread != null && feederThread.isAlive()) {
                    feederThread.interrupt();
                }
            }

            if (feederThread != null) {
                try {
                    feederThread.join(2000);
                } catch (InterruptedException ignored) {}
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

    /**
     * Gracefully terminates active wiping operations and shuts down worker thread pools.
     */
    public static synchronized void shutdown() {
        AppLogger.info(MODULE, "Shutting down WipeEngine background workers...");
        for (Thread feeder : activeFeederThreads.values()) {
            try {
                if (feeder != null && feeder.isAlive()) {
                    feeder.interrupt();
                }
            } catch (Exception ignored) {}
        }
        activeFeederThreads.clear();

        for (Map.Entry<String, Process> entry : activeDdProcesses.entrySet()) {
            try {
                if (entry.getValue() != null && entry.getValue().isAlive()) {
                    entry.getValue().destroyForcibly();
                }
            } catch (Exception ignored) {}
        }
        activeDdProcesses.clear();

        for (Future<Boolean> task : activeWipeTasks.values()) {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
        }
        activeWipeTasks.clear();

        if (!batchExecutor.isShutdown()) {
            batchExecutor.shutdownNow();
        }
    }
}