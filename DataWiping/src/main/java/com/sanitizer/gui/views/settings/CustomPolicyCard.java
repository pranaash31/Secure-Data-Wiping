package com.sanitizer.gui.views.settings;

import com.sanitizer.audit.SecurityAuditLogger;
import com.sanitizer.gui.components.ToastNotification.ToastType;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.i18n.I18n;
import com.sanitizer.policy.WipePass;
import com.sanitizer.policy.WipePatternType;
import com.sanitizer.policy.WipePolicy;
import com.sanitizer.service.ConfigurationService;
import com.sanitizer.service.SystemConfigurationService;
import com.sanitizer.session.SessionContext;
import com.sanitizer.session.UserRole;
import com.sanitizer.engine.WipeVerifier;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Settings Card encapsulating Custom Wipe Standard & Pattern Builder.
 */
public class CustomPolicyCard extends VBox {

    private final ConfigurationService configService;

    private ComboBox<WipePolicy> cmbPolicySelector;
    private TextField txtPolicyName;
    private TextField txtPolicyOrg;
    private TextField txtPolicyCode;
    private TextArea txtPolicyDesc;
    private ComboBox<WipeVerifier.VerificationMode> cmbPolicyVerifyMode;
    private VBox passesListContainer;
    private Label lblSequencePreview;
    private Label lblPolicyTypeBadge;
    private Button btnDeletePolicy;

    private final List<WipePass> currentEditingPasses = new ArrayList<>();

    public CustomPolicyCard() {
        this(SystemConfigurationService.getInstance());
    }

    public CustomPolicyCard(ConfigurationService configService) {
        super(18);
        this.configService = (configService != null) ? configService : SystemConfigurationService.getInstance();
        getStyleClass().add("card");
        buildUi();
    }

    private SessionContext currentSession() {
        var nav = NavigationManager.getInstance();
        return new SessionContext(nav.getOfficerName(), nav.getAgencyId(), nav.getRole());
    }

    private void buildUi() {
        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("🛠️ [STANDARDS & POLICIES]");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8B5CF6; " +
                "-fx-background-color: rgba(139,92,246,0.12); -fx-background-radius: 6px; -fx-padding: 4 10;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Custom Wipe Standard & Pattern Builder");
        title.getStyleClass().add("settings-section-title");
        Label subtitle = new Label("Configure multi-pass sanitization algorithms, custom byte patterns (0xAA, 0x55, etc.), international presets & JSON profiles.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBadge, titleBox);

        // ── Top Controls: Policy Selector & Actions ──
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblSelect = new Label("Active Standard / Policy:");
        lblSelect.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #334155;");

        cmbPolicySelector = new ComboBox<>();
        cmbPolicySelector.setStyle("-fx-font-size: 12px; -fx-pref-width: 320px;");
        refreshPolicySelectorItems();

        lblPolicyTypeBadge = new Label("STANDARD PRESET");
        lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 4 8; -fx-background-radius: 4px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button btnNew = new Button("+ New Policy");
        btnNew.getStyleClass().add("button-primary");
        btnNew.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnNew.setOnAction(e -> handleCreateNewPolicy());

        Button btnSave = new Button("💾 Save Policy");
        btnSave.getStyleClass().add("button-secondary");
        btnSave.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnSave.setOnAction(e -> handleSaveCurrentPolicy());

        Button btnSetDefault = new Button("⭐ Set Active Default");
        btnSetDefault.setStyle("-fx-font-size: 11px; -fx-padding: 6 12; -fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnSetDefault.setOnAction(e -> handleSetActiveDefaultPolicy());

        btnDeletePolicy = new Button("🗑️ Delete");
        btnDeletePolicy.setStyle("-fx-font-size: 11px; -fx-padding: 6 10; -fx-text-fill: #EF4444; -fx-background-color: #FEE2E2; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnDeletePolicy.setOnAction(e -> handleDeleteCurrentPolicy());

        Button btnExport = new Button("📤 Export JSON");
        btnExport.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnExport.setOnAction(e -> handleExportPolicyJson());

        Button btnImport = new Button("📥 Import JSON");
        btnImport.setStyle("-fx-font-size: 11px; -fx-padding: 6 12;");
        btnImport.setOnAction(e -> handleImportPolicyJson());

        topBar.getChildren().addAll(lblSelect, cmbPolicySelector, lblPolicyTypeBadge, topSpacer, btnNew, btnSave, btnSetDefault, btnExport, btnImport, btnDeletePolicy);

        // ── Form Fields: Metadata ──
        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(10);

        Label lblName = new Label("Policy Name:");
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyName = new TextField();
        txtPolicyName.setPromptText("e.g. Enterprise 4-Pass Sanitization");

        Label lblOrg = new Label("Organization / Authority:");
        lblOrg.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyOrg = new TextField();
        txtPolicyOrg.setPromptText("e.g. Acme Corp Cyber Security Team");

        Label lblCode = new Label("Standard Code:");
        lblCode.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyCode = new TextField();
        txtPolicyCode.setPromptText("e.g. ACME_SEC_WIPE_v1");

        Label lblVerify = new Label("Post-Wipe Sampling & Verification:");
        lblVerify.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        cmbPolicyVerifyMode = new ComboBox<>();
        cmbPolicyVerifyMode.getItems().addAll(WipeVerifier.VerificationMode.values());
        cmbPolicyVerifyMode.setMaxWidth(Double.MAX_VALUE);

        Label lblDesc = new Label("Description & Scope:");
        lblDesc.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        txtPolicyDesc = new TextArea();
        txtPolicyDesc.setPrefRowCount(2);
        txtPolicyDesc.setPromptText("Detailed specifications for this sanitization policy profile...");

        formGrid.add(lblName, 0, 0);
        formGrid.add(txtPolicyName, 1, 0);
        formGrid.add(lblOrg, 2, 0);
        formGrid.add(txtPolicyOrg, 3, 0);

        formGrid.add(lblCode, 0, 1);
        formGrid.add(txtPolicyCode, 1, 1);
        formGrid.add(lblVerify, 2, 1);
        formGrid.add(cmbPolicyVerifyMode, 3, 1);

        formGrid.add(lblDesc, 0, 2);
        formGrid.add(txtPolicyDesc, 1, 2, 3, 1);

        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints(260);
        ColumnConstraints c2 = new ColumnConstraints(220);
        ColumnConstraints c3 = new ColumnConstraints(260);
        formGrid.getColumnConstraints().addAll(c0, c1, c2, c3);

        // ── Passes Pattern Editor ──
        VBox passesSection = new VBox(10);
        passesSection.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 14; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

        HBox passesHeader = new HBox(12);
        passesHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblPassesTitle = new Label("Sanitization Passes Sequence & Bit Patterns:");
        lblPassesTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1E293B;");

        lblSequencePreview = new Label("0x00");
        lblSequencePreview.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-padding: 3 8; -fx-background-radius: 4px;");

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Button btnAddPass = new Button("+ Add Overwrite Pass");
        btnAddPass.getStyleClass().add("button-secondary");
        btnAddPass.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
        btnAddPass.setOnAction(e -> handleAddPass());

        passesHeader.getChildren().addAll(lblPassesTitle, lblSequencePreview, pSpacer, btnAddPass);

        passesListContainer = new VBox(8);

        passesSection.getChildren().addAll(passesHeader, new Separator(), passesListContainer);

        // Populate initial selection
        cmbPolicySelector.setOnAction(e -> {
            WipePolicy selected = cmbPolicySelector.getValue();
            if (selected != null) {
                loadPolicyIntoForm(selected);
            }
        });

        if (!cmbPolicySelector.getItems().isEmpty()) {
            WipePolicy def = configService.getDefaultPolicy();
            cmbPolicySelector.setValue(def);
            loadPolicyIntoForm(def);
        }

        getChildren().addAll(header, new Separator(), topBar, formGrid, passesSection);
    }

    private void refreshPolicySelectorItems() {
        WipePolicy prev = cmbPolicySelector.getValue();
        List<WipePolicy> all = configService.getAllPolicies();
        cmbPolicySelector.setItems(FXCollections.observableArrayList(all));
        if (prev != null && all.contains(prev)) {
            cmbPolicySelector.setValue(prev);
        } else if (!all.isEmpty()) {
            cmbPolicySelector.setValue(configService.getDefaultPolicy());
        }
    }

    private void loadPolicyIntoForm(WipePolicy policy) {
        if (policy == null) return;

        txtPolicyName.setText(policy.getName());
        txtPolicyOrg.setText(policy.getOrganization());
        txtPolicyCode.setText(policy.getStandardCode());
        txtPolicyDesc.setText(policy.getDescription());
        cmbPolicyVerifyMode.setValue(policy.getVerificationMode() != null ? policy.getVerificationMode() : WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT);

        if (policy.isSystemBuiltin()) {
            lblPolicyTypeBadge.setText("INTERNATIONAL STANDARD (PRESET)");
            lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #EDE9FE; -fx-text-fill: #7C3AED; -fx-padding: 4 8; -fx-background-radius: 4px;");
            btnDeletePolicy.setDisable(true);
        } else {
            lblPolicyTypeBadge.setText("CUSTOM COMPANY POLICY");
            lblPolicyTypeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 4 8; -fx-background-radius: 4px;");
            btnDeletePolicy.setDisable(false);
        }

        currentEditingPasses.clear();
        if (policy.getPasses() != null) {
            for (WipePass p : policy.getPasses()) {
                currentEditingPasses.add(new WipePass(p.getPassNumber(), p.getPatternType(), p.getCustomByteValue(), p.getDescription()));
            }
        }
        rebuildPassRows();
    }

    private void rebuildPassRows() {
        passesListContainer.getChildren().clear();

        for (int i = 0; i < currentEditingPasses.size(); i++) {
            final int index = i;
            WipePass pass = currentEditingPasses.get(i);
            pass.setPassNumber(i + 1);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 8 12; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-radius: 6px;");

            Label lblPassNum = new Label("Pass " + (i + 1) + ":");
            lblPassNum.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-min-width: 55px;");

            ComboBox<WipePatternType> cmbType = new ComboBox<>();
            cmbType.getItems().addAll(WipePatternType.values());
            cmbType.setValue(pass.getPatternType());
            cmbType.setStyle("-fx-font-size: 11px; -fx-pref-width: 220px;");

            TextField txtHex = new TextField(pass.getPatternType() == WipePatternType.CUSTOM_BYTE ? String.format("0x%02X", pass.getCustomByteValue()) : "0x00");
            txtHex.setPromptText("Hex (0xAA)");
            txtHex.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-pref-width: 85px;");
            txtHex.setVisible(pass.getPatternType() == WipePatternType.CUSTOM_BYTE);
            txtHex.setManaged(pass.getPatternType() == WipePatternType.CUSTOM_BYTE);

            Label badge = new Label(pass.getPatternHex());
            badge.setStyle("-fx-font-family: monospace; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-padding: 3 6; -fx-background-radius: 4px;");

            TextField txtDesc = new TextField(pass.getDescription());
            txtDesc.setPromptText("Pass description...");
            txtDesc.setStyle("-fx-font-size: 11px;");
            HBox.setHgrow(txtDesc, Priority.ALWAYS);

            cmbType.setOnAction(e -> {
                WipePatternType sel = cmbType.getValue();
                pass.setPatternType(sel);
                boolean isCustom = (sel == WipePatternType.CUSTOM_BYTE);
                txtHex.setVisible(isCustom);
                txtHex.setManaged(isCustom);
                badge.setText(pass.getPatternHex());
                updateSequencePreview();
            });

            txtHex.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    String clean = newVal.replace("0x", "").replace("0X", "").trim();
                    if (!clean.isEmpty()) {
                        int val = Integer.parseInt(clean, 16);
                        pass.setCustomByteValue(val);
                        badge.setText(pass.getPatternHex());
                        updateSequencePreview();
                    }
                } catch (Exception ignored) {}
            });

            txtDesc.textProperty().addListener((obs, o, n) -> pass.setDescription(n));

            Button btnUp = new Button("▲");
            btnUp.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            btnUp.setDisable(index == 0);
            btnUp.setOnAction(e -> {
                Collections.swap(currentEditingPasses, index, index - 1);
                rebuildPassRows();
            });

            Button btnDown = new Button("▼");
            btnDown.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            btnDown.setDisable(index == currentEditingPasses.size() - 1);
            btnDown.setOnAction(e -> {
                Collections.swap(currentEditingPasses, index, index + 1);
                rebuildPassRows();
            });

            Button btnRemove = new Button("✖");
            btnRemove.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 2 6;");
            btnRemove.setDisable(currentEditingPasses.size() <= 1);
            btnRemove.setOnAction(e -> {
                currentEditingPasses.remove(index);
                rebuildPassRows();
            });

            row.getChildren().addAll(lblPassNum, cmbType, txtHex, badge, txtDesc, btnUp, btnDown, btnRemove);
            passesListContainer.getChildren().add(row);
        }

        updateSequencePreview();
    }

    private void updateSequencePreview() {
        if (currentEditingPasses.isEmpty()) {
            lblSequencePreview.setText("Empty");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentEditingPasses.size(); i++) {
            if (i > 0) sb.append(" ➔ ");
            sb.append(currentEditingPasses.get(i).getPatternHex());
        }
        lblSequencePreview.setText(sb.toString());
    }

    private void handleAddPass() {
        currentEditingPasses.add(new WipePass(currentEditingPasses.size() + 1, WipePatternType.ZERO_FILL, 0x00, "Zero Fill Pass"));
        rebuildPassRows();
    }

    private void handleCreateNewPolicy() {
        WipePolicy newPolicy = new WipePolicy(
                UUID.randomUUID().toString(),
                "New Custom Wipe Policy",
                "Custom enterprise sanitization policy profile",
                "Internal Security Team",
                "CUSTOM_POLICY",
                List.of(
                        new WipePass(1, WipePatternType.CUSTOM_BYTE, 0xAA, "Pass 1: Pattern 0xAA"),
                        new WipePass(2, WipePatternType.CUSTOM_BYTE, 0x55, "Pass 2: Pattern 0x55"),
                        new WipePass(3, WipePatternType.PSEUDO_RANDOM, -1, "Pass 3: CSPRNG Random"),
                        new WipePass(4, WipePatternType.ZERO_FILL, 0x00, "Pass 4: Final Zero Verification")
                ),
                true,
                WipeVerifier.VerificationMode.FAST_SAMPLE_5_PERCENT,
                false,
                "1.0.0",
                "User"
        );
        configService.savePolicy(newPolicy, currentSession());
        refreshPolicySelectorItems();
        cmbPolicySelector.setValue(newPolicy);
        loadPolicyIntoForm(newPolicy);
        NavigationManager.getInstance().showNotification("New Policy Created", "Configure passes and click Save.", ToastType.SUCCESS);
    }

    private void handleSaveCurrentPolicy() {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.POLICY_EDIT)) {
            nav.showAccessDeniedDialog("Wipe Policy Configuration", "SUPERVISOR / CHIEF AUDITOR");
            SecurityAuditLogger.logAccessDenied(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Wipe Policy Manager", "POLICY_EDIT");
            return;
        }

        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;

        String name = txtPolicyName.getText().trim();
        if (name.isEmpty()) {
            NavigationManager.getInstance().showNotification("Validation Error", I18n.get("dialog.policy.name_empty"), ToastType.ERROR);
            return;
        }

        if (current.isSystemBuiltin()) {
            // Fork into a custom policy if editing a built-in standard
            WipePolicy customFork = new WipePolicy(
                    UUID.randomUUID().toString(),
                    name + " (Custom)",
                    txtPolicyDesc.getText().trim(),
                    txtPolicyOrg.getText().trim(),
                    txtPolicyCode.getText().trim(),
                    new ArrayList<>(currentEditingPasses),
                    true,
                    cmbPolicyVerifyMode.getValue(),
                    false,
                    "1.0.0",
                    "Custom"
            );
            configService.savePolicy(customFork, currentSession());
            refreshPolicySelectorItems();
            cmbPolicySelector.setValue(customFork);
            loadPolicyIntoForm(customFork);
            NavigationManager.getInstance().showNotification("Policy Forked & Saved", "Saved as custom policy profile: " + customFork.getName(), ToastType.SUCCESS);
            return;
        }

        current.setName(name);
        current.setOrganization(txtPolicyOrg.getText().trim());
        current.setStandardCode(txtPolicyCode.getText().trim());
        current.setDescription(txtPolicyDesc.getText().trim());
        current.setVerificationMode(cmbPolicyVerifyMode.getValue());
        current.setPasses(new ArrayList<>(currentEditingPasses));

        configService.savePolicy(current, currentSession());
        refreshPolicySelectorItems();
        cmbPolicySelector.setValue(current);
        NavigationManager.getInstance().showNotification("Policy Saved", I18n.get("dialog.policy.saved"), ToastType.SUCCESS);
    }

    private void handleDeleteCurrentPolicy() {
        var nav = NavigationManager.getInstance();
        if (!nav.hasPermission(UserRole.Permission.POLICY_EDIT)) {
            nav.showAccessDeniedDialog("Wipe Policy Configuration", "SUPERVISOR / CHIEF AUDITOR");
            SecurityAuditLogger.logAccessDenied(nav.getOfficerName(), nav.getAgencyId(), nav.getRole(), "Wipe Policy Manager", "POLICY_EDIT");
            return;
        }

        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null || current.isSystemBuiltin()) return;

        boolean deleted = configService.deletePolicy(current.getId(), currentSession());
        if (deleted) {
            refreshPolicySelectorItems();
            loadPolicyIntoForm(configService.getDefaultPolicy());
            NavigationManager.getInstance().showNotification("Policy Deleted", I18n.get("dialog.policy.deleted"), ToastType.WARNING);
        }
    }

    private void handleSetActiveDefaultPolicy() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;
        configService.setDefaultPolicy(current);
        NavigationManager.getInstance().showNotification("Default Standard Updated",
                current.getName() + " is now set as the active default wiping standard.", ToastType.SUCCESS);
    }

    private void handleExportPolicyJson() {
        WipePolicy current = cmbPolicySelector.getValue();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Wipe Policy Configuration Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Policy Profile (*.json)", "*.json"));
        chooser.setInitialFileName(current.getName().toLowerCase().replaceAll("[^a-z0-9]", "_") + "_policy.json");
        File dest = chooser.showSaveDialog(null);

        if (dest != null) {
            try {
                configService.exportPolicyToFile(current, dest);
                NavigationManager.getInstance().showNotification("Policy Exported",
                        "Exported policy profile to: " + dest.getName(), ToastType.SUCCESS);
            } catch (Exception ex) {
                NavigationManager.getInstance().showNotification("Export Error",
                        "Failed to export policy: " + ex.getMessage(), ToastType.ERROR);
            }
        }
    }

    private void handleImportPolicyJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Wipe Policy Configuration Profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Policy Profile (*.json)", "*.json"));
        File src = chooser.showOpenDialog(null);

        if (src != null) {
            try {
                WipePolicy imported = configService.importPolicyFromFile(src);
                configService.savePolicy(imported, currentSession());
                refreshPolicySelectorItems();
                cmbPolicySelector.setValue(imported);
                loadPolicyIntoForm(imported);
                NavigationManager.getInstance().showNotification("Policy Imported",
                        "Successfully imported: " + imported.getName() + " (" + imported.getPassCount() + " passes)",
                        ToastType.SUCCESS);
            } catch (Exception ex) {
                NavigationManager.getInstance().showNotification("Import Error",
                        "Failed to import JSON policy: " + ex.getMessage(), ToastType.ERROR);
            }
        }
    }
}
