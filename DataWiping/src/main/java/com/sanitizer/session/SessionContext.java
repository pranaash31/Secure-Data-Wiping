package com.sanitizer.session;

import java.util.UUID;

/**
 * SessionContext — immutable value object capturing authenticated officer's
 * identity, assigned clearance tier (UserRole), session lifecycle timestamps,
 * and permission evaluator.
 *
 * @param officerName Full display name of the authenticated officer.
 * @param agencyId    Government agency / department identifier.
 * @param userRole    Assigned role tier (INSPECTOR, SUPERVISOR, CHIEF_AUDITOR).
 * @param sessionId   Unique session token identifier.
 * @param loginEpochMs Timestamp when session was authenticated.
 */
public record SessionContext(
        String officerName,
        String agencyId,
        UserRole userRole,
        String sessionId,
        long loginEpochMs
) {
    /** Compact constructor: sanitises blank/null inputs to safe defaults. */
    public SessionContext {
        officerName = (officerName != null && !officerName.isBlank())
                ? officerName.trim() : "Unknown Officer";
        agencyId    = (agencyId    != null && !agencyId.isBlank())
                ? agencyId.trim()    : "GOV-DEF-8942";
        userRole    = (userRole    != null)
                ? userRole           : UserRole.INSPECTOR;
        sessionId   = (sessionId   != null && !sessionId.isBlank())
                ? sessionId          : UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        if (loginEpochMs <= 0) {
            loginEpochMs = System.currentTimeMillis();
        }
    }

    /** Backward-compat 3-String constructor (e.g. from existing code). */
    public SessionContext(String officerName, String agencyId, String roleStr) {
        this(officerName, agencyId, UserRole.fromString(roleStr), null, System.currentTimeMillis());
    }

    /** 3-arg constructor with UserRole. */
    public SessionContext(String officerName, String agencyId, UserRole role) {
        this(officerName, agencyId, role, null, System.currentTimeMillis());
    }

    /** Returns role title string for backward compatibility. */
    public String role() {
        return userRole.getTitle();
    }

    /** Checks whether the session officer has a specific permission. */
    public boolean hasPermission(UserRole.Permission permission) {
        return userRole.hasPermission(permission);
    }

    /** Checks if the session officer's clearance meets or exceeds a target role tier. */
    public boolean isRoleAtLeast(UserRole targetRole) {
        return userRole.isAtLeast(targetRole);
    }

    /** Returns a short display string suitable for UI labels. */
    public String displayLabel() {
        return "Officer: " + officerName + "   |   Agency: " + agencyId + "   |   Role: " + userRole.getTitle();
    }
}
