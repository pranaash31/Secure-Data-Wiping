package com.sanitizer;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;

import java.util.List;

public class Maintest {
    public static void main(String[] args) {
        System.out.println("=== MODULE 1 & 2: DEFENSE WIPING & CRYPTOGRAPHIC AUDIT TEST ===");

        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        if (drives.isEmpty()) {
            System.out.println("No USB pen drives detected. Insert a pen drive and re-run.");
            return;
        }

        UsbDetector.UsbDriveInfo target = drives.get(0);
        System.out.println("\nTargeting drive: " + target.model());

        // 1. Execute Wipe
        boolean result = WipeEngine.executeWipe(
                target.systemPath(),
                target.sizeBytes(),
                WipeEngine.WipeStandard.DOD_5220_22_M,
                percent -> System.out.printf("\rOverall Progress: %.2f%%", percent)
        );

        if (result) {
            System.out.println("\n\n[1/3] Wipe Completed Successfully.");

            // 2. Generate RSA Digital Signature
            String auditPayload = target.model() + "|" + target.serial() + "|" + target.formattedSize() + "|DOD_5220_22_M|SUCCESS";
            String signature = CryptoSigner.signData(auditPayload);
            System.out.println("[2/3] RSA-2048 Signature Generated: " + signature.substring(0, 30) + "...");

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
                System.out.println("[3/3] Audit Record Persisted into SQLite Database!");
                System.out.println("\n--- Current SQLite Audit History ---");
                for (AuditDb.AuditRecord record : AuditDb.getAllRecords()) {
                    System.out.println("ID: " + record.id() + " | Time: " + record.timestamp() +
                            " | Model: " + record.driveModel() + " | Serial: " + record.serialNumber() +
                            " | Status: " + record.status());
                }
            }
        }
    }
}