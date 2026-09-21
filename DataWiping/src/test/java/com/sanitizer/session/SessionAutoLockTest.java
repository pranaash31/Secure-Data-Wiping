package com.sanitizer.session;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Category 10: Session Auto-Lock (FISMA / HIPAA) Tests")
public class SessionAutoLockTest {

    @Test
    @DisplayName("Verify timeout configuration and getters")
    public void testTimeoutConfiguration() {
        SessionAutoLockManager manager = SessionAutoLockManager.getInstance();

        manager.setTimeoutSeconds(60);
        assertEquals(60, manager.getTimeoutSeconds());

        manager.setTimeoutSeconds(900);
        assertEquals(900, manager.getTimeoutSeconds());

        manager.setTimeoutSeconds(0);
        assertEquals(0, manager.getTimeoutSeconds());

        // Restore default
        manager.setTimeoutSeconds(300);
        assertEquals(300, manager.getTimeoutSeconds());
    }

    @Test
    @DisplayName("Verify session lock and unlock lifecycle with PIN validation")
    public void testSessionLockAndUnlock() {
        SessionAutoLockManager manager = SessionAutoLockManager.getInstance();
        manager.setOfficerPin("4321");

        AtomicBoolean lockedNotified = new AtomicBoolean(false);
        AtomicBoolean unlockedNotified = new AtomicBoolean(false);

        SessionAutoLockManager.LockListener listener = new SessionAutoLockManager.LockListener() {
            @Override
            public void onSessionLocked() {
                lockedNotified.set(true);
            }

            @Override
            public void onSessionUnlocked() {
                unlockedNotified.set(true);
            }
        };

        manager.addListener(listener);

        try {
            // 1. Lock session
            manager.lockSession("Test Inactivity", "Officer Test", "DEF-01", "INSPECTOR");
            assertTrue(manager.isLocked());
            assertTrue(lockedNotified.get());

            // 2. Attempt unlock with incorrect PIN
            boolean failUnlock = manager.unlockSession("0000", "Officer Test", "DEF-01", "INSPECTOR");
            assertFalse(failUnlock);
            assertTrue(manager.isLocked());

            // 3. Unlock with correct custom PIN
            boolean successUnlock = manager.unlockSession("4321", "Officer Test", "DEF-01", "INSPECTOR");
            assertTrue(successUnlock);
            assertFalse(manager.isLocked());
            assertTrue(unlockedNotified.get());
        } finally {
            manager.removeListener(listener);
            manager.setOfficerPin("1234");
        }
    }

    @Test
    @DisplayName("Verify remaining seconds calculation")
    public void testRemainingSeconds() {
        SessionAutoLockManager manager = SessionAutoLockManager.getInstance();
        manager.setTimeoutSeconds(300);
        manager.recordUserActivity();

        int remaining = manager.getRemainingSecondsBeforeLock();
        assertTrue(remaining > 0 && remaining <= 300);

        manager.setTimeoutSeconds(0); // Disabled
        assertEquals(0, manager.getRemainingSecondsBeforeLock());

        manager.setTimeoutSeconds(300);
    }
}
