package com.sanitizer.session;

/**
 * SessionContext — immutable value object that captures the authenticated
 * officer's identity for the duration of one application session.
 * <p>
 * Created by {@link com.sanitizer.gui.navigation.NavigationManager#loginSuccess}
 * and passed through the UI layer; no credentials are ever hard-coded in
 * routing or layout code.
 *
 * @param officerName Full display name of the authenticated officer.
 * @param agencyId    Government agency / department identifier.
 * @param role        Role / clearance level of the officer.
 */
public record SessionContext(
        String officerName,
        String agencyId,
        String role
) {
    /** Compact constructor: sanitises blank/null inputs to safe defaults. */
    public SessionContext {
        officerName = (officerName != null && !officerName.isBlank())
                ? officerName.trim() : "Unknown Officer";
        agencyId    = (agencyId    != null && !agencyId.isBlank())
                ? agencyId.trim()    : "UNSET-AGENCY";
        role        = (role        != null && !role.isBlank())
                ? role.trim()        : "Inspector";
    }

    /** Returns a short display string suitable for UI labels. */
    public String displayLabel() {
        return "Officer: " + officerName + "   |   Agency: " + agencyId;
    }
}
