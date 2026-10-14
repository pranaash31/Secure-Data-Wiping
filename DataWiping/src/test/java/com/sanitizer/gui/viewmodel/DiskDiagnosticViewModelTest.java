package com.sanitizer.gui.viewmodel;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

@DisplayName("DiskDiagnosticViewModel Unit Tests — Decoupled Telemetry & Math")
class DiskDiagnosticViewModelTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @AfterEach
    void tearDown() {
        UsbDetector.clearMockDrivesOverride();
        SmartDiagnostics.clearMockReports();
    }

    @Test
    @DisplayName("Verify block calculation math: sizeBytes / 512")
    void testCalculateTotalBlocks() {
        assertThat(DiskDiagnosticViewModel.calculateTotalBlocks(0)).isEqualTo(0);
        assertThat(DiskDiagnosticViewModel.calculateTotalBlocks(-1024)).isEqualTo(0);
        assertThat(DiskDiagnosticViewModel.calculateTotalBlocks(512)).isEqualTo(1);
        assertThat(DiskDiagnosticViewModel.calculateTotalBlocks(1024)).isEqualTo(2);
        // 16 GB = 17,179,869,184 bytes -> 33,554,432 sectors
        long sixteenGb = 16L * 1024 * 1024 * 1024;
        assertThat(DiskDiagnosticViewModel.calculateTotalBlocks(sixteenGb)).isEqualTo(33554432L);
    }

    @Test
    @DisplayName("Verify estimated runtime calculation: powerOnHours / (24 * 365.25)")
    void testCalculateYearsRuntime() {
        assertThat(DiskDiagnosticViewModel.calculateYearsRuntime(0)).isEqualTo(0.1);
        assertThat(DiskDiagnosticViewModel.calculateYearsRuntime(-100)).isEqualTo(0.1);

        // 8,766 hours = exactly 1 year of 365.25 days
        double oneYearHours = 24.0 * 365.25;
        assertThat(DiskDiagnosticViewModel.calculateYearsRuntime((long) oneYearHours)).isCloseTo(1.0, offset(0.001));

        // 17,532 hours = 2 years
        assertThat(DiskDiagnosticViewModel.calculateYearsRuntime((long) (oneYearHours * 2))).isCloseTo(2.0, offset(0.001));
    }

    @Test
    @DisplayName("Verify sector defect threshold evaluation")
    void testHasSectorDefects() {
        assertThat(DiskDiagnosticViewModel.hasSectorDefects(0, 0)).isFalse();
        assertThat(DiskDiagnosticViewModel.hasSectorDefects(1, 0)).isTrue();
        assertThat(DiskDiagnosticViewModel.hasSectorDefects(0, 4)).isTrue();
        assertThat(DiskDiagnosticViewModel.hasSectorDefects(3, 8)).isTrue();
    }

    @Test
    @DisplayName("Verify flash wear leveling threshold evaluation")
    void testIsFlashWearCritical() {
        assertThat(DiskDiagnosticViewModel.isFlashWearCritical(100)).isFalse();
        assertThat(DiskDiagnosticViewModel.isFlashWearCritical(50)).isFalse();
        assertThat(DiskDiagnosticViewModel.isFlashWearCritical(49)).isTrue();
        assertThat(DiskDiagnosticViewModel.isFlashWearCritical(10)).isTrue();
        assertThat(DiskDiagnosticViewModel.isFlashWearCritical(0)).isTrue();
    }

    @Test
    @DisplayName("Verify thermal message domain evaluation")
    void testEvaluateThermalMessage() {
        assertThat(DiskDiagnosticViewModel.evaluateThermalMessage(32))
                .contains("safe thermal limits");
        assertThat(DiskDiagnosticViewModel.evaluateThermalMessage(48))
                .contains("ELEVATED TEMPERATURE");
        assertThat(DiskDiagnosticViewModel.evaluateThermalMessage(55))
                .contains("ELEVATED TEMPERATURE");
        assertThat(DiskDiagnosticViewModel.evaluateThermalMessage(60))
                .contains("CRITICAL THERMAL LEVEL");
        assertThat(DiskDiagnosticViewModel.evaluateThermalMessage(75))
                .contains("CRITICAL THERMAL LEVEL");
    }

    @Test
    @DisplayName("Verify hardware listener registration and unregistration lifecycle")
    void testListenerLifecycle() {
        DiskDiagnosticViewModel vm = new DiskDiagnosticViewModel();
        assertThat(vm.isListening()).isFalse();

        vm.startListening();
        assertThat(vm.isListening()).isTrue();

        // Idempotent start
        vm.startListening();
        assertThat(vm.isListening()).isTrue();

        vm.stopListening();
        assertThat(vm.isListening()).isFalse();

        // Idempotent stop
        vm.stopListening();
        assertThat(vm.isListening()).isFalse();

        vm.startListening();
        assertThat(vm.isListening()).isTrue();
        vm.dispose();
        assertThat(vm.isListening()).isFalse();
    }

    @Test
    @DisplayName("Verify default empty state when no drives are connected")
    void testEmptyStateProperties() {
        DiskDiagnosticViewModel vm = new DiskDiagnosticViewModel();
        vm.updateDrives(List.of());

        assertThat(vm.getDrives()).isEmpty();
        assertThat(vm.selectedDriveProperty().get()).isNull();
        assertThat(vm.modelProperty().get()).isEqualTo("No drive connected");
        assertThat(vm.serialProperty().get()).isEqualTo("--");
        assertThat(vm.capacityProperty().get()).isEqualTo("--");
        assertThat(vm.totalBlocksProperty().get()).isEqualTo("--");
        assertThat(vm.healthValueProperty().get()).isEqualTo("-- / 100");
        assertThat(vm.healthScoreBadgeProperty().get()).isEqualTo("NO DEVICE");
        assertThat(vm.isWipeAllowedProperty().get()).isFalse();
        assertThat(vm.preWipeVerdictProperty().get()).contains("WAITING FOR DEVICE");
        assertThat(vm.wearProgressProperty().get()).isEqualTo(0.0);
        assertThat(vm.interfaceAnomalyVisibleProperty().get()).isFalse();
        assertThat(vm.getSmartAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Verify loaded diagnostics properties and formatted outputs for healthy drive")
    void testHealthyDriveDiagnostics() {
        UsbDetector.UsbDriveInfo mockDrive = new UsbDetector.UsbDriveInfo(
                "SanDisk Ultra", "SD-987654", 32L * 1024 * 1024 * 1024, "32.0 GB", "/dev/rdisk4"
        );

        SmartDiagnostics.HealthScoreResult health = new SmartDiagnostics.HealthScoreResult(
                95, SmartDiagnostics.HealthStatus.HEALTHY, List.of(), true, "Drive is in optimal condition."
        );

        SmartDiagnostics.SmartReport report = new SmartDiagnostics.SmartReport(
                "/dev/rdisk4", "SanDisk Ultra", "SD-987654",
                0, 98, 0, 8766, 36, 12, 0, 0, 0,
                health,
                SmartDiagnostics.ThermalStatus.NORMAL,
                null,
                List.of(new SmartDiagnostics.SmartAttribute(1, "Raw Read Error Rate", "100", "100", "50", "0", "OK")),
                true
        );

        SmartDiagnostics.setMockReport("/dev/rdisk4", report);

        DiskDiagnosticViewModel vm = new DiskDiagnosticViewModel();
        vm.updateDrives(List.of(mockDrive));

        assertThat(vm.modelProperty().get()).isEqualTo("SanDisk Ultra");
        assertThat(vm.serialProperty().get()).isEqualTo("SD-987654");
        assertThat(vm.totalBlocksProperty().get()).contains("sectors");
        assertThat(vm.healthValueProperty().get()).isEqualTo("95 / 100");
        assertThat(vm.healthScoreBadgeProperty().get()).isEqualTo("HEALTHY");
        assertThat(vm.isWipeAllowedProperty().get()).isTrue();
        assertThat(vm.preWipeVerdictProperty().get()).contains("PASSED (95/100 - HEALTHY)");
        assertThat(vm.tempValueProperty().get()).isEqualTo("36 °C");
        assertThat(vm.thermalBadgeProperty().get()).isEqualTo("NORMAL");
        assertThat(vm.yearsRuntimeProperty().get()).isCloseTo(1.0, offset(0.01));
        assertThat(vm.sectorsValueProperty().get()).isEqualTo("0 BAD / 0 REALLOC");
        assertThat(vm.wearProgressProperty().get()).isEqualTo(0.98);
        assertThat(vm.wearValueProperty().get()).isEqualTo("98% Remaining Life");
        assertThat(vm.interfaceAnomalyVisibleProperty().get()).isFalse();
        assertThat(vm.getSmartAttributes()).hasSize(1);
    }

    @Test
    @DisplayName("Verify pre-wipe critical warning and interface anomaly banner presentation")
    void testCriticalDriveAndInterfaceAnomaly() {
        UsbDetector.UsbDriveInfo mockDrive = new UsbDetector.UsbDriveInfo(
                "Failing Drive", "BAD-111", 16L * 1024 * 1024 * 1024, "16.0 GB", "/dev/rdisk5"
        );

        SmartDiagnostics.HealthScoreResult health = new SmartDiagnostics.HealthScoreResult(
                35, SmartDiagnostics.HealthStatus.CRITICAL, List.of("High Bad Sectors", "Severe Wear"), false, "Drive replacement required."
        );

        SmartDiagnostics.InterfaceAnomalyResult iface = new SmartDiagnostics.InterfaceAnomalyResult(
                true, SmartDiagnostics.InterfaceSeverity.CRITICAL, 42, 15, "Bad cable connection", "Reconnect to another USB port"
        );

        SmartDiagnostics.SmartReport report = new SmartDiagnostics.SmartReport(
                "/dev/rdisk5", "Failing Drive", "BAD-111",
                25, 30, 12, 26298, 62, 150, 42, 42, 15,
                health,
                SmartDiagnostics.ThermalStatus.CRITICAL,
                iface,
                List.of(),
                true
        );

        SmartDiagnostics.setMockReport("/dev/rdisk5", report);

        DiskDiagnosticViewModel vm = new DiskDiagnosticViewModel();
        vm.updateDrives(List.of(mockDrive));

        assertThat(vm.healthValueProperty().get()).isEqualTo("35 / 100");
        assertThat(vm.healthScoreBadgeProperty().get()).isEqualTo("CRITICAL");
        assertThat(vm.isWipeAllowedProperty().get()).isFalse();
        assertThat(vm.preWipeVerdictProperty().get()).contains("CRITICAL WARNING (35/100 - CRITICAL)");
        assertThat(vm.tempValueProperty().get()).isEqualTo("62 °C");
        assertThat(vm.tempDescProperty().get()).contains("CRITICAL THERMAL LEVEL");
        assertThat(vm.wearProgressProperty().get()).isEqualTo(0.30);
        assertThat(vm.wearDescProperty().get()).contains("High flash wear");
        assertThat(vm.sectorsValueProperty().get()).isEqualTo("12 BAD / 25 REALLOC");
        assertThat(vm.sectorsDescProperty().get()).contains("Sector defects detected");

        // Interface anomaly banner
        assertThat(vm.interfaceAnomalyVisibleProperty().get()).isTrue();
        assertThat(vm.anomalyTitleProperty().get()).contains("INTERFACE ANOMALY DETECTED");
        assertThat(vm.anomalyTitleProperty().get()).contains("Critical Interface Flaw");
        assertThat(vm.anomalyDiagnosisProperty().get()).contains("CRC Errors (ID 199): 42");
        assertThat(vm.anomalyPromptProperty().get()).contains("Reconnect to another USB port");
    }
}
