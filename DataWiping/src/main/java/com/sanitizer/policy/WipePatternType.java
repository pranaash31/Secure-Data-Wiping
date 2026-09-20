package com.sanitizer.policy;

/**
 * Standard data wiping pattern types.
 */
public enum WipePatternType {
    ZERO_FILL("Zero Fill (0x00)", "0x00", 0x00),
    ONE_FILL("One Fill (0xFF)", "0xFF", 0xFF),
    CUSTOM_BYTE("Custom Byte Pattern", "0x??", -1),
    PSEUDO_RANDOM("CSPRNG Cryptographic Random", "RANDOM", -1),
    COMPLEMENT("Bitwise Inversion / Complement", "COMPLEMENT", -1);

    private final String displayName;
    private final String defaultHex;
    private final int defaultByte;

    WipePatternType(String displayName, String defaultHex, int defaultByte) {
        this.displayName = displayName;
        this.defaultHex = defaultHex;
        this.defaultByte = defaultByte;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultHex() {
        return defaultHex;
    }

    public int getDefaultByte() {
        return defaultByte;
    }
}
