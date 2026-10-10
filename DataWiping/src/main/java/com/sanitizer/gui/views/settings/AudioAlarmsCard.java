package com.sanitizer.gui.views.settings;

import com.sanitizer.audio.AudioAlarmConfig;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Settings Card encapsulating Operator Bench Audio & Visual Alarms (Chimes + TTS + Screen Flash).
 */
public class AudioAlarmsCard extends VBox {

    private final ConfigurationService configService;

    public AudioAlarmsCard() {
        this(SystemConfigurationService.getInstance());
    }

    public AudioAlarmsCard(ConfigurationService configService) {
        super(18);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🔊 [OPERATOR BENCH ALARMS]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38BDF8; " +
                "-fx-background-color: rgba(56,189,248,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Audio & Visual Completion Alarms (Chimes + Text-To-Speech)");
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label("Configurable audible harmonic chimes, synthesized spoken voice announcements, and visual screen flash alerts for hardware wiping workstations.");
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        titleCol.getChildren().addAll(lblTitle, lblDesc);
        header.getChildren().addAll(iconBadge, titleCol);

        AudioAlarmConfig cfg = configService.getAudioAlarmConfig();
        if (cfg == null) {
            cfg = new AudioAlarmConfig();
        }

        // Settings Form Controls
        GridPane controlsGrid = new GridPane();
        controlsGrid.setHgap(24);
        controlsGrid.setVgap(16);

        // 1. Alarm Mode Selector
        Label lblMode = new Label("Alarm Notification Mode:");
        lblMode.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        ComboBox<AudioAlarmConfig.AlarmMode> cmbMode = new ComboBox<>();
        cmbMode.getItems().addAll(AudioAlarmConfig.AlarmMode.values());
        cmbMode.setValue(cfg.getAlarmMode());
        cmbMode.setStyle("-fx-pref-width: 320px;");

        // 2. Volume Slider
        Label lblVolume = new Label("Master Alarm Volume:");
        lblVolume.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Slider volumeSlider = new Slider(0.0, 1.0, cfg.getVolume());
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(false);
        volumeSlider.setPrefWidth(220);

        Label lblVolPercent = new Label((int)(cfg.getVolume() * 100) + "%");
        lblVolPercent.setStyle("-fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-pref-width: 50px;");
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblVolPercent.setText((int)(newVal.doubleValue() * 100) + "%");
        });

        HBox volBox = new HBox(12, volumeSlider, lblVolPercent);
        volBox.setAlignment(Pos.CENTER_LEFT);

        controlsGrid.add(lblMode, 0, 0);
        controlsGrid.add(cmbMode, 1, 0);
        controlsGrid.add(lblVolume, 0, 1);
        controlsGrid.add(volBox, 1, 1);

        // 3. Trigger Conditions & Events
        VBox triggersSection = new VBox(10);
        triggersSection.setPadding(new Insets(14));
        triggersSection.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        Label lblTriggers = new Label("Active Alert Trigger Conditions:");
        lblTriggers.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #e2e8f0;");

        CheckBox chkPass = new CheckBox("Announce completion of each wipe pass (e.g., 'Pass 1 of 3 complete on disk 2')");
        chkPass.setSelected(cfg.isEnablePassCompleteAlarm());
        chkPass.setStyle("-fx-text-fill: #cbd5e1;");

        CheckBox chkJob = new CheckBox("Announce final sanitization completion & cryptographic certificate generation");
        chkJob.setSelected(cfg.isEnableJobCompleteAlarm());
        chkJob.setStyle("-fx-text-fill: #cbd5e1;");

        CheckBox chkThermal = new CheckBox("Sound siren + voice warning when drive temperature exceeds thermal policy limit (>65°C)");
        chkThermal.setSelected(cfg.isEnableThermalAlertAlarm());
        chkThermal.setStyle("-fx-text-fill: #cbd5e1;");

        CheckBox chkFailure = new CheckBox("Sound alert siren on verification failure or premature drive disconnection");
        chkFailure.setSelected(cfg.isEnableVerificationFailureAlarm());
        chkFailure.setStyle("-fx-text-fill: #cbd5e1;");

        CheckBox chkVisual = new CheckBox("Flash screen border on critical operator alerts (for noisy laboratory environments)");
        chkVisual.setSelected(cfg.isVisualFlashEnabled());
        chkVisual.setStyle("-fx-text-fill: #cbd5e1;");

        triggersSection.getChildren().addAll(lblTriggers, chkPass, chkJob, chkThermal, chkFailure, chkVisual);

        // 4. Custom Spoken Voice Template
        VBox templateSection = new VBox(8);
        Label lblTemplate = new Label("Custom Spoken Voice Announcement Template:");
        lblTemplate.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        TextField txtTemplate = new TextField(cfg.getSpeechTemplate());
        txtTemplate.setStyle("-fx-pref-width: 600px; -fx-font-family: 'Monaco', 'Courier New', monospace; -fx-font-size: 11px;");

        Label lblTemplateHelp = new Label("Available Variables: {drive} = Target Drive  |  {standard} = Overwrite Standard  |  {entropy} = Residual Entropy  |  {status} = Pass/Fail");
        lblTemplateHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        templateSection.getChildren().addAll(lblTemplate, txtTemplate, lblTemplateHelp);

        // 5. Action Buttons
        HBox actionsRow = new HBox(14);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button btnTestAudio = new Button("▶ Test Audio Alarm & Speech");
        btnTestAudio.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6px;");
        btnTestAudio.setOnAction(e -> {
            AudioAlarmConfig testCfg = new AudioAlarmConfig();
            testCfg.setAlarmMode(cmbMode.getValue());
            testCfg.setVolume(volumeSlider.getValue());
            testCfg.setSpeechTemplate(txtTemplate.getText());
            configService.saveAudioAlarmConfig(testCfg);
            configService.testAudioAlarm(cmbMode.getValue());
            NavigationManager.getInstance().showNotification(
                    "Audio Alarm Preview",
                    "Triggered preview alarm for mode: " + (cmbMode.getValue() != null ? cmbMode.getValue().getDisplayName() : "Custom"),
                    ToastType.INFO
            );
        });

        Button btnSaveAudio = new Button("💾 Save Audio Preferences");
        btnSaveAudio.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 6px;");
        btnSaveAudio.setOnAction(e -> {
            AudioAlarmConfig toSave = new AudioAlarmConfig();
            toSave.setAlarmMode(cmbMode.getValue());
            toSave.setVolume(volumeSlider.getValue());
            toSave.setEnablePassCompleteAlarm(chkPass.isSelected());
            toSave.setEnableJobCompleteAlarm(chkJob.isSelected());
            toSave.setEnableThermalAlertAlarm(chkThermal.isSelected());
            toSave.setEnableVerificationFailureAlarm(chkFailure.isSelected());
            toSave.setVisualFlashEnabled(chkVisual.isSelected());
            toSave.setSpeechTemplate(txtTemplate.getText());
            configService.saveAudioAlarmConfig(toSave);

            NavigationManager.getInstance().showNotification(
                    "Audio Alarms Saved",
                    "Operator bench audio alarm settings updated and active.",
                    ToastType.SUCCESS
            );
        });

        actionsRow.getChildren().addAll(btnTestAudio, btnSaveAudio);

        getChildren().addAll(header, new Separator(), controlsGrid, triggersSection, templateSection, actionsRow);
    }
}
