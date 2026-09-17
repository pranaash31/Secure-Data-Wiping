package com.sanitizer.gui.components;

import com.sanitizer.engine.WipeMetrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sector Heatmap Visualizer Unit Tests")
class SectorHeatmapTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify BlockState enumeration values and semantics")
    void testBlockStateEnum() {
        assertThat(SectorHeatmapComponent.BlockState.values()).containsExactly(
                SectorHeatmapComponent.BlockState.DIRTY,
                SectorHeatmapComponent.BlockState.ACTIVE_HEAD,
                SectorHeatmapComponent.BlockState.PATTERN,
                SectorHeatmapComponent.BlockState.ZEROED,
                SectorHeatmapComponent.BlockState.IDLE
        );
    }

    @Test
    @DisplayName("Verify component initialization, reset, and progress transitions")
    void testComponentProgressTransitions() {
        SectorHeatmapComponent component = new SectorHeatmapComponent(50, 10, 12, 2, 2);
        assertThat(component).isNotNull();

        // Simulate 0%
        component.reset(1_000_000_000L);

        // Simulate 50% on DoD Pass 1
        WipeMetrics metrics50 = new WipeMetrics(
                "/dev/rdisk99",
                50.0,
                1,
                3,
                "Zero Fill (0x00)",
                500_000_000L,
                1_000_000_000L,
                48.5,
                20
        );
        component.updateProgress(metrics50);

        // Simulate DoD Pass 2 (Cryptographic Random)
        WipeMetrics metricsPass2 = new WipeMetrics(
                "/dev/rdisk99",
                60.0,
                2,
                3,
                "Cryptographic Random",
                600_000_000L,
                1_000_000_000L,
                55.0,
                15
        );
        component.updateProgress(metricsPass2);

        // Simulate Completion
        component.setCompleted();

        // Simulate Abort
        component.setAborted();
    }
}
