package com.sanitizer.shield;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DiskLockShield Unmount & Locking Tests")
class DiskLockShieldTest {

    @Test
    @DisplayName("Lock acquisition and safe release on simulated target device")
    void testDiskLockAndReleaseCycle() {
        String testTarget = "/dev/rdisk99";

        // Attempt locking
        assertThat(DiskLockShield.prepareAndLockDisk(testTarget)).isTrue();
        assertThat(DiskLockShield.isDiskLocked(testTarget)).isTrue();

        // Release lock
        DiskLockShield.releaseDiskLock(testTarget);
        assertThat(DiskLockShield.isDiskLocked(testTarget)).isFalse();
    }

    @Test
    @DisplayName("Null target path handling is safe and non-throwing")
    void testNullPathLocking() {
        boolean locked = DiskLockShield.prepareAndLockDisk(null);
        assertThat(locked).isFalse();
        DiskLockShield.releaseDiskLock(null);
    }
}
