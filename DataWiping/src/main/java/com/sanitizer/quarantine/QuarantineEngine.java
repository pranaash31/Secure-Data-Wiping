package com.sanitizer.quarantine;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.util.AppLogger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core Assessment Engine for Defective Storage Assets & Hardware Quarantine.
 * Evaluates sector-level I/O faults, media physics, and regulatory compliance standards
 * (NIST SP 800-88 Rev. 1 Section 4.7 / DoD 5220.22-M / DIN 66399) to prescribe mandatory physical destruction.
 */
public class QuarantineEngine {

    private static final String MODULE = "QuarantineEngine";
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_INSTANT;

    /**
     * Assesses a failed sanitization attempt and constructs a complete QuarantineRecord.
     */
    public static QuarantineRecord assessHardwareFailure(
            String driveModel,
            String serialNumber,
            String capacity,
            String systemPath,
            String attemptedStandard,
            String failureReason,
            List<LbaFailureRecord> failingSectors,
            int preHealthScore,
            int postHealthScore,
            String smartIntegritySummary
    ) {
        String timestamp = ISO_FMT.format(Instant.now());
        String quarantineId = generateQuarantineId();

        // Deduce Media & Interface Type
        String mediaType = "SSD / NAND Flash";
        String interfaceType = "NVMe / PCIe";

        if (driveModel != null) {
            String mUpper = driveModel.toUpperCase();
            if (mUpper.contains("HDD") || mUpper.contains("BARRACUDA") || mUpper.contains("WD BLUE") || mUpper.contains("IRONWOLF") || mUpper.contains("DESKSTAR") || mUpper.contains("ROTATIONAL")) {
                mediaType = "Magnetic HDD (Rotational Media)";
                interfaceType = "SATA III / SAS";
            } else if (mUpper.contains("USB") || mUpper.contains("SANDISK") || mUpper.contains("CRUISER") || mUpper.contains("FLASH")) {
                mediaType = "NAND Flash (USB / Removable)";
                interfaceType = "USB 3.2 / Type-C";
            } else if (mUpper.contains("NVME") || mUpper.contains("980") || mUpper.contains("970") || mUpper.contains("SN850") || mUpper.contains("P5")) {
                mediaType = "Solid-State NVMe SSD";
                interfaceType = "PCIe Gen4 x4 NVMe";
            }
        }

        // Determine Physical Destruction Method
        QuarantineRecord.PhysicalDestructionMethod destructionMethod;
        if (mediaType.contains("Magnetic HDD")) {
            destructionMethod = QuarantineRecord.PhysicalDestructionMethod.MAGNETIC_DEGAUSSING;
        } else {
            destructionMethod = QuarantineRecord.PhysicalDestructionMethod.MECHANICAL_SHREDDING;
        }

        List<LbaFailureRecord> sectors = failingSectors != null ? new ArrayList<>(failingSectors) : new ArrayList<>();
        int badSectorCount = sectors.stream().mapToInt(s -> (int) s.sectorCount()).sum();
        if (badSectorCount == 0) {
            badSectorCount = 1; // At least one failing block caused the quarantine
        }

        String complianceNotice = """
                NON-COMPLIANCE NOTICE & CHAIN-OF-CUSTODY QUARANTINE WARNING:
                This storage device encountered unrecoverable hardware I/O write failures and/or bad sectors during low-level overwrite.
                Per NIST SP 800-88 Rev. 1 Section 4.7, ISO/IEC 27040:2015, and EU GDPR Article 17, software-based sanitization CANNOT
                be certified as complete when raw sectors or reallocated blocks cannot be physically overwritten and verified.
                MANDATORY ACTION: Do NOT return this media to inventory or circular reuse. Dispatch immediately for physical destruction.
                """;

        String payloadToSign = quarantineId + "|" + driveModel + "|" + serialNumber + "|" + capacity + "|" + failureReason + "|" + destructionMethod.name();
        String signature = CryptoSigner.signData(payloadToSign);

        AppLogger.warn(MODULE, "QUARANTINE ASSESSMENT FIRED for " + driveModel + " (" + serialNumber + ") -> " + destructionMethod.getTitle());

        return new QuarantineRecord(
                quarantineId,
                timestamp,
                driveModel,
                serialNumber,
                capacity,
                interfaceType,
                mediaType,
                attemptedStandard,
                failureReason,
                sectors,
                badSectorCount,
                preHealthScore,
                postHealthScore,
                smartIntegritySummary,
                destructionMethod,
                complianceNotice.trim(),
                signature
        );
    }

    /**
     * Generates a formal tracking quarantine ID e.g. "QRN-20260920-A4B7".
     */
    public static String generateQuarantineId() {
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(java.time.ZoneId.of("UTC")).format(Instant.now());
        String uuidSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "QRN-" + dateStr + "-" + uuidSuffix;
    }
}
