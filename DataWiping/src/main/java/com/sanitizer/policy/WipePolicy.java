package com.sanitizer.policy;

import com.sanitizer.engine.WipeVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a sanitization policy profile (standard preset or user-defined).
 */
public class WipePolicy {

    private String id;
    private String name;
    private String description;
    private String organization;
    private String standardCode;
    private List<WipePass> passes;
    private boolean verifyAfterWipe;
    private WipeVerifier.VerificationMode verificationMode;
    private boolean isSystemBuiltin;
    private String version;
    private String author;
    private long createdAt;

    public WipePolicy() {
        this.id = UUID.randomUUID().toString();
        this.passes = new ArrayList<>();
        this.verifyAfterWipe = true;
        this.verificationMode = WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT;
        this.isSystemBuiltin = false;
        this.version = "1.0.0";
        this.createdAt = System.currentTimeMillis();
    }

    public WipePolicy(String id, String name, String description, String organization,
                      String standardCode, List<WipePass> passes, boolean verifyAfterWipe,
                      WipeVerifier.VerificationMode verificationMode, boolean isSystemBuiltin,
                      String version, String author) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Unnamed Policy";
        this.description = description != null ? description : "";
        this.organization = organization != null ? organization : "Custom";
        this.standardCode = standardCode != null ? standardCode : "CUSTOM";
        this.passes = passes != null ? new ArrayList<>(passes) : new ArrayList<>();
        this.verifyAfterWipe = verifyAfterWipe;
        this.verificationMode = verificationMode != null ? verificationMode : WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT;
        this.isSystemBuiltin = isSystemBuiltin;
        this.version = version != null ? version : "1.0.0";
        this.author = author != null ? author : "System";
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getStandardCode() { return standardCode; }
    public void setStandardCode(String standardCode) { this.standardCode = standardCode; }

    public List<WipePass> getPasses() { return passes; }
    public void setPasses(List<WipePass> passes) { this.passes = passes != null ? passes : new ArrayList<>(); }

    public int getPassCount() { return passes != null ? passes.size() : 0; }

    public boolean isVerifyAfterWipe() { return verifyAfterWipe; }
    public void setVerifyAfterWipe(boolean verifyAfterWipe) { this.verifyAfterWipe = verifyAfterWipe; }

    public WipeVerifier.VerificationMode getVerificationMode() { return verificationMode; }
    public void setVerificationMode(WipeVerifier.VerificationMode verificationMode) { this.verificationMode = verificationMode; }

    public boolean isSystemBuiltin() { return isSystemBuiltin; }
    public void setSystemBuiltin(boolean systemBuiltin) { isSystemBuiltin = systemBuiltin; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getShortBadge() {
        return String.format("%s (%d-Pass)", name, getPassCount());
    }

    public String getPatternSummary() {
        if (passes == null || passes.isEmpty()) return "No passes configured";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < passes.size(); i++) {
            if (i > 0) sb.append(" ➔ ");
            sb.append(passes.get(i).getPatternHex());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WipePolicy that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getShortBadge();
    }
}
