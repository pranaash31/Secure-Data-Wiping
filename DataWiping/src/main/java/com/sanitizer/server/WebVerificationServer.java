package com.sanitizer.server;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.util.AppLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * High-performance, zero-dependency embedded HTTP Verification Server.
 * Serves the mobile-responsive Web Verification Portal for phone camera QR code scans
 * and REST API validation for enterprise B2B compliance audits.
 */
public class WebVerificationServer {

    private static final String MODULE = "WebVerificationServer";
    private static HttpServer server;
    private static int activePort = 8080;
    private static boolean isRunning = false;

    static {
        startServerAuto();
    }

    public static synchronized void startServerAuto() {
        if (isRunning) return;
        int[] trialPorts = {8080, 8088, 8888, 9090, 0};
        for (int port : trialPorts) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/verify", new VerifyPageHandler());
                server.createContext("/api/verify", new VerifyApiHandler());
                server.setExecutor(Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "WebVerifier-Worker");
                    t.setDaemon(true);
                    return t;
                }));
                server.start();
                activePort = server.getAddress().getPort();
                isRunning = true;
                AppLogger.info(MODULE, "Web Verification Portal started on http://localhost:" + activePort + "/verify");
                break;
            } catch (IOException e) {
                AppLogger.warn(MODULE, "Port " + port + " unavailable, trying next...");
            }
        }
    }

    public static synchronized void stopServer() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
            AppLogger.info(MODULE, "Web Verification Server stopped.");
        }
    }

    public static int getPort() {
        return activePort;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * Generates a fully qualified universal verification web link for a certificate.
     */
    public static String generateVerificationUrl(AuditDb.AuditRecord record) {
        String base = "http://localhost:" + activePort + "/verify";
        String sig = record.digitalSignature() != null ? record.digitalSignature() : "";

        return String.format("%s?certId=SAN-CERT-%d&model=%s&serial=%s&cap=%s&std=%s&status=%s&sig=%s",
                base,
                record.id(),
                urlEncode(record.driveModel()),
                urlEncode(record.serialNumber()),
                urlEncode(record.capacity()),
                urlEncode(record.wipeStandard()),
                urlEncode(record.status()),
                urlEncode(sig)
        );
    }

    private static String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0 && idx < pair.length() - 1) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    private static class VerifyPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String certId = params.getOrDefault("certId", "SAN-CERT-UNKNOWN");
            String model = params.getOrDefault("model", "Unknown Storage Device");
            String serial = params.getOrDefault("serial", "N/A");
            String cap = params.getOrDefault("cap", "N/A");
            String std = params.getOrDefault("std", "DoD 5220.22-M");
            String status = params.getOrDefault("status", "SUCCESS");
            String sig = params.getOrDefault("sig", "");

            String payload = model + "|" + serial + "|" + cap + "|" + std + "|" + status;
            boolean isValid = !sig.isBlank() && CryptoSigner.verifySignature(payload, sig);

            String html = generateHtmlPage(certId, model, serial, cap, std, status, sig, isValid);

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static class VerifyApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getRawQuery());

            String certId = params.getOrDefault("certId", "");
            String model = params.getOrDefault("model", "");
            String serial = params.getOrDefault("serial", "");
            String cap = params.getOrDefault("cap", "");
            String std = params.getOrDefault("std", "");
            String status = params.getOrDefault("status", "");
            String sig = params.getOrDefault("sig", "");

            String payload = model + "|" + serial + "|" + cap + "|" + std + "|" + status;
            boolean isValid = !sig.isBlank() && CryptoSigner.verifySignature(payload, sig);

            String json = String.format(
                    "{\"valid\":%b,\"certId\":\"%s\",\"model\":\"%s\",\"serial\":\"%s\",\"capacity\":\"%s\",\"standard\":\"%s\",\"status\":\"%s\",\"algorithm\":\"SHA256withRSA\",\"message\":\"%s\"}",
                    isValid,
                    escapeJson(certId),
                    escapeJson(model),
                    escapeJson(serial),
                    escapeJson(cap),
                    escapeJson(std),
                    escapeJson(status),
                    isValid ? "Cryptographically Authenticated" : "Invalid Signature"
            );

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    private static String generateHtmlPage(String certId, String model, String serial, String cap,
                                           String std, String status, String sig, boolean isValid) {
        String badgeColor = isValid ? "#10B981" : "#EF4444";
        String badgeBg = isValid ? "#ECFDF5" : "#FEF2F2";
        String statusTitle = isValid ? "AUTHENTIC CERTIFICATE VERIFIED" : "SIGNATURE VERIFICATION FAILED";
        String statusDesc = isValid
                ? "This sanitization certificate is cryptographically authentic, tamper-proof, and issued by an authorized compliance officer."
                : "The cryptographic digital seal on this certificate does not match the payload, indicating potential tampering.";

        String sigSnippet = (sig != null && sig.length() > 40) ? sig.substring(0, 40) + "..." : sig;

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Audit Verification — %s</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                        body { background: #0B1120; color: #F8FAFC; display: flex; justify-content: center; align-items: center; min-height: 100vh; padding: 20px; }
                        .card { background: #1E293B; border: 1px solid #334155; border-radius: 16px; width: 100%%; max-width: 580px; padding: 32px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); }
                        .header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #334155; padding-bottom: 18px; margin-bottom: 24px; }
                        .brand { font-size: 16px; font-weight: bold; color: #38BDF8; letter-spacing: 0.5px; }
                        .badge { background: %s; color: %s; border: 1px solid %s; padding: 6px 14px; border-radius: 9999px; font-size: 12px; font-weight: bold; }
                        .status-box { background: rgba(15, 23, 42, 0.6); border: 1px solid #334155; border-radius: 12px; padding: 18px; margin-bottom: 24px; text-align: center; }
                        .status-title { font-size: 17px; font-weight: bold; color: %s; margin-bottom: 6px; }
                        .status-desc { font-size: 12px; color: #94A3B8; line-height: 1.5; }
                        .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 24px; }
                        .cell { background: #0F172A; padding: 12px 14px; border-radius: 8px; border: 1px solid #1E293B; }
                        .cell-label { font-size: 10px; text-transform: uppercase; color: #64748B; font-weight: bold; margin-bottom: 4px; }
                        .cell-value { font-size: 13px; font-weight: bold; color: #F1F5F9; word-break: break-word; }
                        .esg-box { background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(6, 95, 70, 0.15)); border: 1px solid #059669; border-radius: 10px; padding: 14px; margin-bottom: 20px; font-size: 12px; color: #34D399; }
                        .sig-box { background: #0F172A; border-radius: 8px; padding: 12px; font-family: monospace; font-size: 11px; color: #94A3B8; word-break: break-all; margin-bottom: 20px; }
                        .footer { text-align: center; font-size: 11px; color: #64748B; border-top: 1px solid #334155; padding-top: 16px; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="header">
                            <div class="brand">🛡️ SECUREERASE PRO AUDIT VAULT</div>
                            <div class="badge">%s</div>
                        </div>

                        <div class="status-box">
                            <div class="status-title">%s</div>
                            <div class="status-desc">%s</div>
                        </div>

                        <div class="grid">
                            <div class="cell">
                                <div class="cell-label">Certificate ID</div>
                                <div class="cell-value">%s</div>
                            </div>
                            <div class="cell">
                                <div class="cell-label">Sanitization Method</div>
                                <div class="cell-value">%s</div>
                            </div>
                            <div class="cell">
                                <div class="cell-label">Device Model</div>
                                <div class="cell-value">%s</div>
                            </div>
                            <div class="cell">
                                <div class="cell-label">Serial Number</div>
                                <div class="cell-value">%s</div>
                            </div>
                            <div class="cell">
                                <div class="cell-label">Drive Capacity</div>
                                <div class="cell-value">%s</div>
                            </div>
                            <div class="cell">
                                <div class="cell-label">Execution Status</div>
                                <div class="cell-value" style="color: %s;">%s</div>
                            </div>
                        </div>

                        <div class="esg-box">
                            🌱 <strong>ESG Sustainability Impact:</strong> Sanitizing this %s storage drive diverted <strong>~1.4 kg of e-waste</strong> and avoided <strong>~12.6 kg of CO₂</strong> compared to physical destruction.
                        </div>

                        <div class="sig-box">
                            <strong>SHA256withRSA (2048-bit) Signature:</strong><br>
                            %s
                        </div>

                        <div class="footer">
                            Official Compliance Standard: NIST SP 800-88 Rev. 1 & DoD 5220.22-M<br>
                            Cryptographic Audit Verification Seal &copy; 2026 SecureErase Pro Enterprise
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                certId,
                badgeBg, badgeColor, badgeColor,
                badgeColor,
                certId,
                statusTitle,
                statusDesc,
                certId,
                std,
                model,
                serial,
                cap,
                badgeColor, status,
                cap,
                sigSnippet
        );
    }
}
