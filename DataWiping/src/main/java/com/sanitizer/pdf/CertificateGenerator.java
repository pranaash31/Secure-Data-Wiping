package com.sanitizer.pdf;

import com.sanitizer.db.AuditDb;
import com.sanitizer.util.QrGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CertificateGenerator {

    public static String generateCertificate(AuditDb.AuditRecord record) {
        String fileName = "Sanitization_Certificate_" + record.id() + ".pdf";

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Fonts
                var boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Format timestamp using SimpleDateFormat
                String formattedDate = formatTimestamp(record.timestamp());

                // Header Banner
                cs.beginText();
                cs.setFont(boldFont, 20);
                cs.newLineAtOffset(140, 720);
                cs.showText("CERTIFICATE OF DATA SANITIZATION");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(180, 700);
                cs.showText("NIST SP 800-88 / DoD 5220.22-M Compliance Proof");
                cs.endText();

                // Horizontal Line
                cs.setLineWidth(1.0f);
                cs.moveTo(50, 685);
                cs.lineTo(550, 685);
                cs.stroke();

                // Record Details
                int y = 640;
                int leading = 25;

                drawField(cs, boldFont, regularFont, "Certificate ID:", "SAN-CERT-" + record.id(), 50, y);
                drawField(cs, boldFont, regularFont, "Timestamp:", formattedDate, 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Device Model:", record.driveModel(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Serial Number:", record.serialNumber(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Capacity:", record.capacity(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Sanitization Method:", record.wipeStandard(), 50, y -= leading);
                drawField(cs, boldFont, regularFont, "Execution Status:", record.status(), 50, y -= leading);

                // Horizontal Line
                cs.setLineWidth(0.5f);
                cs.moveTo(50, y - 20);
                cs.lineTo(550, y - 20);
                cs.stroke();

                // QR Code Generation & Embedding
                String signature = record.digitalSignature() != null ? record.digitalSignature() : "N/A";
                BufferedImage qrImage = QrGenerator.generateQrCodeImage(
                        "SAN-CERT-ID:" + record.id() + "\nSIGNATURE:" + signature, 150, 150
                );

                if (qrImage != null) {
                    PDImageXObject pdQrImage = LosslessFactory.createFromImage(document, qrImage);
                    cs.drawImage(pdQrImage, 50, y - 200, 150, 150);
                }

                // RSA Signature Details Text
                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(220, y - 60);
                cs.showText("Cryptographic Verification Seal");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(220, y - 80);
                cs.showText("Algorithm: SHA256withRSA (2048-bit)");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7);
                cs.newLineAtOffset(220, y - 100);
                String sigSnippet = signature.length() > 45 ? signature.substring(0, 45) + "..." : signature;
                cs.showText("Signature: " + sanitize(sigSnippet));
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(220, y - 130);
                cs.showText("Scan QR code to audit tamper-proof digital signature.");
                cs.endText();
            }

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
        cs.setFont(bold, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(label));
        cs.endText();

        cs.beginText();
        cs.setFont(regular, 11);
        cs.newLineAtOffset(x + 150, y);
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

    // Overloaded sanitize methods to handle nulls, primitives, and objects safely
    private static String sanitize(Object input) {
        if (input == null) return "";
        return sanitize(String.valueOf(input));
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\r\\n\\t]", " ").replaceAll("[^\\x20-\\x7E]", "");
    }
}