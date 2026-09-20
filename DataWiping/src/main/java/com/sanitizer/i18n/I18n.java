package com.sanitizer.i18n;

import com.sanitizer.util.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Internationalization (i18n) Manager for SecureErase Pro.
 * Provides thread-safe multi-language resource bundle management,
 * UTF-8 properties decoding, reactive locale change listeners,
 * parameter formatting, and fallback mechanisms.
 *
 * Supported Locales:
 * - English (en) [Default]
 * - French (fr)
 * - Spanish (es)
 * - German (de)
 * - Hindi (hi)
 */
public final class I18n {

    private static final String MODULE = "I18n";
    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    public static final Locale LOCALE_EN = Locale.ENGLISH;
    public static final Locale LOCALE_FR = Locale.FRENCH;
    public static final Locale LOCALE_ES = new Locale("es");
    public static final Locale LOCALE_DE = Locale.GERMAN;
    public static final Locale LOCALE_HI = new Locale("hi");

    private static final List<Locale> SUPPORTED_LOCALES = List.of(
            LOCALE_EN,
            LOCALE_FR,
            LOCALE_ES,
            LOCALE_DE,
            LOCALE_HI
    );

    private static Locale currentLocale = LOCALE_EN;
    private static ResourceBundle currentBundle;
    private static final List<Consumer<Locale>> listeners = new CopyOnWriteArrayList<>();

    static {
        loadBundle(currentLocale);
    }

    private I18n() {}

    /**
     * Custom UTF-8 ResourceBundle Control to ensure character integrity across all Java runtimes.
     */
    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
        @Override
        public List<String> getFormats(String baseName) {
            return FORMAT_PROPERTIES;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream == null) {
                // Try with leading slash or context class loader
                stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    };

    /**
     * Load or reload the ResourceBundle for the given Locale.
     */
    private static synchronized void loadBundle(Locale locale) {
        try {
            currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale, UTF8_CONTROL);
            currentLocale = locale;
            AppLogger.info(MODULE, "Loaded language bundle for locale: " + locale.getLanguage());
        } catch (Exception ex) {
            try {
                currentBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN);
                currentLocale = LOCALE_EN;
                AppLogger.info(MODULE, "Loaded default English bundle for locale: " + locale.getLanguage());
            } catch (Exception e) {
                AppLogger.error(MODULE, "Critical error loading default fallback bundle: " + e.getMessage());
            }
        }
    }

    /**
     * Set active locale and notify all registered listeners.
     */
    public static synchronized void setLocale(Locale newLocale) {
        if (newLocale == null) return;
        Locale target = newLocale;
        if (!SUPPORTED_LOCALES.contains(target)) {
            // Find best matching supported locale by language code
            final String targetLang = target.getLanguage();
            Optional<Locale> match = SUPPORTED_LOCALES.stream()
                    .filter(l -> l.getLanguage().equalsIgnoreCase(targetLang))
                    .findFirst();
            target = match.orElse(LOCALE_EN);
        }

        if (!target.equals(currentLocale)) {
            loadBundle(target);
            notifyListeners(currentLocale);
        }
    }

    /**
     * Retrieve current active locale.
     */
    public static synchronized Locale getLocale() {
        return currentLocale;
    }

    /**
     * Get list of all supported locales.
     */
    public static List<Locale> getSupportedLocales() {
        return Collections.unmodifiableList(SUPPORTED_LOCALES);
    }

    /**
     * Retrieve localized text string by key with optional message parameters.
     */
    public static String get(String key, Object... args) {
        if (key == null) return "";
        try {
            if (currentBundle != null && currentBundle.containsKey(key)) {
                String value = currentBundle.getString(key);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(value, args);
                }
                return value;
            }
        } catch (MissingResourceException ignored) {}

        // Fallback to English bundle if missing in current locale
        if (!LOCALE_EN.equals(currentLocale)) {
            try {
                ResourceBundle enBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, LOCALE_EN, UTF8_CONTROL);
                if (enBundle.containsKey(key)) {
                    String value = enBundle.getString(key);
                    if (args != null && args.length > 0) {
                        return MessageFormat.format(value, args);
                    }
                    return value;
                }
            } catch (Exception ignored) {}
        }

        return key;
    }

    /**
     * Convenience alias for get(key).
     */
    public static String getString(String key) {
        return get(key);
    }

    /**
     * Get human-friendly display name with native representation and ISO tag.
     */
    public static String getLanguageDisplayName(Locale locale) {
        if (locale == null) return "Unknown";
        String lang = locale.getLanguage().toLowerCase();
        return switch (lang) {
            case "en" -> "English (EN)";
            case "fr" -> "Français (FR)";
            case "es" -> "Español (ES)";
            case "de" -> "Deutsch (DE)";
            case "hi" -> "हिन्दी (HI)";
            default -> locale.getDisplayName(locale);
        };
    }

    /**
     * Cycle to the next supported language in rotation (useful for hotkey Ctrl/Cmd + L).
     */
    public static synchronized Locale cycleNextLanguage() {
        int idx = SUPPORTED_LOCALES.indexOf(currentLocale);
        int nextIdx = (idx + 1) % SUPPORTED_LOCALES.size();
        Locale next = SUPPORTED_LOCALES.get(nextIdx);
        setLocale(next);
        return next;
    }

    /**
     * Register a listener to be invoked whenever the active Locale changes.
     */
    public static void addListener(Consumer<Locale> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister a listener.
     */
    public static void removeListener(Consumer<Locale> listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(Locale locale) {
        for (Consumer<Locale> listener : listeners) {
            try {
                listener.accept(locale);
            } catch (Exception ex) {
                AppLogger.warn(MODULE, "Exception in locale change listener: " + ex.getMessage());
            }
        }
    }
}
