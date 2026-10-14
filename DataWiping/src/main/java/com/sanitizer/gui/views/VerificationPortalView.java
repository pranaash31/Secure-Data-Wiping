package com.sanitizer.gui.views;

import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.service.CertificateVerificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

import com.sanitizer.gui.navigation.ViewLifecycle;

public class VerificationPortalView implements ViewLifecycle {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(20);
    private final CertificateVerificationService verificationService;

    private TextField txtTokenInput;
    private Label lblResultBadge;
    private Label lblResultTitle;
    private Label lblResultDesc;

    private Label lblCertId;
    private Label lblDeviceModel;
    private Label lblSerial;
    private Label lblCapacity;
    private Label lblMethod;
    private Label lblStatus;
    private Label lblEsgFootprint;
    private TextArea txtSignature;
    private Button btnOpenWebPortal;

    private String currentVerifyUrl = null;

    public VerificationPortalView() {
        this(CertificateVerificationService.getInstance());
    }

    public VerificationPortalView(CertificateVerificationService verificationService) {
        this.verificationService = verificationService != null ? verificationService : CertificateVerificationService.getInstance();
        buildUi();
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    @Override
    public void onActivated() {
        loadLatestAuditIfAvailable();
    }

    @Override
    public void onDeactivated() {
        // No background tasks
    }

    private void buildUi() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(24, 28, 28, 28));

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Certificate & Cryptographic Seal Verification Portal");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Independently audit and validate tamper-proof PDF certificates with 2048-bit RSA digital signatures");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("RSA-2048 AUDITOR");
        badge.getStyleClass().add("badge-info");
        badge.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");

        header.getChildren().addAll(titleBox, spacer, badge);

        // ── Card 1: Input & Drag-and-Drop Zone ────────────────────────────
        VBox cardInput = new VBox(14);
        cardInput.getStyleClass().add("card");
        cardInput.setPadding(new Insets(20));

        Label inputTitle = new Label("Select or Drag & Drop PDF Sanitization Certificate");
        inputTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        // Drag and Drop Target Box
        VBox dropBox = new VBox(10);
        dropBox.setAlignment(Pos.CENTER);
        dropBox.setPadding(new Insets(30));
        dropBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

        Label dropIcon = new Label("📄");
        dropIcon.setStyle("-fx-font-size: 28px;");

        Label dropLabel = new Label("Drag and drop any generated .PDF Sanitization Certificate here");
        dropLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Button btnBrowse = new Button("📂 Browse PDF File");
        btnBrowse.getStyleClass().add("button-primary");
        btnBrowse.setOnAction(e -> handleBrowseFile());

        dropBox.getChildren().addAll(dropIcon, dropLabel, btnBrowse);

        // Drag and Drop Event Listeners
        dropBox.setOnDragOver(event -> {
            if (event.getGestureSource() != dropBox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                dropBox.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #3B82F6; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            }
            event.consume();
        });

        dropBox.setOnDragExited(event -> {
            dropBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-style: dashed; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            event.consume();
        });

        dropBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    verifyPdfFile(file);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Or Paste URL / Token Box
        HBox tokenBox = new HBox(10);
        tokenBox.setAlignment(Pos.CENTER_LEFT);

        txtTokenInput = new TextField();
        txtTokenInput.setPromptText("Or paste scanned QR Code URL (e.g. http://localhost:8080/verify?certId=...)");
        HBox.setHgrow(txtTokenInput, Priority.ALWAYS);

        Button btnVerifyToken = new Button("🔍 Verify Token");
        btnVerifyToken.getStyleClass().add("button-success");
        btnVerifyToken.setOnAction(e -> handleTokenVerification());

        tokenBox.getChildren().addAll(txtTokenInput, btnVerifyToken);

        cardInput.getChildren().addAll(inputTitle, dropBox, new Separator(), tokenBox);

        // ── Card 2: Cryptographic Audit Results ─────────────────────────
        VBox cardResult = new VBox(16);
        cardResult.getStyleClass().add("card");
        cardResult.setPadding(new Insets(20));

        HBox resultHeader = new HBox(14);
        resultHeader.setAlignment(Pos.CENTER_LEFT);

        lblResultBadge = new Label("READY FOR AUDIT");
        lblResultBadge.getStyleClass().add("badge-info");
        lblResultBadge.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");

        lblResultTitle = new Label("Cryptographic Audit Results");
        lblResultTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region rSpacer = new Region();
        HBox.setHgrow(rSpacer, Priority.ALWAYS);

        btnOpenWebPortal = new Button("🌐 Open in Web Verification Portal");
        btnOpenWebPortal.getStyleClass().add("button-secondary");
        btnOpenWebPortal.setDisable(true);
        btnOpenWebPortal.setOnAction(e -> openWebPortalUrl());

        resultHeader.getChildren().addAll(lblResultTitle, rSpacer, lblResultBadge, btnOpenWebPortal);

        lblResultDesc = new Label("Load a PDF certificate or scan QR token to verify cryptographic integrity against RSA-2048 public key vault.");
        lblResultDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        // Details Grid
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);

        lblCertId = createDetailValue("N/A");
        lblDeviceModel = createDetailValue("N/A");
        lblSerial = createDetailValue("N/A");
        lblCapacity = createDetailValue("N/A");
        lblMethod = createDetailValue("N/A");
        lblStatus = createDetailValue("N/A");

        addGridRow(grid, 0, "Certificate ID:", lblCertId, "Sanitization Method:", lblMethod);
        addGridRow(grid, 1, "Device Model:", lblDeviceModel, "Execution Status:", lblStatus);
        addGridRow(grid, 2, "Serial Number:", lblSerial, "Storage Capacity:", lblCapacity);

        // ESG Impact Pill
        lblEsgFootprint = new Label("🌱 ESG Impact: ~1.4 kg e-waste diverted | ~12.6 kg CO2 emissions prevented");
        lblEsgFootprint.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #065F46; -fx-padding: 8 14; -fx-background-radius: 8px; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Signature Area
        Label lblSigTitle = new Label("RSA-2048 Digital Signature & Cryptographic Payload:");
        lblSigTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        txtSignature = new TextArea();
        txtSignature.setEditable(false);
        txtSignature.setPrefRowCount(3);
        txtSignature.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        txtSignature.setPromptText("Digital signature data will appear here...");

        cardResult.getChildren().addAll(resultHeader, lblResultDesc, grid, lblEsgFootprint, lblSigTitle, txtSignature);

        rootContainer.getChildren().addAll(header, cardInput, cardResult);

        // Default verification on latest record if exists
        loadLatestAuditIfAvailable();
    }

    private void handleBrowseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Sanitization Certificate PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"));
        File file = chooser.showOpenDialog(rootContainer.getScene() != null ? rootContainer.getScene().getWindow() : null);
        if (file != null) {
            verifyPdfFile(file);
        }
    }

    private void verifyPdfFile(File file) {
        CertificateGenerator.CertificateVerificationResult result = verificationService.verifyPdfCertificate(file);

        if (result.isValid()) {
            lblResultBadge.setText("AUTHENTIC & VERIFIED");
            lblResultBadge.getStyleClass().setAll("badge-success");
            lblResultTitle.setText("✅ " + file.getName() + " — AUTHENTIC");
            lblResultDesc.setText("Cryptographic verification passed. RSA-2048 digital signature is valid and immutable.");
        } else {
            lblResultBadge.setText("INVALID / TAMPERED");
            lblResultBadge.getStyleClass().setAll("badge-danger");
            lblResultTitle.setText("❌ " + file.getName() + " — SIGNATURE MISMATCH");
            lblResultDesc.setText("Verification failed: " + result.message());
        }

        lblCertId.setText(result.certificateId() != null ? result.certificateId() : "N/A");
        lblDeviceModel.setText(result.driveModel() != null ? result.driveModel() : "N/A");
        lblSerial.setText(result.serialNumber() != null ? result.serialNumber() : "N/A");
        lblCapacity.setText(result.capacity() != null ? result.capacity() : "N/A");
        lblMethod.setText(result.wipeStandard() != null ? result.wipeStandard() : "N/A");
        lblStatus.setText(result.status() != null ? result.status() : "N/A");

        EsgCalculator.EsgMetrics esg = EsgCalculator.calculate(result.capacity());
        lblEsgFootprint.setText("🌱 ESG Sustainability Proof: Diverted ~" + String.format(java.util.Locale.US, "%.1f", esg.eWasteDivertedKg()) +
                " kg e-waste | Avoided ~" + String.format(java.util.Locale.US, "%.1f", esg.co2EmissionsSavedKg()) + " kg CO₂ (Scope 3 GHG)");

        txtSignature.setText(result.digitalSignature() != null ? result.digitalSignature() : "No signature found");

        currentVerifyUrl = verificationService.generateVerificationUrl(result);
        btnOpenWebPortal.setDisable(currentVerifyUrl == null);
    }

    private void handleTokenVerification() {
        String input = txtTokenInput.getText().trim();
        if (input.isBlank()) return;

        var result = verificationService.verifyTokenUrl(input);

        if (result.isFormatValid()) {
            if (result.isValid()) {
                lblResultBadge.setText("AUTHENTIC & VERIFIED");
                lblResultBadge.getStyleClass().setAll("badge-success");
                lblResultTitle.setText("✅ " + result.certificateId() + " — AUTHENTIC");
                lblResultDesc.setText("Cryptographic verification passed via Web Portal token.");
            } else {
                lblResultBadge.setText("INVALID / TAMPERED");
                lblResultBadge.getStyleClass().setAll("badge-danger");
                lblResultTitle.setText("❌ " + result.certificateId() + " — SIGNATURE MISMATCH");
                lblResultDesc.setText("Token signature mismatch or payload modified.");
            }

            lblCertId.setText(result.certificateId());
            lblDeviceModel.setText(result.driveModel());
            lblSerial.setText(result.serialNumber());
            lblCapacity.setText(result.capacity());
            lblMethod.setText(result.wipeStandard());
            lblStatus.setText(result.status());
            txtSignature.setText(result.digitalSignature());

            EsgCalculator.EsgMetrics esg = result.esgMetrics();
            lblEsgFootprint.setText("🌱 ESG Sustainability Proof: Diverted ~" + String.format(java.util.Locale.US, "%.1f", esg.eWasteDivertedKg()) +
                    " kg e-waste | Avoided ~" + String.format(java.util.Locale.US, "%.1f", esg.co2EmissionsSavedKg()) + " kg CO₂ (Scope 3 GHG)");

            currentVerifyUrl = result.verificationUrl();
            btnOpenWebPortal.setDisable(false);
        } else {
            lblResultBadge.setText("FORMAT ERROR");
            lblResultBadge.getStyleClass().setAll("badge-warning");
            lblResultDesc.setText(result.message());
        }
    }

    private void openWebPortalUrl() {
        if (currentVerifyUrl == null) return;
        verificationService.openInBrowser(currentVerifyUrl);
    }

    private void loadLatestAuditIfAvailable() {
        verificationService.findLatestCertificateFile().ifPresent(this::verifyPdfFile);
    }

    private void addGridRow(GridPane grid, int row, String lbl1, Label val1, String lbl2, Label val2) {
        Label l1 = new Label(lbl1);
        l1.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        Label l2 = new Label(lbl2);
        l2.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        grid.add(l1, 0, row);
        grid.add(val1, 1, row);
        grid.add(l2, 2, row);
        grid.add(val2, 3, row);
    }

    private Label createDetailValue(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        return lbl;
    }
}
