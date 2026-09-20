package com.sanitizer.quarantine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanitizer.util.AppLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Enterprise PDF and JSON Report Generator for Defective Storage Assets & Hardware Quarantine.
 * Produces official NIST SP 800-88 Rev. 1 / NSA / DIN 66399 compliant Physical Destruction Orders.
 */
public class QuarantineReportGenerator {

    private static final String MODULE = "QuarantineReportGen";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Generates a formal Defective Hardware Quarantine & Physical Destruction Order PDF document.
     * @return Absolute file path to the generated PDF.
     */
    public static String generatePdfReport(QuarantineRecord record) {
        String fileName = "Quarantine_Report_" + record.quarantineId().replace("-", "_") + ".pdf";
        File targetFile = new File(fileName);

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                var boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                var obliqueFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                // --- Top Red Hazard Header Bar ---
                cs.setNonStrokingColor(0.85f, 0.15f, 0.15f); // Red
                cs.addRect(40, 720, 520, 45);
                cs.fill();

                cs.setNonStrokingColor(1.0f, 1.0f, 1.0f); // White
                cs.beginText();
                cs.setFont(boldFont, 14);
                cs.newLineAtOffset(55, 745);
                cs.showText("DEFECTIVE HARDWARE QUARANTINE & DESTRUCTION ORDER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 8.5f);
                cs.newLineAtOffset(55, 730);
                cs.showText("Official NIST SP 800-88 Rev. 1 / NSA CSS / DIN 66399 Media Sanitization Failure Record");
                cs.endText();

                cs.setNonStrokingColor(0.0f, 0.0f, 0.0f); // Reset to Black

                int y = 700;
                int leading = 15;

                // Section: Quarantine Metadata
                drawField(cs, boldFont, regularFont, "Quarantine Tracking ID:", record.quarantineId(), 50, y);
                drawField(cs, boldFont, regularFont, "Assessment Timestamp (UTC):", record.timestampUtc(), 320, y);
                y -= leading;

                drawField(cs, boldFont, regularFont, "Target Device Model:", record.driveModel(), 50, y);
                drawField(cs, boldFont, regularFont, "Serial Number:", record.serialNumber(), 320, y);
                y -= leading;

                drawField(cs, boldFont, regularFont, "Raw Storage Capacity:", record.capacity(), 50, y);
                drawField(cs, boldFont, regularFont, "Hardware Media / Interface:", record.mediaType() + " (" + record.interfaceType() + ")", 320, y);
                y -= leading;

                drawField(cs, boldFont, regularFont, "Attempted Sanitization Policy:", record.attemptedStandard(), 50, y);
                y -= leading;

                drawField(cs, boldFont, regularFont, "Primary Failure Diagnosis:", record.failureReason(), 50, y);
                y -= leading;

                drawField(cs, boldFont, regularFont, "S.M.A.R.T. Health Score Delta:", record.preHealthScore() + " -> " + record.postHealthScore() + " (" + record.smartIntegritySummary() + ")", 50, y);
                y -= 20;

                // Divider Line
                cs.setLineWidth(0.8f);
                cs.setStrokingColor(0.7f, 0.7f, 0.7f);
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();
                y -= 14;

                // --- Section: Granular Failing LBA / Bad Sector Table ---
                cs.beginText();
                cs.setFont(boldFont, 10);
                cs.newLineAtOffset(50, y);
                cs.showText("GRANULAR SECTOR FAILURE TRACKING & BAD LBA FAULT LOG:");
                cs.endText();
                y -= 15;

                // Table Header
                cs.setNonStrokingColor(0.92f, 0.94f, 0.96f);
                cs.addRect(50, y - 4, 500, 16);
                cs.fill();
                cs.setNonStrokingColor(0.0f, 0.0f, 0.0f);

                cs.beginText();
                cs.setFont(boldFont, 8);
                cs.newLineAtOffset(55, y);
                cs.showText("LBA Hex Range");
                cs.newLineAtOffset(130, 0);
                cs.showText("Sector Count");
                cs.newLineAtOffset(80, 0);
                cs.showText("Pass #");
                cs.newLineAtOffset(50, 0);
                cs.showText("Fault Error Code");
                cs.newLineAtOffset(110, 0);
                cs.showText("Diagnostic Timestamp");
                cs.endText();
                y -= 14;

                List<LbaFailureRecord> sectors = record.failingSectors();
                int rowsToShow = Math.min(6, sectors.size());
                if (sectors.isEmpty()) {
                    cs.beginText();
                    cs.setFont(obliqueFont, 8);
                    cs.newLineAtOffset(55, y);
                    cs.showText("Low-level I/O write fault triggered across multiple raw block boundaries. (See System Diagnostic Log)");
                    cs.endText();
                    y -= 13;
                } else {
                    for (int i = 0; i < rowsToShow; i++) {
                        LbaFailureRecord f = sectors.get(i);
                        cs.beginText();
                        cs.setFont(regularFont, 7.5f);
                        cs.newLineAtOffset(55, y);
                        cs.showText(f.formattedHexRange());
                        cs.newLineAtOffset(130, 0);
                        cs.showText(String.format("%,d LBAs", f.sectorCount()));
                        cs.newLineAtOffset(80, 0);
                        cs.showText("Pass " + f.passNumber());
                        cs.newLineAtOffset(50, 0);
                        cs.showText(f.errorType());
                        cs.newLineAtOffset(110, 0);
                        cs.showText(f.timestamp().length() > 19 ? f.timestamp().substring(0, 19) : f.timestamp());
                        cs.endText();
                        y -= 12;
                    }
                }

                if (sectors.size() > rowsToShow) {
                    cs.beginText();
                    cs.setFont(obliqueFont, 7.5f);
                    cs.newLineAtOffset(55, y);
                    cs.showText(String.format("... and %,d additional defective LBA ranges recorded in system audit log.", sectors.size() - rowsToShow));
                    cs.endText();
                    y -= 12;
                }

                y -= 8;
                // Divider Line
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();
                y -= 16;

                // --- Section: Mandatory Physical Destruction Order ---
                cs.setNonStrokingColor(0.98f, 0.95f, 0.92f);
                cs.addRect(50, y - 85, 500, 95);
                cs.fill();

                cs.setStrokingColor(0.85f, 0.45f, 0.05f); // Orange border
                cs.setLineWidth(1.0f);
                cs.addRect(50, y - 85, 500, 95);
                cs.stroke();

                cs.setNonStrokingColor(0.7f, 0.2f, 0.0f); // Dark Orange
                cs.beginText();
                cs.setFont(boldFont, 10);
                cs.newLineAtOffset(60, y - 2);
                cs.showText("MANDATORY PHYSICAL DESTRUCTION DIRECTIVE: " + record.destructionRecommendation().getTitle().toUpperCase());
                cs.endText();

                cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
                cs.beginText();
                cs.setFont(boldFont, 8);
                cs.newLineAtOffset(60, y - 18);
                cs.showText("Governing Regulatory Standard: " + record.destructionRecommendation().getStandardReference());
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7.5f);
                cs.newLineAtOffset(60, y - 32);
                cs.showText("Technical Requirement: " + record.destructionRecommendation().getTechnicalDescription());
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 7.5f);
                cs.newLineAtOffset(60, y - 46);
                cs.showText("Reason: Logical overwriting cannot guarantee zero-residual sanitization on failing or reallocated sectors.");
                cs.endText();

                cs.beginText();
                cs.setFont(boldFont, 7.5f);
                cs.newLineAtOffset(60, y - 62);
                cs.showText("DISPOSITION STATUS: PERMANENTLY QUARANTINED — DO NOT REUSE, RESELL, OR RE-COMMISSION.");
                cs.endText();

                y -= 105;

                // --- Section: Chain of Custody & Sign-Off Attestation ---
                cs.beginText();
                cs.setFont(boldFont, 9);
                cs.newLineAtOffset(50, y);
                cs.showText("CHAIN OF CUSTODY & PHYSICAL DESTRUCTION SIGN-OFF:");
                cs.endText();
                y -= 16;

                // Sign-off boxes
                cs.setStrokingColor(0.6f, 0.6f, 0.6f);
                cs.setLineWidth(0.6f);

                // Box 1: Custodian
                cs.addRect(50, y - 45, 240, 50);
                cs.stroke();
                cs.beginText();
                cs.setFont(boldFont, 7.5f);
                cs.newLineAtOffset(55, y - 3);
                cs.showText("Security Custodian / Sanitize Operator:");
                cs.endText();
                cs.beginText();
                cs.setFont(regularFont, 7);
                cs.newLineAtOffset(55, y - 24);
                cs.showText("Signature: __________________________________");
                cs.newLineAtOffset(0, -12);
                cs.showText("Badge / Employee ID: _________ Date: _________");
                cs.endText();

                // Box 2: Physical Destruction Facility Witness
                cs.addRect(310, y - 45, 240, 50);
                cs.stroke();
                cs.beginText();
                cs.setFont(boldFont, 7.5f);
                cs.newLineAtOffset(315, y - 3);
                cs.showText("Destruction Facility Witness / E-Waste Recycler:");
                cs.endText();
                cs.beginText();
                cs.setFont(regularFont, 7);
                cs.newLineAtOffset(315, y - 24);
                cs.showText("Witness Signature: ___________________________");
                cs.newLineAtOffset(0, -12);
                cs.showText("Facility Name: _______________ Date: _________");
                cs.endText();

                y -= 60;

                // Footer Digital Attestation Proof
                cs.setLineWidth(0.5f);
                cs.setStrokingColor(0.8f, 0.8f, 0.8f);
                cs.moveTo(50, y);
                cs.lineTo(550, y);
                cs.stroke();
                y -= 12;

                cs.beginText();
                cs.setFont(regularFont, 6.5f);
                cs.newLineAtOffset(50, y);
                cs.showText("Cryptographic Hardware Quarantine Attestation (SHA256withRSA): " +
                        (record.digitalAttestationSignature() != null ? record.digitalAttestationSignature() : "N/A"));
                cs.endText();
            }

            document.save(targetFile);
            AppLogger.info(MODULE, "Generated Defective Hardware Quarantine Report PDF: " + targetFile.getAbsolutePath());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            AppLogger.error(MODULE, "Failed to generate Quarantine PDF Report", e);
            return null;
        }
    }

    /**
     * Generates structured JSON representation of the Quarantine record.
     */
    public static String generateJsonReport(QuarantineRecord record) {
        return GSON.toJson(record);
    }

    /**
     * Exports the Quarantine record to a JSON file on disk.
     */
    public static void exportToJson(QuarantineRecord record, File targetFile) throws IOException {
        String json = generateJsonReport(record);
        Files.writeString(targetFile.toPath(), json, StandardCharsets.UTF_8);
        AppLogger.info(MODULE, "Exported Quarantine JSON record to: " + targetFile.getAbsolutePath());
    }

    private static void drawField(PDPageContentStream cs, PDType1Font labelFont, PDType1Font valFont, String label, String val, int x, int y) throws IOException {
        cs.beginText();
        cs.setFont(labelFont, 8.5f);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(valFont, 8.5f);
        cs.newLineAtOffset(x + 135, y);
        cs.showText(val != null ? val : "N/A");
        cs.endText();
    }
}
