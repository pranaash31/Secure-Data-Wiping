package com.sanitizer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KeyManagementService Unit Tests — Decoupled Key Management & Export I/O")
class KeyManagementServiceTest {

    private final KeyManagementService service = KeyManagementService.getInstance();

    @Test
    @DisplayName("Verify KeyManagementService instance and fingerprint formatting")
    void testFingerprintAndKeyId() {
        assertThat(service).isNotNull();

        String fingerprint = service.getPublicKeyFingerprint();
        assertThat(fingerprint).isNotBlank().contains(":");

        String keyId = service.generateKeyId();
        assertThat(keyId).startsWith("SE-KEY-");
    }

    @Test
    @DisplayName("Verify PEM serialization for public, private, and signed custom keys")
    void testFormatPem() {
        String pubPem = service.formatPem("PUBLIC KEY", "default");
        assertThat(pubPem)
                .startsWith("-----BEGIN PUBLIC KEY-----")
                .contains("-----END PUBLIC KEY-----");

        String privPem = service.formatPem("PRIVATE KEY", "default");
        assertThat(privPem)
                .startsWith("-----BEGIN PRIVATE KEY-----")
                .contains("-----END PRIVATE KEY-----");

        String customPem = service.formatPem("CERTIFICATE_REQ", "my_req");
        assertThat(customPem)
                .startsWith("-----BEGIN CERTIFICATE_REQ-----")
                .contains("-----END CERTIFICATE_REQ-----");
    }

    @Test
    @DisplayName("Verify exporting key to file writes valid content to disk")
    void testExportKeyToFile(@TempDir Path tempDir) throws IOException {
        File dest = tempDir.resolve("exported_public.pem").toFile();

        service.exportKeyToFile(dest, "PUBLIC KEY", "public.pem");

        assertThat(dest).exists();
        String content = Files.readString(dest.toPath());
        assertThat(content)
                .startsWith("-----BEGIN PUBLIC KEY-----")
                .contains("-----END PUBLIC KEY-----");

        assertThatThrownBy(() -> service.exportKeyToFile(null, "PUBLIC KEY", "test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Verify key rotation produces valid RSA keypair")
    void testRotateKeyPair() {
        KeyPair newPair = service.rotateKeyPair();
        assertThat(newPair).isNotNull();
        assertThat(newPair.getPublic()).isNotNull();
        assertThat(newPair.getPrivate()).isNotNull();
        assertThat(service.getPublicKeyFingerprint()).isNotBlank().contains(":");
    }
}
