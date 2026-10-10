package com.sanitizer.service;

import com.sanitizer.alert.AlertConfig;
import com.sanitizer.audio.AudioAlarmConfig;
import com.sanitizer.db.AuditDb;
import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.engine.WipeVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemConfigurationServiceTest {

    private File tempDbFile;
    private ConfigurationService configService;
    private SessionContext session;

    @BeforeEach
    void setUp() throws IOException {
        tempDbFile = File.createTempFile("test_config_service_", ".db");
        AuditDb.setDbUrlForTesting("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        configService = SystemConfigurationService.getInstance();
        session = new SessionContext("Lead Architect", "AGY-HQ-001", UserRole.SUPERVISOR);
    }

    @AfterEach
    void tearDown() {
        AuditDb.setDbUrlForTesting("jdbc:sqlite:sanitizer_history.db");
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testExportDatabaseBackupAsync(@TempDir Path tempDir) throws ExecutionException, InterruptedException {
        File backupDest = tempDir.resolve("backup.db").toFile();

        CompletableFuture<File> future = configService.exportDatabaseBackupAsync(backupDest);
        File result = future.get();

        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.length()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testExportDatabaseBackupAsyncNullDest() {
        CompletableFuture<File> future = configService.exportDatabaseBackupAsync(null);
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testClearAuditHistory() {
        AuditDb.saveRecord("Sample Drive", "SN-9988", "128 GB", "DoD 5220.22-M", "SUCCESS", "SIG-MOCK");
        assertThat(AuditDb.getAllRecords()).isNotEmpty();

        boolean cleared = configService.clearAuditHistory(session);
        assertThat(cleared).isTrue();

        // One reset record should be present in the database
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).driveModel()).isEqualTo("SYSTEM_RESET");
    }

    @Test
    void testAlertConfigLifecycle() {
        AlertConfig original = configService.getAlertConfig();
        assertThat(original).isNotNull();

        AlertConfig updated = new AlertConfig();
        updated.setWebhookEnabled(true);
        updated.setWebhookUrl("https://hooks.slack.com/services/TEST/MOCK/TOKEN");
        updated.setSmtpEnabled(false);

        configService.saveAlertConfig(updated, session);

        AlertConfig reloaded = configService.getAlertConfig();
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.isWebhookEnabled()).isTrue();
        assertThat(reloaded.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/TEST/MOCK/TOKEN");
    }

    @Test
    void testSendTestAlertAsync() throws ExecutionException, InterruptedException {
        AlertConfig dummy = new AlertConfig();
        dummy.setWebhookEnabled(false);
        dummy.setSmtpEnabled(false);

        CompletableFuture<Boolean> testFuture = configService.sendTestAlertAsync(dummy);
        Boolean result = testFuture.get();
        // Since no webhook or SMTP is active, dispatcher resolves cleanly without crashing
        assertThat(result).isNotNull();
    }

    @Test
    void testThermalPolicyLifecycle() {
        ThermalPolicy policy = configService.getThermalPolicy(DeviceType.NVME_SSD);
        assertThat(policy).isNotNull();

        configService.saveThermalPolicy(DeviceType.NVME_SSD, 78, 55, session);
        ThermalPolicy updated = configService.getThermalPolicy(DeviceType.NVME_SSD);
        assertThat(updated.autoPauseCelsius()).isEqualTo(78);
        assertThat(updated.resumeCelsius()).isEqualTo(55);

        configService.resetThermalPoliciesToDefaults(session);
        ThermalPolicy restored = configService.getThermalPolicy(DeviceType.NVME_SSD);
        assertThat(restored.autoPauseCelsius()).isEqualTo(ThermalPolicy.defaultFor(DeviceType.NVME_SSD).autoPauseCelsius());
    }

    @Test
    void testAudioAlarmConfigLifecycle() {
        AudioAlarmConfig cfg = configService.getAudioAlarmConfig();
        assertThat(cfg).isNotNull();

        cfg.setVolume(0.85);
        cfg.setAlarmMode(AudioAlarmConfig.AlarmMode.CHIME_ONLY);
        cfg.setVisualFlashEnabled(true);

        configService.saveAudioAlarmConfig(cfg);

        AudioAlarmConfig saved = configService.getAudioAlarmConfig();
        assertThat(saved.getVolume()).isEqualTo(0.85);
        assertThat(saved.getAlarmMode()).isEqualTo(AudioAlarmConfig.AlarmMode.CHIME_ONLY);
        assertThat(saved.isVisualFlashEnabled()).isTrue();
    }

    @Test
    void testTestAudioAlarmDoesNotThrow() {
        // Muted mode ensures no audio playback on CI/headless systems while verifying code path
        configService.testAudioAlarm(AudioAlarmConfig.AlarmMode.MUTED);
    }

    @Test
    void testCustomPolicyLifecycle() {
        List<WipePolicy> initialPolicies = configService.getAllPolicies();
        assertThat(initialPolicies).isNotEmpty();

        WipePolicy defaultPolicy = configService.getDefaultPolicy();
        assertThat(defaultPolicy).isNotNull();

        String uniqueId = "test-policy-" + UUID.randomUUID();
        WipePolicy custom = new WipePolicy(
                uniqueId,
                "Automated Integration Policy",
                "Unit testing custom wipe profiles",
                "Secure Erase QA",
                "QA-CUSTOM-01",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 1: 0x55"),
                        new WipePass(2, WipePatternType.ZERO_FILL, 0x00, "Pass 2: Zero")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0.0",
                "QA Automation"
        );

        boolean saved = configService.savePolicy(custom, session);
        assertThat(saved).isTrue();

        configService.setDefaultPolicy(custom);
        assertThat(configService.getDefaultPolicy().getId()).isEqualTo(uniqueId);

        boolean deleted = configService.deletePolicy(uniqueId, session);
        assertThat(deleted).isTrue();

        // Restore default policy to a standard
        configService.setDefaultPolicy(defaultPolicy);
    }

    @Test
    void testExportAndImportPolicyToFile(@TempDir Path tempDir) throws IOException {
        File exportFile = tempDir.resolve("export_policy.json").toFile();

        WipePolicy custom = new WipePolicy(
                "export-import-test",
                "Export Import Policy",
                "Testing JSON file export and import",
                "Standards Body",
                "STD-TEST-01",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1: 0xAA"),
                        new WipePass(2, WipePatternType.PSEUDO_RANDOM, -1, "Pass 2: Random")
                ),
                true,
                WipeVerifier.VerificationMode.EXTENSIVE_SAMPLE_10_PERCENT,
                false,
                "1.0.0",
                "Engineer"
        );

        configService.exportPolicyToFile(custom, exportFile);
        assertThat(exportFile.exists()).isTrue();
        assertThat(Files.readString(exportFile.toPath())).contains("Export Import Policy");

        WipePolicy imported = configService.importPolicyFromFile(exportFile);
        assertThat(imported).isNotNull();
        assertThat(imported.getName()).isEqualTo("Export Import Policy");
        assertThat(imported.getPassCount()).isEqualTo(2);
    }
}
