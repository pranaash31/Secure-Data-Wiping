package com.sanitizer.audit;

import com.sanitizer.db.AuditDb;
import com.sanitizer.util.AppLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SecurityAuditLogger — Tamper-evident, cryptographically chained activity ledger
 * tracking all security, administrative, and operational events.
 * <p>
 * Implements SHA-256 block hash chaining for FISMA / HIPAA / NIST SP 800-53 compliance.
 */
public class SecurityAuditLogger {

    private static final String MODULE = "SecurityAuditLogger";

    // Event Types
    public static final String EVENT_LOGIN = "AUTH_LOGIN";
    public static final String EVENT_LOGOUT = "AUTH_LOGOUT";
    public static final String EVENT_AUTH_FAILED = "AUTH_FAILED";
    public static final String EVENT_SESSION_LOCKED = "SESSION_LOCKED";
    public static final String EVENT_SESSION_UNLOCKED = "SESSION_UNLOCKED";
    public static final String EVENT_CONFIG_CHANGE = "CONFIG_MODIFIED";
    public static final String EVENT_POLICY_CHANGE = "POLICY_MODIFIED";
    public static final String EVENT_THERMAL_CHANGE = "THERMAL_POLICY_UPDATED";
    public static final String EVENT_ALERT_CHANGE = "ALERT_CONFIG_UPDATED";
    public static final String EVENT_WIPE_INITIATED = "WIPE_INITIATED";
    public static final String EVENT_WIPE_COMPLETED = "WIPE_COMPLETED";
    public static final String EVENT_WIPE_FAILED = "WIPE_FAILED";
    public static final String EVENT_BATCH_WIPE = "BATCH_WIPE_EVENT";
    public static final String EVENT_EXPORT_DATA = "DATA_EXPORT";
    public static final String EVENT_KEY_ROTATION = "KEYVAULT_KEY_ACTION";
    public static final String EVENT_ACCESS_DENIED = "RBAC_ACCESS_DENIED";
    public static final String EVENT_LEDGER_VERIFIED = "LEDGER_INTEGRITY_CHECK";

    static {
        initDatabase();
        seedInitialEventsIfEmpty();
    }

    private static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(AuditDb.getDbUrl());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS security_audit_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    event_type TEXT NOT NULL,
                    actor_name TEXT NOT NULL,
                    actor_agency TEXT NOT NULL,
                    actor_role TEXT NOT NULL,
                    action_summary TEXT NOT NULL,
                    target_resource TEXT NOT NULL,
                    status TEXT NOT NULL,
                    prev_event_hash TEXT NOT NULL,
                    event_hash TEXT NOT NULL
                );
                """);
        }
        return conn;
    }

    public static synchronized void initDatabase() {
        try (Connection conn = getConnection()) {
            // table created via getConnection()
        } catch (SQLException e) {
            AppLogger.error(MODULE, "Failed to initialize security_audit_events table", e);
        }
    }

    private static void seedInitialEventsIfEmpty() {
        if (!getAllEvents().isEmpty()) return;

        AppLogger.info(MODULE, "Seeding baseline tamper-evident security audit trail...");
        logEvent(EVENT_CONFIG_CHANGE, "System Initializer", "GOV-DEF-8942", "CHIEF_AUDITOR",
                "Cryptographic Root Key Sealed & Security Subsystem Bootstrapped", "Hardware Key Store", "SUCCESS");
        logEvent(EVENT_LOGIN, "Chief Auditor Davis", "GOV-DEF-8942", "CHIEF_AUDITOR",
                "Initial Security Profile & Clearance Attestation Established", "Authentication Subsystem", "SUCCESS");
        logEvent(EVENT_POLICY_CHANGE, "Supervisor Vance", "GOV-DEF-8942", "SUPERVISOR",
                "Applied NIST SP 800-88 & DoD 5220.22-M Standard Policies", "Policy Engine", "SUCCESS");
        logEvent(EVENT_LOGIN, "Officer Pranaash", "GOV-DEF-8942", "INSPECTOR",
                "Session Authenticated (Tier 1 Operator Clearance)", "Authentication Subsystem", "SUCCESS");
    }

    /**
     * Computes the SHA-256 block hash for an event.
     */
    public static String computeEventHash(String prevHash, int id, String timestamp, String eventType,
                                          String actorName, String actorAgency, String actorRole,
                                          String actionSummary, String targetResource, String status) {
        String safePrev = (prevHash != null && !prevHash.isBlank()) ? prevHash.trim() : SecurityAuditRecord.GENESIS_PREV_HASH;
        String safeTimestamp = timestamp != null ? timestamp.trim() : "";
        String safeEventType = eventType != null ? eventType.trim() : "";
        String safeActorName = actorName != null ? actorName.trim() : "";
        String safeActorAgency = actorAgency != null ? actorAgency.trim() : "";
        String safeActorRole = actorRole != null ? actorRole.trim() : "";
        String safeSummary = actionSummary != null ? actionSummary.trim() : "";
        String safeTarget = targetResource != null ? targetResource.trim() : "";
        String safeStatus = status != null ? status.trim() : "SUCCESS";

        String canonicalPayload = String.join("|",
                safePrev,
                String.valueOf(id),
                safeTimestamp,
                safeEventType,
                safeActorName,
                safeActorAgency,
                safeActorRole,
                safeSummary,
                safeTarget,
                safeStatus
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    /**
     * Core method to append a tamper-evident, SHA-256 block-chained security audit event.
     */
    public static synchronized boolean logEvent(String eventType, String actorName, String actorAgency,
                                                String actorRole, String actionSummary, String targetResource,
                                                String status) {
        String insertSql = """
            INSERT INTO security_audit_events(
                event_type, actor_name, actor_agency, actor_role, action_summary, target_resource, status, prev_event_hash, event_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = getConnection()) {
            // 1. Fetch latest record's hash to form next chain link
            String prevHash = SecurityAuditRecord.GENESIS_PREV_HASH;
            String queryLatestSql = "SELECT event_hash FROM security_audit_events WHERE event_hash IS NOT NULL AND event_hash != '' ORDER BY id DESC LIMIT 1;";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryLatestSql)) {
                if (rs.next()) {
                    String last = rs.getString("event_hash");
                    if (last != null && !last.isBlank()) {
                        prevHash = last.trim();
                    }
                }
            }

            // 2. Insert provisional record
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, eventType);
                pstmt.setString(2, actorName);
                pstmt.setString(3, actorAgency);
                pstmt.setString(4, actorRole);
                pstmt.setString(5, actionSummary);
                pstmt.setString(6, targetResource);
                pstmt.setString(7, status);
                pstmt.setString(8, prevHash);
                pstmt.setString(9, ""); // provisional
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int insertedId = generatedKeys.getInt(1);
                        String queryTimeSql = "SELECT timestamp FROM security_audit_events WHERE id = ?;";
                        try (PreparedStatement selStmt = conn.prepareStatement(queryTimeSql)) {
                            selStmt.setInt(1, insertedId);
                            try (ResultSet rsTime = selStmt.executeQuery()) {
                                if (rsTime.next()) {
                                    String actualTimestamp = rsTime.getString("timestamp");
                                    String sealedHash = computeEventHash(prevHash, insertedId, actualTimestamp,
                                            eventType, actorName, actorAgency, actorRole, actionSummary, targetResource, status);

                                    String updateSql = "UPDATE security_audit_events SET event_hash = ? WHERE id = ?;";
                                    try (PreparedStatement updStmt = conn.prepareStatement(updateSql)) {
                                        updStmt.setString(1, sealedHash);
                                        updStmt.setInt(2, insertedId);
                                        updStmt.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AppLogger.info(MODULE, String.format("[%s] %s (%s): %s -> %s", eventType, actorName, actorRole, actionSummary, status));
            return true;
        } catch (SQLException e) {
            AppLogger.error(MODULE, "Failed to write security audit log entry", e);
            return false;
        }
    }

    // ── Convenience Helpers ──────────────────────────────────────────────────

    public static void logLogin(String officer, String agency, String role, boolean success) {
        logEvent(
                success ? EVENT_LOGIN : EVENT_AUTH_FAILED,
                officer, agency, role,
                success ? "Officer authenticated successfully (Session established)" : "Authentication credentials rejected (Invalid PIN / clearance)",
                "Authentication Portal",
                success ? "SUCCESS" : "FAILED"
        );
    }

    public static void logLogout(String officer, String agency, String role) {
        logEvent(EVENT_LOGOUT, officer, agency, role, "Officer signed out from session", "Session Manager", "SUCCESS");
    }

    public static void logSessionLock(String officer, String agency, String role, String triggerReason) {
        logEvent(EVENT_SESSION_LOCKED, officer, agency, role, "Session Auto-Locked (" + triggerReason + ") — FISMA/HIPAA Requirement", "Session Guard", "WARNING");
    }

    public static void logSessionUnlock(String officer, String agency, String role, boolean success) {
        logEvent(EVENT_SESSION_UNLOCKED, officer, agency, role,
                success ? "Session unlocked via PIN re-authentication" : "Session unlock attempt failed (Invalid PIN)",
                "Session Guard",
                success ? "SUCCESS" : "FAILED"
        );
    }

    public static void logConfigChange(String officer, String agency, String role, String configType, String details) {
        logEvent(EVENT_CONFIG_CHANGE, officer, agency, role, "Configuration updated: " + configType + " (" + details + ")", "System Settings", "SUCCESS");
    }

    public static void logPolicyChange(String officer, String agency, String role, String action, String policyName) {
        logEvent(EVENT_POLICY_CHANGE, officer, agency, role, "Wipe Policy " + action + ": " + policyName, "Wipe Policy Manager", "SUCCESS");
    }

    public static void logThermalChange(String officer, String agency, String role, String details) {
        logEvent(EVENT_THERMAL_CHANGE, officer, agency, role, "Hardware thermal throttling policies modified (" + details + ")", "Thermal Policy Manager", "SUCCESS");
    }

    public static void logAlertChange(String officer, String agency, String role, String details) {
        logEvent(EVENT_ALERT_CHANGE, officer, agency, role, "Real-time alert dispatchers updated (" + details + ")", "Alert Dispatcher", "SUCCESS");
    }

    public static void logWipeAction(String eventType, String officer, String agency, String role, String drive, String standard, String status) {
        logEvent(eventType, officer, agency, role, "Sector wipe operation: standard=" + standard + ", drive=" + drive, drive, status);
    }

    public static void logExport(String officer, String agency, String role, String exportType, String path) {
        logEvent(EVENT_EXPORT_DATA, officer, agency, role, "Audit export generated: " + exportType + " (" + path + ")", "Audit Exporter", "SUCCESS");
    }

    public static void logKeyAction(String officer, String agency, String role, String action, String keyId) {
        logEvent(EVENT_KEY_ROTATION, officer, agency, role, "Cryptographic Key Action: " + action + " [" + keyId + "]", "Key Vault", "SUCCESS");
    }

    public static void logAccessDenied(String officer, String agency, String role, String resource, String requiredPermission) {
        logEvent(EVENT_ACCESS_DENIED, officer, agency, role,
                "Unauthorized access attempt blocked: requires permission " + requiredPermission,
                resource,
                "BLOCKED"
        );
    }

    // ── Queries & Verification ───────────────────────────────────────────────

    public static List<SecurityAuditRecord> getAllEvents() {
        List<SecurityAuditRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM security_audit_events ORDER BY id DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(new SecurityAuditRecord(
                        rs.getInt("id"),
                        rs.getString("timestamp"),
                        rs.getString("event_type"),
                        rs.getString("actor_name"),
                        rs.getString("actor_agency"),
                        rs.getString("actor_role"),
                        rs.getString("action_summary"),
                        rs.getString("target_resource"),
                        rs.getString("status"),
                        rs.getString("prev_event_hash"),
                        rs.getString("event_hash")
                ));
            }
        } catch (SQLException e) {
            AppLogger.error(MODULE, "Error querying security_audit_events", e);
        }
        return records;
    }

    public static List<SecurityAuditRecord> getAllEventsChronological() {
        List<SecurityAuditRecord> records = getAllEvents();
        Collections.reverse(records);
        return records;
    }

    /**
     * Result of cryptographic integrity check on security audit log.
     */
    public record SecurityLedgerVerificationResult(
            boolean isFullyValid,
            int totalEventsChecked,
            int validChainLength,
            String genesisHash,
            String latestBlockHash,
            List<String> anomalies
    ) {
        public String getSummaryMessage() {
            if (isFullyValid) {
                return String.format(
                        "All %d security audit events verified successfully. SHA-256 block chain is 100%% continuous, tamper-evident, and mathematically sound.",
                        totalEventsChecked
                );
            } else {
                return String.format(
                        "INTEGRITY ANOMALIES DETECTED: %d of %d security events failed hash or block chain continuity verification.",
                        anomalies.size(), totalEventsChecked
                );
            }
        }
    }

    public static SecurityLedgerVerificationResult verifySecurityLedgerIntegrity() {
        List<SecurityAuditRecord> records = getAllEventsChronological();
        if (records.isEmpty()) {
            return new SecurityLedgerVerificationResult(true, 0, 0, SecurityAuditRecord.GENESIS_PREV_HASH, SecurityAuditRecord.GENESIS_PREV_HASH, List.of());
        }

        List<String> anomalies = new ArrayList<>();
        String expectedPrevHash = SecurityAuditRecord.GENESIS_PREV_HASH;
        int validChainLength = 0;

        for (SecurityAuditRecord r : records) {
            // Check link to previous
            if (!expectedPrevHash.equalsIgnoreCase(r.prevEventHash())) {
                anomalies.add(String.format("Record #%d broken chain: expected prevHash %s but found %s",
                        r.id(), expectedPrevHash, r.prevEventHash()));
            }

            // Check self hash recomputation
            String recomputed = computeEventHash(
                    r.prevEventHash(),
                    r.id(),
                    r.timestamp(),
                    r.eventType(),
                    r.actorName(),
                    r.actorAgency(),
                    r.actorRole(),
                    r.actionSummary(),
                    r.targetResource(),
                    r.status()
            );

            if (!recomputed.equalsIgnoreCase(r.eventHash())) {
                anomalies.add(String.format("Record #%d hash mismatch: stored %s, recomputed %s",
                        r.id(), r.eventHash(), recomputed));
            } else {
                validChainLength++;
            }

            expectedPrevHash = r.eventHash();
        }

        boolean valid = anomalies.isEmpty();
        String genesis = records.get(0).prevEventHash();
        String latest = records.get(records.size() - 1).eventHash();

        return new SecurityLedgerVerificationResult(valid, records.size(), validChainLength, genesis, latest, anomalies);
    }
}
