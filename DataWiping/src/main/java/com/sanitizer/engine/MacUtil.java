package com.sanitizer.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MacUtil {

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
            Process process = Runtime.getRuntime().exec(new String[]{"diskutil", "unmountDisk", diskPath});

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[macOS diskutil]: " + line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("macOS diskutil unmount warning: " + e.getMessage());
        }
    }
}