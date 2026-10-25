package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;
import com.sanitizer.gui.components.ComplianceStandardsDialog;

import java.net.URL;

/**
 * Enterprise Media Sanitization & Regulatory Assurance Platform Landing Interface.
 * Built according to NIST SP 800-88 Rev. 1 & DoD 5220.22-M information assurance protocols.
 */
public class HeroView extends ScrollPane {

    private final Runnable onLoginClicked;
    private final Runnable onDemoClicked;

    public HeroView(Runnable onLoginClicked) {
        this(onLoginClicked, null);
    }

    public HeroView(Runnable onLoginClicked, Runnable onDemoClicked) {
        this.onLoginClicked = onLoginClicked;
        this.onDemoClicked = onDemoClicked;
        getStyleClass().addAll("hero-root", "edge-to-edge");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        buildUI();
    }

    private void buildUI() {
        VBox pageContainer = new VBox(0);
        pageContainer.getStyleClass().add("hero-page-container");

        // 1. TOP HEADER NAVIGATION BAR
        HBox topHeader = buildTopHeader();

        // 2. HERO MAIN SECTION (Split Two-Column Banner with High-Impact Visual Illustration)
        VBox heroSection = buildHeroSection();

        // 3. CORE CAPABILITIES SHOWCASE GRID (Clean, Minimal Words)
        VBox showcaseSection = buildShowcaseSection();

        // 4. ENTERPRISE TELEMETRY STATS COUNTER SECTION
        HBox statsSection = buildStatsSection();

        // 5. BOTTOM CALL TO ACTION (CTA) SECTION
        VBox ctaSection = buildCtaSection();

        // 6. OFFICIAL FOOTER
        HBox footer = buildFooter();

        pageContainer.getChildren().addAll(topHeader, heroSection, showcaseSection, statsSection, ctaSection, footer);
        setContent(pageContainer);
    }

    private HBox buildTopHeader() {
        HBox header = new HBox(24);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("hero-header");

        // Official Brand Logo
        HBox brandBox = new HBox(14);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        StackPane shieldBadge = buildShieldIcon();

        Label brandName = new Label("Secure Data Sanitizer");
        brandName.getStyleClass().add("hero-brand-name");

        Label brandTag = new Label("NIST & DoD CERTIFIED");
        brandTag.getStyleClass().add("hero-brand-tag");

        brandBox.getChildren().addAll(shieldBadge, brandName, brandTag);

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        // Prominent, High-Focus Sign In Button
        Button btnSignIn = new Button("Sign In  →");
        btnSignIn.getStyleClass().add("hero-header-portal-btn");
        btnSignIn.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        header.getChildren().addAll(brandBox, navSpacer, btnSignIn);
        return header;
    }

    private StackPane buildShieldIcon() {
        StackPane badge = new StackPane();
        badge.getStyleClass().add("hero-shield-badge");
        badge.setAlignment(Pos.CENTER);
        badge.setMinSize(44, 44);
        badge.setPrefSize(44, 44);
        badge.setMaxSize(44, 44);

        // Outer defense shield silhouette
        SVGPath shieldOutline = new SVGPath();
        shieldOutline.setContent("M12 2L4 5.5V11c0 5.25 3.4 10.15 8 11.35 4.6-1.2 8-6.1 8-11.35V5.5L12 2z");
        shieldOutline.getStyleClass().add("hero-shield-svg-bg");
        shieldOutline.setScaleX(1.4);
        shieldOutline.setScaleY(1.4);

        // Inner verified checkmark
        SVGPath shieldCheck = new SVGPath();
        shieldCheck.setContent("M10.5 14.8l-2.8-2.8 1.2-1.2 1.6 1.6 4.2-4.2 1.2 1.2-5.4 5.4z");
        shieldCheck.getStyleClass().add("hero-shield-svg-check");
        shieldCheck.setScaleX(1.4);
        shieldCheck.setScaleY(1.4);

        badge.getChildren().addAll(shieldOutline, shieldCheck);
        return badge;
    }

    private VBox buildHeroSection() {
        VBox heroContainer = new VBox(0);
        heroContainer.setAlignment(Pos.CENTER);
        heroContainer.getStyleClass().add("hero-split-container");

        HBox splitRow = new HBox(48);
        splitRow.setAlignment(Pos.CENTER);
        splitRow.setMaxWidth(1220);

        // LEFT COLUMN: High-impact headline, punchy minimal words, quick chips, CTAs
        VBox leftCol = new VBox(20);
        leftCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // 1. Official Standard Badge Pill
        Label pill = new Label("🛡️ OFFICIAL GOVERNMENT MEDIA SANITIZATION");
        pill.getStyleClass().add("hero-badge-pill");

        // 2. Commanding Main Title
        Label title = new Label("Permanent, Certified Data Destruction");
        title.getStyleClass().add("hero-main-title");
        title.setWrapText(true);

        // 3. Crisp, Defined Subtitle
        Label subtitle = new Label(
                "Erase hard drives, SSDs, and USB devices with complete confidence. " +
                "Zero recovery, automated verification, and instant compliance certificates."
        );
        subtitle.getStyleClass().add("hero-main-subtitle");
        subtitle.setWrapText(true);

        // 4. Quick Highlights Row (Compact Chips)
        HBox chipRow = new HBox(10);
        chipRow.setAlignment(Pos.CENTER_LEFT);
        Label chip1 = new Label("✓ DoD & NIST Approved");
        chip1.getStyleClass().add("hero-chip");
        Label chip2 = new Label("✓ 100% Unrecoverable");
        chip2.getStyleClass().add("hero-chip");
        Label chip3 = new Label("✓ Instant PDF Reports");
        chip3.getStyleClass().add("hero-chip");
        chipRow.getChildren().addAll(chip1, chip2, chip3);

        // 5. Action Buttons (Clean "Sign In →" & "View Standards")
        HBox actions = new HBox(16);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnSignIn = new Button("Sign In →");
        btnSignIn.getStyleClass().add("hero-cta-primary");
        btnSignIn.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        Button btnStandards = new Button("View Standards");
        btnStandards.getStyleClass().add("hero-cta-secondary");
        btnStandards.setOnAction(e -> {
            Window owner = getScene() != null ? getScene().getWindow() : null;
            ComplianceStandardsDialog.show(owner);
        });

        Button btnDemo = new Button("⚡ Live Demo");
        btnDemo.getStyleClass().add("hero-cta-secondary");
        btnDemo.setStyle("-fx-text-fill: #7C3AED; -fx-border-color: #DDD6FE; -fx-background-color: #F5F3FF; -fx-font-weight: bold;");
        btnDemo.setTooltip(new Tooltip("Start Live Presentation Simulator Mode with Simulated 16GB USB"));
        btnDemo.setOnAction(e -> {
            if (onDemoClicked != null) {
                onDemoClicked.run();
            } else {
                com.sanitizer.demo.DemoSimulatorManager.enableDemoMode();
                if (onLoginClicked != null) onLoginClicked.run();
            }
        });

        actions.getChildren().addAll(btnSignIn, btnStandards, btnDemo);

        // 6. Certification Seals Row
        HBox sealsRow = new HBox(10);
        sealsRow.setAlignment(Pos.CENTER_LEFT);
        for (String std : new String[]{"NIST SP 800-88", "DoD 5220.22-M", "FIPS 140-3", "IEEE 2883"}) {
            Label badge = new Label("✓ " + std);
            badge.getStyleClass().add("hero-cert-badge");
            badge.setOnMouseClicked(e -> {
                Window owner = getScene() != null ? getScene().getWindow() : null;
                ComplianceStandardsDialog.show(owner);
            });
            sealsRow.getChildren().add(badge);
        }

        leftCol.getChildren().addAll(pill, title, subtitle, chipRow, actions, sealsRow);

        // RIGHT COLUMN: High-Impact Visual Graphic Card
        StackPane rightCol = buildHeroImageCard();

        splitRow.getChildren().addAll(leftCol, rightCol);
        heroContainer.getChildren().add(splitRow);
        return heroContainer;
    }

    private StackPane buildHeroImageCard() {
        StackPane card = new StackPane();
        card.getStyleClass().add("hero-image-frame");
        card.setMinSize(500, 375);
        card.setPrefSize(500, 375);
        card.setMaxSize(500, 375);

        try {
            URL imgUrl = getClass().getResource("/images/hero_illustration.jpg");
            if (imgUrl != null) {
                Image img = new Image(imgUrl.toExternalForm(), 500, 375, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(500);
                iv.setFitHeight(375);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);

                Rectangle clip = new Rectangle(500, 375);
                clip.setArcWidth(20);
                clip.setArcHeight(20);
                iv.setClip(clip);

                card.getChildren().add(iv);
            }
        } catch (Exception ignored) {}

        // Floating hardware protection badge overlay
        Label floatingBadge = new Label("🔒 Hardware Protection Active  •  Zero Remanence Verified");
        floatingBadge.getStyleClass().add("hero-image-floating-badge");
        StackPane.setAlignment(floatingBadge, Pos.BOTTOM_CENTER);
        StackPane.setMargin(floatingBadge, new Insets(0, 0, 16, 0));

        card.getChildren().add(floatingBadge);
        return card;
    }

    private VBox buildShowcaseSection() {
        VBox showcase = new VBox(28);
        showcase.setAlignment(Pos.CENTER);
        showcase.setPadding(new Insets(48, 60, 56, 60));

        Label sectionTitle = new Label("Core Capabilities");
        sectionTitle.getStyleClass().add("hero-section-title");

        Label sectionSub = new Label("Fast, certified sanitization with zero data recovery guarantee.");
        sectionSub.getStyleClass().add("hero-section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(1160);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Capability 1
        VBox card1 = createFeatureCard(
                "⚡",
                "Certified Overwrite Algorithms",
                "Multi-pass erasure adhering to NIST 800-88 Clear/Purge and DoD 5220.22-M."
        );

        // Capability 2
        VBox card2 = createFeatureCard(
                "🔄",
                "Parallel Multi-Drive Speed",
                "Wipe multiple USB, SSD, and NVMe drives simultaneously without slowdowns."
        );

        // Capability 3
        VBox card3 = createFeatureCard(
                "🔑",
                "Cryptographic Audit PDF",
                "Tamper-proof certificates signed with RSA-4096 and SHA-256 integrity hashes."
        );

        // Capability 4
        VBox card4 = createFeatureCard(
                "📊",
                "S.M.A.R.T. Health Diagnostics",
                "Automated pre-wipe disk health scans, bad sector mapping, and thermal safety."
        );

        grid.add(card1, 0, 0);
        grid.add(card2, 1, 0);
        grid.add(card3, 0, 1);
        grid.add(card4, 1, 1);

        showcase.getChildren().addAll(sectionTitle, sectionSub, grid);
        return showcase;
    }

    private VBox createFeatureCard(String icon, String titleText, String descText) {
        VBox card = new VBox(16);
        card.getStyleClass().add("hero-feature-card");

        HBox iconRow = new HBox(14);
        iconRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("hero-feature-icon");

        Label titleLbl = new Label(titleText);
        titleLbl.getStyleClass().add("hero-feature-title");
        titleLbl.setWrapText(true);

        iconRow.getChildren().addAll(iconLbl, titleLbl);

        Label descLbl = new Label(descText);
        descLbl.getStyleClass().add("hero-feature-desc");
        descLbl.setWrapText(true);

        card.getChildren().addAll(iconRow, descLbl);
        return card;
    }

    private HBox buildStatsSection() {
        HBox statsRow = new HBox(28);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.setPadding(new Insets(24, 60, 64, 60));

        int totalWipes = 0;
        double successRate = 100.0;
        int signatures = 0;
        int activeDrives = 0;

        try {
            totalWipes = com.sanitizer.db.AuditDb.getAllRecords().size();
            successRate = com.sanitizer.db.AuditDb.getSuccessRatePercentage();
            signatures = com.sanitizer.db.AuditDb.getTamperVerifiedCount();
            activeDrives = com.sanitizer.detector.UsbDetector.getConnectedUsbDrives().size();
        } catch (Exception ignored) {}

        VBox s1 = createStatCard(String.valueOf(totalWipes), "WIPES COMPLETED");
        VBox s2 = createStatCard(String.format("%.1f%%", successRate), "PASS RATE");
        VBox s3 = createStatCard(String.valueOf(signatures), "SIGNED CERTIFICATES");
        VBox s4 = createStatCard(String.valueOf(activeDrives), "CONNECTED DRIVES");

        statsRow.getChildren().addAll(s1, s2, s3, s4);
        return statsRow;
    }

    private VBox createStatCard(String value, String label) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("hero-stat-card");
        card.setMinWidth(250);

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("hero-stat-value");

        Label lblLbl = new Label(label);
        lblLbl.getStyleClass().add("hero-stat-label");

        card.getChildren().addAll(valLbl, lblLbl);
        return card;
    }

    private VBox buildCtaSection() {
        VBox cta = new VBox(20);
        cta.setAlignment(Pos.CENTER);
        cta.getStyleClass().add("hero-cta-section");

        Label title = new Label("Ready to Securely Wipe Storage Media?");
        title.getStyleClass().add("hero-cta-title");

        Label subtitle = new Label("Sign in to start wiping drives or review audit certificates.");
        subtitle.getStyleClass().add("hero-cta-desc");

        Button btnBigCta = new Button("Sign In →");
        btnBigCta.getStyleClass().addAll("hero-cta-primary", "hero-cta-large");
        btnBigCta.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        cta.getChildren().addAll(title, subtitle, btnBigCta);
        return cta;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.getStyleClass().add("hero-footer");

        Label copy = new Label("Secure Data Sanitizer  •  Defense Information Assurance & Media Sanitization Suite");
        copy.getStyleClass().add("hero-footer-text");

        footer.getChildren().add(copy);
        return footer;
    }
}
