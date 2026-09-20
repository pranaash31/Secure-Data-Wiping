package com.sanitizer.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        originalLocale = I18n.getLocale();
    }

    @AfterEach
    void tearDown() {
        I18n.setLocale(originalLocale);
    }

    @Test
    void testSupportedLocalesList() {
        List<Locale> locales = I18n.getSupportedLocales();
        assertThat(locales).hasSize(5);
        assertThat(locales).contains(
                I18n.LOCALE_EN,
                I18n.LOCALE_FR,
                I18n.LOCALE_ES,
                I18n.LOCALE_DE,
                I18n.LOCALE_HI
        );
    }

    @Test
    void testEnglishBundleLoading() {
        I18n.setLocale(I18n.LOCALE_EN);
        assertThat(I18n.getLocale()).isEqualTo(I18n.LOCALE_EN);
        assertThat(I18n.get("app.title")).isEqualTo("SecureErase Pro");
        assertThat(I18n.get("nav.dashboard")).isEqualTo("Dashboard");
        assertThat(I18n.get("topbar.badge")).isEqualTo("ENTERPRISE v2.0");
    }

    @Test
    void testFrenchBundleLoading() {
        I18n.setLocale(I18n.LOCALE_FR);
        assertThat(I18n.getLocale()).isEqualTo(I18n.LOCALE_FR);
        assertThat(I18n.get("nav.dashboard")).isEqualTo("Tableau de Bord");
        assertThat(I18n.get("topbar.sign_out")).isEqualTo("Déconnexion");
        assertThat(I18n.get("app.actions")).isEqualTo("Actions");
    }

    @Test
    void testSpanishBundleLoading() {
        I18n.setLocale(I18n.LOCALE_ES);
        assertThat(I18n.getLocale()).isEqualTo(I18n.LOCALE_ES);
        assertThat(I18n.get("nav.dashboard")).isEqualTo("Panel de Control");
        assertThat(I18n.get("topbar.sign_out")).isEqualTo("Cerrar Sesión");
        assertThat(I18n.get("app.actions")).isEqualTo("Acciones");
    }

    @Test
    void testGermanBundleLoading() {
        I18n.setLocale(I18n.LOCALE_DE);
        assertThat(I18n.getLocale()).isEqualTo(I18n.LOCALE_DE);
        assertThat(I18n.get("nav.dashboard")).isEqualTo("Dashboard");
        assertThat(I18n.get("topbar.sign_out")).isEqualTo("Abmelden");
        assertThat(I18n.get("app.actions")).isEqualTo("Aktionen");
    }

    @Test
    void testHindiBundleLoading() {
        I18n.setLocale(I18n.LOCALE_HI);
        assertThat(I18n.getLocale()).isEqualTo(I18n.LOCALE_HI);
        assertThat(I18n.get("nav.dashboard")).isEqualTo("डैशबोर्ड");
        assertThat(I18n.get("topbar.sign_out")).isEqualTo("साइन आउट");
        assertThat(I18n.get("app.actions")).isEqualTo("कार्रवाई");
    }

    @Test
    void testParameterFormatting() {
        I18n.setLocale(I18n.LOCALE_EN);
        String formattedEn = I18n.get("dashboard.esg_ewaste", "55.4");
        assertThat(formattedEn).contains("55.4 kg");

        I18n.setLocale(I18n.LOCALE_FR);
        String formattedFr = I18n.get("dashboard.esg_ewaste", "55.4");
        assertThat(formattedFr).contains("55.4 kg");
    }

    @Test
    void testMissingKeyFallbackToEnglish() {
        I18n.setLocale(I18n.LOCALE_DE);
        // An unknown random key returns the key itself
        String nonExistent = I18n.get("non.existent.key.xyz");
        assertThat(nonExistent).isEqualTo("non.existent.key.xyz");
    }

    @Test
    void testLocaleChangeListener() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        I18n.addListener(loc -> {
            if (I18n.LOCALE_ES.equals(loc)) {
                triggered.set(true);
            }
        });

        I18n.setLocale(I18n.LOCALE_ES);
        assertThat(triggered.get()).isTrue();
    }

    @Test
    void testCycleLanguageRotation() {
        I18n.setLocale(I18n.LOCALE_EN);
        Locale next1 = I18n.cycleNextLanguage();
        assertThat(next1).isEqualTo(I18n.LOCALE_FR);

        Locale next2 = I18n.cycleNextLanguage();
        assertThat(next2).isEqualTo(I18n.LOCALE_ES);

        Locale next3 = I18n.cycleNextLanguage();
        assertThat(next3).isEqualTo(I18n.LOCALE_DE);

        Locale next4 = I18n.cycleNextLanguage();
        assertThat(next4).isEqualTo(I18n.LOCALE_HI);

        Locale next5 = I18n.cycleNextLanguage();
        assertThat(next5).isEqualTo(I18n.LOCALE_EN);
    }

    @Test
    void testLanguageDisplayName() {
        assertThat(I18n.getLanguageDisplayName(I18n.LOCALE_EN)).contains("English");
        assertThat(I18n.getLanguageDisplayName(I18n.LOCALE_FR)).contains("Français");
        assertThat(I18n.getLanguageDisplayName(I18n.LOCALE_ES)).contains("Español");
        assertThat(I18n.getLanguageDisplayName(I18n.LOCALE_DE)).contains("Deutsch");
        assertThat(I18n.getLanguageDisplayName(I18n.LOCALE_HI)).contains("हिन्दी");
    }
}
