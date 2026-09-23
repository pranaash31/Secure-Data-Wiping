package com.sanitizer.detector;

/**
 * Represents storage media device types with their distinctive physical thermal profiles,
 * operational characteristics, and default temperature safety thresholds.
 */
public enum DeviceType {

    USB_FLASH(
            "USB Flash / Pen Drives",
            "Compact USB thumb drives, OTG keys & flash memory",
            55, // Default Auto-Pause at 55°C
            42, // Default Resume at 42°C
            48, // Warning threshold at 48°C
            52, // Default Thermal Throttling threshold at 52°C
            "USB",
            "#F59E0B",
            "#FEF3C7"
    ),

    NVME_SSD(
            "NVMe / High-Speed SSDs",
            "High-throughput PCIe NVMe, M.2 & SATA Solid State Drives",
            70, // Default Auto-Pause at 70°C
            52, // Default Resume at 52°C
            60, // Warning threshold at 60°C
            65, // Default Thermal Throttling threshold at 65°C
            "NVMe/SSD",
            "#3B82F6",
            "#EFF6FF"
    ),

    MAGNETIC_HDD(
            "Magnetic HDDs",
            "Mechanical spinning platter drives (5400/7200/10K RPM)",
            50, // Default Auto-Pause at 50°C
            40, // Default Resume at 40°C
            45, // Warning threshold at 45°C
            48, // Default Thermal Throttling threshold at 48°C
            "HDD",
            "#10B981",
            "#ECFDF5"
    );

    private final String displayName;
    private final String description;
    private final int defaultAutoPauseCelsius;
    private final int defaultResumeCelsius;
    private final int defaultWarningCelsius;
    private final int defaultThrottleCelsius;
    private final String shortBadge;
    private final String accentColor;
    private final String bgColor;

    DeviceType(String displayName, String description,
               int defaultAutoPauseCelsius, int defaultResumeCelsius, int defaultWarningCelsius,
               int defaultThrottleCelsius,
               String shortBadge, String accentColor, String bgColor) {
        this.displayName = displayName;
        this.description = description;
        this.defaultAutoPauseCelsius = defaultAutoPauseCelsius;
        this.defaultResumeCelsius = defaultResumeCelsius;
        this.defaultWarningCelsius = defaultWarningCelsius;
        this.defaultThrottleCelsius = defaultThrottleCelsius;
        this.shortBadge = shortBadge;
        this.accentColor = accentColor;
        this.bgColor = bgColor;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getDefaultAutoPauseCelsius() { return defaultAutoPauseCelsius; }
    public int getDefaultResumeCelsius() { return defaultResumeCelsius; }
    public int getDefaultWarningCelsius() { return defaultWarningCelsius; }
    public int getDefaultThrottleCelsius() { return defaultThrottleCelsius; }
    public String getShortBadge() { return shortBadge; }
    public String getAccentColor() { return accentColor; }
    public String getBgColor() { return bgColor; }

    /**
     * Heuristically determines the storage device type from drive model, path, and capacity.
     */
    public static DeviceType fromDrive(String model, String systemPath, long sizeBytes) {
        String m = (model != null ? model.toLowerCase() : "");
        String p = (systemPath != null ? systemPath.toLowerCase() : "");

        // 1. NVMe / SSD Indicators
        if (m.contains("nvme") || m.contains("ssd") || m.contains("pcie")
                || m.contains("optane") || m.contains("solid state") || m.contains("m.2")
                || m.contains("samsung ssd") || m.contains("crucial") || m.contains("sk hynix")
                || p.contains("nvme")) {
            return NVME_SSD;
        }

        // 2. Magnetic HDD Indicators
        if (m.contains("hdd") || m.contains("hard disk") || m.contains("barracuda")
                || m.contains("wd blue") || m.contains("wd red") || m.contains("wd black")
                || m.contains("ironwolf") || m.contains("toshiba dt") || m.contains("spinpoint")
                || m.matches(".*st[0-9]{3,}.*") || m.matches(".*wd[0-9]{3,}.*")
                || m.contains("5400") || m.contains("7200") || m.contains("10k")) {
            return MAGNETIC_HDD;
        }

        // 3. Fallback: Capacity-based or standard USB flash drive
        if (sizeBytes > 500L * 1024 * 1024 * 1024) {
            // Large drives (>500GB) without SSD keyword are commonly external HDDs
            return MAGNETIC_HDD;
        }

        return USB_FLASH;
    }
}
