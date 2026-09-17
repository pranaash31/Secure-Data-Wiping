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
}
