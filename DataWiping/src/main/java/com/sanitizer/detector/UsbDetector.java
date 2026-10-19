package com.sanitizer.detector;

import com.sanitizer.shield.SystemDiskShield;
import com.sanitizer.util.AppLogger;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.UsbDevice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class UsbDetector {

    private static final String MODULE = "UsbDetector";

    public record UsbDriveInfo(String model, String serial, long sizeBytes, String formattedSize, String systemPath) {}

    public enum BusInterface {
        USB,
        THUNDERBOLT,
        SD,
        SATA,
        INTERNAL,
        UNKNOWN
    }

    private static List<UsbDriveInfo> mockDrivesOverride = null;

    public static synchronized void setMockDrivesOverride(List<UsbDriveInfo> mockDrives) {
        mockDrivesOverride = mockDrives;
    }

    public static synchronized void clearMockDrivesOverride() {
        mockDrivesOverride = null;
    }

    /**
     * Identifies internal host boot drives or internal host hardware.
     * NOTE: Does NOT filter out "ssd", "sata", or "/dev/sda" to allow external SSDs
     * and external SATA drives to be detected and sanitized.
     */
    public static boolean isSystemOrSsdDisk(String name, String model, String serial) {
        if (name == null) name = "";
        if (model == null) model = "";
        if (serial == null) serial = "";

        String n = name.toLowerCase();
        String m = model.toLowerCase();

        return n.contains("disk0")
                || n.contains("physicaldrive0")
                || m.contains("apple internal")
                || m.contains("internal nvme")
                || m.contains("internal storage")
                || m.equals("apple macintosh hd");
    }

    public static final long MIN_DRIVE_SIZE_BYTES = 100L * 1024 * 1024; // 100 MB minimum (supports flash drives, SD cards, external SSDs)

    /**
     * Checks if the drive size is valid for sanitization.
     * The previous 128 GB maximum size cap has been removed to support multi-terabyte external SSDs and SATA drives.
     */
    public static boolean isValidPenDriveSize(long sizeBytes) {
        return sizeBytes >= MIN_DRIVE_SIZE_BYTES;
    }

    public static boolean isValidDriveSize(long sizeBytes) {
        return sizeBytes >= MIN_DRIVE_SIZE_BYTES;
    }

    public static String formatDiskSize(long sizeBytes) {
        if (sizeBytes >= 1000L * 1024 * 1024 * 1024) {
            double tb = (double) sizeBytes / (1024L * 1024 * 1024 * 1024);
            return String.format("%.1f TB", tb);
        }
        long sizeGb = sizeBytes / (1024L * 1024 * 1024);
        if (sizeGb > 0) {
            return sizeGb + " GB";
        }
        return (sizeBytes / (1024L * 1024)) + " MB";
    }

    /**
     * Inspects the physical bus topology of a disk to check if it is on an external/peripheral bus
     * (USB, Thunderbolt, SD Card, external SATA) rather than internal host system bus.
     */
    public static BusInterface detectDiskInterface(HWDiskStore disk, HardwareAbstractionLayer hal) {
        if (disk == null) return BusInterface.UNKNOWN;

        String name = disk.getName() != null ? disk.getName().toLowerCase() : "";
        String model = disk.getModel() != null ? disk.getModel().toLowerCase() : "";
        String serial = disk.getSerial() != null ? disk.getSerial().toLowerCase() : "";

        // Internal host drive checks
        if (name.contains("disk0") || name.contains("physicaldrive0") || model.contains("apple internal")) {
            return BusInterface.INTERNAL;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();

        // 1. macOS topology inspection via diskutil
        if (osName.contains("mac")) {
            BusInterface macBus = inspectMacDiskutilBus(disk.getName());
            if (macBus != null && macBus != BusInterface.UNKNOWN) {
                return macBus;
            }
        }

        // 2. Linux topology inspection via /sys/block
        if (osName.contains("linux")) {
            BusInterface linuxBus = inspectLinuxSysfsBus(disk.getName());
            if (linuxBus != null && linuxBus != BusInterface.UNKNOWN) {
                return linuxBus;
            }
        }

        // 3. OSHI UsbDevice Tree inspection
        if (hal != null) {
            try {
                Set<String> usbIdentifiers = collectUsbDeviceIdentifiers(hal.getUsbDevices(true));
                if (usbIdentifiers.contains(serial) || usbIdentifiers.contains(model) || usbIdentifiers.contains(name)) {
                    return BusInterface.USB;
                }
            } catch (Exception ignored) {}
        }

        // 4. Model and protocol heuristics fallback
        if (model.contains("usb") || model.contains("flash") || model.contains("thumb") || model.contains("portable")
                || model.contains("datatraveler") || model.contains("ultra flair") || model.contains("cruzer")
                || model.contains("card reader") || model.contains("sd card")) {
            return BusInterface.USB;
        }
        if (model.contains("thunderbolt")) {
            return BusInterface.THUNDERBOLT;
        }
        if (model.contains("sata") || model.contains("external")) {
            return BusInterface.SATA;
        }

        return BusInterface.USB;
    }

    public static boolean isExternalPeripheralBus(HWDiskStore disk, HardwareAbstractionLayer hal) {
        BusInterface bus = detectDiskInterface(disk, hal);
        return bus == BusInterface.USB || bus == BusInterface.THUNDERBOLT || bus == BusInterface.SD || bus == BusInterface.SATA;
    }

    private static BusInterface inspectMacDiskutilBus(String diskName) {
        if (diskName == null || diskName.isEmpty()) return BusInterface.UNKNOWN;
        try {
            String shortDisk = diskName.substring(Math.max(0, diskName.lastIndexOf('/') + 1));
            Process process = new ProcessBuilder("diskutil", "info", shortDisk).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String protocol = "";
                String location = "";
                boolean isRemovable = false;

                while ((line = reader.readLine()) != null) {
                    String lower = line.toLowerCase().trim();
                    if (lower.startsWith("protocol:")) {
                        protocol = lower.substring(lower.indexOf(':') + 1).trim();
                    } else if (lower.startsWith("device location:")) {
                        location = lower.substring(lower.indexOf(':') + 1).trim();
                    } else if (lower.startsWith("removable media:")) {
                        isRemovable = lower.contains("removable") || lower.contains("yes");
                    }
                }

                if (location.contains("internal") && protocol.contains("apple fabric")) {
                    return BusInterface.INTERNAL;
                }
                if (protocol.contains("usb")) return BusInterface.USB;
                if (protocol.contains("thunderbolt")) return BusInterface.THUNDERBOLT;
                if (protocol.contains("secure digital") || protocol.contains("sd")) return BusInterface.SD;
                if (protocol.contains("sata") && (location.contains("external") || isRemovable)) return BusInterface.SATA;
                if (location.contains("external") || isRemovable) return BusInterface.USB;
            }
        } catch (Exception ignored) {}
        return BusInterface.UNKNOWN;
    }

    private static BusInterface inspectLinuxSysfsBus(String diskName) {
        if (diskName == null || diskName.isEmpty()) return BusInterface.UNKNOWN;
        try {
            String base = diskName.substring(Math.max(0, diskName.lastIndexOf('/') + 1));
            Path sysPath = Path.of("/sys/block", base);
            if (Files.exists(sysPath)) {
                String realPath = sysPath.toRealPath().toString().toLowerCase();
                if (realPath.contains("/usb")) return BusInterface.USB;
                if (realPath.contains("/thunderbolt")) return BusInterface.THUNDERBOLT;
                if (realPath.contains("/mmc")) return BusInterface.SD;

                Path remFile = sysPath.resolve("removable");
                if (Files.exists(remFile) && Files.readString(remFile).trim().equals("1")) {
                    return BusInterface.USB;
                }
            }
        } catch (Exception ignored) {}
        return BusInterface.UNKNOWN;
    }

    private static Set<String> collectUsbDeviceIdentifiers(List<UsbDevice> devices) {
        Set<String> set = new HashSet<>();
        if (devices == null) return set;
        for (UsbDevice dev : devices) {
            if (dev.getSerialNumber() != null && !dev.getSerialNumber().isBlank()) {
                set.add(dev.getSerialNumber().toLowerCase().trim());
            }
            if (dev.getName() != null && !dev.getName().isBlank()) {
                set.add(dev.getName().toLowerCase().trim());
            }
            if (dev.getConnectedDevices() != null && !dev.getConnectedDevices().isEmpty()) {
                set.addAll(collectUsbDeviceIdentifiers(dev.getConnectedDevices()));
            }
        }
        return set;
    }

    public static List<UsbDriveInfo> getConnectedUsbDrives() {
        if (mockDrivesOverride != null) {
            return new ArrayList<>(mockDrivesOverride);
        }

        List<UsbDriveInfo> drives = new ArrayList<>();
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();

        for (HWDiskStore disk : hal.getDiskStores()) {
            String rawName = disk.getName();
            String fullPath;
            if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                int diskIdx = rawName.lastIndexOf("disk");
                if (diskIdx >= 0) {
                    String diskId = rawName.substring(diskIdx);
                    fullPath = "/dev/r" + diskId;
                } else {
                    fullPath = rawName;
                }
            } else {
                fullPath = rawName;
            }

            // 1. Host Protection Shield (Live Mount Tree + Root Protection)
            if (!SystemDiskShield.isSafeToWipe(fullPath) || !SystemDiskShield.isSafeToWipe(rawName)) {
                AppLogger.shield(MODULE, "Blocked Host / Protected Boot Disk: " + disk.getModel() + " (" + fullPath + ")");
                continue;
            }

            // 2. Internal host keyword filter
            if (isSystemOrSsdDisk(disk.getName(), disk.getModel(), disk.getSerial())) {
                AppLogger.shield(MODULE, "Blocked Internal Host Hardware: " + disk.getModel());
                continue;
            }

            // 3. Inspect Physical Bus Topology (USB / Thunderbolt / SD / External SATA)
            if (!isExternalPeripheralBus(disk, hal)) {
                AppLogger.shield(MODULE, "Blocked Non-External Bus Disk: " + disk.getModel());
                continue;
            }

            // 4. Validate drive capacity (no arbitrary 128 GB cap)
            if (isValidPenDriveSize(disk.getSize())) {
                long sizeBytes = disk.getSize();
                String formattedSize = formatDiskSize(sizeBytes);

                AppLogger.info(MODULE, "External Drive Detected: " + disk.getModel() + " | Size: " + formattedSize + " | Path: " + fullPath);

                drives.add(new UsbDriveInfo(
                        disk.getModel().trim().isEmpty() ? "External Storage Device" : disk.getModel(),
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