package com.sanitizer.detector;

import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.ArrayList;
import java.util.List;

public class UsbDetector {

    public record UsbDriveInfo(String model, String serial, long sizeBytes, String formattedSize, String systemPath) {}

    public static List<UsbDriveInfo> getConnectedUsbDrives() {
        List<UsbDriveInfo> drives = new ArrayList<>();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            String model = disk.getModel().toLowerCase();
            String serial = disk.getSerial().toLowerCase();
            String name = disk.getName().toLowerCase();
            long sizeBytes = disk.getSize();

            // ❌ SAFETY SHIELD: Ignore primary macOS drive & internal SSDs
            boolean isSsdOrSystem = name.contains("disk0")
                    || model.contains("apple")
                    || model.contains("internal")
                    || model.contains("ssd")
                    || model.contains("nvme")
                    || model.contains("sata")
                    || model.contains("apfs")
                    || serial.contains("ssd")
                    || serial.contains("nvme");

            if (isSsdOrSystem) {
                System.out.println("[Safety Shield] Blocked Non-Pen Drive / SSD: " + disk.getModel());
                continue;
            }

            // ✅ PEN DRIVE FILTER (1 GB to 128 GB)
            long minPenDriveSizeBytes = 1L * 1024 * 1024 * 1024;
            long maxPenDriveSizeBytes = 128L * 1024 * 1024 * 1024;

            boolean isValidPenDriveSize = sizeBytes >= minPenDriveSizeBytes && sizeBytes <= maxPenDriveSizeBytes;

            if (isValidPenDriveSize) {
                long sizeGb = sizeBytes / (1024 * 1024 * 1024);
                String formattedSize = sizeGb > 0 ? sizeGb + " GB" : (sizeBytes / (1024 * 1024)) + " MB";

                String rawName = disk.getName(); // e.g., "disk4" or "/dev/disk4"

                // Construct proper macOS raw block device path: "/dev/rdisk4"
                String fullPath;
                if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                    String diskId = rawName.substring(rawName.lastIndexOf("disk")); // extracts "disk4"
                    fullPath = "/dev/r" + diskId; // results in "/dev/rdisk4"
                } else {
                    fullPath = rawName;
                }

                System.out.println("[Pen Drive Detected] Model: " + disk.getModel() + " | Size: " + formattedSize + " | Path: " + fullPath);

                drives.add(new UsbDriveInfo(
                        disk.getModel().trim().isEmpty() ? "32GB USB Flash Drive" : disk.getModel(),
                        disk.getSerial().trim().isEmpty() ? "UNKNOWN_SERIAL" : disk.getSerial().trim(),
                        sizeBytes,
                        formattedSize,
                        fullPath
                ));
            }
        }
        return drives;
    }
}