package com.sanitizer.alert;

import com.sanitizer.util.AppLogger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Socket-based SMTP email client for automated dispatch of security alerts.
 */
public class SmtpEmailDispatcher {

    private static final String MODULE = "SmtpEmailDispatcher";

    /**
     * Dispatches an alert email via SMTP to configured recipients.
     */
    public static boolean dispatch(AlertConfig config, WebhookDispatcher.AlertPayload payload) {
        if (config == null || !config.isSmtpEnabled() || config.getSmtpHost().isBlank() || config.getRecipientEmails().isBlank()) {
            return false;
        }

        String[] recipients = config.getRecipientEmails().split("[,;]");
        if (recipients.length == 0) return false;

        String subject = "[SecureErase Alert] " + payload.severity() + ": " + payload.title();
        String htmlBody = buildHtmlBody(payload);

        try {
            // Attempt standard SMTP connection with timeout
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(config.getSmtpHost(), config.getSmtpPort()), 5000);
                socket.setSoTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                String greeting = reader.readLine();
                if (greeting == null || !greeting.startsWith("220")) {
                    AppLogger.warn(MODULE, "Invalid SMTP greeting from " + config.getSmtpHost() + ": " + greeting);
                    return false;
                }

                sendSmtpCommand(writer, "EHLO localhost");
                readSmtpResponse(reader);

                if (!config.getSmtpUsername().isBlank() && !config.getSmtpPassword().isBlank()) {
                    sendSmtpCommand(writer, "AUTH LOGIN");
                    reader.readLine();
                    sendSmtpCommand(writer, Base64.getEncoder().encodeToString(config.getSmtpUsername().getBytes(StandardCharsets.UTF_8)));
                    reader.readLine();
                    sendSmtpCommand(writer, Base64.getEncoder().encodeToString(config.getSmtpPassword().getBytes(StandardCharsets.UTF_8)));
                    String authRes = reader.readLine();
                    if (authRes == null || !authRes.startsWith("235")) {
                        AppLogger.warn(MODULE, "SMTP authentication failed for " + config.getSmtpUsername());
                    }
                }

                sendSmtpCommand(writer, "MAIL FROM:<" + config.getFromEmail() + ">");
                reader.readLine();

                for (String rcpt : recipients) {
                    String clean = rcpt.trim();
                    if (!clean.isBlank()) {
                        sendSmtpCommand(writer, "RCPT TO:<" + clean + ">");
                        reader.readLine();
                    }
                }

                sendSmtpCommand(writer, "DATA");
                reader.readLine();

                // Write MIME Message
                writer.write("From: " + config.getFromEmail() + "\r\n");
                writer.write("To: " + config.getRecipientEmails() + "\r\n");
                writer.write("Subject: " + subject + "\r\n");
                writer.write("MIME-Version: 1.0\r\n");
                writer.write("Content-Type: text/html; charset=UTF-8\r\n");
                writer.write("X-Priority: " + (payload.severity().equalsIgnoreCase("CRITICAL") ? "1" : "3") + "\r\n");
                writer.write("\r\n");
                writer.write(htmlBody);
                writer.write("\r\n.\r\n");
                writer.flush();

                reader.readLine();
                sendSmtpCommand(writer, "QUIT");
            }

            AppLogger.info(MODULE, "Sent SMTP alert email to " + recipients.length + " recipient(s).");
            return true;
        } catch (Exception e) {
            AppLogger.warn(MODULE, "SMTP dispatch network attempt completed with status: " + e.getMessage());
            return false;
        }
    }

    private static void sendSmtpCommand(BufferedWriter writer, String cmd) throws IOException {
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    private static void readSmtpResponse(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() >= 4 && line.charAt(3) == ' ') {
                break; // Final line of multiline response (e.g. "250 OK")
            }
        }
    }

    public static String buildHtmlBody(WebhookDispatcher.AlertPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #F8FAFC; padding: 20px; }");
        sb.append(".card { background: white; border-radius: 8px; border: 1px solid #E2E8F0; padding: 24px; max-width: 600px; margin: 0 auto; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }");
        sb.append(".header { padding-bottom: 12px; border-bottom: 2px solid #E2E8F0; margin-bottom: 16px; }");
        sb.append(".title { font-size: 18px; font-weight: bold; color: #0F172A; }");
        sb.append(".badge-critical { background: #FEF2F2; color: #DC2626; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
        sb.append(".badge-warning { background: #FFFBEB; color: #D97706; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
        sb.append(".badge-success { background: #ECFDF5; color: #059669; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; }");
        sb.append(".table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }");
        sb.append(".table td { padding: 8px 6px; border-bottom: 1px solid #F1F5F9; }");
        sb.append(".table td.label { font-weight: bold; color: #64748B; width: 35%; }");
        sb.append(".footer { margin-top: 20px; font-size: 11px; color: #94A3B8; text-align: center; }");
        sb.append("</style></head><body>");

        sb.append("<div class='card'>");
        sb.append("<div class='header'>");
        String badgeClass = switch (payload.severity().toUpperCase()) {
            case "CRITICAL" -> "badge-critical";
            case "WARNING" -> "badge-warning";
            case "SUCCESS" -> "badge-success";
            default -> "badge-warning";
        };
        sb.append("<span class='").append(badgeClass).append("'>").append(payload.severity()).append("</span> ");
        sb.append("<span class='title'>").append(payload.title()).append("</span>");
        sb.append("</div>");

        sb.append("<p style='color: #334155; font-size: 14px; line-height: 1.5;'>").append(payload.description()).append("</p>");

        if (payload.details() != null && !payload.details().isEmpty()) {
            sb.append("<table class='table'>");
            for (Map.Entry<String, Object> entry : payload.details().entrySet()) {
                sb.append("<tr>");
                sb.append("<td class='label'>").append(entry.getKey()).append("</td>");
                sb.append("<td style='color: #0F172A;'>").append(entry.getValue()).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        sb.append("<div class='footer'>");
        sb.append("SecureErase Pro Enterprise Sanitization Suite • Timestamp: ").append(payload.timestampUtc()).append("<br/>");
        sb.append("Automated Security Operations Alert");
        sb.append("</div>");

        sb.append("</div></body></html>");
        return sb.toString();
    }
}
