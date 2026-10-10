package com.sanitizer.gui.views;

import com.sanitizer.gui.views.settings.*;
import com.sanitizer.i18n.I18n;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * SettingsView acts solely as the master layout container assembling modular,
 * domain-specific settings cards into a responsive enterprise configuration dashboard.
 */
public class SettingsView {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(24);
    private final ConfigurationService configService;

    public SettingsView() {
        this(SystemConfigurationService.getInstance());
    }

    public SettingsView(ConfigurationService configService) {
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        buildUi();
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    private void buildUi() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(28));

        // ── Header ──────────────────────────────────────────────────────
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label(I18n.get("settings.title"));
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label(I18n.get("settings.subtitle"));
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // ── Domain Modular Cards ────────────────────────────────────────
        VBox cardA11y = new AccessibilitySettingsCard();
        VBox cardAutoLock = new AutoLockSecurityCard();
        VBox cardPolicyBuilder = new CustomPolicyCard(configService);
        VBox cardThermal = new ThermalPolicyCard(configService);
        VBox cardAlerts = new AlertDispatcherCard(configService);
        VBox cardAudioAlarms = new AudioAlarmsCard(configService);

        // ── Settings Grid (2x2) ─────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);

        VBox cardCrypto = new CryptoParametersCard();
        VBox cardShield = new HardwareSafetyCard();
        VBox cardDb = new DatabaseMaintenanceCard(configService);
        VBox cardSystem = new SystemInfoCard();

        grid.add(cardCrypto, 0, 0);
        grid.add(cardShield, 1, 0);
        grid.add(cardDb,     0, 1);
        grid.add(cardSystem, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // ── About Footer ────────────────────────────────────────────────
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("card");
        footer.setPadding(new Insets(16, 20, 16, 20));
        Label footerLabel = new Label("SecureErase Pro — Enterprise Data Sanitization Suite  |  " +
                "NIST SP 800-88 & DoD 5220.22-M Compliant  |  (c) 2025 SecureErase Technologies Pvt. Ltd.");
        footerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label versionBadge = new Label("v2.0.0 ENTERPRISE");
        versionBadge.getStyleClass().add("badge-info");
        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footer.getChildren().addAll(footerLabel, footerSpacer, versionBadge);

        rootContainer.getChildren().addAll(
                titleBox,
                cardA11y,
                cardAutoLock,
                cardPolicyBuilder,
                cardThermal,
                cardAlerts,
                cardAudioAlarms,
                grid,
                footer
        );
    }
}
