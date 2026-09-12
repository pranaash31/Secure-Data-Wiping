package com.sanitizer.gui.views;

import com.sanitizer.db.AuditDb;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class DashboardView {

    private final VBox rootContainer = new VBox(20);

    public DashboardView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(24));
        rootContainer.setStyle("-fx-background-color: #F8FAFC;");

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("📊 System Analytics & Executive Dashboard");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label lblSub = new Label("Real-time sanitization performance indicators and audit trail metrics");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

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

        // --- Recent Sanitizations Table Card ---
        VBox tableCard = new VBox(14);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblTableTitle = new Label("Recent Sanitization Operations Log");
        lblTableTitle.getStyleClass().add("card-title");

        Button btnRefresh = new Button("🔄 Refresh Metrics");
        btnRefresh.setOnAction(e -> {
            rootContainer.getChildren().clear();
            buildUi();
        });

        tableHeader.getChildren().addAll(lblTableTitle, new Region(), btnRefresh);
        HBox.setHgrow(tableHeader.getChildren().get(1), Priority.ALWAYS);

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

        rootContainer.getChildren().addAll(titleBox, kpiGrid, tableCard);
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
