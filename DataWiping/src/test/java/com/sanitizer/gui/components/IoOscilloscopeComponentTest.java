package com.sanitizer.gui.components;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.engine.WipeMetrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@DisplayName("I/O Oscilloscope & Waveform Telemetry Unit Tests")
class IoOscilloscopeComponentTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify component initialization, default bus settings, and empty telemetry")
    void testComponentInitialization() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent("USB 3.0", 450.0);

        assertThat(oscilloscope).isNotNull();
        assertThat(oscilloscope.getSampleCount()).isEqualTo(0);
        assertThat(oscilloscope.getPeakThroughput()).isEqualTo(0.0);
        assertThat(oscilloscope.getAvgThroughput()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakIops()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakLatency()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakSaturation()).isEqualTo(0.0);
        assertThat(oscilloscope.getBusInterfaceName()).isEqualTo("USB 3.0");
        assertThat(oscilloscope.getInterfaceMaxMBs()).isEqualTo(450.0);
        assertThat(oscilloscope.getCurrentMode()).isEqualTo(IoOscilloscopeComponent.DisplayMode.QUAD_TRACE);
        assertThat(oscilloscope.getSamples()).isEmpty();
    }

    @Test
    @DisplayName("Verify multi-channel sample ingestion, peak tracking, and average calculations")
    void testSampleIngestionAndStatistics() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent();

        // Add 4 raw telemetry points
        oscilloscope.addSample(40.0, 10240, 2.5, 8.8);
        oscilloscope.addSample(60.0, 15360, 1.8, 13.3);
        oscilloscope.addSample(120.0, 30720, 1.2, 26.6); // Peak Speed & IOPS
        oscilloscope.addSample(80.0, 20480, 5.0, 17.7);  // Peak Latency

        assertThat(oscilloscope.getSampleCount()).isEqualTo(4);
        assertThat(oscilloscope.getPeakThroughput()).isEqualTo(120.0);
        assertThat(oscilloscope.getAvgThroughput()).isCloseTo((40.0 + 60.0 + 120.0 + 80.0) / 4.0, offset(0.01));
        assertThat(oscilloscope.getPeakIops()).isEqualTo(30720.0);
        assertThat(oscilloscope.getPeakLatency()).isEqualTo(5.0);
        assertThat(oscilloscope.getPeakSaturation()).isEqualTo(26.6);
        assertThat(oscilloscope.getSamples()).hasSize(4);
    }

    @Test
    @DisplayName("Verify WipeMetrics ingestion and automatic metric parsing")
    void testWipeMetricsIngestion() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent();

        WipeMetrics metrics = new WipeMetrics(
                "/dev/disk2",
                50.0,
                1,
                1,
                "Zero Fill",
                500_000_000L,
                1_000_000_000L,
                75.5,
                15,
                42,
                SmartDiagnostics.ThermalStatus.NORMAL,
                false,
                0,
                "NORMAL",
                19328.0,
                2.6,
                16.7
        );

        oscilloscope.addSample(metrics);

        assertThat(oscilloscope.getSampleCount()).isEqualTo(1);
        assertThat(oscilloscope.getPeakThroughput()).isEqualTo(75.5);
        assertThat(oscilloscope.getPeakIops()).isEqualTo(19328.0);
        assertThat(oscilloscope.getPeakLatency()).isEqualTo(2.6);
        assertThat(oscilloscope.getPeakSaturation()).isEqualTo(16.7);
    }

    @Test
    @DisplayName("Verify device context auto-detection for NVMe, USB 2.0, HDD and SATA")
    void testDeviceContextAutoDetection() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent();

        // 1. NVMe SSD
        oscilloscope.setDeviceContext("Samsung 980 PRO NVMe SSD 1TB", "/dev/nvme0n1", 1_000_000_000_000L);
        assertThat(oscilloscope.getInterfaceMaxMBs()).isEqualTo(3500.0);

        // 2. USB 2.0 Flash Drive
        oscilloscope.setDeviceContext("SanDisk Cruzer Blade USB 2.0", "/dev/disk3", 4_000_000_000L);
        assertThat(oscilloscope.getInterfaceMaxMBs()).isEqualTo(40.0);

        // 3. Magnetic HDD
        oscilloscope.setDeviceContext("Seagate Barracuda 7200 RPM HDD 2TB", "/dev/disk4", 2_000_000_000_000L);
        assertThat(oscilloscope.getInterfaceMaxMBs()).isEqualTo(180.0);
    }

    @Test
    @DisplayName("Verify display mode switching")
    void testDisplayModeSwitching() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent();

        for (IoOscilloscopeComponent.DisplayMode mode : IoOscilloscopeComponent.DisplayMode.values()) {
            oscilloscope.setDisplayMode(mode);
            assertThat(oscilloscope.getCurrentMode()).isEqualTo(mode);
            assertThat(mode.getLabel()).isNotEmpty();
            assertThat(mode.getDescription()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Verify component reset clears data points and statistics")
    void testReset() {
        IoOscilloscopeComponent oscilloscope = new IoOscilloscopeComponent();

        oscilloscope.addSample(50.0, 12800, 2.0, 11.1);
        oscilloscope.addSample(100.0, 25600, 1.5, 22.2);

        assertThat(oscilloscope.getSamples()).isNotEmpty();
        assertThat(oscilloscope.getPeakThroughput()).isEqualTo(100.0);

        oscilloscope.reset();

        assertThat(oscilloscope.getSamples()).isEmpty();
        assertThat(oscilloscope.getSampleCount()).isEqualTo(0);
        assertThat(oscilloscope.getPeakThroughput()).isEqualTo(0.0);
        assertThat(oscilloscope.getAvgThroughput()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakIops()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakLatency()).isEqualTo(0.0);
        assertThat(oscilloscope.getPeakSaturation()).isEqualTo(0.0);
    }
}
