package com.sanitizer.gui.components;

/**
 * Colorblind-Optimized Color Palettes for the Sector LBA Heatmap Visualizer.
 * Provides scientifically tailored color mappings for:
 * 1. Standard (Default Red-Green-Amber)
 * 2. Deuteranopia (Red-Green blindness / green-weak, Okabe-Ito scale)
 * 3. Protanopia (Red-blind / red-weak, high luminance contrast)
 * 4. Tritanopia (Blue-Yellow blindness, Carmine-Cyan-Teal scale)
 * 5. High-Contrast Monochrome (WCAG AAA ≥10:1 contrast ratio)
 */
public enum HeatmapPalette {

    STANDARD(
            "Standard (Vibrant Red-Green)",
            "#EF4444", "#B91C1C", // Dirty
            "#F59E0B", "#FBBF24", "rgba(245, 158, 11, 0.7)", // Active Head
            "#0284C7", "#38BDF8", // Pattern
            "#10B981", "#059669", // Zeroed
            "#9333EA", "#EF4444", "rgba(147, 51, 234, 0.85)", // Bad Sector
            "#CBD5E1", "#94A3B8"  // Idle
    ),

    DEUTERANOPIA(
            "Deuteranopia-Optimized (Red-Green Deficient)",
            "#D55E00", "#9A3412", // Dirty (Vermilion)
            "#F0E442", "#FEF08A", "rgba(240, 228, 66, 0.8)", // Active Head (Vivid Yellow)
            "#56B4E9", "#38BDF8", // Pattern (Sky Blue)
            "#0072B2", "#0369A1", // Zeroed (Deep Cobalt Blue)
            "#CC79A7", "#BE185D", "rgba(204, 121, 167, 0.85)", // Bad Sector (Reddish Purple)
            "#94A3B8", "#64748B"  // Idle (Cool Slate)
    ),

    PROTANOPIA(
            "Protanopia-Optimized (Red-Weak / Blue-Amber)",
            "#E69F00", "#C2410C", // Dirty (Bright Orange)
            "#F0E442", "#FEF08A", "rgba(240, 228, 66, 0.8)", // Active Head (Luminous Yellow)
            "#009E73", "#047857", // Pattern (Teal / Bluish Green)
            "#0072B2", "#0369A1", // Zeroed (Cobalt Blue)
            "#D55E00", "#EF4444", "rgba(213, 94, 0, 0.85)", // Bad Sector (High-contrast Vermilion)
            "#64748B", "#475569"  // Idle (Neutral Slate)
    ),

    TRITANOPIA(
            "Tritanopia-Optimized (Blue-Yellow Deficient)",
            "#D81B60", "#9D174D", // Dirty (Carmine Red)
            "#FFC107", "#FEF08A", "rgba(255, 193, 7, 0.8)", // Active Head (Amber Yellow)
            "#004D40", "#065F46", // Pattern (Forest Teal)
            "#1E88E5", "#0284C7", // Zeroed (Sky Cyan)
            "#FF0055", "#E11D48", "rgba(255, 0, 85, 0.85)", // Bad Sector (Crimson)
            "#B0BEC5", "#78909C"  // Idle (Light Blue-Grey)
    ),

    HIGH_CONTRAST(
            "High-Contrast Monochrome (WCAG AAA ≥10:1)",
            "#1E293B", "#475569", // Dirty (Charcoal with light border)
            "#FEF08A", "#FFFFFF", "rgba(254, 240, 138, 0.9)", // Active Head (Neon Yellow)
            "#60A5FA", "#93C5FD", // Pattern (Ice Blue)
            "#FFFFFF", "#CBD5E1", // Zeroed (Pure White)
            "#EF4444", "#FCA5A5", "rgba(239, 68, 68, 0.9)", // Bad Sector (Vivid Coral)
            "#0F172A", "#334155"  // Idle (Pitch Black)
    );

    private final String displayName;
    private final String dirtyFill;
    private final String dirtyStroke;
    private final String activeHeadFill;
    private final String activeHeadStroke;
    private final String activeHeadGlow;
    private final String patternFill;
    private final String patternStroke;
    private final String zeroedFill;
    private final String zeroedStroke;
    private final String badSectorFill;
    private final String badSectorStroke;
    private final String badSectorGlow;
    private final String idleFill;
    private final String idleStroke;

    HeatmapPalette(String displayName,
                   String dirtyFill, String dirtyStroke,
                   String activeHeadFill, String activeHeadStroke, String activeHeadGlow,
                   String patternFill, String patternStroke,
                   String zeroedFill, String zeroedStroke,
                   String badSectorFill, String badSectorStroke, String badSectorGlow,
                   String idleFill, String idleStroke) {
        this.displayName = displayName;
        this.dirtyFill = dirtyFill;
        this.dirtyStroke = dirtyStroke;
        this.activeHeadFill = activeHeadFill;
        this.activeHeadStroke = activeHeadStroke;
        this.activeHeadGlow = activeHeadGlow;
        this.patternFill = patternFill;
        this.patternStroke = patternStroke;
        this.zeroedFill = zeroedFill;
        this.zeroedStroke = zeroedStroke;
        this.badSectorFill = badSectorFill;
        this.badSectorStroke = badSectorStroke;
        this.badSectorGlow = badSectorGlow;
        this.idleFill = idleFill;
        this.idleStroke = idleStroke;
    }

    public String getDisplayName() { return displayName; }
    public String getDirtyFill() { return dirtyFill; }
    public String getDirtyStroke() { return dirtyStroke; }
    public String getActiveHeadFill() { return activeHeadFill; }
    public String getActiveHeadStroke() { return activeHeadStroke; }
    public String getActiveHeadGlow() { return activeHeadGlow; }
    public String getPatternFill() { return patternFill; }
    public String getPatternStroke() { return patternStroke; }
    public String getZeroedFill() { return zeroedFill; }
    public String getZeroedStroke() { return zeroedStroke; }
    public String getBadSectorFill() { return badSectorFill; }
    public String getBadSectorStroke() { return badSectorStroke; }
    public String getBadSectorGlow() { return badSectorGlow; }
    public String getIdleFill() { return idleFill; }
    public String getIdleStroke() { return idleStroke; }

    @Override
    public String toString() {
        return displayName;
    }
}
