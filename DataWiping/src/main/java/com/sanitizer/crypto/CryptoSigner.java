package com.sanitizer.crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CryptoSigner {

    private static final String PRIVATE_KEY_FILE = "sanitizer_private.key";
    private static final String PUBLIC_KEY_FILE = "sanitizer_public.key";

    private static KeyPair rsaKeyPair;

    static {
        initKeys();
    }

    private static synchronized void initKeys() {
        try {
            File privFile = new File(PRIVATE_KEY_FILE);
            File pubFile = new File(PUBLIC_KEY_FILE);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            if (privFile.exists() && pubFile.exists()) {
                byte[] privBytes = Files.readAllBytes(privFile.toPath());
                byte[] pubBytes = Files.readAllBytes(pubFile.toPath());

                PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));

                rsaKeyPair = new KeyPair(publicKey, privateKey);
            } else {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                rsaKeyPair = keyGen.generateKeyPair();

                Files.write(privFile.toPath(), rsaKeyPair.getPrivate().getEncoded());
                Files.write(pubFile.toPath(), rsaKeyPair.getPublic().getEncoded());
            }
        } catch (Exception e) {
            System.err.println("CryptoSigner key initialization warning: " + e.getMessage());
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);
                rsaKeyPair = keyGen.generateKeyPair();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to initialize RSA key pair", ex);
            }
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
        if (data == null || signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
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