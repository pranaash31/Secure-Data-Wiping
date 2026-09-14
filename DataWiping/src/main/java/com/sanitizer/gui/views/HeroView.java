package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;

public class HeroView extends ScrollPane {

    private Runnable onLoginClicked;

    public HeroView(Runnable onLoginClicked) {
        this.onLoginClicked = onLoginClicked;
        getStyleClass().add("hero-root");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        buildUI();
    }

    private void buildUI() {
        VBox pageContainer = new VBox(0);
        pageContainer.setStyle("-fx-background-color: #F8FAFC;");

        // 1. TOP HEADER NAVIGATION BAR
        HBox topHeader = buildTopHeader();

        // 2. HERO MAIN SECTION
        VBox heroSection = buildHeroSection();

        // 3. PRODUCT ADVERTISEMENT SHOWCASE GRID
        VBox showcaseSection = buildShowcaseSection();

        // 4. ENTERPRISE STATS COUNTER SECTION
        HBox statsSection = buildStatsSection();

        // 5. BOTTOM CALL TO ACTION (CTA) FOOTER
        VBox ctaSection = buildCtaSection();

        // 6. FOOTER
        HBox footer = buildFooter();

        pageContainer.getChildren().addAll(topHeader, heroSection, showcaseSection, statsSection, ctaSection, footer);
        setContent(pageContainer);
    }

    private HBox buildTopHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("hero-header");

        // Brand logo area
        HBox brandBox = new HBox(10);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        Label shieldIcon = new Label("[S]");
        shieldIcon.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: #EFF6FF; -fx-background-radius: 8px; -fx-padding: 4 10;");

        Label brandName = new Label("SecureErase Pro");
        brandName.getStyleClass().add("hero-brand-name");

        Label brandTag = new Label("ENTERPRISE v2.0");
        brandTag.getStyleClass().add("hero-brand-tag");

        brandBox.getChildren().addAll(shieldIcon, brandName, brandTag);

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        // Header Navigation Links
        HBox navLinks = new HBox(12);
        navLinks.setAlignment(Pos.CENTER);

        Button btnFeat = new Button("Features");
        btnFeat.getStyleClass().add("hero-nav-link");

        Button btnCompliance = new Button("Compliance");
        btnCompliance.getStyleClass().add("hero-nav-link");

        Button btnArch = new Button("Architecture");
        btnArch.getStyleClass().add("hero-nav-link");

        Button btnSecurity = new Button("PKI Security");
        btnSecurity.getStyleClass().add("hero-nav-link");

        navLinks.getChildren().addAll(btnFeat, btnCompliance, btnArch, btnSecurity);

        // Access Portal Button
        Button btnPortal = new Button("Access Officer Portal");
        btnPortal.getStyleClass().add("button-primary");
        btnPortal.setStyle("-fx-font-size: 13px; -fx-padding: 8 18;");
        btnPortal.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        header.getChildren().addAll(brandBox, navSpacer, navLinks, btnPortal);
        return header;
    }

    private VBox buildHeroSection() {
        VBox hero = new VBox(24);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(60, 40, 50, 40));

        // Pill Tag
        Label pill = new Label("🛡️ MILITARY-GRADE HARDWARE DATA SANITIZATION PLATFORM");
        pill.getStyleClass().add("hero-badge-pill");

        // Headline
        Label title = new Label("Defense-Grade Data Destruction & Compliance Automation");
        title.getStyleClass().add("hero-main-title");
        title.setWrapText(true);
        title.setAlignment(Pos.CENTER);

        // Subtitle
        Label subtitle = new Label(
                "Protect your organization with sector-level physical sanitization, cryptographic PKI audit trails, " +
                "and zero-recovery verification for government, healthcare, and enterprise storage assets."
        );
        subtitle.getStyleClass().add("hero-main-subtitle");
        subtitle.setMaxWidth(820);
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        // Action Buttons Row
        HBox actions = new HBox(16);
        actions.setAlignment(Pos.CENTER);

        Button btnLoginPrimary = new Button("🚀 Access Officer Portal");
        btnLoginPrimary.getStyleClass().add("hero-cta-primary");
        btnLoginPrimary.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        Button btnSpecs = new Button("📄 Explore System Specs");
        btnSpecs.getStyleClass().add("hero-cta-secondary");

        actions.getChildren().addAll(btnLoginPrimary, btnSpecs);

        // Standard Badges
        HBox standards = new HBox(14);
        standards.setAlignment(Pos.CENTER);

        for (String std : new String[]{"NIST 800-88 Rev 1", "DoD 5220.22-M", "FIPS 140-3", "Common Criteria EAL4+", "GDPR & HIPAA"}) {
            Label badge = new Label(std);
            badge.getStyleClass().add("badge-info");
            badge.setStyle("-fx-font-size: 11px; -fx-padding: 5 14;");
            standards.getChildren().add(badge);
        }

        hero.getChildren().addAll(pill, title, subtitle, actions, standards);
        return hero;
    }

    private VBox buildShowcaseSection() {
        VBox showcase = new VBox(28);
        showcase.setAlignment(Pos.CENTER);
        showcase.setPadding(new Insets(40, 60, 50, 60));

        Label sectionTitle = new Label("Platform Enterprise Features");
        sectionTitle.getStyleClass().add("section-label");

        Label sectionSub = new Label("Everything you need for zero-risk data sanitization & compliance readiness");
        sectionSub.getStyleClass().add("section-sublabel");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(24);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(1100);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Feature 1
        VBox card1 = createFeatureCard(
                "⚡",
                "NIST 800-88 & DoD 5220.22-M Compliance",
                "Automated multi-pass sector overwrite algorithms with cryptographically verified Zero-Pass validation to satisfy federal data destruction standards."
        );

        // Feature 2
        VBox card2 = createFeatureCard(
                "🔄",
                "Concurrent Multi-Drive Parallel Wiping",
                "Simultaneously wipe up to 32 NVMe, SSD, and USB drives in parallel without throughput degradation or bus bottlenecking."
        );

        // Feature 3
        VBox card3 = createFeatureCard(
                "🔑",
                "Tamper-Proof PKI & Cryptographic Certificates",
                "Every sanitization generates an immutable PDF certificate cryptographically signed with RSA-4096 keys and SHA-256 integrity hashes."
        );

        // Feature 4
        VBox card4 = createFeatureCard(
                "📊",
                "SMART Hardware Diagnostics & Bad Sector Mapping",
                "Inspect drive health, bad sector maps, temperature gauges, and firmware serials prior to sanitization execution."
        );

        grid.add(card1, 0, 0);
        grid.add(card2, 1, 0);
        grid.add(card3, 0, 1);
        grid.add(card4, 1, 1);

        showcase.getChildren().addAll(sectionTitle, sectionSub, grid);
        return showcase;
    }

    private VBox createFeatureCard(String icon, String titleText, String descText) {
        VBox card = new VBox(14);
        card.getStyleClass().add("hero-feature-card");

        HBox iconRow = new HBox(12);
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
        HBox statsRow = new HBox(24);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.setPadding(new Insets(20, 60, 50, 60));

        int totalWipes = com.sanitizer.db.AuditDb.getAllRecords().size();
        double successRate = com.sanitizer.db.AuditDb.getSuccessRatePercentage();
        int signatures = com.sanitizer.db.AuditDb.getTamperVerifiedCount();
        int activeDrives = com.sanitizer.detector.UsbDetector.getConnectedUsbDrives().size();

        VBox s1 = createStatCard(String.valueOf(totalWipes) + " OPS", "VERIFIED AUDIT LOGS");
        VBox s2 = createStatCard(String.format("%.1f%%", successRate), "ZERO-RECOVERY GUARANTEE");
        VBox s3 = createStatCard(String.valueOf(signatures) + " SEALS", "VALIDATED RSA-4096 SIGNATURES");
        VBox s4 = createStatCard(String.valueOf(activeDrives) + " DRIVES", "HARDWARE TARGETS ATTACHED");

        statsRow.getChildren().addAll(s1, s2, s3, s4);
        return statsRow;
    }

    private VBox createStatCard(String value, String label) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("hero-stat-card");
        card.setMinWidth(220);

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
        cta.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-width: 1 0 1 0;");
        cta.setPadding(new Insets(48, 40, 48, 40));

        Label title = new Label("Ready to Sanitize Enterprise Data Assets?");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subtitle = new Label("Log in to the Security Officer Portal to start single or batch drive wiping immediately.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Button btnBigCta = new Button("Enter Officer Authentication Portal →");
        btnBigCta.getStyleClass().add("hero-cta-primary");
        btnBigCta.setStyle("-fx-font-size: 16px; -fx-padding: 16 36;");
        btnBigCta.setOnAction(e -> {
            if (onLoginClicked != null) onLoginClicked.run();
        });

        cta.getChildren().addAll(title, subtitle, btnBigCta);
        return cta;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 24 40;");

        Label copy = new Label("© 2025 SecureErase Pro Enterprise Suite. Trusted by Defense & Government Agencies Worldwide.");
        copy.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        footer.getChildren().add(copy);
        return footer;
    }
}
