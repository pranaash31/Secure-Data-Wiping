package com.sanitizer.gui.views.settings;

import com.sanitizer.i18n.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.sanitizer.gui.views.settings.SettingsRowUtil.createSettingRow;

/**
 * Settings Card displaying OS Protection & Hardware Safety Shield details.
 */
public class HardwareSafetyCard extends VBox {

    public HardwareSafetyCard() {
        super(16);
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        HBox shieldHeader = new HBox(10);
        shieldHeader.setAlignment(Pos.CENTER_LEFT);
        Label shieldIcon = new Label("[SHIELD]");
        shieldIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #34D399; " +
                "-fx-background-color: rgba(16,185,129,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label shieldTitle = new Label(I18n.get("settings.shield_title"));
        shieldTitle.getStyleClass().add("settings-section-title");
        shieldHeader.getChildren().addAll(shieldIcon, shieldTitle);

        VBox shieldFields = new VBox(12);
        shieldFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.shield_protected"), "/dev/disk0 (Primary OS)"),
                createSettingRow(I18n.get("settings.shield_enforcement"), "Kernel + App Layer Intercept"),
                createSettingRow(I18n.get("settings.shield_override"), "Hardcoded (Non-Bypassable)"),
                createSettingRow(I18n.get("settings.shield_status"), "ACTIVE — ZERO RISK")
        );
        getChildren().addAll(shieldHeader, new Separator(), shieldFields);
    }
}
