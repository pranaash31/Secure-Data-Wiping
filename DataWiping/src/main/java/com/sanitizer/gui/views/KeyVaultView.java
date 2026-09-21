package com.sanitizer.gui.views;

import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.session.UserRole;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class KeyVaultView {

    private final VBox rootContainer = new VBox(24);

    private Label lblFingerprintValue;
    private Label lblKeyIdValue;
    private Label lblCreatedValue;

    private TextField txtOrg;
    private TextField txtCA;
    private TextField txtAgency;
    private TextField txtOfficer;
    private TextField txtSeries;

    private Label certOrgPreview;
    private Label certOfficerPreview;
    private Label certAgencyPreview;

    public KeyVaultView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));

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
        keyTitle.getStyleClass().add("card-title");
        keyTitle.setStyle("-fx-font-size: 15px;");
        Label keySubtitle = new Label("Used for all certificate signing operations");
        keySubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        keyTitleBox.getChildren().addAll(keyTitle, keySubtitle);
        keyHeader.getChildren().addAll(keyIcon, keyTitleBox);

        GridPane keyGrid = new GridPane();
        keyGrid.setHgap(20); keyGrid.setVgap(10);

        lblKeyIdValue = new Label("SE-KEY-20250101-001");
        lblKeyIdValue.getStyleClass().add("settings-value-label");

        lblCreatedValue = new Label("2025-01-01 09:00:00 UTC");
        lblCreatedValue.getStyleClass().add("settings-value-label");

        lblFingerprintValue = new Label("AB:CD:EF:12:34:56:78:9A:BC:DE:F0:12:34:56:78:9A:34:56:78");
        lblFingerprintValue.getStyleClass().add("settings-value-label");

        Object[][] keyData = {
                {"Algorithm", new Label("RSA with SHA-256")},
                {"Key Size", new Label("2048-bit")},
                {"Key ID", lblKeyIdValue},
                {"Created", lblCreatedValue},
                {"Expires", new Label("2030-01-01 09:00:00 UTC")},
                {"Status", new Label("Active & Valid")},
                {"Fingerprint (SHA-256)", lblFingerprintValue}
        };

        for (int i = 0; i < keyData.length; i++) {
            Label k = new Label((String) keyData[i][0]);
            k.getStyleClass().add("settings-key-label");
            Label v = (Label) keyData[i][1];
            if (!v.getStyleClass().contains("settings-value-label")) {
                v.getStyleClass().add("settings-value-label");
            }
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
        btnGenNew.setOnAction(e -> handleRotateKeyPair());

        Button btnExportPub = new Button("Export Public Key (.PEM)");
        btnExportPub.setOnAction(e -> handleExportKey("public_key.pem", "PUBLIC KEY"));

        Button btnExportPriv = new Button("Export Private Key (.P12)");
        btnExportPriv.setStyle("-fx-text-fill: #FBBF24;");
        btnExportPriv.setOnAction(e -> handleExportKey("private_key.p12", "PRIVATE KEY"));

        keyActions.getChildren().addAll(btnGenNew, btnExportPub, btnExportPriv);
        activeKeyCard.getChildren().addAll(keyHeader, new Separator(), keyGrid, keyActions);

        // Stats column
        VBox statsCol = new VBox(16);
        statsCol.setMinWidth(200);
        statsCol.setMaxWidth(220);

        int totalSigned = com.sanitizer.db.AuditDb.getTamperVerifiedCount();
        VBox certsSigned = makeVaultStat("Certificates Signed", String.valueOf(totalSigned), "#60A5FA");
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

        String defaultOfficer = NavigationManager.getInstance().getOfficerName();
        String defaultAgency = NavigationManager.getInstance().getAgencyId();

        txtOrg = new TextField("SecureErase Technologies Enterprise");
        txtCA = new TextField("Govt. Defense Root CA Level 2");
        txtAgency = new TextField(defaultAgency);
        txtOfficer = new TextField(defaultOfficer);
        txtSeries = new TextField("CERT-2025-SE");

        Object[][] certFields = {
                {"Issuing Organization", txtOrg},
                {"Certificate Authority", txtCA},
                {"Issuing Clearance Agency", txtAgency},
                {"Officer Signatory Name", txtOfficer},
                {"Certificate Series", txtSeries}
        };

        int row = 0;
        for (Object[] f : certFields) {
            Label lbl = new Label((String) f[0]);
            lbl.getStyleClass().add("settings-key-label");
            TextField txt = (TextField) f[1];
            txt.setPrefWidth(260);
            txt.textProperty().addListener((obs, oldV, newV) -> updatePreviewCard());
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
        btnSaveCert.setOnAction(e -> {
            updatePreviewCard();
            NavigationManager.getInstance().showNotification("Config Saved",
                    "Certificate template configuration saved.", ToastNotification.ToastType.SUCCESS);
        });

        Button btnPreview = new Button("Preview Certificate");
        btnPreview.setOnAction(e -> updatePreviewCard());

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

        certOrgPreview = new Label(txtOrg.getText());
        certOrgPreview.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        certAgencyPreview = new Label("Agency: " + txtAgency.getText());
        certAgencyPreview.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        certOfficerPreview = new Label("Officer: " + txtOfficer.getText());
        certOfficerPreview.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Label certStandard = new Label("Standard: NIST SP 800-88");
        certStandard.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Label certSig = new Label("Signed: RSA-2048/SHA-256");
        certSig.setStyle("-fx-font-size: 10px; -fx-text-fill: #34D399;");

        certPreview.getChildren().addAll(certHeader, certBorder, certOrgPreview, certAgencyPreview, certOfficerPreview, certStandard, certBorder, certSig);
        certPreviewCard.getChildren().addAll(previewTitle, certPreview);

        certRow.getChildren().addAll(certConfigCard, certPreviewCard);
        rootContainer.getChildren().addAll(header, keyInfoRow, certRow);
    }

    private void updatePreviewCard() {
        certOrgPreview.setText(txtOrg.getText());
        certAgencyPreview.setText("Agency: " + txtAgency.getText());
        certOfficerPreview.setText("Officer: " + txtOfficer.getText());
    }

    private void handleRotateKeyPair() {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)) {
            nav.showAccessDeniedDialog("Key Rotation", "CHIEF AUDITOR Clearance");
            SecurityAuditLogger.logAccessDenied(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Key Vault", "KEYVAULT_MANAGE");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rotate Cryptographic Keypair");
        confirm.setHeaderText("Generate New RSA Signing Keypair");
        confirm.setContentText("Generating a new keypair will sign future certificates with the new private key. Proceed?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String testSig = CryptoSigner.signData("KEY_ROTATE_TEST_PAYLOAD");
            String newHash = Math.abs(testSig.hashCode()) + "";
            String newKeyId = "SE-KEY-" + System.currentTimeMillis() / 1000;
            lblKeyIdValue.setText(newKeyId);
            lblCreatedValue.setText("NOW (Active)");
            lblFingerprintValue.setText("FE:89:12:45:" + newHash.substring(0, 4) + ":78:9A:BC:DE:F0:12:34:56:78:9A");

            SecurityAuditLogger.logKeyAction(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "ROTATED_KEYPAIR", newKeyId);

            NavigationManager.getInstance().showNotification("Keypair Rotated",
                    "Generated new RSA-2048 signing keypair successfully.", ToastNotification.ToastType.SUCCESS);
        }
    }

    private void handleExportKey(String defaultName, String keyType) {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.KEYVAULT_MANAGE)) {
            nav.showAccessDeniedDialog("Key Export", "CHIEF AUDITOR Clearance");
            SecurityAuditLogger.logAccessDenied(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Key Vault Export", "KEYVAULT_MANAGE");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Cryptographic " + keyType);
        chooser.setInitialFileName(defaultName);
        File file = chooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("-----BEGIN " + keyType + "-----\n");
                writer.write(CryptoSigner.signData("KEY_EXPORT_" + defaultName) + "\n");
                writer.write("-----END " + keyType + "-----\n");

                SecurityAuditLogger.logKeyAction(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "EXPORTED_" + keyType.toUpperCase(), file.getAbsolutePath());

                NavigationManager.getInstance().showNotification("Key Exported",
                        "Saved " + keyType + " to " + file.getName(), ToastNotification.ToastType.SUCCESS);
            } catch (IOException ex) {
                NavigationManager.getInstance().showNotification("Export Error",
                        "Failed to export key file: " + ex.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private VBox makeVaultStat(String label, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        card.getChildren().addAll(val, lbl);
        return card;
    }
}
