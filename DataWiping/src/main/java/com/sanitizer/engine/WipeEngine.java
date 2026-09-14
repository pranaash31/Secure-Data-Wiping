package com.sanitizer.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WipeEngine {

    // DEV TEST TOGGLE DEFAULT
    private static final long TEST_CAP_BYTES = 1L * 1024 * 1024 * 1024; // 1 GB cap in bytes

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
            String err = "CRITICAL ERROR: Primary system drive blocked from wiping!";
            System.err.println(err);
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

    private static void log(Consumer<String> logCallback, String msg) {
        System.out.println(msg);
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

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
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
            }

            return process.waitFor() == 0;
        } catch (Exception e) {
            String err = "Wipe Command Execution Error: " + e.getMessage();
            System.err.println(err);
            if (logCallback != null) logCallback.accept(err);
            return false;
        }
    }
}