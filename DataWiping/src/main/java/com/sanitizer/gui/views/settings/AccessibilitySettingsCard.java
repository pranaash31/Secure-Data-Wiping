package com.sanitizer.gui.views.settings;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.components.AccessibilityHelpDialog;
import com.sanitizer.gui.components.HeatmapPalette;
import com.sanitizer.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.util.Locale;

/**
 * Settings Card encapsulating Internationalization & Accessibility (WCAG 2.1 AA / Section 508).
 */
public class AccessibilitySettingsCard extends VBox {

    public AccessibilitySettingsCard() {
        super(18);
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("♿ [I18N & A11Y]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: rgba(37,99,235,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label(I18n.get("a11y.section_title"));
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label(I18n.get("a11y.section_sub"));
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        lblDesc.getStyleClass().add("settings-key-label");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label wcagBadge = new Label(I18n.get("a11y.wcag_badge"));
        wcagBadge.getStyleClass().add("badge-success");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, wcagBadge);

        // Grid of Settings
        GridPane a11yGrid = new GridPane();
        a11yGrid.setHgap(20);
        a11yGrid.setVgap(16);

        // 1. Language Picker
        VBox langBox = new VBox(6);
        Label lblLang = new Label(I18n.get("a11y.language_select"));
        lblLang.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblLang.getStyleClass().add("settings-key-label");

        ComboBox<Locale> cmbLang = new ComboBox<>();
        cmbLang.getItems().addAll(I18n.getSupportedLocales());
        cmbLang.setValue(I18n.getLocale());
        cmbLang.setMaxWidth(Double.MAX_VALUE);
        cmbLang.setConverter(new StringConverter<>() {
            @Override
            public String toString(Locale locale) {
                return I18n.getLanguageDisplayName(locale);
            }
            @Override
            public Locale fromString(String s) { return null; }
        });
        cmbLang.setOnAction(e -> {
            Locale selected = cmbLang.getValue();
            if (selected != null && !selected.equals(I18n.getLocale())) {
                I18n.setLocale(selected);
            }
        });
        AccessibilityManager.setupAccessible(cmbLang, "Language Selector", "Select interface language", AccessibleRole.COMBO_BOX);
        langBox.getChildren().addAll(lblLang, cmbLang);

        // 2. Contrast Theme Picker
        VBox themeBox = new VBox(6);
        Label lblTheme = new Label(I18n.get("a11y.theme_select"));
        lblTheme.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblTheme.getStyleClass().add("settings-key-label");

        ComboBox<AccessibilityManager.Theme> cmbTheme = new ComboBox<>();
        cmbTheme.getItems().addAll(AccessibilityManager.Theme.values());
        cmbTheme.setValue(AccessibilityManager.getTheme());
        cmbTheme.setMaxWidth(Double.MAX_VALUE);
        cmbTheme.setConverter(new StringConverter<>() {
            @Override
            public String toString(AccessibilityManager.Theme t) {
                return t != null ? t.getDisplayName() : "";
            }
            @Override
            public AccessibilityManager.Theme fromString(String s) { return null; }
        });
        cmbTheme.setOnAction(e -> {
            AccessibilityManager.Theme selected = cmbTheme.getValue();
            if (selected != null) {
                AccessibilityManager.setTheme(selected);
            }
        });
        AccessibilityManager.setupAccessible(cmbTheme, "Theme Selector", "Select visual contrast theme", AccessibleRole.COMBO_BOX);
        themeBox.getChildren().addAll(lblTheme, cmbTheme);

        // 3. Font Scaling / Zoom Level
        VBox scaleBox = new VBox(6);
        Label lblScale = new Label(I18n.get("a11y.font_scale_title"));
        lblScale.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblScale.getStyleClass().add("settings-key-label");

        HBox scaleControls = new HBox(8);
        scaleControls.setAlignment(Pos.CENTER_LEFT);

        Button btnScaleDown = new Button("A-");
        btnScaleDown.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleDown.setOnAction(e -> AccessibilityManager.decreaseFontScale());
        AccessibilityManager.setupAccessible(btnScaleDown, "Decrease Font Scale", "Scales down font size", AccessibleRole.BUTTON);

        Label lblScaleValue = new Label((int)(AccessibilityManager.getFontScale() * 100) + "%");
        lblScaleValue.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 50px; -fx-alignment: CENTER;");
        AccessibilityManager.addScaleListener(s -> lblScaleValue.setText((int)(s * 100) + "%"));

        Button btnScaleUp = new Button("A+");
        btnScaleUp.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleUp.setOnAction(e -> AccessibilityManager.increaseFontScale());
        AccessibilityManager.setupAccessible(btnScaleUp, "Increase Font Scale", "Scales up font size", AccessibleRole.BUTTON);

        Button btnScaleReset = new Button(I18n.get("a11y.zoom_reset"));
        btnScaleReset.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        btnScaleReset.setOnAction(e -> AccessibilityManager.resetFontScale());
        AccessibilityManager.setupAccessible(btnScaleReset, "Reset Font Scale", "Resets font scaling to 100%", AccessibleRole.BUTTON);

        scaleControls.getChildren().addAll(btnScaleDown, lblScaleValue, btnScaleUp, btnScaleReset);
        scaleBox.getChildren().addAll(lblScale, scaleControls);

        // 4. Keyboard Shortcuts Help Action
        VBox actionBox = new VBox(6);
        Label lblHelp = new Label(I18n.get("a11y.keyboard_shortcuts_title"));
        lblHelp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblHelp.getStyleClass().add("settings-key-label");

        Button btnOpenA11yHelp = new Button(I18n.get("a11y.btn_shortcuts_help"));
        btnOpenA11yHelp.getStyleClass().add("button-primary");
        btnOpenA11yHelp.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 16;");
        btnOpenA11yHelp.setOnAction(e -> AccessibilityHelpDialog.show(getScene() != null ? getScene().getWindow() : null));
        btnOpenA11yHelp.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER || ev.getCode() == KeyCode.SPACE) {
                AccessibilityHelpDialog.show(getScene() != null ? getScene().getWindow() : null);
            }
        });
        AccessibilityManager.setupAccessible(btnOpenA11yHelp, "Keyboard Navigation Guide", "Opens keyboard shortcuts and accessibility help dialog (F1)", AccessibleRole.BUTTON);
        actionBox.getChildren().addAll(lblHelp, btnOpenA11yHelp);

        a11yGrid.add(langBox, 0, 0);
        a11yGrid.add(themeBox, 1, 0);
        a11yGrid.add(scaleBox, 0, 1);
        a11yGrid.add(actionBox, 1, 1);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        a11yGrid.getColumnConstraints().addAll(c1, c2);

        // ── 5. Colorblind-Optimized Sector Heatmap Palette ──
        VBox paletteSection = new VBox(10);
        paletteSection.setPadding(new Insets(14));
        paletteSection.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        Label lblPaletteTitle = new Label("🎨 Colorblind-Optimized Sector Visualizer Palette (Deuteranopia, Protanopia, Tritanopia):");
        lblPaletteTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        HBox paletteControls = new HBox(16);
        paletteControls.setAlignment(Pos.CENTER_LEFT);

        ComboBox<HeatmapPalette> cmbPalette = new ComboBox<>();
        cmbPalette.getItems().addAll(HeatmapPalette.values());
        cmbPalette.setValue(AccessibilityManager.getHeatmapPalette());
        cmbPalette.setStyle("-fx-pref-width: 340px; -fx-font-size: 11px;");

        HBox swatchBox = new HBox(10);
        swatchBox.setAlignment(Pos.CENTER_LEFT);

        Runnable updateSwatches = () -> {
            swatchBox.getChildren().clear();
            HeatmapPalette p = cmbPalette.getValue();
            if (p == null) p = AccessibilityManager.getHeatmapPalette();

            swatchBox.getChildren().addAll(
                    createPaletteSwatch(p.getDirtyFill(), "Raw Data"),
                    createPaletteSwatch(p.getActiveHeadFill(), "Write Head"),
                    createPaletteSwatch(p.getPatternFill(), "Pattern"),
                    createPaletteSwatch(p.getZeroedFill(), "Zeroed 0x00"),
                    createPaletteSwatch(p.getBadSectorFill(), "⚠️ Bad LBA")
            );
        };

        cmbPalette.setOnAction(e -> {
            HeatmapPalette selected = cmbPalette.getValue();
            if (selected != null) {
                AccessibilityManager.setHeatmapPalette(selected);
                updateSwatches.run();
            }
        });

        AccessibilityManager.addPaletteListener(p -> {
            if (p != null && !p.equals(cmbPalette.getValue())) {
                cmbPalette.setValue(p);
                updateSwatches.run();
            }
        });

        updateSwatches.run();
        paletteControls.getChildren().addAll(cmbPalette, swatchBox);

        Label lblPaletteDesc = new Label("Provides high-contrast chromatic separation based on scientific Okabe-Ito and ColorBrewer scales to prevent red-green and blue-yellow confusion during live sanitization.");
        lblPaletteDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        paletteSection.getChildren().addAll(lblPaletteTitle, paletteControls, lblPaletteDesc);

        getChildren().addAll(header, new Separator(), a11yGrid, paletteSection);
    }

    private HBox createPaletteSwatch(String colorHex, String labelText) {
        HBox swatch = new HBox(5);
        swatch.setAlignment(Pos.CENTER_LEFT);

        Rectangle box = new Rectangle(12, 12);
        box.setArcWidth(3);
        box.setArcHeight(3);
        box.setStyle("-fx-fill: " + colorHex + "; -fx-stroke: rgba(255,255,255,0.3); -fx-stroke-width: 0.8;");

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1; -fx-font-weight: bold;");

        swatch.getChildren().addAll(box, label);
        return swatch;
    }
}
