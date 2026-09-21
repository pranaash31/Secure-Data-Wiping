package com.sanitizer.gui.views;

import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.audit.SecurityAuditRecord;
import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.pdf.CertificateGenerator;
import com.sanitizer.report.AuditExporter;
import com.sanitizer.session.UserRole;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AuditView {

    private final VBox rootContainer = new VBox(16);

    // Tab 1: Sanitization History
    private TableView<AuditDb.AuditRecord> tblAuditHistory;
    private ObservableList<AuditDb.AuditRecord> auditData;
    private FilteredList<AuditDb.AuditRecord> filteredData;
    private Label lblEsgBannerText;

    // Tab 2: Security Audit Trail
    private TableView<SecurityAuditRecord> tblSecurityHistory;
    private ObservableList<SecurityAuditRecord> securityData;
    private FilteredList<SecurityAuditRecord> filteredSecurityData;

    public AuditView() {
        buildUi();
        loadAuditHistory();
        loadSecurityHistory();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    private void buildUi() {
        rootContainer.setPadding(new Insets(20));

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Audit Trail & Security Governance");
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setStyle("-fx-font-size: 22px;");
        Label lblSub = new Label("Tamper-evident sanitization records, cryptographic ledger chaining, and multi-tier RBAC security event logs");
        lblSub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // TabPane
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabSanitization = new Tab("💿 Sanitization History & Certificates", buildSanitizationTab());
        Tab tabSecurity = new Tab("🛡️ Security Activity Trail (RBAC & System Events)", buildSecurityTrailTab());

        tabPane.getTabs().addAll(tabSanitization, tabSecurity);

        rootContainer.getChildren().addAll(titleBox, tabPane);
    }

    // ── Tab 1: Sanitization History ──────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildSanitizationTab() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(container, Priority.ALWAYS);

        // --- ESG Corporate Sustainability Banner ---
        HBox esgBanner = new HBox(12);
        esgBanner.setAlignment(Pos.CENTER_LEFT);
        esgBanner.setStyle("-fx-background-color: linear-gradient(to right, #064E3B, #0F172A); -fx-border-color: #059669; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 16;");

        lblEsgBannerText = new Label("🌱 ESG Impact: Calculating circular economy e-waste & carbon offsets...");
        lblEsgBannerText.setStyle("-fx-text-fill: #A7F3D0; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region esgRegion = new Region();
        HBox.setHgrow(esgRegion, Priority.ALWAYS);

        Button btnExportEsg = new Button("🌱 ESG Audit Report");
        btnExportEsg.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnExportEsg.setTooltip(new Tooltip("Generate ESG Corporate Sustainability Impact Summary Report"));
        btnExportEsg.setOnAction(e -> handleExportEsgReport());

        esgBanner.getChildren().addAll(lblEsgBannerText, esgRegion, btnExportEsg);

        // --- Card: Audit Log Table & Toolbar ---
        VBox cardTable = new VBox(12);
        cardTable.getStyleClass().add("card");
        VBox.setVgrow(cardTable, Priority.ALWAYS);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Search by model, serial, standard...");
        txtSearch.setPrefWidth(220);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterLog(newVal));

        Button btnRefresh = new Button("Refresh (F5)");
        btnRefresh.setTooltip(new Tooltip("Reload audit log from SQLite (F5)"));
        btnRefresh.setOnAction(e -> loadAuditHistory());

        Button btnCompliance = new Button("📊 Compliance KPIs");
        btnCompliance.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnCompliance.setTooltip(new Tooltip("View Consolidated Corporate Compliance & Executive KPIs"));
        btnCompliance.setOnAction(e -> handleShowComplianceSummary());

        // Multi-Format Batch Export MenuButton
        MenuButton mbExport = new MenuButton("📤 Batch Export");
        mbExport.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        mbExport.setTooltip(new Tooltip("Export sanitized records into standard enterprise audit formats"));

        MenuItem itemCsv = new MenuItem("📄 Export CSV (.csv)");
        itemCsv.setOnAction(e -> handleBatchExport("CSV"));

        MenuItem itemJson = new MenuItem("📋 Export JSON (.json)");
        itemJson.setOnAction(e -> handleBatchExport("JSON"));

        MenuItem itemExcel = new MenuItem("📊 Export Excel Spreadsheet (.xml)");
        itemExcel.setOnAction(e -> handleBatchExport("EXCEL"));

        MenuItem itemAll = new MenuItem("📦 Export Full Package (CSV + JSON + Excel)");
        itemAll.setOnAction(e -> handleExportFullPackage());

        mbExport.getItems().addAll(itemCsv, itemJson, itemExcel, new SeparatorMenuItem(), itemAll);

        Button btnExportPdf = new Button("Export PDF Certificate");
        btnExportPdf.getStyleClass().add("button-primary");
        btnExportPdf.setTooltip(new Tooltip("Generate PDF certificate for selected record (also: double-click row)"));
        btnExportPdf.setOnAction(e -> handleExportPdf());

        Button btnQuarantine = new Button("⚠️ Quarantine Report");
        btnQuarantine.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnQuarantine.setTooltip(new Tooltip("Generate Defective Hardware Quarantine & Physical Destruction Order PDF"));
        btnQuarantine.setOnAction(e -> handleExportQuarantineReport());

        Button btnVerify = new Button("Verify Signature");
        btnVerify.setTooltip(new Tooltip("Verify SHA256withRSA signature for selected record"));
        btnVerify.setOnAction(e -> handleVerifySignature());

        Button btnVerifyLedger = new Button("🔒 Verify Ledger Integrity");
        btnVerifyLedger.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnVerifyLedger.setTooltip(new Tooltip("Verify SHA-256 cryptographic block chaining & tamper resistance across the entire database"));
        btnVerifyLedger.setOnAction(e -> handleVerifyLedgerIntegrity());

        toolbar.getChildren().addAll(txtSearch, new Region(), btnRefresh, btnCompliance, mbExport, btnExportPdf, btnQuarantine, btnVerify, btnVerifyLedger);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        // Table
        tblAuditHistory = new TableView<>();
        VBox.setVgrow(tblAuditHistory, Priority.ALWAYS);

        TableColumn<AuditDb.AuditRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()).asObject());
        colId.setPrefWidth(50);

        TableColumn<AuditDb.AuditRecord, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp()));
        colTime.setPrefWidth(150);

        TableColumn<AuditDb.AuditRecord, String> colModel = new TableColumn<>("Device Model");
        colModel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().driveModel()));
        colModel.setPrefWidth(190);

        TableColumn<AuditDb.AuditRecord, String> colSerial = new TableColumn<>("Serial Number");
        colSerial.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().serialNumber()));
        colSerial.setPrefWidth(140);

        TableColumn<AuditDb.AuditRecord, String> colCapacity = new TableColumn<>("Capacity");
        colCapacity.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().capacity()));
        colCapacity.setPrefWidth(85);

        TableColumn<AuditDb.AuditRecord, String> colStandard = new TableColumn<>("Sanitization Standard");
        colStandard.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().wipeStandard()));
        colStandard.setPrefWidth(145);

        TableColumn<AuditDb.AuditRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colStatus.setPrefWidth(80);

        TableColumn<AuditDb.AuditRecord, String> colHealth = new TableColumn<>("S.M.A.R.T. Health Delta");
        colHealth.setCellValueFactory(data -> {
            AuditDb.AuditRecord r = data.getValue();
            return new SimpleStringProperty(String.format("%d -> %d (%s)", r.preHealthScore(), r.postHealthScore(), r.smartDeltaSummary()));
        });
        colHealth.setPrefWidth(200);

        TableColumn<AuditDb.AuditRecord, String> colLedger = new TableColumn<>("SHA-256 Ledger Hash");
        colLedger.setCellValueFactory(data -> {
            String hash = data.getValue().recordHash();
            if (hash == null || hash.isBlank()) return new SimpleStringProperty("UNSEALED");
            return new SimpleStringProperty(hash.length() > 16 ? hash.substring(0, 16) + "..." : hash);
        });
        colLedger.setPrefWidth(150);

        TableColumn<AuditDb.AuditRecord, String> colSig = new TableColumn<>("RSA Digital Signature");
        colSig.setCellValueFactory(data -> {
            String s = data.getValue().digitalSignature();
            return new SimpleStringProperty(s.length() > 24 ? s.substring(0, 24) + "..." : s);
        });
        colSig.setPrefWidth(170);

        tblAuditHistory.getColumns().addAll(colId, colTime, colModel, colSerial, colCapacity, colStandard, colStatus, colHealth, colLedger, colSig);

        auditData = FXCollections.observableArrayList();
        filteredData = new FilteredList<>(auditData, p -> true);
        tblAuditHistory.setItems(filteredData);

        // Double-click row → export PDF
        tblAuditHistory.setRowFactory(tv -> {
            TableRow<AuditDb.AuditRecord> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) handleExportPdf();
            });
            return row;
        });

        cardTable.getChildren().addAll(toolbar, tblAuditHistory);
        container.getChildren().addAll(esgBanner, cardTable);
        return container;
    }

    // ── Tab 2: Security Activity Trail ───────────────────────────────────────
    @SuppressWarnings("unchecked")
    private VBox buildSecurityTrailTab() {
        VBox container = new VBox(14);
        container.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(container, Priority.ALWAYS);

        // Security Banner
        HBox securityBanner = new HBox(12);
        securityBanner.setAlignment(Pos.CENTER_LEFT);
        securityBanner.setStyle("-fx-background-color: linear-gradient(to right, #1E1B4B, #0F172A); -fx-border-color: #6366F1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 16;");

        Label lblSecBannerText = new Label("🛡️ FISMA / HIPAA / NIST SP 800-53 Tamper-Evident Security Log (SHA-256 Cryptographic Block Chaining Active)");
        lblSecBannerText.setStyle("-fx-text-fill: #C7D2FE; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region secRegion = new Region();
        HBox.setHgrow(secRegion, Priority.ALWAYS);

        Button btnVerifySecurityChain = new Button("🔒 Verify Security Event Chain");
        btnVerifySecurityChain.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnVerifySecurityChain.setTooltip(new Tooltip("Verify unbroken SHA-256 block hash chaining across all security & administrative events"));
        btnVerifySecurityChain.setOnAction(e -> handleVerifySecurityEventChain());

        securityBanner.getChildren().addAll(lblSecBannerText, secRegion, btnVerifySecurityChain);

        // Toolbar
        VBox cardTable = new VBox(12);
        cardTable.getStyleClass().add("card");
        VBox.setVgrow(cardTable, Priority.ALWAYS);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Filter security events, actors, actions...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterSecurityLog(newVal));

        ComboBox<String> cmbEventType = new ComboBox<>();
        cmbEventType.getItems().addAll("ALL EVENTS", "AUTH_LOGIN", "AUTH_LOGOUT", "AUTH_FAILED",
                "SESSION_LOCKED", "SESSION_UNLOCKED", "CONFIG_MODIFIED", "WIPE_COMPLETED", "DATA_EXPORT", "RBAC_ACCESS_DENIED");
        cmbEventType.setValue("ALL EVENTS");
        cmbEventType.setOnAction(e -> filterSecurityLog(txtSearch.getText(), cmbEventType.getValue()));

        Button btnRefresh = new Button("Refresh (F5)");
        btnRefresh.setOnAction(e -> loadSecurityHistory());

        toolbar.getChildren().addAll(txtSearch, cmbEventType, new Region(), btnRefresh);
        HBox.setHgrow(toolbar.getChildren().get(2), Priority.ALWAYS);

        // Table
        tblSecurityHistory = new TableView<>();
        VBox.setVgrow(tblSecurityHistory, Priority.ALWAYS);

        TableColumn<SecurityAuditRecord, Integer> colSecId = new TableColumn<>("ID");
        colSecId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()).asObject());
        colSecId.setPrefWidth(50);

        TableColumn<SecurityAuditRecord, String> colSecTime = new TableColumn<>("Timestamp");
        colSecTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp()));
        colSecTime.setPrefWidth(140);

        TableColumn<SecurityAuditRecord, String> colSecType = new TableColumn<>("Event Type");
        colSecType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().eventType()));
        colSecType.setPrefWidth(150);

        TableColumn<SecurityAuditRecord, String> colSecActor = new TableColumn<>("Actor / Officer");
        colSecActor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().actorName()));
        colSecActor.setPrefWidth(140);

        TableColumn<SecurityAuditRecord, String> colSecRole = new TableColumn<>("Role Tier");
        colSecRole.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().actorRole()));
        colSecRole.setPrefWidth(110);

        TableColumn<SecurityAuditRecord, String> colSecAction = new TableColumn<>("Action Summary & Scope");
        colSecAction.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().actionSummary()));
        colSecAction.setPrefWidth(280);

        TableColumn<SecurityAuditRecord, String> colSecTarget = new TableColumn<>("Target Resource");
        colSecTarget.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().targetResource()));
        colSecTarget.setPrefWidth(150);

        TableColumn<SecurityAuditRecord, String> colSecStatus = new TableColumn<>("Status");
        colSecStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colSecStatus.setPrefWidth(85);

        TableColumn<SecurityAuditRecord, String> colSecHash = new TableColumn<>("SHA-256 Event Hash");
        colSecHash.setCellValueFactory(data -> {
            String hash = data.getValue().eventHash();
            return new SimpleStringProperty(hash.length() > 16 ? hash.substring(0, 16) + "..." : hash);
        });
        colSecHash.setPrefWidth(150);

        tblSecurityHistory.getColumns().addAll(colSecId, colSecTime, colSecType, colSecActor, colSecRole, colSecAction, colSecTarget, colSecStatus, colSecHash);

        securityData = FXCollections.observableArrayList();
        filteredSecurityData = new FilteredList<>(securityData, p -> true);
        tblSecurityHistory.setItems(filteredSecurityData);

        cardTable.getChildren().addAll(toolbar, tblSecurityHistory);
        container.getChildren().addAll(securityBanner, cardTable);
        return container;
    }

    private void loadAuditHistory() {
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        auditData.setAll(records);

        EsgCalculator.EsgMetrics esg = EsgCalculator.calculateAggregate(records);
        lblEsgBannerText.setText(String.format(
                java.util.Locale.US,
                "🌱 Corporate ESG Impact: %.1f kg E-Waste Diverted | %.1f kg CO₂ Mitigated (~%.2f Tree Seedlings Equiv.) across %d sanitized devices",
                esg.eWasteDivertedKg(),
                esg.co2EmissionsSavedKg(),
                esg.treesEquivalent(),
                records.size()
        ));
    }

    private void loadSecurityHistory() {
        List<SecurityAuditRecord> records = SecurityAuditLogger.getAllEvents();
        securityData.setAll(records);
    }

    private void filterLog(String query) {
        if (query == null || query.isBlank()) {
            filteredData.setPredicate(p -> true);
            return;
        }
        String q = query.toLowerCase();
        filteredData.setPredicate(record ->
                record.driveModel().toLowerCase().contains(q) ||
                record.serialNumber().toLowerCase().contains(q) ||
                record.wipeStandard().toLowerCase().contains(q) ||
                record.status().toLowerCase().contains(q)
        );
    }

    private void filterSecurityLog(String query) {
        filterSecurityLog(query, "ALL EVENTS");
    }

    private void filterSecurityLog(String query, String selectedType) {
        String q = (query != null) ? query.toLowerCase() : "";
        boolean filterByType = (selectedType != null && !selectedType.equals("ALL EVENTS"));

        filteredSecurityData.setPredicate(record -> {
            boolean matchesType = !filterByType || record.eventType().equalsIgnoreCase(selectedType);
            boolean matchesQuery = q.isEmpty()
                    || record.eventType().toLowerCase().contains(q)
                    || record.actorName().toLowerCase().contains(q)
                    || record.actorRole().toLowerCase().contains(q)
                    || record.actionSummary().toLowerCase().contains(q)
                    || record.targetResource().toLowerCase().contains(q);
            return matchesType && matchesQuery;
        });
    }

    private void handleVerifySecurityEventChain() {
        var res = SecurityAuditLogger.verifySecurityLedgerIntegrity();
        showSecurityLedgerVerificationDialog(res);
    }

    public static void showSecurityLedgerVerificationDialog(SecurityAuditLogger.SecurityLedgerVerificationResult result) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Security Audit Trail — SHA-256 Block Chain Proof");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().add(ButtonType.CLOSE);
        dp.setStyle("-fx-background-color: #0F172A;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(650);

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(14, 16, 14, 16));

        Label lblBadge = new Label(result.isFullyValid() ? "🔒 100% TAMPER-PROOF" : "⚠️ INTEGRITY BREACH DETECTED");
        lblBadge.setStyle(result.isFullyValid()
                ? "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981; -fx-background-color: rgba(16,185,129,0.15); -fx-padding: 6 12; -fx-background-radius: 6px;"
                : "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #EF4444; -fx-background-color: rgba(239,68,68,0.15); -fx-padding: 6 12; -fx-background-radius: 6px;");

        Label lblTitle = new Label(result.isFullyValid()
                ? "Security Activity SHA-256 Block Chain Authenticated"
                : "Cryptographic Tampering / Anomaly Detected in Security Trail");
        lblTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        headerBox.getChildren().addAll(lblBadge, lblTitle);
        headerBox.setStyle(result.isFullyValid()
                ? "-fx-background-color: #13271F; -fx-background-radius: 8px; -fx-border-color: #059669; -fx-border-radius: 8px; -fx-border-width: 1px;"
                : "-fx-background-color: #311417; -fx-background-radius: 8px; -fx-border-color: #DC2626; -fx-border-radius: 8px; -fx-border-width: 1px;");

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.add(createLedgerTile("Total Security Events", String.valueOf(result.totalEventsChecked()), "#818CF8"), 0, 0);
        grid.add(createLedgerTile("Continuous Blocks", result.validChainLength() + " / " + result.totalEventsChecked(), result.isFullyValid() ? "#34D399" : "#F87171"), 1, 0);
        grid.add(createLedgerTile("Genesis Hash Root", result.genesisHash().substring(0, Math.min(16, result.genesisHash().length())) + "...", "#94A3B8"), 0, 1);
        grid.add(createLedgerTile("Latest Block Hash", result.latestBlockHash().substring(0, Math.min(16, result.latestBlockHash().length())) + "...", "#C084FC"), 1, 1);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        Label lblDetails = new Label(result.getSummaryMessage());
        lblDetails.setWrapText(true);
        lblDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1; -fx-line-spacing: 4px;");

        content.getChildren().addAll(headerBox, grid, lblDetails);

        if (!result.anomalies().isEmpty()) {
            VBox anomalyBox = new VBox(8);
            Label lblAnomTitle = new Label("Security Chain Anomalies Detected:");
            lblAnomTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FCA5A5;");
            anomalyBox.getChildren().add(lblAnomTitle);

            for (String a : result.anomalies()) {
                Label lblA = new Label("• " + a);
                lblA.setStyle("-fx-font-size: 11px; -fx-text-fill: #F87171;");
                anomalyBox.getChildren().add(lblA);
            }
            content.getChildren().add(anomalyBox);
        }

        dp.setContent(content);
        dialog.showAndWait();
    }

    private void handleExportEsgReport() {
        List<AuditDb.AuditRecord> records = AuditDb.getAllRecords();
        EsgCalculator.EsgMetrics esg = EsgCalculator.calculateAggregate(records);

        String report = String.format(
                java.util.Locale.US,
                """
                =======================================================
                🌱 ENTERPRISE ESG SUSTAINABILITY & CIRCULAR ECONOMY AUDIT
                Standard: ISO 14064-1 / GHG Protocol Scope 3 Compliant
                =======================================================
                Total Sanitized Assets for Circular Reuse: %d devices
                Total Storage Capacity Sanitized: %s
                -------------------------------------------------------
                E-Waste Diverted from Landfills: %.1f kg
                Scope 3 CO₂ Emissions Mitigated: %.1f kg CO₂e
                10-Year Tree Seedlings Equivalent: %.2f trees
                Manufacturing Power Conserved: %.1f kWh
                Circular Economy Disposal Index: 100%% Zero-Landfill
                -------------------------------------------------------
                Executive Compliance Proof:
                %s
                =======================================================
                """,
                records.size(),
                EsgCalculator.formatGb(esg.capacityGb()),
                esg.eWasteDivertedKg(),
                esg.co2EmissionsSavedKg(),
                esg.treesEquivalent(),
                esg.energySavedKwh(),
                esg.impactStatement()
        );

        var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
        SecurityAuditLogger.logExport(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "ESG Corporate Sustainability Report", "In-App Dialog");
        loadSecurityHistory();
        showAlert(Alert.AlertType.INFORMATION, "🌱 ESG Corporate Sustainability Audit Report", report);
    }

    private void handleShowComplianceSummary() {
        List<AuditDb.AuditRecord> records = new ArrayList<>(filteredData);
        if (records.isEmpty()) {
            records = AuditDb.getAllRecords();
        }

        AuditExporter.ComplianceSummary s = AuditExporter.generateComplianceSummary(records);

        String summaryText = String.format(
                java.util.Locale.US,
                """
                ══════════════════════════════════════════════════════════════
                📊 CONSOLIDATED ENTERPRISE SANITIZATION COMPLIANCE KPIS
                Standard: NIST SP 800-88 R1 / ISO 27040 / EU GDPR Art. 17
                ══════════════════════════════════════════════════════════════
                • Total Storage Assets Processed: %d devices
                • Successful Sanitizations:       %d devices (%.2f%%)
                • Failed Sanitization Attempts:   %d devices
                • Total Data Volume Sanitized:    %s (%d bytes)
                • Average Residual Shannon Score: %.4f bits/byte (Target: 0.000)
                • Earliest Logged Audit Entry:    %s
                • Latest Logged Audit Entry:      %s
                ──────────────────────────────────────────────────────────────
                🌱 Corporate ESG & Scope 3 Sustainability Impact:
                • E-Waste Diverted from Landfills: %.1f kg
                • Scope 3 CO₂ Emissions Mitigated: %.1f kg CO₂e
                • 10-Year Tree Seedlings Saved:    %.2f trees
                • Manufacturing Power Conserved:   %.1f kWh
                • Circular Disposition Index:      100%% Zero-Landfill E-Waste
                ══════════════════════════════════════════════════════════════
                """,
                s.totalDrivesProcessed(),
                s.successCount(),
                s.passRatePercent(),
                s.failureCount(),
                s.formattedTotalCapacity(),
                s.totalCapacityBytes(),
                s.averageResidualEntropy(),
                s.earliestRecordTimestamp(),
                s.latestRecordTimestamp(),
                s.esgImpact().eWasteDivertedKg(),
                s.esgImpact().co2EmissionsSavedKg(),
                s.esgImpact().treesEquivalent(),
                s.esgImpact().energySavedKwh()
        );

        showAlert(Alert.AlertType.INFORMATION, "📊 Executive Compliance & Sanitization KPIs", summaryText);
    }

    private void handleBatchExport(String format) {
        List<AuditDb.AuditRecord> records = new ArrayList<>(filteredData);
        if (records.isEmpty()) {
            records = AuditDb.getAllRecords();
        }
        if (records.isEmpty()) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification(
                    "No Records", "No audit records found to export.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sanitization Audit Report (" + format + ")");
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.now());

        switch (format.toUpperCase()) {
            case "CSV" -> {
                fileChooser.setInitialFileName("Sanitization_Audit_Report_" + timestamp + ".csv");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Comma Delimited (*.csv)", "*.csv"));
            }
            case "JSON" -> {
                fileChooser.setInitialFileName("Sanitization_Audit_Report_" + timestamp + ".json");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Data (*.json)", "*.json"));
            }
            case "EXCEL" -> {
                fileChooser.setInitialFileName("Sanitization_Audit_Report_" + timestamp + ".xml");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Spreadsheet XML (*.xml, *.xls)", "*.xml", "*.xls"));
            }
        }

        File targetFile = fileChooser.showSaveDialog(rootContainer.getScene() != null ? rootContainer.getScene().getWindow() : null);
        if (targetFile == null) return;

        final List<AuditDb.AuditRecord> exportList = records;
        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                switch (format.toUpperCase()) {
                    case "CSV" -> AuditExporter.exportToCsv(exportList, targetFile);
                    case "JSON" -> AuditExporter.exportToJson(exportList, targetFile);
                    case "EXCEL" -> AuditExporter.exportToExcelXml(exportList, targetFile);
                }
                return null;
            }
        };

        exportTask.setOnSucceeded(ev -> {
            var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
            SecurityAuditLogger.logExport(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), format + " Batch Export", targetFile.getAbsolutePath());
            loadSecurityHistory();
            nav.showNotification(
                    format + " Export Successful",
                    "Exported " + exportList.size() + " records to: " + targetFile.getName(),
                    com.sanitizer.gui.components.ToastNotification.ToastType.SUCCESS
            );
            showAlert(Alert.AlertType.INFORMATION, format + " Audit Export Complete",
                    "Audit log successfully exported (" + exportList.size() + " records):\n\n" + targetFile.getAbsolutePath());
        });

        exportTask.setOnFailed(ev -> {
            Throwable ex = exportTask.getException();
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification(
                    "Export Failed", ex != null ? ex.getMessage() : "Unknown export error",
                    com.sanitizer.gui.components.ToastNotification.ToastType.ERROR
            );
        });

        new Thread(exportTask, "audit-export-thread").start();
    }

    private void handleExportFullPackage() {
        List<AuditDb.AuditRecord> records = new ArrayList<>(filteredData);
        if (records.isEmpty()) {
            records = AuditDb.getAllRecords();
        }
        if (records.isEmpty()) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification(
                    "No Records", "No audit records found to export.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Target Folder for Full Audit Package");
        File targetDir = dirChooser.showDialog(rootContainer.getScene() != null ? rootContainer.getScene().getWindow() : null);
        if (targetDir == null) return;

        final List<AuditDb.AuditRecord> exportList = records;
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.now());

        File csvFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".csv");
        File jsonFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".json");
        File excelFile = new File(targetDir, "Sanitization_Audit_" + timestamp + ".xml");

        Task<Void> packageTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                AuditExporter.exportToCsv(exportList, csvFile);
                AuditExporter.exportToJson(exportList, jsonFile);
                AuditExporter.exportToExcelXml(exportList, excelFile);
                return null;
            }
        };

        packageTask.setOnSucceeded(ev -> {
            var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
            SecurityAuditLogger.logExport(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Full Audit Package", targetDir.getAbsolutePath());
            loadSecurityHistory();
            nav.showNotification(
                    "Audit Package Exported",
                    "Generated CSV, JSON, and Excel reports in " + targetDir.getName(),
                    com.sanitizer.gui.components.ToastNotification.ToastType.SUCCESS
            );
            showAlert(Alert.AlertType.INFORMATION, "Audit Package Export Complete",
                    "Full Compliance Audit Package exported successfully:\n\n"
                    + "• CSV:   " + csvFile.getAbsolutePath() + "\n"
                    + "• JSON:  " + jsonFile.getAbsolutePath() + "\n"
                    + "• Excel: " + excelFile.getAbsolutePath());
        });

        packageTask.setOnFailed(ev -> {
            Throwable ex = packageTask.getException();
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification(
                    "Package Export Failed", ex != null ? ex.getMessage() : "Unknown error",
                    com.sanitizer.gui.components.ToastNotification.ToastType.ERROR
            );
        });

        new Thread(packageTask, "audit-package-thread").start();
    }

    private void handleExportPdf() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("No Selection",
                    "Select an audit log entry from the table first.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        Task<String> pdfTask = new Task<>() {
            @Override
            protected String call() {
                return CertificateGenerator.generateCertificate(selected);
            }
        };
        pdfTask.setOnSucceeded(ev -> {
            String pdfPath = pdfTask.getValue();
            if (pdfPath != null) {
                var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
                SecurityAuditLogger.logExport(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "PDF Sanitization Certificate", pdfPath);
                loadSecurityHistory();
                nav.showNotification("PDF Exported",
                        "Certificate generated at: " + pdfPath, com.sanitizer.gui.components.ToastNotification.ToastType.SUCCESS);
                showAlert(Alert.AlertType.INFORMATION, "PDF Certificate Exported",
                        "Sanitization Proof Certificate created successfully:\n" + pdfPath);
            } else {
                com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("Export Error",
                        "Failed to generate PDF Certificate.", com.sanitizer.gui.components.ToastNotification.ToastType.ERROR);
            }
        });
        pdfTask.setOnFailed(ev ->
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("Export Error",
                    pdfTask.getException().getMessage(), com.sanitizer.gui.components.ToastNotification.ToastType.ERROR));
        new Thread(pdfTask, "audit-pdf-export-thread").start();
    }

    private void handleExportQuarantineReport() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("No Selection",
                    "Select an audit record to generate Quarantine Report.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        Task<String> qTask = new Task<>() {
            @Override
            protected String call() {
                com.sanitizer.quarantine.QuarantineRecord qRecord = com.sanitizer.quarantine.QuarantineEngine.assessHardwareFailure(
                        selected.driveModel(),
                        selected.serialNumber(),
                        selected.capacity(),
                        null,
                        selected.wipeStandard(),
                        "Defective Blocks / Non-Compliant Sanitization Attempt",
                        List.of(),
                        selected.preHealthScore(),
                        selected.postHealthScore(),
                        selected.smartDeltaSummary()
                );
                return com.sanitizer.quarantine.QuarantineReportGenerator.generatePdfReport(qRecord);
            }
        };

        qTask.setOnSucceeded(ev -> {
            String qPath = qTask.getValue();
            if (qPath != null) {
                var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
                SecurityAuditLogger.logExport(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Quarantine Destruction Order", qPath);
                loadSecurityHistory();
                nav.showNotification("Quarantine Report Exported",
                        "Order generated at: " + qPath, com.sanitizer.gui.components.ToastNotification.ToastType.SUCCESS);
                showAlert(Alert.AlertType.INFORMATION, "Defective Hardware Quarantine Order",
                        "Physical Destruction Order generated successfully:\n\n" + qPath);
            }
        });

        qTask.setOnFailed(ev ->
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("Report Error",
                    qTask.getException().getMessage(), com.sanitizer.gui.components.ToastNotification.ToastType.ERROR));
        new Thread(qTask, "quarantine-pdf-thread").start();
    }

    private void handleVerifySignature() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("No Selection",
                    "Select an audit record to verify signature.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        String payload = selected.driveModel() + "|" + selected.serialNumber() + "|" + selected.capacity() + "|" + selected.wipeStandard() + "|" + selected.status();
        boolean valid = CryptoSigner.verifySignature(payload, selected.digitalSignature());

        if (valid) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("Signature Authenticated",
                    "SHA256withRSA signature matches payload!", com.sanitizer.gui.components.ToastNotification.ToastType.SUCCESS);
            showAlert(Alert.AlertType.INFORMATION, "Signature Authenticated",
                    "VERIFICATION SUCCESSFUL\n\nThe SHA256withRSA signature matches the record payload.\nThis record is authentic and tamper-free!");
        } else {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("Signature Mismatch",
                    "Verification failed!", com.sanitizer.gui.components.ToastNotification.ToastType.ERROR);
            showAlert(Alert.AlertType.ERROR, "Verification Failed",
                    "SIGNATURE MISMATCH\n\nThe digital signature could not be verified against the current keypair or record payload.");
        }
    }

    private void handleVerifyLedgerIntegrity() {
        var result = AuditDb.verifyDatabaseIntegrity();
        var nav = com.sanitizer.gui.navigation.NavigationManager.getInstance();
        SecurityAuditLogger.logEvent(SecurityAuditLogger.EVENT_LEDGER_VERIFIED, nav.getOfficerName(), nav.getAgencyId(), nav.getRole(),
                "Sanitization DB Ledger Verification: " + (result.isFullyValid() ? "PASSED (100% Tamper Proof)" : "FAILED"), "sanitizer_history.db",
                result.isFullyValid() ? "SUCCESS" : "FAILED");
        loadSecurityHistory();
        showLedgerVerificationDialog(result);
    }

    public static void showLedgerVerificationDialog(com.sanitizer.db.LedgerIntegrityEngine.LedgerVerificationResult result) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("SQLite Audit Ledger — Cryptographic Integrity Proof");
        dialog.setHeaderText(null);

        DialogPane dp = dialog.getDialogPane();
        dp.getButtonTypes().add(ButtonType.CLOSE);
        dp.setStyle("-fx-background-color: #0F172A;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setPrefWidth(650);

        // Header Status Banner
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(14, 16, 14, 16));

        Label lblBadge = new Label(result.isFullyValid() ? "🔒 100% TAMPER-PROOF" : "⚠️ INTEGRITY BREACH DETECTED");
        lblBadge.setStyle(result.isFullyValid()
                ? "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10B981; -fx-background-color: rgba(16,185,129,0.15); -fx-padding: 6 12; -fx-background-radius: 6px;"
                : "-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #EF4444; -fx-background-color: rgba(239,68,68,0.15); -fx-padding: 6 12; -fx-background-radius: 6px;");

        Label lblTitle = new Label(result.isFullyValid()
                ? "SHA-256 Ledger Chain Authenticated"
                : "Cryptographic Tampering / Anomaly Detected");
        lblTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        headerBox.getChildren().addAll(lblBadge, lblTitle);
        headerBox.setStyle(result.isFullyValid()
                ? "-fx-background-color: #13271F; -fx-background-radius: 8px; -fx-border-color: #059669; -fx-border-radius: 8px; -fx-border-width: 1px;"
                : "-fx-background-color: #311417; -fx-background-radius: 8px; -fx-border-color: #DC2626; -fx-border-radius: 8px; -fx-border-width: 1px;");

        // Metrics Grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        grid.add(createLedgerTile("Total Records Verified", String.valueOf(result.totalRecordsChecked()), "#38BDF8"), 0, 0);
        grid.add(createLedgerTile("Valid Block Chains", result.validChainLength() + " / " + result.totalRecordsChecked(), result.isFullyValid() ? "#34D399" : "#F87171"), 1, 0);
        grid.add(createLedgerTile("Genesis Root", result.genesisHash().substring(0, Math.min(16, result.genesisHash().length())) + "...", "#94A3B8"), 0, 1);
        grid.add(createLedgerTile("Latest Block Hash", result.latestBlockHash().substring(0, Math.min(16, result.latestBlockHash().length())) + "...", "#A78BFA"), 1, 1);

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        // Status Details
        Label lblDetails = new Label(result.getSummaryMessage());
        lblDetails.setWrapText(true);
        lblDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1; -fx-line-spacing: 4px;");

        content.getChildren().addAll(headerBox, grid, lblDetails);

        if (result.hasAnomalies()) {
            VBox anomalyBox = new VBox(8);
            Label lblAnomTitle = new Label("Forensic Defect & Tamper Inspection:");
            lblAnomTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FCA5A5;");
            anomalyBox.getChildren().add(lblAnomTitle);

            for (var anom : result.anomalies()) {
                VBox item = new VBox(4);
                item.setPadding(new Insets(8, 12, 8, 12));
                item.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 6px; -fx-border-color: #DC2626; -fx-border-radius: 6px;");

                Label lblType = new Label("[" + anom.type() + "] Record ID: " + anom.recordId() + " (S/N: " + anom.serialNumber() + ")");
                lblType.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #F87171;");

                Label lblDesc = new Label(anom.description());
                lblDesc.setWrapText(true);
                lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #E2E8F0;");

                Label lblHashes = new Label("Expected: " + anom.expectedHash() + "\nFound:    " + anom.actualHash());
                lblHashes.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 10px; -fx-text-fill: #94A3B8;");

                item.getChildren().addAll(lblType, lblDesc, lblHashes);
                anomalyBox.getChildren().add(item);
            }
            content.getChildren().add(anomalyBox);
        }

        dp.setContent(content);
        dialog.showAndWait();
    }

    private static VBox createLedgerTile(String title, String value, String valueColor) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8px; -fx-border-color: #334155; -fx-border-radius: 8px;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Label lblVal = new Label(value);
        lblVal.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + valueColor + ";");

        box.getChildren().addAll(lblTitle, lblVal);
        return box;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setOnShown(ev -> {
            Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.requestFocus();
        });
        alert.showAndWait();
    }
}
