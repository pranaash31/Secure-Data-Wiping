package com.sanitizer.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * AppLogger — lightweight, zero-dependency structured logger for SecureErase Pro.
 * <p>
 * Usage:
 * <pre>
 *   AppLogger.info("AuditDb",  "Record saved, id=42");
 *   AppLogger.warn("WipeEngine", "Test-mode active — 1 GB cap applied");
 *   AppLogger.error("CryptoSigner", "Key init failed", e);
 * </pre>
 * All output is directed to {@code System.out} / {@code System.err} with a
 * {@code HH:mm:ss [MODULE] LEVEL  message} prefix so output is easily grepped.
 */
public final class AppLogger {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private AppLogger() { /* utility class — no instances */ }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Log an informational message. */
    public static void info(String module, String message) {
        System.out.printf("%s [%-16s] INFO   %s%n", now(), module, message);
    }

    /** Log a warning. */
    public static void warn(String module, String message) {
        System.out.printf("%s [%-16s] WARN   %s%n", now(), module, message);
    }

    /** Log an error with no exception. */
    public static void error(String module, String message) {
        System.err.printf("%s [%-16s] ERROR  %s%n", now(), module, message);
    }

    /** Log an error with the root-cause exception message appended. */
    public static void error(String module, String message, Throwable cause) {
        System.err.printf("%s [%-16s] ERROR  %s — %s%n",
                now(), module, message,
                cause != null ? cause.getMessage() : "null");
    }

    /** Log a security/safety-shield event (always printed regardless of level). */
    public static void shield(String module, String message) {
        System.out.printf("%s [%-16s] SHIELD %s%n", now(), module, message);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static String now() {
        return LocalTime.now().format(TIME_FMT);
    }
}
