package com.sanitizer.gui.components;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Command Palette (Cmd+K / Ctrl+K) Unit Tests")
class CommandPaletteDialogTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify command catalog building and categories coverage")
    void testCommandCatalogBuilding() {
        NavigationManager navManager = NavigationManager.getInstance();
        List<CommandPaletteDialog.CommandItem> commands = CommandPaletteDialog.buildCommandCatalog(navManager);

        assertThat(commands).isNotEmpty();
        assertThat(commands.size()).isGreaterThanOrEqualTo(15);

        // Verify that all essential categories exist
        boolean hasNav = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.NAVIGATION);
        boolean hasWipe = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.WIPE_ACTION);
        boolean hasHw = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.HARDWARE);
        boolean hasTheme = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.THEME);
        boolean hasLang = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.LANGUAGE);
        boolean hasAudit = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.AUDIT);
        boolean hasSec = commands.stream().anyMatch(c -> c.category() == CommandPaletteDialog.Category.SECURITY);

        assertThat(hasNav).isTrue();
        assertThat(hasWipe).isTrue();
        assertThat(hasHw).isTrue();
        assertThat(hasTheme).isTrue();
        assertThat(hasLang).isTrue();
        assertThat(hasAudit).isTrue();
        assertThat(hasSec).isTrue();
    }

    @Test
    @DisplayName("Verify CommandItem search and fuzzy keyword matching")
    void testCommandItemMatching() {
        CommandPaletteDialog.CommandItem item = new CommandPaletteDialog.CommandItem(
                "test_item",
                "Start NIST SP 800-88 Rev. 1 Clear (1-Pass)",
                "Standard logical block zero-fill overwrite",
                CommandPaletteDialog.Category.WIPE_ACTION,
                "🛡️",
                "Ctrl+W",
                List.of("nist", "800-88", "clear", "single"),
                () -> {}
        );

        // Direct Title match
        assertThat(item.matches("NIST")).isTrue();
        assertThat(item.matches("800-88")).isTrue();
        assertThat(item.matches("clear")).isTrue();

        // Subtitle match
        assertThat(item.matches("zero-fill")).isTrue();
        assertThat(item.matches("overwrite")).isTrue();

        // Keyword match
        assertThat(item.matches("single")).isTrue();

        // Category match
        assertThat(item.matches("Wipe Operations")).isTrue();

        // Blank/Empty query matches everything
        assertThat(item.matches("")).isTrue();
        assertThat(item.matches("   ")).isTrue();
        assertThat(item.matches(null)).isTrue();

        // Non-matching query
        assertThat(item.matches("quantum crypto decrypt")).isFalse();
    }

    @Test
    @DisplayName("Verify Theme switching command execution from palette")
    void testThemeCommandExecution() {
        NavigationManager navManager = NavigationManager.getInstance();
        List<CommandPaletteDialog.CommandItem> commands = CommandPaletteDialog.buildCommandCatalog(navManager);

        CommandPaletteDialog.CommandItem darkThemeCmd = commands.stream()
                .filter(c -> "theme_dark".equals(c.id()))
                .findFirst()
                .orElseThrow();

        darkThemeCmd.action().run();
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.DARK);

        CommandPaletteDialog.CommandItem lightThemeCmd = commands.stream()
                .filter(c -> "theme_light".equals(c.id()))
                .findFirst()
                .orElseThrow();

        lightThemeCmd.action().run();
        assertThat(AccessibilityManager.getTheme()).isEqualTo(AccessibilityManager.Theme.LIGHT);
    }

    @Test
    @DisplayName("Verify Language switching command execution from palette")
    void testLanguageCommandExecution() {
        NavigationManager navManager = NavigationManager.getInstance();
        List<CommandPaletteDialog.CommandItem> commands = CommandPaletteDialog.buildCommandCatalog(navManager);

        CommandPaletteDialog.CommandItem deLangCmd = commands.stream()
                .filter(c -> "lang_de".equals(c.id()))
                .findFirst()
                .orElseThrow();

        deLangCmd.action().run();
        assertThat(I18n.getLocale()).isEqualTo(Locale.GERMAN);

        // Reset to English
        CommandPaletteDialog.CommandItem enLangCmd = commands.stream()
                .filter(c -> "lang_en".equals(c.id()))
                .findFirst()
                .orElseThrow();

        enLangCmd.action().run();
        assertThat(I18n.getLocale()).isEqualTo(Locale.ENGLISH);
    }
}
