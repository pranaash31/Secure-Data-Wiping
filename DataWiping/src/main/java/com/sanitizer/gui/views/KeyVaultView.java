package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class KeyVaultView {

    private final VBox rootContainer = new VBox(24);

    public KeyVaultView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));
        rootContainer.setStyle("-fx-background-color: #0B0F19;");

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Key Vault & Certificate Authority");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Manage RSA keypairs, digital certificates, and cryptographic signing operations");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label vaultStatus = new Label("VAULT SEALED: SECURE");
        vaultStatus.getStyleClass().add("badge-success");

        header.getChildren().addAll(titleBox, hSpacer, vaultStatus);

        // ── Active Key Info Row ─────────────────────────────────────────
        HBox keyInfoRow = new HBox(16);

        VBox activeKeyCard = new VBox(16);
        activeKeyCard.getStyleClass().add("card");
        HBox.setHgrow(activeKeyCard, Priority.ALWAYS);

        HBox keyHeader = new HBox(10);
        keyHeader.setAlignment(Pos.CENTER_LEFT);
        Label keyIcon = new Label("[KEY]");
        keyIcon.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FBBF24; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 8px; -fx-padding: 6 10;");
        VBox keyTitleBox = new VBox(2);
        Label keyTitle = new Label("Active Signing Keypair");
        keyTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label keySubtitle = new Label("Used for all certificate signing operations");
        keySubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        keyTitleBox.getChildren().addAll(keyTitle, keySubtitle);
        keyHeader.getChildren().addAll(keyIcon, keyTitleBox);

        GridPane keyGrid = new GridPane();
        keyGrid.setHgap(20); keyGrid.setVgap(10);
        String[][] keyData = {
                {"Algorithm", "RSA with SHA-256"},
                {"Key Size", "2048-bit"},
                {"Key ID", "SE-KEY-20250101-001"},
                {"Created", "2025-01-01 09:00:00 UTC"},
                {"Expires", "2030-01-01 09:00:00 UTC"},
                {"Status", "Active & Valid"},
                {"Fingerprint (SHA-256)", "AB:CD:EF:12:34:56:78:9A:BC:DE:F0:12:34:56:78:9A:..."}
        };
        for (int i = 0; i < keyData.length; i++) {
            Label k = new Label(keyData[i][0]);
            k.getStyleClass().add("settings-key-label");
            Label v = new Label(keyData[i][1]);
            v.getStyleClass().add("settings-value-label");
            if (i == 5) v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #34D399;");
            keyGrid.add(k, 0, i);
            keyGrid.add(v, 1, i);
        }
        ColumnConstraints cc0 = new ColumnConstraints(200);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setFillWidth(true);
        keyGrid.getColumnConstraints().addAll(cc0, cc1);

        HBox keyActions = new HBox(12);
        keyActions.setAlignment(Pos.CENTER_LEFT);
        Button btnGenNew = new Button("Generate New Keypair");
        btnGenNew.getStyleClass().add("button-primary");
        Button btnExportPub = new Button("Export Public Key (.PEM)");
        Button btnExportPriv = new Button("Export Private Key (.P12)");
        btnExportPriv.setStyle("-fx-text-fill: #FBBF24;");
        keyActions.getChildren().addAll(btnGenNew, btnExportPub, btnExportPriv);

        activeKeyCard.getChildren().addAll(keyHeader, new Separator(), keyGrid, keyActions);

        // Stats column
        VBox statsCol = new VBox(16);
        statsCol.setMinWidth(200);
        statsCol.setMaxWidth(220);

        VBox certsSigned = makeVaultStat("Certificates Signed", "47", "#60A5FA");
        VBox keysRotated = makeVaultStat("Key Rotations", "3", "#FBBF24");
        VBox vaultAge = makeVaultStat("Vault Age (Days)", "248", "#34D399");
        VBox failedOps = makeVaultStat("Failed Operations", "0", "#34D399");
        statsCol.getChildren().addAll(certsSigned, keysRotated, vaultAge, failedOps);

        keyInfoRow.getChildren().addAll(activeKeyCard, statsCol);

        // ── Certificate Customizer ──────────────────────────────────────
        HBox certRow = new HBox(16);

        VBox certConfigCard = new VBox(16);
        certConfigCard.getStyleClass().add("card");
        HBox.setHgrow(certConfigCard, Priority.ALWAYS);

        Label certTitle = new Label("Certificate Configuration");
        certTitle.getStyleClass().add("card-title");

        GridPane certForm = new GridPane();
        certForm.setHgap(16); certForm.setVgap(12);

        String[][] certFields = {
                {"Issuing Organization", "SecureErase Technologies Pvt. Ltd."},
                {"Certificate Authority", "Govt. of India Root CA Level 2"},
                {"Country", "IN — India"},
                {"Province / State", "Tamil Nadu"},
                {"Officer Signatory Name", "Officer Pranaash"},
                {"Certificate Series", "CERT-2025-SE"},
                {"Validity Duration", "365 Days (1 Year)"}
        };

        int row = 0;
        for (String[] f : certFields) {
            Label lbl = new Label(f[0]);
            lbl.getStyleClass().add("settings-key-label");
            TextField txt = new TextField(f[1]);
            txt.setPrefWidth(260);
            certForm.add(lbl, 0, row);
            certForm.add(txt, 1, row);
            row++;
        }
        ColumnConstraints certCC0 = new ColumnConstraints(200);
        ColumnConstraints certCC1 = new ColumnConstraints();
        certCC1.setFillWidth(true);
        certForm.getColumnConstraints().addAll(certCC0, certCC1);

        HBox certActions = new HBox(12);
        Button btnSaveCert = new Button("Save Configuration");
        btnSaveCert.getStyleClass().add("button-primary");
        Button btnPreview = new Button("Preview Certificate");
        certActions.getChildren().addAll(btnSaveCert, btnPreview);

        certConfigCard.getChildren().addAll(certTitle, certForm, certActions);

        // Certificate Preview Card
        VBox certPreviewCard = new VBox(16);
        certPreviewCard.getStyleClass().add("card");
        certPreviewCard.setMinWidth(280);
        certPreviewCard.setMaxWidth(300);

        Label previewTitle = new Label("Certificate Preview");
        previewTitle.getStyleClass().add("card-title");

        VBox certPreview = new VBox(10);
        certPreview.setStyle("-fx-background-color: #020617; -fx-border-color: #1E293B; -fx-border-radius: 10px; " +
                "-fx-background-radius: 10px; -fx-padding: 20;");

        Label certHeader = new Label("SANITIZATION CERTIFICATE");
        certHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; -fx-letter-spacing: 1px;");
        Label certBorder = new Label("━━━━━━━━━━━━━━━━━━━━");
        certBorder.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px;");
        Label certOrg = new Label("SecureErase Technologies");
        certOrg.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");
        Label certDate = new Label("Date: 2025-09-14");
        certDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        Label certOfficer = new Label("Officer: Pranaash");
        certOfficer.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        Label certStandard = new Label("Standard: NIST SP 800-88");
        certStandard.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        Label certSig = new Label("Signed: RSA-2048/SHA-256");
        certSig.setStyle("-fx-font-size: 10px; -fx-text-fill: #34D399;");

        certPreview.getChildren().addAll(certHeader, certBorder, certOrg, certDate, certOfficer, certStandard, certBorder, certSig);
        certPreviewCard.getChildren().addAll(previewTitle, certPreview);

        certRow.getChildren().addAll(certConfigCard, certPreviewCard);

        rootContainer.getChildren().addAll(header, keyInfoRow, certRow);
    }

    private VBox makeVaultStat(String label, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569; -fx-font-weight: bold;");
        card.getChildren().addAll(val, lbl);
        return card;
    }
}
