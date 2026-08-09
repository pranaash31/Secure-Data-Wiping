package com.sanitizer.crypto;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class CryptoSigner {

    private static KeyPair rsaKeyPair;

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            rsaKeyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize RSA key pair generator", e);
        }
    }

    /**
     * Digitally signs payload metadata using SHA256withRSA.
     */
    public static String signData(String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(rsaKeyPair.getPrivate());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] digitalSignature = signature.sign();
            return Base64.getEncoder().encodeToString(digitalSignature);
        } catch (Exception e) {
            System.err.println("Signing Error: " + e.getMessage());
            return "SIGNATURE_ERROR";
        }
    }

    /**
     * Verifies digital signature authenticity using RSA Public Key.
     */
    public static boolean verifySignature(String data, String signatureBase64) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(rsaKeyPair.getPublic());
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }
}