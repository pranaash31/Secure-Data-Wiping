package com.sanitizer.pdf;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.pdf.CertificateGenerator.CertificateVerificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PDF Certificate Generation & Tamper Verification Unit Tests")
class CertificateGeneratorTest {

    @Test
    @DisplayName("Generate PDF Certificate and verify cryptographic authenticity from embedded QR code and PDF fields")
    void testCertificateGenerationAndVerification() {
        String model = "SanDisk Ultra Flair 32GB";
        String serial = "SD-FLAIR-99421";
        String capacity = "32 GB";
        String standard = "DoD 5220.22-M";
        String status = "SUCCESS";

        String auditPayload = model + "|" + serial + "|" + capacity + "|" + standard + "|" + status;
        String signature = CryptoSigner.signData(auditPayload);

        AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                999,
                "2026-09-16 17:00:00 UTC",
                model,
                serial,
                capacity,
                standard,
                status,
                signature
        );

        // Generate PDF Certificate
        String pdfFileName = CertificateGenerator.generateCertificate(record);
        assertThat(pdfFileName).isNotNull();

        File pdfFile = new File(pdfFileName);
        assertThat(pdfFile).exists().isFile();

        try {
            // Verify PDF Certificate
            CertificateVerificationResult result = CertificateGenerator.verifyPdfCertificate(pdfFile);

            assertThat(result.isValid())
                    .withFailMessage("Verification failed: " + result.message()
                            + "\nExtracted model: '" + result.driveModel() + "'"
                            + "\nExtracted serial: '" + result.serialNumber() + "'"
                            + "\nExtracted capacity: '" + result.capacity() + "'"
                            + "\nExtracted standard: '" + result.wipeStandard() + "'"
                            + "\nExtracted status: '" + result.status() + "'"
                            + "\nExtracted sig: '" + result.digitalSignature() + "'")
                    .isTrue();

            assertThat(result.driveModel()).isEqualTo(model);
            assertThat(result.serialNumber()).isEqualTo(serial);
            assertThat(result.wipeStandard()).isEqualTo(standard);
            assertThat(result.status()).isEqualTo(status);
            assertThat(result.digitalSignature()).isEqualTo(signature);
            assertThat(result.message()).contains("Tamper-Free");

        } finally {
            if (pdfFile.exists()) {
                pdfFile.delete();
            }
        }
    }

    @Test
    @DisplayName("Verify non-existent PDF file returns invalid result")
    void testNonExistentPdfFileVerification() {
        File dummyFile = new File("non_existent_certificate.pdf");
        CertificateVerificationResult result = CertificateGenerator.verifyPdfCertificate(dummyFile);

        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).contains("File does not exist");
    }
}
