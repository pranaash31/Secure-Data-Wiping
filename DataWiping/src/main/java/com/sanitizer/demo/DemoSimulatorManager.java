package com.sanitizer.demo;

import com.sanitizer.detector.UsbDetector;
import com.sanitizer.detector.UsbDetector.UsbDriveInfo;
import com.sanitizer.util.AppLogger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Enterprise Presentation & Evaluator Demo Simulator Mode.
 * Enables risk-free live demonstrations of data sanitization workflows
 * without requiring physical disposable USB drives.
 *
 * Mounts a virtual "Simulated 16GB Secure USB" with realistic hardware metadata,
 * S.M.A.R.T. telemetry, live multi-pass overwrite animation, sector heatmap rendering,
 * zero-pass Shannon entropy verification, and defense-grade PDF certificate issuance.
 */
public class DemoSimulatorManager {

    private static final String MODULE = "DemoSimulator";

    public static final String SIMULATED_DRIVE_PATH = "/dev/mock_usb_16gb";
    public static final String SIMULATED_MODEL = "Simulated 16GB Secure USB";
    public static final String SIMULATED_SERIAL = "SIM-SECURE-16GB-X88";
    public static final long SIMULATED_SIZE_BYTES = 16L * 1024 * 1024 * 1024; // 16 GB = 17,179,869,184 bytes
    public static final String SIMULATED_FORMATTED_SIZE = "16 GB";

    public static final UsbDriveInfo SIMULATED_DRIVE = new UsbDriveInfo(
            SIMULATED_MODEL,
            SIMULATED_SERIAL,
            SIMULATED_SIZE_BYTES,
            SIMULATED_FORMATTED_SIZE,
            SIMULATED_DRIVE_PATH
    );

    private static final AtomicBoolean demoModeActive = new AtomicBoolean(false);
    private static final List<Consumer<Boolean>> modeListeners = new CopyOnWriteArrayList<>();

    private DemoSimulatorManager() {}

    /**
     * Checks if Presentation Simulator Mode is currently active.
     */
    public static boolean isDemoModeActive() {
        return demoModeActive.get();
    }

    /**
     * Enables Presentation Simulator Mode and mounts the Simulated 16GB Secure USB drive.
     */
    public static synchronized void enableDemoMode() {
        if (!demoModeActive.get()) {
            demoModeActive.set(true);
            UsbDetector.setMockDrivesOverride(List.of(SIMULATED_DRIVE));
            UsbDetector.notifyListenersNow();
            AppLogger.info(MODULE, "Presentation Simulator Mode ENABLED: " + SIMULATED_MODEL + " mounted at " + SIMULATED_DRIVE_PATH);
            notifyListeners(true);
        }
    }

    /**
     * Disables Presentation Simulator Mode and unmounts the simulated drive.
     */
    public static synchronized void disableDemoMode() {
        if (demoModeActive.get()) {
            demoModeActive.set(false);
            UsbDetector.clearMockDrivesOverride();
            UsbDetector.notifyListenersNow();
            AppLogger.info(MODULE, "Presentation Simulator Mode DISABLED: Simulated drive unmounted.");
            notifyListeners(false);
        }
    }

    /**
     * Toggles Presentation Simulator Mode on or off.
     * @return New active state (true if enabled, false if disabled)
     */
    public static synchronized boolean toggleDemoMode() {
        if (isDemoModeActive()) {
            disableDemoMode();
            return false;
        } else {
            enableDemoMode();
            return true;
        }
    }

    /**
     * Checks if a specific device path corresponds to a simulated or mock drive.
     */
    public static boolean isSimulatedDrive(String systemPath) {
        if (systemPath == null) return false;
        String p = systemPath.toLowerCase().trim();
        return p.equals(SIMULATED_DRIVE_PATH.toLowerCase())
                || p.contains("mock_usb")
                || p.contains("simulated_usb")
                || p.contains("sim-secure");
    }

    /**
     * Checks if a UsbDriveInfo descriptor corresponds to a simulated drive.
     */
    public static boolean isSimulatedDrive(UsbDriveInfo drive) {
        if (drive == null) return false;
        return isSimulatedDrive(drive.systemPath())
                || drive.model().toLowerCase().contains("simulated")
                || drive.serial().equalsIgnoreCase(SIMULATED_SERIAL);
    }

    public static UsbDriveInfo getSimulatedDrive() {
        return SIMULATED_DRIVE;
    }

    public static void addDemoModeListener(Consumer<Boolean> listener) {
        if (listener != null) {
            modeListeners.add(listener);
        }
    }

    public static void removeDemoModeListener(Consumer<Boolean> listener) {
        if (listener != null) {
            modeListeners.remove(listener);
        }
    }

    private static void notifyListeners(boolean active) {
        for (Consumer<Boolean> l : modeListeners) {
            try {
                l.accept(active);
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Error in demo mode listener: " + e.getMessage());
            }
        }
    }
}
