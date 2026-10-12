package com.sanitizer.service;

import com.sanitizer.alert.AlertConfig;
import com.sanitizer.alert.AlertConfigManager;
import com.sanitizer.alert.AlertDispatcher;
import com.sanitizer.audio.AudioAlarmConfig;
import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.policy.WipePolicyManager;
import com.sanitizer.session.SessionAutoLockManager;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.util.AppLogger;
import com.sanitizer.util.SoundManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete system configuration service centralizing storage engine maintenance,
 * alert triggers, thermal policies, operator audio chimes, and custom wipe policy profiles.
 */
public class SystemConfigurationService implements ConfigurationService {

    private static final String MODULE = "SystemConfigService";

    private final ExecutorService executor;

    /** Thread-safe Initialization-on-Demand Holder Singleton. */
    private static class InstanceHolder {
        private static final SystemConfigurationService INSTANCE = new SystemConfigurationService();
    }

    public static SystemConfigurationService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public SystemConfigurationService() {
        AtomicInteger count = new AtomicInteger(1);
        this.executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "config-service-worker-" + count.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    public SystemConfigurationService(ExecutorService executor) {
        this.executor = (executor != null) ? executor : Executors.newCachedThreadPool();
    }

    private SessionContext resolveSession(SessionContext session) {
        return (session != null) ? session : new SessionContext("System Administrator", "GOV-DEF-8942", UserRole.SUPERVISOR);
    }

    // ── Database Maintenance ───────────────────────────────────────────────

    @Override
    public CompletableFuture<File> exportDatabaseBackupAsync(File destinationFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (destinationFile == null) {
                throw new IllegalArgumentException("Destination backup file cannot be null");
            }

            try {
                // Execute atomic SQLite WAL checkpoint and native VACUUM INTO
                AuditDb.backupDatabase(destinationFile);
                AppLogger.info(MODULE, "Successfully exported atomic SQLite database backup to: " + destinationFile.getAbsolutePath());
                return destinationFile;
            } catch (Exception e) {
                AppLogger.error(MODULE, "Failed to export database backup: " + e.getMessage(), e);
                throw new RuntimeException("Database backup failed: " + e.getMessage(), e);
            }
        }, executor);
    }

    @Override
    public boolean clearAuditHistory(SessionContext session) {
        SessionContext ctx = resolveSession(session);
        // Enforce RBAC clearance check (SUPERVISOR or CHIEF_AUDITOR clearance required)
        if (!ctx.hasPermission(UserRole.Permission.AUDIT_CLEAR) && !ctx.isRoleAtLeast(UserRole.SUPERVISOR)) {
            SecurityAuditLogger.logEvent(
                    "DATABASE_CLEAR_DENIED",
                    ctx.officerName(), ctx.agencyId(), ctx.role(),
                    "Unauthorized purge attempt: User lacks clearance to clear database",
                    "sanitizer_history.db",
                    "FAILED"
            );
            AppLogger.warn(MODULE, "Access denied: Officer " + ctx.officerName() + " (" + ctx.role() + ") lacks clearance to clear audit logs");
            return false;
        }

        try {
            // Step 1: Transactional deletion of existing records
            AuditDb.purgeAuditHistory();

            // Step 2: Seed fresh genesis state with SYSTEM_RESET anchor
            AuditDb.saveRecord("SYSTEM_RESET", "N/A", "0 GB", "LOG_CLEAR", "RESET", "N/A");

            // Step 3: Immutable SIEM Security Audit Logging
            SecurityAuditLogger.logEvent(
                    "DATABASE_CLEARED",
                    ctx.officerName(), ctx.agencyId(), ctx.role(),
                    "Audit log purged by authorized administrator (SYSTEM_RESET recorded)",
                    "sanitizer_history.db",
                    "WARNING"
            );
            AppLogger.info(MODULE, "Audit log history cleared and reset by " + ctx.officerName());
            return true;
        } catch (Exception e) {
            AppLogger.error(MODULE, "Failed to clear audit history: " + e.getMessage(), e);
            return false;
        }
    }

    // ── Alerts & Notifications ─────────────────────────────────────────────

    @Override
    public AlertConfig getAlertConfig() {
        return AlertConfigManager.getInstance().getConfig();
    }

    @Override
    public void saveAlertConfig(AlertConfig config, SessionContext session) {
        if (config == null) return;
        AlertConfigManager.getInstance().saveConfig(config);
        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logAlertChange(
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                "Webhook=" + config.isWebhookEnabled() + ", SMTP=" + config.isSmtpEnabled()
        );
    }

    @Override
    public CompletableFuture<Boolean> sendTestAlertAsync(AlertConfig draft) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Future<Boolean> testFuture = AlertDispatcher.sendTestNotification(draft);
                return testFuture != null && testFuture.get(15, TimeUnit.SECONDS);
            } catch (Exception e) {
                AppLogger.error(MODULE, "Failed to send test alert: " + e.getMessage(), e);
                return false;
            }
        }, executor);
    }

    // ── Hardware Thermal Throttling Policies ───────────────────────────────

    @Override
    public ThermalPolicy getThermalPolicy(DeviceType type) {
        return ThermalPolicyManager.getInstance().getPolicy(type);
    }

    @Override
    public void saveThermalPolicy(DeviceType type, int pauseCelsius, int resumeCelsius, SessionContext session) {
        ThermalPolicyManager.getInstance().setPolicy(type, pauseCelsius, resumeCelsius);
        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logThermalChange(
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                type.name() + ": Pause=" + pauseCelsius + "°C, Resume=" + resumeCelsius + "°C"
        );
    }

    @Override
    public void resetThermalPoliciesToDefaults(SessionContext session) {
        ThermalPolicyManager.getInstance().resetToDefaults();
        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logThermalChange(
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                "Reset all thermal policies to factory defaults"
        );
    }

    // ── Operator Bench Audio & Visual Alarms ───────────────────────────────

    @Override
    public AudioAlarmConfig getAudioAlarmConfig() {
        return SoundManager.getConfig();
    }

    @Override
    public void saveAudioAlarmConfig(AudioAlarmConfig config) {
        if (config == null) return;
        config.save();
        SoundManager.setConfig(config);
    }

    @Override
    public void testAudioAlarm(AudioAlarmConfig.AlarmMode mode) {
        SoundManager.testAlarm(mode);
    }

    // ── Session Auto-Lock Security Controls ─────────────────────────────────

    @Override
    public int getAutoLockTimeoutSeconds() {
        return SessionAutoLockManager.getInstance().getTimeoutSeconds();
    }

    @Override
    public void saveAutoLockTimeout(int timeoutSeconds, SessionContext session) {
        SessionAutoLockManager.getInstance().setTimeoutSeconds(timeoutSeconds);
        SessionContext ctx = resolveSession(session);
        SecurityAuditLogger.logConfigChange(
                ctx.officerName(), ctx.agencyId(), ctx.role(),
                "Session Auto-Lock Timeout",
                timeoutSeconds == 0 ? "Disabled" : timeoutSeconds + "s"
        );
        AppLogger.info(MODULE, "Session auto-lock timeout updated to: " + timeoutSeconds + "s by " + ctx.officerName());
    }

    // ── Custom Wipe Standard & Pattern Profiles ────────────────────────────

    @Override
    public List<WipePolicy> getAllPolicies() {
        return new ArrayList<>(WipePolicyManager.getInstance().getAllPolicies());
    }

    @Override
    public WipePolicy getDefaultPolicy() {
        return WipePolicyManager.getInstance().getDefaultPolicy();
    }

    @Override
    public void setDefaultPolicy(WipePolicy policy) {
        if (policy != null) {
            WipePolicyManager.getInstance().setDefaultPolicy(policy);
        }
    }

    @Override
    public boolean savePolicy(WipePolicy policy, SessionContext session) {
        if (policy == null) return false;
        boolean saved = WipePolicyManager.getInstance().savePolicy(policy);
        if (saved) {
            SessionContext ctx = resolveSession(session);
            SecurityAuditLogger.logPolicyChange(
                    ctx.officerName(), ctx.agencyId(), ctx.role(),
                    "Saved Custom Policy", policy.getName()
            );
        }
        return saved;
    }

    @Override
    public boolean deletePolicy(String policyId, SessionContext session) {
        if (policyId == null) return false;
        boolean deleted = WipePolicyManager.getInstance().deletePolicy(policyId);
        if (deleted) {
            SessionContext ctx = resolveSession(session);
            SecurityAuditLogger.logPolicyChange(
                    ctx.officerName(), ctx.agencyId(), ctx.role(),
                    "Deleted Custom Policy", policyId
            );
        }
        return deleted;
    }

    @Override
    public void exportPolicyToFile(WipePolicy policy, File targetFile) throws IOException {
        WipePolicyManager.exportPolicyToFile(policy, targetFile);
    }

    @Override
    public WipePolicy importPolicyFromFile(File sourceFile) throws IOException, IllegalArgumentException {
        return WipePolicyManager.importPolicyFromFile(sourceFile);
    }
}
