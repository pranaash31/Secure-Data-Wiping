package com.sanitizer.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CryptoSigner RSA-2048 & SHA-256 Unit Tests")
class CryptoSignerTest {

    @Test
    @DisplayName("RSA Key files are initialized and accessible")
    void testRsaKeyFilesGenerated() {
        File privKeyFile = new File("sanitizer_private.key");
        File pubKeyFile = new File("sanitizer_public.key");

        // Force class loading & key init
        String sampleData = "SAMPLE_AUDIT_LOG_ENTRY_12345";
        String signature = CryptoSigner.signData(sampleData);

        assertThat(signature).isNotNull().isNotEqualTo("SIGNATURE_ERROR");
        assertThat(privKeyFile).exists();
        assertThat(pubKeyFile).exists();
        assertThat(privKeyFile.length()).isGreaterThan(0);
        assertThat(pubKeyFile.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Valid SHA256withRSA signature generation and verification succeeds")
    void testValidSignatureSigningAndVerification() {
        String data = "DRIVE: SanDisk Ultra 32GB | SERIAL: SD-FLAIR-99421 | STANDARD: DoD 5220.22-M | STATUS: SUCCESS";

        String signatureBase64 = CryptoSigner.signData(data);

        assertThat(signatureBase64).isNotNull().isNotBlank();

        boolean isVerified = CryptoSigner.verifySignature(data, signatureBase64);
        assertThat(isVerified).isTrue();
    }

    @Test
    @DisplayName("Tampered data payload fails signature verification")
    void testTamperedDataFailsVerification() {
        String originalData = "DRIVE: SanDisk Ultra 32GB | SERIAL: SD-FLAIR-99421 | STATUS: SUCCESS";
        String tamperedData = "DRIVE: SanDisk Ultra 32GB | SERIAL: SD-FLAIR-99421 | STATUS: FAILED";

        String signatureBase64 = CryptoSigner.signData(originalData);

        boolean isVerified = CryptoSigner.verifySignature(tamperedData, signatureBase64);
        assertThat(isVerified).isFalse();
    }

    @ParameterizedTest(name = "Invalid signature string: {0} should return false")
    @ValueSource(strings = {
            "INVALID_BASE_64_SIG!!!",
            "SIG_SHA256_RSA4096_0x99A418F",
            "dGVzdCBzaWduYXR1cmUgZHVtbXk="
    })
    void testInvalidSignatureStringsFailVerification(String invalidSignature) {
        String data = "DRIVE: SanDisk Ultra 32GB | SERIAL: SD-FLAIR-99421";
        boolean isVerified = CryptoSigner.verifySignature(data, invalidSignature);
        assertThat(isVerified).isFalse();
    }

    @ParameterizedTest(name = "Null or empty input handles safely")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testNullOrEmptyInputHandling(String blankInput) {
        assertThat(CryptoSigner.verifySignature("Data", blankInput)).isFalse();
        assertThat(CryptoSigner.verifySignature(blankInput, "ValidBase64==")).isFalse();
    }
}
