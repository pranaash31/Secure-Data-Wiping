package com.sanitizer.audit;

import com.sanitizer.db.AuditDb;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category 10: Security Audit Logger & Cryptographic Ledger Tests")
public class SecurityAuditLoggerTest {

    @BeforeAll
    public static void setupTestDatabase() {
        String testDbUrl = "jdbc:sqlite:target/test_security_audit.db";
        new File("target").mkdirs();
        new File("target/test_security_audit.db").delete();
        AuditDb.setDbUrlForTesting(testDbUrl);
        SecurityAuditLogger.initDatabase();
    }

    @Test
    @DisplayName("Verify event logging, SHA-256 block hash computation, and querying")
    public void testEventLoggingAndChaining() {
        boolean logged1 = SecurityAuditLogger.logEvent(
                SecurityAuditLogger.EVENT_LOGIN,
                "Officer Alice", "DEF-01", "INSPECTOR",
                "User authenticated via PIN", "Auth Portal", "SUCCESS"
        );
        assertTrue(logged1);

        boolean logged2 = SecurityAuditLogger.logEvent(
                SecurityAuditLogger.EVENT_CONFIG_CHANGE,
                "Supervisor Bob", "DEF-01", "SUPERVISOR",
                "Thermal policy adjusted", "Thermal Engine", "SUCCESS"
        );
        assertTrue(logged2);

        List<SecurityAuditRecord> events = SecurityAuditLogger.getAllEvents();
        assertTrue(events.size() >= 2);

        SecurityAuditRecord latest = events.get(0);
        SecurityAuditRecord previous = events.get(1);

        assertEquals("Supervisor Bob", latest.actorName());
        assertEquals(SecurityAuditLogger.EVENT_CONFIG_CHANGE, latest.eventType());
        assertNotNull(latest.eventHash());
        assertFalse(latest.eventHash().isEmpty());

        // Verify chain link: latest's prevEventHash must equal previous's eventHash
        assertEquals(previous.eventHash(), latest.prevEventHash());
    }

    @Test
    @DisplayName("Verify Security Ledger Cryptographic Block Chain Integrity passes on intact records")
    public void testLedgerIntegrityVerification() {
        SecurityAuditLogger.logEvent(
                SecurityAuditLogger.EVENT_WIPE_COMPLETED,
                "Officer Alice", "DEF-01", "INSPECTOR",
                "Sanitization completed for /dev/disk2", "/dev/disk2", "SUCCESS"
        );

        SecurityAuditLogger.SecurityLedgerVerificationResult result =
                SecurityAuditLogger.verifySecurityLedgerIntegrity();

        assertTrue(result.isFullyValid(), "Intact security audit log chain must pass verification");
        assertTrue(result.totalEventsChecked() > 0);
        assertEquals(result.totalEventsChecked(), result.validChainLength());
        assertTrue(result.anomalies().isEmpty());
    }

    @Test
    @DisplayName("Verify convenience logger methods produce valid events")
    public void testConvenienceHelpers() {
        int initialCount = SecurityAuditLogger.getAllEvents().size();

        SecurityAuditLogger.logLogin("Inspector Test", "AGENCY-99", "INSPECTOR", true);
        SecurityAuditLogger.logLogout("Inspector Test", "AGENCY-99", "INSPECTOR");
        SecurityAuditLogger.logSessionLock("Inspector Test", "AGENCY-99", "INSPECTOR", "Idle 5m");
        SecurityAuditLogger.logSessionUnlock("Inspector Test", "AGENCY-99", "INSPECTOR", true);
        SecurityAuditLogger.logAccessDenied("Inspector Test", "AGENCY-99", "INSPECTOR", "Key Vault", "KEYVAULT_MANAGE");

        List<SecurityAuditRecord> afterEvents = SecurityAuditLogger.getAllEvents();
        assertEquals(initialCount + 5, afterEvents.size());

        SecurityAuditLogger.SecurityLedgerVerificationResult res =
                SecurityAuditLogger.verifySecurityLedgerIntegrity();
        assertTrue(res.isFullyValid());
    }
}
