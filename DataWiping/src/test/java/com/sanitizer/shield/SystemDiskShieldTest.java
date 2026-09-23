package com.sanitizer.shield;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemDiskShield Multi-OS Guardrail Tests")
class SystemDiskShieldTest {

    @ParameterizedTest(name = "Target path ''{0}'' must be flagged unsafe by SystemDiskShield")
    @ValueSource(strings = {
            "disk0",
            "/dev/disk0",
            "/dev/rdisk0",
            "/dev/disk0s1",
            "/dev/rdisk0s2",
            "/dev/disk0s5",
            "\\\\.\\PhysicalDrive0",
            "PhysicalDrive0",
            "C:",
            "c:\\",
            "C:\\Windows",
            "/dev/sda",
            "/dev/sda1",
            "/dev/nvme0n1",
            "/dev/nvme0n1p1"
    })
    void testBlockedSystemDiskPatterns(String path) {
        SystemDiskShield.SafetyVerdict verdict = SystemDiskShield.evaluate(path);
        assertThat(verdict.isSafe()).isFalse();
        assertThat(verdict.blockReason()).contains("CRITICAL SAFETY SHIELD");
    }

    @Test
    @DisplayName("Null or blank drive paths are safely rejected")
    void testNullOrBlankPathHandling() {
        SystemDiskShield.SafetyVerdict nullVerdict = SystemDiskShield.evaluate(null);
        assertThat(nullVerdict.isSafe()).isFalse();
        assertThat(nullVerdict.blockReason()).contains("Invalid or null target path");

        SystemDiskShield.SafetyVerdict blankVerdict = SystemDiskShield.evaluate("   ");
        assertThat(blankVerdict.isSafe()).isFalse();
    }

    @Test
    @DisplayName("Safe secondary external drive paths pass evaluation")
    void testSafeSecondaryDrives() {
        SystemDiskShield.SafetyVerdict verdict = SystemDiskShield.evaluate("/dev/rdisk4");
        assertThat(verdict.isSafe()).isTrue();
        assertThat(verdict.blockReason()).isNull();

        SystemDiskShield.SafetyVerdict winVerdict = SystemDiskShield.evaluate("\\\\.\\PhysicalDrive2");
        assertThat(winVerdict.isSafe()).isTrue();
    }
}
