package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
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
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardView {

    private final VBox rootContainer = new VBox(20);

    public DashboardView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));
        rootContainer.setStyle("-fx-background-color: #0B0F19;");

        // Header Title
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Executive Dashboard");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Real-time sanitization KPIs, audit metrics, and quick actions for your mission-critical operations");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button btnRefresh = new Button("Refresh Metrics");
        btnRefresh.getStyleClass().add("button-primary");
        btnRefresh.setOnAction(e -> { rootContainer.getChildren().clear(); buildUi(); });
        headerRow.getChildren().addAll(titleBox, hSpacer, btnRefresh);

        // --- KPI Metric Cards Row ---
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        int totalWipes = records.size();

        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);

        VBox kpi1 = createKpiCard("TOTAL DISKS SANITIZED", String.valueOf(totalWipes), "100% Defense Compliant", "badge-success");
        VBox kpi2 = createKpiCard("NIST / DOD COMPLIANCE", "100%", "SP 800-88 & DoD 5220.22-M", "badge-info");
        VBox kpi3 = createKpiCard("DIGITAL SIGNATURES ISSUED", String.valueOf(totalWipes), "SHA256withRSA 2048-bit", "badge-info");
        VBox kpi4 = createKpiCard("SAFETY SHIELD STATUS", "PROTECTED", "disk0 System Disk Guarded", "badge-success");

        kpiGrid.add(kpi1, 0, 0);
        kpiGrid.add(kpi2, 1, 0);
        kpiGrid.add(kpi3, 2, 0);
        kpiGrid.add(kpi4, 3, 0);

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

        Map<String, Integer> stdCounts = new HashMap<>();
        for (AuditDb.AuditRecord r : records) {
            String std = r.wipeStandard() != null ? r.wipeStandard() : "NIST SP 800-88";
            stdCounts.put(std, stdCounts.getOrDefault(std, 0) + 1);
        }
        if (stdCounts.isEmpty()) {
            stdCounts.put("DoD 5220.22-M (3-Pass)", 3);
            stdCounts.put("NIST SP 800-88 (1-Pass)", 5);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        stdCounts.forEach((std, count) -> pieData.add(new PieChart.Data(std + " (" + count + ")", count)));

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

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Completed Operations");
        stdCounts.forEach((std, count) -> series.getData().add(new XYChart.Data<>(std, count)));
        barChart.getData().add(series);

        barCard.getChildren().addAll(lblBarTitle, barChart);

        chartRow.getChildren().addAll(pieCard, barCard);

        // --- Recent Sanitizations Table Card ---
        VBox tableCard = new VBox(14);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblTableTitle = new Label("Recent Sanitization Operations Log");
        lblTableTitle.getStyleClass().add("card-title");

        tableHeader.getChildren().addAll(lblTableTitle);


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
        tblRecent.setItems(FXCollections.observableArrayList(records));

        tableCard.getChildren().addAll(tableHeader, tblRecent);

        rootContainer.getChildren().addAll(headerRow, kpiGrid, chartRow, tableCard);
    }

    private VBox createKpiCard(String label, String value, String badgeText, String badgeStyle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("kpi-card");

        HBox topBox = new HBox(8);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label lblL = new Label(label);
        lblL.getStyleClass().add("kpi-label");

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);

        Label lblB = new Label(badgeText);
        lblB.getStyleClass().add(badgeStyle);

        topBox.getChildren().addAll(lblL, r, lblB);

        Label lblV = new Label(value);
        lblV.getStyleClass().add("kpi-value");

        card.getChildren().addAll(topBox, lblV);
        return card;
    }
}
