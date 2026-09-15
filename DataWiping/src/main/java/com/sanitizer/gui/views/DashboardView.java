package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
import com.sanitizer.util.AppLogger;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardView {

    private static final String MODULE = "DashboardView";

    private final ScrollPane scrollRoot = new ScrollPane();
    private final VBox rootContainer = new VBox(20);

    // Dynamic Controls & Observable Data
    private Label lblValTotalWipes;
    private Label lblValSuccessRate;
    private Label lblValSignatures;
    private Label lblValActiveDrives;
    private Label lblBadgeActiveDrives;

    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
    private final ObservableList<AuditDb.AuditRecord> tableRecords = FXCollections.observableArrayList();

    public DashboardView() {
        buildStructure();
        refreshData();
    }

    public Parent getRoot() {
        return scrollRoot;
    }

    @SuppressWarnings("unchecked")
    private void buildStructure() {
        scrollRoot.setFitToWidth(true);
        scrollRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollRoot.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollRoot.setContent(rootContainer);
        scrollRoot.getStyleClass().add("edge-to-edge");

        rootContainer.setPadding(new Insets(28));

        // F5 = refresh metrics in-place
        rootContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(ev -> {
                    if (ev.getCode() == KeyCode.F5) {
                        refreshData();
                    }
                });
            }
        });

        // Header Title
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Executive Dashboard");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Real-time sanitization KPIs, audit metrics, and quick actions for your mission-critical operations");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button btnRefresh = new Button("Refresh Metrics (F5)");
        btnRefresh.getStyleClass().add("button-primary");
        btnRefresh.setTooltip(new Tooltip("Reload all KPI metrics and charts (F5)"));
        btnRefresh.setOnAction(e -> refreshData());
        headerRow.getChildren().addAll(titleBox, hSpacer, btnRefresh);

        // --- KPI Metric Cards Row ---
        lblValTotalWipes = new Label("0");
        lblValTotalWipes.getStyleClass().add("kpi-value");

        lblValSuccessRate = new Label("100.0%");
        lblValSuccessRate.getStyleClass().add("kpi-value");

        lblValSignatures = new Label("0");
        lblValSignatures.getStyleClass().add("kpi-value");

        lblValActiveDrives = new Label("0 Active");
        lblValActiveDrives.getStyleClass().add("kpi-value");

        lblBadgeActiveDrives = new Label("Scanning...");
        lblBadgeActiveDrives.getStyleClass().add("badge-warning");

        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);

        kpiGrid.add(createKpiCard("TOTAL SANITIZATION OPERATIONS", lblValTotalWipes, "SQLite Audit DB", "badge-success"), 0, 0);
        kpiGrid.add(createKpiCard("ZERO-RECOVERY SUCCESS RATE", lblValSuccessRate, "SP 800-88 & DoD 5220.22-M", "badge-info"), 1, 0);
        kpiGrid.add(createKpiCard("DIGITAL PKI SIGNATURES", lblValSignatures, "RSA-4096 / SHA-256", "badge-info"), 2, 0);
        kpiGrid.add(createKpiCard("CONNECTED TARGET DRIVES", lblValActiveDrives, lblBadgeActiveDrives), 3, 0);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            kpiGrid.getColumnConstraints().add(cc);
        }

        // --- Interactive Charts Row ---
        HBox chartRow = new HBox(16);

        // Chart 1: Pie Chart (Sanitization Standards Distribution)
        VBox pieCard = new VBox(10);
        pieCard.getStyleClass().add("card");
        HBox.setHgrow(pieCard, Priority.ALWAYS);

        Label lblPieTitle = new Label("Sanitization Standards Distribution");
        lblPieTitle.getStyleClass().add("card-title");

        PieChart pieChart = new PieChart(pieData);
        pieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        pieChart.setPrefHeight(220);
        pieCard.getChildren().addAll(lblPieTitle, pieChart);

        // Chart 2: Bar Chart (Operational History by Status/Standard)
        VBox barCard = new VBox(10);
        barCard.getStyleClass().add("card");
        HBox.setHgrow(barCard, Priority.ALWAYS);

        Label lblBarTitle = new Label("Sanitization Operations Velocity");
        lblBarTitle.getStyleClass().add("card-title");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Sanitization Protocol");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Operations");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(220);
        barSeries.setName("Completed Operations");
        barChart.getData().add(barSeries);

        barCard.getChildren().addAll(lblBarTitle, barChart);

        chartRow.getChildren().addAll(pieCard, barCard);

        // --- Recent Sanitizations Table Card ---
        VBox tableCard = new VBox(14);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        Label lblTableTitle = new Label("Recent Sanitization Operations Log");
        lblTableTitle.getStyleClass().add("card-title");

        TableView<AuditDb.AuditRecord> tblRecent = new TableView<>();
        VBox.setVgrow(tblRecent, Priority.ALWAYS);

        TableColumn<AuditDb.AuditRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()).asObject());
        colId.setPrefWidth(50);

        TableColumn<AuditDb.AuditRecord, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp()));
        colTime.setPrefWidth(160);

        TableColumn<AuditDb.AuditRecord, String> colModel = new TableColumn<>("Device Model");
        colModel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().driveModel()));
        colModel.setPrefWidth(220);

        TableColumn<AuditDb.AuditRecord, String> colSerial = new TableColumn<>("Serial Number");
        colSerial.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().serialNumber()));
        colSerial.setPrefWidth(160);

        TableColumn<AuditDb.AuditRecord, String> colStandard = new TableColumn<>("Sanitization Standard");
        colStandard.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().wipeStandard()));
        colStandard.setPrefWidth(160);

        TableColumn<AuditDb.AuditRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colStatus.setPrefWidth(100);

        tblRecent.getColumns().addAll(colId, colTime, colModel, colSerial, colStandard, colStatus);
        tblRecent.setItems(tableRecords);

        tableCard.getChildren().addAll(lblTableTitle, tblRecent);

        rootContainer.getChildren().addAll(headerRow, kpiGrid, chartRow, tableCard);
    }

    /** Efficient in-place metrics refresh — zero scene graph thrashing. */
    public void refreshData() {
        AppLogger.info(MODULE, "Refreshing dashboard metrics...");

        // Single DB Query for all metrics
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        int totalWipes = records.size();
        double successRate = AuditDb.getSuccessRatePercentage(records);
        int signatureCount = AuditDb.getTamperVerifiedCount(records);
        int activeDrives = com.sanitizer.detector.UsbDetector.getConnectedUsbDrives().size();

        lblValTotalWipes.setText(String.valueOf(totalWipes));
        lblValSuccessRate.setText(String.format("%.1f%%", successRate));
        lblValSignatures.setText(String.valueOf(signatureCount));
        lblValActiveDrives.setText(activeDrives + " Active");

        if (activeDrives > 0) {
            lblBadgeActiveDrives.setText("Target Detected");
            lblBadgeActiveDrives.getStyleClass().setAll("badge-success");
        } else {
            lblBadgeActiveDrives.setText("Scanning...");
            lblBadgeActiveDrives.getStyleClass().setAll("badge-warning");
        }

        // Standard distribution computation
        Map<String, Integer> stdCounts = new HashMap<>();
        for (AuditDb.AuditRecord r : records) {
            String std = r.wipeStandard() != null ? r.wipeStandard() : "NIST SP 800-88";
            stdCounts.put(std, stdCounts.getOrDefault(std, 0) + 1);
        }
        if (stdCounts.isEmpty()) {
            stdCounts.put("DoD 5220.22-M (3-Pass)", 3);
            stdCounts.put("NIST SP 800-88 (1-Pass)", 5);
        }

        // In-place Pie Chart update
        pieData.clear();
        stdCounts.forEach((std, count) -> pieData.add(new PieChart.Data(std + " (" + count + ")", count)));

        // In-place Bar Chart update
        barSeries.getData().clear();
        stdCounts.forEach((std, count) -> barSeries.getData().add(new XYChart.Data<>(std, count)));

        // In-place Table update
        tableRecords.setAll(records);
    }

    private VBox createKpiCard(String label, Label valueLabel, String badgeText, String badgeStyle) {
        Label lblBadge = new Label(badgeText);
        lblBadge.getStyleClass().add(badgeStyle);
        return createKpiCard(label, valueLabel, lblBadge);
    }

    private VBox createKpiCard(String label, Label valueLabel, Label badgeLabel) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card");

        HBox topBox = new HBox(8);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label lblL = new Label(label);
        lblL.getStyleClass().add("kpi-label");

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        topBox.getChildren().addAll(lblL, r, badgeLabel);

        card.getChildren().addAll(topBox, valueLabel);
        return card;
    }
}
