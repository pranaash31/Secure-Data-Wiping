package com.sanitizer.gui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ClientManagerView {

    private final VBox rootContainer = new VBox(24);

    public ClientManagerView() {
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(28));

        // ── Header ──────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Client & Organization Manager");
        lblTitle.getStyleClass().add("section-label");
        Label lblSub = new Label("Manage client organization profiles, compliance certifications, and assignment history");
        lblSub.getStyleClass().add("section-sublabel");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add New Client");
        btnAdd.getStyleClass().add("button-primary");

        Button btnExport = new Button("Export Client Report");
        btnExport.setStyle("-fx-margin-left: 10px;");

        HBox btnGroup = new HBox(10, btnExport, btnAdd);
        btnGroup.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(titleBox, hSpacer, btnGroup);

        // ── Stats Row ─────────────────────────────────────────────────
        int totalAuditWipes = com.sanitizer.db.AuditDb.getAllRecords().size();
        HBox statsRow = new HBox(16);
        VBox s1 = makeStatMini("Total Clients", "6 Enterprise Organizations", "#2563EB");
        VBox s2 = makeStatMini("Active Engagements", "3 Defense & Gov", "#059669");
        VBox s3 = makeStatMini("Pending Compliance Review", "2 Healthcare/Edu", "#D97706");
        VBox s4 = makeStatMini("Wipes Performed for Clients", totalAuditWipes + " Verified Wipes", "#DC2626");
        for (VBox s : new VBox[]{s1, s2, s3, s4}) {
            HBox.setHgrow(s, Priority.ALWAYS);
        }
        statsRow.getChildren().addAll(s1, s2, s3, s4);

        // ── Search Bar ─────────────────────────────────────────────────
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("card");
        searchBar.setPadding(new Insets(14, 20, 14, 20));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by organization name, sector, or compliance tag...");
        searchField.setPrefWidth(380);

        ComboBox<String> sectorFilter = new ComboBox<>();
        sectorFilter.getItems().addAll("All Sectors", "Defense", "Healthcare", "Finance", "Government", "Education");
        sectorFilter.getSelectionModel().select(0);

        ComboBox<String> complianceFilter = new ComboBox<>();
        complianceFilter.getItems().addAll("All Standards", "HIPAA", "GDPR", "DoD 5220.22-M", "NIST 800-88", "PCI-DSS");
        complianceFilter.getSelectionModel().select(0);

        Button btnSearch = new Button("Search");
        btnSearch.getStyleClass().add("button-primary");

        searchBar.getChildren().addAll(searchField, sectorFilter, complianceFilter, btnSearch);

        // ── Client Cards ─────────────────────────────────────────────────
        VBox clientList = new VBox(14);
        Object[][] clients = {
                {"Ministry of Defense, India", "DEFENSE", new String[]{"DoD 5220.22-M", "NIST 800-88", "FIPS 140-2"}, "ACTIVE", "12 wipes", "badge-success"},
                {"National Cyber Security Centre", "GOVERNMENT", new String[]{"NIST 800-88", "ISO 27001"}, "ACTIVE", "8 wipes", "badge-success"},
                {"Apollo Hospitals Group", "HEALTHCARE", new String[]{"HIPAA", "GDPR"}, "PENDING REVIEW", "5 wipes", "badge-warning"},
                {"SBI Financial Services", "FINANCE", new String[]{"PCI-DSS", "GDPR", "ISO 27001"}, "ACTIVE", "14 wipes", "badge-success"},
                {"ISRO — Indian Space Research", "GOVERNMENT", new String[]{"DoD 5220.22-M", "NIST 800-88"}, "COMPLETED", "8 wipes", "badge-info"},
                {"IIT Madras Research Foundation", "EDUCATION", new String[]{"NIST 800-88"}, "PENDING REVIEW", "0 wipes", "badge-warning"}
        };

        for (Object[] c : clients) {
            clientList.getChildren().add(createClientCard(
                    (String) c[0], (String) c[1], (String[]) c[2], (String) c[3], (String) c[4], (String) c[5]
            ));
        }

        ScrollPane scrollPane = new ScrollPane(clientList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(header, statsRow, searchBar, scrollPane);
    }

    private HBox createClientCard(String name, String sector, String[] compliance, String status, String wipes, String badge) {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 22, 18, 22));

        // Avatar / initials
        Label avatar = new Label(name.substring(0, 2).toUpperCase());
        avatar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 8px; " +
                "-fx-min-width: 44px; -fx-min-height: 44px; -fx-alignment: CENTER;");

        // Organization info
        VBox orgInfo = new VBox(6);
        Label orgName = new Label(name);
        orgName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F1F5F9;");
        Label sectorLabel = new Label("Sector: " + sector + "  |  " + wipes + " performed");
        sectorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");

        HBox complianceTags = new HBox(8);
        for (String tag : compliance) {
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; " +
                    "-fx-background-color: #1E293B; -fx-background-radius: 6px; -fx-padding: 3 8;");
            complianceTags.getChildren().add(tagLabel);
        }
        orgInfo.getChildren().addAll(orgName, sectorLabel, complianceTags);
        HBox.setHgrow(orgInfo, Priority.ALWAYS);

        // Status badge
        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add(badge);
        statusLabel.setMinWidth(120);
        statusLabel.setAlignment(Pos.CENTER);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnView = new Button("View Profile");
        btnView.getStyleClass().add("button-primary");
        btnView.setStyle("-fx-padding: 7 14; -fx-font-size: 11px;");
        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-padding: 7 14; -fx-font-size: 11px;");
        Button btnDel = new Button("Delete");
        btnDel.setStyle("-fx-padding: 7 14; -fx-font-size: 11px; -fx-text-fill: #F87171;");
        actions.getChildren().addAll(btnView, btnEdit, btnDel);

        card.getChildren().addAll(avatar, orgInfo, statusLabel, actions);
        return card;
    }

    private VBox makeStatMini(String label, String value, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        card.getChildren().addAll(val, lbl);
        return card;
    }
}
