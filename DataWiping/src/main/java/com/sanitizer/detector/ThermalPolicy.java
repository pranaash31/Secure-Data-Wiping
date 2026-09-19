package com.sanitizer.detector;

/**
 * Encapsulates the thermal safety limits and auto-throttling policy for a specific device type.
 *
 * @param deviceType         The category of storage device.
 * @param autoPauseCelsius   The temperature at which sanitization is paused to prevent damage.
 * @param resumeCelsius      The temperature to which the drive must cool before wiping resumes.
 * @param warningCelsius     The temperature at which elevated thermal warnings are logged/flagged.
 */
public record ThermalPolicy(
        DeviceType deviceType,
        int autoPauseCelsius,
        int resumeCelsius,
        int warningCelsius
) {
    public static final int MIN_ALLOWED_TEMP = 35;
    public static final int MAX_ALLOWED_TEMP = 95;
    public static final int MIN_HYSTERESIS_GAP = 3; // Pause must be at least 3°C above Resume

    public ThermalPolicy {
        if (deviceType == null) {
            deviceType = DeviceType.USB_FLASH;
        }
        // Sanitize bounds
        autoPauseCelsius = Math.max(MIN_ALLOWED_TEMP + MIN_HYSTERESIS_GAP, Math.min(MAX_ALLOWED_TEMP, autoPauseCelsius));
        resumeCelsius = Math.max(MIN_ALLOWED_TEMP, Math.min(autoPauseCelsius - MIN_HYSTERESIS_GAP, resumeCelsius));
        warningCelsius = Math.max(resumeCelsius, Math.min(autoPauseCelsius, warningCelsius));
    }

    /**
     * Creates a standard policy instance from auto-pause and resume temperatures,
     * calculating a proportionate warning threshold.
     */
    public static ThermalPolicy of(DeviceType deviceType, int autoPauseCelsius, int resumeCelsius) {
        int warning = Math.max(resumeCelsius, autoPauseCelsius - 7);
        return new ThermalPolicy(deviceType, autoPauseCelsius, resumeCelsius, warning);
    }

    /**
     * Creates the factory default policy for the specified device type.
     */
    public static ThermalPolicy defaultFor(DeviceType type) {
        return new ThermalPolicy(
                type,
                type.getDefaultAutoPauseCelsius(),
                type.getDefaultResumeCelsius(),
                type.getDefaultWarningCelsius()
        );
    }

    /**
     * Validates whether a candidate pause/resume pair satisfies safety constraints.
     */
    public static boolean isValid(int autoPause, int resume) {
        return autoPause >= MIN_ALLOWED_TEMP + MIN_HYSTERESIS_GAP
                && autoPause <= MAX_ALLOWED_TEMP
                && resume >= MIN_ALLOWED_TEMP
                && resume <= autoPause - MIN_HYSTERESIS_GAP;
    }

    public String summary() {
        return String.format("Auto-Pause: %d°C | Resumes: %d°C (Warn: %d°C)",
                autoPauseCelsius, resumeCelsius, warningCelsius);
    }
}
