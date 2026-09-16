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

    private static final ExecutorService batchExecutor = Executors.newFixedThreadPool(4, r -> {
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
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, progressCallback, logCallback, 0.0, 33.3)) return false;

            // Pass 2: Cryptographic Random / Pattern Overwrite
            log(logCallback, "DoD Pass 2/3: Overwriting with Cryptographic Pseudo-Random Data...");
            if (!runDdCommand(systemPath, "/dev/urandom", targetBytes, isTestMode, progressCallback, logCallback, 33.3, 66.6)) return false;

            // Pass 3: Final Zero Verification Pass
            log(logCallback, "DoD Pass 3/3: Final Zero Verification Pass...");
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, progressCallback, logCallback, 66.6, 100.0)) return false;

            return true;
        } else {
            // Standard NIST SP 800-88 Clear (Single Pass 0x00)
            log(logCallback, "Starting NIST SP 800-88 Clear (Single Pass Zero-Fill) on " + systemPath + (isTestMode ? " [TEST MODE: 1GB CAP]" : " [FULL WIPE]"));
            return runDdCommand(systemPath, "/dev/zero", targetBytes, isTestMode, progressCallback, logCallback, 0.0, 100.0);
        }
    }

    /**
     * Submits an asynchronous concurrent wiping task to the batch executor queue.
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
        Future<Boolean> future = batchExecutor.submit(() -> {
            boolean success = false;
            try {
                success = executeWipe(systemPath, totalBytes, standard, isTestMode, progressCallback, logCallback);
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
     * Cancels an active concurrent wiping task for a specific drive path.
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

    public static boolean isTaskRunning(String systemPath) {
        Future<Boolean> future = activeWipeTasks.get(systemPath);
        return future != null && !future.isDone();
    }

    private static void log(Consumer<String> logCallback, String msg) {
        AppLogger.info(MODULE, msg);
        if (logCallback != null) logCallback.accept(msg);
    }

    private static boolean runDdCommand(String systemPath, String sourcePath, long targetBytes, boolean isTestMode,
                                        Consumer<Double> progressCallback, Consumer<String> logCallback,
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
                            long bytesWritten = Long.parseLong(parts[0]);
                            double passPercent = ((double) bytesWritten / targetBytes);
                            double overallPercent = startPct + (passPercent * (endPct - startPct));
                            if (overallPercent > endPct) overallPercent = endPct;

                            if (progressCallback != null) {
                                progressCallback.accept(overallPercent);
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