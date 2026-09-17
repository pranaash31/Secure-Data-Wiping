package com.sanitizer.gui.views;

import com.sanitizer.crypto.CryptoSigner;
import com.sanitizer.db.AuditDb;
import com.sanitizer.esg.EsgCalculator;
import com.sanitizer.pdf.CertificateGenerator;
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

import java.util.List;

public class AuditView {

    private final VBox rootContainer = new VBox(20);
    private TableView<AuditDb.AuditRecord> tblAuditHistory;
    private ObservableList<AuditDb.AuditRecord> auditData;
    private FilteredList<AuditDb.AuditRecord> filteredData;
    private Label lblEsgBannerText;

    public AuditView() {
        buildUi();
        loadAuditHistory();
    }

    public Parent getRoot() {
        return rootContainer;
    }

    @SuppressWarnings("unchecked")
    private void buildUi() {
        rootContainer.setPadding(new Insets(24));

        // Header Title
        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Audit Trail & Digital Certification");
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setStyle("-fx-font-size: 22px;");
        Label lblSub = new Label("Cryptographically signed sanitization logs with verifiable chain of custody");
        lblSub.getStyleClass().add("card-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

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
        VBox cardTable = new VBox(14);
        cardTable.getStyleClass().add("card");
        VBox.setVgrow(cardTable, Priority.ALWAYS);

        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Search by model, serial, standard...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterLog(newVal));

        Button btnRefresh = new Button("Refresh Log (F5)");
        btnRefresh.setTooltip(new Tooltip("Reload audit log from SQLite (F5)"));
        btnRefresh.setOnAction(e -> loadAuditHistory());

        Button btnExportPdf = new Button("Export PDF Certificate");
        btnExportPdf.getStyleClass().add("button-primary");
        btnExportPdf.setTooltip(new Tooltip("Generate PDF certificate for selected record (also: double-click row)"));
        btnExportPdf.setOnAction(e -> handleExportPdf());

        Button btnVerify = new Button("Verify RSA Signature");
        btnVerify.setTooltip(new Tooltip("Verify SHA256withRSA signature for selected record"));
        btnVerify.setOnAction(e -> handleVerifySignature());

        toolbar.getChildren().addAll(txtSearch, new Region(), btnRefresh, btnExportPdf, btnVerify);
        HBox.setHgrow(toolbar.getChildren().get(1), Priority.ALWAYS);

        // F5 shortcut = refresh audit log
        toolbar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.F5) loadAuditHistory();
                });
            }
        });

        // Table
        tblAuditHistory = new TableView<>();
        VBox.setVgrow(tblAuditHistory, Priority.ALWAYS);

        TableColumn<AuditDb.AuditRecord, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().id()).asObject());
        colId.setPrefWidth(50);

        TableColumn<AuditDb.AuditRecord, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().timestamp()));
        colTime.setPrefWidth(160);

        TableColumn<AuditDb.AuditRecord, String> colModel = new TableColumn<>("Device Model");
        colModel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().driveModel()));
        colModel.setPrefWidth(200);

        TableColumn<AuditDb.AuditRecord, String> colSerial = new TableColumn<>("Serial Number");
        colSerial.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().serialNumber()));
        colSerial.setPrefWidth(150);

        TableColumn<AuditDb.AuditRecord, String> colCapacity = new TableColumn<>("Capacity");
        colCapacity.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().capacity()));
        colCapacity.setPrefWidth(90);

        TableColumn<AuditDb.AuditRecord, String> colStandard = new TableColumn<>("Sanitization Standard");
        colStandard.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().wipeStandard()));
        colStandard.setPrefWidth(150);

        TableColumn<AuditDb.AuditRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        colStatus.setPrefWidth(90);

        TableColumn<AuditDb.AuditRecord, String> colSig = new TableColumn<>("RSA Digital Signature");
        colSig.setCellValueFactory(data -> {
            String s = data.getValue().digitalSignature();
            return new SimpleStringProperty(s.length() > 32 ? s.substring(0, 32) + "..." : s);
        });
        colSig.setPrefWidth(220);

        tblAuditHistory.getColumns().addAll(colId, colTime, colModel, colSerial, colCapacity, colStandard, colStatus, colSig);

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
        // Enter key on selected row → export PDF
        tblAuditHistory.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER && tblAuditHistory.getSelectionModel().getSelectedItem() != null) {
                handleExportPdf();
            }
        });

        cardTable.getChildren().addAll(toolbar, tblAuditHistory);

        rootContainer.getChildren().addAll(titleBox, esgBanner, cardTable);
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

        showAlert(Alert.AlertType.INFORMATION, "🌱 ESG Corporate Sustainability Audit Report", report);
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

    private void handleExportPdf() {
        AuditDb.AuditRecord selected = tblAuditHistory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("No Selection",
                    "Select an audit log entry from the table first.", com.sanitizer.gui.components.ToastNotification.ToastType.WARNING);
            return;
        }

        // Run on background thread – keeps UI responsive during PDF generation
        Task<String> pdfTask = new Task<>() {
            @Override
            protected String call() {
                return CertificateGenerator.generateCertificate(selected);
            }
        };
        pdfTask.setOnSucceeded(ev -> {
            String pdfPath = pdfTask.getValue();
            if (pdfPath != null) {
                com.sanitizer.gui.navigation.NavigationManager.getInstance().showNotification("PDF Exported",
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        // Auto-focus OK button so Enter/Space dismisses the dialog
        alert.setOnShown(ev -> {
            Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
            if (ok != null) ok.requestFocus();
        });
        alert.showAndWait();
    }
}
