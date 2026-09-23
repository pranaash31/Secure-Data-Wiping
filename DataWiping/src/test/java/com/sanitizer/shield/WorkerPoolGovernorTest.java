package com.sanitizer.shield;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkerPoolGovernor Concurrency Regulator Tests")
class WorkerPoolGovernorTest {

    private WorkerPoolGovernor governor;

    @BeforeEach
    void setUp() {
        governor = WorkerPoolGovernor.getInstance();
        governor.reset();
    }

    @Test
    @DisplayName("Acquire and release concurrency slots dynamically")
    void testSlotAcquisitionAndRelease() {
        assertThat(governor.getActiveSlotCount()).isZero();

        boolean acquired1 = governor.acquireSlot("/dev/rdisk3");
        boolean acquired2 = governor.acquireSlot("/dev/rdisk4");

        assertThat(acquired1).isTrue();
        assertThat(acquired2).isTrue();
        assertThat(governor.getActiveSlotCount()).isEqualTo(2);

        // Re-acquiring same drive should succeed without double-counting
        boolean reacquired = governor.acquireSlot("/dev/rdisk3");
        assertThat(reacquired).isTrue();
        assertThat(governor.getActiveSlotCount()).isEqualTo(2);

        governor.releaseSlot("/dev/rdisk3");
        assertThat(governor.getActiveSlotCount()).isEqualTo(1);

        governor.releaseSlot("/dev/rdisk4");
        assertThat(governor.getActiveSlotCount()).isZero();
    }

    @Test
    @DisplayName("Null system paths safely rejected without throwing")
    void testNullSlotHandling() {
        boolean acquired = governor.acquireSlot(null);
        assertThat(acquired).isFalse();
        governor.releaseSlot(null);
        assertThat(governor.getActiveSlotCount()).isZero();
    }

    @Test
    @DisplayName("Capacity, utilization, and metrics calculations")
    void testMetricsAndStatusSummary() {
        int capacity = governor.getConfiguredPoolCapacity();
        assertThat(capacity).isGreaterThanOrEqualTo(8);

        governor.acquireSlot("/dev/rdisk5");
        int util = governor.getUtilizationPercent();
        assertThat(util).isGreaterThan(0);

        String summary = governor.getStatusSummary();
        assertThat(summary)
                .contains("Slots Active")
                .contains("Host CPU:");

        double load = governor.getCpuLoad();
        assertThat(load).isBetween(0.0, 1.0);

        long freeMem = governor.getAvailableMemoryBytes();
        assertThat(freeMem).isGreaterThan(0L);
    }
}
