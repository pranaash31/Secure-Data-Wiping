package com.sanitizer.quarantine;

import java.util.List;

/**
 * Encapsulates the complete Hardware Quarantine & Physical Destruction Order assessment for a defective drive.
 */
public record QuarantineRecord(
        String quarantineId,
        String timestampUtc,
        String driveModel,
        String serialNumber,
        String capacity,
        String interfaceType,      // NVMe, SATA, USB, SAS, etc.
        String mediaType,          // SSD, HDD, HYBRID, UNKNOWN
        String attemptedStandard,
        String failureReason,
        List<LbaFailureRecord> failingSectors,
        int totalBadSectorsDetected,
        int preHealthScore,
        int postHealthScore,
        String smartIntegritySummary,
        PhysicalDestructionMethod destructionRecommendation,
        String complianceWarningNotice,
        String digitalAttestationSignature
) {
    public enum PhysicalDestructionMethod {
        MAGNETIC_DEGAUSSING(
                "Physical Degaussing (>= 1.5 Tesla)",
                "NSA/CSS EPL-9-12A / NIST SP 800-88 Purge",
                "Applies a high-intensity magnetic flux field (minimum 15,000 Gauss / 1.5 Tesla) exceeding the coercivity of magnetic platters to permanently randomize magnetic domains.",
                "Magnetic HDD (Platters / Rotational Media)"
        ),
        MECHANICAL_SHREDDING(
                "Mechanical Shredding (DIN 66399 Level H-5 / E-5)",
                "DIN 66399 Security Level E-5 / H-5 (Max 2mm Particle Size)",
                "Physical disintegration through cross-cut mechanical shredding ensuring max particle size < 2 mm² to prevent NAND silicon die reconstitution.",
                "Solid-State Drive (NAND Flash / NVMe / SSD)"
        ),
        PHYSICAL_INCINERATION(
                "High-Temperature Incineration (1,000°C)",
                "DoD 5220.22-M Section 8 Physical Destruction",
                "Thermal destruction in a certified hazardous materials furnace converting semiconductor dies and magnetic media into slag.",
                "High-Security Classified Assets / Unidentifiable Media"
        );

        private final String title;
        private final String standardReference;
        private final String technicalDescription;
        private final String targetMedia;

        PhysicalDestructionMethod(String title, String standardReference, String technicalDescription, String targetMedia) {
            this.title = title;
            this.standardReference = standardReference;
            this.technicalDescription = technicalDescription;
            this.targetMedia = targetMedia;
        }

        public String getTitle() { return title; }
        public String getStandardReference() { return standardReference; }
        public String getTechnicalDescription() { return technicalDescription; }
        public String getTargetMedia() { return targetMedia; }
    }
}
