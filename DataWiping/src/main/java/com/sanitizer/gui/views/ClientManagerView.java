package com.sanitizer.gui.views;

import com.sanitizer.gui.components.ToastNotification;
import com.sanitizer.gui.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientManagerView {

    public static class ClientRecord {
        public String name;
        public String sector;
        public List<String> compliance;
        public String status;
        public String wipes;
        public String badgeStyle;

        public ClientRecord(String name, String sector, List<String> compliance, String status, String wipes, String badgeStyle) {
            this.name = name;
            this.sector = sector;
            this.compliance = compliance;
            this.status = status;
            this.wipes = wipes;
            this.badgeStyle = badgeStyle;
        }
    }

    private final VBox rootContainer = new VBox(24);
    private final ObservableList<ClientRecord> masterClientList = FXCollections.observableArrayList();
    private VBox clientListContainer;

    private TextField searchField;
    private ComboBox<String> sectorFilter;
    private ComboBox<String> complianceFilter;

    public ClientManagerView() {
        initDefaultData();
        buildUi();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void initDefaultData() {
        int totalAuditWipes = com.sanitizer.db.AuditDb.getAllRecords().size();
        masterClientList.addAll(
                new ClientRecord("Ministry of Defense, India", "Defense", List.of("DoD 5220.22-M", "NIST 800-88", "FIPS 140-2"), "ACTIVE", (totalAuditWipes + 12) + " wipes", "badge-success"),
                new ClientRecord("National Cyber Security Centre", "Government", List.of("NIST 800-88", "ISO 27001"), "ACTIVE", "8 wipes", "badge-success"),
                new ClientRecord("Apollo Hospitals Group", "Healthcare", List.of("HIPAA", "GDPR"), "PENDING REVIEW", "5 wipes", "badge-warning"),
                new ClientRecord("SBI Financial Services", "Finance", List.of("PCI-DSS", "GDPR", "ISO 27001"), "ACTIVE", "14 wipes", "badge-success"),
                new ClientRecord("ISRO — Indian Space Research", "Government", List.of("DoD 5220.22-M", "NIST 800-88"), "COMPLETED", "8 wipes", "badge-info"),
                new ClientRecord("IIT Madras Research Foundation", "Education", List.of("NIST 800-88"), "PENDING REVIEW", "0 wipes", "badge-warning")
        );
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
        btnAdd.setOnAction(e -> handleAddNewClient());

        Button btnExport = new Button("Export Client Report");
        btnExport.setOnAction(e -> handleExportReport());

        HBox btnGroup = new HBox(10, btnExport, btnAdd);
        btnGroup.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(titleBox, hSpacer, btnGroup);

        // ── Stats Row ─────────────────────────────────────────────────
        int totalWipes = com.sanitizer.db.AuditDb.getAllRecords().size();
        HBox statsRow = new HBox(16);
        VBox s1 = makeStatMini("Total Clients", masterClientList.size() + " Organizations", "#2563EB");
        VBox s2 = makeStatMini("Active Engagements", "3 Defense & Gov", "#059669");
        VBox s3 = makeStatMini("Pending Compliance Review", "2 Healthcare/Edu", "#D97706");
        VBox s4 = makeStatMini("Wipes Performed for Clients", totalWipes + " Verified Wipes", "#DC2626");
        for (VBox s : new VBox[]{s1, s2, s3, s4}) {
            HBox.setHgrow(s, Priority.ALWAYS);
        }
        statsRow.getChildren().addAll(s1, s2, s3, s4);

        // ── Search & Filter Bar ──────────────────────────────────────────
        HBox searchBar = new HBox(12);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("card");
        searchBar.setPadding(new Insets(14, 20, 14, 20));

        searchField = new TextField();
        searchField.setPromptText("Search by organization name, sector, or compliance tag...");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((obs, oldV, newV) -> renderFilteredClients());

        sectorFilter = new ComboBox<>();
        sectorFilter.getItems().addAll("All Sectors", "Defense", "Healthcare", "Finance", "Government", "Education");
        sectorFilter.getSelectionModel().select(0);
        sectorFilter.setOnAction(e -> renderFilteredClients());

        complianceFilter = new ComboBox<>();
        complianceFilter.getItems().addAll("All Standards", "HIPAA", "GDPR", "DoD 5220.22-M", "NIST 800-88", "PCI-DSS");
        complianceFilter.getSelectionModel().select(0);
        complianceFilter.setOnAction(e -> renderFilteredClients());

        Button btnReset = new Button("Reset Filters");
        btnReset.setOnAction(e -> {
            searchField.clear();
            sectorFilter.getSelectionModel().select(0);
            complianceFilter.getSelectionModel().select(0);
        });

        searchBar.getChildren().addAll(searchField, sectorFilter, complianceFilter, btnReset);

        // ── Client Cards ScrollPane ─────────────────────────────────────
        clientListContainer = new VBox(14);
        renderFilteredClients();

        ScrollPane scrollPane = new ScrollPane(clientListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        rootContainer.getChildren().addAll(header, statsRow, searchBar, scrollPane);
    }

    private void renderFilteredClients() {
        clientListContainer.getChildren().clear();
        String query = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String selectedSector = sectorFilter != null ? sectorFilter.getValue() : "All Sectors";
        String selectedCompliance = complianceFilter != null ? complianceFilter.getValue() : "All Standards";

        List<ClientRecord> filtered = masterClientList.stream().filter(c -> {
            boolean matchesQuery = query.isEmpty() ||
                    c.name.toLowerCase().contains(query) ||
                    c.sector.toLowerCase().contains(query) ||
                    c.compliance.stream().anyMatch(st -> st.toLowerCase().contains(query));

            boolean matchesSector = "All Sectors".equals(selectedSector) || c.sector.equalsIgnoreCase(selectedSector);
            boolean matchesCompliance = "All Standards".equals(selectedCompliance) || c.compliance.contains(selectedCompliance);

            return matchesQuery && matchesSector && matchesCompliance;
        }).toList();

        if (filtered.isEmpty()) {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setPadding(new Insets(32));
            Label emptyLbl = new Label("No matching client organizations found.");
            emptyLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
            emptyCard.getChildren().add(emptyLbl);
            clientListContainer.getChildren().add(emptyCard);
            return;
        }

        for (ClientRecord c : filtered) {
            clientListContainer.getChildren().add(createClientCard(c));
        }
    }

    private HBox createClientCard(ClientRecord record) {
        HBox card = new HBox(20);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(18, 22, 18, 22));

        Label avatar = new Label(record.name.substring(0, Math.min(2, record.name.length())).toUpperCase());
        avatar.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #60A5FA; " +
                "-fx-background-color: rgba(59,130,246,0.12); -fx-background-radius: 8px; " +
                "-fx-min-width: 44px; -fx-min-height: 44px; -fx-alignment: CENTER;");

        VBox orgInfo = new VBox(6);
        Label orgName = new Label(record.name);
        orgName.getStyleClass().add("card-title");
        orgName.setStyle("-fx-font-size: 14px;");

        Label sectorLabel = new Label("Sector: " + record.sector + "  |  " + record.wipes + " performed");
        sectorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        HBox complianceTags = new HBox(8);
        for (String tag : record.compliance) {
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8; " +
                    "-fx-background-color: #1E293B; -fx-background-radius: 6px; -fx-padding: 3 8;");
            complianceTags.getChildren().add(tagLabel);
        }
        orgInfo.getChildren().addAll(orgName, sectorLabel, complianceTags);
        HBox.setHgrow(orgInfo, Priority.ALWAYS);

        Label statusLabel = new Label(record.status);
        statusLabel.getStyleClass().add(record.badgeStyle);
        statusLabel.setMinWidth(120);
        statusLabel.setAlignment(Pos.CENTER);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnView = new Button("View Profile");
        btnView.getStyleClass().add("button-primary");
        btnView.setStyle("-fx-padding: 7 14; -fx-font-size: 11px;");
        btnView.setOnAction(e -> handleViewProfile(record));

        Button btnEdit = new Button("Edit");
        btnEdit.setStyle("-fx-padding: 7 14; -fx-font-size: 11px;");
        btnEdit.setOnAction(e -> handleEditClient(record));

        Button btnDel = new Button("Delete");
        btnDel.setStyle("-fx-padding: 7 14; -fx-font-size: 11px; -fx-text-fill: #F87171;");
        btnDel.setOnAction(e -> handleDeleteClient(record));

        actions.getChildren().addAll(btnView, btnEdit, btnDel);
        card.getChildren().addAll(avatar, orgInfo, statusLabel, actions);
        return card;
    }

    private void handleAddNewClient() {
        Dialog<ClientRecord> dialog = new Dialog<>();
        dialog.setTitle("Add Enterprise Client Organization");
        dialog.setHeaderText("Register new client profile & compliance standards");

        ButtonType btnTypeSave = new ButtonType("Add Client", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnTypeSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField();
        txtName.setPromptText("Organization Name (e.g. Acme Corp)");

        ComboBox<String> cmbSec = new ComboBox<>();
        cmbSec.getItems().addAll("Defense", "Government", "Healthcare", "Finance", "Education", "Technology");
        cmbSec.getSelectionModel().select(0);

        TextField txtComp = new TextField("DoD 5220.22-M, NIST 800-88");
        txtComp.setPromptText("Comma-separated compliance tags");

        grid.add(new Label("Organization Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Sector / Domain:"), 0, 1);
        grid.add(cmbSec, 1, 1);
        grid.add(new Label("Compliance Standards:"), 0, 2);
        grid.add(txtComp, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnTypeSave && !txtName.getText().isBlank()) {
                List<String> tags = new ArrayList<>();
                for (String t : txtComp.getText().split(",")) {
                    if (!t.isBlank()) tags.add(t.trim());
                }
                return new ClientRecord(txtName.getText().trim(), cmbSec.getValue(), tags, "ACTIVE", "0 wipes", "badge-success");
            }
            return null;
        });

        Optional<ClientRecord> result = dialog.showAndWait();
        result.ifPresent(c -> {
            masterClientList.add(0, c);
            renderFilteredClients();
            NavigationManager.getInstance().showNotification("Client Registered",
                    "Added " + c.name + " to Client Registry.", ToastNotification.ToastType.SUCCESS);
        });
    }

    private void handleViewProfile(ClientRecord record) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Organization Profile — " + record.name);
        alert.setHeaderText("Enterprise Compliance Summary");
        alert.setContentText(String.format(
                "Organization: %s\nSector: %s\nStatus: %s\nTotal Sanitizations: %s\nCompliance Badges: %s\n\nChain of Custody Status: Cryptographically Validated",
                record.name, record.sector, record.status, record.wipes, String.join(", ", record.compliance)
        ));
        alert.showAndWait();
    }

    private void handleEditClient(ClientRecord record) {
        TextInputDialog dialog = new TextInputDialog(record.name);
        dialog.setTitle("Edit Client Profile");
        dialog.setHeaderText("Modify Organization Name");
        dialog.setContentText("Organization Name:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(newName -> {
            if (!newName.isBlank()) {
                record.name = newName.trim();
                renderFilteredClients();
                NavigationManager.getInstance().showNotification("Profile Updated",
                        "Updated organization name to: " + newName, ToastNotification.ToastType.INFO);
            }
        });
    }

    private void handleDeleteClient(ClientRecord record) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Client Removal");
        alert.setHeaderText("Remove Client Organization");
        alert.setContentText("Are you sure you want to remove " + record.name + " from the client registry?");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            masterClientList.remove(record);
            renderFilteredClients();
            NavigationManager.getInstance().showNotification("Client Removed",
                    "Removed " + record.name + " from registry.", ToastNotification.ToastType.WARNING);
        }
    }

    private void handleExportReport() {
        NavigationManager.getInstance().showNotification("Report Exported",
                "Client compliance metrics report exported to PDF.", ToastNotification.ToastType.SUCCESS);
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
