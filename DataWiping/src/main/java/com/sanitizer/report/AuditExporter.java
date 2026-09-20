package com.sanitizer.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sanitizer.db.AuditDb;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enterprise Multi-Format Audit Data Exporter (CSV, Structured JSON, Excel SpreadsheetML).
 * Generates consolidated corporate compliance summaries and regulatory audit packages.
 */
public class AuditExporter {

    private static final String MODULE = "AuditExporter";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_INSTANT;

    public record ComplianceSummary(
            int totalDrivesProcessed,
            int successCount,
            int failureCount,
            double passRatePercent,
            long totalCapacityBytes,
            String formattedTotalCapacity,
            double averageResidualEntropy,
            EsgCalculator.EsgMetrics esgImpact,
            String generatedAtUtc,
            String earliestRecordTimestamp,
            String latestRecordTimestamp
    ) {}

    /**
     * Computes consolidated executive KPIs and compliance metrics across a list of audit records.
     */
    public static ComplianceSummary generateComplianceSummary(List<AuditDb.AuditRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ComplianceSummary(
                    0, 0, 0, 100.0, 0L, "0 GB", 0.0,
                    EsgCalculator.calculate("0 GB"),
                    ISO_FMT.format(Instant.now()), "N/A", "N/A"
            );
        }

        int total = records.size();
        int success = 0;
        int failure = 0;
        long totalBytes = 0;
        double totalEntropy = 0;
        int entropyCount = 0;

        String earliest = records.get(0).timestamp();
        String latest = records.get(0).timestamp();

        for (AuditDb.AuditRecord r : records) {
            if ("SUCCESS".equalsIgnoreCase(r.status())) {
                success++;
            } else {
                failure++;
            }

            long b = parseCapacityToBytes(r.capacity());
            totalBytes += b;

            totalEntropy += r.entropyScore();
            entropyCount++;

            if (r.timestamp() != null) {
                if (earliest == null || r.timestamp().compareTo(earliest) < 0) earliest = r.timestamp();
                if (latest == null || r.timestamp().compareTo(latest) > 0) latest = r.timestamp();
            }
        }

        double passRate = total > 0 ? ((double) success / total) * 100.0 : 100.0;
        double avgEntropy = entropyCount > 0 ? (totalEntropy / entropyCount) : 0.0;
        EsgCalculator.EsgMetrics esg = EsgCalculator.calculateAggregate(records);

        return new ComplianceSummary(
                total,
                success,
                failure,
                passRate,
                totalBytes,
                formatBytes(totalBytes),
                avgEntropy,
                esg,
                ISO_FMT.format(Instant.now()),
                earliest != null ? earliest : "N/A",
                latest != null ? latest : "N/A"
        );
    }

    // ── 1. CSV EXPORT (RFC 4180 Standard with Executive Header) ───────

    public static String generateCsvContent(List<AuditDb.AuditRecord> records) {
        ComplianceSummary summary = generateComplianceSummary(records);
        StringBuilder sb = new StringBuilder();

        // Corporate Executive Compliance Summary Header Block
        sb.append("# ════════════════════════════════════════════════════════════════════════════════\n");
        sb.append("# SECURE DATA WIPING ENTERPRISE AUDIT TRAIL REPORT\n");
        sb.append("# Standard Compliance: NIST SP 800-88 Rev. 1 / ISO/IEC 27040 / GDPR Art. 17 / HIPAA\n");
        sb.append("# Generated At (UTC): ").append(summary.generatedAtUtc()).append("\n");
        sb.append("# Total Assets Processed: ").append(summary.totalDrivesProcessed()).append(" devices\n");
        sb.append("# Sanitized Data Volume: ").append(summary.formattedTotalCapacity()).append("\n");
        sb.append(String.format(Locale.US, "# Pass Rate: %.2f%% (Success: %d, Fail: %d)\n", summary.passRatePercent(), summary.successCount(), summary.failureCount()));
        sb.append(String.format(Locale.US, "# Avg Zero-Residual Shannon Entropy: %.4f bits/byte (0.000%% Residual Target)\n", summary.averageResidualEntropy()));
        sb.append(String.format(Locale.US, "# ESG Circular Impact: %.1f kg E-Waste Diverted | %.1f kg CO2e Mitigated (~%.2f Trees Saved)\n",
                summary.esgImpact().eWasteDivertedKg(), summary.esgImpact().co2EmissionsSavedKg(), summary.esgImpact().treesEquivalent()));
        sb.append("# ════════════════════════════════════════════════════════════════════════════════\n");

        // Column Headers
        sb.append(String.join(",", List.of(
                "Audit_ID",
                "Timestamp",
                "Device_Model",
                "Serial_Number",
                "Capacity",
                "Sanitization_Standard",
                "Status",
                "Pre_Health_Score",
                "Post_Health_Score",
                "Bad_Blocks_Delta",
                "Wear_Delta_Percent",
                "Integrity_Delta_Summary",
                "Peak_Temp_Celsius",
                "Thermal_Pause_Count",
                "CRC_Errors",
                "Interface_Anomaly_Telemetry",
                "Verification_Status",
                "Verified_Sectors_LBA",
                "Residual_Entropy_Score",
                "Sample_SHA256_Proof",
                "RSA_Digital_Signature"
        ))).append("\n");

        if (records != null) {
            for (AuditDb.AuditRecord r : records) {
                sb.append(r.id()).append(",")
                  .append(csvEscape(r.timestamp())).append(",")
                  .append(csvEscape(r.driveModel())).append(",")
                  .append(csvEscape(r.serialNumber())).append(",")
                  .append(csvEscape(r.capacity())).append(",")
                  .append(csvEscape(r.wipeStandard())).append(",")
                  .append(csvEscape(r.status())).append(",")
                  .append(r.preHealthScore()).append(",")
                  .append(r.postHealthScore()).append(",")
                  .append(r.badBlocksDelta()).append(",")
                  .append(r.wearDeltaPercent()).append(",")
                  .append(csvEscape(r.smartDeltaSummary())).append(",")
                  .append(r.peakTempCelsius()).append(",")
                  .append(r.thermalPauseCount()).append(",")
                  .append(r.crcErrors()).append(",")
                  .append(csvEscape(r.interfaceAnomalySummary())).append(",")
                  .append(csvEscape(r.verificationStatus())).append(",")
                  .append(r.verifiedSectorsCount()).append(",")
                  .append(String.format(Locale.US, "%.4f", r.entropyScore())).append(",")
                  .append(csvEscape(r.verificationHash())).append(",")
                  .append(csvEscape(r.digitalSignature()))
                  .append("\n");
            }
        }

        return sb.toString();
    }

    public static void exportToCsv(List<AuditDb.AuditRecord> records, File targetFile) throws IOException {
        String csv = generateCsvContent(records);
        Files.writeString(targetFile.toPath(), csv, StandardCharsets.UTF_8);
        AppLogger.info(MODULE, "Exported " + (records != null ? records.size() : 0) + " audit records to CSV: " + targetFile.getAbsolutePath());
    }

    // ── 2. STRUCTURED JSON EXPORT ─────────────────────────────────────

    public static String generateJsonContent(List<AuditDb.AuditRecord> records) {
        ComplianceSummary summary = generateComplianceSummary(records);

        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reportTitle", "Enterprise Data Sanitization & Regulatory Audit Report");
        metadata.put("generatedAtUtc", summary.generatedAtUtc());
        metadata.put("software", "SecureErase Pro v2.0 Enterprise");
        metadata.put("complianceStandards", List.of(
                "NIST SP 800-88 Rev. 1 Section 4.7 (Media Sanitization & Verification)",
                "ISO/IEC 27040:2015 (Storage Security)",
                "DoD 5220.22-M / DoD 5220.22-M ECE",
                "British HMG Infosec Standard 5",
                "German BSI-VS / BSI TL-03423",
                "Canadian RCMP TSSIT OPS-II",
                "EU GDPR Article 17 (Right to Erasure)",
                "HIPAA Security Rule 45 CFR § 164.310"
        ));
        root.put("metadata", metadata);

        Map<String, Object> execSummary = new LinkedHashMap<>();
        execSummary.put("totalAssetsProcessed", summary.totalDrivesProcessed());
        execSummary.put("successCount", summary.successCount());
        execSummary.put("failureCount", summary.failureCount());
        execSummary.put("passRatePercent", summary.passRatePercent());
        execSummary.put("totalCapacitySanitizedBytes", summary.totalCapacityBytes());
        execSummary.put("formattedTotalCapacity", summary.formattedTotalCapacity());
        execSummary.put("averageResidualEntropyBitsPerByte", summary.averageResidualEntropy());
        execSummary.put("dateRangeEarliest", summary.earliestRecordTimestamp());
        execSummary.put("dateRangeLatest", summary.latestRecordTimestamp());

        Map<String, Object> esgMap = new LinkedHashMap<>();
        esgMap.put("eWasteDivertedKg", summary.esgImpact().eWasteDivertedKg());
        esgMap.put("co2EmissionsSavedKg", summary.esgImpact().co2EmissionsSavedKg());
        esgMap.put("treesEquivalent", summary.esgImpact().treesEquivalent());
        esgMap.put("energySavedKwh", summary.esgImpact().energySavedKwh());
        esgMap.put("disposalIndex", "100% Circular Reuse (Zero Landfill)");
        execSummary.put("environmentalEsgImpact", esgMap);

        root.put("executiveSummary", execSummary);
        root.put("auditRecords", records != null ? records : List.of());

        return GSON.toJson(root);
    }

    public static void exportToJson(List<AuditDb.AuditRecord> records, File targetFile) throws IOException {
        String json = generateJsonContent(records);
        Files.writeString(targetFile.toPath(), json, StandardCharsets.UTF_8);
        AppLogger.info(MODULE, "Exported " + (records != null ? records.size() : 0) + " audit records to JSON: " + targetFile.getAbsolutePath());
    }

    // ── 3. EXCEL SPREADSHEETML (XML Native Spreadsheet for Excel) ────

    public static String generateExcelXmlContent(List<AuditDb.AuditRecord> records) {
        ComplianceSummary s = generateComplianceSummary(records);
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:html=\"http://www.w3.org/TR/REC-html40\">\n");

        // Styles
        sb.append(" <Styles>\n");
        sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Color=\"#1E293B\"/></Style>\n");
        sb.append("  <Style ss:ID=\"TitleStyle\"><Font ss:FontName=\"Arial\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#0F172A\"/></Style>\n");
        sb.append("  <Style ss:ID=\"SubtitleStyle\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Italic=\"1\" ss:Color=\"#64748B\"/></Style>\n");
        sb.append("  <Style ss:ID=\"KpiHeader\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#475569\"/><Interior ss:Color=\"#F1F5F9\" ss:Pattern=\"Solid\"/></Style>\n");
        sb.append("  <Style ss:ID=\"KpiValue\"><Font ss:FontName=\"Arial\" ss:Size=\"12\" ss:Bold=\"1\" ss:Color=\"#2563EB\"/></Style>\n");
        sb.append("  <Style ss:ID=\"TableHeader\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#1E293B\" ss:Pattern=\"Solid\"/><Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/></Style>\n");
        sb.append("  <Style ss:ID=\"StatusPass\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#166534\"/><Interior ss:Color=\"#DCFCE7\" ss:Pattern=\"Solid\"/><Alignment ss:Horizontal=\"Center\"/></Style>\n");
        sb.append("  <Style ss:ID=\"StatusFail\"><Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#991B1B\"/><Interior ss:Color=\"#FEE2E2\" ss:Pattern=\"Solid\"/><Alignment ss:Horizontal=\"Center\"/></Style>\n");
        sb.append("  <Style ss:ID=\"MonoCode\"><Font ss:FontName=\"Courier New\" ss:Size=\"9\" ss:Color=\"#334155\"/></Style>\n");
        sb.append(" </Styles>\n");

        // ── Worksheet 1: Executive KPI Summary ──
        sb.append(" <Worksheet ss:Name=\"Executive Audit Summary\">\n");
        sb.append("  <Table ss:DefaultRowHeight=\"18\">\n");
        sb.append("   <Column ss:Width=\"220\"/>\n");
        sb.append("   <Column ss:Width=\"280\"/>\n");
        sb.append("   <Column ss:Width=\"220\"/>\n");
        sb.append("   <Column ss:Width=\"280\"/>\n");

        sb.append("   <Row ss:Height=\"28\"><Cell ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">SECURE ERASE ENTERPRISE COMPLIANCE AUDIT</Data></Cell></Row>\n");
        sb.append("   <Row><Cell ss:StyleID=\"SubtitleStyle\"><Data ss:Type=\"String\">Automated Sanitization &amp; Zero-Residual Entropy Verification Report</Data></Cell></Row>\n");
        sb.append("   <Row ss:Height=\"10\"></Row>\n");

        // KPI Table Rows
        addExcelRow(sb, "Report Generation Date (UTC):", s.generatedAtUtc(), "Compliance Frameworks:", "NIST SP 800-88 / ISO 27040 / GDPR");
        addExcelRow(sb, "Total Storage Media Assets:", String.valueOf(s.totalDrivesProcessed()), "Data Sanitization Volume:", s.formattedTotalCapacity());
        addExcelRow(sb, "Audit Pass Rate (%):", String.format(Locale.US, "%.2f%%", s.passRatePercent()), "Successful Sanitizations:", String.valueOf(s.successCount()));
        addExcelRow(sb, "Failed Sanitizations:", String.valueOf(s.failureCount()), "Avg Shannon Residual Entropy:", String.format(Locale.US, "%.4f bits/byte (0.000%% Target)", s.averageResidualEntropy()));
        addExcelRow(sb, "E-Waste Diverted from Landfill:", String.format(Locale.US, "%.1f kg", s.esgImpact().eWasteDivertedKg()), "Scope 3 CO2 Mitigated:", String.format(Locale.US, "%.1f kg CO2e", s.esgImpact().co2EmissionsSavedKg()));
        addExcelRow(sb, "Tree Seedlings Equivalency:", String.format(Locale.US, "%.2f trees", s.esgImpact().treesEquivalent()), "Disposal Route:", "100% Circular Economy Reuse");
        addExcelRow(sb, "Earliest Audit Entry:", s.earliestRecordTimestamp(), "Latest Audit Entry:", s.latestRecordTimestamp());

        sb.append("  </Table>\n");
        sb.append(" </Worksheet>\n");

        // ── Worksheet 2: Detailed Sanitization Logs ──
        sb.append(" <Worksheet ss:Name=\"Detailed Sanitization Logs\">\n");
        sb.append("  <Table ss:DefaultRowHeight=\"18\">\n");
        sb.append("   <Column ss:Width=\"45\"/>\n");  // ID
        sb.append("   <Column ss:Width=\"130\"/>\n"); // Timestamp
        sb.append("   <Column ss:Width=\"180\"/>\n"); // Model
        sb.append("   <Column ss:Width=\"140\"/>\n"); // Serial
        sb.append("   <Column ss:Width=\"75\"/>\n");  // Capacity
        sb.append("   <Column ss:Width=\"180\"/>\n"); // Standard
        sb.append("   <Column ss:Width=\"80\"/>\n");  // Status
        sb.append("   <Column ss:Width=\"110\"/>\n"); // Pre/Post Score
        sb.append("   <Column ss:Width=\"180\"/>\n"); // S.M.A.R.T. Delta
        sb.append("   <Column ss:Width=\"75\"/>\n");  // Peak Temp
        sb.append("   <Column ss:Width=\"110\"/>\n"); // Interface
        sb.append("   <Column ss:Width=\"160\"/>\n"); // Verification
        sb.append("   <Column ss:Width=\"100\"/>\n"); // Entropy
        sb.append("   <Column ss:Width=\"220\"/>\n"); // Signature

        // Header Row
        sb.append("   <Row ss:Height=\"24\">\n");
        for (String h : List.of("ID", "Timestamp", "Drive Model", "Serial Number", "Capacity", "Sanitization Standard", "Status", "Health Delta", "SMART Integrity Delta", "Peak Temp", "Interface Telemetry", "Verification Attestation", "Entropy (bits/B)", "RSA Signature Digest")) {
            sb.append("    <Cell ss:StyleID=\"TableHeader\"><Data ss:Type=\"String\">").append(xmlEscape(h)).append("</Data></Cell>\n");
        }
        sb.append("   </Row>\n");

        if (records != null) {
            for (AuditDb.AuditRecord r : records) {
                boolean isPass = "SUCCESS".equalsIgnoreCase(r.status());
                String statusStyle = isPass ? "StatusPass" : "StatusFail";

                sb.append("   <Row>\n");
                sb.append("    <Cell><Data ss:Type=\"Number\">").append(r.id()).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.timestamp())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.driveModel())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.serialNumber())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.capacity())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.wipeStandard())).append("</Data></Cell>\n");
                sb.append("    <Cell ss:StyleID=\"").append(statusStyle).append("\"><Data ss:Type=\"String\">").append(xmlEscape(r.status())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(r.preHealthScore()).append(" -> ").append(r.postHealthScore()).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.smartDeltaSummary())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(r.peakTempCelsius()).append(" °C</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.interfaceAnomalySummary())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"String\">").append(xmlEscape(r.verificationStatus())).append("</Data></Cell>\n");
                sb.append("    <Cell><Data ss:Type=\"Number\">").append(String.format(Locale.US, "%.4f", r.entropyScore())).append("</Data></Cell>\n");
                sb.append("    <Cell ss:StyleID=\"MonoCode\"><Data ss:Type=\"String\">").append(xmlEscape(r.digitalSignature())).append("</Data></Cell>\n");
                sb.append("   </Row>\n");
            }
        }

        sb.append("  </Table>\n");
        sb.append(" </Worksheet>\n");

        sb.append("</Workbook>\n");
        return sb.toString();
    }

    public static void exportToExcelXml(List<AuditDb.AuditRecord> records, File targetFile) throws IOException {
        String xml = generateExcelXmlContent(records);
        Files.writeString(targetFile.toPath(), xml, StandardCharsets.UTF_8);
        AppLogger.info(MODULE, "Exported " + (records != null ? records.size() : 0) + " audit records to Excel XML: " + targetFile.getAbsolutePath());
    }

    // ── Helper Utilities ─────────────────────────────────────────────

    private static void addExcelRow(StringBuilder sb, String k1, String v1, String k2, String v2) {
        sb.append("   <Row>\n");
        sb.append("    <Cell ss:StyleID=\"KpiHeader\"><Data ss:Type=\"String\">").append(xmlEscape(k1)).append("</Data></Cell>\n");
        sb.append("    <Cell ss:StyleID=\"KpiValue\"><Data ss:Type=\"String\">").append(xmlEscape(v1)).append("</Data></Cell>\n");
        sb.append("    <Cell ss:StyleID=\"KpiHeader\"><Data ss:Type=\"String\">").append(xmlEscape(k2)).append("</Data></Cell>\n");
        sb.append("    <Cell ss:StyleID=\"KpiValue\"><Data ss:Type=\"String\">").append(xmlEscape(v2)).append("</Data></Cell>\n");
        sb.append("   </Row>\n");
    }

    private static String csvEscape(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"").replace("\n", " ").replace("\r", "") + "\"";
    }

    private static String xmlEscape(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    private static long parseCapacityToBytes(String capStr) {
        if (capStr == null || capStr.isBlank()) return 0;
        String clean = capStr.trim().toUpperCase();
        try {
            if (clean.endsWith("TB")) {
                double v = Double.parseDouble(clean.replace("TB", "").trim());
                return (long) (v * 1024L * 1024L * 1024L * 1024L);
            } else if (clean.endsWith("GB")) {
                double v = Double.parseDouble(clean.replace("GB", "").trim());
                return (long) (v * 1024L * 1024L * 1024L);
            } else if (clean.endsWith("MB")) {
                double v = Double.parseDouble(clean.replace("MB", "").trim());
                return (long) (v * 1024L * 1024L);
            } else if (clean.endsWith("KB")) {
                double v = Double.parseDouble(clean.replace("KB", "").trim());
                return (long) (v * 1024L);
            } else if (clean.endsWith("B")) {
                return Long.parseLong(clean.replace("B", "").trim());
            } else {
                double v = Double.parseDouble(clean);
                return (long) (v * 1024L * 1024L * 1024L); // default to GB
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.2f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else if (bytes >= 1024L * 1024L) {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return bytes + " Bytes";
        }
    }
}
