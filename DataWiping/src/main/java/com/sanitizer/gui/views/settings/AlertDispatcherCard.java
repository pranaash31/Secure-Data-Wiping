package com.sanitizer.gui.views.settings;

import com.sanitizer.alert.AlertConfig;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.UserRole;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings Card encapsulating Real-Time Email & Webhook Alerts.
 */
public class AlertDispatcherCard extends VBox {

    private final ConfigurationService configService;

    public AlertDispatcherCard() {
        this(SystemConfigurationService.getInstance());
    }

    public AlertDispatcherCard(ConfigurationService configService) {
        super(18);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("📧 [REAL-TIME ALERTS]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981; " +
                "-fx-background-color: rgba(16,185,129,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Real-Time Email & Webhook Alerts");
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label("Configure automated Slack, Discord, MS Teams, or SMTP email alerts for critical operations");
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label activeBadge = new Label("ACTIVE DISPATCH");
        activeBadge.getStyleClass().add("badge-success");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, activeBadge);

        AlertConfig config = configService.getAlertConfig();

        // 2-Column Grid for Webhook & SMTP Configuration
        GridPane alertGrid = new GridPane();
        alertGrid.setHgap(24);
        alertGrid.setVgap(16);

        // Column 1: Webhook Integration
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14;");

        CheckBox chkWebhook = new CheckBox("Enable Webhook Alerts");
        chkWebhook.setSelected(config.isWebhookEnabled());
        chkWebhook.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label lblPlatform = new Label("Webhook Platform / Format:");
        lblPlatform.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        ComboBox<AlertConfig.WebhookPlatform> cmbPlatform = new ComboBox<>();
        cmbPlatform.getItems().addAll(AlertConfig.WebhookPlatform.values());
        cmbPlatform.setValue(config.getWebhookPlatform());
        cmbPlatform.setMaxWidth(Double.MAX_VALUE);

        Label lblUrl = new Label("Webhook Target URL:");
        lblUrl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        TextField txtWebhookUrl = new TextField(config.getWebhookUrl());
        txtWebhookUrl.setPromptText("https://hooks.slack.com/services/... or Discord Webhook URL");

        webhookBox.getChildren().addAll(chkWebhook, lblPlatform, cmbPlatform, lblUrl, txtWebhookUrl);

        // Column 2: SMTP Email Integration
        VBox smtpBox = new VBox(10);
        smtpBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14;");

        CheckBox chkSmtp = new CheckBox("Enable SMTP Email Alerts");
        chkSmtp.setSelected(config.isSmtpEnabled());
        chkSmtp.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        HBox hostPortBox = new HBox(8);
        TextField txtHost = new TextField(config.getSmtpHost());
        txtHost.setPromptText("smtp.gmail.com");
        HBox.setHgrow(txtHost, Priority.ALWAYS);
        Spinner<Integer> spnPort = new Spinner<>(1, 65535, config.getSmtpPort());
        spnPort.setPrefWidth(90);
        spnPort.setEditable(true);
        hostPortBox.getChildren().addAll(txtHost, spnPort);

        HBox authBox = new HBox(8);
        TextField txtUser = new TextField(config.getSmtpUsername());
        txtUser.setPromptText("SMTP Username / Email");
        HBox.setHgrow(txtUser, Priority.ALWAYS);
        PasswordField txtPass = new PasswordField();
        txtPass.setText(config.getSmtpPassword());
        txtPass.setPromptText("App Password / Token");
        HBox.setHgrow(txtPass, Priority.ALWAYS);
        authBox.getChildren().addAll(txtUser, txtPass);

        TextField txtRecipients = new TextField(config.getRecipientEmails());
        txtRecipients.setPromptText("soc-alerts@company.com, admin@enterprise.com");

        smtpBox.getChildren().addAll(
                chkSmtp,
                new Label("SMTP Host & Port:") {{ setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;"); }},
                hostPortBox,
                new Label("Credentials & Auth:") {{ setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;"); }},
                authBox,
                new Label("Recipient Email(s):") {{ setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;"); }},
                txtRecipients
        );

        alertGrid.add(webhookBox, 0, 0);
        alertGrid.add(smtpBox, 1, 0);
        ColumnConstraints ac1 = new ColumnConstraints();
        ac1.setPercentWidth(50);
        ColumnConstraints ac2 = new ColumnConstraints();
        ac2.setPercentWidth(50);
        alertGrid.getColumnConstraints().addAll(ac1, ac2);

        // Event Triggers Checkboxes
        VBox triggersCard = new VBox(8);
        triggersCard.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 12 16; -fx-background-radius: 6px;");
        Label lblTriggers = new Label("AUTOMATED EVENT NOTIFICATION TRIGGERS:");
        lblTriggers.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        FlowPane triggerFlow = new FlowPane(16, 8);
        CheckBox chkWipe = new CheckBox("Single Wipe Completed (Success / Fail)");
        chkWipe.setSelected(config.isNotifyOnWipeComplete());
        CheckBox chkBatch = new CheckBox("Multi-Drive Batch Wipe Completed");
        chkBatch.setSelected(config.isNotifyOnBatchWipeComplete());
        CheckBox chkThermal = new CheckBox("Thermal Auto-Pause Safeguard (>55°C)");
        chkThermal.setSelected(config.isNotifyOnThermalPause());
        CheckBox chkShield = new CheckBox("System Disk (disk0) Shield Intercept");
        chkShield.setSelected(config.isNotifyOnShieldBlock());
        CheckBox chkQuarantine = new CheckBox("Hardware Quarantine & Physical Destruction Order");
        chkQuarantine.setSelected(config.isNotifyOnQuarantineDefect());

        triggerFlow.getChildren().addAll(chkWipe, chkBatch, chkThermal, chkShield, chkQuarantine);
        triggersCard.getChildren().addAll(lblTriggers, triggerFlow);

        // Action Toolbar
        HBox actionToolbar = new HBox(12);
        actionToolbar.setAlignment(Pos.CENTER_RIGHT);

        Button btnTestAlert = new Button("🧪 Send Test Alert");
        btnTestAlert.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnTestAlert.setOnAction(e -> {
            AlertConfig currentDraft = buildAlertConfigFromForm(
                    chkWebhook.isSelected(), cmbPlatform.getValue(), txtWebhookUrl.getText(),
                    chkSmtp.isSelected(), txtHost.getText(), spnPort.getValue(),
                    txtUser.getText(), txtPass.getText(), txtRecipients.getText(),
                    chkWipe.isSelected(), chkBatch.isSelected(), chkThermal.isSelected(),
                    chkShield.isSelected(), chkQuarantine.isSelected()
            );
            btnTestAlert.setDisable(true);
            btnTestAlert.setText("Sending Test...");
            configService.sendTestAlertAsync(currentDraft).thenAcceptAsync(ok -> {
                btnTestAlert.setDisable(false);
                btnTestAlert.setText("🧪 Send Test Alert");
                NavigationManager.getInstance().showNotification(
                        ok ? "Test Alert Dispatched" : "Test Alert Failed",
                        ok ? "Dispatched diagnostic test alert to configured channels." : "Failed to dispatch test notification.",
                        ok ? ToastType.SUCCESS : ToastType.ERROR
                );
            }, Platform::runLater);
        });

        Button btnSaveAlerts = new Button("💾 Save Alert Configuration");
        btnSaveAlerts.getStyleClass().add("button-primary");
        btnSaveAlerts.setOnAction(e -> {
            var nav = NavigationManager.getInstance();
            if (!nav.hasPermission(UserRole.Permission.ALERT_CONFIG)) {
                nav.showAccessDeniedDialog("Real-Time Alert Dispatcher", "SUPERVISOR / CHIEF AUDITOR");
                return;
            }
            AlertConfig toSave = buildAlertConfigFromForm(
                    chkWebhook.isSelected(), cmbPlatform.getValue(), txtWebhookUrl.getText(),
                    chkSmtp.isSelected(), txtHost.getText(), spnPort.getValue(),
                    txtUser.getText(), txtPass.getText(), txtRecipients.getText(),
                    chkWipe.isSelected(), chkBatch.isSelected(), chkThermal.isSelected(),
                    chkShield.isSelected(), chkQuarantine.isSelected()
            );
            configService.saveAlertConfig(toSave, nav.getSessionContext());
            nav.showNotification(
                    "Alert Settings Saved",
                    "Real-time email and webhook alert settings updated successfully.",
                    ToastType.SUCCESS
            );
        });

        actionToolbar.getChildren().addAll(btnTestAlert, btnSaveAlerts);

        getChildren().addAll(header, new Separator(), alertGrid, triggersCard, actionToolbar);
    }

    private AlertConfig buildAlertConfigFromForm(
            boolean whEnabled, AlertConfig.WebhookPlatform platform, String whUrl,
            boolean smtpEnabled, String smtpHost, int port,
            String user, String pass, String recipients,
            boolean onWipe, boolean onBatch, boolean onThermal, boolean onShield, boolean onQuarantine
    ) {
        AlertConfig c = new AlertConfig();
        c.setWebhookEnabled(whEnabled);
        c.setWebhookPlatform(platform);
        c.setWebhookUrl(whUrl);
        c.setSmtpEnabled(smtpEnabled);
        c.setSmtpHost(smtpHost);
        c.setSmtpPort(port);
        c.setSmtpUsername(user);
        c.setSmtpPassword(pass);
        c.setRecipientEmails(recipients);
        c.setNotifyOnWipeComplete(onWipe);
        c.setNotifyOnBatchWipeComplete(onBatch);
        c.setNotifyOnThermalPause(onThermal);
        c.setNotifyOnShieldBlock(onShield);
        c.setNotifyOnQuarantineDefect(onQuarantine);
        return c;
    }
}
