package com.sanitizer;

import com.sanitizer.detector.UsbDetector;
import com.sanitizer.engine.WipeEngine;

import java.util.List;

public class Maintest {
    public static void main(String[] args) {
        System.out.println("=== MODULE 1: DEFENSE-GRADE HARDWARE SANITIZATION TEST ===");

        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();

        if (drives.isEmpty()) {
            System.out.println("No USB pen drives detected. Insert a pen drive and re-run.");
            return;
        }

        UsbDetector.UsbDriveInfo target = drives.get(0);
        System.out.println("\nTargeting drive: " + target.model());

        // Execute DoD 5220.22-M (3-Pass Wipe)
        boolean result = WipeEngine.executeWipe(
                target.systemPath(),
                target.sizeBytes(),
                WipeEngine.WipeStandard.DOD_5220_22_M,
                percent -> System.out.printf("\rOverall Progress: %.2f%%", percent)
        );

        if (result) {
            System.out.println("\n\nSUCCESS: Drive sanitization completed according to DoD 5220.22-M!");
        } else {
            System.out.println("\n\nFAILED: Ensure terminal was launched with sudo privileges!");
        }
    }
}