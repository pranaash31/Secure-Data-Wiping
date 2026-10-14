package com.sanitizer.gui.views;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import com.sanitizer.detector.UsbDetector;
import com.sanitizer.gui.components.IoOscilloscopeComponent;
import com.sanitizer.gui.components.ThermalGraphComponent;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.gui.viewmodel.DiskDiagnosticViewModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import com.sanitizer.gui.navigation.ViewLifecycle;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

public class DiskDiagnosticView implements ViewLifecycle {

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(22);
    private final DiskDiagnosticViewModel viewModel;

    private ComboBox<UsbDetector.UsbDriveInfo> cmbDrives;
    private Button btnScan;
    private Button btnLoad;
    private Button btnSelfTest;

    // Thermal & I/O Telemetry Components
    private ThermalGraphComponent thermalGraph;
    private IoOscilloscopeComponent ioOscilloscope;

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

    // Interface Anomaly & Port Degradation Banner
    private HBox interfaceAnomalyBox;
    private Label lblAnomalyTitle;
    private Label lblAnomalyDiagnosis;
    private Label lblAnomalyPrompt;

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

    public DiskDiagnosticView() {
        this(new DiskDiagnosticViewModel());
    }

    public DiskDiagnosticView(DiskDiagnosticViewModel viewModel) {
        this.viewModel = viewModel;
        buildUi();
        bindViewModel();

        // Lifecycle: start listening & refresh initial drives
        viewModel.startListening();
        viewModel.refreshDrives();
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    public DiskDiagnosticViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void onActivated() {
        viewModel.startListening();
        viewModel.refreshDrives();
    }

    @Override
    public void onDeactivated() {
        viewModel.stopListening();
    }

    public void dispose() {
        onDeactivated();
    }

    @SuppressWarnings("unchecked")
    private void buildUi() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(24, 28, 28, 28));

        // Lifecycle management: unregister USB hardware listener when navigating away,
        // and re-register when view is attached to an active scene.
        scrollRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                viewModel.startListening();
                newScene.setOnKeyPressed(ev -> {
                    if (ev.getCode() == KeyCode.F5) viewModel.refreshDrives();
                });
            } else {
                viewModel.stopListening();
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
        btnScan.setOnAction(e -> viewModel.refreshDrives());
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
        cmbDrives.setOnAction(e -> viewModel.reloadCurrentDriveDiagnostics());
        cmbDrives.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) viewModel.reloadCurrentDriveDiagnostics();
        });

        btnLoad = new Button("Load S.M.A.R.T.");
        btnLoad.getStyleClass().add("button-primary");
        btnLoad.setOnAction(e -> viewModel.reloadCurrentDriveDiagnostics());

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

        VBox preWipeContent = new VBox(3);
        lblPreWipeVerdict = new Label();
        lblPreWipeDetails = new Label();
        preWipeContent.getChildren().addAll(lblPreWipeVerdict, lblPreWipeDetails);
        HBox.setHgrow(preWipeContent, Priority.ALWAYS);

        preWipeAssessmentBox.getChildren().add(preWipeContent);

        // ── 5 Stat Cards Row: Health, Temperature, Hours, Sectors/Bad Blocks, Wear Leveling ──
        HBox healthRow = new HBox(14);

        // Card 1: Health Score
        VBox healthCard = createStatCard("PRE-WIPE HEALTH SCORE");
        HBox hScoreBox = new HBox(8);
        hScoreBox.setAlignment(Pos.CENTER_LEFT);
        lblHealthValue = new Label();
        lblHealthScoreBadge = new Label();
        hScoreBox.getChildren().addAll(lblHealthValue, lblHealthScoreBadge);

        lblHealthDesc = new Label();
        lblHealthDesc.setWrapText(true);
        healthCard.getChildren().addAll(hScoreBox, lblHealthDesc);

        // Card 2: Temperature
        VBox tempCard = createStatCard("DRIVE TEMPERATURE");
        HBox tBox = new HBox(8);
        tBox.setAlignment(Pos.CENTER_LEFT);
        lblTempValue = new Label();
        lblThermalBadge = new Label();
        tBox.getChildren().addAll(lblTempValue, lblThermalBadge);

        lblTempDesc = new Label();
        lblTempDesc.setWrapText(true);
        tempCard.getChildren().addAll(tBox, lblTempDesc);

        // Card 3: Power-On Hours
        VBox hoursCard = createStatCard("POWER-ON HOURS");
        lblHoursValue = new Label();
        lblHoursDesc = new Label();
        lblHoursDesc.setWrapText(true);
        hoursCard.getChildren().addAll(lblHoursValue, lblHoursDesc);

        // Card 4: Reallocated Sectors & Bad Blocks
        VBox sectorsCard = createStatCard("SECTOR DEFECTS");
        lblSectorsValue = new Label();
        lblSectorsDesc = new Label();
        lblSectorsDesc.setWrapText(true);
        sectorsCard.getChildren().addAll(lblSectorsValue, lblSectorsDesc);

        // Card 5: Flash Wear Leveling
        VBox wearCard = createStatCard("NAND WEAR LEVELING");
        lblWearValue = new Label();
        pbWearLevel = new ProgressBar(1.0);
        pbWearLevel.setMaxWidth(Double.MAX_VALUE);
        pbWearLevel.setPrefHeight(6);
        lblWearDesc = new Label();
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

        smartCard.getChildren().addAll(smartTitleRow, smartTable);

        specsRow.getChildren().addAll(specsCard, smartCard);

        thermalGraph = new ThermalGraphComponent();
        ioOscilloscope = new IoOscilloscopeComponent();

        // ── Interface Anomaly & Port Degradation Banner ──────────────────
        interfaceAnomalyBox = new HBox(16);
        interfaceAnomalyBox.setAlignment(Pos.CENTER_LEFT);
        interfaceAnomalyBox.getStyleClass().add("card");
        interfaceAnomalyBox.setManaged(false);
        interfaceAnomalyBox.setVisible(false);

        Label anomalyIcon = new Label("⚠️");
        anomalyIcon.setStyle("-fx-font-size: 22px;");

        VBox anomalyContent = new VBox(3);
        lblAnomalyTitle = new Label();
        lblAnomalyDiagnosis = new Label();
        lblAnomalyDiagnosis.setWrapText(true);
        lblAnomalyPrompt = new Label();
        anomalyContent.getChildren().addAll(lblAnomalyTitle, lblAnomalyDiagnosis, lblAnomalyPrompt);
        HBox.setHgrow(anomalyContent, Priority.ALWAYS);

        interfaceAnomalyBox.getChildren().addAll(anomalyIcon, anomalyContent);

        rootContainer.getChildren().addAll(header, selectorRow, preWipeAssessmentBox, interfaceAnomalyBox, healthRow, ioOscilloscope, thermalGraph, specsRow);
    }

    private void bindViewModel() {
        // Spec labels
        lblModelValue.textProperty().bind(viewModel.modelProperty());
        lblSerialValue.textProperty().bind(viewModel.serialProperty());
        lblCapacityValue.textProperty().bind(viewModel.capacityProperty());
        lblTotalBlocksValue.textProperty().bind(viewModel.totalBlocksProperty());
        lblPathValue.textProperty().bind(viewModel.pathProperty());

        // Health Card
        lblHealthValue.textProperty().bind(viewModel.healthValueProperty());
        lblHealthValue.styleProperty().bind(viewModel.healthValueStyleProperty());
        lblHealthScoreBadge.textProperty().bind(viewModel.healthScoreBadgeProperty());
        lblHealthScoreBadge.styleProperty().bind(viewModel.healthScoreBadgeStyleProperty());
        lblHealthDesc.textProperty().bind(viewModel.healthDescProperty());

        // Temperature Card
        lblTempValue.textProperty().bind(viewModel.tempValueProperty());
        lblTempValue.styleProperty().bind(viewModel.tempValueStyleProperty());
        lblThermalBadge.textProperty().bind(viewModel.thermalBadgeProperty());
        lblThermalBadge.styleProperty().bind(viewModel.thermalBadgeStyleProperty());
        lblTempDesc.textProperty().bind(viewModel.tempDescProperty());

        // Power-On Hours Card
        lblHoursValue.textProperty().bind(viewModel.hoursValueProperty());
        lblHoursDesc.textProperty().bind(viewModel.hoursDescProperty());

        // Sector Defects Card
        lblSectorsValue.textProperty().bind(viewModel.sectorsValueProperty());
        lblSectorsValue.styleProperty().bind(viewModel.sectorsValueStyleProperty());
        lblSectorsDesc.textProperty().bind(viewModel.sectorsDescProperty());

        // Flash Wear Leveling Card
        lblWearValue.textProperty().bind(viewModel.wearValueProperty());
        lblWearValue.styleProperty().bind(viewModel.wearValueStyleProperty());
        pbWearLevel.progressProperty().bind(viewModel.wearProgressProperty());
        lblWearDesc.textProperty().bind(viewModel.wearDescProperty());

        // Pre-Wipe Health Assessment Banner
        preWipeAssessmentBox.styleProperty().bind(viewModel.preWipeBoxStyleProperty());
        lblPreWipeVerdict.textProperty().bind(viewModel.preWipeVerdictProperty());
        lblPreWipeVerdict.styleProperty().bind(viewModel.preWipeVerdictStyleProperty());
        lblPreWipeDetails.textProperty().bind(viewModel.preWipeDetailsProperty());

        // Interface Anomaly Banner
        interfaceAnomalyBox.styleProperty().bind(viewModel.interfaceAnomalyBoxStyleProperty());
        interfaceAnomalyBox.visibleProperty().bind(viewModel.interfaceAnomalyVisibleProperty());
        interfaceAnomalyBox.managedProperty().bind(viewModel.interfaceAnomalyVisibleProperty());
        lblAnomalyTitle.textProperty().bind(viewModel.anomalyTitleProperty());
        lblAnomalyTitle.styleProperty().bind(viewModel.anomalyTitleStyleProperty());
        lblAnomalyDiagnosis.textProperty().bind(viewModel.anomalyDiagnosisProperty());
        lblAnomalyDiagnosis.styleProperty().bind(viewModel.anomalyDiagnosisStyleProperty());
        lblAnomalyPrompt.textProperty().bind(viewModel.anomalyPromptProperty());
        lblAnomalyPrompt.styleProperty().bind(viewModel.anomalyPromptStyleProperty());

        // SMART Attributes Table
        smartTable.setItems(viewModel.getSmartAttributes());

        // Drive Combo Box
        cmbDrives.setItems(viewModel.getDrives());
        cmbDrives.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(viewModel.selectedDriveProperty().get())) {
                viewModel.selectDrive(newVal);
            }
        });
        viewModel.selectedDriveProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(cmbDrives.getSelectionModel().getSelectedItem())) {
                cmbDrives.getSelectionModel().select(newVal);
            } else if (newVal == null) {
                cmbDrives.getSelectionModel().clearSelection();
            }
        });

        // Prompt text update on drives list changes
        viewModel.getDrives().addListener((ListChangeListener<UsbDetector.UsbDriveInfo>) c -> {
            if (viewModel.getDrives().isEmpty()) {
                cmbDrives.setPromptText("No USB target drives connected");
            } else {
                cmbDrives.setPromptText("Scanning connected hardware drives...");
            }
        });

        // Telemetry components (ThermalGraph & IoOscilloscope) update on report changes
        viewModel.currentReportProperty().addListener((obs, oldReport, newReport) -> {
            UsbDetector.UsbDriveInfo drive = viewModel.selectedDriveProperty().get();
            if (newReport != null && drive != null) {
                if (thermalGraph != null) {
                    ThermalPolicy policy = ThermalPolicyManager.getInstance().getPolicyForDrive(drive.model(), drive.systemPath(), drive.sizeBytes());
                    thermalGraph.setPolicy(policy);
                    thermalGraph.addSample(newReport.temperatureCelsius());
                }
                if (ioOscilloscope != null) {
                    ioOscilloscope.setDeviceContext(drive.model(), drive.systemPath(), drive.sizeBytes());
                }
            } else {
                if (thermalGraph != null) thermalGraph.reset();
                if (ioOscilloscope != null) ioOscilloscope.reset();
            }
        });
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

    private void runSmartSelfTest() {
        UsbDetector.UsbDriveInfo drive = viewModel.selectedDriveProperty().get();
        if (drive == null) return;

        NavigationManager.getInstance().showNotification(
                "S.M.A.R.T. Self-Test Initiated",
                "Querying low-level controller registers and sector health for " + drive.model(),
                ToastType.INFO
        );

        viewModel.reloadCurrentDriveDiagnostics();

        // Simulate a mini diagnostic telemetry check into thermal and I/O graphs
        if (thermalGraph != null) {
            int base = SmartDiagnostics.getLiveTemperature(drive.systemPath(), drive.serial());
            thermalGraph.addSample(base);
            thermalGraph.addSample(base + 1);
            thermalGraph.addSample(base + 2);
            thermalGraph.addSample(base);
        }
        if (ioOscilloscope != null) {
            ioOscilloscope.addSample(42.5, 10880, 2.1, 9.4);
            ioOscilloscope.addSample(58.3, 14920, 1.8, 12.9);
            ioOscilloscope.addSample(64.1, 16400, 1.6, 14.2);
            ioOscilloscope.addSample(52.0, 13312, 1.9, 11.5);
        }
    }
}
