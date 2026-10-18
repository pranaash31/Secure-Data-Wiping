package com.sanitizer.engine;

import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        WipeEngine.executeWipeWithPolicy(
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
        future.get();
        assertThat(completed.get()).isTrue();
    }

    @Test
    @DisplayName("resolveEffectiveByte correctly resolves pattern byte values")
    void testResolveEffectiveByte() {
        // Zero fill
        WipePass zeroPass = new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Zero");
        assertThat(WipeEngine.resolveEffectiveByte(zeroPass, 0x55)).isEqualTo(0x00);

        // One fill
        WipePass onePass = new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "One");
        assertThat(WipeEngine.resolveEffectiveByte(onePass, 0x00)).isEqualTo(0xFF);

        // Custom byte
        WipePass customPass = new WipePass(3, WipePatternType.CUSTOM_BYTE, 0x96, "Custom");
        assertThat(WipeEngine.resolveEffectiveByte(customPass, 0x00)).isEqualTo(0x96);

        // Complement
        WipePass compPass = new WipePass(4, WipePatternType.COMPLEMENT, -1, "Complement");
        assertThat(WipeEngine.resolveEffectiveByte(compPass, 0x00)).isEqualTo(0xFF);
        assertThat(WipeEngine.resolveEffectiveByte(compPass, 0xAA)).isEqualTo(0x55);
        assertThat(WipeEngine.resolveEffectiveByte(compPass, 0x55)).isEqualTo(0xAA);
        assertThat(WipeEngine.resolveEffectiveByte(compPass, 0xFF)).isEqualTo(0x00);

        // Pseudo-random returns -1 (dynamic stream)
        WipePass randPass = new WipePass(5, WipePatternType.PSEUDO_RANDOM, -1, "Random");
        assertThat(WipeEngine.resolveEffectiveByte(randPass, 0x00)).isEqualTo(-1);
    }

    @Test
    @DisplayName("WipeEngine writes authentic 0xFF pattern for ONE_FILL passes")
    void testOneFillPatternWritesToFile() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("test_one_fill_", ".bin");
        tempFile.deleteOnExit();

        WipePolicy oneFillPolicy = new WipePolicy(
                "test-one-fill-policy",
                "One Fill Test Standard",
                "Tests that 0xFF is authentically written to storage",
                "Test Lab",
                "ONE_FILL_TEST",
                List.of(new WipePass(1, WipePatternType.ONE_FILL, 0xFF, "All-Ones Pass")),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0",
                "Tester"
        );

        long targetSize = 4L * 1024 * 1024; // 4 MB test target
        boolean success = WipeEngine.executeWipeWithPolicy(
                tempFile.getAbsolutePath(),
                targetSize,
                oneFillPolicy,
                true,
                null,
                null
        );

        assertThat(success).isTrue();
        assertThat(tempFile.length()).isEqualTo(targetSize);

        // Verify that bytes in file are 0xFF (not 0x00)
        try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int read = fis.read(buffer);
            assertThat(read).isGreaterThan(0);
            for (int i = 0; i < read; i++) {
                assertThat(buffer[i] & 0xFF)
                        .as("Byte at index %d should be 0xFF", i)
                        .isEqualTo(0xFF);
            }
        } finally {
            tempFile.delete();
        }
    }

    @Test
    @DisplayName("WipeEngine writes authentic CUSTOM_BYTE and COMPLEMENT patterns to storage")
    void testCustomByteAndComplementPatternWrites() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("test_custom_pattern_", ".bin");
        tempFile.deleteOnExit();

        // 2-Pass Policy: Pass 1 writes 0xAA, Pass 2 writes Complement (~0xAA = 0x55)
        WipePolicy compPolicy = new WipePolicy(
                "test-complement-policy",
                "Complement Pattern Test",
                "Tests custom byte followed by bitwise complement",
                "Test Lab",
                "COMP_TEST",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1: 0xAA"),
                        new WipePass(2, WipePatternType.COMPLEMENT, -1, "Pass 2: ~Pass1 (0x55)")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0",
                "Tester"
        );

        long targetSize = 2L * 1024 * 1024; // 2 MB
        boolean success = WipeEngine.executeWipeWithPolicy(
                tempFile.getAbsolutePath(),
                targetSize,
                compPolicy,
                true,
                null,
                null
        );

        assertThat(success).isTrue();
        assertThat(tempFile.length()).isEqualTo(targetSize);

        // Verify the final pass bytes are 0x55 (~0xAA & 0xFF)
        try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int read = fis.read(buffer);
            assertThat(read).isGreaterThan(0);
            for (int i = 0; i < read; i++) {
                assertThat(buffer[i] & 0xFF)
                        .as("Byte at index %d should be inverted complement 0x55", i)
                        .isEqualTo(0x55);
            }
        } finally {
            tempFile.delete();
        }
    }
}
