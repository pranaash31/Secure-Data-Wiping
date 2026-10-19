package com.sanitizer.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PasswordHasher Salted PBKDF2 Unit Tests (NIST SP 800-132)")
class PasswordHasherTest {

    @Test
    @DisplayName("Hash generation produces salted PBKDF2 standard string format")
    void testHashFormat() {
        String hash = PasswordHasher.hashPassword("SuperSecretPin123");
        assertThat(hash).isNotNull().startsWith("$pbkdf2-sha256$65536$");
        String[] parts = hash.split("\\$");
        assertThat(parts.length).isEqualTo(5);
        assertThat(parts[1]).isEqualTo("pbkdf2-sha256");
        assertThat(parts[2]).isEqualTo("65536");
        assertThat(parts[3].length()).isEqualTo(32); // 16 bytes salt in hex
        assertThat(parts[4].length()).isEqualTo(64); // 32 bytes (256-bit) hash in hex
    }

    @Test
    @DisplayName("Unique cryptographic salts are generated for identical PIN inputs")
    void testUniqueSalts() {
        String pin = "1234";
        String hash1 = PasswordHasher.hashPassword(pin);
        String hash2 = PasswordHasher.hashPassword(pin);

        assertThat(hash1).isNotEqualTo(hash2);
        assertTrue(PasswordHasher.verifyPassword(pin, hash1));
        assertTrue(PasswordHasher.verifyPassword(pin, hash2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234", "admin", "987654", "Compl!anceP@ss2026"})
    @DisplayName("Valid password verification succeeds across various inputs")
    void testValidPasswordVerification(String password) {
        String hash = PasswordHasher.hashPassword(password);
        assertTrue(PasswordHasher.verifyPassword(password, hash));
        assertFalse(PasswordHasher.verifyPassword(password + "_wrong", hash));
    }

    @Test
    @DisplayName("Invalid password and null/empty checks fail safely")
    void testInvalidAndEdgeCases() {
        String hash = PasswordHasher.hashPassword("correctPin");

        assertFalse(PasswordHasher.verifyPassword("wrongPin", hash));
        assertFalse(PasswordHasher.verifyPassword(null, hash));
        assertFalse(PasswordHasher.verifyPassword("", hash));
        assertFalse(PasswordHasher.verifyPassword("correctPin", null));
        assertFalse(PasswordHasher.verifyPassword("correctPin", ""));
        assertFalse(PasswordHasher.verifyPassword("correctPin", "invalid-hash-string"));
    }
}
