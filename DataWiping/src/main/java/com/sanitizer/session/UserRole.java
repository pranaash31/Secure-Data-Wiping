package com.sanitizer.session;

import java.util.EnumSet;
import java.util.Set;

/**
 * UserRole — Defines the 3-tier clearance hierarchy and associated permissions
 * for the SecureErase Pro Enterprise suite.
 * <ul>
 *   <li><b>INSPECTOR</b> (Tier 1): Operator role for disk sanitization, diagnostics, and viewing own tasks.</li>
 *   <li><b>SUPERVISOR</b> (Tier 2): Managerial role for policy configuration, thermal thresholds, and alerts.</li>
 *   <li><b>CHIEF_AUDITOR</b> (Tier 3): Executive compliance officer with full cryptographic verification and governance.</li>
 * </ul>
 */
public enum UserRole {

    INSPECTOR(
            "Sanitization Inspector",
            "Tier 1 — Operator",
            "badge-info",
            "#0284C7",
            "#E0F2FE",
            EnumSet.of(
                    Permission.WIPE_EXECUTE,
                    Permission.BATCH_WIPE,
                    Permission.DIAGNOSTICS_RUN,
                    Permission.DASHBOARD_VIEW,
                    Permission.CLIENTS_VIEW,
                    Permission.CERTIFICATE_GENERATE
            )
    ),

    SUPERVISOR(
            "Operations Supervisor",
            "Tier 2 — Supervisor",
            "badge-warning",
            "#D97706",
            "#FEF3C7",
            EnumSet.of(
                    Permission.WIPE_EXECUTE,
                    Permission.BATCH_WIPE,
                    Permission.DIAGNOSTICS_RUN,
                    Permission.DASHBOARD_VIEW,
                    Permission.CLIENTS_VIEW,
                    Permission.CLIENTS_MANAGE,
                    Permission.CERTIFICATE_GENERATE,
                    Permission.POLICY_EDIT,
                    Permission.THERMAL_CONFIG,
                    Permission.ALERT_CONFIG,
                    Permission.AUDIT_VIEW,
                    Permission.AUDIT_EXPORT,
                    Permission.SETTINGS_MODIFY
            )
    ),

    CHIEF_AUDITOR(
            "Chief Compliance Auditor",
            "Tier 3 — Chief Auditor",
            "badge-danger",
            "#9333EA",
            "#F3E8FF",
            EnumSet.allOf(Permission.class)
    );

    private final String title;
    private final String tierLabel;
    private final String styleClass;
    private final String accentColor;
    private final String bgColor;
    private final Set<Permission> permissions;

    UserRole(String title, String tierLabel, String styleClass, String accentColor, String bgColor, Set<Permission> permissions) {
        this.title = title;
        this.tierLabel = tierLabel;
        this.styleClass = styleClass;
        this.accentColor = accentColor;
        this.bgColor = bgColor;
        this.permissions = permissions;
    }

    public String getTitle() {
        return title;
    }

    public String getTierLabel() {
        return tierLabel;
    }

    public String getStyleClass() {
        return styleClass;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getBgColor() {
        return bgColor;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean isAtLeast(UserRole other) {
        return this.ordinal() >= other.ordinal();
    }

    /**
     * Resolves a role from string safely (e.g. "INSPECTOR", "Inspector", "SUPERVISOR", etc.).
     */
    public static UserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return INSPECTOR;
        }
        String normalized = roleStr.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        for (UserRole r : values()) {
            if (r.name().equals(normalized) || r.title.equalsIgnoreCase(roleStr.trim())) {
                return r;
            }
        }
        if (normalized.contains("CHIEF") || normalized.contains("AUDITOR")) return CHIEF_AUDITOR;
        if (normalized.contains("SUPERVISOR") || normalized.contains("MANAGER")) return SUPERVISOR;
        return INSPECTOR;
    }

    /**
     * Discrete permission tokens for granular authorization across the platform.
     */
    public enum Permission {
        // Operational
        WIPE_EXECUTE,
        BATCH_WIPE,
        DIAGNOSTICS_RUN,
        DASHBOARD_VIEW,
        CLIENTS_VIEW,
        CLIENTS_MANAGE,
        CERTIFICATE_GENERATE,

        // Configuration & Governance
        POLICY_EDIT,
        THERMAL_CONFIG,
        ALERT_CONFIG,
        SETTINGS_MODIFY,

        // Security & Cryptography
        KEYVAULT_MANAGE,
        LEDGER_VERIFY,

        // Audit Trail & Compliance
        AUDIT_VIEW,
        AUDIT_EXPORT,
        AUDIT_CLEAR,
        SECURITY_AUDIT_VIEW,
        SECURITY_AUDIT_VERIFY,
        SECURITY_AUDIT_EXPORT,
        ESG_REPORT_GENERATE
    }
}
