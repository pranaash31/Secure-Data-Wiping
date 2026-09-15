package com.sanitizer.detector;

import com.sanitizer.util.AppLogger;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class UsbDetector {

    private static final String MODULE = "UsbDetector";

    public record UsbDriveInfo(String model, String serial, long sizeBytes, String formattedSize, String systemPath) {}

    public static List<UsbDriveInfo> getConnectedUsbDrives() {
        List<UsbDriveInfo> drives = new ArrayList<>();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            String model = disk.getModel().toLowerCase();
            String serial = disk.getSerial().toLowerCase();
            String name = disk.getName().toLowerCase();
            long sizeBytes = disk.getSize();

            // SAFETY SHIELD: Ignore primary macOS drive & internal SSDs
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
                AppLogger.shield(MODULE, "Blocked Non-Pen Drive / System Disk: " + disk.getModel());
                continue;
            }

            // PEN DRIVE FILTER (1 GB to 128 GB)
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

                AppLogger.info(MODULE, "Pen Drive Detected: " + disk.getModel() + " | Size: " + formattedSize + " | Path: " + fullPath);

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

    private static final CopyOnWriteArrayList<Consumer<List<UsbDriveInfo>>> listeners = new CopyOnWriteArrayList<>();
    private static ScheduledExecutorService scheduler;

    public static synchronized void registerListener(Consumer<List<UsbDriveInfo>> listener) {
        listeners.add(listener);
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "UsbPollerThread");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    List<UsbDriveInfo> drives = getConnectedUsbDrives();
                    for (var l : listeners) {
                        javafx.application.Platform.runLater(() -> l.accept(drives));
                    }
                } catch (Exception e) {
                    AppLogger.error(MODULE, "Polling Error", e);
                }
            }, 0, 2, TimeUnit.SECONDS);

            // Shutdown hook to clean up thread pool on app exit
            Runtime.getRuntime().addShutdownHook(new Thread(UsbDetector::shutdown));
        }
    }

    public static synchronized void unregisterListener(Consumer<List<UsbDriveInfo>> listener) {
        listeners.remove(listener);
    }

    public static synchronized void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            AppLogger.info(MODULE, "Shutting down USB detector polling executor...");
            scheduler.shutdownNow();
        }
    }
}