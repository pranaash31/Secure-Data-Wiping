package com.sanitizer.alert;

import com.google.gson.Gson;
import com.sanitizer.util.AppLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * High-performance Webhook Dispatcher supporting Slack, Discord, MS Teams, and Generic JSON endpoints.
 */
public class WebhookDispatcher {

    private static final String MODULE = "WebhookDispatcher";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record AlertPayload(
            String eventType,
            String title,
            String severity, // INFO, WARNING, CRITICAL, SUCCESS
            String description,
            Map<String, Object> details,
            String timestampUtc
    ) {
        public static AlertPayload of(String eventType, String title, String severity, String description, Map<String, Object> details) {
            return new AlertPayload(eventType, title, severity, description, details, Instant.now().toString());
        }
    }

    /**
     * Dispatches an alert payload to the configured webhook endpoint.
     */
    public static boolean dispatch(AlertConfig config, AlertPayload payload) {
        if (config == null || !config.isWebhookEnabled() || config.getWebhookUrl().isBlank()) {
            return false;
        }

        String jsonBody = formatPayloadForPlatform(config.getWebhookPlatform(), payload);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWebhookUrl().trim()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "SecureErasePro-AlertEngine/2.0")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                AppLogger.info(MODULE, "Webhook dispatched successfully (" + statusCode + ") to: " + config.getWebhookPlatform().getDisplayName());
                return true;
            } else {
                AppLogger.warn(MODULE, "Webhook endpoint responded with error HTTP " + statusCode + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            AppLogger.error(MODULE, "Webhook dispatch failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Formats the AlertPayload into platform-specific JSON payload structures.
     */
    public static String formatPayloadForPlatform(AlertConfig.WebhookPlatform platform, AlertPayload payload) {
        if (platform == null) platform = AlertConfig.WebhookPlatform.GENERIC_JSON;

        String emoji = switch (payload.severity().toUpperCase()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            case "SUCCESS" -> "✅";
            default -> "ℹ️";
        };

        return switch (platform) {
            case SLACK -> {
                StringBuilder sb = new StringBuilder();
                sb.append(emoji).append(" *").append(payload.title()).append("*\n");
                sb.append(payload.description()).append("\n");
                if (payload.details() != null && !payload.details().isEmpty()) {
                    for (Map.Entry<String, Object> entry : payload.details().entrySet()) {
                        sb.append("• *").append(entry.getKey()).append("*: ").append(entry.getValue()).append("\n");
                    }
                }
                sb.append("`Time (UTC): ").append(payload.timestampUtc()).append("`");

                Map<String, String> slackMap = Map.of("text", sb.toString());
                yield GSON.toJson(slackMap);
            }
            case DISCORD -> {
                StringBuilder desc = new StringBuilder();
                desc.append(payload.description()).append("\n\n");
                if (payload.details() != null && !payload.details().isEmpty()) {
                    for (Map.Entry<String, Object> entry : payload.details().entrySet()) {
                        desc.append("**").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n");
                    }
                }

                int colorCode = switch (payload.severity().toUpperCase()) {
                    case "CRITICAL" -> 0xEF4444; // Red
                    case "WARNING" -> 0xF59E0B;  // Amber
                    case "SUCCESS" -> 0x10B981;  // Green
                    default -> 0x3B82F6;         // Blue
                };

                Map<String, Object> embed = new LinkedHashMap<>();
                embed.put("title", emoji + " " + payload.title());
                embed.put("description", desc.toString());
                embed.put("color", colorCode);
                embed.put("timestamp", payload.timestampUtc());

                Map<String, Object> discordRoot = new LinkedHashMap<>();
                discordRoot.put("content", "📢 **Secure Data Wiping Security Alert**");
                discordRoot.put("embeds", java.util.List.of(embed));
                yield GSON.toJson(discordRoot);
            }
            case MS_TEAMS -> {
                StringBuilder sb = new StringBuilder();
                sb.append(payload.description()).append("<br/><br/>");
                if (payload.details() != null && !payload.details().isEmpty()) {
                    for (Map.Entry<String, Object> entry : payload.details().entrySet()) {
                        sb.append("<b>").append(entry.getKey()).append("</b>: ").append(entry.getValue()).append("<br/>");
                    }
                }

                Map<String, Object> teams = new LinkedHashMap<>();
                teams.put("@type", "MessageCard");
                teams.put("@context", "http://schema.org/extensions");
                teams.put("themeColor", payload.severity().equalsIgnoreCase("CRITICAL") ? "EF4444" : "0076D7");
                teams.put("summary", payload.title());
                teams.put("title", emoji + " " + payload.title());
                teams.put("text", sb.toString());
                yield GSON.toJson(teams);
            }
            case GENERIC_JSON -> {
                Map<String, Object> root = new LinkedHashMap<>();
                root.put("source", "SecureErasePro");
                root.put("eventType", payload.eventType());
                root.put("severity", payload.severity());
                root.put("title", payload.title());
                root.put("description", payload.description());
                root.put("timestampUtc", payload.timestampUtc());
                root.put("details", payload.details() != null ? payload.details() : Map.of());
                yield GSON.toJson(root);
            }
        };
    }
}
