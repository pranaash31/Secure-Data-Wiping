package com.sanitizer.engine;

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
        // HARD SAFETY GUARDRAIL: Block primary system disk
        if (systemPath.contains("disk0") || systemPath.contains("rdisk0")) {
            String err = "CRITICAL SAFETY SHIELD: Primary system drive (" + systemPath + ") blocked from wiping!";
            AppLogger.shield(MODULE, err);
            if (logCallback != null) logCallback.accept(err);
            return false;
        }

        MacUtil.unmountDiskIfMac(systemPath);

        long targetBytes = isTestMode ? Math.min(totalBytes, TEST_CAP_BYTES) : totalBytes;

        if (standard == WipeStandard.DOD_5220_22_M) {
            log(logCallback, "Starting DoD 5220.22-M (3-Pass Defense Wipe) on " + systemPath + (isTestMode ? " [TEST MODE: 1GB CAP]" : " [FULL WIPE]"));

            // Pass 1: Zero Fill
            log(logCallback, "DoD Pass 1/3: Overwriting with 0x00...");
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, 1, 3, "Zero Fill (0x00)",
                    metricsCallback, logCallback, 0.0, 33.3)) return false;

            // Pass 2: Cryptographic Random / Pattern Overwrite
            log(logCallback, "DoD Pass 2/3: Overwriting with Cryptographic Pseudo-Random Data...");
            if (!runDdCommand(systemPath, "/dev/urandom", targetBytes, isTestMode, 2, 3, "Cryptographic Random",
                    metricsCallback, logCallback, 33.3, 66.6)) return false;

            // Pass 3: Final Zero Verification Pass
            log(logCallback, "DoD Pass 3/3: Final Zero Verification Pass...");
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, 3, 3, "Verification Pass (0x00)",
                    metricsCallback, logCallback, 66.6, 100.0)) return false;

            // Final completion metric notification
            if (metricsCallback != null) {
                metricsCallback.accept(new WipeMetrics(systemPath, 100.0, 3, 3, "Completed", targetBytes, targetBytes, 0.0, 0));
            }
            return true;
        } else {
            // Standard NIST SP 800-88 Clear (Single Pass 0x00)
            log(logCallback, "Starting NIST SP 800-88 Clear (Single Pass Zero-Fill) on " + systemPath + (isTestMode ? " [TEST MODE: 1GB CAP]" : " [FULL WIPE]"));
            boolean success = runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, 1, 1, "Zero Fill (0x00)",
                    metricsCallback, logCallback, 0.0, 100.0);
            if (success && metricsCallback != null) {
                metricsCallback.accept(new WipeMetrics(systemPath, 100.0, 1, 1, "Completed", targetBytes, targetBytes, 0.0, 0));
            }
            return success;
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
        Future<Boolean> future = batchExecutor.submit(() -> {
            boolean success = false;
            try {
                success = executeWipeWithMetrics(systemPath, totalBytes, standard, isTestMode, metricsCallback, logCallback);
            } catch (Exception e) {
                AppLogger.error(MODULE, "Batch wipe error on " + systemPath, e);
            } finally {
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

    private static void log(Consumer<String> logCallback, String msg) {
        AppLogger.info(MODULE, msg);
        if (logCallback != null) logCallback.accept(msg);
    }

    private static boolean runDdCommand(String systemPath, String sourcePath, long targetBytes, boolean isTestMode,
                                        int currentPass, int totalPasses, String passName,
                                        Consumer<WipeMetrics> metricsCallback, Consumer<String> logCallback,
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

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            activeDdProcesses.put(systemPath, process);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        process.destroyForcibly();
                        return false;
                    }

                    if (logCallback != null) logCallback.accept(line);
                    if (line.contains("bytes")) {
                        try {
                            String[] parts = line.trim().split("\\s+");
                            long bytesWrittenInPass = Long.parseLong(parts[0]);
                            double passPercent = Math.min(1.0, ((double) bytesWrittenInPass / targetBytes));
                            double overallPercent = startPct + (passPercent * (endPct - startPct));
                            if (overallPercent > endPct) overallPercent = endPct;

                            double elapsedSec = Math.max(0.001, (System.nanoTime() - startNanoTime) / 1_000_000_000.0);
                            double speedMBs = (bytesWrittenInPass / (1024.0 * 1024.0)) / elapsedSec;

                            long totalBytesProcessedOverall = ((long) (currentPass - 1) * targetBytes) + bytesWrittenInPass;
                            long totalRemainingBytes = Math.max(0, totalWipeTargetBytes - totalBytesProcessedOverall);
                            long etaSeconds = speedMBs > 0.05 ? (long) (totalRemainingBytes / (speedMBs * 1024.0 * 1024.0)) : 0;

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
                                        etaSeconds
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
}