package com.sanitizer.engine;

import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WipeEngine Custom Policy Execution Tests")
class WipeEnginePolicyTest {

    @Test
    @DisplayName("Safety shield blocks system drive when executing with WipePolicy")
    void testSafetyShieldBlocksWithPolicy() {
        WipePolicy policy = WipePolicyManager.getInstance().getPolicyById("bsi-vs-7pass");
        List<String> logs = new ArrayList<>();

        boolean result = WipeEngine.executeWipeWithPolicy(
                "/dev/disk0",
                16L * 1024 * 1024 * 1024,
                policy,
                true,
                null,
                logs::add
        );

        assertThat(result).isFalse();
        assertThat(logs).anyMatch(l -> l.contains("CRITICAL SAFETY SHIELD"));
    }

    @Test
    @DisplayName("Custom 4-pass policy executes correctly through WipeEngine")
    void testCustomPolicyExecution() {
        WipePolicy custom = new WipePolicy(
                "unit-test-custom-policy",
                "Unit Test 4-Pass Standard",
                "Policy for unit test verification",
                "Test Corp",
                "TEST_4P",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1 (0xAA)"),
                        new WipePass(2, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 2 (0x55)"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3 (Random)"),
                        new WipePass(4, WipePatternType.ZERO_FILL, 0x00, "Pass 4 (Zero)")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0",
                "Tester"
        );

        List<String> logs = new ArrayList<>();
        List<Double> progressList = new ArrayList<>();

        boolean success = WipeEngine.executeWipeWithPolicy(
                "/dev/rdisk_mock_policy_drive",
                4L * 1024 * 1024 * 1024,
                custom,
                true, // test mode
                metrics -> progressList.add(metrics.overallPercent()),
                logs::add
        );

        // Under test mode with mock device, dd runs
        assertThat(logs).isNotEmpty();
        assertThat(logs).anyMatch(l -> l.contains("Unit Test 4-Pass Standard"));
        assertThat(logs).anyMatch(l -> l.contains("Pass 1/4"));
    }

    @Test
    @DisplayName("Batch execution accepts custom WipePolicy")
    void testBatchExecutionWithPolicy() throws Exception {
        WipePolicy policy = WipePolicyManager.getInstance().getPolicyById("hmg-infosec-5-enhanced");
        AtomicBoolean completed = new AtomicBoolean(false);

        Future<Boolean> future = WipeEngine.submitBatchWipeTaskWithPolicy(
                "/dev/rdisk_mock_batch_drive",
                2L * 1024 * 1024 * 1024,
                policy,
                true,
                null,
                null,
                res -> completed.set(true)
        );

        assertThat(future).isNotNull();
        // Wait briefly for batch task
        boolean res = future.get();
        assertThat(completed.get()).isTrue();
    }
}
