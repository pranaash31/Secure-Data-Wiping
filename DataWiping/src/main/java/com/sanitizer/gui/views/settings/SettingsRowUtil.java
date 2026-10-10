package com.sanitizer.gui.views.settings;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Utility helper for standard two-column key-value settings rows.
 */
public final class SettingsRowUtil {

    private SettingsRowUtil() {}

    public static HBox createSettingRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblKey = new Label(label);
        lblKey.getStyleClass().add("settings-key-label");

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        Label lblVal = new Label(value);
        lblVal.getStyleClass().add("settings-value-label");

        row.getChildren().addAll(lblKey, r, lblVal);
        return row;
    }
}
