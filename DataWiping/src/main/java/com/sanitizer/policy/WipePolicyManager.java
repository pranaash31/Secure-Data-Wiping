package com.sanitizer.policy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sanitizer.engine.WipeVerifier;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry & Lifecycle Manager for Wipe Policies (International Standards & Custom User Profiles).
 * Supports JSON import/export, profile validation, and local disk persistence.
 */
public class WipePolicyManager {

    private static final String MODULE = "WipePolicyManager";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File STORAGE_DIR = new File(System.getProperty("user.home"), ".sanitizer/policies");

    private static final WipePolicyManager INSTANCE = new WipePolicyManager();

    private final Map<String, WipePolicy> policies = new LinkedHashMap<>();
    private String defaultPolicyId = "nist-800-88";

    public static WipePolicyManager getInstance() {
        return INSTANCE;
    }

    private WipePolicyManager() {
        initBuiltinStandards();
        loadCustomPoliciesFromDisk();
    }

    private void initBuiltinStandards() {
        // 1. NIST SP 800-88 Rev. 1 Clear (Single Pass 0x00)
        WipePolicy nist = new WipePolicy(
                "nist-800-88",
                "NIST SP 800-88 Rev. 1 Clear",
                "Single pass logical 0x00 overwrite. Standard compliance for sanitization of magnetic and flash media.",
                "National Institute of Standards and Technology (USA)",
                "NIST_800_88_CLEAR",
                List.of(new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Zero-fill logical block overwrite")),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                "Rev. 1",
                "NIST SP 800-88"
        );

        // 2. US DoD 5220.22-M (Standard 3-Pass)
        WipePolicy dod = new WipePolicy(
                "dod-5220-22-m",
                "US DoD 5220.22-M (3-Pass)",
                "National Industrial Security Program Operating Manual 3-pass overwrite standard (Zero, Complement, Random).",
                "Department of Defense (USA)",
                "DOD_5220_22_M",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1: Fixed Zero-Fill (0x00)"),
                        new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "Pass 2: Fixed One-Fill / Inversion (0xFF)"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: CSPRNG Pseudo-Random Sequence")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                "NISPOM",
                "US DoD"
        );

        // 3. US DoD 5220.22-M ECE (7-Pass Military Wipe)
        WipePolicy dodEce = new WipePolicy(
                "dod-5220-22-m-ece",
                "US DoD 5220.22-M ECE (7-Pass)",
                "Extended 7-pass military sanitization with alternating fixed bytes, inversions, and random noise.",
                "Department of Defense (USA)",
                "DOD_5220_22_M_ECE",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1: Zero Fill (0x00)"),
                        new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "Pass 2: One Fill (0xFF)"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: CSPRNG Random"),
                        new WipePass(4, WipePatternType.CUSTOM_BYTE, 0x96, "Pass 4: Fixed Byte Pattern (0x96)"),
                        new WipePass(5, WipePatternType.ZERO_FILL, 0x00, "Pass 5: Zero Fill (0x00)"),
                        new WipePass(6, WipePatternType.ONE_FILL, 0xFF, "Pass 6: One Fill (0xFF)"),
                        new WipePass(7, WipePatternType.PSEUDO_RANDOM, -1, "Pass 7: CSPRNG Random")
                ),
                true,
                WipeVerifier.VerificationMode.EXTENSIVE_SAMPLE_10_PERCENT,
                true,
                "ECE-7",
                "US DoD"
        );

        // 4. British HMG Infosec Standard 5 (Enhanced)
        WipePolicy hmg = new WipePolicy(
                "hmg-infosec-5-enhanced",
                "British HMG Infosec Standard 5 (Enhanced)",
                "UK Government Communications-Electronics Security Group standard for baseline and enhanced media clearing.",
                "National Cyber Security Centre (UK)",
                "HMG_IS5_ENHANCED",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1: Overwrite with all zeros (0x00)"),
                        new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "Pass 2: Overwrite with all ones (0xFF)"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: Overwrite with cryptographic pseudo-random characters")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                true,
                "IS5 v4.3",
                "UK NCSC / CESG"
        );

        // 5. German BSI-VS / TL-03423 (7-Pass)
        WipePolicy bsi = new WipePolicy(
                "bsi-vs-7pass",
                "German BSI-VS / BSI TL-03423 (7-Pass)",
                "Federal Office for Information Security (Bundesamt für Sicherheit in der Informationstechnik) 7-pass standard.",
                "Federal Office for Information Security (BSI Germany)",
                "BSI_VS_7PASS",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1: Fixed Zero Fill (0x00)"),
                        new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "Pass 2: Fixed One Fill (0xFF)"),
                        new WipePass(3, WipePatternType.ZERO_FILL, 0x00, "Pass 3: Fixed Zero Fill (0x00)"),
                        new WipePass(4, WipePatternType.ONE_FILL, 0xFF, "Pass 4: Fixed One Fill (0xFF)"),
                        new WipePass(5, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 5: Alternating Pattern 1 (0xAA / 10101010)"),
                        new WipePass(6, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 6: Alternating Pattern 2 (0x55 / 01010101)"),
                        new WipePass(7, WipePatternType.PSEUDO_RANDOM, -1, "Pass 7: CSPRNG Random Verification Sequence")
                ),
                true,
                WipeVerifier.VerificationMode.EXTENSIVE_SAMPLE_10_PERCENT,
                true,
                "BSI-TL-03423",
                "BSI Germany"
        );

        // 6. Canadian RCMP TSSIT OPS-II (7-Pass)
        WipePolicy rcmp = new WipePolicy(
                "rcmp-tssit-ops-ii",
                "Canadian RCMP TSSIT OPS-II (7-Pass)",
                "Royal Canadian Mounted Police Technical Security Standard for Information Technology clearing OPS-II.",
                "Royal Canadian Mounted Police (Canada)",
                "RCMP_TSSIT_OPS_II",
                List.of(
                        new WipePass(1, WipePatternType.ZERO_FILL, 0x00, "Pass 1: Write all zeros (0x00)"),
                        new WipePass(2, WipePatternType.ONE_FILL, 0xFF, "Pass 2: Write all ones (0xFF)"),
                        new WipePass(3, WipePatternType.ZERO_FILL, 0x00, "Pass 3: Write all zeros (0x00)"),
                        new WipePass(4, WipePatternType.ONE_FILL, 0xFF, "Pass 4: Write all ones (0xFF)"),
                        new WipePass(5, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 5: Bit alternation pattern 0xAA"),
                        new WipePass(6, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 6: Bit alternation complement 0x55"),
                        new WipePass(7, WipePatternType.PSEUDO_RANDOM, -1, "Pass 7: Pseudo-random stream")
                ),
                true,
                WipeVerifier.VerificationMode.EXTENSIVE_SAMPLE_10_PERCENT,
                true,
                "OPS-II",
                "RCMP Canada"
        );

        registerBuiltin(nist);
        registerBuiltin(dod);
        registerBuiltin(dodEce);
        registerBuiltin(hmg);
        registerBuiltin(bsi);
        registerBuiltin(rcmp);
    }

    private synchronized void registerBuiltin(WipePolicy p) {
        policies.put(p.getId(), p);
    }

    public synchronized List<WipePolicy> getAllPolicies() {
        return new ArrayList<>(policies.values());
    }

    public synchronized WipePolicy getPolicyById(String id) {
        if (id == null) return getDefaultPolicy();
        WipePolicy policy = policies.get(id);
        return policy != null ? policy : getDefaultPolicy();
    }

    public synchronized WipePolicy getDefaultPolicy() {
        WipePolicy def = policies.get(defaultPolicyId);
        return def != null ? def : policies.values().iterator().next();
    }

    public synchronized void setDefaultPolicy(WipePolicy policy) {
        if (policy != null && policies.containsKey(policy.getId())) {
            this.defaultPolicyId = policy.getId();
        }
    }

    public synchronized boolean savePolicy(WipePolicy policy) {
        if (policy == null || policy.getName() == null || policy.getName().isBlank()) {
            return false;
        }
        if (policy.getPasses() == null || policy.getPasses().isEmpty()) {
            return false;
        }

        // Re-number passes consecutively
        for (int i = 0; i < policy.getPasses().size(); i++) {
            policy.getPasses().get(i).setPassNumber(i + 1);
        }

        policies.put(policy.getId(), policy);

        if (!policy.isSystemBuiltin()) {
            persistPolicyToDisk(policy);
        }
        return true;
    }

    public synchronized boolean deletePolicy(String id) {
        WipePolicy p = policies.get(id);
        if (p == null || p.isSystemBuiltin()) {
            return false; // Cannot delete built-in international standards
        }
        policies.remove(id);
        deletePolicyFromDisk(id);
        if (Objects.equals(defaultPolicyId, id)) {
            defaultPolicyId = "nist-800-88";
        }
        return true;
    }

    // ── JSON Import & Export ──────────────────────────────────────────

    public static String exportPolicyToJson(WipePolicy policy) {
        return GSON.toJson(policy);
    }

    public static void exportPolicyToFile(WipePolicy policy, File targetFile) throws IOException {
        String json = exportPolicyToJson(policy);
        Files.writeString(targetFile.toPath(), json, StandardCharsets.UTF_8);
    }

    public static WipePolicy importPolicyFromJson(String json) throws IllegalArgumentException {
        try {
            WipePolicy policy = GSON.fromJson(json, WipePolicy.class);
            if (policy == null || policy.getName() == null || policy.getPasses() == null || policy.getPasses().isEmpty()) {
                throw new IllegalArgumentException("Invalid policy format: Missing policy name or passes list.");
            }
            policy.setSystemBuiltin(false);
            if (policy.getId() == null || policy.getId().isBlank()) {
                policy.setId(UUID.randomUUID().toString());
            }
            return policy;
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON parse error: " + e.getMessage(), e);
        }
    }

    public static WipePolicy importPolicyFromFile(File sourceFile) throws IOException, IllegalArgumentException {
        String json = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
        return importPolicyFromJson(json);
    }

    // ── Disk Persistence Helpers ─────────────────────────────────────

    private void persistPolicyToDisk(WipePolicy policy) {
        try {
            if (!STORAGE_DIR.exists()) {
                STORAGE_DIR.mkdirs();
            }
            File file = new File(STORAGE_DIR, policy.getId() + ".json");
            exportPolicyToFile(policy, file);
            AppLogger.info(MODULE, "Saved custom wipe policy to: " + file.getAbsolutePath());
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Failed to persist wipe policy to disk: " + e.getMessage());
        }
    }

    private void deletePolicyFromDisk(String id) {
        try {
            File file = new File(STORAGE_DIR, id + ".json");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Failed to delete policy file: " + e.getMessage());
        }
    }

    private void loadCustomPoliciesFromDisk() {
        if (!STORAGE_DIR.exists() || !STORAGE_DIR.isDirectory()) return;

        File[] files = STORAGE_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File f : files) {
            try {
                WipePolicy policy = importPolicyFromFile(f);
                policy.setSystemBuiltin(false);
                policies.put(policy.getId(), policy);
                AppLogger.info(MODULE, "Loaded custom wipe policy: " + policy.getName());
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Failed to load policy file " + f.getName() + ": " + e.getMessage());
            }
        }
    }
}
