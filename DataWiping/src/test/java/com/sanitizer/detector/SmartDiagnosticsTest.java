package com.sanitizer.detector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SmartDiagnosticsTest {

    @BeforeEach
    @AfterEach
    public void cleanup() {
        SmartDiagnostics.clearMockReports();
    }

    @Test
    public void testInspectDriveDeterministic() {
        UsbDetector.UsbDriveInfo drive = new UsbDetector.UsbDriveInfo(
                "SanDisk Ultra 64GB",
                "SD1234567890",
                64L * 1024 * 1024 * 1024,
                "64 GB",
                "/dev/rdisk4"
        );

        SmartDiagnostics.SmartReport report = SmartDiagnostics.inspectDrive(drive);
        assertNotNull(report, "SMART report should not be null");
        assertEquals("/dev/rdisk4", report.systemPath());
        assertEquals("SanDisk Ultra 64GB", report.model());
        assertEquals("SD1234567890", report.serial());

        // Verify deep SMART parsed fields
        assertTrue(report.wearLevelingPercent() >= 0 && report.wearLevelingPercent() <= 100);
        assertTrue(report.powerOnHours() >= 0);
        assertTrue(report.temperatureCelsius() > 0);
        assertNotNull(report.healthScore());
        assertTrue(report.healthScore().score() >= 0 && report.healthScore().score() <= 100);
        assertNotNull(report.thermalStatus());
        assertFalse(report.attributes().isEmpty(), "SMART attributes table should contain attributes");
    }

    @Test
    public void testParseSmartctlOutput() {
        List<String> mockSmartctl = new ArrayList<>();
        mockSmartctl.add("smartctl 7.3 2022-02-28 r5338 [Darwin 23.4.0 arm64] (local build)");
        mockSmartctl.add("=== START OF READ SMART DATA SECTION ===");
        mockSmartctl.add("ID# ATTRIBUTE_NAME          FLAG     VALUE WORST THRESH TYPE      UPDATED  WHEN_FAILED RAW_VALUE");
        mockSmartctl.add("  1 Raw_Read_Error_Rate     0x002f   100   100   051    Pre-fail  Always       -       0");
        mockSmartctl.add("  5 Reallocated_Sector_Ct   0x0033   100   100   010    Pre-fail  Always       -       3");
        mockSmartctl.add("  9 Power_On_Hours          0x0032   095   095   000    Old_age   Always       -       4250");
        mockSmartctl.add(" 12 Power_Cycle_Count       0x0032   099   099   000    Old_age   Always       -       120");
        mockSmartctl.add("173 Wear_Leveling_Count     0x0032   094   094   000    Old_age   Always       -       94");
        mockSmartctl.add("194 Temperature_Celsius     0x0022   038   045   000    Old_age   Always       -       38");
        mockSmartctl.add("197 Current_Pending_Sector  0x0032   100   100   000    Old_age   Always       -       0");

        SmartDiagnostics.SmartReport report = SmartDiagnostics.parseSmartctlOutput(
                mockSmartctl,
                "/dev/disk3",
                "Kingston DataTraveler",
                "KNG998877",
                32L * 1024 * 1024 * 1024
        );

        assertNotNull(report);
        assertEquals(3, report.reallocatedSectors());
        assertEquals(94, report.wearLevelingPercent());
        assertEquals(0, report.badBlocks());
        assertEquals(4250, report.powerOnHours());
        assertEquals(38, report.temperatureCelsius());
        assertEquals(120, report.powerCycleCount());
        assertTrue(report.isHardwareSmartSupported());

        // Attributes list checks
        assertEquals(7, report.attributes().size());
        assertEquals("Reallocated_Sector_Ct", report.attributes().get(1).name());
        assertEquals("WARNING", report.attributes().get(1).status());
    }

    @Test
    public void testHealthScoreCalculationHealthy() {
        // Optimal drive: 0 reallocated, 100% wear, 0 bad blocks, low hours, 32C
        SmartDiagnostics.HealthScoreResult health = SmartDiagnostics.calculateHealthScore(
                0, 100, 0, 500, 0, 0, 32
        );

        assertEquals(100, health.score());
        assertEquals(SmartDiagnostics.HealthStatus.HEALTHY, health.status());
        assertTrue(health.isWipePermittedWithoutOverride());
        assertTrue(health.warnings().isEmpty());
    }

    @Test
    public void testHealthScoreCalculationDegraded() {
        // Moderate wear: 1 reallocated sector, 70% wear, 15,000 hrs, 40C
        SmartDiagnostics.HealthScoreResult health = SmartDiagnostics.calculateHealthScore(
                1, 70, 0, 15000, 0, 0, 40
        );

        assertTrue(health.score() >= 50 && health.score() < 90);
        assertFalse(health.warnings().isEmpty());
        assertTrue(health.isWipePermittedWithoutOverride());
    }

    @Test
    public void testHealthScoreCalculationCritical() {
        // Critical: 4 bad blocks, 5 reallocated sectors, 40% wear, 62C overheating
        SmartDiagnostics.HealthScoreResult health = SmartDiagnostics.calculateHealthScore(
                5, 40, 4, 45000, 2, 3, 62
        );

        assertTrue(health.score() < 50, "Critical drive score should be below 50");
        assertEquals(SmartDiagnostics.HealthStatus.CRITICAL, health.status());
        assertFalse(health.isWipePermittedWithoutOverride(), "Critical health must trigger pre-wipe override requirement");
        assertTrue(health.warnings().size() >= 3);
    }

    @Test
    public void testThermalStatusEvaluation() {
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL, SmartDiagnostics.evaluateThermalStatus(35));
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL, SmartDiagnostics.evaluateThermalStatus(44));
        assertEquals(SmartDiagnostics.ThermalStatus.ELEVATED, SmartDiagnostics.evaluateThermalStatus(48));
        assertEquals(SmartDiagnostics.ThermalStatus.ELEVATED, SmartDiagnostics.evaluateThermalStatus(52));
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL, SmartDiagnostics.evaluateThermalStatus(55)); // 55°C is default USB Auto-Pause threshold
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL, SmartDiagnostics.evaluateThermalStatus(60));
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL, SmartDiagnostics.evaluateThermalStatus(72));

        // Test with explicit device types:
        // NVMe SSD: 55°C is NORMAL, 62°C is ELEVATED, 70°C is CRITICAL
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL, SmartDiagnostics.evaluateThermalStatus(55, DeviceType.NVME_SSD));
        assertEquals(SmartDiagnostics.ThermalStatus.ELEVATED, SmartDiagnostics.evaluateThermalStatus(62, DeviceType.NVME_SSD));
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL, SmartDiagnostics.evaluateThermalStatus(70, DeviceType.NVME_SSD));

        // Magnetic HDD: 42°C is NORMAL, 46°C is ELEVATED, 50°C is CRITICAL
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL, SmartDiagnostics.evaluateThermalStatus(42, DeviceType.MAGNETIC_HDD));
        assertEquals(SmartDiagnostics.ThermalStatus.ELEVATED, SmartDiagnostics.evaluateThermalStatus(46, DeviceType.MAGNETIC_HDD));
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL, SmartDiagnostics.evaluateThermalStatus(50, DeviceType.MAGNETIC_HDD));
    }

    @Test
    public void testLiveTemperatureTracking() {
        SmartDiagnostics.setLiveTemperature("/dev/rdisk5", 58);
        assertEquals(58, SmartDiagnostics.getLiveTemperature("/dev/rdisk5", "SER-123"));

        SmartDiagnostics.setLiveTemperature("/dev/rdisk5", 42);
        assertEquals(42, SmartDiagnostics.getLiveTemperature("/dev/rdisk5", "SER-123"));
    }

    @Test
    public void testCaptureSnapshotAndCompareSnapshotsClean() {
        SmartDiagnostics.SmartSnapshot pre = new SmartDiagnostics.SmartSnapshot(
                "/dev/rdisk2", "SER-CLEAN", 100, SmartDiagnostics.HealthStatus.HEALTHY,
                0, 100, 0, 500L, 32, 0, 0, 0, System.currentTimeMillis()
        );

        SmartDiagnostics.SmartSnapshot post = new SmartDiagnostics.SmartSnapshot(
                "/dev/rdisk2", "SER-CLEAN", 99, SmartDiagnostics.HealthStatus.HEALTHY,
                0, 99, 0, 501L, 38, 0, 0, 0, System.currentTimeMillis() + 60000
        );

        SmartDiagnostics.SmartDelta delta = SmartDiagnostics.compareSnapshots(pre, post);
        assertNotNull(delta);
        assertEquals(-1, delta.healthScoreDelta());
        assertEquals(0, delta.badBlocksDelta());
        assertEquals(0, delta.reallocatedDelta());
        assertEquals(1, delta.wearDeltaPercent());
        assertEquals(6, delta.tempDeltaCelsius());
        assertTrue(delta.isIntegrityMaintained());
        assertTrue(delta.integrityVerdict().contains("INTEGRITY CERTIFIED"));
        assertTrue(delta.formattedSummary().contains("Health: 100 -> 99"));
    }

    @Test
    public void testCompareSnapshotsDegradation() {
        SmartDiagnostics.SmartSnapshot pre = new SmartDiagnostics.SmartSnapshot(
                "/dev/rdisk2", "SER-DEG", 90, SmartDiagnostics.HealthStatus.HEALTHY,
                0, 95, 0, 12000L, 35, 0, 0, 0, System.currentTimeMillis()
        );

        SmartDiagnostics.SmartSnapshot post = new SmartDiagnostics.SmartSnapshot(
                "/dev/rdisk2", "SER-DEG", 55, SmartDiagnostics.HealthStatus.WARNING,
                3, 90, 2, 12002L, 52, 0, 0, 0, System.currentTimeMillis() + 120000
        );

        SmartDiagnostics.SmartDelta delta = SmartDiagnostics.compareSnapshots(pre, post);
        assertNotNull(delta);
        assertEquals(-35, delta.healthScoreDelta());
        assertEquals(2, delta.badBlocksDelta());
        assertEquals(3, delta.reallocatedDelta());
        assertEquals(5, delta.wearDeltaPercent());
        assertFalse(delta.isIntegrityMaintained());
        assertTrue(delta.integrityVerdict().contains("DEGRADATION DETECTED"));
    }
}
