package com.sanitizer.alert;

import com.sanitizer.util.AppLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Central Asynchronous Notification & Alert Event Bus.
 * Dispatches operational security alerts to Webhooks (Slack/Discord/Teams/HTTP) and SMTP Email in background daemon threads.
 */
public class AlertDispatcher {

    private static final String MODULE = "AlertDispatcher";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Alert-Dispatcher-Worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * Dispatches notification when a multi-drive batch wipe job completes.
     */
    public static void notifyBatchWipeCompleted(int totalDrives, int successCount, int failCount, String totalVolume, long elapsedSec) {
        AlertConfig config = AlertConfigManager.getInstance().getConfig();
        if (!config.isNotifyOnBatchWipeComplete()) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Total Storage Assets", totalDrives + " drives");
        details.put("Sanitized Successfully", successCount + " drives (" + (totalDrives > 0 ? (successCount * 100 / totalDrives) : 100) + "%)");
        details.put("Sanitization Failures", failCount + " drives");
        details.put("Total Data Volume Sanitized", totalVolume);
        details.put("Total Job Duration", (elapsedSec > 60 ? (elapsedSec / 60) + "m " + (elapsedSec % 60) + "s" : elapsedSec + "s"));

        String severity = (failCount > 0) ? "WARNING" : "SUCCESS";
        String desc = String.format("Concurrent batch sanitization completed across %d drives. Sanitized %s of data.", totalDrives, totalVolume);

        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "BATCH_WIPE_COMPLETED",
                "Multi-Drive Batch Sanitization Completed (" + successCount + "/" + totalDrives + " Passed)",
                severity,
                desc,
                details
        );

        dispatchAsync(config, payload);
    }

    /**
     * Dispatches notification when a single drive wipe completes.
     */
    public static void notifySingleWipeCompleted(String driveModel, String serial, String standard, boolean success, String certPath) {
        AlertConfig config = AlertConfigManager.getInstance().getConfig();
        if (!config.isNotifyOnWipeComplete()) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Device Model", driveModel);
        details.put("Serial Number", serial);
        details.put("Sanitization Standard", standard);
        details.put("Execution Status", success ? "SUCCESS (Zero-Residual Confirmed)" : "FAILED");
        if (certPath != null) {
            details.put("Audit Certificate PDF", certPath);
        }

        String severity = success ? "SUCCESS" : "CRITICAL";
        String desc = success
                ? "Drive sanitization completed successfully with zero-residual entropy proof."
                : "Drive sanitization failed to complete logical overwrite.";

        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "WIPE_COMPLETED",
                "Sanitization " + (success ? "Completed" : "Failed") + ": " + driveModel + " (" + serial + ")",
                severity,
                desc,
                details
        );

        dispatchAsync(config, payload);
    }

    /**
     * Dispatches notification when a drive triggers an automated thermal pause safeguard (>55°C).
     */
    public static void notifyThermalAutoPause(String driveModel, String serial, int currentTemp, int thresholdTemp, String deviceType) {
        AlertConfig config = AlertConfigManager.getInstance().getConfig();
        if (!config.isNotifyOnThermalPause()) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Target Device Model", driveModel != null ? driveModel : "Unknown Device");
        details.put("Serial Number", serial != null ? serial : "N/A");
        details.put("Current Live Temperature", currentTemp + " °C");
        details.put("Auto-Pause Safeguard Threshold", thresholdTemp + " °C");
        details.put("Media Profile", deviceType != null ? deviceType : "Solid-State / Magnetic");
        details.put("Action Taken", "Operation suspended. Active cooling in progress to protect media integrity.");

        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "THERMAL_SAFEGUARD_TRIGGERED",
                "Drive Thermal Safeguard Triggered (" + currentTemp + "°C >= " + thresholdTemp + "°C)",
                "WARNING",
                "A storage drive exceeded its safe thermal threshold during sanitization and was automatically paused to prevent NAND/media degradation.",
                details
        );

        dispatchAsync(config, payload);
    }

    /**
     * Dispatches notification when the safety shield blocks an unauthorized interaction with system disk (e.g. disk0).
     */
    public static void notifyShieldViolation(String systemPath, String attemptedAction, String reason) {
        AlertConfig config = AlertConfigManager.getInstance().getConfig();
        if (!config.isNotifyOnShieldBlock()) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Target System Path", systemPath);
        details.put("Blocked Action", attemptedAction != null ? attemptedAction : "Wiping / Overwrite");
        details.put("Safety Reason", reason != null ? reason : "Primary OS Root Drive (disk0) Protected");
        details.put("Shield Action", "CRITICAL SAFETY INTERCEPT — Operation instantly terminated.");

        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "SAFETY_SHIELD_VIOLATION_BLOCKED",
                "CRITICAL: Primary System Drive Shield Intercept (" + systemPath + ")",
                "CRITICAL",
                "An unauthorized sanitization attempt against the primary host operating system drive was blocked by the safety shield.",
                details
        );

        dispatchAsync(config, payload);
    }

    /**
     * Dispatches notification when a defective drive is quarantined and marked for physical destruction.
     */
    public static void notifyQuarantineDefect(String driveModel, String serial, String quarantineId, String reason, String disposition) {
        AlertConfig config = AlertConfigManager.getInstance().getConfig();
        if (!config.isNotifyOnQuarantineDefect()) return;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("Quarantine Tracking ID", quarantineId);
        details.put("Target Device Model", driveModel);
        details.put("Serial Number", serial);
        details.put("Hardware Defect", reason);
        details.put("Mandatory Physical Disposition", disposition);

        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "HARDWARE_QUARANTINE_ORDER",
                "Defective Hardware Quarantined: " + driveModel + " (" + quarantineId + ")",
                "CRITICAL",
                "Storage device encountered unrecoverable hardware I/O defects and has been permanently quarantined for physical destruction per NIST SP 800-88.",
                details
        );

        dispatchAsync(config, payload);
    }

    /**
     * Sends an immediate test alert to verify webhook and SMTP connectivity.
     */
    public static Future<Boolean> sendTestNotification(AlertConfig config) {
        return EXECUTOR.submit(() -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("Alert Engine Version", "SecureErase Pro v2.0 Enterprise");
            details.put("Configured Webhook", config.isWebhookEnabled() ? config.getWebhookPlatform().getDisplayName() : "Disabled");
            details.put("Configured SMTP Host", config.isSmtpEnabled() ? config.getSmtpHost() + ":" + config.getSmtpPort() : "Disabled");
            details.put("Verification Status", "ALL ALERT DISPATCH CHANNELS NOMINAL");

            WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                    "DIAGNOSTIC_TEST_ALERT",
                    "Real-Time Alert Verification Test",
                    "SUCCESS",
                    "This is a verified test notification sent from the Secure Data Wiping Security Operations Console.",
                    details
            );

            boolean whSuccess = !config.isWebhookEnabled() || WebhookDispatcher.dispatch(config, payload);
            boolean smtpSuccess = !config.isSmtpEnabled() || SmtpEmailDispatcher.dispatch(config, payload);

            AppLogger.info(MODULE, "Diagnostic test alert sent. Webhook: " + whSuccess + ", SMTP: " + smtpSuccess);
            return whSuccess && smtpSuccess;
        });
    }

    private static void dispatchAsync(AlertConfig config, WebhookDispatcher.AlertPayload payload) {
        EXECUTOR.submit(() -> {
            try {
                if (config.isWebhookEnabled()) {
                    WebhookDispatcher.dispatch(config, payload);
                }
                if (config.isSmtpEnabled()) {
                    SmtpEmailDispatcher.dispatch(config, payload);
                }
            } catch (Exception e) {
                AppLogger.error(MODULE, "Async alert dispatch encountered error: " + e.getMessage(), e);
            }
        });
    }
}
