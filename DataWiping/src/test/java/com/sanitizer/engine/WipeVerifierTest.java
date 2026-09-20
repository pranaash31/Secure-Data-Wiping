package com.sanitizer.engine;

import com.sanitizer.db.AuditDb;
import com.sanitizer.pdf.CertificateGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
    @DisplayName("WipeVerifier correctly executes sampling across all verification modes")
    void testVerificationModes(WipeVerifier.VerificationMode mode) {
        List<String> logs = new ArrayList<>();
        List<Double> progressList = new ArrayList<>();

        long driveBytes = 8L * 1024 * 1024 * 1024; // 8 GB
        WipeVerifier.VerificationResult result = WipeVerifier.verifyDrive(
                "/dev/rdisk_mock_test",
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
