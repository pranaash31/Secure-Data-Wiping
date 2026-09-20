package com.sanitizer.alert;

/**
 * Configuration model for Webhook and SMTP email alerting.
 */
public class AlertConfig {

    public enum WebhookPlatform {
        SLACK("Slack Webhook"),
        DISCORD("Discord Webhook"),
        MS_TEAMS("Microsoft Teams"),
        GENERIC_JSON("Custom HTTP JSON Webhook");

        private final String displayName;

        WebhookPlatform(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Webhook settings
    private boolean webhookEnabled = false;
    private String webhookUrl = "";
    private WebhookPlatform webhookPlatform = WebhookPlatform.GENERIC_JSON;

    // SMTP Email settings
    private boolean smtpEnabled = false;
    private String smtpHost = "smtp.gmail.com";
    private int smtpPort = 587;
    private boolean useTls = true;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private String fromEmail = "alerts@secure-erase.internal";
    private String recipientEmails = "";

    // Event Trigger Filters
    private boolean notifyOnWipeComplete = true;
    private boolean notifyOnBatchWipeComplete = true;
    private boolean notifyOnThermalPause = true;
    private boolean notifyOnShieldBlock = true;
    private boolean notifyOnQuarantineDefect = true;

    public AlertConfig() {}

    // Getters and Setters
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean webhookEnabled) { this.webhookEnabled = webhookEnabled; }

    public String getWebhookUrl() { return webhookUrl != null ? webhookUrl : ""; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public WebhookPlatform getWebhookPlatform() { return webhookPlatform != null ? webhookPlatform : WebhookPlatform.GENERIC_JSON; }
    public void setWebhookPlatform(WebhookPlatform webhookPlatform) { this.webhookPlatform = webhookPlatform; }

    public boolean isSmtpEnabled() { return smtpEnabled; }
    public void setSmtpEnabled(boolean smtpEnabled) { this.smtpEnabled = smtpEnabled; }

    public String getSmtpHost() { return smtpHost != null ? smtpHost : ""; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public int getSmtpPort() { return smtpPort > 0 ? smtpPort : 587; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public String getSmtpUsername() { return smtpUsername != null ? smtpUsername : ""; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPassword() { return smtpPassword != null ? smtpPassword : ""; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public String getFromEmail() { return fromEmail != null ? fromEmail : "alerts@secure-erase.internal"; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getRecipientEmails() { return recipientEmails != null ? recipientEmails : ""; }
    public void setRecipientEmails(String recipientEmails) { this.recipientEmails = recipientEmails; }

    public boolean isNotifyOnWipeComplete() { return notifyOnWipeComplete; }
    public void setNotifyOnWipeComplete(boolean notifyOnWipeComplete) { this.notifyOnWipeComplete = notifyOnWipeComplete; }

    public boolean isNotifyOnBatchWipeComplete() { return notifyOnBatchWipeComplete; }
    public void setNotifyOnBatchWipeComplete(boolean notifyOnBatchWipeComplete) { this.notifyOnBatchWipeComplete = notifyOnBatchWipeComplete; }

    public boolean isNotifyOnThermalPause() { return notifyOnThermalPause; }
    public void setNotifyOnThermalPause(boolean notifyOnThermalPause) { this.notifyOnThermalPause = notifyOnThermalPause; }

    public boolean isNotifyOnShieldBlock() { return notifyOnShieldBlock; }
    public void setNotifyOnShieldBlock(boolean notifyOnShieldBlock) { this.notifyOnShieldBlock = notifyOnShieldBlock; }

    public boolean isNotifyOnQuarantineDefect() { return notifyOnQuarantineDefect; }
    public void setNotifyOnQuarantineDefect(boolean notifyOnQuarantineDefect) { this.notifyOnQuarantineDefect = notifyOnQuarantineDefect; }
}
