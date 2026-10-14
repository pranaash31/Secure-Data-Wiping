package com.sanitizer.service;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.server.WebVerificationServer;
import com.sanitizer.util.AppLogger;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Application service for certificate verification, QR / Web Portal token verification,
 * and external desktop browser interactions, decoupled from the UI.
 */
public class CertificateVerificationService {

    private static final String MODULE = "CertificateVerificationService";

    private static class InstanceHolder {
        private static final CertificateVerificationService INSTANCE = new CertificateVerificationService();
    }

    public static CertificateVerificationService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public record TokenVerificationResult(
            boolean isValid,
            boolean isFormatValid,
            String certificateId,
            String driveModel,
            String serialNumber,
            String capacity,
            String wipeStandard,
            String status,
            String digitalSignature,
            EsgCalculator.EsgMetrics esgMetrics,
            String verificationUrl,
            String message
    ) {}

    /**
     * Verifies a digital sanitization certificate PDF file.
     */
    public CertificateGenerator.CertificateVerificationResult verifyPdfCertificate(File file) {
        if (file == null || !file.exists()) {
            return new CertificateGenerator.CertificateVerificationResult(
                    false, null, null, null, null, null, null, null, "Certificate file not found or inaccessible"
            );
        }
        AppLogger.info(MODULE, "Verifying PDF certificate: " + file.getAbsolutePath());
        return CertificateGenerator.verifyPdfCertificate(file);
    }

    /**
     * Extracts parameters and verifies digital signature authenticity from a Web Verification token or URL.
     */
    public TokenVerificationResult verifyTokenUrl(String input) {
        if (input == null || input.isBlank()) {
            return new TokenVerificationResult(
                    false, false, null, null, null, null, null, null, null, null, null,
                    "Empty token input provided"
            );
        }

        String model = extractParam(input, "model");
        String serial = extractParam(input, "serial");
        String cap = extractParam(input, "cap");
        String std = extractParam(input, "std");
        String status = extractParam(input, "status");
        String sig = extractParam(input, "sig");
        String certId = extractParam(input, "certId");

        if (sig != null && !sig.isBlank() && model != null && !model.isBlank()) {
            String payload = model + "|" + serial + "|" + cap + "|" + std + "|" + status;
            boolean isValid = CryptoSigner.verifySignature(payload, sig);
            EsgCalculator.EsgMetrics esg = EsgCalculator.calculate(cap);

            String resolvedCertId = (certId != null && !certId.isBlank()) ? certId : "SAN-CERT-TOKEN";
            String msg = isValid
                    ? "Cryptographic verification passed via Web Portal token."
                    : "Token signature mismatch or payload modified.";

            return new TokenVerificationResult(
                    isValid, true, resolvedCertId, model, serial, cap, std, status, sig, esg, input, msg
            );
        } else {
            return new TokenVerificationResult(
                    false, false, null, null, null, null, null, null, null, null, null,
                    "Could not parse verification parameters from token URL."
            );
        }
    }

    /**
     * Locates the latest generated PDF certificate on disk if available in the audit ledger.
     */
    public Optional<File> findLatestCertificateFile() {
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        if (!records.isEmpty()) {
            AuditDb.AuditRecord latest = records.get(0);
            File certFile = new File("Sanitization_Certificate_" + latest.id() + ".pdf");
            if (certFile.exists()) {
                return Optional.of(certFile);
            }
        }
        return Optional.empty();
    }

    /**
     * Generates a Web Portal verification URL for an audit record.
     */
    public String generateVerificationUrl(CertificateGenerator.CertificateVerificationResult result) {
        if (result == null) return null;
        AuditDb.AuditRecord mockRec = new AuditDb.AuditRecord(
                1,
                "2026-09-17 12:00:00",
                result.driveModel() != null ? result.driveModel() : "USB Storage",
                result.serialNumber() != null ? result.serialNumber() : "N/A",
                result.capacity() != null ? result.capacity() : "N/A",
                result.wipeStandard() != null ? result.wipeStandard() : "DoD 5220.22-M",
                result.status() != null ? result.status() : "SUCCESS",
                result.digitalSignature()
        );
        return WebVerificationServer.generateVerificationUrl(mockRec);
    }

    /**
     * Launches the system default web browser to view the verification portal.
     */
    public boolean openInBrowser(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                AppLogger.info(MODULE, "Opened verification URL in browser: " + url);
                return true;
            } else {
                AppLogger.warn(MODULE, "Desktop browse action is not supported on this platform.");
                return false;
            }
        } catch (Exception e) {
            AppLogger.error(MODULE, "Failed to launch browser for URL: " + url, e);
            return false;
        }
    }

    /**
     * Robust URL parameter extraction with UTF-8 decoding.
     */
    public String extractParam(String query, String key) {
        if (query == null || key == null || !query.contains(key + "=")) {
            return "";
        }
        int start = query.indexOf(key + "=") + key.length() + 1;
        int end = query.indexOf("&", start);
        if (end == -1) end = query.length();
        try {
            return URLDecoder.decode(query.substring(start, end), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return query.substring(start, end);
        }
    }
}
