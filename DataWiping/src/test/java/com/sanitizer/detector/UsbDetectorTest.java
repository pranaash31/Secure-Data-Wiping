package com.sanitizer.detector;

import com.sanitizer.harness.MockHardwareHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsbDetector & Mock Hardware Harness Unit Tests")
class UsbDetectorTest {

    private MockHardwareHarness harness;

    @BeforeEach
    void setUp() {
        harness = new MockHardwareHarness();
        harness.reset();
    }

    @AfterEach
    void tearDown() {
        harness.reset();
    }

    @Test
    @DisplayName("Drive Size Filter accepts valid range including multi-terabyte external drives (No 128 GB cap)")
    void testValidPenDriveSizeFilter() {
        long minSizeBytes = 1L * 1024 * 1024 * 1024; // 1 GB
        long midSizeBytes = 32L * 1024 * 1024 * 1024; // 32 GB
        long largeSizeBytes = 500L * 1024 * 1024 * 1024; // 500 GB External SSD
        long tbSizeBytes = 2L * 1024 * 1024 * 1024 * 1024; // 2 TB External SSD

        assertThat(UsbDetector.isValidPenDriveSize(minSizeBytes)).isTrue();
        assertThat(UsbDetector.isValidPenDriveSize(midSizeBytes)).isTrue();
        assertThat(UsbDetector.isValidPenDriveSize(largeSizeBytes)).isTrue();
        assertThat(UsbDetector.isValidPenDriveSize(tbSizeBytes)).isTrue();
    }

    @Test
    @DisplayName("Drive Size Filter rejects invalid sizes (<= 0 or below minimum threshold)")
    void testInvalidPenDriveSizeFilter() {
        long zeroSize = 0L;
        long negativeSize = -1L;
        long tooSmall = 50L * 1024 * 1024; // 50 MB (below 100 MB threshold)

        assertThat(UsbDetector.isValidPenDriveSize(zeroSize)).isFalse();
        assertThat(UsbDetector.isValidPenDriveSize(negativeSize)).isFalse();
        assertThat(UsbDetector.isValidPenDriveSize(tooSmall)).isFalse();
    }

    @ParameterizedTest(name = "Internal host hardware: {0} is blocked")
    @ValueSource(strings = {
            "disk0",
            "Apple Macintosh HD",
            "Internal NVMe SSD",
            "Apple Internal Storage"
    })
    void testSystemOrSsdDiskFilterBlocksInternalDrives(String systemIdentifier) {
        boolean isSystemDisk = UsbDetector.isSystemOrSsdDisk(systemIdentifier, systemIdentifier, systemIdentifier);
        assertThat(isSystemDisk).isTrue();
    }

    @ParameterizedTest(name = "External USB SSD or SATA candidate: {0} is NOT blocked by keywords")
    @ValueSource(strings = {
            "SanDisk Extreme Portable SSD",
            "Crucial MX500 SATA SSD",
            "Samsung Portable SSD T7",
            "Kingston External SATA Dock",
            "WD My Passport SSD"
    })
    void testExternalUsbSsdAndSataNotBlockedByKeywords(String externalCandidate) {
        boolean isSystemDisk = UsbDetector.isSystemOrSsdDisk("/dev/rdisk4", externalCandidate, "EXT-SERIAL-1234");
        assertThat(isSystemDisk).isFalse();
    }

    @Test
    @DisplayName("Disk size formatter correctly formats gigabytes and terabytes")
    void testFormatDiskSize() {
        assertThat(UsbDetector.formatDiskSize(32L * 1024 * 1024 * 1024)).isEqualTo("32 GB");
        assertThat(UsbDetector.formatDiskSize(500L * 1024 * 1024 * 1024)).isEqualTo("500 GB");
        assertThat(UsbDetector.formatDiskSize(1024L * 1024 * 1024 * 1024)).isEqualTo("1.0 TB");
        assertThat(UsbDetector.formatDiskSize(2048L * 1024 * 1024 * 1024)).isEqualTo("2.0 TB");
    }

    @Test
    @DisplayName("Mock Hardware Harness simulates USB hot-plug event and notifies listeners")
    void testMockHardwareHotPlugNotification() {
        List<List<UsbDetector.UsbDriveInfo>> listenerEvents = new ArrayList<>();
        AtomicInteger notificationCount = new AtomicInteger(0);

        UsbDetector.registerListener(drives -> {
            notificationCount.incrementAndGet();
            listenerEvents.add(new ArrayList<>(drives));
        });

        // 1. Connect SanDisk 32GB drive
        UsbDetector.UsbDriveInfo sanDisk = MockHardwareHarness.createSanDisk32GB();
        harness.connectDrive(sanDisk);

        assertThat(notificationCount.get()).isGreaterThanOrEqualTo(1);
        List<UsbDetector.UsbDriveInfo> lastEventDrives = listenerEvents.get(listenerEvents.size() - 1);
        assertThat(lastEventDrives).hasSize(1);
        assertThat(lastEventDrives.get(0).model()).contains("SanDisk");
        assertThat(lastEventDrives.get(0).systemPath()).isEqualTo("/dev/rdisk4");

        // 2. Connect Kingston 64GB drive
        UsbDetector.UsbDriveInfo kingston = MockHardwareHarness.createKingston64GB();
        harness.connectDrive(kingston);

        lastEventDrives = listenerEvents.get(listenerEvents.size() - 1);
        assertThat(lastEventDrives).hasSize(2);
        assertThat(lastEventDrives).extracting(UsbDetector.UsbDriveInfo::serial)
                .containsExactlyInAnyOrder("SD-FLAIR-99421", "KG-DT100-3882");

        // 3. Hot-unplug SanDisk drive
        harness.disconnectDriveBySerial("SD-FLAIR-99421");

        lastEventDrives = listenerEvents.get(listenerEvents.size() - 1);
        assertThat(lastEventDrives).hasSize(1);
        assertThat(lastEventDrives.get(0).serial()).isEqualTo("KG-DT100-3882");
    }
}
