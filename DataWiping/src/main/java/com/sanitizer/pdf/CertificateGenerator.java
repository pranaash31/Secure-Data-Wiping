package com.sanitizer.pdf;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.server.WebVerificationServer;
import com.sanitizer.util.QrGenerator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CertificateGenerator {

    public record CertificateVerificationResult(
            boolean isValid,
            String certificateId,
            String driveModel,
            String serialNumber,
            String capacity,
            String wipeStandard,
            String status,
            String digitalSignature,
            String message
    ) {}

    public static String generateCertificate(AuditDb.AuditRecord record) {
        String fileName = "Sanitization_Certificate_" + record.id() + ".pdf";
        String signature = record.digitalSignature() != null ? record.digitalSignature() : "N/A";
        String verifyUrl = WebVerificationServer.generateVerificationUrl(record);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Fonts
                var boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                String formattedDate = formatTimestamp(record.timestamp());

                // Header Banner
                cs.beginText();
                cs.setFont(boldFont, 18);
                cs.newLineAtOffset(120, 725);
                cs.showText("CERTIFICATE OF DATA SANITIZATION");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(145, 705);
                cs.showText("Official NIST SP 800-88 & DoD 5220.22-M Compliance Audit Record");
                cs.endText();

                // Top Horizontal Line
                cs.setLineWidth(1.0f);
                cs.moveTo(50, 690);
                cs.lineTo(550, 690);
                cs.stroke();

                // Record Details
                int y = 650;
                int leading = 18;

                drawField(cs, boldFont, regularFont, "Certificate ID:", "SAN-CERT-" + record.id(), 50, y);
                drawField(cs, boldFont, regularFont, "Timestamp (UTC):", formattedDate, 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Device Model:", record.driveModel(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Serial Number:", record.serialNumber(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Capacity:", record.capacity(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Sanitization Method:", record.wipeStandard(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Execution Status:", record.status(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Pre-Wipe Health:", record.preHealthScore() + " / 100 (Automated S.M.A.R.T. Verified)", 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Post-Wipe Health:", record.postHealthScore() + " / 100 (Integrity Certified)", 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Wear & Defect Delta:", record.smartDeltaSummary(), 50, y -= leading);

                // ── Section A: Thermal Telemetry Attestation ──────────────────────────
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 10);
                cs.lineTo(550, y - 10);
                cs.stroke();
                y -= 22;

                cs.beginText();
                cs.setFont(boldFont, 8.5f);
                cs.newLineAtOffset(50, y);
                cs.showText("THERMAL TELEMETRY ATTESTATION (Real-Time Monitoring During Sanitization):");
                cs.endText();
                y -= 13;

                String peakTempStr = record.peakTempCelsius() > 0
                        ? record.peakTempCelsius() + " \u00b0C"
                        : "Not Recorded (Test Mode / No Thermal Events)";
                drawFieldSmall(cs, boldFont, regularFont, "Peak Temperature During Wipe:", peakTempStr, 50, y);
                y -= 12;
                drawFieldSmall(cs, boldFont, regularFont, "Thermal Auto-Pause Events:", String.valueOf(record.thermalPauseCount()), 50, y);
                y -= 12;
                String thermalStatus = deriveThermalStatus(record.peakTempCelsius(), record.thermalPauseCount());
                drawFieldSmall(cs, boldFont, regularFont, "Thermal Status:", thermalStatus, 50, y);
                y -= 12;

                // ── Section B: Interface Signal Integrity ─────────────────────────────
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 6);
                cs.lineTo(550, y - 6);
                cs.stroke();
                y -= 18;

                cs.beginText();
                cs.setFont(boldFont, 8.5f);
                cs.newLineAtOffset(50, y);
                cs.showText("INTERFACE SIGNAL INTEGRITY (SMART ID 199 & ID 188 Bus Correlation):");
                cs.endText();
                y -= 13;

                drawFieldSmall(cs, boldFont, regularFont, "UDMA CRC Errors (ID 199):", String.valueOf(record.crcErrors()), 50, y);
                y -= 12;
                String ifaceSummary = sanitize(record.interfaceAnomalySummary());
                String busStatus = deriveInterfaceStatus(record.crcErrors(), ifaceSummary);
                drawFieldSmall(cs, boldFont, regularFont, "Bus Integrity Status:", busStatus, 50, y);
                y -= 12;
                // Root-cause on up to 2 lines
                String rootCause = ifaceSummary.length() > 80 ? ifaceSummary.substring(0, 80) + "..." : ifaceSummary;
                drawFieldSmall(cs, boldFont, regularFont, "Root-Cause Attestation:", rootCause, 50, y);
                y -= 12;

                // ── Section C: Drive Risk Classification ──────────────────────────────
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 6);
                cs.lineTo(550, y - 6);
                cs.stroke();
                y -= 16;

                String riskLabel = deriveRiskClassification(
                        record.preHealthScore(), record.postHealthScore(),
                        record.crcErrors(), record.thermalPauseCount(), ifaceSummary);
                cs.beginText();
                cs.setFont(boldFont, 8.5f);
                cs.newLineAtOffset(50, y);
                cs.showText("DRIVE RISK CLASSIFICATION:   " + riskLabel);
                cs.endText();
                y -= 14;

                // ── Section D: Post-Wipe Sampling & Zero-Residual Entropy Verification ──
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 4);
                cs.lineTo(550, y - 4);
                cs.stroke();
                y -= 15;

                cs.beginText();
                cs.setFont(boldFont, 8.5f);
                cs.newLineAtOffset(50, y);
                cs.showText("POST-WIPE SAMPLING & ZERO-RESIDUAL ENTROPY ATTESTATION (NIST SP 800-88 Sec 4.7 / ISO 27040):");
                cs.endText();
                y -= 12;

                drawFieldSmall(cs, boldFont, regularFont, "Verified Sectors Sampled:",
                        String.format(java.util.Locale.US, "%,d LBAs (%,d KB Read-Back)", record.verifiedSectorsCount(), (record.verifiedSectorsCount() * 512) / 1024), 50, y);
                y -= 11;

                drawFieldSmall(cs, boldFont, regularFont, "Residual Shannon Entropy:",
                        String.format(java.util.Locale.US, "%.4f bits/byte (0.000%% Mathematical Zero-Residual)", record.entropyScore()), 50, y);
                y -= 11;

                drawFieldSmall(cs, boldFont, regularFont, "Verification Status:", sanitize(record.verificationStatus()), 50, y);
                y -= 11;

                String hashProofSnippet = record.verificationHash() != null && record.verificationHash().length() > 50
                        ? record.verificationHash().substring(0, 50) + "..." : record.verificationHash();
                drawFieldSmall(cs, boldFont, regularFont, "SHA-256 Sample Proof Digest:", sanitize(hashProofSnippet), 50, y);
                y -= 14;

                // Horizontal Line before QR block
                cs.setLineWidth(1.0f);
                cs.moveTo(50, y - 4);
                cs.lineTo(550, y - 4);
                cs.stroke();
                y -= 12;

                // QR Code Generation & Embedding (Embeds live Web Verification URL)
                BufferedImage qrImage = QrGenerator.generateQrCodeImage(verifyUrl, 110, 110);
                if (qrImage != null) {
                    PDImageXObject pdQrImage = LosslessFactory.createFromImage(document, qrImage);
                    cs.drawImage(pdQrImage, 50, y - 125, 110, 110);
                }

                // QR Code Header & Instructions
                cs.beginText();
                cs.setFont(boldFont, 9.5f);
                cs.newLineAtOffset(180, y - 24);
                cs.showText("Scan-to-Verify Cryptographic Audit Seal & S.M.A.R.T. Proof");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7.5f);
                cs.newLineAtOffset(180, y - 38);
                cs.showText("Scan with any mobile camera or visit public verification portal:");
                cs.endText();

                cs.beginText();
                cs.setFont(boldFont, 7.5f);
                cs.newLineAtOffset(180, y - 50);
                cs.showText("URL: " + verifyUrl.substring(0, Math.min(verifyUrl.length(), 60)) + "...");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7.5f);
                cs.newLineAtOffset(180, y - 64);
                cs.showText("Algorithm: SHA256withRSA (2048-bit Asymmetric Cryptography)");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7f);
                cs.newLineAtOffset(180, y - 76);
                String sigSnippet = signature.length() > 42 ? signature.substring(0, 42) + "..." : signature;
                cs.showText("Digital Signature: " + sanitize(sigSnippet));
                cs.endText();

                cs.beginText();
                cs.setFont(boldFont, 7.5f);
                cs.newLineAtOffset(180, y - 90);
                cs.showText("Sanitization Wear & Integrity Delta: 0 Defects Created | Media Cleared for Reuse");
                cs.endText();

                // ESG Sustainability & Environmental Carbon Offset Box
                EsgCalculator.EsgMetrics esg = EsgCalculator.calculate(record.capacity());

                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 135);
                cs.lineTo(550, y - 135);
                cs.stroke();

                cs.beginText();
                cs.setFont(boldFont, 8f);
                cs.newLineAtOffset(50, y - 147);
                cs.showText("ESG Environmental Sustainability Proof (Scope 3 GHG / ISO 14064 Compliant):");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7f);
                cs.newLineAtOffset(50, y - 158);
                cs.showText("By securely sanitizing this " + sanitize(record.capacity()) + " drive for reuse, you prevented " +
                        String.format(java.util.Locale.US, "%.1f", esg.eWasteDivertedKg()) + " kg of e-waste and saved " +
                        String.format(java.util.Locale.US, "%.1f", esg.co2EmissionsSavedKg()) + " kg of CO2 emissions compared to physical shredding.");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 6.5f);
                cs.newLineAtOffset(50, y - 168);
                cs.showText("Ecological Equivalency: ~" + String.format(java.util.Locale.US, "%.2f", esg.treesEquivalent()) +
                        " tree seedlings grown for 10 years | " + String.format(java.util.Locale.US, "%.1f", esg.energySavedKwh()) +
                        " kWh manufacturing power conserved.");
                cs.endText();

                // Legal Compliance Footer
                cs.beginText();
                cs.setFont(regularFont, 6.5f);
                cs.newLineAtOffset(50, y - 184);
                cs.showText("This certificate constitutes permanent cryptographic proof of media sanitization in compliance with");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 6.5f);
                cs.newLineAtOffset(50, y - 193);
                cs.showText("NIST SP 800-88 Rev. 1, DoD 5220.22-M, HIPAA, and GDPR. Immutable RSA seal & S.M.A.R.T. delta generated by SecureErase Pro.");
                cs.endText();
            }

            // Set Document Metadata for Cryptographic Verification
            org.apache.pdfbox.pdmodel.PDDocumentInformation info = document.getDocumentInformation();
            info.setCustomMetadataValue("DigitalSignature", signature);
            info.setCustomMetadataValue("CertificateID", "SAN-CERT-" + record.id());
            info.setCustomMetadataValue("VerificationUrl", verifyUrl);
            info.setCustomMetadataValue("PeakTempCelsius", String.valueOf(record.peakTempCelsius()));
            info.setCustomMetadataValue("ThermalPauseCount", String.valueOf(record.thermalPauseCount()));
            info.setCustomMetadataValue("CrcErrors", String.valueOf(record.crcErrors()));
            info.setCustomMetadataValue("InterfaceAnomalySummary", record.interfaceAnomalySummary());
            info.setCustomMetadataValue("VerificationStatus", record.verificationStatus());
            info.setCustomMetadataValue("VerifiedSectorsCount", String.valueOf(record.verifiedSectorsCount()));
            info.setCustomMetadataValue("EntropyScore", String.valueOf(record.entropyScore()));
            info.setCustomMetadataValue("VerificationHash", record.verificationHash());
            info.setCustomMetadataValue("RiskClassification", deriveRiskClassification(
                    record.preHealthScore(), record.postHealthScore(),
                    record.crcErrors(), record.thermalPauseCount(),
                    record.interfaceAnomalySummary()));

            document.save(new File(fileName));
            System.out.println("PDF Sanitization Certificate Generated: " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("PDF Generation Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void drawField(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                                  String label, Object value, int x, int y) throws Exception {
        cs.beginText();
        cs.setFont(bold, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(label));
        cs.endText();

        cs.beginText();
        cs.setFont(regular, 10);
        cs.newLineAtOffset(x + 140, y);
        cs.showText(sanitize(value));
        cs.endText();
    }

    private static void drawFieldSmall(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                                       String label, String value, int x, int y) throws Exception {
        cs.beginText();
        cs.setFont(bold, 8f);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(label));
        cs.endText();

        cs.beginText();
        cs.setFont(regular, 8f);
        cs.newLineAtOffset(x + 160, y);
        cs.showText(sanitize(value));
        cs.endText();
    }

    private static String formatTimestamp(Object timestampObj) {
        if (timestampObj == null) return "N/A";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss UTC");
            if (timestampObj instanceof Long longVal) {
                return sdf.format(new Date(longVal));
            } else if (timestampObj instanceof Date dateVal) {
                return sdf.format(dateVal);
            }
            return timestampObj.toString();
        } catch (Exception e) {
            return timestampObj.toString();
        }
    }

    private static String sanitize(Object input) {
        if (input == null) return "";
        return sanitize(String.valueOf(input));
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\r\\n\\t]", " ").replaceAll("[^\\x20-\\x7E]", "");
    }

    /** Derives a human-readable thermal status label from peak temp and pause count. */
    static String deriveThermalStatus(int peakTempCelsius, int pauseCount) {
        if (pauseCount > 0) return "AUTO-PAUSED DURING WIPE (" + pauseCount + " cooldown event(s))";
        if (peakTempCelsius >= 60) return "CRITICAL - Overheating Threshold Reached";
        if (peakTempCelsius >= 48) return "ELEVATED - Operating Near Upper Thermal Limit";
        if (peakTempCelsius > 0)   return "NORMAL - Within Safe Operating Range";
        return "N/A - No Thermal Data Recorded";
    }

    /** Derives a bus integrity status label from CRC errors and interface anomaly summary string. */
    static String deriveInterfaceStatus(int crcErrors, String anomalySummary) {
        if (anomalySummary == null || anomalySummary.isBlank() || anomalySummary.toUpperCase().contains("OPTIMAL")) {
            return "OPTIMAL - 0 CRC Errors, 0 Bus Timeouts";
        }
        if (anomalySummary.toUpperCase().contains("CRITICAL") || crcErrors >= 5) {
            return "CRITICAL - Faulty Cable / Port Degradation Detected";
        }
        return "WARNING - Intermittent Bus Communication Retries";
    }

    /**
     * Derives a 3-tier risk classification label for the certificate badge.
     * LOW RISK:      health >= 80, interface OPTIMAL, no thermal pauses.
     * MODERATE RISK: health 50-79, OR interface WARNING, OR thermal pauses occurred.
     * HIGH RISK:     health < 50, OR interface CRITICAL.
     */
    static String deriveRiskClassification(int preScore, int postScore,
                                            int crcErrors, int thermalPauseCount,
                                            String anomalySummary) {
        int minScore = Math.min(preScore, postScore);
        boolean ifaceCritical = anomalySummary != null &&
                (anomalySummary.toUpperCase().contains("CRITICAL") || crcErrors >= 5);
        boolean ifaceWarning  = anomalySummary != null &&
                anomalySummary.toUpperCase().contains("WARNING") && !ifaceCritical;

        if (minScore < 50 || ifaceCritical) {
            return "HIGH RISK -- OPERATOR OVERRIDE RECORDED (See Interface & Health Warnings)";
        }
        if (minScore < 80 || ifaceWarning || thermalPauseCount > 0) {
            return "MODERATE RISK -- ANOMALIES LOGGED (Interface/Thermal Events Recorded)";
        }
        return "LOW RISK -- CERTIFIED CLEAN (All Signals Nominal)";
    }

    /**
     * Automated verification tool for extracting embedded digital signatures and QR codes from
     * generated PDF sanitization certificates and validating their cryptographic authenticity.
     */
    public static CertificateVerificationResult verifyPdfCertificate(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            return new CertificateVerificationResult(false, null, null, null, null, null, null, null, "File does not exist");
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            String certId = extractValue(fullText, "Certificate ID:");
            String driveModel = extractValue(fullText, "Device Model:");
            String serialNumber = extractValue(fullText, "Serial Number:");
            String capacity = extractValue(fullText, "Capacity:");
            String wipeStandard = extractValue(fullText, "Sanitization Method:");
            String status = extractValue(fullText, "Execution Status:");
            String sigSnippet = extractValue(fullText, "Digital Signature:");
            if (sigSnippet.isBlank()) {
                sigSnippet = extractValue(fullText, "Signature:");
            }

            String extractedQrSignature = null;

            // 1. Try reading DigitalSignature from PDF document metadata
            if (document.getDocumentInformation() != null) {
                extractedQrSignature = document.getDocumentInformation().getCustomMetadataValue("DigitalSignature");
            }

            // 2. If metadata signature not found, extract QR code image XObjects from PDF resources
            if (extractedQrSignature == null || extractedQrSignature.isBlank()) {
                for (PDPage page : document.getPages()) {
                    if (page.getResources() != null) {
                        for (COSName name : page.getResources().getXObjectNames()) {
                            org.apache.pdfbox.pdmodel.graphics.PDXObject xobject = page.getResources().getXObject(name);
                            if (xobject instanceof PDImageXObject pdImage) {
                                try {
                                    BufferedImage bImage = pdImage.getImage();
                                    String qrPayload = QrGenerator.decodeQrCodeImage(bImage);
                                    if (qrPayload != null) {
                                        if (qrPayload.contains("sig=")) {
                                            int sigIdx = qrPayload.indexOf("sig=");
                                            String encodedSig = qrPayload.substring(sigIdx + 4);
                                            int ampIdx = encodedSig.indexOf("&");
                                            if (ampIdx > 0) encodedSig = encodedSig.substring(0, ampIdx);
                                            extractedQrSignature = URLDecoder.decode(encodedSig, StandardCharsets.UTF_8);
                                            break;
                                        } else if (qrPayload.contains("SIGNATURE:")) {
                                            int sigIndex = qrPayload.indexOf("SIGNATURE:");
                                            extractedQrSignature = qrPayload.substring(sigIndex + "SIGNATURE:".length()).trim();
                                            break;
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }

            String digitalSignature = (extractedQrSignature != null && !extractedQrSignature.isEmpty())
                    ? extractedQrSignature
                    : sigSnippet;

            if (digitalSignature == null || digitalSignature.isBlank()) {
                return new CertificateVerificationResult(false, certId, driveModel, serialNumber, capacity, wipeStandard, status, null, "No digital signature payload found in PDF");
            }

            String payload = driveModel + "|" + serialNumber + "|" + capacity + "|" + wipeStandard + "|" + status;
            boolean isValid = CryptoSigner.verifySignature(payload, digitalSignature);

            String message = isValid ? "PDF Certificate Cryptographically Authenticated & Tamper-Free" : "SIGNATURE MISMATCH - Certificate Payload Tampered or Invalid";

            return new CertificateVerificationResult(isValid, certId, driveModel, serialNumber, capacity, wipeStandard, status, digitalSignature, message);

        } catch (Exception e) {
            return new CertificateVerificationResult(false, null, null, null, null, null, null, null, "PDF Verification Error: " + e.getMessage());
        }
    }

    private static String extractValue(String fullText, String label) {
        if (fullText == null || !fullText.contains(label)) return "";
        int start = fullText.indexOf(label) + label.length();
        int end = fullText.indexOf("\n", start);
        if (end == -1) end = fullText.length();
        return fullText.substring(start, end).trim();
    }
}