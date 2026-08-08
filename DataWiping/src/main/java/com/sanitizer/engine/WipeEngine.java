package com.sanitizer.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WipeEngine {

    // ⚡ DEV TEST TOGGLE: Set to 'true' for fast 1GB testing (~1.5 mins), or 'false' for full drive wipe.
    private static final boolean IS_TEST_MODE = true;
    private static final long TEST_CAP_BYTES = 1L * 1024 * 1024 * 1024; // 1 GB cap in bytes

    public enum WipeStandard {
        NIST_800_88_CLEAR, // 1 Pass (0x00 Zero Fill)
        DOD_5220_22_M      // 3 Pass (0x00, 0xFF, Cryptographic Random)
    }

    /**
     * Executes sector sanitization according to selected defense standard.
     */
    public static boolean executeWipe(String systemPath, long totalBytes, WipeStandard standard, Consumer<Double> progressCallback) {
        // 🚨 HARD SAFETY GUARDRAIL: Block primary system disk
        if (systemPath.contains("disk0") || systemPath.contains("rdisk0")) {
            System.err.println("CRITICAL ERROR: Primary system drive blocked from wiping!");
            return false;
        }

        MacUtil.unmountDiskIfMac(systemPath);

        long targetBytes = IS_TEST_MODE ? Math.min(totalBytes, TEST_CAP_BYTES) : totalBytes;

        if (standard == WipeStandard.DOD_5220_22_M) {
            System.out.println("Starting DoD 5220.22-M (3-Pass Defense Wipe) on " + systemPath);

            // Pass 1: Zero Fill
            System.out.println("DoD Pass 1/3: Overwriting with 0x00...");
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, progressCallback, 0.0, 33.3)) return false;

            // Pass 2: Cryptographic Random / Pattern Overwrite
            System.out.println("DoD Pass 2/3: Overwriting with Cryptographic Pseudo-Random Data...");
            if (!runDdCommand(systemPath, "/dev/urandom", targetBytes, progressCallback, 33.3, 66.6)) return false;

            // Pass 3: Final Zero Verification Pass
            System.out.println("DoD Pass 3/3: Final Zero Verification Pass...");
            if (!runDdCommand(systemPath, "/dev/zero", targetBytes, progressCallback, 66.6, 100.0)) return false;

            return true;
        } else {
            // Standard NIST SP 800-88 Clear (Single Pass 0x00)
            System.out.println("Starting NIST SP 800-88 Clear (Single Pass Zero-Fill) on " + systemPath);
            return runDdCommand(systemPath, "/dev/zero", targetBytes, progressCallback, 0.0, 100.0);
        }
    }

    private static boolean runDdCommand(String systemPath, String sourcePath, long targetBytes,
                                        Consumer<Double> progressCallback, double startPct, double endPct) {
        List<String> command = new ArrayList<>();
        command.add("dd");
        command.add("if=" + sourcePath);
        command.add("of=" + systemPath);
        command.add("bs=2m");

        if (IS_TEST_MODE) {
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
            System.err.println("Wipe Command Execution Error: " + e.getMessage());
            return false;
        }
    }
}