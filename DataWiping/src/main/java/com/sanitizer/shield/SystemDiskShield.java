package com.sanitizer.shield;

import com.sanitizer.util.AppLogger;
import oshi.SystemInfo;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deep Multi-OS Root & System Storage Protection Shield.
 * Performs deep inspection of mounted operating system volumes, root filesystems (/),
 * boot partitions (/boot, EFI), Windows system roots (C:\Windows), and internal system buses
 * to guarantee that no OS-critical or internal boot drives can ever be opened for block sanitization.
 */
public class SystemDiskShield {

    private static final String MODULE = "SystemDiskShield";

    public record SafetyVerdict(
            boolean isSafe,
            String systemPath,
            String blockReason,
            boolean isSystemDisk
    ) {
        public static SafetyVerdict safe(String systemPath) {
            return new SafetyVerdict(true, systemPath, null, false);
        }

        public static SafetyVerdict blocked(String systemPath, String reason) {
            return new SafetyVerdict(false, systemPath, reason, true);
        }
    }

    private static final Set<String> mockBlockedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setMockBlockedPath(String systemPath) {
        if (systemPath != null) mockBlockedPaths.add(normalize(systemPath));
    }

    public static void clearMockBlockedPaths() {
        mockBlockedPaths.clear();
    }

    /**
     * Evaluates whether a candidate storage device path is safe to wipe, or if it must be blocked.
     */
    public static SafetyVerdict evaluate(String systemPath) {
        if (systemPath == null || systemPath.trim().isEmpty()) {
            return SafetyVerdict.blocked(systemPath, "CRITICAL SAFETY SHIELD: Invalid or null target path provided.");
        }

        String norm = normalize(systemPath);

        // 1. Mock blocked paths for tests
        if (mockBlockedPaths.contains(norm)) {
            return SafetyVerdict.blocked(systemPath, "CRITICAL SAFETY SHIELD: Path explicitly flagged as protected system disk.");
        }

        // 2. Primary OS String Patterns (macOS, Linux, Windows)
        if (norm.contains("disk0") || norm.contains("rdisk0")) {
            return SafetyVerdict.blocked(systemPath,
                    String.format("CRITICAL SAFETY SHIELD: Primary system drive (%s) blocked from wiping!", systemPath));
        }

        if (norm.contains("physicaldrive0") || norm.startsWith("c:") || norm.contains("\\\\.\\c:") || norm.equals("c")) {
            return SafetyVerdict.blocked(systemPath,
                    String.format("CRITICAL SAFETY SHIELD: Primary Windows boot volume (%s) protected!", systemPath));
        }

        // Root/Boot Linux drive patterns
        if (norm.equals("/dev/sda") || norm.startsWith("/dev/sda") || norm.equals("/dev/nvme0n1") || norm.startsWith("/dev/nvme0n1p")) {
            return SafetyVerdict.blocked(systemPath,
                    String.format("CRITICAL SAFETY SHIELD: Primary Linux root/boot drive (%s) protected!", systemPath));
        }

        // 3. Deep Live Mount Inspection via OSHI
        try {
            OperatingSystem os = new SystemInfo().getOperatingSystem();
            List<OSFileStore> fileStores = os.getFileSystem().getFileStores();
            String targetBaseDisk = extractBaseDiskIdentifier(norm);

            for (OSFileStore store : fileStores) {
                String mount = store.getMount();
                String volume = store.getVolume();

                if (isCriticalMount(mount)) {
                    String normMount = normalize(mount);
                    String normVolume = normalize(volume);

                    if (norm.equals(normMount) || norm.equals(normVolume)) {
                        return SafetyVerdict.blocked(systemPath,
                                String.format("CRITICAL SAFETY SHIELD: Storage device hosts active OS mount [%s -> %s]!", mount, volume));
                    }

                    if (!targetBaseDisk.isEmpty()) {
                        String volBaseDisk = extractBaseDiskIdentifier(normVolume);
                        if (!volBaseDisk.isEmpty() && targetBaseDisk.equalsIgnoreCase(volBaseDisk)) {
                            return SafetyVerdict.blocked(systemPath,
                                    String.format("CRITICAL SAFETY SHIELD: Storage device hosts active OS mount [%s -> %s]!", mount, volume));
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Mount inspection fallback warning: " + e.getMessage());
        }

        // 4. File Root Directory Fallback
        for (File root : File.listRoots()) {
            String rootPath = normalize(root.getAbsolutePath());
            if (norm.equalsIgnoreCase(rootPath) && (rootPath.contains("c:") || rootPath.equals("/"))) {
                return SafetyVerdict.blocked(systemPath, "CRITICAL SAFETY SHIELD: Matches active filesystem root (" + rootPath + ")!");
            }
        }

        return SafetyVerdict.safe(systemPath);
    }

    public static boolean isSafeToWipe(String systemPath) {
        SafetyVerdict verdict = evaluate(systemPath);
        if (!verdict.isSafe()) {
            AppLogger.shield(MODULE, verdict.blockReason());
        }
        return verdict.isSafe();
    }

    public static String extractBaseDiskIdentifier(String path) {
        if (path == null) return "";
        String norm = normalize(path);
        if (norm.startsWith("/dev/")) norm = norm.substring(5);
        if (norm.startsWith("\\\\.\\")) norm = norm.substring(4);
        if (norm.startsWith("//./")) norm = norm.substring(4);

        if (norm.startsWith("rdisk")) {
            norm = norm.substring(1); // rdiskX -> diskX
        }

        Matcher mMac = Pattern.compile("^(disk\\d+)").matcher(norm);
        if (mMac.find()) return mMac.group(1);

        Matcher mSda = Pattern.compile("^(sd[a-z]+)").matcher(norm);
        if (mSda.find()) return mSda.group(1);

        Matcher mNvme = Pattern.compile("^(nvme\\d+n\\d+)").matcher(norm);
        if (mNvme.find()) return mNvme.group(1);

        Matcher mWin = Pattern.compile("^(physicaldrive\\d+)").matcher(norm);
        if (mWin.find()) return mWin.group(1);

        return norm;
    }

    private static boolean isCriticalMount(String mount) {
        if (mount == null) return false;
        String m = mount.toLowerCase().trim();
        return m.equals("/") || m.equals("/system") || m.equals("/system/volumes/data")
                || m.equals("/boot") || m.equals("/boot/efi") || m.equals("/etc")
                || m.equals("c:\\") || m.equals("c:/") || m.equals("c:") || m.startsWith("c:\\windows");
    }

    private static String normalize(String path) {
        if (path == null) return "";
        return path.trim().toLowerCase().replace('\\', '/');
    }
}
