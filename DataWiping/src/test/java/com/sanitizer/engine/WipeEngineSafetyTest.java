package com.sanitizer.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WipeEngine Safety Shield Unit Tests")
class WipeEngineSafetyTest {

    @ParameterizedTest(name = "Target system disk path: {0} must be BLOCKED by Safety Shield")
    @ValueSource(strings = {
            "disk0",
            "rdisk0",
            "/dev/disk0",
            "/dev/rdisk0",
            "/dev/disk0s1",
            "/dev/rdisk0s2",
            "/dev/disk0s5",
            "/Volumes/Macintosh HD/disk0"
    })
    void testSafetyShieldBlocksPrimarySystemDrive(String systemPath) {
        List<String> logMessages = new ArrayList<>();
        AtomicBoolean progressCalled = new AtomicBoolean(false);

        boolean result = WipeEngine.executeWipe(
                systemPath,
                16L * 1024 * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                pct -> progressCalled.set(true),
                logMessages::add
        );

        // Safety shield MUST return false
        assertThat(result).isFalse();

        // Progress callback MUST NOT be called for blocked drive
        assertThat(progressCalled.get()).isFalse();

        // Log callback MUST contain critical safety shield warning
        assertThat(logMessages)
                .isNotEmpty()
                .anyMatch(msg -> msg.contains("CRITICAL SAFETY SHIELD") && msg.contains(systemPath));
    }

    @Test
    @DisplayName("Safety Shield blocks DoD 5220.22-M wipe on rdisk0")
    void testSafetyShieldBlocksDoDWipeOnRdisk0() {
        List<String> logMessages = new ArrayList<>();

        boolean result = WipeEngine.executeWipe(
                "/dev/rdisk0",
                32L * 1024 * 1024 * 1024,
                WipeEngine.WipeStandard.DOD_5220_22_M,
                true,
                null,
                logMessages::add
        );

        assertThat(result).isFalse();
        assertThat(logMessages)
                .anyMatch(msg -> msg.contains("CRITICAL SAFETY SHIELD: Primary system drive (/dev/rdisk0) blocked from wiping!"));
    }

    @Test
    @DisplayName("Wipe Engine accepts valid non-system drive path structure")
    void testNonSystemDrivePathEvaluation() {
        List<String> logMessages = new ArrayList<>();

        // Test mode attempt on dummy non-existing device path (will execute dd or fail gracefully at dd execution, NOT blocked by shield)
        WipeEngine.executeWipe(
                "/dev/rdisk999",
                1L * 1024 * 1024 * 1024,
                WipeEngine.WipeStandard.NIST_800_88_CLEAR,
                true,
                null,
                logMessages::add
        );

        // Should not be blocked by safety shield log message
        assertThat(logMessages)
                .noneMatch(msg -> msg.contains("CRITICAL SAFETY SHIELD"));
    }
}
