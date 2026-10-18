package com.sanitizer.engine;

import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Automated Post-Wipe Sampling & Residual Entropy Verification Engine.
 * Conforms to NIST SP 800-88 Rev. 1 Section 4.7 & ISO/IEC 27040:2015 standards.
 *
 * Automatically reads back physical storage media sectors using representative strided
 * sampling across beginning, middle, end, and pseudo-random strides, calculates Shannon
 * entropy (H = -sum(p * log2(p))), verifies pattern compliance, and produces a
 * cryptographic SHA-256 digest proof.
 */
public class WipeVerifier {

    private static final String MODULE = "WipeVerifier";
    public static final int SECTOR_SIZE_BYTES = 512;
    private static final int BUFFER_CHUNK_SIZE = 64 * 1024; // 64 KB chunk

    public enum VerificationMode {
        FAST_SAMPLE_5_PERCENT("Fast 5% Sampling (Beginning, Middle, End & Random Strides)", 0.05),
        EXTENSIVE_SAMPLE_10_PERCENT("Extensive 10% Uniform Sampling", 0.10),
        FULL_100_PERCENT("Full 100% Read-Back Verification", 1.00);

        private final String displayName;
        private final double sampleRatio;

        VerificationMode(String displayName, double sampleRatio) {
            this.displayName = displayName;
            this.sampleRatio = sampleRatio;
        }

        public String getDisplayName() { return displayName; }
        public double getSampleRatio() { return sampleRatio; }
    }

    public record VerificationResult(
            boolean isClean,
            String statusSummary,
            long totalSectorsVerified,
            double entropyScore,
            String sha256Proof,
            long sampleBytesRead,
            long nonZeroBytesFound,
            String verificationMethod
    ) {}

    /**
     * Executes automated read-back verification against the sanitized media with policy context.
     *
     * @param systemPath Target block device or raw device path (e.g. /dev/rdisk4)
     * @param totalBytes Total physical drive capacity in bytes
     * @param mode Selected sampling intensity mode
     * @param policy Sanitization policy defining the expected pattern (or null for zero-fill)
     * @param isTestMode If true, caps evaluation to test boundaries safely
     * @param progressCallback Progress observer (0.0 to 1.0)
     * @param logCallback Log message observer
     * @return Mathematical verification attestation result
     */
    public static VerificationResult verifyDrive(
            String systemPath,
            long totalBytes,
            VerificationMode mode,
            WipePolicy policy,
            boolean isTestMode,
            Consumer<Double> progressCallback,
            Consumer<String> logCallback
    ) {
        if (mode == null) mode = VerificationMode.FAST_SAMPLE_5_PERCENT;

        log(logCallback, "═════════════════════════════════════════════════════════════════");
        log(logCallback, "[VERIFIER] Initiating Automated Post-Wipe Verification Pass...");
        log(logCallback, "[VERIFIER] Standard: NIST SP 800-88 Rev. 1 Section 4.7 & ISO/IEC 27040");
        log(logCallback, "[VERIFIER] Sampling Mode: " + mode.getDisplayName());

        File devFile = new File(systemPath);
        if (!devFile.exists()) {
            String status = "FAIL — Verification Aborted: Target device or file not found (" + systemPath + ")";
            log(logCallback, "[VERIFIER] " + status);
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return new VerificationResult(
                    false,
                    status,
                    0,
                    Double.NaN,
                    "N/A",
                    0,
                    0,
                    "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015"
            );
        }

        if (!devFile.canRead()) {
            String status = "FAIL — Read Verification Failed: Permission denied for " + systemPath;
            log(logCallback, "[VERIFIER] " + status);
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return new VerificationResult(
                    false,
                    status,
                    0,
                    Double.NaN,
                    "N/A",
                    0,
                    0,
                    "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015"
            );
        }

        long targetCapacity = isTestMode ? Math.min(totalBytes, 1L * 1024 * 1024 * 1024) : totalBytes;
        if (devFile.isFile() && devFile.length() > 0) {
            targetCapacity = Math.min(targetCapacity, devFile.length());
        }

        if (targetCapacity <= 0) {
            String status = "FAIL — Read Verification Failed: Device capacity is 0 bytes";
            log(logCallback, "[VERIFIER] " + status);
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return new VerificationResult(
                    false,
                    status,
                    0,
                    Double.NaN,
                    "N/A",
                    0,
                    0,
                    "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015"
            );
        }

        long bytesToSample = (long) Math.ceil(targetCapacity * mode.getSampleRatio());
        // Minimum 2048 sectors (1 MB) if disk capacity allows, but never exceed targetCapacity
        bytesToSample = Math.min(targetCapacity, Math.max(Math.min(targetCapacity, 2048L * SECTOR_SIZE_BYTES), bytesToSample));
        long sectorsToSample = bytesToSample / SECTOR_SIZE_BYTES;

        log(logCallback, String.format("[VERIFIER] Target Sectors to Verify: %,d LBAs (%,d KB)", sectorsToSample, bytesToSample / 1024));

        WipePass finalPass = (policy != null && policy.getPasses() != null && !policy.getPasses().isEmpty())
                ? policy.getPasses().get(policy.getPasses().size() - 1)
                : null;
        WipePatternType patternType = finalPass != null ? finalPass.getPatternType() : WipePatternType.ZERO_FILL;
        int expectedByte = resolveExpectedByte(finalPass);

        long nonCompliantCount = 0;
        long bytesReadTotal = 0;
        int[] byteFrequencies = new int[256];
        MessageDigest sha256Digest;

        try {
            sha256Digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }

        int chunkSize = (int) Math.min(BUFFER_CHUNK_SIZE, targetCapacity);
        int numBlocks = (int) Math.max(1, bytesToSample / chunkSize);

        boolean fullScan = (mode == VerificationMode.FULL_100_PERCENT) || (bytesToSample >= targetCapacity);
        List<Long> sampleOffsets = fullScan ? null : calculateSampleOffsets(targetCapacity, chunkSize, numBlocks);

        byte[] chunk = new byte[chunkSize];

        try (RandomAccessFile raf = new RandomAccessFile(devFile, "r")) {
            if (fullScan) {
                long remaining = targetCapacity;
                while (remaining > 0) {
                    int toRead = (int) Math.min(chunk.length, remaining);
                    int read = raf.read(chunk, 0, toRead);
                    if (read <= 0) break;

                    for (int i = 0; i < read; i++) {
                        int b = chunk[i] & 0xFF;
                        byteFrequencies[b]++;
                        if (isDiscrepancy(patternType, expectedByte, b)) {
                            nonCompliantCount++;
                        }
                    }

                    sha256Digest.update(chunk, 0, read);
                    bytesReadTotal += read;
                    remaining -= read;

                    if (progressCallback != null && targetCapacity > 0) {
                        progressCallback.accept(Math.min(1.0, (double) bytesReadTotal / targetCapacity));
                    }
                }
            } else {
                for (long offset : sampleOffsets) {
                    try {
                        raf.seek(offset);
                    } catch (IOException seekEx) {
                        log(logCallback, "[VERIFIER] Seek error at offset " + offset + ": " + seekEx.getMessage());
                        throw seekEx;
                    }

                    int toRead = (int) Math.min(chunk.length, targetCapacity - offset);
                    int read = raf.read(chunk, 0, toRead);
                    if (read <= 0) break;

                    for (int i = 0; i < read; i++) {
                        int b = chunk[i] & 0xFF;
                        byteFrequencies[b]++;
                        if (isDiscrepancy(patternType, expectedByte, b)) {
                            nonCompliantCount++;
                        }
                    }

                    sha256Digest.update(chunk, 0, read);
                    bytesReadTotal += read;

                    if (progressCallback != null && bytesToSample > 0) {
                        progressCallback.accept(Math.min(1.0, (double) bytesReadTotal / bytesToSample));
                    }
                }
            }
        } catch (Exception ex) {
            AppLogger.error(MODULE, "Direct disk read failed: " + ex.getMessage(), ex);
            String status = "FAIL — Read Verification Failed: " + ex.getMessage();
            log(logCallback, "[VERIFIER] " + status);
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return new VerificationResult(
                    false,
                    status,
                    bytesReadTotal / SECTOR_SIZE_BYTES,
                    bytesReadTotal > 0 ? calculateShannonEntropy(byteFrequencies, bytesReadTotal) : Double.NaN,
                    bytesReadTotal > 0 ? "SHA256:" + bytesToHex(sha256Digest.digest()) : "N/A",
                    bytesReadTotal,
                    nonCompliantCount,
                    "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015"
            );
        }

        if (bytesReadTotal == 0) {
            String status = "FAIL — Read Verification Failed: Zero bytes read from media";
            log(logCallback, "[VERIFIER] " + status);
            log(logCallback, "═════════════════════════════════════════════════════════════════");
            return new VerificationResult(false, status, 0, Double.NaN, "N/A", 0, 0,
                    "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015");
        }

        if (progressCallback != null) {
            progressCallback.accept(1.0);
        }

        double entropy = calculateShannonEntropy(byteFrequencies, bytesReadTotal);
        byte[] hashBytes = sha256Digest.digest();
        String hashHex = bytesToHex(hashBytes);

        boolean isClean;
        String status;

        if (patternType == WipePatternType.PSEUDO_RANDOM) {
            isClean = (entropy >= 7.2);
            status = isClean
                    ? String.format("PASS — Cryptographic Random Residual Confirmed (%.4f bits/byte Entropy)", entropy)
                    : String.format("FAIL — Insufficient Random Residual Entropy: %.4f bits/byte (Threshold: 7.2)", entropy);
        } else {
            isClean = (nonCompliantCount == 0) && (entropy < 0.001);
            if (isClean) {
                if (patternType == WipePatternType.ONE_FILL) {
                    status = "PASS — One-Fill Residual Confirmed (0.000% Entropy)";
                } else if (patternType == WipePatternType.CUSTOM_BYTE) {
                    status = String.format("PASS — Fixed Pattern 0x%02X Residual Confirmed (0.000%% Entropy)", expectedByte);
                } else if (patternType == WipePatternType.COMPLEMENT) {
                    status = String.format("PASS — Complement Pattern 0x%02X Residual Confirmed (0.000%% Entropy)", expectedByte);
                } else {
                    status = "PASS — Zero Residual Data Confirmed (0.000% Entropy)";
                }
            } else {
                status = String.format("FAIL — Residual Bytes Detected (%,d non-compliant bytes, Entropy: %.4f bits/byte)",
                        nonCompliantCount, entropy);
            }
        }

        long sectorsVerified = bytesReadTotal / SECTOR_SIZE_BYTES;
        log(logCallback, String.format("[VERIFIER] Verification Completed: Verified %,d Sectors (%,d bytes).", sectorsVerified, bytesReadTotal));
        log(logCallback, String.format("[VERIFIER] Shannon Residual Entropy: %.4f bits/byte (%.3f%% Residual)", entropy, entropy * 100.0 / 8.0));
        log(logCallback, String.format("[VERIFIER] Non-Compliant Residual Bytes: %,d bytes", nonCompliantCount));
        log(logCallback, "[VERIFIER] SHA-256 Sample Proof Digest: " + hashHex);
        log(logCallback, "[VERIFIER] Attestation Status: " + status);
        log(logCallback, "═════════════════════════════════════════════════════════════════");

        return new VerificationResult(
                isClean,
                status,
                sectorsVerified,
                entropy,
                "SHA256:" + hashHex,
                bytesReadTotal,
                nonCompliantCount,
                "NIST SP 800-88 Rev. 1 Section 4.7 / ISO/IEC 27040:2015"
        );
    }

    /**
     * Executes automated read-back verification against the sanitized media (defaults to zero-fill).
     */
    public static VerificationResult verifyDrive(
            String systemPath,
            long totalBytes,
            VerificationMode mode,
            boolean isTestMode,
            Consumer<Double> progressCallback,
            Consumer<String> logCallback
    ) {
        return verifyDrive(systemPath, totalBytes, mode, null, isTestMode, progressCallback, logCallback);
    }

    /**
     * Generates representative sample offsets across Beginning, Middle, End, and uniform random strides
     * in accordance with NIST SP 800-88 Rev. 1 Section 4.7.
     */
    public static List<Long> calculateSampleOffsets(long capacity, long chunkSize, int numChunks) {
        List<Long> offsets = new ArrayList<>(numChunks);
        if (numChunks <= 1 || capacity <= chunkSize) {
            offsets.add(0L);
            return offsets;
        }

        long maxOffset = Math.max(0L, capacity - chunkSize);
        maxOffset = (maxOffset / SECTOR_SIZE_BYTES) * SECTOR_SIZE_BYTES;

        if (numChunks == 2) {
            offsets.add(0L);
            offsets.add(maxOffset);
            return offsets;
        }

        if (numChunks == 3) {
            offsets.add(0L);
            long middle = ((maxOffset / 2) / SECTOR_SIZE_BYTES) * SECTOR_SIZE_BYTES;
            offsets.add(middle);
            offsets.add(maxOffset);
            return offsets;
        }

        // numChunks >= 4:
        // Beginning (LBA 0), Middle of media, End of media, and uniform strides with pseudo-random offsets
        Random rng = new Random(capacity ^ 0x5A5A5A5AL);
        double stride = (double) maxOffset / (numChunks - 1);

        offsets.add(0L); // Beginning (LBA 0)

        int middleIndex = numChunks / 2;
        for (int i = 1; i < numChunks - 1; i++) {
            if (i == middleIndex) {
                long middle = ((maxOffset / 2) / SECTOR_SIZE_BYTES) * SECTOR_SIZE_BYTES;
                offsets.add(middle);
            } else {
                long windowStart = (long) (i * stride);
                long windowEnd = (long) ((i + 1) * stride);
                long windowRange = Math.max(1, windowEnd - windowStart);
                long jitter = (long) (rng.nextDouble() * windowRange);
                long offset = windowStart + jitter;
                offset = (offset / SECTOR_SIZE_BYTES) * SECTOR_SIZE_BYTES;
                offset = Math.max(0L, Math.min(maxOffset, offset));
                offsets.add(offset);
            }
        }

        offsets.add(maxOffset); // End of disk
        return offsets;
    }

    private static boolean isDiscrepancy(WipePatternType patternType, int expectedByte, int actualByte) {
        if (patternType == WipePatternType.PSEUDO_RANDOM) {
            return false;
        }
        return actualByte != expectedByte;
    }

    private static int resolveExpectedByte(WipePass pass) {
        if (pass == null) return 0x00;
        return switch (pass.getPatternType()) {
            case ONE_FILL -> 0xFF;
            case CUSTOM_BYTE -> pass.getCustomByteValue() & 0xFF;
            case COMPLEMENT -> 0x55;
            case ZERO_FILL, PSEUDO_RANDOM -> 0x00;
        };
    }

    /**
     * Calculates Shannon Entropy H(X) = - sum(p * log2(p)) over the byte frequency histogram.
     * Pure zero-fill results in 0.0000 bits/byte.
     */
    public static double calculateShannonEntropy(int[] byteFrequencies, long totalBytes) {
        if (totalBytes <= 0) return 0.0;
        double entropy = 0.0;
        for (int freq : byteFrequencies) {
            if (freq > 0) {
                double p = (double) freq / totalBytes;
                entropy -= p * (Math.log(p) / Math.log(2.0));
            }
        }
        return entropy;
    }

    /**
     * Calculates Shannon Entropy from a raw byte array.
     */
    public static double calculateShannonEntropy(byte[] data) {
        if (data == null || data.length == 0) return 0.0;
        int[] freqs = new int[256];
        for (byte b : data) {
            freqs[b & 0xFF]++;
        }
        return calculateShannonEntropy(freqs, data.length);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void log(Consumer<String> callback, String msg) {
        if (callback != null) {
            callback.accept(msg);
        }
        AppLogger.info(MODULE, msg);
    }
}

