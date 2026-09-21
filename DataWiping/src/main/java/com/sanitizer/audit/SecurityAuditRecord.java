package com.sanitizer.audit;

/**
 * SecurityAuditRecord — immutable model representing a cryptographically chained,
 * tamper-evident administrative & security activity event.
 * <p>
 * Meets FISMA / HIPAA / NIST SP 800-53 security audit log requirements.
 */
public record SecurityAuditRecord(
        int id,
        String timestamp,
        String eventType,
        String actorName,
        String actorAgency,
        String actorRole,
        String actionSummary,
        String targetResource,
        String status,
        String prevEventHash,
        String eventHash
) {
    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    /** Compact constructor for default fallbacks. */
    public SecurityAuditRecord {
        eventType = (eventType != null && !eventType.isBlank()) ? eventType.trim() : "GENERAL_AUDIT";
        actorName = (actorName != null && !actorName.isBlank()) ? actorName.trim() : "SYSTEM";
        actorAgency = (actorAgency != null && !actorAgency.isBlank()) ? actorAgency.trim() : "GOV-DEF-8942";
        actorRole = (actorRole != null && !actorRole.isBlank()) ? actorRole.trim() : "SYSTEM";
        actionSummary = (actionSummary != null) ? actionSummary.trim() : "";
        targetResource = (targetResource != null) ? targetResource.trim() : "N/A";
        status = (status != null && !status.isBlank()) ? status.trim() : "SUCCESS";
        prevEventHash = (prevEventHash != null && !prevEventHash.isBlank()) ? prevEventHash.trim() : GENESIS_PREV_HASH;
        eventHash = (eventHash != null) ? eventHash.trim() : "";
    }
}
