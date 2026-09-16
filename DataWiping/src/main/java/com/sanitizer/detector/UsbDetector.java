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

    private static List<UsbDriveInfo> mockDrivesOverride = null;

    public static synchronized void setMockDrivesOverride(List<UsbDriveInfo> mockDrives) {
        mockDrivesOverride = mockDrives;
    }

    public static synchronized void clearMockDrivesOverride() {
        mockDrivesOverride = null;
    }

    public static boolean isSystemOrSsdDisk(String name, String model, String serial) {
        if (name == null) name = "";
        if (model == null) model = "";
        if (serial == null) serial = "";

        String n = name.toLowerCase();
        String m = model.toLowerCase();
        String s = serial.toLowerCase();

        return n.contains("disk0")
                || m.contains("apple")
                || m.contains("internal")
                || m.contains("ssd")
                || m.contains("nvme")
                || m.contains("sata")
                || m.contains("apfs")
                || s.contains("ssd")
                || s.contains("nvme");
    }

    public static boolean isValidPenDriveSize(long sizeBytes) {
        long minPenDriveSizeBytes = 1L * 1024 * 1024 * 1024; // 1 GB
        long maxPenDriveSizeBytes = 128L * 1024 * 1024 * 1024; // 128 GB
        return sizeBytes >= minPenDriveSizeBytes && sizeBytes <= maxPenDriveSizeBytes;
    }

    public static List<UsbDriveInfo> getConnectedUsbDrives() {
        if (mockDrivesOverride != null) {
            return new ArrayList<>(mockDrivesOverride);
        }

        List<UsbDriveInfo> drives = new ArrayList<>();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            boolean isSsdOrSystem = isSystemOrSsdDisk(disk.getName(), disk.getModel(), disk.getSerial());

            if (isSsdOrSystem) {
                AppLogger.shield(MODULE, "Blocked Non-Pen Drive / System Disk: " + disk.getModel());
                continue;
            }

            if (isValidPenDriveSize(disk.getSize())) {
                long sizeBytes = disk.getSize();
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
                        notifyListener(l, drives);
                    }
                } catch (Exception e) {
                    AppLogger.error(MODULE, "Polling Error", e);
                }
            }, 0, 2, TimeUnit.SECONDS);

            // Shutdown hook to clean up thread pool on app exit
            Runtime.getRuntime().addShutdownHook(new Thread(UsbDetector::shutdown));
        }
    }

    public static void notifyListenersNow() {
        List<UsbDriveInfo> drives = getConnectedUsbDrives();
        for (var l : listeners) {
            notifyListener(l, drives);
        }
    }

    private static void notifyListener(Consumer<List<UsbDriveInfo>> listener, List<UsbDriveInfo> drives) {
        try {
            javafx.application.Platform.runLater(() -> listener.accept(drives));
        } catch (IllegalStateException e) {
            // JavaFX Platform not initialized (e.g. headless unit tests) -> invoke directly
            listener.accept(drives);
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