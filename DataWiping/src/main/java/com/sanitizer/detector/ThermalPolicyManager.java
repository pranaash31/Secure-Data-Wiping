package com.sanitizer.detector;

import com.sanitizer.util.AppLogger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * Singleton manager for configurable thermal limits and auto-throttling policies.
 * Ensures thread-safe updates, persistence across application sessions, and
 * consistent policy enforcement across all sanitization passes.
 */
public class ThermalPolicyManager {

    private static final String MODULE = "ThermalPolicyMgr";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThermalPolicyManager.class);

    private static final ThermalPolicyManager INSTANCE = new ThermalPolicyManager();

    private final Map<DeviceType, ThermalPolicy> activePolicies = new EnumMap<>(DeviceType.class);
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    private ThermalPolicyManager() {
        loadPolicies();
    }

    public static ThermalPolicyManager getInstance() {
        return INSTANCE;
    }

    /**
     * Loads saved policies from persistent preferences or falls back to defaults.
     */
    public synchronized void loadPolicies() {
        for (DeviceType type : DeviceType.values()) {
            int defaultPause = type.getDefaultAutoPauseCelsius();
            int defaultResume = type.getDefaultResumeCelsius();

            int savedPause = prefs.getInt("thermal_pause_" + type.name(), defaultPause);
            int savedResume = prefs.getInt("thermal_resume_" + type.name(), defaultResume);

            ThermalPolicy policy = ThermalPolicy.of(type, savedPause, savedResume);
            activePolicies.put(type, policy);
        }
        AppLogger.info(MODULE, "Thermal policies initialized: " +
                "USB=" + getPolicy(DeviceType.USB_FLASH).autoPauseCelsius() + "/" + getPolicy(DeviceType.USB_FLASH).resumeCelsius() + "°C, " +
                "NVMe=" + getPolicy(DeviceType.NVME_SSD).autoPauseCelsius() + "/" + getPolicy(DeviceType.NVME_SSD).resumeCelsius() + "°C, " +
                "HDD=" + getPolicy(DeviceType.MAGNETIC_HDD).autoPauseCelsius() + "/" + getPolicy(DeviceType.MAGNETIC_HDD).resumeCelsius() + "°C");
    }

    /**
     * Retrieves the active thermal policy for a specific device type.
     */
    public synchronized ThermalPolicy getPolicy(DeviceType type) {
        if (type == null) type = DeviceType.USB_FLASH;
        return activePolicies.getOrDefault(type, ThermalPolicy.defaultFor(type));
    }

    /**
     * Updates and persists the thermal policy for a device type.
     */
    public synchronized boolean setPolicy(DeviceType type, int autoPauseCelsius, int resumeCelsius) {
        if (type == null) return false;

        ThermalPolicy updated = ThermalPolicy.of(type, autoPauseCelsius, resumeCelsius);
        activePolicies.put(type, updated);

        try {
            prefs.putInt("thermal_pause_" + type.name(), updated.autoPauseCelsius());
            prefs.putInt("thermal_resume_" + type.name(), updated.resumeCelsius());
            prefs.flush();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Failed to persist thermal policy preferences: " + e.getMessage());
        }

        AppLogger.info(MODULE, String.format("Thermal Policy Updated for %s: Auto-Pause=%d°C, Resume=%d°C",
                type.getDisplayName(), updated.autoPauseCelsius(), updated.resumeCelsius()));

        notifyListeners();
        return true;
    }

    /**
     * Gets the auto-pause threshold in °C for the given device type.
     */
    public int getPauseThreshold(DeviceType type) {
        return getPolicy(type).autoPauseCelsius();
    }

    /**
     * Gets the safe resume threshold in °C for the given device type.
     */
    public int getResumeThreshold(DeviceType type) {
        return getPolicy(type).resumeCelsius();
    }

    /**
     * Resolves the device type and active thermal policy for a target drive.
     */
    public ThermalPolicy getPolicyForDrive(String model, String systemPath, long sizeBytes) {
        DeviceType detected = DeviceType.fromDrive(model, systemPath, sizeBytes);
        return getPolicy(detected);
    }

    /**
     * Resets all device policies to factory default standards and persists them.
     */
    public synchronized void resetToDefaults() {
        for (DeviceType type : DeviceType.values()) {
            ThermalPolicy def = ThermalPolicy.defaultFor(type);
            activePolicies.put(type, def);
            try {
                prefs.putInt("thermal_pause_" + type.name(), def.autoPauseCelsius());
                prefs.putInt("thermal_resume_" + type.name(), def.resumeCelsius());
            } catch (Exception ignored) {}
        }
        try {
            prefs.flush();
        } catch (Exception ignored) {}

        AppLogger.info(MODULE, "All thermal throttling policies reset to factory defaults.");
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) changeListeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : changeListeners) {
            try {
                r.run();
            } catch (Exception e) {
                AppLogger.error(MODULE, "Error in policy change listener", e);
            }
        }
    }
}
