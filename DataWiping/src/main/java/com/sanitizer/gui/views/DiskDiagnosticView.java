package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class DiskDiagnosticView {

    private final VBox rootContainer = new VBox(24);

    public DiskDiagnosticView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));
        rootContainer.setStyle("-fx-background-color: #0B0F19;");

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Drive Diagnostics & Health Inspector");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("SMART health analysis, drive specifications, and read performance benchmarks");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button btnScan = new Button("Scan All Drives");
        btnScan.getStyleClass().add("button-primary");

        header.getChildren().addAll(titleBox, hSpacer, btnScan);

        // ── Drive Selector ───────────────────────────────────────────────
        HBox selectorRow = new HBox(14);
        selectorRow.setAlignment(Pos.CENTER_LEFT);
        Label selectorLabel = new Label("Select Drive:");
        selectorLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        ComboBox<String> driveSelector = new ComboBox<>();
        driveSelector.getItems().addAll(
                "disk0 — APPLE SSD AP0512 (512 GB) — System",
                "disk2 — SanDisk Ultra USB 3.0 (64 GB)",
                "disk3 — Samsung T7 SSD (500 GB)",
                "disk4 — Seagate Portable HDD (1 TB)"
        );
        driveSelector.getSelectionModel().select(1);
        driveSelector.setPrefWidth(380);
        Button btnLoad = new Button("Load Diagnostics");
        btnLoad.getStyleClass().add("button-primary");
        selectorRow.getChildren().addAll(selectorLabel, driveSelector, btnLoad);

        // ── Top Cards Row: Health Status ─────────────────────────────────
        HBox healthRow = new HBox(16);

        VBox healthCard = createStatCard("Overall Health Status", "HEALTHY", "#34D399",
                "SMART data shows no critical errors.\nAll sectors are responsive.", "badge-success");
        VBox tempCard = createStatCard("Drive Temperature", "38°C", "#FBBF24",
                "Operating within safe limits.\nMax safe: 65°C", "badge-warning");
        VBox hoursCard = createStatCard("Power-On Hours", "4,286 hrs", "#60A5FA",
                "Estimated lifespan: 3.2 years remaining.\nTotal writes: 12.4 TB", "badge-info");
        VBox errorsCard = createStatCard("Reallocated Sectors", "0", "#34D399",
                "No bad sectors detected.\nDisk surface is clean.", "badge-success");

        for (VBox c : new VBox[]{healthCard, tempCard, hoursCard, errorsCard}) {
            HBox.setHgrow(c, Priority.ALWAYS);
        }
        healthRow.getChildren().addAll(healthCard, tempCard, hoursCard, errorsCard);

        // ── Drive Specifications ─────────────────────────────────────────
        HBox specsRow = new HBox(16);

        VBox specsCard = new VBox(16);
        specsCard.getStyleClass().add("card");
        HBox.setHgrow(specsCard, Priority.ALWAYS);

        Label specsTitle = new Label("Drive Specifications");
        specsTitle.getStyleClass().add("card-title");

        GridPane specsGrid = new GridPane();
        specsGrid.setHgap(16);
        specsGrid.setVgap(10);

        String[][] specs = {
                {"Model", "SanDisk Ultra USB 3.0"},
                {"Serial Number", "AA01234567890"},
                {"Firmware Version", "1.00"},
                {"Interface", "USB 3.0 (SuperSpeed)"},
                {"Capacity", "64.0 GB (68,719,476,736 bytes)"},
                {"Block Size", "512 bytes"},
                {"Total Blocks", "134,217,728 sectors"},
                {"Rotation Type", "Solid State (SSD/Flash)"},
                {"Bus Type", "USB Mass Storage"},
                {"Partition Scheme", "MBR (Master Boot Record)"}
        };

        for (int i = 0; i < specs.length; i++) {
            Label key = new Label(specs[i][0]);
            key.getStyleClass().add("settings-key-label");
            Label val = new Label(specs[i][1]);
            val.getStyleClass().add("settings-value-label");
            specsGrid.add(key, 0, i);
            specsGrid.add(val, 1, i);
        }
        ColumnConstraints c0 = new ColumnConstraints(170);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setFillWidth(true);
        specsGrid.getColumnConstraints().addAll(c0, c1);

        specsCard.getChildren().addAll(specsTitle, specsGrid);

        // SMART Attributes Card
        VBox smartCard = new VBox(16);
        smartCard.getStyleClass().add("card");
        HBox.setHgrow(smartCard, Priority.ALWAYS);

        Label smartTitle = new Label("SMART Attributes");
        smartTitle.getStyleClass().add("card-title");

        TableView<SmartAttr> smartTable = new TableView<>();
        smartTable.setPrefHeight(280);

        TableColumn<SmartAttr, String> colAttr = new TableColumn<>("Attribute");
        colAttr.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name));
        colAttr.setPrefWidth(200);

        TableColumn<SmartAttr, String> colVal = new TableColumn<>("Value");
        colVal.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().value));
        colVal.setPrefWidth(80);

        TableColumn<SmartAttr, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().status));
        colStatus.setPrefWidth(100);

        smartTable.getColumns().addAll(colAttr, colVal, colStatus);
        smartTable.getItems().addAll(
                new SmartAttr("Read Error Rate", "0", "OK"),
                new SmartAttr("Reallocated Sector Count", "0", "OK"),
                new SmartAttr("Power-On Hours Count", "4286", "OK"),
                new SmartAttr("Power Cycle Count", "312", "OK"),
                new SmartAttr("Reported Uncorrectable Errors", "0", "OK"),
                new SmartAttr("Command Timeout", "0", "OK"),
                new SmartAttr("High Fly Writes", "0", "OK"),
                new SmartAttr("Temperature Celsius", "38", "WARN")
        );

        smartCard.getChildren().addAll(smartTitle, smartTable);

        specsRow.getChildren().addAll(specsCard, smartCard);

        rootContainer.getChildren().addAll(header, selectorRow, healthRow, specsRow);
    }

    private VBox createStatCard(String title, String value, String valueColor, String description, String badge) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("card-subtitle");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");

        Label descLbl = new Label(description);
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        descLbl.setWrapText(true);

        card.getChildren().addAll(titleLbl, valueLbl, descLbl);
        return card;
    }

    public static class SmartAttr {
        public String name, value, status;
        public SmartAttr(String name, String value, String status) {
            this.name = name; this.value = value; this.status = status;
        }
    }
}
