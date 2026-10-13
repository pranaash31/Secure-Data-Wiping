package com.sanitizer.gui.views.settings;

import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.UserRole;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * Settings Card encapsulating Configurable Thermal Limits & Hardware Throttling Policies.
 */
public class ThermalPolicyCard extends VBox {

    private final ConfigurationService configService;
    private final Map<DeviceType, Spinner<Integer>> pauseSpinners = new EnumMap<>(DeviceType.class);
    private final Map<DeviceType, Spinner<Integer>> resumeSpinners = new EnumMap<>(DeviceType.class);
    private final Map<DeviceType, Label> policyStatusBadges = new EnumMap<>(DeviceType.class);

    public ThermalPolicyCard() {
        this(SystemConfigurationService.getInstance());
    }

    public ThermalPolicyCard(ConfigurationService configService) {
        super(18);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("[THERMAL POLICIES]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444; " +
                "-fx-background-color: rgba(239,68,68,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label(I18n.get("settings.thermal_title"));
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label(I18n.get("settings.thermal_sub"));
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        lblDesc.getStyleClass().add("settings-key-label");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label shieldStatus = new Label(I18n.get("settings.thermal_safeguard"));
        shieldStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #10B981; " +
                "-fx-background-color: #ECFDF5; -fx-background-radius: 12px; -fx-border-color: #A7F3D0; " +
                "-fx-border-radius: 12px; -fx-padding: 4 12;");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, shieldStatus);

        // 3 Device Type Policy Rows
        VBox devicePolicyList = new VBox(14);
        for (DeviceType type : DeviceType.values()) {
            devicePolicyList.getChildren().add(buildDevicePolicyRow(type));
        }

        // Actions Toolbar
        HBox actionsBar = new HBox(12);
        actionsBar.setAlignment(Pos.CENTER_LEFT);

        Button btnSave = new Button(I18n.get("settings.thermal_apply"));
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8 18;");
        btnSave.setOnAction(e -> handleSaveThermalPolicies());
        btnSave.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleSaveThermalPolicies(); });

        Button btnReset = new Button(I18n.get("settings.thermal_reset"));
        btnReset.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        btnReset.setOnAction(e -> handleResetThermalDefaults());
        btnReset.setOnKeyPressed(ev -> { if (ev.getCode() == KeyCode.ENTER) handleResetThermalDefaults(); });

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        Label lblHelp = new Label(I18n.get("settings.thermal_help"));
        lblHelp.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-style: italic;");
        lblHelp.getStyleClass().add("settings-key-label");

        actionsBar.getChildren().addAll(btnSave, btnReset, actionSpacer, lblHelp);

        getChildren().addAll(header, new Separator(), devicePolicyList, actionsBar);
    }

    private Node buildDevicePolicyRow(DeviceType type) {
        ThermalPolicy currentPolicy = configService.getThermalPolicy(type);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10px; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 10px; -fx-padding: 12 16;");

        // Device Icon & Title Column
        VBox devInfo = new VBox(3);
        devInfo.setPrefWidth(260);

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(type.getShortBadge());
        badge.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s; " +
                "-fx-background-color: %s; -fx-background-radius: 6px; -fx-padding: 2 6;", type.getAccentColor(), type.getBgColor()));

        Label name = new Label(type.getDisplayName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0F172A;");
        name.getStyleClass().add("settings-section-title");
        titleBox.getChildren().addAll(badge, name);

        Label desc = new Label(type.getDescription());
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        desc.getStyleClass().add("settings-key-label");
        devInfo.getChildren().addAll(titleBox, desc);

        // Auto-Pause Control
        VBox pauseBox = new VBox(3);
        pauseBox.setAlignment(Pos.CENTER_LEFT);
        Label lblPause = new Label("Auto-Pause Limit:");
        lblPause.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        Spinner<Integer> spPause = new Spinner<>(40, 95, currentPolicy.autoPauseCelsius(), 1);
        spPause.setEditable(true);
        spPause.setPrefWidth(90);
        spPause.setStyle("-fx-font-size: 12px;");
        pauseSpinners.put(type, spPause);
        pauseBox.getChildren().addAll(lblPause, spPause);

        // Safe-Resume Control
        VBox resumeBox = new VBox(3);
        resumeBox.setAlignment(Pos.CENTER_LEFT);
        Label lblResume = new Label("Safe Resume Cooldown:");
        lblResume.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        Spinner<Integer> spResume = new Spinner<>(35, 85, currentPolicy.resumeCelsius(), 1);
        spResume.setEditable(true);
        spResume.setPrefWidth(90);
        spResume.setStyle("-fx-font-size: 12px;");
        resumeSpinners.put(type, spResume);
        resumeBox.getChildren().addAll(lblResume, spResume);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label statusBadge = new Label(formatPolicyBadge(currentPolicy.autoPauseCelsius(), currentPolicy.resumeCelsius()));
        statusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B; " +
                "-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-padding: 6 12;");
        policyStatusBadges.put(type, statusBadge);

        row.getChildren().addAll(devInfo, pauseBox, resumeBox, spacer, statusBadge);
        return row;
    }

    private String formatPolicyBadge(int pause, int resume) {
        return String.format("Auto-Pause: %d°C  |  Resumes: %d°C", pause, resume);
    }

    private void handleSaveThermalPolicies() {
        boolean allValid = true;
        StringBuilder errorMsg = new StringBuilder();

        for (DeviceType type : DeviceType.values()) {
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);

            if (spPause == null || spResume == null) continue;

            int pause = spPause.getValue();
            int resume = spResume.getValue();

            if (!ThermalPolicy.isValid(pause, resume)) {
                allValid = false;
                errorMsg.append(String.format("• %s: Auto-pause (%d°C) must be at least 3°C above Resume threshold (%d°C).\n",
                        type.getDisplayName(), pause, resume));
            }
        }

        if (!allValid) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18n.get("dialog.thermal.invalid.title"));
            alert.setHeaderText(I18n.get("dialog.thermal.invalid.header"));
            alert.setContentText(errorMsg.toString());
            alert.showAndWait();
            return;
        }

        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.THERMAL_CONFIG)) {
            nav.showAccessDeniedDialog("Thermal Policy Configuration", "SUPERVISOR / CHIEF AUDITOR");
            return;
        }

        // Commit and apply all policies through ConfigurationService
        for (DeviceType type : DeviceType.values()) {
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);
            if (spPause != null && spResume != null) {
                int pause = spPause.getValue();
                int resume = spResume.getValue();
                configService.saveThermalPolicy(type, pause, resume, nav.getSessionContext());

                Label badge = policyStatusBadges.get(type);
                if (badge != null) {
                    badge.setText(formatPolicyBadge(pause, resume));
                }
            }
        }

        nav.showNotification(
                "Thermal Policies Saved",
                "Hardware temperature limits and throttling thresholds updated successfully.",
                ToastType.SUCCESS
        );
    }

    private void handleResetThermalDefaults() {
        var nav = NavigationManager.getInstance();
        configService.resetThermalPoliciesToDefaults(nav.getSessionContext());

        for (DeviceType type : DeviceType.values()) {
            ThermalPolicy def = configService.getThermalPolicy(type);
            Spinner<Integer> spPause = pauseSpinners.get(type);
            Spinner<Integer> spResume = resumeSpinners.get(type);
            Label badge = policyStatusBadges.get(type);

            if (spPause != null) spPause.getValueFactory().setValue(def.autoPauseCelsius());
            if (spResume != null) spResume.getValueFactory().setValue(def.resumeCelsius());
            if (badge != null) badge.setText(formatPolicyBadge(def.autoPauseCelsius(), def.resumeCelsius()));
        }

        nav.showNotification(
                "Defaults Restored",
                "Thermal throttling thresholds reset to factory default standards (USB: 55/42°C, NVMe: 70/52°C, HDD: 50/40°C).",
                ToastType.INFO
        );
    }
}
