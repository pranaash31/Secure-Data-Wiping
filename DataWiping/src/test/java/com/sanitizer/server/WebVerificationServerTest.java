package com.sanitizer.server;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebVerificationServer & Scan-to-Verify Portal Tests")
class WebVerificationServerTest {

    @Test
    @DisplayName("Verify WebVerificationServer starts and generates valid QR verification URLs")
    void testServerStatusAndUrlGeneration() {
        assertThat(WebVerificationServer.isRunning()).isTrue();
        assertThat(WebVerificationServer.getPort()).isGreaterThan(0);

        String payload = "SanDisk Ultra 128GB|SN-88291|128 GB|DoD 5220.22-M|SUCCESS";
        String sig = CryptoSigner.signData(payload);

        AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                42,
                "2026-09-17 12:00:00",
                "SanDisk Ultra 128GB",
                "SN-88291",
                "128 GB",
                "DoD 5220.22-M",
                "SUCCESS",
                sig
        );

        String url = WebVerificationServer.generateVerificationUrl(record);
        assertThat(url).contains("/verify?certId=SAN-CERT-42");
        assertThat(url).contains("model=SanDisk+Ultra+128GB");
        assertThat(url).contains("sig=");
    }

    @Test
    @DisplayName("HTTP GET /verify with valid signature returns 200 OK and authentic HTML page")
    void testVerifyEndpointHttp() throws Exception {
        String payload = "Kingston DataTraveler|KN-9921|64 GB|NIST SP 800-88|SUCCESS";
        String sig = CryptoSigner.signData(payload);

        AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                99,
                "2026-09-17 12:00:00",
                "Kingston DataTraveler",
                "KN-9921",
                "64 GB",
                "NIST SP 800-88",
                "SUCCESS",
                sig
        );

        String verifyUrl = WebVerificationServer.generateVerificationUrl(record);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("AUTHENTIC CERTIFICATE VERIFIED");
        assertThat(response.body()).contains("Kingston DataTraveler");
        assertThat(response.body()).contains("ESG Sustainability Impact");
    }

    @Test
    @DisplayName("HTTP GET /api/verify returns valid JSON with cryptographic validation")
    void testApiVerifyEndpointJson() throws Exception {
        String payload = "Samsung BAR Plus|SM-101|256 GB|DoD 5220.22-M|SUCCESS";
        String sig = CryptoSigner.signData(payload);

        String apiUrl = String.format("http://localhost:%d/api/verify?certId=SAN-CERT-101&model=Samsung+BAR+Plus&serial=SM-101&cap=256+GB&std=DoD+5220.22-M&status=SUCCESS&sig=%s",
                WebVerificationServer.getPort(),
                java.net.URLEncoder.encode(sig, java.nio.charset.StandardCharsets.UTF_8)
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"valid\":true");
        assertThat(response.body()).contains("\"model\":\"Samsung BAR Plus\"");
        assertThat(response.body()).contains("\"algorithm\":\"SHA256withRSA\"");
        assertThat(response.body()).contains("\"esg\":{");
        assertThat(response.body()).contains("\"eWasteDivertedKg\":");
    }

    @Test
    @DisplayName("HTML entity escaping sanitizes malicious parameters against XSS injection")
    void testHtmlEntityEscapingPreventsXss() throws Exception {
        String xssModel = "<script>alert('pwned')</script>";
        String xssSerial = "\"><img src=x onerror=alert(1)>";

        String verifyUrl = String.format("http://localhost:%d/verify?certId=SAN-CERT-XSS&model=%s&serial=%s&cap=128+GB&std=DoD+5220.22-M&status=SUCCESS&sig=dummy",
                WebVerificationServer.getPort(),
                java.net.URLEncoder.encode(xssModel, java.nio.charset.StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(xssSerial, java.nio.charset.StandardCharsets.UTF_8)
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();

        // Must NOT contain raw executable script or img tags
        assertThat(body).doesNotContain("<script>alert('pwned')</script>");
        assertThat(body).doesNotContain("<img src=x onerror=alert(1)>");

        // Must contain escaped HTML entities
        assertThat(body).contains("&lt;script&gt;");
        assertThat(body).contains("&lt;/script&gt;");
        assertThat(body).contains("&lt;img src=x onerror=alert(1)&gt;");
    }

    @Test
    @DisplayName("External QR code URL configuration and LAN IP resolution")
    void testExternalQrCodeUrlConfiguration() {
        // 1. Verify LAN IP resolution
        String lanIp = WebVerificationServer.resolveLocalLanIp();
        assertThat(lanIp).isNotNull().isNotBlank();

        // 2. Default base URL contains resolved LAN IP or localhost
        String defaultBase = WebVerificationServer.getBaseVerificationUrl();
        assertThat(defaultBase).startsWith("http://");
        assertThat(defaultBase).contains(":" + WebVerificationServer.getPort() + "/verify");

        // 3. Test external domain configuration
        try {
            WebVerificationServer.setBaseVerificationUrl("https://verify.enterprise.com/cert");
            assertThat(WebVerificationServer.getBaseVerificationUrl()).isEqualTo("https://verify.enterprise.com/cert");

            AuditDb.AuditRecord record = new AuditDb.AuditRecord(
                    55, "2026-10-19 12:00:00", "Enterprise NVMe", "SN-ENT-001",
                    "1 TB", "NIST SP 800-88", "SUCCESS", "MOCK_SIG"
            );

            String url = WebVerificationServer.generateVerificationUrl(record);
            assertThat(url).startsWith("https://verify.enterprise.com/cert?");
            assertThat(url).contains("certId=SAN-CERT-55");
            assertThat(url).contains("keyId=SE-KEY-");
        } finally {
            // Restore default
            WebVerificationServer.setBaseVerificationUrl(null);
        }
    }
}
