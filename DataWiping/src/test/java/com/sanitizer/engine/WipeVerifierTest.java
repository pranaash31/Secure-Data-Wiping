package com.sanitizer.engine;

import com.sanitizer.db.AuditDb;
import com.sanitizer.pdf.CertificateGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Automated Post-Wipe Sampling & Zero-Residual Entropy Verification Tests")
class WipeVerifierTest {

    @Test
    @DisplayName("Zero-fill buffer yields exact 0.0000 bits/byte Shannon entropy")
    void testZeroFillEntropy() {
        byte[] zeroBuffer = new byte[64 * 1024];
        Arrays.fill(zeroBuffer, (byte) 0x00);

        double entropy = WipeVerifier.calculateShannonEntropy(zeroBuffer);
        assertThat(entropy).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Random byte distribution yields high Shannon entropy (> 7.5 bits/byte)")
    void testRandomDataEntropy() {
        byte[] randomBuffer = new byte[64 * 1024];
        new Random(42).nextBytes(randomBuffer);

        double entropy = WipeVerifier.calculateShannonEntropy(randomBuffer);
        assertThat(entropy).isGreaterThan(7.5);
    }

    @Test
    @DisplayName("Empty or null data safely returns 0.0 entropy")
    void testEmptyDataEntropy() {
        assertThat(WipeVerifier.calculateShannonEntropy(new byte[0])).isEqualTo(0.0);
        assertThat(WipeVerifier.calculateShannonEntropy(null)).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "Verification Mode: {0}")
    @EnumSource(WipeVerifier.VerificationMode.class)
    @DisplayName("WipeVerifier correctly executes sampling across all verification modes on clean media")
    void testVerificationModes(WipeVerifier.VerificationMode mode) throws Exception {
        File tempZeroFile = File.createTempFile("test_verify_clean_", ".bin");
        tempZeroFile.deleteOnExit();

        // Populate with 2 MB of zeroes (4096 sectors * 512 bytes)
        byte[] zeroBuffer = new byte[64 * 1024];
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempZeroFile)) {
            for (int i = 0; i < 32; i++) {
                fos.write(zeroBuffer);
            }
        }

        List<String> logs = new ArrayList<>();
        List<Double> progressList = new ArrayList<>();

        long driveBytes = 8L * 1024 * 1024 * 1024; // 8 GB
        WipeVerifier.VerificationResult result = WipeVerifier.verifyDrive(
                tempZeroFile.getAbsolutePath(),
                driveBytes,
                mode,
                true, // test mode caps to 1GB
                progressList::add,
                logs::add
        );

        assertThat(result).isNotNull();
        assertThat(result.isClean()).isTrue();
        assertThat(result.entropyScore()).isEqualTo(0.0);
        assertThat(result.statusSummary()).contains("PASS");
        assertThat(result.sha256Proof()).startsWith("SHA256:");
        assertThat(result.sha256Proof()).hasSize(71); // "SHA256:" (7) + 64 hex chars
        assertThat(result.totalSectorsVerified()).isGreaterThanOrEqualTo(2048);
        assertThat(result.verificationMethod()).contains("NIST SP 800-88");

        assertThat(logs).isNotEmpty();
        assertThat(logs).anyMatch(l -> l.contains("[VERIFIER]"));
        assertThat(progressList).isNotEmpty();
        assertThat(progressList.get(progressList.size() - 1)).isEqualTo(1.0);

        tempZeroFile.delete();
    }

    @Test
    @DisplayName("Verification fails immediately when target device or file does not exist (Emits IO_PERMISSION_ERROR)")
    void testVerificationFailsOnNonExistentDevice() {
        List<String> logs = new ArrayList<>();
        WipeVerifier.VerificationResult result = WipeVerifier.verifyDrive(
                "/dev/nonexistent_mock_device_drive_xyz",
                8L * 1024 * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                null,
                logs::add
        );

        assertThat(result).isNotNull();
        assertThat(result.isClean()).isFalse();
        assertThat(result.statusSummary()).contains("FAIL");
        assertThat(result.statusSummary()).contains(WipeVerifier.IO_PERMISSION_ERROR);
        assertThat(result.statusSummary()).contains("not found");
        assertThat(result.totalSectorsVerified()).isEqualTo(0);
        assertThat(result.sha256Proof()).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Verification detects non-zero residual bytes and issues FAIL attestation")
    void testVerificationFailsWhenResidualDataDetected() throws Exception {
        File dirtyFile = File.createTempFile("test_verify_dirty_", ".bin");
        dirtyFile.deleteOnExit();

        // 2 MB file with zeroes, except some sectors with dirty bytes
        byte[] buffer = new byte[64 * 1024];
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dirtyFile)) {
            for (int i = 0; i < 32; i++) {
                if (i == 5 || i == 16) {
                    Arrays.fill(buffer, (byte) 0xEF);
                } else {
                    Arrays.fill(buffer, (byte) 0x00);
                }
                fos.write(buffer);
            }
        }

        List<String> logs = new ArrayList<>();
        WipeVerifier.VerificationResult result = WipeVerifier.verifyDrive(
                dirtyFile.getAbsolutePath(),
                2L * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                null,
                logs::add
        );

        assertThat(result).isNotNull();
        assertThat(result.isClean()).isFalse();
        assertThat(result.nonZeroBytesFound()).isGreaterThan(0);
        assertThat(result.statusSummary()).contains("FAIL — Residual Bytes Detected");

        dirtyFile.delete();
    }

    @Test
    @DisplayName("Sampling offsets span Header (First 1%), Middle (50% capacity), and Tail (Last 1%)")
    void testStridedSamplingDistribution() {
        long capacity = 100L * 1024 * 1024; // 100 MB
        long chunkSize = 64 * 1024; // 64 KB
        int numChunks = 50;

        List<Long> offsets = WipeVerifier.calculateSampleOffsets(capacity, chunkSize, numChunks);

        assertThat(offsets).isNotEmpty();
        assertThat(offsets.get(0)).isEqualTo(0L); // Header: First LBA 0

        long maxExpectedOffset = (capacity - chunkSize) / WipeVerifier.SECTOR_SIZE_BYTES * WipeVerifier.SECTOR_SIZE_BYTES;
        assertThat(offsets.get(offsets.size() - 1)).isEqualTo(maxExpectedOffset); // Tail: Final LBA block

        // Verify Header Zone (first 1% = 1 MB)
        long headerBound = (long) (capacity * 0.01);
        boolean hasHeaderSample = offsets.stream().anyMatch(off -> off <= headerBound);
        assertThat(hasHeaderSample).isTrue();

        // Verify Middle Strides Zone (50% capacity = 25 MB to 75 MB)
        long midStart = (long) (capacity * 0.25);
        long midEnd = (long) (capacity * 0.75);
        boolean hasMiddleSample = offsets.stream().anyMatch(off -> off >= midStart && off <= midEnd);
        assertThat(hasMiddleSample).isTrue();

        // Verify Tail Zone (last 1% = 99 MB to 100 MB)
        long tailBound = (long) (capacity * 0.99);
        boolean hasTailSample = offsets.stream().anyMatch(off -> off >= tailBound);
        assertThat(hasTailSample).isTrue();

        // Every offset must be 512-byte sector-aligned
        for (long offset : offsets) {
            assertThat(offset % WipeVerifier.SECTOR_SIZE_BYTES).isEqualTo(0L);
            assertThat(offset).isBetween(0L, maxExpectedOffset);
        }
    }

    @Test
    @DisplayName("Verification compares read blocks against expected pattern (0xFF vs 0x00)")
    void testPolicyAwareVerification() throws Exception {
        // Test ONE_FILL policy against 0xFF filled file
        File oneFillFile = File.createTempFile("test_verify_onefill_", ".bin");
        oneFillFile.deleteOnExit();
        byte[] ones = new byte[64 * 1024];
        Arrays.fill(ones, (byte) 0xFF);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(oneFillFile)) {
            for (int i = 0; i < 32; i++) fos.write(ones);
        }

        com.sanitizer.policy.WipePolicy onePolicy = new com.sanitizer.policy.WipePolicy(
                "one-fill-policy", "One Fill Policy", "Description", "Org", "CODE",
                List.of(new com.sanitizer.policy.WipePass(1, com.sanitizer.policy.WipePatternType.ONE_FILL, 0xFF, "All-Ones")),
                true, WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT, false, "v1", "TAG"
        );

        WipeVerifier.VerificationResult oneResult = WipeVerifier.verifyDrive(
                oneFillFile.getAbsolutePath(), 2L * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                onePolicy, true, null, null
        );
        assertThat(oneResult.isClean()).isTrue();
        assertThat(oneResult.statusSummary()).contains("0xFF Inversion Residual Confirmed");

        // Negative check: Verifying 0xFF file against default ZERO_FILL policy must FAIL
        WipeVerifier.VerificationResult zeroExpectResult = WipeVerifier.verifyDrive(
                oneFillFile.getAbsolutePath(), 2L * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true, null, null
        );
        assertThat(zeroExpectResult.isClean()).isFalse();
        assertThat(zeroExpectResult.statusSummary()).contains("FAIL — Residual Bytes Detected");
        assertThat(zeroExpectResult.statusSummary()).contains("expected 0x00");

        oneFillFile.delete();

        // Negative check: Verifying 0x00 file against ONE_FILL policy must FAIL
        File zeroFile = File.createTempFile("test_verify_zerofill_", ".bin");
        zeroFile.deleteOnExit();
        byte[] zeroes = new byte[64 * 1024];
        Arrays.fill(zeroes, (byte) 0x00);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zeroFile)) {
            for (int i = 0; i < 32; i++) fos.write(zeroes);
        }

        WipeVerifier.VerificationResult oneExpectResult = WipeVerifier.verifyDrive(
                zeroFile.getAbsolutePath(), 2L * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                onePolicy, true, null, null
        );
        assertThat(oneExpectResult.isClean()).isFalse();
        assertThat(oneExpectResult.statusSummary()).contains("FAIL — Residual Bytes Detected");
        assertThat(oneExpectResult.statusSummary()).contains("expected 0xFF");

        zeroFile.delete();

        // Test PSEUDO_RANDOM policy against high-entropy random file
        File randomFile = File.createTempFile("test_verify_random_", ".bin");
        randomFile.deleteOnExit();
        byte[] randomBytes = new byte[64 * 1024];
        Random rng = new Random(12345);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(randomFile)) {
            for (int i = 0; i < 32; i++) {
                rng.nextBytes(randomBytes);
                fos.write(randomBytes);
            }
        }

        com.sanitizer.policy.WipePolicy randomPolicy = new com.sanitizer.policy.WipePolicy(
                "random-policy", "Random Policy", "Description", "Org", "CODE",
                List.of(new com.sanitizer.policy.WipePass(1, com.sanitizer.policy.WipePatternType.PSEUDO_RANDOM, -1, "Random Pass")),
                true, WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT, false, "v1", "TAG"
        );

        WipeVerifier.VerificationResult randomResult = WipeVerifier.verifyDrive(
                randomFile.getAbsolutePath(), 2L * 1024 * 1024,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                randomPolicy, true, null, null
        );
        assertThat(randomResult.isClean()).isTrue();
        assertThat(randomResult.entropyScore()).isGreaterThan(7.5);
        assertThat(randomResult.statusSummary()).contains("Cryptographic Random Residual Confirmed");

        randomFile.delete();
    }

    @Test
    @DisplayName("AuditDb properly persists and retrieves verification attestation fields")
    void testAuditDbVerificationPersistence() {
        String testDb = "jdbc:sqlite:target/test_verifier_audit.db";
        AuditDb.setDbUrlForTesting(testDb);

        boolean saved = AuditDb.saveRecord(
                "Kingston DataTraveler 3.0",
                "VERIFY-TEST-9988",
                "32 GB",
                "NIST SP 800-88",
                "SUCCESS",
                "MOCK_SIGNATURE_PROOF",
                100,
                100,
                0,
                0,
                "Integrity Verified: 0 Defects",
                38,
                0,
                0,
                "OPTIMAL",
                "PASS — Zero Residual Data Confirmed (0.000% Entropy)",
                40960,
                0.0000,
                "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );

        assertThat(saved).isTrue();

        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        assertThat(records).isNotEmpty();

        AuditDb.AuditRecord latest = records.get(0);
        assertThat(latest.serialNumber()).isEqualTo("VERIFY-TEST-9988");
        assertThat(latest.verificationStatus()).isEqualTo("PASS — Zero Residual Data Confirmed (0.000% Entropy)");
        assertThat(latest.verifiedSectorsCount()).isEqualTo(40960);
        assertThat(latest.entropyScore()).isEqualTo(0.0000);
        assertThat(latest.verificationHash()).isEqualTo("SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        // Verify PDF generation with Section D verification block
        String pdfPath = CertificateGenerator.generateCertificate(latest);
        assertThat(pdfPath).isNotNull();
        File pdfFile = new File(pdfPath);
        assertThat(pdfFile.exists()).isTrue();
        assertThat(pdfFile.length()).isGreaterThan(1000);
    }
}
