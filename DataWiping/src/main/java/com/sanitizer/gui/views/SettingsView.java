package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView {

    private final VBox rootContainer = new VBox(20);

    public SettingsView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(24));
        rootContainer.setStyle("-fx-background-color: #F8FAFC;");

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("⚙️ Security Policy & System Settings");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label lblSub = new Label("Cryptographic parameters, hardware safety shield, and PDF export configuration");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // Grid of Settings Cards
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        // --- Card 1: Cryptographic Key Specifications ---
        VBox cardCrypto = new VBox(14);
        cardCrypto.getStyleClass().add("card");

        Label lblCryptoTitle = new Label("🔒 Cryptographic Signing Parameters");
        lblCryptoTitle.getStyleClass().add("card-title");

        VBox cryptoFields = new VBox(10);
        cryptoFields.getChildren().addAll(
                createSettingRow("Signature Algorithm", "SHA256withRSA"),
                createSettingRow("Keypair Strength", "RSA 2048-bit Key Length"),
                createSettingRow("Verification Barcode", "ZXing QR Code Matrix (150x150)"),
                createSettingRow("Audit Payload Format", "Model|Serial|Capacity|Standard|Status")
        );
        cardCrypto.getChildren().addAll(lblCryptoTitle, new Separator(), cryptoFields);

        // --- Card 2: Hardware Safety Shield ---
        VBox cardShield = new VBox(14);
        cardShield.getStyleClass().add("card");

        Label lblShieldTitle = new Label("🛡️ Hardware Safety Shield Guardrails");
        lblShieldTitle.getStyleClass().add("card-title");

        VBox shieldFields = new VBox(10);
        shieldFields.getChildren().addAll(
                createSettingRow("Primary Disk Guard", "ENABLED (disk0 & rdisk0 Blocked)"),
                createSettingRow("Internal Storage Filter", "ENABLED (Apple SSD, NVMe, SATA Ignored)"),
                createSettingRow("Target Capacity Window", "1 GB to 128 GB Removable USB Pen Drives"),
                createSettingRow("macOS Block Unmount", "diskutil unmountDisk /dev/diskX")
        );
        cardShield.getChildren().addAll(lblShieldTitle, new Separator(), shieldFields);

        grid.add(cardCrypto, 0, 0);
        grid.add(cardShield, 1, 0);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // --- Card 3: Storage & System Engine Info ---
        VBox cardSystem = new VBox(14);
        cardSystem.getStyleClass().add("card");

        Label lblSysTitle = new Label("📦 Storage Engine & System Environment");
        lblSysTitle.getStyleClass().add("card-title");

        VBox sysFields = new VBox(10);
        sysFields.getChildren().addAll(
                createSettingRow("Database Engine", "SQLite 3.45 JDBC (sanitizer_history.db)"),
                createSettingRow("PDF Reporting Engine", "Apache PDFBox 3.0.1"),
                createSettingRow("Hardware Detection Library", "OSHI Hardware Abstraction Layer 6.4.10"),
                createSettingRow("Low-Level POSIX Binary", "dd (Block Size: bs=2m)")
        );
        cardSystem.getChildren().addAll(lblSysTitle, new Separator(), sysFields);

        rootContainer.getChildren().addAll(titleBox, grid, cardSystem);
    }

    private HBox createSettingRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #475569;");

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        Label lblVal = new Label(value);
        lblVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #0F172A; -fx-font-weight: bold;");

        row.getChildren().addAll(lblKey, r, lblVal);
        return row;
    }
}
