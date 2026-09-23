package com.sanitizer.alert;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Real-Time Email & Webhook Alert Engine Unit Tests")
class AlertDispatcherTest {

    @Test
    @DisplayName("AlertConfig defaults are securely initialized and event triggers active")
    void testAlertConfigDefaults() {
        AlertConfig config = new AlertConfig();
        assertThat(config.isWebhookEnabled()).isFalse();
        assertThat(config.isSmtpEnabled()).isFalse();
        assertThat(config.getWebhookPlatform()).isEqualTo(AlertConfig.WebhookPlatform.GENERIC_JSON);
        assertThat(config.isNotifyOnWipeComplete()).isTrue();
        assertThat(config.isNotifyOnBatchWipeComplete()).isTrue();
        assertThat(config.isNotifyOnThermalPause()).isTrue();
        assertThat(config.isNotifyOnShieldBlock()).isTrue();
        assertThat(config.isNotifyOnQuarantineDefect()).isTrue();
    }

    @Test
    @DisplayName("Webhook formatting generates valid Slack, Discord, Teams, and Generic JSON structures")
    void testWebhookPayloadFormatting() {
        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "TEST_EVENT",
                "Critical Test Alert",
                "CRITICAL",
                "Test description for event verification",
                Map.of("Device", "/dev/rdisk3", "Temp", "62 °C")
        );

        // 1. Slack
        String slackJson = WebhookDispatcher.formatPayloadForPlatform(AlertConfig.WebhookPlatform.SLACK, payload);
        assertThat(slackJson).contains("\"text\":");
        assertThat(slackJson).contains("Critical Test Alert");
        assertThat(slackJson).contains("/dev/rdisk3");

        // 2. Discord
        String discordJson = WebhookDispatcher.formatPayloadForPlatform(AlertConfig.WebhookPlatform.DISCORD, payload);
        assertThat(discordJson).contains("\"embeds\":");
        assertThat(discordJson).contains("Critical Test Alert");

        // 3. MS Teams
        String teamsJson = WebhookDispatcher.formatPayloadForPlatform(AlertConfig.WebhookPlatform.MS_TEAMS, payload);
        assertThat(teamsJson).contains("MessageCard");
        assertThat(teamsJson).contains("Critical Test Alert");

        // 4. Generic JSON
        String genericJson = WebhookDispatcher.formatPayloadForPlatform(AlertConfig.WebhookPlatform.GENERIC_JSON, payload);
        JsonObject root = JsonParser.parseString(genericJson).getAsJsonObject();
        assertThat(root.get("eventType").getAsString()).isEqualTo("TEST_EVENT");
        assertThat(root.get("severity").getAsString()).isEqualTo("CRITICAL");
        assertThat(root.getAsJsonObject("details").get("Temp").getAsString()).isEqualTo("62 °C");
    }

    @Test
    @DisplayName("WebhookDispatcher successfully delivers HTTP POST payload to live mock endpoint")
    void testWebhookDispatcherDelivery() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedBody = new AtomicReference<>();

        HttpServer testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.createContext("/webhook/test", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            receivedBody.set(new String(bytes, StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 2);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("OK".getBytes(StandardCharsets.UTF_8));
            }
            latch.countDown();
        });
        testServer.start();

        int port = testServer.getAddress().getPort();
        String mockUrl = "http://localhost:" + port + "/webhook/test";

        try {
            AlertConfig config = new AlertConfig();
            config.setWebhookEnabled(true);
            config.setWebhookUrl(mockUrl);
            config.setWebhookPlatform(AlertConfig.WebhookPlatform.GENERIC_JSON);

            WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                    "THERMAL_ALERT",
                    "Thermal Threshold Exceeded",
                    "WARNING",
                    "Drive temperature reached 58°C",
                    Map.of("Drive", "Samsung 980 Pro", "Temp", "58°C")
            );

            boolean success = WebhookDispatcher.dispatch(config, payload);
            assertThat(success).isTrue();

            boolean received = latch.await(4, TimeUnit.SECONDS);
            assertThat(received).isTrue();
            assertThat(receivedBody.get()).contains("THERMAL_ALERT");
            assertThat(receivedBody.get()).contains("Samsung 980 Pro");
        } finally {
            testServer.stop(0);
        }
    }

    @Test
    @DisplayName("SmtpEmailDispatcher generates responsive HTML email template with priority badges")
    void testSmtpEmailDispatcherHtmlBody() {
        WebhookDispatcher.AlertPayload payload = WebhookDispatcher.AlertPayload.of(
                "SHIELD_BLOCK",
                "System Disk Shield Violation Blocked",
                "CRITICAL",
                "An attempt to write to /dev/disk0 was safely blocked by the kernel shield.",
                Map.of("Target", "/dev/disk0", "Shield Status", "ACTIVE")
        );

        String html = SmtpEmailDispatcher.buildHtmlBody(payload);
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("System Disk Shield Violation Blocked");
        assertThat(html).contains("CRITICAL");
        assertThat(html).contains("/dev/disk0");
        assertThat(html).contains("SecureErase Pro Enterprise Sanitization Suite");
    }

    @Test
    @DisplayName("AlertDispatcher dispatches operational triggers without throwing exceptions")
    void testAlertDispatcherTriggers() throws Exception {
        AlertConfig config = new AlertConfig();
        config.setWebhookEnabled(false);
        config.setSmtpEnabled(false);
        AlertConfigManager.getInstance().saveConfig(config);

        // Batch Wipe Complete
        AlertDispatcher.notifyBatchWipeCompleted(4, 4, 0, "1.8 TB", 120);

        // Single Wipe Complete
        AlertDispatcher.notifySingleWipeCompleted("WD Blue 1TB", "WD-1234", "NIST SP 800-88", true, "/tmp/cert.pdf");

        // Thermal Auto-Pause
        AlertDispatcher.notifyThermalAutoPause("Crucial P5", "CP5-5678", 58, 55, "NVMe SSD");

        // Shield Block
        AlertDispatcher.notifyShieldViolation("/dev/disk0", "Wipe Command", "Primary System Disk Protected");

        // Quarantine Defect
        AlertDispatcher.notifyQuarantineDefect("Seagate 2TB", "ST-9999", "QRN-20260920-ABCD", "POSIX EIO Bad Sectors", "Physical Degaussing");

        // Test notification future
        Future<Boolean> testFuture = AlertDispatcher.sendTestNotification(config);
        Boolean res = testFuture.get(3, TimeUnit.SECONDS);
        assertThat(res).isTrue();
    }
}
