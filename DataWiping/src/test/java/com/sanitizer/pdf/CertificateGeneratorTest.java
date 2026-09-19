package com.sanitizer.pdf;

import com.sanitizer.db.AuditDb;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced PDF Sanitization Certificate:
 * - Thermal Telemetry Attestation section
 * - Interface Signal Integrity section (SMART ID 199 & 188)
 * - Drive Risk Classification badge
 * - Helper derive* methods (pure logic, no PDF generation required)
 */
class CertificateGeneratorTest {

    // ── deriveThermalStatus ────────────────────────────────────────────────────

    @Test
    void testThermalStatus_NoPauseNormalTemp() {
        String status = CertificateGenerator.deriveThermalStatus(38, 0);
        assertTrue(status.contains("NORMAL"), "38°C with no pauses should be NORMAL");
    }

    @Test
    void testThermalStatus_ElevatedTemp() {
        String status = CertificateGenerator.deriveThermalStatus(50, 0);
        assertTrue(status.contains("ELEVATED"), "50°C should be ELEVATED");
    }

    @Test
    void testThermalStatus_CriticalTemp() {
        String status = CertificateGenerator.deriveThermalStatus(62, 0);
        assertTrue(status.contains("CRITICAL"), "62°C should be CRITICAL");
    }

    @Test
    void testThermalStatus_PauseEventTakesPriority() {
        String status = CertificateGenerator.deriveThermalStatus(38, 2);
        assertTrue(status.contains("AUTO-PAUSED"), "Pause count > 0 should indicate AUTO-PAUSED");
        assertTrue(status.contains("2"), "Pause count should appear in the label");
    }

    @Test
    void testThermalStatus_NoDataRecorded() {
        String status = CertificateGenerator.deriveThermalStatus(0, 0);
        assertTrue(status.contains("N/A"), "Zero temp with no pauses should show N/A");
    }

    // ── deriveInterfaceStatus ─────────────────────────────────────────────────

    @Test
    void testInterfaceStatus_Optimal() {
        String status = CertificateGenerator.deriveInterfaceStatus(0, "OPTIMAL");
        assertTrue(status.contains("OPTIMAL"), "0 CRC errors + OPTIMAL summary should return OPTIMAL");
    }

    @Test
    void testInterfaceStatus_Warning() {
        String status = CertificateGenerator.deriveInterfaceStatus(2, "Degraded Link (Port/Cable): INTERMITTENT PORT JITTER");
        assertTrue(status.contains("WARNING"), "Low CRC errors should map to WARNING status");
    }

    @Test
    void testInterfaceStatus_CriticalByCrcCount() {
        String status = CertificateGenerator.deriveInterfaceStatus(6, "Degraded Link (Port/Cable): anything");
        assertTrue(status.contains("CRITICAL"), "CRC >= 5 should produce CRITICAL status");
    }

    @Test
    void testInterfaceStatus_CriticalBySummaryKeyword() {
        String status = CertificateGenerator.deriveInterfaceStatus(2, "Critical Interface Flaw: FAULTY USB CABLE");
        assertTrue(status.contains("CRITICAL"), "Summary containing CRITICAL keyword should produce CRITICAL status");
    }

    // ── deriveRiskClassification ──────────────────────────────────────────────

    @Test
    void testRiskClassification_LowRisk() {
        String risk = CertificateGenerator.deriveRiskClassification(95, 93, 0, 0, "OPTIMAL");
        assertTrue(risk.contains("LOW RISK"), "High health, OPTIMAL interface, no pauses should be LOW RISK");
        assertTrue(risk.contains("CERTIFIED CLEAN"), "LOW RISK badge should say CERTIFIED CLEAN");
    }

    @Test
    void testRiskClassification_ModerateRisk_ByHealth() {
        String risk = CertificateGenerator.deriveRiskClassification(72, 70, 0, 0, "OPTIMAL");
        assertTrue(risk.contains("MODERATE RISK"), "Health 70-79 should be MODERATE RISK");
    }

    @Test
    void testRiskClassification_ModerateRisk_ByThermalPause() {
        String risk = CertificateGenerator.deriveRiskClassification(90, 88, 0, 3, "OPTIMAL");
        assertTrue(risk.contains("MODERATE RISK"), "Thermal pause events should escalate to MODERATE RISK");
    }

    @Test
    void testRiskClassification_ModerateRisk_ByInterfaceWarning() {
        String risk = CertificateGenerator.deriveRiskClassification(85, 83, 2, 0,
                "Degraded Link (Port/Cable): WARNING something");
        assertTrue(risk.contains("MODERATE RISK"), "Interface WARNING should produce MODERATE RISK");
    }

    @Test
    void testRiskClassification_HighRisk_ByHealth() {
        String risk = CertificateGenerator.deriveRiskClassification(40, 38, 0, 0, "OPTIMAL");
        assertTrue(risk.contains("HIGH RISK"), "Health < 50 should be HIGH RISK");
    }

    @Test
    void testRiskClassification_HighRisk_ByInterfaceCritical() {
        String risk = CertificateGenerator.deriveRiskClassification(88, 85, 6, 0,
                "Critical Interface Flaw: FAULTY USB CABLE / PORT DEGRADATION");
        assertTrue(risk.contains("HIGH RISK"),
                "Interface CRITICAL (crc >= 5 or CRITICAL keyword) should be HIGH RISK");
        assertTrue(risk.contains("OPERATOR OVERRIDE RECORDED"),
                "HIGH RISK badge should mention operator override");
    }

    // ── AuditRecord new fields ────────────────────────────────────────────────

    @Test
    void testAuditRecordNewFields_DefaultsToOptimal() {
        AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                1, "2026-01-01", "Test Drive", "SN-001", "32 GB",
                "NIST SP 800-88", "SUCCESS", "SIG_TEST");
        assertEquals(0, record.peakTempCelsius(), "Legacy 8-param constructor should default peakTempCelsius to 0");
        assertEquals(0, record.thermalPauseCount(), "Legacy constructor should default thermalPauseCount to 0");
        assertEquals(0, record.crcErrors(), "Legacy constructor should default crcErrors to 0");
        assertEquals("OPTIMAL", record.interfaceAnomalySummary(), "Legacy constructor should default interface to OPTIMAL");
    }

    @Test
    void testAuditRecordNewFields_FullConstructor() {
        AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                42, "2026-09-19", "Samsung NVMe 512GB", "SS-NVMe-001", "512 GB",
                "DoD 5220.22-M", "SUCCESS", "SIG_FULL",
                95, 93, 0, 0, "Integrity Verified: 0 Defects",
                52, 1, 3, "Critical Interface Flaw: FAULTY USB CABLE / PORT DEGRADATION");

        assertEquals(52, record.peakTempCelsius());
        assertEquals(1, record.thermalPauseCount());
        assertEquals(3, record.crcErrors());
        assertTrue(record.interfaceAnomalySummary().contains("FAULTY USB CABLE"));
    }
}
