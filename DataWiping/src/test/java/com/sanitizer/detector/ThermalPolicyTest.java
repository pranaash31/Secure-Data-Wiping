package com.sanitizer.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThermalPolicyTest {

    private ThermalPolicyManager policyManager;

    @BeforeEach
    public void setUp() {
        policyManager = ThermalPolicyManager.getInstance();
        policyManager.resetToDefaults();
    }

    @Test
    public void testDeviceTypeDefaults() {
        // 1. USB Flash / Pen Drives: 55°C auto-pause, 42°C resume
        assertEquals(55, DeviceType.USB_FLASH.getDefaultAutoPauseCelsius());
        assertEquals(42, DeviceType.USB_FLASH.getDefaultResumeCelsius());

        // 2. NVMe / High-Speed SSDs: 70°C auto-pause, 52°C resume
        assertEquals(70, DeviceType.NVME_SSD.getDefaultAutoPauseCelsius());
        assertEquals(52, DeviceType.NVME_SSD.getDefaultResumeCelsius());

        // 3. Magnetic HDDs: 50°C auto-pause, 40°C resume
        assertEquals(50, DeviceType.MAGNETIC_HDD.getDefaultAutoPauseCelsius());
        assertEquals(40, DeviceType.MAGNETIC_HDD.getDefaultResumeCelsius());
    }

    @Test
    public void testDeviceTypeHeuristicDetection() {
        // NVMe / SSD Detection
        assertEquals(DeviceType.NVME_SSD, DeviceType.fromDrive("Samsung 980 Pro NVMe SSD 1TB", "/dev/rdisk2", 1000L * 1024 * 1024 * 1024));
        assertEquals(DeviceType.NVME_SSD, DeviceType.fromDrive("Crucial P3 Plus PCIe M.2 SSD", "/dev/rdisk3", 500L * 1024 * 1024 * 1024));
        assertEquals(DeviceType.NVME_SSD, DeviceType.fromDrive("Solid State Drive", "/dev/nvme0n1", 256L * 1024 * 1024 * 1024));

        // Magnetic HDD Detection
        assertEquals(DeviceType.MAGNETIC_HDD, DeviceType.fromDrive("Seagate Barracuda HDD 2TB", "/dev/rdisk4", 2000L * 1024 * 1024 * 1024));
        assertEquals(DeviceType.MAGNETIC_HDD, DeviceType.fromDrive("WD Blue 5400RPM Hard Drive", "/dev/rdisk5", 1000L * 1024 * 1024 * 1024));
        assertEquals(DeviceType.MAGNETIC_HDD, DeviceType.fromDrive("ST1000DM010-2EP102", "/dev/rdisk6", 1000L * 1024 * 1024 * 1024));

        // USB Flash Drive Detection
        assertEquals(DeviceType.USB_FLASH, DeviceType.fromDrive("SanDisk Ultra Flair 3.0", "/dev/rdisk7", 32L * 1024 * 1024 * 1024));
        assertEquals(DeviceType.USB_FLASH, DeviceType.fromDrive("Kingston DataTraveler 64GB", "/dev/rdisk8", 64L * 1024 * 1024 * 1024));
    }

    @Test
    public void testThermalPolicyValidation() {
        // Valid policies
        assertTrue(ThermalPolicy.isValid(55, 42));
        assertTrue(ThermalPolicy.isValid(70, 52));
        assertTrue(ThermalPolicy.isValid(50, 40));
        assertTrue(ThermalPolicy.isValid(45, 40));

        // Invalid: Pause <= Resume
        assertFalse(ThermalPolicy.isValid(50, 50));
        assertFalse(ThermalPolicy.isValid(40, 50));

        // Invalid: Hysteresis gap less than 3°C
        assertFalse(ThermalPolicy.isValid(50, 49));
        assertFalse(ThermalPolicy.isValid(50, 48));

        // Invalid: Out of bounds
        assertFalse(ThermalPolicy.isValid(30, 25)); // Below minimum allowed temp
        assertFalse(ThermalPolicy.isValid(110, 50)); // Above max allowed temp
    }

    @Test
    public void testThermalPolicyManagerUpdateAndReset() {
        // Custom policy configuration
        policyManager.setPolicy(DeviceType.USB_FLASH, 58, 44);
        ThermalPolicy usbPolicy = policyManager.getPolicy(DeviceType.USB_FLASH);
        assertEquals(58, usbPolicy.autoPauseCelsius());
        assertEquals(44, usbPolicy.resumeCelsius());

        policyManager.setPolicy(DeviceType.NVME_SSD, 75, 55);
        ThermalPolicy nvmePolicy = policyManager.getPolicy(DeviceType.NVME_SSD);
        assertEquals(75, nvmePolicy.autoPauseCelsius());
        assertEquals(55, nvmePolicy.resumeCelsius());

        policyManager.setPolicy(DeviceType.MAGNETIC_HDD, 48, 38);
        ThermalPolicy hddPolicy = policyManager.getPolicy(DeviceType.MAGNETIC_HDD);
        assertEquals(48, hddPolicy.autoPauseCelsius());
        assertEquals(38, hddPolicy.resumeCelsius());

        // Reset to factory defaults
        policyManager.resetToDefaults();
        assertEquals(55, policyManager.getPauseThreshold(DeviceType.USB_FLASH));
        assertEquals(42, policyManager.getResumeThreshold(DeviceType.USB_FLASH));

        assertEquals(70, policyManager.getPauseThreshold(DeviceType.NVME_SSD));
        assertEquals(52, policyManager.getResumeThreshold(DeviceType.NVME_SSD));

        assertEquals(50, policyManager.getPauseThreshold(DeviceType.MAGNETIC_HDD));
        assertEquals(40, policyManager.getResumeThreshold(DeviceType.MAGNETIC_HDD));
    }

    @Test
    public void testDeviceTypeAwareThermalStatus() {
        policyManager.resetToDefaults();

        // 56°C is Overheating for USB Flash (>55°C), but Normal for NVMe SSD (limits: 70°C pause, 60°C warning)
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL,
                SmartDiagnostics.evaluateThermalStatus(56, DeviceType.USB_FLASH));
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL,
                SmartDiagnostics.evaluateThermalStatus(56, DeviceType.NVME_SSD));

        // 51°C is Overheating for Magnetic HDD (>50°C), but Normal for USB Flash & NVMe SSD
        assertEquals(SmartDiagnostics.ThermalStatus.CRITICAL,
                SmartDiagnostics.evaluateThermalStatus(51, DeviceType.MAGNETIC_HDD));
        assertEquals(SmartDiagnostics.ThermalStatus.NORMAL,
                SmartDiagnostics.evaluateThermalStatus(44, DeviceType.MAGNETIC_HDD));

        // 65°C is Elevated for NVMe SSD (>= 60°C and < 70°C)
        assertEquals(SmartDiagnostics.ThermalStatus.ELEVATED,
                SmartDiagnostics.evaluateThermalStatus(65, DeviceType.NVME_SSD));
    }

    @Test
    public void testHealthScoreDeviceTypeAwareness() {
        policyManager.resetToDefaults();

        // At 56°C:
        // For USB Flash: triggers critical thermal penalty (-15 pts) -> 85 score
        SmartDiagnostics.HealthScoreResult usbResult =
                SmartDiagnostics.calculateHealthScore(0, 100, 0, 100, 0, 0, 56, DeviceType.USB_FLASH);
        assertEquals(85, usbResult.score());
        assertTrue(usbResult.warnings().stream().anyMatch(w -> w.contains("Thermal Overheat (USB Flash / Pen Drives)")));

        // For NVMe SSD: 56°C is completely within safe operating range -> 100 score
        SmartDiagnostics.HealthScoreResult nvmeResult =
                SmartDiagnostics.calculateHealthScore(0, 100, 0, 100, 0, 0, 56, DeviceType.NVME_SSD);
        assertEquals(100, nvmeResult.score());
        assertTrue(nvmeResult.warnings().isEmpty());
    }

    @Test
    public void testPolicyForDriveResolution() {
        policyManager.resetToDefaults();

        ThermalPolicy nvmePolicy = policyManager.getPolicyForDrive("Samsung 990 Pro 2TB NVMe", "/dev/rdisk1", 2000L * 1024 * 1024 * 1024);
        assertEquals(DeviceType.NVME_SSD, nvmePolicy.deviceType());
        assertEquals(70, nvmePolicy.autoPauseCelsius());
        assertEquals(52, nvmePolicy.resumeCelsius());

        ThermalPolicy hddPolicy = policyManager.getPolicyForDrive("WD Red Plus 4TB HDD", "/dev/rdisk3", 4000L * 1024 * 1024 * 1024);
        assertEquals(DeviceType.MAGNETIC_HDD, hddPolicy.deviceType());
        assertEquals(50, hddPolicy.autoPauseCelsius());
        assertEquals(40, hddPolicy.resumeCelsius());

        ThermalPolicy usbPolicy = policyManager.getPolicyForDrive("SanDisk Cruzer Glide", "/dev/rdisk5", 16L * 1024 * 1024 * 1024);
        assertEquals(DeviceType.USB_FLASH, usbPolicy.deviceType());
        assertEquals(55, usbPolicy.autoPauseCelsius());
        assertEquals(42, usbPolicy.resumeCelsius());
    }

    @Test
    public void testPolicyChangeListener() {
        java.util.concurrent.atomic.AtomicBoolean listenerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable listener = () -> listenerCalled.set(true);

        policyManager.addChangeListener(listener);
        policyManager.setPolicy(DeviceType.USB_FLASH, 56, 43);

        assertTrue(listenerCalled.get());

        // Remove listener
        listenerCalled.set(false);
        policyManager.removeChangeListener(listener);
        policyManager.setPolicy(DeviceType.USB_FLASH, 57, 44);

        assertFalse(listenerCalled.get());
    }
}
