package com.sanitizer.service;

import com.sanitizer.audit.SecurityAuditLogger.SecurityLedgerVerificationResult;
import com.sanitizer.db.AuditDb.AuditRecord;
import com.sanitizer.db.LedgerIntegrityEngine.LedgerVerificationResult;
import com.sanitizer.session.SessionContext;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract for enterprise audit exports, compliance analytics,
 * cryptographic verification, and SIEM activity logging.
 */
public interface AuditService {

    /**
     * Asynchronously exports audit records into the specified format (CSV, JSON, EXCEL)
     * and logs the export action to SIEM.
     */
    CompletableFuture<ExportResult> exportBatchAsync(
            List<AuditRecord> records,
            File targetFile,
            ExportFormat format,
            SessionContext session
    );

    /**
     * Asynchronously exports a full corporate audit bundle (CSV, JSON, Excel) into a target folder
     * and logs the export action to SIEM.
     */
    CompletableFuture<ExportResult> exportFullPackageAsync(
            List<AuditRecord> records,
            File targetDir,
            SessionContext session
    );

    /**
     * Asynchronously generates a PDF sanitization certificate for a given audit record
     * and logs the action to SIEM.
     */
    CompletableFuture<ExportResult> exportCertificatePdfAsync(
            AuditRecord record,
            SessionContext session
    );

    /**
     * Asynchronously assesses hardware failure, generates a DIN 66399 physical destruction order PDF,
     * and logs the action to SIEM.
     */
    CompletableFuture<ExportResult> exportQuarantineReportPdfAsync(
            AuditRecord record,
            SessionContext session
    );

    /**
     * Generates a formatted ESG Sustainability & Scope 3 carbon mitigation report string
     * and logs the report access to SIEM.
     */
    String generateEsgReport(List<AuditRecord> records, SessionContext session);

    /**
     * Generates a consolidated Executive Compliance & Sanitization KPI summary string.
     */
    String generateComplianceSummaryText(List<AuditRecord> records);

    /**
     * Formats an inline summary string for the ESG impact banner based on active audit records.
     */
    String formatEsgBannerText(List<AuditRecord> records);

    /**
     * Validates the SHA-256 block chain integrity of the SQLite audit ledger
     * and records the verification event to SIEM.
     */
    LedgerVerificationResult verifyDatabaseLedger(SessionContext session);

    /**
     * Validates the SHA-256 block chain integrity of the security activity audit trail.
     */
    SecurityLedgerVerificationResult verifySecurityLedger();

    /**
     * Cryptographically verifies the RSA-2048 digital signature of an audit record against its payload.
     */
    boolean verifyRecordSignature(AuditRecord record);
}
