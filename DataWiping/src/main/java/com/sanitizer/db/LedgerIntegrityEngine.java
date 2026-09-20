package com.sanitizer.db;

import com.sanitizer.util.AppLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Enterprise Cryptographic Audit Ledger & Tamper-Detection Engine.
 * Implements SHA-256 block chaining (immutable hash-linked sequence):
 * Each audit entry encapsulates prev_record_hash + canonical record fields,
 * guaranteeing mathematical proof of immutability across the SQLite database.
 */
public class LedgerIntegrityEngine {

    private static final String MODULE = "LedgerIntegrityEngine";

    /**
     * Genesis Block Root Hash (64 zeroes in hex) representing the anchor of the chain.
     */
    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    public enum AnomalyType {
        GENESIS_LINK_CORRUPTED,
        PREV_HASH_CHAIN_MISMATCH,
        RECORD_PAYLOAD_TAMPERED,
        SEQUENTIAL_ID_GAP,
        UNSEALED_RECORD_FOUND
    }

    public record LedgerAnomaly(
            AnomalyType type,
            int recordId,
            String serialNumber,
            String timestamp,
            String expectedHash,
            String actualHash,
            String description
    ) {}

    public record LedgerVerificationResult(
            boolean isFullyValid,
            int totalRecordsChecked,
            int validChainLength,
            String genesisHash,
            String latestBlockHash,
            String verificationTimestampUtc,
            List<LedgerAnomaly> anomalies
    ) {
        public boolean hasAnomalies() {
            return anomalies != null && !anomalies.isEmpty();
        }

        public String getSummaryMessage() {
            if (isFullyValid) {
                return String.format(Locale.US,
                        "AUDIT LEDGER 100%% VERIFIED — All %d audit records mathematically chained. Zero mutations, deletions, or injections detected.",
                        totalRecordsChecked);
            } else {
                return String.format(Locale.US,
                        "CRITICAL TAMPER WARNING — Detected %d ledger integrity anomalies across %d records! Possible unauthorized external modifications.",
                        anomalies.size(), totalRecordsChecked);
            }
        }
    }

    /**
     * Computes the SHA-256 hash for a given audit record and its parent block hash.
     */
    public static String computeRecordHash(String prevHash,
                                           int id,
                                           String timestamp,
                                           String driveModel,
                                           String serialNumber,
                                           String capacity,
                                           String wipeStandard,
                                           String status,
                                           String digitalSignature,
                                           int preHealthScore,
                                           int postHealthScore,
                                           int badBlocksDelta,
                                           int wearDeltaPercent,
                                           String smartDeltaSummary,
                                           int peakTempCelsius,
                                           int thermalPauseCount,
                                           int crcErrors,
                                           String interfaceAnomalySummary,
                                           String verificationStatus,
                                           long verifiedSectorsCount,
                                           double entropyScore,
                                           String verificationHash) {
        String effectivePrev = (prevHash != null && !prevHash.isBlank()) ? prevHash.trim() : GENESIS_PREV_HASH;

        String canonicalPayload = String.join("|",
                effectivePrev,
                String.valueOf(id),
                timestamp != null ? timestamp.trim() : "",
                driveModel != null ? driveModel.trim() : "",
                serialNumber != null ? serialNumber.trim() : "",
                capacity != null ? capacity.trim() : "",
                wipeStandard != null ? wipeStandard.trim() : "",
                status != null ? status.trim() : "",
                digitalSignature != null ? digitalSignature.trim() : "",
                String.valueOf(preHealthScore),
                String.valueOf(postHealthScore),
                String.valueOf(badBlocksDelta),
                String.valueOf(wearDeltaPercent),
                smartDeltaSummary != null ? smartDeltaSummary.trim() : "",
                String.valueOf(peakTempCelsius),
                String.valueOf(thermalPauseCount),
                String.valueOf(crcErrors),
                interfaceAnomalySummary != null ? interfaceAnomalySummary.trim() : "",
                verificationStatus != null ? verificationStatus.trim() : "",
                String.valueOf(verifiedSectorsCount),
                String.format(Locale.US, "%.4f", entropyScore),
                verificationHash != null ? verificationHash.trim() : ""
        );

        return sha256Hex(canonicalPayload);
    }

    public static String computeRecordHash(String prevHash, AuditDb.AuditRecord record) {
        if (record == null) return GENESIS_PREV_HASH;
        return computeRecordHash(
                prevHash,
                record.id(),
                record.timestamp(),
                record.driveModel(),
                record.serialNumber(),
                record.capacity(),
                record.wipeStandard(),
                record.status(),
                record.digitalSignature(),
                record.preHealthScore(),
                record.postHealthScore(),
                record.badBlocksDelta(),
                record.wearDeltaPercent(),
                record.smartDeltaSummary(),
                record.peakTempCelsius(),
                record.thermalPauseCount(),
                record.crcErrors(),
                record.interfaceAnomalySummary(),
                record.verificationStatus(),
                record.verifiedSectorsCount(),
                record.entropyScore(),
                record.verificationHash()
        );
    }

    /**
     * Verifies cryptographic chain integrity and tamper detection across all audit records chronologically.
     */
    public static LedgerVerificationResult verifyLedgerIntegrity(List<AuditDb.AuditRecord> recordsChronological) {
        String nowUtc = Instant.now().toString();

        if (recordsChronological == null || recordsChronological.isEmpty()) {
            return new LedgerVerificationResult(
                    true, 0, 0, GENESIS_PREV_HASH, GENESIS_PREV_HASH, nowUtc, List.of()
            );
        }

        List<LedgerAnomaly> anomalies = new ArrayList<>();
        String expectedPrevHash = GENESIS_PREV_HASH;
        String genesisHash = null;
        String latestBlockHash = null;
        int validChainCount = 0;
        int previousId = -1;

        for (int i = 0; i < recordsChronological.size(); i++) {
            AuditDb.AuditRecord current = recordsChronological.get(i);
            boolean recordIsValid = true;

            // 1. Check ID sequence continuity (optional warning for row deletion)
            if (previousId != -1 && current.id() != previousId + 1) {
                // Noticeable ID gap
                anomalies.add(new LedgerAnomaly(
                        AnomalyType.SEQUENTIAL_ID_GAP,
                        current.id(),
                        current.serialNumber(),
                        current.timestamp(),
                        "Expected ID: " + (previousId + 1),
                        "Found ID: " + current.id(),
                        "Sequential ID gap detected between record " + previousId + " and " + current.id() + ". Row(s) may have been deleted."
                ));
                recordIsValid = false;
            }
            previousId = current.id();

            // 2. Check Unsealed state
            if (current.recordHash() == null || current.recordHash().isBlank()) {
                anomalies.add(new LedgerAnomaly(
                        AnomalyType.UNSEALED_RECORD_FOUND,
                        current.id(),
                        current.serialNumber(),
                        current.timestamp(),
                        "Valid SHA-256 Digest",
                        "NULL / EMPTY",
                        "Audit record #" + current.id() + " is unsealed (missing cryptographic block hash)."
                ));
                recordIsValid = false;
            }

            // 3. Check prev_record_hash linkage
            String actualPrevHash = current.prevRecordHash() != null ? current.prevRecordHash().trim() : "";
            if (i == 0) {
                // First record must point to Genesis Root
                if (!GENESIS_PREV_HASH.equalsIgnoreCase(actualPrevHash)) {
                    anomalies.add(new LedgerAnomaly(
                            AnomalyType.GENESIS_LINK_CORRUPTED,
                            current.id(),
                            current.serialNumber(),
                            current.timestamp(),
                            GENESIS_PREV_HASH,
                            actualPrevHash,
                            "Root record #" + current.id() + " prev_record_hash does not match GENESIS anchor."
                    ));
                    recordIsValid = false;
                }
            } else {
                // Subsequent records must point to the previous record's record_hash
                if (!expectedPrevHash.equalsIgnoreCase(actualPrevHash)) {
                    anomalies.add(new LedgerAnomaly(
                            AnomalyType.PREV_HASH_CHAIN_MISMATCH,
                            current.id(),
                            current.serialNumber(),
                            current.timestamp(),
                            expectedPrevHash,
                            actualPrevHash,
                            "Chain link broken at record #" + current.id() + ". prev_record_hash does not match block #" + (current.id() - 1) + " hash."
                    ));
                    recordIsValid = false;
                }
            }

            // 4. Recompute payload hash and verify against stored record_hash
            String expectedCurrentHash = computeRecordHash(actualPrevHash, current);
            String actualCurrentHash = current.recordHash() != null ? current.recordHash().trim() : "";

            if (!expectedCurrentHash.equalsIgnoreCase(actualCurrentHash)) {
                anomalies.add(new LedgerAnomaly(
                        AnomalyType.RECORD_PAYLOAD_TAMPERED,
                        current.id(),
                        current.serialNumber(),
                        current.timestamp(),
                        expectedCurrentHash,
                        actualCurrentHash,
                        "Record #" + current.id() + " payload modified or tampered outside the application!"
                ));
                recordIsValid = false;
            }

            if (recordIsValid) {
                validChainCount++;
            }

            if (i == 0) {
                genesisHash = actualCurrentHash;
            }
            latestBlockHash = actualCurrentHash;
            expectedPrevHash = actualCurrentHash;
        }

        boolean isFullyValid = anomalies.isEmpty();
        return new LedgerVerificationResult(
                isFullyValid,
                recordsChronological.size(),
                validChainCount,
                genesisHash != null ? genesisHash : GENESIS_PREV_HASH,
                latestBlockHash != null ? latestBlockHash : GENESIS_PREV_HASH,
                nowUtc,
                anomalies
        );
    }

    /**
     * Seals any unsealed or legacy records in the SQLite database chronologically.
     * Backfills prev_record_hash and record_hash in place.
     */
    public static synchronized int sealDatabaseLedgerIfUnsealed(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM wipe_logs WHERE record_hash IS NULL OR record_hash = '' OR prev_record_hash IS NULL OR prev_record_hash = '';";
        String selectAllSql = "SELECT * FROM wipe_logs ORDER BY id ASC;";
        String updateSql = "UPDATE wipe_logs SET prev_record_hash = ?, record_hash = ? WHERE id = ?;";

        int sealedCount = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rsCount = stmt.executeQuery(countSql)) {
            if (rsCount.next() && rsCount.getInt(1) == 0) {
                // Everything is already sealed
                return 0;
            }

            AppLogger.info(MODULE, "Unsealed records detected. Backfilling cryptographic block hashes in sequence...");

            List<AuditDb.AuditRecord> records = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(selectAllSql)) {
                while (rs.next()) {
                    records.add(new AuditDb.AuditRecord(
                            rs.getInt("id"),
                            rs.getString("timestamp"),
                            rs.getString("drive_model"),
                            rs.getString("serial_number"),
                            rs.getString("capacity"),
                            rs.getString("wipe_standard"),
                            rs.getString("status"),
                            rs.getString("digital_signature"),
                            rs.getInt("pre_health_score"),
                            rs.getInt("post_health_score"),
                            rs.getInt("bad_blocks_delta"),
                            rs.getInt("wear_delta_percent"),
                            rs.getString("smart_delta_summary"),
                            rs.getInt("peak_temp_celsius"),
                            rs.getInt("thermal_pause_count"),
                            rs.getInt("crc_errors"),
                            rs.getString("interface_anomaly_summary"),
                            rs.getString("verification_status"),
                            rs.getLong("verified_sectors_count"),
                            rs.getDouble("entropy_score"),
                            rs.getString("verification_hash"),
                            rs.getString("prev_record_hash"),
                            rs.getString("record_hash")
                    ));
                }
            }

            String runningPrevHash = GENESIS_PREV_HASH;
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                for (AuditDb.AuditRecord r : records) {
                    String blockHash = computeRecordHash(runningPrevHash, r);
                    updateStmt.setString(1, runningPrevHash);
                    updateStmt.setString(2, blockHash);
                    updateStmt.setInt(3, r.id());
                    updateStmt.executeUpdate();
                    runningPrevHash = blockHash;
                    sealedCount++;
                }
            }

            AppLogger.info(MODULE, "Successfully cryptographically sealed " + sealedCount + " audit records.");
        } catch (SQLException e) {
            AppLogger.error(MODULE, "Error sealing database ledger", e);
        }
        return sealedCount;
    }

    /**
     * Computes raw SHA-256 hash in lowercase hex format.
     */
    public static String sha256Hex(String input) {
        if (input == null) input = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
