package com.sanitizer.detector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Interface Anomaly & Port Degradation Detection.
 * Covers SMART ID 199 (UDMA CRC Error Count) and ID 188 (Command Timeout) correlation logic,
 * root-cause differentiation (cable/port vs. media defect), severity classification, and user prompts.
 */
class InterfaceAnomalyTest {

    // ── OPTIMAL (no errors) ────────────────────────────────────────────────────

    @Test
    void testOptimal_NoErrorsNoMediaDefects() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(0, 0, 0, 0);

        assertFalse(result.isDegraded(), "Should not be degraded with 0 errors");
        assertEquals(SmartDiagnostics.InterfaceSeverity.OPTIMAL, result.severity());
        assertEquals(0, result.crcErrors());
        assertEquals(0, result.commandTimeouts());
        assertTrue(result.rootCauseDiagnosis().contains("INTERFACE SIGNAL OPTIMAL"),
                "Root cause should indicate optimal interface");
        assertTrue(result.userPrompt().contains("stable"),
                "Prompt should indicate stable connection");
    }

    // ── WARNING (minor errors: crcErrors < 5 && commandTimeouts < 3) ─────────

    @Test
    void testWarning_LowCrcErrors_NoMedia() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(2, 0, 0, 0);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.WARNING, result.severity());
        assertEquals(2, result.crcErrors());
        assertEquals(0, result.commandTimeouts());
        assertTrue(result.rootCauseDiagnosis().contains("PORT JITTER"),
                "Pure CRC errors with no media defects should indicate port/cable jitter");
        assertTrue(result.userPrompt().contains("USB cable"),
                "Prompt should advise inspecting USB cable");
    }

    @Test
    void testWarning_LowTimeouts_NoMedia() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(0, 1, 0, 0);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.WARNING, result.severity());
    }

    @Test
    void testWarning_LowErrors_WithMediaDefects() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(1, 0, 2, 1);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.WARNING, result.severity());
        assertTrue(result.rootCauseDiagnosis().contains("DEGRADED BUS LINK"),
                "CRC errors alongside media wear should indicate degraded bus link");
    }

    // ── CRITICAL (crcErrors >= 5 || commandTimeouts >= 3) ────────────────────

    @Test
    void testCritical_HighCrcErrors_NoMedia() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(5, 0, 0, 0);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity());
        assertEquals(5, result.crcErrors());
    }

    @Test
    void testCritical_HighTimeouts_NoMedia() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(0, 3, 0, 0);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity());
        assertEquals(3, result.commandTimeouts());
    }

    @Test
    void testCritical_BothHighErrors_NoMedia_PureCableRoot() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(10, 5, 0, 0);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity());
        assertEquals(10, result.crcErrors());
        assertEquals(5, result.commandTimeouts());
        // Media surface intact → must attribute to cable/port
        assertTrue(result.rootCauseDiagnosis().contains("FAULTY USB CABLE / PORT DEGRADATION"),
                "With 0 bad blocks and 0 reallocated sectors, root cause must be cable/port");
        assertTrue(result.rootCauseDiagnosis().contains("0 bad blocks"),
                "Root cause should explicitly mention 0 bad blocks");
    }

    @Test
    void testCritical_HighErrors_WithMediaDefects_CompoundedFailure() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(8, 4, 3, 2);

        assertTrue(result.isDegraded());
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity());
        assertTrue(result.rootCauseDiagnosis().contains("COMPOUNDED FAILURE"),
                "Both interface errors and media defects should indicate compounded failure");
    }

    // ── User Prompt Exact String ───────────────────────────────────────────────

    @Test
    void testCritical_UserPromptExactString() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(5, 3, 0, 0);

        assertEquals(
                "High CRC errors detected — check cable connection or switch USB port.",
                result.userPrompt(),
                "Critical user prompt must match exact specification"
        );
    }

    @Test
    void testWarning_UserPromptMentionsCable() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(1, 0, 0, 0);

        assertNotNull(result.userPrompt());
        assertFalse(result.userPrompt().isBlank());
        assertTrue(result.userPrompt().contains("USB cable"),
                "Warning-level prompt should mention USB cable");
    }

    // ── Boundary cases ────────────────────────────────────────────────────────

    @Test
    void testBoundary_ExactlyFiveCrc_IsCritical() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(5, 0, 0, 0);
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity(),
                "Exactly 5 CRC errors should be CRITICAL");
    }

    @Test
    void testBoundary_FourCrc_IsWarning() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(4, 0, 0, 0);
        assertEquals(SmartDiagnostics.InterfaceSeverity.WARNING, result.severity(),
                "4 CRC errors should be WARNING, not CRITICAL");
    }

    @Test
    void testBoundary_ExactlyThreeTimeouts_IsCritical() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(0, 3, 0, 0);
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, result.severity(),
                "Exactly 3 command timeouts should be CRITICAL");
    }

    @Test
    void testBoundary_TwoTimeouts_IsWarning() {
        SmartDiagnostics.InterfaceAnomalyResult result =
                SmartDiagnostics.evaluateInterfaceHealth(0, 2, 0, 0);
        assertEquals(SmartDiagnostics.InterfaceSeverity.WARNING, result.severity(),
                "2 command timeouts should be WARNING");
    }

    // ── Integration with calculateHealthScore ─────────────────────────────────

    @Test
    void testHealthScorePenalty_InterfaceAnomalyApplied() {
        // No anomaly baseline
        SmartDiagnostics.HealthScoreResult clean =
                SmartDiagnostics.calculateHealthScore(0, 100, 0, 1000L, 0, 0, 0, 35, DeviceType.USB_FLASH);

        // With critical interface anomaly
        SmartDiagnostics.HealthScoreResult anomaly =
                SmartDiagnostics.calculateHealthScore(0, 100, 0, 1000L, 0, 8, 4, 35, DeviceType.USB_FLASH);

        assertTrue(anomaly.score() < clean.score(),
                "Drive with interface anomaly should score lower than clean drive");
        assertTrue(anomaly.warnings().stream().anyMatch(w -> w.contains("Interface Anomaly")),
                "Health warnings should include Interface Anomaly entry");
    }

    @Test
    void testHealthScoreWarning_ContainsUserPrompt() {
        SmartDiagnostics.HealthScoreResult result =
                SmartDiagnostics.calculateHealthScore(0, 100, 0, 500L, 0, 6, 0, 35, DeviceType.USB_FLASH);

        assertTrue(result.warnings().stream()
                        .anyMatch(w -> w.contains("check cable connection or switch USB port")),
                "Health score warnings should contain exact CRC user prompt text");
    }

    // ── SmartReport integration — parseSmartctlOutput ─────────────────────────

    @Test
    void testParseSmartctlOutput_ParsesId188() {
        java.util.List<String> lines = java.util.List.of(
                "ID# ATTRIBUTE_NAME          FLAG     VALUE WORST THRESH TYPE      UPDATED  WHEN_FAILED RAW_VALUE",
                "  1 Raw_Read_Error_Rate     0x002f   100   100   051    Pre-fail  Always       -       0",
                "188 Command_Timeout         0x0032   100   100   000    Old_age   Always       -       3",
                "199 UDMA_CRC_Error_Count    0x003e   200   200   000    Old_age   Always       -       7"
        );

        SmartDiagnostics.SmartReport report =
                SmartDiagnostics.parseSmartctlOutput(lines, "/dev/disk4", "Test Drive", "SN123", 64_000_000_000L);

        assertNotNull(report, "Report should not be null with valid attributes");
        assertEquals(3, report.commandTimeouts(), "Should parse ID 188 Command Timeout value as 3");
        assertEquals(7, report.crcErrors(), "Should parse ID 199 CRC Error value as 7");
        assertNotNull(report.interfaceAnomaly(), "interfaceAnomaly should be populated");
        assertTrue(report.interfaceAnomaly().isDegraded(),
                "Report with high CRC+timeouts should be flagged as degraded");
        assertEquals(SmartDiagnostics.InterfaceSeverity.CRITICAL, report.interfaceAnomaly().severity());
    }
}
