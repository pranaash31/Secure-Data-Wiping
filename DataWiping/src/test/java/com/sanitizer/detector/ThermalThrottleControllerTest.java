package com.sanitizer.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Thermal Throttling Auto-Recovery:
 * Dynamic I/O queue depth throttling across 65°C threshold and graduated ramp-up recovery once cooled.
 */
class ThermalThrottleControllerTest {

    private ThermalThrottleController controller;
    private ThermalPolicy nvmePolicy;
    private ThermalPolicy usbPolicy;

    @BeforeEach
    void setUp() {
        controller = new ThermalThrottleController();
        // NVMe: Auto-Pause=70°C, Throttle=65°C, Resume=52°C
        nvmePolicy = ThermalPolicy.of(DeviceType.NVME_SSD, 70, 52, 65);
        // USB: Auto-Pause=55°C, Throttle=52°C, Resume=42°C
        usbPolicy = ThermalPolicy.of(DeviceType.USB_FLASH, 55, 42, 52);
    }

    @Test
    @DisplayName("Normal temperature (<65°C) operates at 100% throughput with 0ms delay")
    void testNormalOperationUnder65C() {
        var decision = controller.evaluate(42, nvmePolicy);

        assertEquals(ThermalThrottleController.ThrottleState.NORMAL, decision.state());
        assertEquals(0, decision.throttlePercent());
        assertEquals(0, decision.delayMillis());
        assertEquals(100, decision.queueDepthPercent());
        assertFalse(controller.isThrottled());
        assertFalse(controller.isPaused());
    }

    @Test
    @DisplayName("Crossing 65°C dynamically throttles I/O queue depth and introduces micro-sleep pacing")
    void testDynamicThrottlingOver65C() {
        // Evaluate at exactly 65°C (throttle boundary)
        var decision65 = controller.evaluate(65, nvmePolicy);

        assertEquals(ThermalThrottleController.ThrottleState.THROTTLED, decision65.state());
        assertTrue(decision65.throttlePercent() >= 30, "Should apply at least 30% throttle");
        assertTrue(decision65.delayMillis() >= 15, "Should insert at least 15ms pacing delay");
        assertTrue(decision65.queueDepthPercent() <= 70, "Queue depth should be reduced");
        assertTrue(controller.isThrottled());
        assertNotNull(decision65.logMessage(), "First throttle transition should generate log notification");

        // Further heating to 68°C should increase throttle intensity
        var decision68 = controller.evaluate(68, nvmePolicy);

        assertEquals(ThermalThrottleController.ThrottleState.THROTTLED, decision68.state());
        assertTrue(decision68.throttlePercent() > decision65.throttlePercent(), "Higher temperature should yield higher throttle percentage");
        assertTrue(decision68.delayMillis() > decision65.delayMillis(), "Higher temperature should yield higher pacing delay");
        assertTrue(decision68.queueDepthPercent() < decision65.queueDepthPercent(), "Queue depth should decrease under higher heat");
    }

    @Test
    @DisplayName("Reaching Auto-Pause threshold (>=70°C) suspends I/O completely (100% throttle)")
    void testCriticalAutoPause() {
        var pauseDecision = controller.evaluate(70, nvmePolicy);

        assertEquals(ThermalThrottleController.ThrottleState.PAUSED_CRITICAL, pauseDecision.state());
        assertEquals(100, pauseDecision.throttlePercent());
        assertEquals(0, pauseDecision.queueDepthPercent());
        assertTrue(controller.isPaused());
        assertNotNull(pauseDecision.logMessage());
    }

    @Test
    @DisplayName("Auto-Recovery gracefully ramps throughput back up in stages once cooled below resume threshold")
    void testAutoRecoveryRampUp() {
        // Step 1: Push into critical pause at 70°C
        controller.evaluate(70, nvmePolicy);
        assertTrue(controller.isPaused());

        // Step 2: Drive cools down to 50°C (below resume threshold 52°C)
        // Stage 1 Ramp-Up (50% throughput)
        var stage1 = controller.evaluate(50, nvmePolicy);
        assertEquals(ThermalThrottleController.ThrottleState.RAMPING_UP, stage1.state());
        assertEquals(50, stage1.throttlePercent());
        assertEquals(50, stage1.queueDepthPercent());
        assertEquals(1, controller.getRampStage());
        assertTrue(controller.isThrottled());

        // Stage 2 Ramp-Up (75% throughput)
        var stage2 = controller.evaluate(50, nvmePolicy);
        assertEquals(ThermalThrottleController.ThrottleState.RAMPING_UP, stage2.state());
        assertEquals(25, stage2.throttlePercent());
        assertEquals(75, stage2.queueDepthPercent());
        assertEquals(2, controller.getRampStage());

        // Stage 3 Ramp-Up (100% full recovery to NORMAL)
        var stage3 = controller.evaluate(50, nvmePolicy);
        assertEquals(ThermalThrottleController.ThrottleState.NORMAL, stage3.state());
        assertEquals(0, stage3.throttlePercent());
        assertEquals(100, stage3.queueDepthPercent());
        assertEquals(0, controller.getRampStage());
        assertEquals(1, controller.getRecoveryCount());
        assertFalse(controller.isThrottled());
    }

    @Test
    @DisplayName("Effective block buffer size scales down during heavy throttling")
    void testCalculateEffectiveBlockSize() {
        int base2MB = 2 * 1024 * 1024;

        assertEquals(base2MB, ThermalThrottleController.calculateEffectiveBlockSize(base2MB, 0));
        assertEquals(1024 * 1024, ThermalThrottleController.calculateEffectiveBlockSize(base2MB, 30));
        assertEquals(512 * 1024, ThermalThrottleController.calculateEffectiveBlockSize(base2MB, 50));
        assertEquals(256 * 1024, ThermalThrottleController.calculateEffectiveBlockSize(base2MB, 80));
    }

    @Test
    @DisplayName("Reset restores controller state cleanly")
    void testReset() {
        controller.evaluate(68, nvmePolicy);
        assertTrue(controller.isThrottled());

        controller.reset();
        assertEquals(ThermalThrottleController.ThrottleState.NORMAL, controller.getCurrentState());
        assertEquals(0, controller.getCurrentThrottlePercent());
        assertEquals(0, controller.getCurrentDelayMillis());
        assertEquals(-1, controller.getPeakRecordedTemp());
    }
}
