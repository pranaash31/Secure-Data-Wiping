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
        int warningCelsius,
        int throttleCelsius
) {
    public static final int MIN_ALLOWED_TEMP = 35;
    public static final int MAX_ALLOWED_TEMP = 95;
    public static final int MIN_HYSTERESIS_GAP = 3; // Pause must be at least 3°C above Resume

    public ThermalPolicy(DeviceType deviceType, int autoPauseCelsius, int resumeCelsius, int warningCelsius) {
        this(deviceType, autoPauseCelsius, resumeCelsius, warningCelsius,
                Math.max(warningCelsius, autoPauseCelsius - (deviceType == DeviceType.NVME_SSD ? 5 : 3)));
    }

    public ThermalPolicy {
        if (deviceType == null) {
            deviceType = DeviceType.USB_FLASH;
        }
        // Sanitize bounds
        autoPauseCelsius = Math.max(MIN_ALLOWED_TEMP + MIN_HYSTERESIS_GAP, Math.min(MAX_ALLOWED_TEMP, autoPauseCelsius));
        resumeCelsius = Math.max(MIN_ALLOWED_TEMP, Math.min(autoPauseCelsius - MIN_HYSTERESIS_GAP, resumeCelsius));
        warningCelsius = Math.max(resumeCelsius, Math.min(autoPauseCelsius, warningCelsius));
        throttleCelsius = Math.max(resumeCelsius, Math.min(autoPauseCelsius, throttleCelsius));
    }

    /**
     * Creates a standard policy instance from auto-pause and resume temperatures,
     * calculating proportionate warning and throttling thresholds.
     */
    public static ThermalPolicy of(DeviceType deviceType, int autoPauseCelsius, int resumeCelsius) {
        int warning = Math.max(resumeCelsius, autoPauseCelsius - 7);
        int throttle = Math.max(warning, autoPauseCelsius - (deviceType == DeviceType.NVME_SSD ? 5 : 3));
        return new ThermalPolicy(deviceType, autoPauseCelsius, resumeCelsius, warning, throttle);
    }

    /**
     * Creates a policy instance with explicitly specified throttling threshold.
     */
    public static ThermalPolicy of(DeviceType deviceType, int autoPauseCelsius, int resumeCelsius, int throttleCelsius) {
        int warning = Math.max(resumeCelsius, throttleCelsius - 4);
        return new ThermalPolicy(deviceType, autoPauseCelsius, resumeCelsius, warning, throttleCelsius);
    }

    /**
     * Creates the factory default policy for the specified device type.
     */
    public static ThermalPolicy defaultFor(DeviceType type) {
        return new ThermalPolicy(
                type,
                type.getDefaultAutoPauseCelsius(),
                type.getDefaultResumeCelsius(),
                type.getDefaultWarningCelsius(),
                type.getDefaultThrottleCelsius()
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
        return String.format("Auto-Pause: %d°C | Resumes: %d°C (Throttle: %d°C, Warn: %d°C)",
                autoPauseCelsius, resumeCelsius, throttleCelsius, warningCelsius);
    }
}
