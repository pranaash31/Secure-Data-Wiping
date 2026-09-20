package com.sanitizer.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerIntegrityTest {

    private File tempDbFile;

    @BeforeEach
    public void setUp() throws Exception {
        tempDbFile = File.createTempFile("test_ledger_", ".db");
        AuditDb.setDbUrlForTesting("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
    }

    @AfterEach
    public void tearDown() {
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    public void testGenesisRootAndDeterministicHashing() {
        String hash1 = LedgerIntegrityEngine.computeRecordHash(
                LedgerIntegrityEngine.GENESIS_PREV_HASH,
                1, "2026-09-20 12:00:00", "SanDisk 64GB", "SN-99881", "64 GB",
                "NIST SP 800-88", "SUCCESS", "SIG_RSA_SAMPLE",
                100, 100, 0, 0, "Integrity Verified: 0 Defects",
                32, 0, 0, "OPTIMAL",
                "PASS — Zero Residual Data Confirmed (0.000% Entropy)", 20480, 0.0000,
                "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );

        assertNotNull(hash1);
        assertEquals(64, hash1.length());

        // Same inputs produce exact same hash
        String hash2 = LedgerIntegrityEngine.computeRecordHash(
                LedgerIntegrityEngine.GENESIS_PREV_HASH,
                1, "2026-09-20 12:00:00", "SanDisk 64GB", "SN-99881", "64 GB",
                "NIST SP 800-88", "SUCCESS", "SIG_RSA_SAMPLE",
                100, 100, 0, 0, "Integrity Verified: 0 Defects",
                32, 0, 0, "OPTIMAL",
                "PASS — Zero Residual Data Confirmed (0.000% Entropy)", 20480, 0.0000,
                "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        assertEquals(hash1, hash2);

        // Modifying any single character produces completely different avalanche hash
        String modifiedHash = LedgerIntegrityEngine.computeRecordHash(
                LedgerIntegrityEngine.GENESIS_PREV_HASH,
                1, "2026-09-20 12:00:00", "SanDisk 64GB", "SN-99881", "64 GB",
                "NIST SP 800-88", "FAILED", "SIG_RSA_SAMPLE",
                100, 100, 0, 0, "Integrity Verified: 0 Defects",
                32, 0, 0, "OPTIMAL",
                "PASS — Zero Residual Data Confirmed (0.000% Entropy)", 20480, 0.0000,
                "SHA256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
        assertNotEquals(hash1, modifiedHash);
    }

    @Test
    public void testValidAuditDatabaseBlockChaining() {
        // Clear seeded records to start fresh
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM wipe_logs;");
        } catch (Exception e) {
            fail("Failed to clean table: " + e.getMessage());
        }

        // Insert sequential records
        AuditDb.saveRecord("Samsung EVO 256GB", "SM-EVO-1001", "256 GB", "NIST SP 800-88", "SUCCESS", "SIG_1");
        AuditDb.saveRecord("Crucial MX500 500GB", "CR-MX-2002", "500 GB", "DoD 5220.22-M", "SUCCESS", "SIG_2");
        AuditDb.saveRecord("WD Blue 1TB", "WD-BL-3003", "1 TB", "British HMG IS5", "SUCCESS", "SIG_3");

        var records = AuditDb.getAllRecordsChronological();
        assertEquals(3, records.size());

        // Verify chaining:
        // Record 1 prev_record_hash must be GENESIS
        assertEquals(LedgerIntegrityEngine.GENESIS_PREV_HASH, records.get(0).prevRecordHash());
        assertNotNull(records.get(0).recordHash());

        // Record 2 prev_record_hash must match Record 1's record_hash
        assertEquals(records.get(0).recordHash(), records.get(1).prevRecordHash());

        // Record 3 prev_record_hash must match Record 2's record_hash
        assertEquals(records.get(1).recordHash(), records.get(2).prevRecordHash());

        // Verify full ledger integrity
        var result = AuditDb.verifyDatabaseIntegrity();
        assertTrue(result.isFullyValid(), "Expected ledger to be 100% valid");
        assertEquals(3, result.totalRecordsChecked());
        assertEquals(3, result.validChainLength());
        assertFalse(result.hasAnomalies());
        assertTrue(result.getSummaryMessage().contains("100% VERIFIED"));
    }

    @Test
    public void testDetectTamperedRecordPayload() throws Exception {
        // Insert records
        AuditDb.saveRecord("Device A", "SN-A", "128 GB", "NIST SP 800-88", "SUCCESS", "SIG_A");
        AuditDb.saveRecord("Device B", "SN-B", "256 GB", "DoD 5220.22-M", "SUCCESS", "SIG_B");
        AuditDb.saveRecord("Device C", "SN-C", "512 GB", "NIST SP 800-88", "SUCCESS", "SIG_C");

        // Simulate malicious out-of-band edit to record 2 in SQLite
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE wipe_logs SET status = 'FAILED' WHERE drive_model = 'Device B';");
        }

        var result = AuditDb.verifyDatabaseIntegrity();
        assertFalse(result.isFullyValid(), "Tamper verification should catch payload modification");
        assertTrue(result.hasAnomalies());

        boolean foundTamper = result.anomalies().stream().anyMatch(
                a -> a.type() == LedgerIntegrityEngine.AnomalyType.RECORD_PAYLOAD_TAMPERED
        );
        assertTrue(foundTamper, "Should detect RECORD_PAYLOAD_TAMPERED anomaly");
    }

    @Test
    public void testDetectDeletedRecordChainGap() throws Exception {
        // Insert 4 records
        AuditDb.saveRecord("Device 1", "SN-1", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_1");
        AuditDb.saveRecord("Device 2", "SN-2", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_2");
        AuditDb.saveRecord("Device 3", "SN-3", "64 GB", "NIST SP 800-88", "SUCCESS", "SIG_3");

        var recs = AuditDb.getAllRecords();
        int middleId = recs.get(1).id();

        // Delete middle record via direct SQL
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM wipe_logs WHERE id = " + middleId + ";");
        }

        var result = AuditDb.verifyDatabaseIntegrity();
        assertFalse(result.isFullyValid(), "Deletion of row must break chain integrity");
        assertTrue(result.hasAnomalies());

        boolean foundGapOrMismatch = result.anomalies().stream().anyMatch(
                a -> a.type() == LedgerIntegrityEngine.AnomalyType.SEQUENTIAL_ID_GAP ||
                     a.type() == LedgerIntegrityEngine.AnomalyType.PREV_HASH_CHAIN_MISMATCH
        );
        assertTrue(foundGapOrMismatch, "Should flag missing block / broken chain link");
    }

    @Test
    public void testRetroactiveSealingOfUnsealedRecords() throws Exception {
        // Insert raw records with empty block hashes to simulate legacy / imported entries
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM wipe_logs;");
            stmt.execute("""
                INSERT INTO wipe_logs (drive_model, serial_number, capacity, wipe_standard, status, digital_signature, prev_record_hash, record_hash)
                VALUES ('Legacy Drive 1', 'LEGACY-001', '100 GB', 'NIST SP 800-88', 'SUCCESS', 'SIG_L1', '', '');
            """);
            stmt.execute("""
                INSERT INTO wipe_logs (drive_model, serial_number, capacity, wipe_standard, status, digital_signature, prev_record_hash, record_hash)
                VALUES ('Legacy Drive 2', 'LEGACY-002', '200 GB', 'DoD 5220.22-M', 'SUCCESS', 'SIG_L2', '', '');
            """);
        }

        // Before sealing, verification should flag unsealed records
        var preResult = AuditDb.verifyDatabaseIntegrity();
        assertFalse(preResult.isFullyValid());

        // Perform ledger sealing
        int sealedCount = AuditDb.sealDatabaseLedger();
        assertEquals(2, sealedCount);

        // After sealing, ledger should be 100% valid
        var postResult = AuditDb.verifyDatabaseIntegrity();
        assertTrue(postResult.isFullyValid());
        assertEquals(2, postResult.totalRecordsChecked());
        assertEquals(2, postResult.validChainLength());
    }
}
