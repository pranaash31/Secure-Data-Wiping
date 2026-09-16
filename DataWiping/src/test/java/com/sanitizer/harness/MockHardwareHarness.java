package com.sanitizer.harness;

import com.sanitizer.detector.UsbDetector;
import com.sanitizer.detector.UsbDetector.UsbDriveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test double generator and mock hardware harness for simulating USB drive connection and disconnection
 * events without physical hardware.
 */
public class MockHardwareHarness {

    private final List<UsbDriveInfo> activeDrives = new ArrayList<>();

    public static UsbDriveInfo createMockDrive(String model, String serial, long sizeBytes, String systemPath) {
        long sizeGb = sizeBytes / (1024L * 1024 * 1024);
        String formattedSize = sizeGb > 0 ? sizeGb + " GB" : (sizeBytes / (1024L * 1024)) + " MB";
        return new UsbDriveInfo(model, serial, sizeBytes, formattedSize, systemPath);
    }

    public static UsbDriveInfo createSanDisk32GB() {
        return createMockDrive("SanDisk Ultra Flair 32GB", "SD-FLAIR-99421", 32L * 1024 * 1024 * 1024, "/dev/rdisk4");
    }

    public static UsbDriveInfo createKingston64GB() {
        return createMockDrive("Kingston DataTraveler 64GB", "KG-DT100-3882", 64L * 1024 * 1024 * 1024, "/dev/rdisk5");
    }

    public static UsbDriveInfo createAppleSystemDisk() {
        return createMockDrive("APPLE SSD AP0512N", "APPLE-SSD-001", 500L * 1024 * 1024 * 1024, "/dev/rdisk0");
    }

    public UsbDriveInfo connectDrive(UsbDriveInfo drive) {
        activeDrives.add(drive);
        syncWithDetector();
        return drive;
    }

    public boolean disconnectDriveBySerial(String serial) {
        boolean removed = activeDrives.removeIf(d -> d.serial().equalsIgnoreCase(serial));
        if (removed) {
            syncWithDetector();
        }
        return removed;
    }

    public void clearAllDrives() {
        activeDrives.clear();
        syncWithDetector();
    }

    public List<UsbDriveInfo> getActiveDrives() {
        return Collections.unmodifiableList(activeDrives);
    }

    public void syncWithDetector() {
        UsbDetector.setMockDrivesOverride(new ArrayList<>(activeDrives));
        UsbDetector.notifyListenersNow();
    }

    public void reset() {
        activeDrives.clear();
        UsbDetector.clearMockDrivesOverride();
    }
}
