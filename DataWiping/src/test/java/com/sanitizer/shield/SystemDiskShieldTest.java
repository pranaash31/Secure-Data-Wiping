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
            "C:\\Windows"
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
    @DisplayName("Safe secondary external drive paths pass evaluation (including external /dev/sda when unmounted)")
    void testSafeSecondaryDrives() {
        SystemDiskShield.SafetyVerdict verdict = SystemDiskShield.evaluate("/dev/rdisk4");
        assertThat(verdict.isSafe()).isTrue();
        assertThat(verdict.blockReason()).isNull();

        SystemDiskShield.SafetyVerdict winVerdict = SystemDiskShield.evaluate("\\\\.\\PhysicalDrive2");
        assertThat(winVerdict.isSafe()).isTrue();

        // External /dev/sda or /dev/sdb on non-root drives must NOT be blocked blindly
        SystemDiskShield.SafetyVerdict sdaVerdict = SystemDiskShield.evaluate("/dev/sda");
        assertThat(sdaVerdict.isSafe()).isTrue();
    }

    @Test
    @DisplayName("Mock blocked path mechanism properly protects flagged targets")
    void testMockBlockedPathProtection() {
        String testTarget = "/dev/mock_test_root_disk";
        SystemDiskShield.clearMockBlockedPaths();

        assertThat(SystemDiskShield.evaluate(testTarget).isSafe()).isTrue();

        SystemDiskShield.setMockBlockedPath(testTarget);
        SystemDiskShield.SafetyVerdict blockedVerdict = SystemDiskShield.evaluate(testTarget);
        assertThat(blockedVerdict.isSafe()).isFalse();
        assertThat(blockedVerdict.blockReason()).contains("Path explicitly flagged as protected system disk");

        SystemDiskShield.clearMockBlockedPaths();
        assertThat(SystemDiskShield.evaluate(testTarget).isSafe()).isTrue();
    }

    @Test
    @DisplayName("Live OS mount inspection reliably blocks active filesystem root")
    void testLiveMountInspectionProtectsRoot() {
        oshi.software.os.OperatingSystem os = new oshi.SystemInfo().getOperatingSystem();
        for (oshi.software.os.OSFileStore store : os.getFileSystem().getFileStores()) {
            if ("/".equals(store.getMount()) || "C:\\".equalsIgnoreCase(store.getMount())) {
                String volume = store.getVolume();
                if (volume != null && !volume.isBlank()) {
                    SystemDiskShield.SafetyVerdict verdict = SystemDiskShield.evaluate(volume);
                    assertThat(verdict.isSafe()).isFalse();
                    assertThat(verdict.blockReason()).contains("CRITICAL SAFETY SHIELD");
                }
            }
        }
    }
}
