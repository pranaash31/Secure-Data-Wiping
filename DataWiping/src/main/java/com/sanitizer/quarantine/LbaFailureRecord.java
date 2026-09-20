package com.sanitizer.quarantine;

/**
 * Granular record representing a specific sector or range of LBAs that suffered an unrecoverable hardware I/O failure.
 */
public record LbaFailureRecord(
        long startLba,
        long endLba,
        long startByteOffset,
        long endByteOffset,
        int passNumber,
        String errorType,        // e.g. "POSIX_EIO", "UNRECOVERABLE_READ", "WRITE_FAULT", "CRC_BURST", "BAD_BLOCK"
        String errorDescription,
        String timestamp
) {
    public long sectorCount() {
        return Math.max(1, endLba - startLba + 1);
    }

    public String formattedHexRange() {
        return String.format("0x%08X - 0x%08X", startLba, endLba);
    }

    public String formattedByteRange() {
        return String.format("%,d - %,d (Offset)", startByteOffset, endByteOffset);
    }

    public static LbaFailureRecord ofSingleLba(long lba, int passNumber, String errorType, String description) {
        long byteOffset = lba * 512;
        String now = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        return new LbaFailureRecord(lba, lba, byteOffset, byteOffset + 511, passNumber, errorType, description, now);
    }

    public static LbaFailureRecord ofByteOffset(long byteOffset, long lengthBytes, int passNumber, String errorType, String description) {
        long startLba = byteOffset / 512;
        long endLba = (byteOffset + Math.max(1, lengthBytes) - 1) / 512;
        String now = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now());
        return new LbaFailureRecord(startLba, endLba, byteOffset, byteOffset + lengthBytes - 1, passNumber, errorType, description, now);
    }
}
