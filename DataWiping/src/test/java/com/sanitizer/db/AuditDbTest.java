package com.sanitizer.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditDb WAL Mode & CRUD Unit Tests")
class AuditDbTest {

    private static final String TEST_DB_PATH = "target/test_audit_db.db";
    private static final String TEST_DB_URL = "jdbc:sqlite:" + TEST_DB_PATH;

    @BeforeEach
    void setUp() {
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        File dbWalFile = new File(TEST_DB_PATH + "-wal");
        if (dbWalFile.exists()) {
            dbWalFile.delete();
        }
        File dbShmFile = new File(TEST_DB_PATH + "-shm");
        if (dbShmFile.exists()) {
            dbShmFile.delete();
        }

        AuditDb.setDbUrlForTesting(TEST_DB_URL);
    }

    @AfterEach
    void tearDown() {
        AuditDb.setDbUrlForTesting("jdbc:sqlite:sanitizer_history.db");
    }

    @Test
    @DisplayName("SQLite Database is configured with Write-Ahead Logging (WAL) Mode")
    void testWalModeEnabled() throws Exception {
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {

            assertThat(rs.next()).isTrue();
            String journalMode = rs.getString(1);
            assertThat(journalMode).isEqualToIgnoringCase("wal");
        }
    }

    @Test
    @DisplayName("AuditDb CRUD operations - Save and Retrieve Records")
    void testSaveAndRetrieveRecords() {
        boolean saved1 = AuditDb.saveRecord(
                "SanDisk Ultra 32GB",
                "SD-TEST-1001",
                "32 GB",
                "DoD 5220.22-M",
                "SUCCESS",
                "SIG_SHA256_RSA2048_TEST_1"
        );

        boolean saved2 = AuditDb.saveRecord(
                "Kingston DT 64GB",
                "KG-TEST-2002",
                "64 GB",
                "NIST SP 800-88",
                "FAILED",
                "SIG_SHA256_RSA2048_TEST_2"
        );

        assertThat(saved1).isTrue();
        assertThat(saved2).isTrue();

        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();

        assertThat(records).hasSizeGreaterThanOrEqualTo(2);

        AuditDb.AuditRecord latest = records.get(0);
        assertThat(latest.driveModel()).isEqualTo("Kingston DT 64GB");
        assertThat(latest.serialNumber()).isEqualTo("KG-TEST-2002");
        assertThat(latest.wipeStandard()).isEqualTo("NIST SP 800-88");
        assertThat(latest.status()).isEqualTo("FAILED");
        assertThat(latest.digitalSignature()).isEqualTo("SIG_SHA256_RSA2048_TEST_2");
    }

    @Test
    @DisplayName("Success Rate calculation metrics evaluate correctly")
    void testSuccessRateMetrics() {
        List<AuditDb.AuditRecord> mockRecords = List.of(
                new AuditDb.AuditRecord(1, "2026-09-16 10:00:00", "Drive A", "SN1", "32GB", "NIST", "SUCCESS", "SIG1"),
                new AuditDb.AuditRecord(2, "2026-09-16 10:05:00", "Drive B", "SN2", "64GB", "DoD", "SUCCESS", "SIG2"),
                new AuditDb.AuditRecord(3, "2026-09-16 10:10:00", "Drive C", "SN3", "16GB", "NIST", "FAILED", "SIG3"),
                new AuditDb.AuditRecord(4, "2026-09-16 10:15:00", "Drive D", "SN4", "128GB", "DoD", "SUCCESS", "SIG4")
        );

        double rate = AuditDb.getSuccessRatePercentage(mockRecords);
        assertThat(rate).isEqualTo(75.0);

        int verifiedCount = AuditDb.getTamperVerifiedCount(mockRecords);
        assertThat(verifiedCount).isEqualTo(4);
    }
}
