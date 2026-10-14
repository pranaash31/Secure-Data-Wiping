package com.sanitizer.service;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

/**
 * Application service for cryptographic key management, key rotation, PEM formatting,
 * and key export file I/O operations, decoupled from the UI.
 */
public class KeyManagementService {

    private static final String MODULE = "KeyManagementService";

    private static class InstanceHolder {
        private static final KeyManagementService INSTANCE = new KeyManagementService();
    }

    public static KeyManagementService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public String getPublicKeyFingerprint() {
        return CryptoSigner.getPublicKeyFingerprint();
    }

    public String getPublicKeyPem() {
        return CryptoSigner.getPublicKeyPem();
    }

    public String getPrivateKeyPem() {
        return CryptoSigner.getPrivateKeyPem();
    }

    public String generateKeyId() {
        return "SE-KEY-" + (System.currentTimeMillis() / 1000);
    }

    /**
     * Rotates and generates a brand-new RSA-2048 signing key pair, persisting to disk.
     */
    public KeyPair rotateKeyPair() {
        AppLogger.info(MODULE, "Rotating RSA-2048 signing keypair");
        return CryptoSigner.rotateKeyPair();
    }

    /**
     * Serializes the requested key type to standard PEM format.
     */
    public String formatPem(String keyType, String defaultName) {
        if ("PUBLIC KEY".equalsIgnoreCase(keyType)) {
            return CryptoSigner.getPublicKeyPem();
        } else if ("PRIVATE KEY".equalsIgnoreCase(keyType)) {
            return CryptoSigner.getPrivateKeyPem();
        } else {
            return "-----BEGIN " + keyType + "-----\n" +
                    CryptoSigner.signData("KEY_EXPORT_" + defaultName) + "\n" +
                    "-----END " + keyType + "-----\n";
        }
    }

    /**
     * Writes the formatted PEM key to the specified file.
     */
    public void exportKeyToFile(File destination, String keyType, String defaultName) throws IOException {
        if (destination == null) {
            throw new IllegalArgumentException("Destination file cannot be null");
        }
        String pemContent = formatPem(keyType, defaultName);
        try (FileWriter writer = new FileWriter(destination)) {
            writer.write(pemContent);
        }
        AppLogger.info(MODULE, "Exported " + keyType + " to: " + destination.getAbsolutePath());
    }
}
