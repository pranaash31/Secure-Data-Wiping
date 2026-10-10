package com.sanitizer.service;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.db.AuditDb.AuditRecord;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class AuditExportServiceTest {

    private File tempDbFile;
    private AuditService auditService;
    private SessionContext session;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = File.createTempFile("test_audit_service_", ".db");
        AuditDb.setDbUrlForTesting("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        auditService = AuditExportService.getInstance();
        session = new SessionContext("Test Auditor", "AGY-SEC-99", UserRole.CHIEF_AUDITOR);
    }

    @AfterEach
    void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private AuditRecord createSampleRecord(int id, String model, String serial, String status) {
        String payload = model + "|" + serial + "|64.0 GB|NIST SP 800-88 Rev. 1 Clear|" + status;
        String sig = CryptoSigner.signData(payload);
        return new AuditRecord(
                id,
                "2026-10-10 14:00:00",
                model,
                serial,
                "64.0 GB",
                "NIST SP 800-88 Rev. 1 Clear",
                status,
                sig
        );
    }

    @Test
    void testExportBatchAsyncCsvAndJson(@TempDir Path tempDir) throws ExecutionException, InterruptedException, IOException {
        AuditRecord rec1 = createSampleRecord(1, "Kingston DataTraveler", "KT-12345", "SUCCESS");
        AuditRecord rec2 = createSampleRecord(2, "Samsung Bar Plus", "SB-67890", "SUCCESS");
        List<AuditRecord> records = List.of(rec1, rec2);

        // Test CSV
        File csvFile = tempDir.resolve("audit_export.csv").toFile();
        ExportResult csvResult = auditService.exportBatchAsync(records, csvFile, ExportFormat.CSV, session).get();

        assertThat(csvResult.success()).isTrue();
        assertThat(csvFile).exists();
        String csvContent = Files.readString(csvFile.toPath());
        assertThat(csvContent).contains("Kingston DataTraveler").contains("Samsung Bar Plus");

        // Test JSON
        File jsonFile = tempDir.resolve("audit_export.json").toFile();
        ExportResult jsonResult = auditService.exportBatchAsync(records, jsonFile, ExportFormat.JSON, session).get();

        assertThat(jsonResult.success()).isTrue();
        assertThat(jsonFile).exists();
        String jsonContent = Files.readString(jsonFile.toPath());
        assertThat(jsonContent).contains("Kingston DataTraveler");
    }

    @Test
    void testExportFullPackageAsync(@TempDir Path tempDir) throws ExecutionException, InterruptedException {
        AuditRecord rec = createSampleRecord(1, "Crucial X6 1TB", "CR-9900", "SUCCESS");
        List<AuditRecord> records = List.of(rec);

        ExportResult pkgResult = auditService.exportFullPackageAsync(records, tempDir.toFile(), session).get();

        assertThat(pkgResult.success()).isTrue();
        assertThat(pkgResult.exportedFiles()).hasSize(3);

        boolean hasCsv = pkgResult.exportedFiles().stream().anyMatch(f -> f.getName().endsWith(".csv"));
        boolean hasJson = pkgResult.exportedFiles().stream().anyMatch(f -> f.getName().endsWith(".json"));
        boolean hasXml = pkgResult.exportedFiles().stream().anyMatch(f -> f.getName().endsWith(".xml"));

        assertThat(hasCsv).isTrue();
        assertThat(hasJson).isTrue();
        assertThat(hasXml).isTrue();
    }

    @Test
    void testGenerateEsgReportAndComplianceKPIs() {
        AuditRecord rec1 = createSampleRecord(1, "Seagate One Touch", "SG-111", "SUCCESS");
        AuditRecord rec2 = createSampleRecord(2, "WD My Passport", "WD-222", "SUCCESS");
        List<AuditRecord> records = List.of(rec1, rec2);

        String esgReport = auditService.generateEsgReport(records, session);
        assertThat(esgReport)
                .contains("ENTERPRISE ESG SUSTAINABILITY & CIRCULAR ECONOMY AUDIT")
                .contains("ISO 14064-1")
                .contains("E-Waste Diverted from Landfills");

        String complianceSummary = auditService.generateComplianceSummaryText(records);
        assertThat(complianceSummary)
                .contains("CONSOLIDATED ENTERPRISE SANITIZATION COMPLIANCE KPIS")
                .contains("NIST SP 800-88 R1")
                .contains("Total Storage Assets Processed: 2 devices");

        String bannerText = auditService.formatEsgBannerText(records);
        assertThat(bannerText)
                .contains("Corporate ESG Impact")
                .contains("across 2 sanitized devices");
    }

    @Test
    void testVerifyLedgersAndSignatures() {
        AuditRecord validRec = createSampleRecord(1, "SanDisk Ultra", "SD-333", "SUCCESS");
        boolean isValidSig = auditService.verifyRecordSignature(validRec);
        assertThat(isValidSig).isTrue();

        AuditRecord tamperedRec = new AuditRecord(
                2, "2026-10-10 12:00:00", "Tampered Model", "FAKE-SN", "1TB",
                "DoD 5220.22-M", "SUCCESS", "INVALID_SIG"
        );
        boolean isTamperedValid = auditService.verifyRecordSignature(tamperedRec);
        assertThat(isTamperedValid).isFalse();

        var dbLedgerResult = auditService.verifyDatabaseLedger(session);
        assertThat(dbLedgerResult).isNotNull();

        var secLedgerResult = auditService.verifySecurityLedger();
        assertThat(secLedgerResult).isNotNull();
    }

    @Test
    void testExportCertificateAndQuarantineReport() throws ExecutionException, InterruptedException {
        AuditRecord rec = createSampleRecord(10, "Corsair Flash Voyager", "CV-555", "SUCCESS");

        // Certificate generation
        ExportResult certResult = auditService.exportCertificatePdfAsync(rec, session).get();
        assertThat(certResult.success()).isTrue();
        assertThat(certResult.primaryFilePath()).isNotNull().endsWith(".pdf");

        // Quarantine order generation
        ExportResult qResult = auditService.exportQuarantineReportPdfAsync(rec, session).get();
        assertThat(qResult.success()).isTrue();
        assertThat(qResult.primaryFilePath()).isNotNull().endsWith(".pdf");

        // Null record handling
        ExportResult nullCert = auditService.exportCertificatePdfAsync(null, session).get();
        assertThat(nullCert.success()).isFalse();

        ExportResult nullQ = auditService.exportQuarantineReportPdfAsync(null, session).get();
        assertThat(nullQ.success()).isFalse();
    }
}
