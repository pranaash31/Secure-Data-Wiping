package com.sanitizer.gui.views.settings;

import com.sanitizer.i18n.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.sanitizer.gui.views.settings.SettingsRowUtil.createSettingRow;

/**
 * Settings Card displaying runtime application, Java, OS and hardware environment info.
 */
public class SystemInfoCard extends VBox {

    public SystemInfoCard() {
        super(16);
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        HBox sysHeader = new HBox(10);
        sysHeader.setAlignment(Pos.CENTER_LEFT);
        Label sysIcon = new Label("[SYS]");
        sysIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; " +
                "-fx-background-color: rgba(148,163,184,0.10); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label sysTitle = new Label(I18n.get("settings.sys_title"));
        sysTitle.getStyleClass().add("settings-section-title");
        sysHeader.getChildren().addAll(sysIcon, sysTitle);

        VBox sysFields = new VBox(12);
        sysFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.sys_app"), "SecureErase Pro v2.0.0 Enterprise"),
                createSettingRow(I18n.get("settings.sys_java"), System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"),
                createSettingRow(I18n.get("settings.sys_javafx"), System.getProperty("javafx.version", "21")),
                createSettingRow(I18n.get("settings.sys_os"), System.getProperty("os.name") + " " + System.getProperty("os.version")),
                createSettingRow(I18n.get("settings.sys_arch"), System.getProperty("os.arch")),
                createSettingRow(I18n.get("settings.sys_wipe_binary"), "dd (Block Size: bs=2m)")
        );
        getChildren().addAll(sysHeader, new Separator(), sysFields);
    }
}
