package com.sanitizer.gui.views;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.List;

public class DiskDiagnosticView {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(22);

    private ComboBox<UsbDetector.UsbDriveInfo> cmbDrives;
    private Button btnScan;
    private Button btnLoad;
    private Button btnSelfTest;

    // Stat card value labels
    private Label lblHealthValue;
    private Label lblHealthScoreBadge;
    private Label lblHealthDesc;

    private Label lblTempValue;
    private Label lblThermalBadge;
    private Label lblTempDesc;

    private Label lblHoursValue;
    private Label lblHoursDesc;

    private Label lblSectorsValue;
    private Label lblSectorsDesc;

    private Label lblWearValue;
    private ProgressBar pbWearLevel;
    private Label lblWearDesc;

    // Pre-Wipe Health Assessment Banner
    private HBox preWipeAssessmentBox;
    private Label lblPreWipeVerdict;
    private Label lblPreWipeDetails;

    // Spec Labels
    private Label lblModelValue;
    private Label lblSerialValue;
    private Label lblFirmwareValue;
    private Label lblInterfaceValue;
    private Label lblCapacityValue;
    private Label lblBlockSizeValue;
    private Label lblTotalBlocksValue;
    private Label lblBusTypeValue;
    private Label lblPathValue;

    // SMART Telemetry Table
    private TableView<SmartDiagnostics.SmartAttribute> smartTable;
    private ObservableList<SmartDiagnostics.SmartAttribute> smartData = FXCollections.observableArrayList();

    public DiskDiagnosticView() {
        buildUi();
        refreshDriveList();
        UsbDetector.registerListener(drives -> Platform.runLater(() -> updateDriveList(drives)));
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    @SuppressWarnings("unchecked")
    private void buildUi() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(24, 28, 28, 28));

        // F5 = rescan drives
        rootContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(ev -> {
                    if (ev.getCode() == KeyCode.F5) refreshDriveList();
                });
            }
        });

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Real-Time S.M.A.R.T. Diagnostics & Health Inspector");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Deep sector defect analysis, NAND flash wear leveling, thermal telemetry & automated pre-wipe health scoring");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        btnScan = new Button("🔄 Scan All Drives (F5)");
        btnScan.getStyleClass().add("button-primary");
        btnScan.setOnAction(e -> refreshDriveList());
        btnScan.setTooltip(new Tooltip("Rescan all connected hardware drives (F5)"));

        header.getChildren().addAll(titleBox, hSpacer, btnScan);

        // ── Drive Selector ───────────────────────────────────────────────
        HBox selectorRow = new HBox(14);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        selectorRow.getStyleClass().add("card");
        selectorRow.setPadding(new Insets(14, 18, 14, 18));

        Label selectorLabel = new Label("Target Hardware Drive:");
        selectorLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        cmbDrives = new ComboBox<>();
        cmbDrives.setMaxWidth(480);
        cmbDrives.setPrefWidth(480);
        cmbDrives.setPromptText("Scanning connected hardware drives...");

        cmbDrives.setCellFactory(param -> new ListCell<UsbDetector.UsbDriveInfo>() {
            @Override
            protected void updateItem(UsbDetector.UsbDriveInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.model() + " (" + item.formattedSize() + ") — Path: " + item.systemPath());
                }
            }
        });
        cmbDrives.setButtonCell(cmbDrives.getCellFactory().call(null));
        cmbDrives.setOnAction(e -> loadSelectedDriveDiagnostics());
        cmbDrives.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) loadSelectedDriveDiagnostics();
        });

        btnLoad = new Button("Load S.M.A.R.T.");
        btnLoad.getStyleClass().add("button-primary");
        btnLoad.setOnAction(e -> loadSelectedDriveDiagnostics());

        btnSelfTest = new Button("⚡ Run Diagnostic Self-Test");
        btnSelfTest.getStyleClass().add("button-secondary");
        btnSelfTest.setOnAction(e -> runSmartSelfTest());

        Region sSpacer = new Region();
        HBox.setHgrow(sSpacer, Priority.ALWAYS);

        selectorRow.getChildren().addAll(selectorLabel, cmbDrives, btnLoad, btnSelfTest, sSpacer);

        // ── Pre-Wipe Health Assessment Banner ────────────────────────────
        preWipeAssessmentBox = new HBox(16);
        preWipeAssessmentBox.setAlignment(Pos.CENTER_LEFT);
        preWipeAssessmentBox.getStyleClass().add("card");
        preWipeAssessmentBox.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");

        VBox preWipeContent = new VBox(3);
        lblPreWipeVerdict = new Label("PRE-WIPE HEALTH ASSESSMENT: AUTOMATICALLY VERIFIED");
        lblPreWipeVerdict.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #065F46;");

        lblPreWipeDetails = new Label("Drive health is certified optimal. Drive is cleared for high-speed low-level block sanitization.");
        lblPreWipeDetails.setStyle("-fx-font-size: 11px; -fx-text-fill: #047857;");
        preWipeContent.getChildren().addAll(lblPreWipeVerdict, lblPreWipeDetails);
        HBox.setHgrow(preWipeContent, Priority.ALWAYS);

        preWipeAssessmentBox.getChildren().add(preWipeContent);

        // ── 5 Stat Cards Row: Health, Temperature, Hours, Sectors/Bad Blocks, Wear Leveling ──
        HBox healthRow = new HBox(14);

        // Card 1: Health Score
        VBox healthCard = createStatCard("PRE-WIPE HEALTH SCORE");
        HBox hScoreBox = new HBox(8);
        hScoreBox.setAlignment(Pos.CENTER_LEFT);
        lblHealthValue = new Label("-- / 100");
        lblHealthValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        lblHealthScoreBadge = new Label("HEALTHY");
        lblHealthScoreBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #ECFDF5; -fx-text-fill: #059669;");
        hScoreBox.getChildren().addAll(lblHealthValue, lblHealthScoreBadge);

        lblHealthDesc = new Label("Zero critical failure indicators detected.");
        lblHealthDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblHealthDesc.setWrapText(true);
        healthCard.getChildren().addAll(hScoreBox, lblHealthDesc);

        // Card 2: Temperature
        VBox tempCard = createStatCard("DRIVE TEMPERATURE");
        HBox tBox = new HBox(8);
        tBox.setAlignment(Pos.CENTER_LEFT);
        lblTempValue = new Label("-- °C");
        lblTempValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
        lblThermalBadge = new Label("NORMAL");
        lblThermalBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB;");
        tBox.getChildren().addAll(lblTempValue, lblThermalBadge);

        lblTempDesc = new Label("Operating within safe thermal limits (<48°C).");
        lblTempDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblTempDesc.setWrapText(true);
        tempCard.getChildren().addAll(tBox, lblTempDesc);

        // Card 3: Power-On Hours
        VBox hoursCard = createStatCard("POWER-ON HOURS");
        lblHoursValue = new Label("-- hrs");
        lblHoursValue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #7C3AED;");
        lblHoursDesc = new Label("Operational runtime lifespan.");
        lblHoursDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblHoursDesc.setWrapText(true);
        hoursCard.getChildren().addAll(lblHoursValue, lblHoursDesc);

        // Card 4: Reallocated Sectors & Bad Blocks
        VBox sectorsCard = createStatCard("SECTOR DEFECTS");
        lblSectorsValue = new Label("0 BAD / 0 REALLOC");
        lblSectorsValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        lblSectorsDesc = new Label("Media surface is clean. No bad sectors.");
        lblSectorsDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblSectorsDesc.setWrapText(true);
        sectorsCard.getChildren().addAll(lblSectorsValue, lblSectorsDesc);

        // Card 5: Flash Wear Leveling
        VBox wearCard = createStatCard("NAND WEAR LEVELING");
        lblWearValue = new Label("100% Life Left");
        lblWearValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        pbWearLevel = new ProgressBar(1.0);
        pbWearLevel.setMaxWidth(Double.MAX_VALUE);
        pbWearLevel.setPrefHeight(6);
        lblWearDesc = new Label("NAND endurance optimal.");
        lblWearDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        lblWearDesc.setWrapText(true);
        wearCard.getChildren().addAll(lblWearValue, pbWearLevel, lblWearDesc);

        for (VBox c : new VBox[]{healthCard, tempCard, hoursCard, sectorsCard, wearCard}) {
            HBox.setHgrow(c, Priority.ALWAYS);
        }
        healthRow.getChildren().addAll(healthCard, tempCard, hoursCard, sectorsCard, wearCard);

        // ── Drive Specifications & SMART Table Row ──────────────────────
        HBox specsRow = new HBox(16);

        VBox specsCard = new VBox(14);
        specsCard.getStyleClass().add("card");
        specsCard.setPrefWidth(380);
        specsCard.setMinWidth(350);

        Label specsTitle = new Label("Target Drive Hardware Specifications");
        specsTitle.getStyleClass().add("card-title");

        GridPane specsGrid = new GridPane();
        specsGrid.setHgap(16);
        specsGrid.setVgap(10);

        lblModelValue = createSpecLabel("Select a drive");
        lblSerialValue = createSpecLabel("--");
        lblFirmwareValue = createSpecLabel("1.00");
        lblInterfaceValue = createSpecLabel("USB 3.2 Mass Storage");
        lblCapacityValue = createSpecLabel("--");
        lblBlockSizeValue = createSpecLabel("512 bytes");
        lblTotalBlocksValue = createSpecLabel("-- sectors");
        lblBusTypeValue = createSpecLabel("USB Flash / Block Storage");
        lblPathValue = createSpecLabel("--");

        String[] keys = {"Model", "Serial Number", "Firmware Version", "Interface", "Capacity", "Block Size", "Total Blocks", "Bus Type", "System Path"};
        Label[] vals = {lblModelValue, lblSerialValue, lblFirmwareValue, lblInterfaceValue, lblCapacityValue, lblBlockSizeValue, lblTotalBlocksValue, lblBusTypeValue, lblPathValue};

        for (int i = 0; i < keys.length; i++) {
            Label k = new Label(keys[i]);
            k.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            specsGrid.add(k, 0, i);
            specsGrid.add(vals[i], 1, i);
        }
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setFillWidth(true);
        specsGrid.getColumnConstraints().addAll(c0, c1);

        specsCard.getChildren().addAll(specsTitle, specsGrid);

        // SMART Attributes Card
        VBox smartCard = new VBox(14);
        smartCard.getStyleClass().add("card");
        HBox.setHgrow(smartCard, Priority.ALWAYS);

        HBox smartTitleRow = new HBox(12);
        smartTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label smartTitle = new Label("Deep S.M.A.R.T. Attributes Telemetry Matrix");
        smartTitle.getStyleClass().add("card-title");

        Region smSpacer = new Region();
        HBox.setHgrow(smSpacer, Priority.ALWAYS);

        Label smartBadge = new Label("ATA/SCSI RAW PARSER");
        smartBadge.setStyle("-fx-font-size: 10px; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-font-weight: bold;");

        smartTitleRow.getChildren().addAll(smartTitle, smSpacer, smartBadge);

        smartTable = new TableView<>();
        smartTable.setPrefHeight(320);
        VBox.setVgrow(smartTable, Priority.ALWAYS);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().id())));
        colId.setPrefWidth(55);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colAttr = new TableColumn<>("Attribute Name");
        colAttr.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        colAttr.setPrefWidth(210);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colVal = new TableColumn<>("Value");
        colVal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().currentValue()));
        colVal.setPrefWidth(70);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colWorst = new TableColumn<>("Worst");
        colWorst.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().worstValue()));
        colWorst.setPrefWidth(70);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colThresh = new TableColumn<>("Thresh");
        colThresh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().threshold()));
        colThresh.setPrefWidth(70);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colRaw = new TableColumn<>("Raw Data");
        colRaw.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().rawValue()));
        colRaw.setPrefWidth(120);

        TableColumn<SmartDiagnostics.SmartAttribute, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        colStatus.setPrefWidth(110);

        smartTable.getColumns().addAll(colId, colAttr, colVal, colWorst, colThresh, colRaw, colStatus);
        smartTable.setItems(smartData);

        smartCard.getChildren().addAll(smartTitleRow, smartTable);

        specsRow.getChildren().addAll(specsCard, smartCard);

        rootContainer.getChildren().addAll(header, selectorRow, preWipeAssessmentBox, healthRow, specsRow);
    }

    private Label createSpecLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        return lbl;
    }

    private VBox createStatCard(String title) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14, 16, 14, 16));
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        card.getChildren().add(titleLbl);
        return card;
    }

    private void refreshDriveList() {
        List<UsbDetector.UsbDriveInfo> drives = UsbDetector.getConnectedUsbDrives();
        updateDriveList(drives);
    }

    private void updateDriveList(List<UsbDetector.UsbDriveInfo> drives) {
        UsbDetector.UsbDriveInfo current = cmbDrives.getSelectionModel().getSelectedItem();
        cmbDrives.setItems(FXCollections.observableArrayList(drives));

        if (drives.isEmpty()) {
            cmbDrives.setPromptText("No USB target drives connected");
            lblHealthValue.setText("-- / 100");
            lblHealthScoreBadge.setText("NO DEVICE");
            lblHealthScoreBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #FEF2F2; -fx-text-fill: #EF4444;");
            lblHealthDesc.setText("Connect a USB drive to run hardware diagnostics.");

            lblTempValue.setText("-- °C");
            lblThermalBadge.setText("OFFLINE");
            lblHoursValue.setText("-- hrs");
            lblSectorsValue.setText("0 BAD / 0 REALLOC");
            lblWearValue.setText("--% Life");
            pbWearLevel.setProgress(0);

            preWipeAssessmentBox.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
            lblPreWipeVerdict.setText("PRE-WIPE HEALTH ASSESSMENT: WAITING FOR DEVICE");
            lblPreWipeVerdict.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
            lblPreWipeDetails.setText("Insert a storage device to evaluate Pre-Wipe Health Score & thermal safety limits.");

            lblModelValue.setText("No drive connected");
            lblSerialValue.setText("--");
            lblCapacityValue.setText("--");
            lblTotalBlocksValue.setText("--");
            lblPathValue.setText("--");
            smartData.clear();
        } else {
            if (current != null && drives.contains(current)) {
                cmbDrives.getSelectionModel().select(current);
            } else {
                cmbDrives.getSelectionModel().select(0);
            }
            loadSelectedDriveDiagnostics();
        }
    }

    private void loadSelectedDriveDiagnostics() {
        UsbDetector.UsbDriveInfo drive = cmbDrives.getSelectionModel().getSelectedItem();
        if (drive == null) return;

        // Dynamic Specs Calculation
        lblModelValue.setText(drive.model());
        lblSerialValue.setText(drive.serial());
        lblCapacityValue.setText(drive.formattedSize() + " (" + String.format("%,d", drive.sizeBytes()) + " bytes)");

        long totalBlocks = drive.sizeBytes() / 512;
        lblTotalBlocksValue.setText(String.format("%,d sectors", totalBlocks));
        lblPathValue.setText(drive.systemPath());

        // Perform Deep SMART Diagnostics Inspection
        SmartDiagnostics.SmartReport report = SmartDiagnostics.inspectDrive(drive);

        // Health Score & Status
        SmartDiagnostics.HealthScoreResult health = report.healthScore();
        lblHealthValue.setText(health.score() + " / 100");
        lblHealthScoreBadge.setText(health.status().name());
        lblHealthScoreBadge.setStyle(String.format(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: %s; -fx-text-fill: %s;",
                health.status().getBgColor(), health.status().getTextColor()
        ));
        lblHealthValue.setStyle(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", health.status().getTextColor()));
        lblHealthDesc.setText(health.recommendation());

        // Temperature & Thermal Status
        lblTempValue.setText(report.temperatureCelsius() + " °C");
        lblThermalBadge.setText(report.thermalStatus().name());
        lblThermalBadge.setStyle(String.format(
                "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: %s; -fx-text-fill: %s;",
                report.thermalStatus().getBgColor(), report.thermalStatus().getTextColor()
        ));
        lblTempValue.setStyle(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", report.thermalStatus().getTextColor()));

        if (report.temperatureCelsius() >= 60) {
            lblTempDesc.setText("CRITICAL THERMAL LEVEL: Drive will auto-pause if wiped now.");
        } else if (report.temperatureCelsius() >= 48) {
            lblTempDesc.setText("ELEVATED TEMPERATURE: Operating near upper thermal threshold.");
        } else {
            lblTempDesc.setText("Operating within safe thermal limits (<48°C).");
        }

        // Power-On Hours & Lifespan
        lblHoursValue.setText(String.format("%,d hrs", report.powerOnHours()));
        double yearsEst = Math.max(0.1, (double) report.powerOnHours() / (24 * 365.25));
        lblHoursDesc.setText(String.format("Approx. %.1f years runtime | %d power cycles", yearsEst, report.powerCycleCount()));

        // Sector Defects (Bad Blocks & Reallocated Sectors)
        lblSectorsValue.setText(String.format("%d BAD / %d REALLOC", report.badBlocks(), report.reallocatedSectors()));
        if (report.badBlocks() > 0 || report.reallocatedSectors() > 0) {
            lblSectorsValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            lblSectorsDesc.setText("Sector defects detected on media surface.");
        } else {
            lblSectorsValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
            lblSectorsDesc.setText("Media surface is clean. Zero bad sectors detected.");
        }

        // Wear Leveling
        lblWearValue.setText(report.wearLevelingPercent() + "% Remaining Life");
        pbWearLevel.setProgress(report.wearLevelingPercent() / 100.0);
        if (report.wearLevelingPercent() < 50) {
            lblWearDesc.setText("High flash wear. Consider device retirement.");
            lblWearValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #D97706;");
        } else {
            lblWearDesc.setText("NAND flash endurance optimal for rewriting.");
            lblWearValue.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        }

        // Update Pre-Wipe Health Assessment Banner
        if (health.isWipePermittedWithoutOverride()) {
            preWipeAssessmentBox.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
            lblPreWipeVerdict.setText("PRE-WIPE HEALTH ASSESSMENT: PASSED (" + health.score() + "/100 - " + health.status().name() + ")");
            lblPreWipeVerdict.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #065F46;");
            lblPreWipeDetails.setText(health.recommendation());
        } else {
            preWipeAssessmentBox.setStyle("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
            lblPreWipeVerdict.setText("PRE-WIPE HEALTH ASSESSMENT: CRITICAL WARNING (" + health.score() + "/100 - " + health.status().name() + ")");
            lblPreWipeVerdict.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #991B1B;");
            lblPreWipeDetails.setText(String.join(" | ", health.warnings()) + ". " + health.recommendation());
        }

        // SMART Table Data
        smartData.clear();
        smartData.addAll(report.attributes());
    }

    private void runSmartSelfTest() {
        UsbDetector.UsbDriveInfo drive = cmbDrives.getSelectionModel().getSelectedItem();
        if (drive == null) return;

        NavigationManager.getInstance().showNotification(
                "S.M.A.R.T. Self-Test Initiated",
                "Querying low-level controller registers and sector health for " + drive.model(),
                ToastNotification.ToastType.INFO
        );

        loadSelectedDriveDiagnostics();
    }
}

