package com.sanitizer.service;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.pdf.CertificateGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CertificateVerificationService Unit Tests — Decoupled Verification & Token Parsing")
class CertificateVerificationServiceTest {

    private final CertificateVerificationService service = CertificateVerificationService.getInstance();

    @Test
    @DisplayName("Verify URL parameter extraction with UTF-8 decoding")
    void testExtractParam() {
        String query = "https://verify.corp/check?model=SanDisk+Ultra&serial=SD%20123&cap=32GB&std=NIST";

        assertThat(service.extractParam(query, "model")).isEqualTo("SanDisk Ultra");
        assertThat(service.extractParam(query, "serial")).isEqualTo("SD 123");
        assertThat(service.extractParam(query, "cap")).isEqualTo("32GB");
        assertThat(service.extractParam(query, "std")).isEqualTo("NIST");
        assertThat(service.extractParam(query, "nonexistent")).isEqualTo("");
        assertThat(service.extractParam(null, "model")).isEqualTo("");
        assertThat(service.extractParam(query, null)).isEqualTo("");
    }

    @Test
    @DisplayName("Verify token verification for authentic cryptographically signed URL")
    void testVerifyTokenUrlAuthentic() {
        String model = "SanDisk Ultra";
        String serial = "SD-998877";
        String cap = "32 GB";
        String std = "DoD 5220.22-M";
        String status = "SUCCESS";

        String payload = model + "|" + serial + "|" + cap + "|" + std + "|" + status;
        String signature = CryptoSigner.signData(payload);

        String encodedSig = URLEncoder.encode(signature, StandardCharsets.UTF_8);
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8);

        String tokenUrl = String.format(
                "http://localhost:8080/verify?certId=CERT-TEST-1&model=%s&serial=%s&cap=32+GB&std=DoD+5220.22-M&status=SUCCESS&sig=%s",
                encodedModel, serial, encodedSig
        );

        CertificateVerificationService.TokenVerificationResult result = service.verifyTokenUrl(tokenUrl);

        assertThat(result.isFormatValid()).isTrue();
        assertThat(result.isValid()).isTrue();
        assertThat(result.certificateId()).isEqualTo("CERT-TEST-1");
        assertThat(result.driveModel()).isEqualTo(model);
        assertThat(result.serialNumber()).isEqualTo(serial);
        assertThat(result.wipeStandard()).isEqualTo(std);
        assertThat(result.digitalSignature()).isEqualTo(signature);
        assertThat(result.esgMetrics()).isNotNull();
        assertThat(result.message()).contains("Cryptographic verification passed");
    }

    @Test
    @DisplayName("Verify token verification for tampered token URL with signature mismatch")
    void testVerifyTokenUrlTampered() {
        String payload = "SanDisk Ultra|SD-998877|32 GB|DoD 5220.22-M|SUCCESS";
        String signature = CryptoSigner.signData(payload);
        String encodedSig = URLEncoder.encode(signature, StandardCharsets.UTF_8);

        // Tampered model ("Tampered King" instead of "SanDisk Ultra")
        String tamperedUrl = String.format(
                "http://localhost:8080/verify?certId=CERT-TEST-1&model=Tampered+King&serial=SD-998877&cap=32+GB&std=DoD+5220.22-M&status=SUCCESS&sig=%s",
                encodedSig
        );

        CertificateVerificationService.TokenVerificationResult result = service.verifyTokenUrl(tamperedUrl);

        assertThat(result.isFormatValid()).isTrue();
        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).contains("mismatch");
    }

    @Test
    @DisplayName("Verify token verification handling for empty or malformed input")
    void testVerifyTokenUrlMalformed() {
        CertificateVerificationService.TokenVerificationResult emptyRes = service.verifyTokenUrl("");
        assertThat(emptyRes.isFormatValid()).isFalse();
        assertThat(emptyRes.isValid()).isFalse();

        CertificateVerificationService.TokenVerificationResult malformedRes = service.verifyTokenUrl("http://localhost:8080/invalid_without_params");
        assertThat(malformedRes.isFormatValid()).isFalse();
        assertThat(malformedRes.isValid()).isFalse();
    }

    @Test
    @DisplayName("Verify PDF verification handles null or non-existent file gracefully")
    void testVerifyPdfCertificateNotFound() {
        CertificateGenerator.CertificateVerificationResult result = service.verifyPdfCertificate(new File("non_existent_file.pdf"));
        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).contains("not found");
    }

    @Test
    @DisplayName("Verify openInBrowser handles null or blank URLs safely")
    void testOpenInBrowserSafety() {
        assertThat(service.openInBrowser(null)).isFalse();
        assertThat(service.openInBrowser("")).isFalse();
        assertThat(service.openInBrowser("   ")).isFalse();
    }
}
