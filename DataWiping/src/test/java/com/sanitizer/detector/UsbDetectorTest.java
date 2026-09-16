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
    @DisplayName("Pen Drive Size Filter accepts valid range (1 GB to 128 GB)")
    void testValidPenDriveSizeFilter() {
        long minSizeBytes = 1L * 1024 * 1024 * 1024; // 1 GB
        long midSizeBytes = 32L * 1024 * 1024 * 1024; // 32 GB
        long maxSizeBytes = 128L * 1024 * 1024 * 1024; // 128 GB

        assertThat(UsbDetector.isValidPenDriveSize(minSizeBytes)).isTrue();
        assertThat(UsbDetector.isValidPenDriveSize(midSizeBytes)).isTrue();
        assertThat(UsbDetector.isValidPenDriveSize(maxSizeBytes)).isTrue();
    }

    @Test
    @DisplayName("Pen Drive Size Filter rejects sizes out of range (<1 GB or >128 GB)")
    void testInvalidPenDriveSizeFilter() {
        long tooSmall = 500L * 1024 * 1024; // 500 MB
        long tooLarge = 500L * 1024 * 1024 * 1024; // 500 GB (External HDD / NVMe)

        assertThat(UsbDetector.isValidPenDriveSize(tooSmall)).isFalse();
        assertThat(UsbDetector.isValidPenDriveSize(tooLarge)).isFalse();
    }

    @ParameterizedTest(name = "System drive or internal SSD candidate: name={0}, model={0} is blocked")
    @ValueSource(strings = {
            "disk0",
            "Apple Macintosh HD",
            "Internal NVMe SSD",
            "SATA Primary Drive",
            "APFS System Container"
    })
    void testSystemOrSsdDiskFilterBlocksInternalDrives(String systemIdentifier) {
        boolean isSystemDisk = UsbDetector.isSystemOrSsdDisk(systemIdentifier, systemIdentifier, systemIdentifier);
        assertThat(isSystemDisk).isTrue();
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
