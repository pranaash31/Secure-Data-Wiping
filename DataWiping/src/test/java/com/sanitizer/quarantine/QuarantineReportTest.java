package com.sanitizer.quarantine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Defective Hardware Quarantine & Bad Sector Reporting Tests")
class QuarantineReportTest {

    @Test
    @DisplayName("LbaFailureRecord accurately computes sector counts and hex address ranges")
    void testLbaFailureRecordMath() {
        LbaFailureRecord single = LbaFailureRecord.ofSingleLba(2048, 1, "POSIX_EIO", "Read failure at block");
        assertThat(single.startLba()).isEqualTo(2048);
        assertThat(single.endLba()).isEqualTo(2048);
        assertThat(single.sectorCount()).isEqualTo(1);
        assertThat(single.formattedHexRange()).isEqualTo("0x00000800 - 0x00000800");

        LbaFailureRecord range = LbaFailureRecord.ofByteOffset(1024 * 1024, 4096, 2, "WRITE_FAULT", "Unrecoverable sector fault");
        // 1MB = 1048576 bytes -> 1048576 / 512 = 2048 LBA
        // 4096 bytes = 8 sectors -> 2048 to 2055 LBA
        assertThat(range.startLba()).isEqualTo(2048);
        assertThat(range.endLba()).isEqualTo(2055);
        assertThat(range.sectorCount()).isEqualTo(8);
        assertThat(range.formattedHexRange()).isEqualTo("0x00000800 - 0x00000807");
    }

    @Test
    @DisplayName("QuarantineEngine prescribes Magnetic Degaussing for rotational HDDs")
    void testQuarantineEngineAssessmentForHdd() {
        LbaFailureRecord fault = LbaFailureRecord.ofSingleLba(50000, 1, "POSIX_EIO", "I/O error");
        QuarantineRecord record = QuarantineEngine.assessHardwareFailure(
                "Seagate Barracuda HDD 2TB",
                "ST2000-DEF-01",
                "2 TB",
                "/dev/rdisk3",
                "NIST SP 800-88",
                "Unrecoverable I/O Write Fault",
                List.of(fault),
                85,
                20,
                "Critical: 100 Bad Sectors Detected"
        );

        assertThat(record.quarantineId()).startsWith("QRN-");
        assertThat(record.mediaType()).contains("Magnetic HDD");
        assertThat(record.destructionRecommendation()).isEqualTo(QuarantineRecord.PhysicalDestructionMethod.MAGNETIC_DEGAUSSING);
        assertThat(record.destructionRecommendation().getTitle()).contains("Degaussing");
        assertThat(record.destructionRecommendation().getStandardReference()).contains("NSA/CSS");
        assertThat(record.complianceWarningNotice()).contains("NIST SP 800-88 Rev. 1 Section 4.7");
        assertThat(record.digitalAttestationSignature()).isNotBlank();
    }

    @Test
    @DisplayName("QuarantineEngine prescribes Mechanical Shredding for Solid State NVMe/SSDs")
    void testQuarantineEngineAssessmentForSsd() {
        LbaFailureRecord fault1 = LbaFailureRecord.ofByteOffset(2048000, 8192, 1, "UNC_ERROR", "Uncorrectable ECC block");
        LbaFailureRecord fault2 = LbaFailureRecord.ofByteOffset(4096000, 4096, 2, "WRITE_FAULT", "NAND Die failure");

        QuarantineRecord record = QuarantineEngine.assessHardwareFailure(
                "Samsung 980 Pro NVMe SSD 1TB",
                "S980-CORRUPT-99",
                "1 TB",
                "/dev/rdisk4",
                "DoD 5220.22-M",
                "Uncorrectable ECC & Controller Write Fault",
                List.of(fault1, fault2),
                95,
                15,
                "Flash Wear Exhaustion & Defective NAND Planes"
        );

        assertThat(record.quarantineId()).startsWith("QRN-");
        assertThat(record.mediaType()).contains("NVMe");
        assertThat(record.destructionRecommendation()).isEqualTo(QuarantineRecord.PhysicalDestructionMethod.MECHANICAL_SHREDDING);
        assertThat(record.destructionRecommendation().getTitle()).contains("Shredding");
        assertThat(record.destructionRecommendation().getStandardReference()).contains("DIN 66399");
        assertThat(record.totalBadSectorsDetected()).isGreaterThanOrEqualTo(2);
        assertThat(record.digitalAttestationSignature()).isNotBlank();
    }

    @Test
    @DisplayName("QuarantineReportGenerator produces valid PDF Destruction Order with LBA table")
    void testQuarantineReportGeneratorPdf() throws IOException {
        LbaFailureRecord fault = LbaFailureRecord.ofByteOffset(1024 * 512, 16384, 1, "POSIX_EIO", "Input/output error");
        QuarantineRecord record = QuarantineEngine.assessHardwareFailure(
                "WD Black SN850X SSD",
                "WDB-BAD-777",
                "1 TB",
                "/dev/rdisk5",
                "British HMG IS5",
                "Media Write Failure at LBA boundary",
                List.of(fault),
                90,
                30,
                "Degraded: S.M.A.R.T. Reallocated Blocks"
        );

        String pdfPath = QuarantineReportGenerator.generatePdfReport(record);
        assertThat(pdfPath).isNotNull();

        File pdfFile = new File(pdfPath);
        assertThat(pdfFile).exists().isNotEmpty();

        // Validate PDF contents using PDFBox text extraction
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("DEFECTIVE HARDWARE QUARANTINE & DESTRUCTION ORDER");
            assertThat(text).contains(record.quarantineId());
            assertThat(text).contains("WD Black SN850X SSD");
            assertThat(text).contains("WDB-BAD-777");
            assertThat(text).contains("MECHANICAL SHREDDING");
            assertThat(text).contains("CHAIN OF CUSTODY & PHYSICAL DESTRUCTION SIGN-OFF");
        } finally {
            pdfFile.deleteOnExit();
        }
    }

    @Test
    @DisplayName("QuarantineReportGenerator produces valid structured JSON export")
    void testQuarantineReportGeneratorJson(@TempDir Path tempDir) throws IOException {
        LbaFailureRecord fault = LbaFailureRecord.ofSingleLba(100000, 1, "POSIX_EIO", "I/O failure");
        QuarantineRecord record = QuarantineEngine.assessHardwareFailure(
                "Kingston KC3000 SSD",
                "KC3000-001",
                "512 GB",
                "/dev/rdisk6",
                "German BSI-VS",
                "Controller I/O Hang",
                List.of(fault),
                92,
                25,
                "Defective Controller / Block Write Timeout"
        );

        String json = QuarantineReportGenerator.generateJsonReport(record);
        assertThat(json).isNotBlank();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertThat(root.has("quarantineId")).isTrue();
        assertThat(root.get("driveModel").getAsString()).isEqualTo("Kingston KC3000 SSD");
        assertThat(root.get("destructionRecommendation").getAsString()).isEqualTo("MECHANICAL_SHREDDING");
        assertThat(root.getAsJsonArray("failingSectors").size()).isEqualTo(1);

        File jsonFile = tempDir.resolve("quarantine_export.json").toFile();
        QuarantineReportGenerator.exportToJson(record, jsonFile);
        assertThat(jsonFile).exists().isNotEmpty();
    }
}
