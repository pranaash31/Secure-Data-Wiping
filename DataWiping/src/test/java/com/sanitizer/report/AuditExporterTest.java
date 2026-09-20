package com.sanitizer.report;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanitizer.db.AuditDb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Multi-Format Audit Data Exporter Tests")
class AuditExporterTest {

    private AuditDb.AuditRecord createSampleRecord(int id, String model, String serial, String capacity, String standard, String status, double entropy) {
        return new AuditDb.AuditRecord(
                id,
                "2026-09-20T10:15:30Z",
                model,
                serial,
                capacity,
                standard,
                status,
                "RSA_MOCK_SIGNATURE_PAYLOAD_BASE64",
                98,
                98,
                0,
                0,
                "Normal",
                34,
                0,
                0,
                "Nominal",
                "VERIFIED_100_PERCENT",
                1000000L,
                entropy,
                "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
    }

    @Test
    @DisplayName("Empty record list generates safe zeroed compliance summary")
    void testEmptyComplianceSummary() {
        AuditExporter.ComplianceSummary summary = AuditExporter.generateComplianceSummary(List.of());
        assertThat(summary).isNotNull();
        assertThat(summary.totalDrivesProcessed()).isEqualTo(0);
        assertThat(summary.successCount()).isEqualTo(0);
        assertThat(summary.failureCount()).isEqualTo(0);
        assertThat(summary.passRatePercent()).isEqualTo(100.0);
        assertThat(summary.totalCapacityBytes()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Populated record list accurately calculates executive compliance KPIs")
    void testPopulatedComplianceSummary() {
        AuditDb.AuditRecord r1 = createSampleRecord(1, "Samsung 980 Pro", "S980-001", "500 GB", "NIST SP 800-88", "SUCCESS", 0.0000);
        AuditDb.AuditRecord r2 = createSampleRecord(2, "WD Black SN850X", "WDB-002", "1 TB", "DoD 5220.22-M", "SUCCESS", 0.0001);
        AuditDb.AuditRecord r3 = createSampleRecord(3, "Seagate Barracuda", "ST-003", "2 TB", "British HMG IS5", "FAILED", 0.8500);

        List<AuditDb.AuditRecord> records = List.of(r1, r2, r3);
        AuditExporter.ComplianceSummary summary = AuditExporter.generateComplianceSummary(records);

        assertThat(summary.totalDrivesProcessed()).isEqualTo(3);
        assertThat(summary.successCount()).isEqualTo(2);
        assertThat(summary.failureCount()).isEqualTo(1);
        assertThat(summary.passRatePercent()).isCloseTo(66.666, org.assertj.core.data.Offset.offset(0.1));
        assertThat(summary.totalCapacityBytes()).isEqualTo((500L + 1024L + 2048L) * 1024L * 1024L * 1024L);
        assertThat(summary.formattedTotalCapacity()).isEqualTo("3.49 TB");
        assertThat(summary.averageResidualEntropy()).isCloseTo((0.0000 + 0.0001 + 0.8500) / 3.0, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(summary.esgImpact()).isNotNull();
        assertThat(summary.esgImpact().eWasteDivertedKg()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("CSV export generates valid RFC 4180 format with executive summary header")
    void testCsvExportGeneration(@TempDir Path tempDir) throws IOException {
        AuditDb.AuditRecord r1 = createSampleRecord(1, "Crucial P5 Plus, NVMe", "CP5-999", "1 TB", "NIST SP 800-88", "SUCCESS", 0.0000);
        List<AuditDb.AuditRecord> records = List.of(r1);

        String csv = AuditExporter.generateCsvContent(records);

        assertThat(csv).contains("# SECURE DATA WIPING ENTERPRISE AUDIT TRAIL REPORT");
        assertThat(csv).contains("# Total Assets Processed: 1 devices");
        assertThat(csv).contains("Audit_ID,Timestamp,Device_Model,Serial_Number");
        assertThat(csv).contains("\"Crucial P5 Plus, NVMe\"");
        assertThat(csv).contains("CP5-999");
        assertThat(csv).contains("VERIFIED_100_PERCENT");

        File csvFile = tempDir.resolve("audit_export.csv").toFile();
        AuditExporter.exportToCsv(records, csvFile);
        assertThat(csvFile).exists();
        String fileContent = Files.readString(csvFile.toPath());
        assertThat(fileContent).contains("# SECURE DATA WIPING ENTERPRISE AUDIT TRAIL REPORT");
        assertThat(fileContent).contains("\"Crucial P5 Plus, NVMe\"");
        assertThat(fileContent).contains("CP5-999");
    }

    @Test
    @DisplayName("JSON export generates structured, valid JSON with regulatory standards and ESG summary")
    void testJsonExportGeneration(@TempDir Path tempDir) throws IOException {
        AuditDb.AuditRecord r1 = createSampleRecord(101, "Kingston KC3000", "KC-777", "2 TB", "German BSI-VS", "SUCCESS", 0.0000);
        List<AuditDb.AuditRecord> records = List.of(r1);

        String json = AuditExporter.generateJsonContent(records);
        assertThat(json).isNotBlank();

        // Validate JSON parsing
        JsonElement root = JsonParser.parseString(json);
        assertThat(root.isJsonObject()).isTrue();
        JsonObject rootObj = root.getAsJsonObject();

        assertThat(rootObj.has("metadata")).isTrue();
        assertThat(rootObj.getAsJsonObject("metadata").get("software").getAsString()).contains("SecureErase");
        assertThat(rootObj.getAsJsonObject("metadata").getAsJsonArray("complianceStandards").size()).isGreaterThanOrEqualTo(5);

        assertThat(rootObj.has("executiveSummary")).isTrue();
        JsonObject exec = rootObj.getAsJsonObject("executiveSummary");
        assertThat(exec.get("totalAssetsProcessed").getAsInt()).isEqualTo(1);
        assertThat(exec.get("passRatePercent").getAsDouble()).isEqualTo(100.0);

        assertThat(rootObj.has("auditRecords")).isTrue();
        assertThat(rootObj.getAsJsonArray("auditRecords").size()).isEqualTo(1);

        File jsonFile = tempDir.resolve("audit_export.json").toFile();
        AuditExporter.exportToJson(records, jsonFile);
        assertThat(jsonFile).exists();
    }

    @Test
    @DisplayName("Excel SpreadsheetML XML generates multi-worksheet spreadsheet")
    void testExcelXmlExportGeneration(@TempDir Path tempDir) throws IOException {
        AuditDb.AuditRecord r1 = createSampleRecord(201, "Micron 7450 Pro", "MIC-444", "960 GB", "Canadian RCMP TSSIT", "SUCCESS", 0.0000);
        List<AuditDb.AuditRecord> records = List.of(r1);

        String excelXml = AuditExporter.generateExcelXmlContent(records);

        assertThat(excelXml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(excelXml).contains("xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
        assertThat(excelXml).contains("ss:Name=\"Executive Audit Summary\"");
        assertThat(excelXml).contains("ss:Name=\"Detailed Sanitization Logs\"");
        assertThat(excelXml).contains("Micron 7450 Pro");
        assertThat(excelXml).contains("MIC-444");
        assertThat(excelXml).contains("Canadian RCMP TSSIT");
        assertThat(excelXml).contains("</Workbook>");

        File xmlFile = tempDir.resolve("audit_export.xml").toFile();
        AuditExporter.exportToExcelXml(records, xmlFile);
        assertThat(xmlFile).exists();
    }
}
