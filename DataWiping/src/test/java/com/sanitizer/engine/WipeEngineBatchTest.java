package com.sanitizer.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WipeEngine Concurrent Batch Queue Unit Tests")
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
                null,
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
                null,
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
        assertThat(WipeEngine.getActiveTaskCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cancel active wiping task removes task from queue and returns status")
    void testCancelWipeTask() {
        Future<Boolean> task = WipeEngine.submitBatchWipeTask(
                "/dev/rdisk993",
                500L * 1024 * 1024,
                WipeEngine.WipeStandard.DOD_5220_22_M,
                true,
                null,
                null,
                null
        );

        assertThat(task).isNotNull();

        boolean cancelled = WipeEngine.cancelWipeTask("/dev/rdisk993");
        assertThat(cancelled).isTrue();
        assertThat(WipeEngine.isTaskRunning("/dev/rdisk993")).isFalse();
    }
}
