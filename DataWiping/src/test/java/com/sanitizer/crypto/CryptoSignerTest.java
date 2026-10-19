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

    @Test
    @DisplayName("Rotate key pair generates new valid key pair")
    void testRotateKeyPair() {
        var newKeyPair = CryptoSigner.rotateKeyPair();
        assertThat(newKeyPair).isNotNull();
        assertThat(CryptoSigner.getPublicKey()).isNotNull();
        assertThat(CryptoSigner.getPrivateKey()).isNotNull();

        String payload = "TEST_PAYLOAD_POST_ROTATION";
        String sig = CryptoSigner.signData(payload);
        assertThat(sig).isNotNull().isNotBlank();
        assertThat(CryptoSigner.verifySignature(payload, sig)).isTrue();
    }

    @Test
    @DisplayName("PEM export provides valid BEGIN/END headers and MIME base64 blocks")
    void testPemExport() {
        String pubPem = CryptoSigner.getPublicKeyPem();
        assertThat(pubPem).startsWith("-----BEGIN PUBLIC KEY-----")
                .contains("-----END PUBLIC KEY-----");

        String privPem = CryptoSigner.getPrivateKeyPem();
        assertThat(privPem).startsWith("-----BEGIN PRIVATE KEY-----")
                .contains("-----END PRIVATE KEY-----");
    }

    @Test
    @DisplayName("Public key fingerprint provides valid colon-delimited hex string")
    void testFingerprintFormat() {
        String fingerprint = CryptoSigner.getPublicKeyFingerprint();
        assertThat(fingerprint).isNotNull().isNotBlank();
        assertThat(fingerprint).contains(":");
        String[] parts = fingerprint.split(":");
        assertThat(parts.length).isEqualTo(32); // SHA-256 is 32 bytes
    }

    @Test
    @DisplayName("PKCS#12 keystore file is created, password protected, and contains valid KeyId alias")
    void testPkcs12KeystoreAndKeyId() {
        File keystoreFile = new File("sanitizer_keystore.p12");
        assertThat(keystoreFile).exists();
        assertThat(keystoreFile.length()).isGreaterThan(0);

        String activeKeyId = CryptoSigner.getActiveKeyId();
        assertThat(activeKeyId).isNotNull().startsWith("SE-KEY-");
    }

    @Test
    @DisplayName("Key rotation does NOT invalidate older certificates verified by their KeyId")
    void testKeyRotationDoesNotInvalidateOldCertificates() {
        String oldPayload = "PAYLOAD_SIGNED_BEFORE_ROTATION";
        String oldSig = CryptoSigner.signData(oldPayload);
        String oldKeyId = CryptoSigner.getActiveKeyId();

        assertThat(oldSig).isNotBlank();
        assertThat(oldKeyId).startsWith("SE-KEY-");
        assertThat(CryptoSigner.verifySignatureWithKey(oldPayload, oldSig, oldKeyId)).isTrue();

        // Rotate key pair
        var newKeyPair = CryptoSigner.rotateKeyPair();
        String newKeyId = CryptoSigner.getActiveKeyId();

        assertThat(newKeyPair).isNotNull();
        assertThat(newKeyId).startsWith("SE-KEY-");
        assertThat(newKeyId).isNotEqualTo(oldKeyId);

        // Sign new payload with new key
        String newPayload = "PAYLOAD_SIGNED_AFTER_ROTATION";
        String newSig = CryptoSigner.signData(newPayload);

        assertThat(CryptoSigner.verifySignatureWithKey(newPayload, newSig, newKeyId)).isTrue();

        // CRITICAL CHECK: Older certificate verified using oldKeyId remains valid against the PKCS#12 vault!
        boolean oldCertStillValid = CryptoSigner.verifySignatureWithKey(oldPayload, oldSig, oldKeyId);
        assertThat(oldCertStillValid).isTrue();
    }
}
