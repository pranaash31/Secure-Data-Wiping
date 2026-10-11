package com.sanitizer.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionContext Unit Tests")
class SessionContextTest {

    @Test
    @DisplayName("Verify SessionContext handles nulls with safe defaults")
    void testSessionContextDefaults() {
        SessionContext ctx = new SessionContext(null, null, (UserRole) null);

        assertThat(ctx.officerName()).isEqualTo("Unknown Officer");
        assertThat(ctx.agencyId()).isEqualTo("GOV-DEF-8942");
        assertThat(ctx.userRole()).isEqualTo(UserRole.INSPECTOR);
        assertThat(ctx.sessionId()).isNotBlank();
        assertThat(ctx.role()).isEqualTo("Sanitization Inspector");
        assertThat(ctx.userRole().getTierLabel()).isEqualTo("Tier 1 — Operator");
    }

    @Test
    @DisplayName("Verify SessionContext permission delegation and role comparison")
    void testSessionContextPermissions() {
        SessionContext inspector = new SessionContext("Agent J", "NSA", UserRole.INSPECTOR);
        assertThat(inspector.hasPermission(UserRole.Permission.WIPE_EXECUTE)).isTrue();
        assertThat(inspector.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)).isFalse();
        assertThat(inspector.isRoleAtLeast(UserRole.INSPECTOR)).isTrue();
        assertThat(inspector.isRoleAtLeast(UserRole.SUPERVISOR)).isFalse();

        SessionContext chief = new SessionContext("Director Vance", "DoD", UserRole.CHIEF_AUDITOR);
        assertThat(chief.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)).isTrue();
        assertThat(chief.hasPermission(UserRole.Permission.SETTINGS_MODIFY)).isTrue();
        assertThat(chief.isRoleAtLeast(UserRole.INSPECTOR)).isTrue();
        assertThat(chief.isRoleAtLeast(UserRole.SUPERVISOR)).isTrue();
        assertThat(chief.isRoleAtLeast(UserRole.CHIEF_AUDITOR)).isTrue();
        assertThat(chief.displayLabel()).contains("Director Vance").contains("DoD");
    }
}
