package com.sanitizer.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WipeEngine Concurrent Batch Queue & Telemetry Unit Tests")
class WipeEngineBatchTest {

    @Test
    @DisplayName("Submit concurrent batch wiping tasks and receive completion callbacks")
    void testSubmitBatchWipeTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean drive1Completed = new AtomicBoolean(false);
        AtomicBoolean drive2Completed = new AtomicBoolean(false);

        Future<Boolean> task1 = WipeEngine.submitBatchWipeTask(
                "/dev/rdisk991",
                100L * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                (ConsumerDouble) null,
                null,
                success -> {
                    drive1Completed.set(true);
                    latch.countDown();
                }
        );

        Future<Boolean> task2 = WipeEngine.submitBatchWipeTask(
                "/dev/rdisk992",
                100L * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                (ConsumerDouble) null,
                null,
                success -> {
                    drive2Completed.set(true);
                    latch.countDown();
                }
        );

        assertThat(task1).isNotNull();
        assertThat(task2).isNotNull();

        boolean completedInTime = latch.await(10, TimeUnit.SECONDS);
        assertThat(completedInTime).isTrue();

        assertThat(drive1Completed.get()).isTrue();
        assertThat(drive2Completed.get()).isTrue();
    }

    @FunctionalInterface
    private interface ConsumerDouble extends java.util.function.Consumer<Double> {}

    @Test
    @DisplayName("Verify concurrent wiping of up to 8 drives simultaneously on multi-threaded pool")
    void testEightConcurrentWipeTasks() throws Exception {
        int driveCount = 8;
        CountDownLatch latch = new CountDownLatch(driveCount);
        List<AtomicBoolean> completions = new ArrayList<>();
        List<Future<Boolean>> tasks = new ArrayList<>();

        for (int i = 1; i <= driveCount; i++) {
            AtomicBoolean completed = new AtomicBoolean(false);
            completions.add(completed);
            String mockDisk = "/dev/rdisk" + (800 + i);

            Future<Boolean> future = WipeEngine.submitBatchWipeTaskWithMetrics(
                    mockDisk,
                    50L * 1024 * 1024,
                    WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                    true,
                    (java.util.function.Consumer<WipeMetrics>) null,
                    null,
                    success -> {
                        completed.set(true);
                        latch.countDown();
                    }
            );
            tasks.add(future);
        }

        assertThat(tasks).hasSize(driveCount);
        assertThat(WipeEngine.getThreadPoolCapacity()).isGreaterThanOrEqualTo(8);

        boolean allDone = latch.await(15, TimeUnit.SECONDS);
        assertThat(allDone).isTrue();

        for (AtomicBoolean completed : completions) {
            assertThat(completed.get()).isTrue();
        }
    }

    @Test
    @DisplayName("Granular safety abort cancels targeted drive without interrupting other active threads")
    void testGranularAbortDoesNotAffectOtherThreads() throws Exception {
        CountDownLatch survivingLatch = new CountDownLatch(2);
        AtomicBoolean driveACompleted = new AtomicBoolean(false);
        AtomicBoolean driveBCompleted = new AtomicBoolean(false);
        AtomicBoolean driveCCompleted = new AtomicBoolean(false);

        // Drive A: Runs normally
        WipeEngine.submitBatchWipeTaskWithMetrics(
                "/dev/rdisk811",
                50L * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                (java.util.function.Consumer<WipeMetrics>) null,
                null,
                success -> {
                    driveACompleted.set(true);
                    survivingLatch.countDown();
                }
        );

        // Drive B: Target for Granular Abort
        WipeEngine.submitBatchWipeTaskWithMetrics(
                "/dev/rdisk812",
                500L * 1024 * 1024,
                WipeEngine.WipeStandard.DOD_5220_22_M,
                true,
                (java.util.function.Consumer<WipeMetrics>) null,
                null,
                success -> driveBCompleted.set(success)
        );

        // Drive C: Runs normally
        WipeEngine.submitBatchWipeTaskWithMetrics(
                "/dev/rdisk813",
                50L * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                (java.util.function.Consumer<WipeMetrics>) null,
                null,
                success -> {
                    driveCCompleted.set(true);
                    survivingLatch.countDown();
                }
        );

        // Granular abort ONLY Drive B
        boolean cancelledB = WipeEngine.cancelWipeTask("/dev/rdisk812");
        assertThat(cancelledB).isTrue();
        assertThat(WipeEngine.isTaskRunning("/dev/rdisk812")).isFalse();

        // Verify Drive A and Drive C survive and complete successfully
        boolean completedSurviving = survivingLatch.await(10, TimeUnit.SECONDS);
        assertThat(completedSurviving).isTrue();
        assertThat(driveACompleted.get()).isTrue();
        assertThat(driveCCompleted.get()).isTrue();
        assertThat(driveBCompleted.get()).isFalse();
    }

    @Test
    @DisplayName("Verify WipeMetrics telemetry, speed formatters, ETA, and pass summaries")
    void testWipeMetricsTelemetryAndFormatters() {
        WipeMetrics metricsDoD = new WipeMetrics(
                "/dev/rdisk998",
                45.5,
                2,
                3,
                "Cryptographic Random",
                500_000_000L,
                1_000_000_000L,
                64.5,
                90
        );

        assertThat(metricsDoD.formattedSpeed()).isEqualTo("64.5 MB/s");
        assertThat(metricsDoD.formattedEta()).isEqualTo("01m 30s");
        assertThat(metricsDoD.formattedProgress()).isEqualTo("45.5%");
        assertThat(metricsDoD.formattedPassSummary()).isEqualTo("Pass 2/3: Cryptographic Random");

        WipeMetrics metricsNist = new WipeMetrics(
                "/dev/rdisk999",
                100.0,
                1,
                1,
                "Zero Fill (0x00)",
                1_000_000_000L,
                1_000_000_000L,
                0.0,
                0
        );

        assertThat(metricsNist.formattedSpeed()).isEqualTo("-- MB/s");
        assertThat(metricsNist.formattedEta()).isEqualTo("00:00");
        assertThat(metricsNist.formattedProgress()).isEqualTo("100.0%");
        assertThat(metricsNist.formattedPassSummary()).isEqualTo("Pass 1/1: Zero Fill (0x00)");
    }

    @Test
    @DisplayName("Verify ambient aggregate bandwidth and cumulative batch volume summation across multiple drives")
    void testAggregateTelemetryCalculations() {
        List<WipeMetrics> activeStreams = List.of(
                new WipeMetrics("/dev/rdisk1", 50.0, 1, 1, "Zero Fill", 500_000_000L, 1_000_000_000L, 52.4, 10),
                new WipeMetrics("/dev/rdisk2", 30.0, 1, 1, "Zero Fill", 300_000_000L, 1_000_000_000L, 48.6, 15),
                new WipeMetrics("/dev/rdisk3", 80.0, 1, 1, "Zero Fill", 800_000_000L, 1_000_000_000L, 65.0, 3)
        );

        double aggregateSpeed = activeStreams.stream().mapToDouble(WipeMetrics::speedMBs).sum();
        long aggregateBytesProcessed = activeStreams.stream().mapToLong(WipeMetrics::bytesProcessedInPass).sum();
        long maxEta = activeStreams.stream().mapToLong(WipeMetrics::etaSeconds).max().orElse(0);

        assertThat(aggregateSpeed).isEqualTo(166.0);
        assertThat(aggregateBytesProcessed).isEqualTo(1_600_000_000L);
        assertThat(maxEta).isEqualTo(15);
    }
}
