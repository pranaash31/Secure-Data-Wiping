package com.sanitizer.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category 10: Multi-Tier Authorization (RBAC) & Role Tests")
public class UserRoleTest {

    @Test
    @DisplayName("Verify Inspector Role has operational permissions but lacks configuration/keyvault clearance")
    public void testInspectorPermissions() {
        UserRole inspector = UserRole.INSPECTOR;

        // Allowed
        assertTrue(inspector.hasPermission(UserRole.Permission.WIPE_EXECUTE));
        assertTrue(inspector.hasPermission(UserRole.Permission.BATCH_WIPE));
        assertTrue(inspector.hasPermission(UserRole.Permission.DIAGNOSTICS_RUN));
        assertTrue(inspector.hasPermission(UserRole.Permission.DASHBOARD_VIEW));
        assertTrue(inspector.hasPermission(UserRole.Permission.CLIENTS_VIEW));

        // Forbidden
        assertFalse(inspector.hasPermission(UserRole.Permission.KEYVAULT_MANAGE));
        assertFalse(inspector.hasPermission(UserRole.Permission.POLICY_EDIT));
        assertFalse(inspector.hasPermission(UserRole.Permission.THERMAL_CONFIG));
        assertFalse(inspector.hasPermission(UserRole.Permission.ALERT_CONFIG));
        assertFalse(inspector.hasPermission(UserRole.Permission.SETTINGS_MODIFY));
        assertFalse(inspector.hasPermission(UserRole.Permission.AUDIT_CLEAR));
    }

    @Test
    @DisplayName("Verify Supervisor Role has policy/thermal configuration clearance but lacks master keyvault management")
    public void testSupervisorPermissions() {
        UserRole supervisor = UserRole.SUPERVISOR;

        // Allowed
        assertTrue(supervisor.hasPermission(UserRole.Permission.WIPE_EXECUTE));
        assertTrue(supervisor.hasPermission(UserRole.Permission.BATCH_WIPE));
        assertTrue(supervisor.hasPermission(UserRole.Permission.POLICY_EDIT));
        assertTrue(supervisor.hasPermission(UserRole.Permission.THERMAL_CONFIG));
        assertTrue(supervisor.hasPermission(UserRole.Permission.ALERT_CONFIG));
        assertTrue(supervisor.hasPermission(UserRole.Permission.SETTINGS_MODIFY));
        assertTrue(supervisor.hasPermission(UserRole.Permission.CLIENTS_MANAGE));
        assertTrue(supervisor.hasPermission(UserRole.Permission.AUDIT_EXPORT));

        // Forbidden
        assertFalse(supervisor.hasPermission(UserRole.Permission.KEYVAULT_MANAGE));
    }

    @Test
    @DisplayName("Verify Chief Auditor Role has full uninhibited governance clearance")
    public void testChiefAuditorPermissions() {
        UserRole chief = UserRole.CHIEF_AUDITOR;

        for (UserRole.Permission p : UserRole.Permission.values()) {
            assertTrue(chief.hasPermission(p), "Chief Auditor must have permission: " + p);
        }
        assertTrue(chief.hasPermission(UserRole.Permission.KEYVAULT_MANAGE));
        assertTrue(chief.hasPermission(UserRole.Permission.LEDGER_VERIFY));
        assertTrue(chief.hasPermission(UserRole.Permission.SECURITY_AUDIT_VIEW));
    }

    @Test
    @DisplayName("Verify Role Hierarchy Tier Comparisons (isAtLeast)")
    public void testRoleHierarchy() {
        assertTrue(UserRole.CHIEF_AUDITOR.isAtLeast(UserRole.SUPERVISOR));
        assertTrue(UserRole.CHIEF_AUDITOR.isAtLeast(UserRole.INSPECTOR));
        assertTrue(UserRole.SUPERVISOR.isAtLeast(UserRole.INSPECTOR));
        assertFalse(UserRole.INSPECTOR.isAtLeast(UserRole.SUPERVISOR));
        assertFalse(UserRole.INSPECTOR.isAtLeast(UserRole.CHIEF_AUDITOR));
    }

    @Test
    @DisplayName("Verify fromString parsing and aliases")
    public void testFromStringParsing() {
        assertEquals(UserRole.INSPECTOR, UserRole.fromString("INSPECTOR"));
        assertEquals(UserRole.INSPECTOR, UserRole.fromString("Sanitization Inspector"));
        assertEquals(UserRole.SUPERVISOR, UserRole.fromString("SUPERVISOR"));
        assertEquals(UserRole.SUPERVISOR, UserRole.fromString("Operations Supervisor"));
        assertEquals(UserRole.CHIEF_AUDITOR, UserRole.fromString("CHIEF_AUDITOR"));
        assertEquals(UserRole.CHIEF_AUDITOR, UserRole.fromString("Chief Compliance Auditor"));
        assertEquals(UserRole.INSPECTOR, UserRole.fromString(null));
        assertEquals(UserRole.INSPECTOR, UserRole.fromString(""));
    }

    @Test
    @DisplayName("Verify SessionContext integration with UserRole and permission evaluator")
    public void testSessionContextIntegration() {
        SessionContext ctx = new SessionContext("Officer Test", "AGENCY-01", UserRole.SUPERVISOR);

        assertEquals("Officer Test", ctx.officerName());
        assertEquals("AGENCY-01", ctx.agencyId());
        assertEquals(UserRole.SUPERVISOR, ctx.userRole());
        assertEquals("Operations Supervisor", ctx.role());
        assertTrue(ctx.hasPermission(UserRole.Permission.POLICY_EDIT));
        assertFalse(ctx.hasPermission(UserRole.Permission.KEYVAULT_MANAGE));
        assertTrue(ctx.isRoleAtLeast(UserRole.INSPECTOR));
        assertNotNull(ctx.sessionId());
        assertTrue(ctx.loginEpochMs() > 0);
    }
}
