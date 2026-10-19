package com.sanitizer.security;

import com.sanitizer.util.AppLogger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * Enterprise Password & PIN Hasher implementing Salted PBKDF2 with HMAC-SHA256
 * (NIST SP 800-132 compliant).
 * Format: $pbkdf2-sha256$<iterations>$<saltHex>$<hashHex>
 */
public final class PasswordHasher {

    private static final String MODULE = "PasswordHasher";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 65536;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {}

    /**
     * Hashes a plain-text PIN or password using PBKDF2 with HMAC-SHA256 and a cryptographically secure salt.
     */
    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, DEFAULT_ITERATIONS, HASH_LENGTH_BITS);

        HexFormat hex = HexFormat.of();
        return "$pbkdf2-sha256$" + DEFAULT_ITERATIONS + "$" + hex.formatHex(salt) + "$" + hex.formatHex(hash);
    }

    /**
     * Verifies an entered password against a stored salted PBKDF2 hash using constant-time comparison.
     */
    public static boolean verifyPassword(String enteredPassword, String storedHash) {
        if (enteredPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        try {
            // Check for standard format: $pbkdf2-sha256$<iterations>$<saltHex>$<hashHex>
            String[] parts = storedHash.split("\\$");
            if (parts.length == 5 && "pbkdf2-sha256".equals(parts[1])) {
                int iterations = Integer.parseInt(parts[2]);
                HexFormat hex = HexFormat.of();
                byte[] salt = hex.parseHex(parts[3]);
                byte[] expectedHash = hex.parseHex(parts[4]);

                byte[] computedHash = pbkdf2(enteredPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(expectedHash, computedHash);
            }

            // Fallback for legacy unhashed plain PIN during progressive migration
            if (MessageDigest.isEqual(storedHash.getBytes(), enteredPassword.getBytes())) {
                AppLogger.warn(MODULE, "Verified legacy unhashed PIN; migration recommended.");
                return true;
            }
        } catch (Exception e) {
            AppLogger.error(MODULE, "Error during password verification", e);
        }
        return false;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 algorithm unavailable", e);
        }
    }
}
