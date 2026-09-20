package com.sanitizer.policy;

import java.util.Objects;

/**
 * Represents a single sector sanitization pass within a wipe policy.
 */
public class WipePass {

    private int passNumber;
    private WipePatternType patternType;
    private int customByteValue; // 0 to 255 (0x00 to 0xFF) when patternType == CUSTOM_BYTE
    private String description;

    public WipePass() {
        this(1, WipePatternType.ZERO_FILL, 0x00, "Zero Fill Pass");
    }

    public WipePass(int passNumber, WipePatternType patternType, int customByteValue, String description) {
        this.passNumber = passNumber;
        this.patternType = patternType != null ? patternType : WipePatternType.ZERO_FILL;
        this.customByteValue = Math.max(0, Math.min(255, customByteValue));
        this.description = description != null ? description : "";
    }

    public int getPassNumber() {
        return passNumber;
    }

    public void setPassNumber(int passNumber) {
        this.passNumber = passNumber;
    }

    public WipePatternType getPatternType() {
        return patternType;
    }

    public void setPatternType(WipePatternType patternType) {
        this.patternType = patternType;
    }

    public int getCustomByteValue() {
        return customByteValue;
    }

    public void setCustomByteValue(int customByteValue) {
        this.customByteValue = Math.max(0, Math.min(255, customByteValue));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the formatted hex string representation (e.g. 0x00, 0xFF, 0xAA, RANDOM).
     */
    public String getPatternHex() {
        return switch (patternType) {
            case ZERO_FILL -> "0x00";
            case ONE_FILL -> "0xFF";
            case CUSTOM_BYTE -> String.format("0x%02X", customByteValue);
            case PSEUDO_RANDOM -> "CSPRNG";
            case COMPLEMENT -> "~INV";
        };
    }

    public String getDisplayName() {
        return switch (patternType) {
            case ZERO_FILL -> "Pass " + passNumber + ": Zero Fill (0x00)";
            case ONE_FILL -> "Pass " + passNumber + ": One Fill (0xFF)";
            case CUSTOM_BYTE -> String.format("Pass %d: Custom Pattern (0x%02X)", passNumber, customByteValue);
            case PSEUDO_RANDOM -> "Pass " + passNumber + ": Cryptographic Random (CSPRNG)";
            case COMPLEMENT -> "Pass " + passNumber + ": Bitwise Inversion / Complement";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WipePass wipePass)) return false;
        return passNumber == wipePass.passNumber &&
                customByteValue == wipePass.customByteValue &&
                patternType == wipePass.patternType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(passNumber, patternType, customByteValue);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
