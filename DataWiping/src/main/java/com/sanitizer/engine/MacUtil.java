package com.sanitizer.engine;

import com.sanitizer.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MacUtil {

    private static final String MODULE = "MacUtil";

    /**
     * Unmounts all logical volumes on a macOS disk so low-level block write isn't blocked by the OS.
     * Example systemPath: "/dev/rdisk2" -> runs `diskutil unmountDisk /dev/disk2`
     */
    public static void unmountDiskIfMac(String systemPath) {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) {
            return;
        }

        try {
            String diskPath = systemPath.replace("/dev/rdisk", "/dev/disk");
            ProcessBuilder pb = new ProcessBuilder("diskutil", "unmountDisk", diskPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    AppLogger.info(MODULE, "[macOS diskutil]: " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                AppLogger.warn(MODULE, "diskutil unmount exited with code: " + exitCode);
            }
        } catch (Exception e) {
            AppLogger.warn(MODULE, "macOS diskutil unmount warning: " + e.getMessage());
        }
    }
}