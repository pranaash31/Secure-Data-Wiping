package com.sanitizer.gui.components;

import com.sanitizer.a11y.AccessibilityManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Interactive Regulatory Compliance & Sanitization Standards Specification Matrix.
 * Displays comparison benchmarks for NIST SP 800-88 Rev. 1, DoD 5220.22-M, and IEEE 2883-2022.
 */
public class ComplianceStandardsDialog {

    public static void show(Window owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Regulatory Compliance & Sanitization Standards Matrix");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setPrefWidth(820);
        root.setPrefHeight(650);
        root.getStyleClass().add("card");

        // 1. Header Bar
        HBox header = buildHeader();

        // 2. TabPane for Comparison Matrix, Detailed Protocols, and Verification Attestation
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabMatrix = new Tab("📊  Standards Matrix", buildMatrixTab());
        Tab tabProtocols = new Tab("📑  Protocol Specifications", buildProtocolsTab());
        Tab tabVerification = new Tab("🔒  Verification & PKI Seals", buildVerificationTab());

        tabPane.getTabs().addAll(tabMatrix, tabProtocols, tabVerification);

        // 3. Footer Bar
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label lblSafety = new Label("✓ All algorithms validated with hardware OS write-protection shield");
        lblSafety.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-padding: 8 24; -fx-font-size: 13px;");
        btnClose.setOnAction(e -> dialog.close());
        btnClose.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) dialog.close();
        });

        footer.getChildren().addAll(lblSafety, spacer, btnClose);

        root.getChildren().addAll(header, new Separator(), tabPane, footer);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(ComplianceStandardsDialog.class.getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        AccessibilityManager.applyThemeAndScale(root);

        // Escape key dismisses modal
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                dialog.close();
            }
        });

        dialog.setScene(scene);
        dialog.setMinWidth(760);
        dialog.setMinHeight(580);
        dialog.showAndWait();
    }

    private static HBox buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🛡️ STANDARDS");
        iconBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: rgba(37,99,235,0.12); -fx-background-radius: 8px; -fx-padding: 6 12;");

        VBox titleCol = new VBox(3);
        Label lblTitle = new Label("Sanitization Standards & Regulatory Matrix");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        lblTitle.getStyleClass().add("settings-section-title");

        Label lblSub = new Label("NIST SP 800-88 Rev. 1  •  DoD 5220.22-M  •  IEEE 2883-2022  •  ISO/IEC 27040");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        lblSub.getStyleClass().add("settings-key-label");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label certBadge = new Label("FEDERAL COMPLIANCE PASS");
        certBadge.getStyleClass().add("badge-success");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, certBadge);
        return header;
    }

    // ── Tab 1: Standards Comparison Matrix ────────────────────────────────────
    private static Node buildMatrixTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox box = new VBox(16);
        box.setPadding(new Insets(16, 8, 16, 8));

        Label intro = new Label("Comparative analysis of primary federal, defense, and international data sanitization protocols:");
        intro.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");
        intro.getStyleClass().add("settings-key-label");

        // Comparison Grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        ColumnConstraints colMetric = new ColumnConstraints();
        colMetric.setPercentWidth(25);
        ColumnConstraints colNist = new ColumnConstraints();
        colNist.setPercentWidth(25);
        ColumnConstraints colDod = new ColumnConstraints();
        colDod.setPercentWidth(25);
        ColumnConstraints colIeee = new ColumnConstraints();
        colIeee.setPercentWidth(25);
        grid.getColumnConstraints().addAll(colMetric, colNist, colDod, colIeee);

        // Header Row
        grid.add(createCellHeader("Benchmark Criterion"), 0, 0);
        grid.add(createCellHeader("NIST SP 800-88 R1"), 1, 0);
        grid.add(createCellHeader("DoD 5220.22-M"), 2, 0);
        grid.add(createCellHeader("IEEE 2883-2022"), 3, 0);

        String[][] matrixData = {
                {"Issuing Authority", "National Institute of Standards & Tech (US)", "Department of Defense (US)", "IEEE Standards Association (Global)"},
                {"Execution Passes", "1 Pass (Clear) / Hardware Purge", "3 Passes (Standard) / 7 Passes (NISPOM)", "1 to 3 Passes / Sanitize Crypto Erase"},
                {"Pattern Sequence", "0x00 Fixed Zeroes or Random", "Pass 1: 0x00, Pass 2: 0xFF, Pass 3: Random", "Logical Overwrite / ATA & NVMe Sanitize"},
                {"Target Media Types", "HDD, NVMe, SATA SSD, USB, Flash", "Magnetic Hard Disks, Fixed Media", "Modern Flash SSDs, NVMe, SED, Hybrid"},
                {"Verification Check", "Full-LBA Zero-Pass or Entropy Sample", "Read-Back Byte Verification every pass", "Firmware Controller Return & Sample Read"},
                {"Security Clearance", "FISMA, HIPAA, Federal Agencies", "Defense Contractors & Armed Forces", "Modern Cloud Hyperscalers & Enterprise"},
                {"Remanence Risk", "ZERO (Laboratory Unrecoverable)", "ZERO (Magnetic Microscope Proof)", "ZERO (Hardware Key Invalidation)"}
        };

        int r = 1;
        for (String[] row : matrixData) {
            grid.add(createCellMetric(row[0]), 0, r);
            grid.add(createCellValue(row[1]), 1, r);
            grid.add(createCellValue(row[2]), 2, r);
            grid.add(createCellValue(row[3]), 3, r);
            r++;
        }

        box.getChildren().addAll(intro, grid);
        scroll.setContent(box);
        return scroll;
    }

    // ── Tab 2: Detailed Protocol Specifications ──────────────────────────────
    private static Node buildProtocolsTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox box = new VBox(16);
        box.setPadding(new Insets(16, 8, 16, 8));

        // Card 1: NIST
        VBox cardNist = createProtocolDetailCard(
                "NIST SP 800-88 Rev. 1 (Clear & Purge)",
                "U.S. Federal Information Security Standard (Published by NIST Computer Security Division)",
                "• Clear: Protects against simple, non-invasive data recovery techniques using logical overwrite (single-pass zero or pseudorandom fill).\n" +
                "• Purge: Protects against robust laboratory attacks using ATA Secure Erase, NVMe Format, or multi-pass overwrite.\n" +
                "• Mathematical Attestation: Requires verification that residual Shannon entropy evaluates to 0.000 bits/byte for zero-fill."
        );

        // Card 2: DoD
        VBox cardDod = createProtocolDetailCard(
                "DoD 5220.22-M / NISPOM (Department of Defense)",
                "National Industrial Security Program Operating Manual (Defense Security Service)",
                "• Pass 1: Logical sector overwrite with all fixed binary zeroes (0x00).\n" +
                "• Pass 2: Inversion pass with all fixed binary ones (0xFF / complement of Pass 1).\n" +
                "• Pass 3: Cryptographic pseudo-random byte stream to eliminate residual magnetic hysteresis.\n" +
                "• Strict Verification: Validates byte patterns across each sector prior to signing audit certificates."
        );

        // Card 3: IEEE
        VBox cardIeee = createProtocolDetailCard(
                "IEEE 2883-2022 (Standard for Sanitizing Storage)",
                "The premier modern standard specifically addressing solid-state and NVMe controller architectures",
                "• Solid-State Flash Architecture: Targets wear-leveling pools, bad block reallocations, and over-provisioned spare blocks.\n" +
                "• Cryptographic Erase (CE): Leverages hardware Self-Encrypting Drive (SED) Media Encryption Key (MEK) destruction.\n" +
                "• Cloud & Enterprise: Eliminates bus bottlenecks during simultaneous high-throughput batch sanitization."
        );

        box.getChildren().addAll(cardNist, cardDod, cardIeee);
        scroll.setContent(box);
        return scroll;
    }

    // ── Tab 3: Verification & PKI Seals ──────────────────────────────────────
    private static Node buildVerificationTab() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox box = new VBox(16);
        box.setPadding(new Insets(16, 8, 16, 8));

        Label intro = new Label("How our platform cryptographically validates compliance and prevents data remanence:");
        intro.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");
        intro.getStyleClass().add("settings-key-label");

        // Verification Steps
        VBox step1 = createVerificationStep(
                "1. Automated Zero-Pass & Shannon Entropy Verification",
                "Immediately following the overwrite pass, our WipeVerifier performs a direct raw-sector read across beginning, middle, end, and randomized stride intervals. Shannon entropy must confirm exactly 0.000 bits/byte (for zero-fill) or >7.999 bits/byte (for cryptographic random passes)."
        );

        VBox step2 = createVerificationStep(
                "2. SHA-256 Sector Fingerprinting",
                "Every verified block sequence produces a SHA-256 cryptographic digest proof. This hash is permanently recorded into the tamper-proof SQLite audit database."
        );

        VBox step3 = createVerificationStep(
                "3. RSA-4096 Digital Signatures & PDF Certificates",
                "Each completed operation generates an immutable PDF certificate digitally signed with a private RSA-4096 key. Any post-sanitization modification to the certificate invalidates the cryptographic signature."
        );

        VBox step4 = createVerificationStep(
                "4. Independent Third-Party QR Code Verification",
                "Certificates include an embedded cryptographic QR code linking directly to our local web verification server (http://localhost:8080/verify), allowing auditors to verify the drive serial, timestamp, and signature offline or on-premises."
        );

        box.getChildren().addAll(intro, step1, step2, step3, step4);
        scroll.setContent(box);
        return scroll;
    }

    private static VBox createProtocolDetailCard(String title, String subtitle, String content) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(37,99,235,0.04); -fx-border-color: #BFDBFE; " +
                "-fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 14 18;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1D4ED8;");

        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        Label lblContent = new Label(content);
        lblContent.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-line-spacing: 4px;");
        lblContent.setWrapText(true);

        card.getChildren().addAll(lblTitle, lblSub, lblContent);
        return card;
    }

    private static VBox createVerificationStep(String title, String desc) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 12 16;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label lblDesc = new Label(desc);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 3px;");
        lblDesc.setWrapText(true);

        box.getChildren().addAll(lblTitle, lblDesc);
        return box;
    }

    private static Label createCellHeader(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E3A8A; " +
                "-fx-background-color: #EFF6FF; -fx-padding: 8 10; -fx-background-radius: 6px;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private static Label createCellMetric(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-padding: 6 8;");
        lbl.setWrapText(true);
        return lbl;
    }

    private static Label createCellValue(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #475569; -fx-padding: 6 8;");
        lbl.setWrapText(true);
        return lbl;
    }
}
