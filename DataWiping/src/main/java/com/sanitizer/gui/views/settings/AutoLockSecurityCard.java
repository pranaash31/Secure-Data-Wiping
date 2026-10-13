package com.sanitizer.gui.views.settings;

import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.UserRole;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Settings Card encapsulating Session Auto-Lock & FISMA/HIPAA Security Controls.
 */
public class AutoLockSecurityCard extends VBox {

    private final ConfigurationService configService;

    public AutoLockSecurityCard() {
        this(SystemConfigurationService.getInstance());
    }

    public AutoLockSecurityCard(ConfigurationService configService) {
        super(18);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🔒 [SESSION AUTO-LOCK]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Session Auto-Lock & FISMA/HIPAA Security Controls");
        lblTitle.getStyleClass().add("settings-section-title");
        Label lblDesc = new Label("Configurable inactivity timeout locking (FISMA High & HIPAA Security Rule 45 CFR § 164.312 requirement)");
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        titleCol.getChildren().addAll(lblTitle, lblDesc);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label statusBadge = new Label("COMPLIANCE ENFORCED");
        statusBadge.getStyleClass().add("badge-warning");

        header.getChildren().addAll(iconBadge, titleCol, headerSpacer, statusBadge);

        GridPane lockGrid = new GridPane();
        lockGrid.setHgap(20);
        lockGrid.setVgap(14);

        // Timeout Selector
        VBox timeoutBox = new VBox(6);
        Label lblTimeout = new Label("Inactivity Auto-Lock Timeout:");
        lblTimeout.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        ComboBox<Integer> cmbTimeout = new ComboBox<>();
        cmbTimeout.getItems().addAll(60, 300, 900, 1800, 0);
        cmbTimeout.setValue(configService.getAutoLockTimeoutSeconds());
        cmbTimeout.setMaxWidth(Double.MAX_VALUE);
        cmbTimeout.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer sec) {
                if (sec == null) return "";
                return switch (sec) {
                    case 60   -> "1 Minute (Strict / Demo)";
                    case 300  -> "5 Minutes (Standard FISMA Default)";
                    case 900  -> "15 Minutes (HIPAA Standard)";
                    case 1800 -> "30 Minutes (Extended Policy)";
                    case 0    -> "Disabled (Testing Mode Only)";
                    default   -> sec + " Seconds";
                };
            }
            @Override
            public Integer fromString(String s) { return null; }
        });

        var nav = NavigationManager.getInstance();
        boolean canEdit = nav.hasPermission(UserRole.Permission.SETTINGS_MODIFY);
        cmbTimeout.setDisable(!canEdit);

        cmbTimeout.setOnAction(e -> {
            Integer val = cmbTimeout.getValue();
            if (val != null) {
                configService.saveAutoLockTimeout(val, nav.getSessionContext());
                nav.showNotification("Auto-Lock Policy Updated",
                        "Inactivity timeout set to: " + cmbTimeout.getConverter().toString(val),
                        ToastType.SUCCESS);
            }
        });
        timeoutBox.getChildren().addAll(lblTimeout, cmbTimeout);

        // Manual Lock & Test Controls
        VBox actionBox = new VBox(6);
        Label lblTest = new Label("Security Lock Actions:");
        lblTest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        HBox btnRow = new HBox(10);
        Button btnLockNow = new Button("🔒 Lock Session Now (Ctrl+Alt+L)");
        btnLockNow.getStyleClass().add("button-primary");
        btnLockNow.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 16;");
        btnLockNow.setOnAction(e -> nav.lockSessionNow());

        btnRow.getChildren().add(btnLockNow);
        actionBox.getChildren().addAll(lblTest, btnRow);

        lockGrid.add(timeoutBox, 0, 0);
        lockGrid.add(actionBox, 1, 0);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        lockGrid.getColumnConstraints().addAll(c1, c2);

        getChildren().addAll(header, new Separator(), lockGrid);
    }
}
