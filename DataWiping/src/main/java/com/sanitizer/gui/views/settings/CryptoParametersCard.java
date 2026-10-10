package com.sanitizer.gui.views.settings;

import com.sanitizer.i18n.I18n;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.sanitizer.gui.views.settings.SettingsRowUtil.createSettingRow;

/**
 * Settings Card displaying Cryptographic Parameters & Ledger configuration.
 */
public class CryptoParametersCard extends VBox {

    public CryptoParametersCard() {
        super(16);
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        HBox cryptoHeader = new HBox(10);
        cryptoHeader.setAlignment(Pos.CENTER_LEFT);
        Label cryptoIcon = new Label("[CRYPTO]");
        cryptoIcon.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FBBF24; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 6px; -fx-padding: 4 8;");
        Label cryptoTitle = new Label(I18n.get("settings.crypto_title"));
        cryptoTitle.getStyleClass().add("settings-section-title");
        cryptoHeader.getChildren().addAll(cryptoIcon, cryptoTitle);

        VBox cryptoFields = new VBox(12);
        cryptoFields.getChildren().addAll(
                createSettingRow(I18n.get("settings.crypto_algo"), "SHA256withRSA"),
                createSettingRow(I18n.get("settings.crypto_keypair"), "RSA 2048-bit"),
                createSettingRow(I18n.get("settings.crypto_barcode"), "ZXing QR Code (150×150)"),
                createSettingRow("Cryptographic Ledger", "SHA-256 Block Chaining Active"),
                createSettingRow(I18n.get("settings.crypto_payload"), "Model|Serial|Capacity|Standard|Status")
        );
        getChildren().addAll(cryptoHeader, new Separator(), cryptoFields);
    }
}
