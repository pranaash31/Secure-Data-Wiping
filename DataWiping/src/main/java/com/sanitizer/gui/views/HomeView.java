package com.sanitizer.gui.views;

import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.navigation.NavigationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class HomeView {

    private final VBox rootContainer = new VBox(20);

    public HomeView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(24));
        rootContainer.setStyle("-fx-background-color: #F8FAFC;");

        // --- Hero Banner ---
        VBox heroCard = new VBox(16);
        heroCard.getStyleClass().add("card");
        heroCard.setStyle("-fx-background-color: linear-gradient(to right, #0F172A, #1E3A8A); -fx-padding: 30;");

        HBox heroBadgeBox = new HBox(8);
        Label lblHeroBadge = new Label("GOVERNMENT DEFENSE PORTAL v1.0");
        lblHeroBadge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.15); -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12px;");
        heroBadgeBox.getChildren().add(lblHeroBadge);

        Label lblHeroTitle = new Label("Defense-Grade USB Data Sanitization System");
        lblHeroTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label lblHeroDesc = new Label(
                "Implements raw sector zero-fill (NIST SP 800-88) and 3-pass DoD 5220.22-M sanitization algorithms. " +
                "Integrates real-time hardware detection, SQLite tamper-evident logging, and SHA256withRSA PDF certificate issuance."
        );
        lblHeroDesc.setWrapText(true);
        lblHeroDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94A3B8; -fx-max-width: 800px;");

        HBox heroBtnBox = new HBox(12);
        Button btnWipeNow = new Button("🛡️  Start Data Wiping");
        btnWipeNow.getStyleClass().add("button-primary");
        btnWipeNow.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        btnWipeNow.setOnAction(e -> NavigationManager.getInstance().navigateTo("wiping"));

        Button btnViewLogs = new Button("📜  View Audit Logs");
        btnViewLogs.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        btnViewLogs.setOnAction(e -> NavigationManager.getInstance().navigateTo("audit"));

        heroBtnBox.getChildren().addAll(btnWipeNow, btnViewLogs);
        heroCard.getChildren().addAll(heroBadgeBox, lblHeroTitle, lblHeroDesc, heroBtnBox);

        // --- Hardware Status Widget ---
        VBox statusCard = new VBox(12);
        statusCard.getStyleClass().add("card");

        HBox statusHeader = new HBox(10);
        statusHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblStatusTitle = new Label("⚡ Hardware Diagnostics & Safety Shield");
        lblStatusTitle.getStyleClass().add("card-title");

        Label lblShieldBadge = new Label("🛡️ SAFETY SHIELD ACTIVE");
        lblShieldBadge.getStyleClass().add("badge-success");

        statusHeader.getChildren().addAll(lblStatusTitle, new Region(), lblShieldBadge);
        HBox.setHgrow(statusHeader.getChildren().get(1), Priority.ALWAYS);

        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();

        HBox statusGrid = new HBox(20);
        VBox stat1 = createStatItem("Connected Pen Drives", drives.size() + " Target(s) Detected");
        VBox stat2 = createStatItem("Primary System Protection", "disk0 & Internal NVMe/SSD Blocked");
        VBox stat3 = createStatItem("Raw Sector Protocol", "macOS Direct Block /dev/rdisk Access");
        statusGrid.getChildren().addAll(stat1, stat2, stat3);

        statusCard.getChildren().addAll(statusHeader, new Separator(), statusGrid);

        // --- Feature Cards Grid ---
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);

        VBox card1 = createFeatureCard(
                "NIST SP 800-88 Clear Standard",
                "Single Pass Zero-Fill (0x00)",
                "Provides baseline logical sanitization by overwriting all user-addressable storage locations with zero bytes.",
                "Standard 1-Pass"
        );

        VBox card2 = createFeatureCard(
                "DoD 5220.22-M Standard",
                "3-Pass Military Grade Overwrite",
                "Applies sequential overwrite passes: Pass 1 (0x00), Pass 2 (Pseudo-Random Data), and Pass 3 Zero Verification.",
                "Military 3-Pass"
        );

        VBox card3 = createFeatureCard(
                "Cryptographic PDF Seals",
                "SHA256withRSA 2048-bit Signature",
                "Generates tamper-proof audit certificates with embedded ZXing QR verification codes for complete chain of custody.",
                "Tamper-Proof"
        );

        grid.add(card1, 0, 0);
        grid.add(card2, 1, 0);
        grid.add(card3, 2, 0);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33.3);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33.3);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33.3);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        rootContainer.getChildren().addAll(heroCard, statusCard, grid);
    }

    private VBox createStatItem(String title, String value) {
        VBox box = new VBox(4);
        Label lblT = new Label(title);
        lblT.getStyleClass().add("card-subtitle");
        Label lblV = new Label(value);
        lblV.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        box.getChildren().addAll(lblT, lblV);
        return box;
    }

    private VBox createFeatureCard(String title, String subtitle, String description, String badgeText) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        HBox topBox = new HBox(8);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label lblBadge = new Label(badgeText);
        lblBadge.getStyleClass().add("badge-info");

        topBox.getChildren().add(lblBadge);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");

        Label lblSub = new Label(subtitle);
        lblSub.getStyleClass().add("card-subtitle");

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        card.getChildren().addAll(topBox, lblTitle, lblSub, lblDesc);
        return card;
    }
}
