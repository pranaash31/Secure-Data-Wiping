package com.sanitizer.service;

import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.audit.SecurityAuditLogger.SecurityLedgerVerificationResult;
import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.db.AuditDb.AuditRecord;
import com.sanitizer.db.LedgerIntegrityEngine.LedgerVerificationResult;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.quarantine.QuarantineEngine;
import com.sanitizer.quarantine.QuarantineRecord;
import com.sanitizer.quarantine.QuarantineReportGenerator;
import com.sanitizer.report.AuditExporter;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete application service orchestrating enterprise audit exports, compliance analytics,
 * cryptographic signature validation, and SIEM security logging.
 */
public class AuditExportService implements AuditService {

    private static final String MODULE = "AuditExportService";
    private static final DateTimeFormatter FILE_TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault());

    private final ExecutorService executor;

    /**
     * Thread-safe Initialization-on-Demand Holder Singleton.
     */
    private static class InstanceHolder {
        private static final AuditExportService INSTANCE = new AuditExportService();
    }

    public static AuditExportService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public AuditExportService() {
        AtomicInteger threadCount = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "audit-export-worker-" + threadCount.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public AuditExportService(ExecutorService executor) {
        this.executor = executor != null ? executor : Executors.newCachedThreadPool();
    }

    private SessionContext resolveSession(SessionContext session) {
        return session != null ? session : new SessionContext("System Auditor", "GOV-DEF-8942", UserRole.INSPECTOR);
    }

    @Override
    public CompletableFuture<ExportResult> exportBatchAsync(
            List<AuditRecord> records,
            File targetFile,
            ExportFormat format,
            SessionContext session
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (targetFile == null) {
                    return ExportResult.failure("Target file cannot be null", new IllegalArgumentException("Target file is null"));
                }
                List<AuditRecord> exportList = (records != null && !records.isEmpty()) ? records : AuditDb.getAllRecords();
                if (exportList.isEmpty()) {
                    return ExportResult.failure("No audit records found to export", new IllegalStateException("Empty records"));
                }

                ExportFormat fmt = (format != null) ? format : ExportFormat.CSV;
                switch (fmt) {
                    case CSV -> AuditExporter.exportToCsv(exportList, targetFile);
                    case JSON -> AuditExporter.exportToJson(exportList, targetFile);
                    case EXCEL -> AuditExporter.exportToExcelXml(exportList, targetFile);
                }

                SessionContext ctx = resolveSession(session);
                SecurityAuditLogger.logExport(
                        ctx.officerName(), ctx.agencyId(), ctx.role(),
                        fmt.getLabel() + " Batch Export", targetFile.getAbsolutePath()
                );

                AppLogger.info(MODULE, String.format("Successfully exported %d records to %s (%s)",
                        exportList.size(), targetFile.getName(), fmt.getLabel()));

                return ExportResult.success(
                        String.format("Exported %d records to: %s", exportList.size(), targetFile.getName()),
                        targetFile
                );
            } catch (Exception e) {
                AppLogger.error(MODULE, "Batch export failed: " + e.getMessage(), e);
                return ExportResult.failure("Export Failed: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ExportResult> exportFullPackageAsync(
            List<AuditRecord> records,
            File targetDir,
            SessionContext session
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (targetDir == null || !targetDir.exists()) {
                    return ExportResult.failure("Target directory does not exist", new IllegalArgumentException("Invalid directory"));
                }
                List<AuditRecord> exportList = (records != null && !records.isEmpty()) ? records : AuditDb.getAllRecords();
                if (exportList.isEmpty()) {
                    return ExportResult.failure("No audit records found to export", new IllegalStateException("Empty records"));
                }

                String timestamp = FILE_TIMESTAMP_FMT.format(Instant.now());
                File csvFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".csv");
                File jsonFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".json");
                File excelFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".xml");

                AuditExporter.exportToCsv(exportList, csvFile);
                AuditExporter.exportToJson(exportList, jsonFile);
                AuditExporter.exportToExcelXml(exportList, excelFile);

                SessionContext ctx = resolveSession(session);
                SecurityAuditLogger.logExport(
                        ctx.officerName(), ctx.agencyId(), ctx.role(),
                        "Full Audit Package", targetDir.getAbsolutePath()
                );

                AppLogger.info(MODULE, "Exported full 3-format corporate audit package into: " + targetDir.getAbsolutePath());

                return ExportResult.success(
                        "Generated CSV, JSON, and Excel reports in " + targetDir.getName(),
                        List.of(csvFile, jsonFile, excelFile)
                );
            } catch (Exception e) {
                AppLogger.error(MODULE, "Full package export failed: " + e.getMessage(), e);
                return ExportResult.failure("Package Export Failed: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ExportResult> exportCertificatePdfAsync(
            AuditRecord record,
            SessionContext session
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (record == null) {
                    return ExportResult.failure("Select an audit log entry from the table first", new IllegalArgumentException("No record selected"));
                }

                String pdfPath = CertificateGenerator.generateCertificate(record);
                if (pdfPath != null) {
                    SessionContext ctx = resolveSession(session);
                    SecurityAuditLogger.logExport(
                            ctx.officerName(), ctx.agencyId(), ctx.role(),
                            "PDF Sanitization Certificate", pdfPath
                    );
                    AppLogger.info(MODULE, "Generated PDF Sanitization Certificate at: " + pdfPath);
                    return ExportResult.success("Certificate generated at: " + pdfPath, pdfPath);
                } else {
                    return ExportResult.failure("Failed to generate PDF Certificate.", new RuntimeException("Generator returned null"));
                }
            } catch (Exception e) {
                AppLogger.error(MODULE, "PDF certificate export failed: " + e.getMessage(), e);
                return ExportResult.failure("PDF Export Error: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<ExportResult> exportQuarantineReportPdfAsync(
            AuditRecord record,
            SessionContext session
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (record == null) {
                    return ExportResult.failure("Select an audit record to generate Quarantine Report", new IllegalArgumentException("No record selected"));
                }

                QuarantineRecord qRecord = QuarantineEngine.assessHardwareFailure(
                        record.driveModel(),
                        record.serialNumber(),
                        record.capacity(),
                        null,
                        record.wipeStandard(),
                        "Defective Blocks / Non-Compliant Sanitization Attempt",
                        List.of(),
                        record.preHealthScore(),
                        record.postHealthScore(),
                        record.smartDeltaSummary()
                );

                String qPath = QuarantineReportGenerator.generatePdfReport(qRecord);
                if (qPath != null) {
                    SessionContext ctx = resolveSession(session);
                    SecurityAuditLogger.logExport(
                            ctx.officerName(), ctx.agencyId(), ctx.role(),
                            "Quarantine Destruction Order", qPath
                    );
                    AppLogger.info(MODULE, "Generated DIN 66399 Quarantine Destruction Order at: " + qPath);
                    return ExportResult.success("Order generated at: " + qPath, qPath);
                } else {
                    return ExportResult.failure("Failed to generate Quarantine Order PDF.", new RuntimeException("Generator returned null"));
                }
            } catch (Exception e) {
                AppLogger.error(MODULE, "Quarantine report export failed: " + e.getMessage(), e);
                return ExportResult.failure("Quarantine Report Error: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public String generateEsgReport(List<AuditRecord> records, SessionContext session) {
        List<AuditRecord> list = (records != null && !records.isEmpty()) ? records : AuditDb.getAllRecords();
        EsgCalculator.EsgMetrics esg = EsgCalculator.calculateAggregate(list);

        String report = String.format(
                java.util.Locale.US,
                """
                =======================================================
                🌱 ENTERPRISE ESG SUSTAINABILITY & CIRCULAR ECONOMY AUDIT
                Standard: ISO 14064-1 / GHG Protocol Scope 3 Compliant
                =======================================================
                Total Sanitized Assets for Circular Reuse: %d devices
                Total Storage Capacity Sanitized: %s
                -------------------------------------------------------
                E-Waste Diverted from Landfills: %.1f kg
                Scope 3 CO₂ Emissions Mitigated: %.1f kg CO₂e
                10-Year Tree Seedlings Equivalent: %.2f trees
                Manufacturing Power Conserved: %.1f kWh
                Circular Economy Disposal Index: 100%% Zero-Landfill
                -------------------------------------------------------
                Executive Compliance Proof:
                %s
                =======================================================
                """,
                list.size(),
                EsgCalculator.formatGb(esg.capacityGb()),
                esg.eWasteDivertedKg(),
                esg.co2EmissionsSavedKg(),
                esg.treesEquivalent(),
                esg.energySavedKwh(),
                esg.impactStatement()
        );

        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logExport(
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                "ESG Corporate Sustainability Report", "In-App Dialog"
        );
        return report;
    }

    @Override
    public String generateComplianceSummaryText(List<AuditRecord> records) {
        List<AuditRecord> list = (records != null && !records.isEmpty()) ? records : AuditDb.getAllRecords();
        AuditExporter.ComplianceSummary s = AuditExporter.generateComplianceSummary(list);

        return String.format(
                java.util.Locale.US,
                """
                ══════════════════════════════════════════════════════════════
                📊 CONSOLIDATED ENTERPRISE SANITIZATION COMPLIANCE KPIS
                Standard: NIST SP 800-88 R1 / ISO 27040 / EU GDPR Art. 17
                ══════════════════════════════════════════════════════════════
                • Total Storage Assets Processed: %d devices
                • Successful Sanitizations:       %d devices (%.2f%%)
                • Failed Sanitization Attempts:   %d devices
                • Total Data Volume Sanitized:    %s (%d bytes)
                • Average Residual Shannon Score: %.4f bits/byte (Target: 0.000)
                • Earliest Logged Audit Entry:    %s
                • Latest Logged Audit Entry:      %s
                ──────────────────────────────────────────────────────────────
                🌱 Corporate ESG & Scope 3 Sustainability Impact:
                • E-Waste Diverted from Landfills: %.1f kg
                • Scope 3 CO₂ Emissions Mitigated: %.1f kg CO₂e
                • 10-Year Tree Seedlings Saved:    %.2f trees
                • Manufacturing Power Conserved:   %.1f kWh
                • Circular Disposition Index:      100%% Zero-Landfill E-Waste
                ══════════════════════════════════════════════════════════════
                """,
                s.totalDrivesProcessed(),
                s.successCount(),
                s.passRatePercent(),
                s.failureCount(),
                s.formattedTotalCapacity(),
                s.totalCapacityBytes(),
                s.averageResidualEntropy(),
                s.earliestRecordTimestamp(),
                s.latestRecordTimestamp(),
                s.esgImpact().eWasteDivertedKg(),
                s.esgImpact().co2EmissionsSavedKg(),
                s.esgImpact().treesEquivalent(),
                s.esgImpact().energySavedKwh()
        );
    }

    @Override
    public String formatEsgBannerText(List<AuditRecord> records) {
        List<AuditRecord> list = (records != null) ? records : Collections.emptyList();
        EsgCalculator.EsgMetrics esg = EsgCalculator.calculateAggregate(list);
        return String.format(
                java.util.Locale.US,
                "🌱 Corporate ESG Impact: %.1f kg E-Waste Diverted | %.1f kg CO₂ Mitigated (~%.2f Tree Seedlings Equiv.) across %d sanitized devices",
                esg.eWasteDivertedKg(),
                esg.co2EmissionsSavedKg(),
                esg.treesEquivalent(),
                list.size()
        );
    }

    @Override
    public LedgerVerificationResult verifyDatabaseLedger(SessionContext session) {
        var result = AuditDb.verifyDatabaseIntegrity();
        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logEvent(
                SecurityAuditLogger.EVENT_LEDGER_VERIFIED,
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                "Sanitization DB Ledger Verification: " + (result.isFullyValid() ? "PASSED (100% Tamper Proof)" : "FAILED"),
                "sanitizer_history.db",
                result.isFullyValid() ? "SUCCESS" : "FAILED"
        );
        return result;
    }

    @Override
    public SecurityLedgerVerificationResult verifySecurityLedger() {
        return SecurityAuditLogger.verifySecurityLedgerIntegrity();
    }

    @Override
    public boolean verifyRecordSignature(AuditRecord record) {
        if (record == null || record.digitalSignature() == null || record.digitalSignature().isBlank()) {
            return false;
        }
        String payload = record.driveModel() + "|" + record.serialNumber() + "|" + record.capacity() + "|" + record.wipeStandard() + "|" + record.status();
        return CryptoSigner.verifySignature(payload, record.digitalSignature());
    }
}
