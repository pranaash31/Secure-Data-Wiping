package com.sanitizer.policy;

import com.sanitizer.engine.WipeVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Custom Wipe Policy & International Standards Tests")
class WipePolicyTest {

    @Test
    @DisplayName("Registry initializes all required International Standards presets")
    void testBuiltinInternationalStandards() {
        WipePolicyManager manager = WipePolicyManager.getInstance();
        List<WipePolicy> policies = manager.getAllPolicies();

        assertThat(policies).isNotEmpty().hasSizeGreaterThanOrEqualTo(6);

        // 1. NIST SP 800-88
        WipePolicy nist = manager.getPolicyById("nist-800-88");
        assertThat(nist).isNotNull();
        assertThat(nist.getPassCount()).isEqualTo(1);
        assertThat(nist.getPasses().get(0).getPatternType()).isEqualTo(WipePatternType.ZERO_FILL);

        // 2. DoD 5220.22-M 3-Pass
        WipePolicy dod = manager.getPolicyById("dod-5220-22-m");
        assertThat(dod).isNotNull();
        assertThat(dod.getPassCount()).isEqualTo(3);
        assertThat(dod.getPasses().get(0).getPatternType()).isEqualTo(WipePatternType.ZERO_FILL);
        assertThat(dod.getPasses().get(1).getPatternType()).isEqualTo(WipePatternType.ONE_FILL);
        assertThat(dod.getPasses().get(2).getPatternType()).isEqualTo(WipePatternType.PSEUDO_RANDOM);

        // 3. DoD 5220.22-M ECE 7-Pass
        WipePolicy dodEce = manager.getPolicyById("dod-5220-22-m-ece");
        assertThat(dodEce).isNotNull();
        assertThat(dodEce.getPassCount()).isEqualTo(7);

        // 4. British HMG Infosec 5 (Enhanced)
        WipePolicy hmg = manager.getPolicyById("hmg-infosec-5-enhanced");
        assertThat(hmg).isNotNull();
        assertThat(hmg.getPassCount()).isEqualTo(3);
        assertThat(hmg.getOrganization()).contains("UK");

        // 5. German BSI-VS 7-Pass
        WipePolicy bsi = manager.getPolicyById("bsi-vs-7pass");
        assertThat(bsi).isNotNull();
        assertThat(bsi.getPassCount()).isEqualTo(7);
        assertThat(bsi.getPasses().get(4).getCustomByteValue()).isEqualTo(0xAA);
        assertThat(bsi.getPasses().get(5).getCustomByteValue()).isEqualTo(0x55);
        assertThat(bsi.getOrganization()).contains("BSI Germany");

        // 6. Canadian RCMP TSSIT OPS-II 7-Pass
        WipePolicy rcmp = manager.getPolicyById("rcmp-tssit-ops-ii");
        assertThat(rcmp).isNotNull();
        assertThat(rcmp.getPassCount()).isEqualTo(7);
        assertThat(rcmp.getPasses().get(4).getCustomByteValue()).isEqualTo(0xAA);
        assertThat(rcmp.getPasses().get(5).getCustomByteValue()).isEqualTo(0x55);
        assertThat(rcmp.getOrganization()).contains("Canada");
    }

    @Test
    @DisplayName("Custom policy with arbitrary hex patterns can be created and saved")
    void testCustomPolicyCreation() {
        WipePolicyManager manager = WipePolicyManager.getInstance();

        WipePolicy custom = new WipePolicy(
                "custom-corp-policy-4p",
                "Corporate Defense 4-Pass",
                "Internal enterprise sanitization policy",
                "CyberSec Division",
                "CORP_SEC_4P",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1: 0xAA"),
                        new WipePass(2, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 2: 0x55"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: CSPRNG Random"),
                        new WipePass(4, WipePatternType.ZERO_FILL, 0x00, "Pass 4: Verification 0x00")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0",
                "Admin"
        );

        boolean saved = manager.savePolicy(custom);
        assertThat(saved).isTrue();

        WipePolicy retrieved = manager.getPolicyById("custom-corp-policy-4p");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getPassCount()).isEqualTo(4);
        assertThat(retrieved.getPatternSummary()).contains("0xAA", "0x55", "CSPRNG", "0x00");

        // Cleanup
        manager.deletePolicy("custom-corp-policy-4p");
    }

    @Test
    @DisplayName("System built-in standards are protected from deletion")
    void testBuiltinProtection() {
        WipePolicyManager manager = WipePolicyManager.getInstance();
        boolean deleted = manager.deletePolicy("nist-800-88");
        assertThat(deleted).isFalse();
        assertThat(manager.getPolicyById("nist-800-88")).isNotNull();
    }

    @Test
    @DisplayName("JSON export and import produces 100% faithful policy profile")
    void testJsonRoundTripSerialization() throws IOException {
        WipePolicy policy = new WipePolicy(
                "export-test-policy",
                "AirGap TopSecret 5-Pass",
                "TopSecret classification wipe standard",
                "Defense Agency",
                "AIRGAP_TS_5P",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1"),
                        new WipePass(2, WipePatternType.CUSTOM_BYTE, 0x92, "Pass 2"),
                        new WipePass(3, WipePatternType.CUSTOM_BYTE, 0x49, "Pass 3"),
                        new WipePass(4, WipePatternType.ONE_FILL, 0xFF, "Pass 4"),
                        new WipePass(5, WipePatternType.PSEUDO_RANDOM, -1, "Pass 5")
                ),
                true,
                WipeVerifier.VerificationMode.EXTENSIVE_SAMPLE_10_PERCENT,
                false,
                "2.1.0",
                "SecOps"
        );

        String json = WipePolicyManager.exportPolicyToJson(policy);
        assertThat(json).contains("AirGap TopSecret 5-Pass", "AIRGAP_TS_5P");

        WipePolicy imported = WipePolicyManager.importPolicyFromJson(json);
        assertThat(imported).isNotNull();
        assertThat(imported.getName()).isEqualTo("AirGap TopSecret 5-Pass");
        assertThat(imported.getPassCount()).isEqualTo(5);
        assertThat(imported.getPasses().get(1).getCustomByteValue()).isEqualTo(0x92);
        assertThat(imported.getPasses().get(1).getPatternHex()).isEqualTo("0x92");
        assertThat(imported.getPasses().get(2).getCustomByteValue()).isEqualTo(0x49);
        assertThat(imported.getPasses().get(2).getPatternHex()).isEqualTo("0x49");

        // Test File IO
        File tempFile = File.createTempFile("test_policy_", ".json");
        tempFile.deleteOnExit();

        WipePolicyManager.exportPolicyToFile(policy, tempFile);
        assertThat(tempFile.exists()).isTrue();
        assertThat(tempFile.length()).isGreaterThan(50);

        WipePolicy importedFromFile = WipePolicyManager.importPolicyFromFile(tempFile);
        assertThat(importedFromFile).isNotNull();
        assertThat(importedFromFile.getName()).isEqualTo(policy.getName());
        assertThat(importedFromFile.getPassCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("Invalid JSON policy import gracefully throws descriptive exception")
    void testInvalidJsonImport() {
        assertThatThrownBy(() -> WipePolicyManager.importPolicyFromJson("{ \"invalid\": true }"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
