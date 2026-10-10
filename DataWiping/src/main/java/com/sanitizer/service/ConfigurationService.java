package com.sanitizer.service;

import com.sanitizer.alert.AlertConfig;
import com.sanitizer.audio.AudioAlarmConfig;
import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.session.SessionContext;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service contract managing enterprise system settings, policy profiles,
 * live hardware limits, database maintenance, and communication dispatchers.
 */
public interface ConfigurationService {

    // ── Database Maintenance ───────────────────────────────────────────────
    CompletableFuture<File> exportDatabaseBackupAsync(File destinationFile);
    boolean clearAuditHistory(SessionContext session);

    // ── Alerts & Notifications ─────────────────────────────────────────────
    AlertConfig getAlertConfig();
    void saveAlertConfig(AlertConfig config, SessionContext session);
    CompletableFuture<Boolean> sendTestAlertAsync(AlertConfig draft);

    // ── Hardware Thermal Throttling Policies ───────────────────────────────
    ThermalPolicy getThermalPolicy(DeviceType type);
    void saveThermalPolicy(DeviceType type, int pauseCelsius, int resumeCelsius, SessionContext session);
    void resetThermalPoliciesToDefaults(SessionContext session);

    // ── Operator Bench Audio & Visual Alarms ───────────────────────────────
    AudioAlarmConfig getAudioAlarmConfig();
    void saveAudioAlarmConfig(AudioAlarmConfig config);
    void testAudioAlarm(AudioAlarmConfig.AlarmMode mode);

    // ── Custom Wipe Standard & Pattern Profiles ────────────────────────────
    List<WipePolicy> getAllPolicies();
    WipePolicy getDefaultPolicy();
    void setDefaultPolicy(WipePolicy policy);
    boolean savePolicy(WipePolicy policy, SessionContext session);
    boolean deletePolicy(String policyId, SessionContext session);
    void exportPolicyToFile(WipePolicy policy, File targetFile) throws IOException;
    WipePolicy importPolicyFromFile(File sourceFile) throws IOException, IllegalArgumentException;
}
