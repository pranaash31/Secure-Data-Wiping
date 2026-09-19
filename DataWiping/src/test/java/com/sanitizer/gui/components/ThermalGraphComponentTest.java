package com.sanitizer.gui.components;

import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@DisplayName("Thermal Sparkline & Temperature Graph Unit Tests")
class ThermalGraphComponentTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify component initialization, policy binding, and default stats")
    void testComponentInitialization() {
        ThermalPolicy policy = ThermalPolicy.defaultFor(DeviceType.NVME_SSD);
        ThermalGraphComponent graph = new ThermalGraphComponent(policy);

        assertThat(graph).isNotNull();
        assertThat(graph.getSampleCount()).isEqualTo(0);
        assertThat(graph.getPeakTemp()).isEqualTo(-1);
        assertThat(graph.getMinTemp()).isEqualTo(-1);
        assertThat(graph.getAvgTemp()).isEqualTo(0.0);
        assertThat(graph.getPauseCount()).isEqualTo(0);
        assertThat(graph.getPoints()).isEmpty();
        assertThat(graph.getEvents()).isEmpty();
    }

    @Test
    @DisplayName("Verify sample ingestion, peak tracking, min tracking, and average calculation")
    void testSampleIngestionAndStatistics() {
        ThermalGraphComponent graph = new ThermalGraphComponent();

        graph.addSample(35);
        graph.addSample(42);
        graph.addSample(58); // Peak
        graph.addSample(45);

        assertThat(graph.getSampleCount()).isEqualTo(4);
        assertThat(graph.getPeakTemp()).isEqualTo(58);
        assertThat(graph.getMinTemp()).isEqualTo(35);
        // (35 + 42 + 58 + 45) / 4 = 180 / 4 = 45.0
        assertThat(graph.getAvgTemp()).isCloseTo(45.0, offset(0.01));
        assertThat(graph.getPoints()).hasSize(4);
    }

    @Test
    @DisplayName("Verify thermal event recording (Auto-Pause and Resume milestones)")
    void testEventRecording() {
        ThermalGraphComponent graph = new ThermalGraphComponent();

        graph.addSample(40);
        graph.recordEvent(56, "AUTO_PAUSE", "Auto-Pause Threshold Exceeded");
        graph.recordEvent(42, "RESUME", "Cooldown Complete - Sanitization Resumed");

        assertThat(graph.getPauseCount()).isEqualTo(1);
        assertThat(graph.getEvents()).hasSize(2);
        assertThat(graph.getEvents().get(0).type()).isEqualTo("AUTO_PAUSE");
        assertThat(graph.getEvents().get(0).tempCelsius()).isEqualTo(56);
        assertThat(graph.getEvents().get(1).type()).isEqualTo("RESUME");
        assertThat(graph.getEvents().get(1).tempCelsius()).isEqualTo(42);
    }

    @Test
    @DisplayName("Verify component reset clears data points, statistics, and milestones")
    void testReset() {
        ThermalGraphComponent graph = new ThermalGraphComponent();

        graph.addSample(40);
        graph.addSample(55);
        graph.recordEvent(55, "AUTO_PAUSE", "Paused");

        assertThat(graph.getPoints()).isNotEmpty();
        assertThat(graph.getEvents()).isNotEmpty();

        graph.reset();

        assertThat(graph.getSampleCount()).isEqualTo(0);
        assertThat(graph.getPeakTemp()).isEqualTo(-1);
        assertThat(graph.getMinTemp()).isEqualTo(-1);
        assertThat(graph.getAvgTemp()).isEqualTo(0.0);
        assertThat(graph.getPauseCount()).isEqualTo(0);
        assertThat(graph.getPoints()).isEmpty();
        assertThat(graph.getEvents()).isEmpty();
    }
}
