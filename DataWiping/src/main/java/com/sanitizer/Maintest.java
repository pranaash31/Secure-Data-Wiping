package com.sanitizer;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.util.AppLogger;

import java.util.List;

/**
 * Maintest — Command-line Integration Verification Entry Point.
 * <p>
 * This class provides a standalone terminal test suite for validating low-level
 * block wiping, RSA-2048 signing, SQLite persistence, and PDF certificate generation
 * without launching the JavaFX GUI runtime.
 */
@SuppressWarnings("unused")
public class Maintest {

    private static final String MODULE = "Maintest";

    public static void main(String[] args) {
        AppLogger.info(MODULE, "=== MODULE 1 & 2: DEFENSE WIPING & CRYPTOGRAPHIC AUDIT TEST ===");

        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        if (drives.isEmpty()) {
            AppLogger.warn(MODULE, "No USB pen drives detected. Insert a pen drive and re-run.");
            return;
        }

        UsbDetector.UsbDriveInfo target = drives.get(0);
        AppLogger.info(MODULE, "Targeting drive: " + target.model());

        // 1. Execute Wipe
        boolean result = WipeEngine.executeWipe(
                target.systemPath(),
                target.sizeBytes(),
                WipeEngine.WipeStandard.DOD_5220_22_M,
                percent -> System.out.printf("\rOverall Progress: %.2f%%", percent)
        );

        if (result) {
            AppLogger.info(MODULE, "[1/3] Wipe Completed Successfully.");

            // 2. Generate RSA Digital Signature
            String auditPayload = target.model() + "|" + target.serial() + "|" + target.formattedSize() + "|DOD_5220_22_M|SUCCESS";
            String signature = CryptoSigner.signData(auditPayload);
            AppLogger.info(MODULE, "[2/3] RSA-2048 Signature Generated: " + signature.substring(0, Math.min(30, signature.length())) + "...");

            // 3. Persist into SQLite Audit DB
            boolean dbSaved = AuditDb.saveRecord(
                    target.model(),
                    target.serial(),
                    target.formattedSize(),
                    "DoD 5220.22-M",
                    "SUCCESS",
                    signature
            );

            if (dbSaved) {
                AppLogger.info(MODULE, "[3/3] Audit Record Persisted into SQLite Database!");
                List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
                if (!records.isEmpty()) {
                    CertificateGenerator.generateCertificate(records.get(0));
                }
                AppLogger.info(MODULE, "--- Current SQLite Audit History ---");
                for (AuditDb.AuditRecord record : records) {
                    AppLogger.info(MODULE, "ID: " + record.id() + " | Time: " + record.timestamp() +
                            " | Model: " + record.driveModel() + " | Serial: " + record.serialNumber() +
                            " | Status: " + record.status());
                }
            }
        }
    }
}