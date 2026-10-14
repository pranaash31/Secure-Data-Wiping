package com.sanitizer.gui.viewmodel;

import com.sanitizer.detector.SmartDiagnostics;
import com.sanitizer.detector.UsbDetector;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel for DiskDiagnosticView.
 * Encapsulates raw SMART telemetry, domain threshold rules, and formatting math,
 * decoupling UI components from direct hardware queries and calculations.
 * Also manages clean lifecycle registration with UsbDetector.
 */
public class DiskDiagnosticViewModel {

    // Domain Constants
    public static final long SECTOR_SIZE_BYTES = 512L;
    public static final double HOURS_PER_YEAR = 24.0 * 365.25;
    public static final int WEAR_WARNING_THRESHOLD_PERCENT = 50;
    public static final int TEMP_CRITICAL_THRESHOLD_CELSIUS = 60;
    public static final int TEMP_ELEVATED_THRESHOLD_CELSIUS = 48;

    // Drive List & Selected Drive
    private final ObservableList<UsbDetector.UsbDriveInfo> drives = FXCollections.observableArrayList();
    private final ObjectProperty<UsbDetector.UsbDriveInfo> selectedDrive = new SimpleObjectProperty<>();
    private final ObjectProperty<SmartDiagnostics.SmartReport> currentReport = new SimpleObjectProperty<>();

    // Specifications Properties
    private final StringProperty model = new SimpleStringProperty("Select a drive");
    private final StringProperty serial = new SimpleStringProperty("--");
    private final StringProperty capacity = new SimpleStringProperty("--");
    private final StringProperty totalBlocks = new SimpleStringProperty("-- sectors");
    private final StringProperty path = new SimpleStringProperty("--");

    // Health Score Properties
    private final StringProperty healthValue = new SimpleStringProperty("-- / 100");
    private final StringProperty healthValueStyle = new SimpleStringProperty("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
    private final StringProperty healthScoreBadge = new SimpleStringProperty("HEALTHY");
    private final StringProperty healthScoreBadgeStyle = new SimpleStringProperty("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #ECFDF5; -fx-text-fill: #059669;");
    private final StringProperty healthDesc = new SimpleStringProperty("Zero critical failure indicators detected.");

    // Thermal Telemetry Properties
    private final StringProperty tempValue = new SimpleStringProperty("-- °C");
    private final StringProperty tempValueStyle = new SimpleStringProperty("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");
    private final StringProperty thermalBadge = new SimpleStringProperty("NORMAL");
    private final StringProperty thermalBadgeStyle = new SimpleStringProperty("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #EFF6FF; -fx-text-fill: #2563EB;");
    private final StringProperty tempDesc = new SimpleStringProperty("Operating within safe thermal limits (<48°C).");

    // Runtime & Lifespan Properties
    private final StringProperty hoursValue = new SimpleStringProperty("-- hrs");
    private final StringProperty hoursDesc = new SimpleStringProperty("Operational runtime lifespan.");
    private final DoubleProperty yearsRuntime = new SimpleDoubleProperty(0.0);

    // Sector Defects Properties
    private final StringProperty sectorsValue = new SimpleStringProperty("0 BAD / 0 REALLOC");
    private final StringProperty sectorsValueStyle = new SimpleStringProperty("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
    private final StringProperty sectorsDesc = new SimpleStringProperty("Media surface is clean. No bad sectors.");

    // Flash Wear Leveling Properties
    private final StringProperty wearValue = new SimpleStringProperty("100% Life Left");
    private final StringProperty wearValueStyle = new SimpleStringProperty("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
    private final DoubleProperty wearProgress = new SimpleDoubleProperty(1.0);
    private final StringProperty wearDesc = new SimpleStringProperty("NAND endurance optimal.");

    // Pre-Wipe Health Assessment Banner Properties
    private final BooleanProperty isWipeAllowed = new SimpleBooleanProperty(true);
    private final StringProperty preWipeVerdict = new SimpleStringProperty("PRE-WIPE HEALTH ASSESSMENT: AUTOMATICALLY VERIFIED");
    private final StringProperty preWipeVerdictStyle = new SimpleStringProperty("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #065F46;");
    private final StringProperty preWipeDetails = new SimpleStringProperty("Drive health is certified optimal. Drive is cleared for high-speed low-level block sanitization.");
    private final StringProperty preWipeBoxStyle = new SimpleStringProperty("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");

    // Interface Anomaly Properties
    private final BooleanProperty interfaceAnomalyVisible = new SimpleBooleanProperty(false);
    private final StringProperty interfaceAnomalyBoxStyle = new SimpleStringProperty("-fx-background-color: #FFF7ED; -fx-border-color: #FED7AA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
    private final StringProperty anomalyTitle = new SimpleStringProperty("INTERFACE ANOMALY DETECTED — USB PORT / CABLE DEGRADATION");
    private final StringProperty anomalyTitleStyle = new SimpleStringProperty("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #92400E;");
    private final StringProperty anomalyDiagnosis = new SimpleStringProperty("");
    private final StringProperty anomalyDiagnosisStyle = new SimpleStringProperty("-fx-font-size: 11px; -fx-text-fill: #B45309;");
    private final StringProperty anomalyPrompt = new SimpleStringProperty("");
    private final StringProperty anomalyPromptStyle = new SimpleStringProperty("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #C2410C;");

    // SMART Telemetry Table Items
    private final ObservableList<SmartDiagnostics.SmartAttribute> smartAttributes = FXCollections.observableArrayList();

    // Hardware listener registration state
    private boolean isListening = false;
    private final Consumer<List<UsbDetector.UsbDriveInfo>> usbListener = this::onHardwareDrivesDetected;

    public DiskDiagnosticViewModel() {
        // Selection listener to update diagnostics automatically when selected drive changes
        selectedDrive.addListener((obs, oldDrive, newDrive) -> {
            if (newDrive != null) {
                loadSelectedDriveDiagnostics(newDrive);
            }
        });
    }

    // ──────────────── Lifecycle Management ────────────────

    /**
     * Registers the hardware USB listener with UsbDetector if not already registered.
     */
    public synchronized void startListening() {
        if (!isListening) {
            UsbDetector.registerListener(usbListener);
            isListening = true;
        }
    }

    /**
     * Unregisters the hardware USB listener to prevent leaks when navigating away.
     */
    public synchronized void stopListening() {
        if (isListening) {
            UsbDetector.unregisterListener(usbListener);
            isListening = false;
        }
    }

    public synchronized boolean isListening() {
        return isListening;
    }

    public void dispose() {
        stopListening();
    }

    private void onHardwareDrivesDetected(List<UsbDetector.UsbDriveInfo> detectedDrives) {
        if (Platform.isFxApplicationThread()) {
            updateDrives(detectedDrives);
        } else {
            try {
                Platform.runLater(() -> updateDrives(detectedDrives));
            } catch (IllegalStateException e) {
                // Headless test environment
                updateDrives(detectedDrives);
            }
        }
    }

    /**
     * Fetches current connected drives from UsbDetector and refreshes the internal model.
     */
    public void refreshDrives() {
        List<UsbDetector.UsbDriveInfo> detectedDrives = UsbDetector.getConnectedUsbDrives();
        updateDrives(detectedDrives);
    }

    /**
     * Updates the drives list and selection state.
     */
    public void updateDrives(List<UsbDetector.UsbDriveInfo> detectedDrives) {
        UsbDetector.UsbDriveInfo current = selectedDrive.get();
        drives.setAll(detectedDrives);

        if (detectedDrives.isEmpty()) {
            selectedDrive.set(null);
            currentReport.set(null);
            resetToEmptyState();
        } else {
            if (current != null && detectedDrives.contains(current)) {
                selectedDrive.set(current);
                loadSelectedDriveDiagnostics(current);
            } else {
                selectedDrive.set(detectedDrives.get(0));
                loadSelectedDriveDiagnostics(detectedDrives.get(0));
            }
        }
    }

    public void selectDrive(UsbDetector.UsbDriveInfo drive) {
        if (drive != null && !drive.equals(selectedDrive.get())) {
            selectedDrive.set(drive);
        } else if (drive != null) {
            loadSelectedDriveDiagnostics(drive);
        }
    }

    // ──────────────── Domain Conversions & Math ────────────────

    public static long calculateTotalBlocks(long sizeBytes) {
        if (sizeBytes <= 0) return 0L;
        return sizeBytes / SECTOR_SIZE_BYTES;
    }

    public static double calculateYearsRuntime(long powerOnHours) {
        if (powerOnHours <= 0) return 0.1;
        return Math.max(0.1, (double) powerOnHours / HOURS_PER_YEAR);
    }

    public static boolean hasSectorDefects(int badBlocks, int reallocatedSectors) {
        return badBlocks > 0 || reallocatedSectors > 0;
    }

    public static boolean isFlashWearCritical(int wearLevelingPercent) {
        return wearLevelingPercent < WEAR_WARNING_THRESHOLD_PERCENT;
    }

    public static String evaluateThermalMessage(int tempCelsius) {
        if (tempCelsius >= TEMP_CRITICAL_THRESHOLD_CELSIUS) {
            return "CRITICAL THERMAL LEVEL: Drive will auto-pause if wiped now.";
        } else if (tempCelsius >= TEMP_ELEVATED_THRESHOLD_CELSIUS) {
            return "ELEVATED TEMPERATURE: Operating near upper thermal threshold.";
        } else {
            return "Operating within safe thermal limits (<48°C).";
        }
    }

    // ──────────────── Diagnostics Loader ────────────────

    public void reloadCurrentDriveDiagnostics() {
        UsbDetector.UsbDriveInfo drive = selectedDrive.get();
        if (drive != null) {
            loadSelectedDriveDiagnostics(drive);
        }
    }

    public void loadSelectedDriveDiagnostics(UsbDetector.UsbDriveInfo drive) {
        if (drive == null) {
            resetToEmptyState();
            return;
        }

        // 1. Hardware Specs
        model.set(drive.model() != null ? drive.model() : "Unknown");
        serial.set(drive.serial() != null ? drive.serial() : "--");
        capacity.set(drive.formattedSize() + " (" + String.format("%,d", drive.sizeBytes()) + " bytes)");

        long blocks = calculateTotalBlocks(drive.sizeBytes());
        totalBlocks.set(String.format("%,d sectors", blocks));
        path.set(drive.systemPath() != null ? drive.systemPath() : "--");

        // 2. Perform Deep SMART Diagnostics Inspection
        SmartDiagnostics.SmartReport report = SmartDiagnostics.inspectDrive(drive);
        currentReport.set(report);

        if (report == null) {
            return;
        }

        // 3. Health Score & Status
        SmartDiagnostics.HealthScoreResult health = report.healthScore();
        if (health != null) {
            healthValue.set(health.score() + " / 100");
            healthScoreBadge.set(health.status().name());
            healthScoreBadgeStyle.set(String.format(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: %s; -fx-text-fill: %s;",
                    health.status().getBgColor(), health.status().getTextColor()
            ));
            healthValueStyle.set(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", health.status().getTextColor()));
            healthDesc.set(health.recommendation());

            boolean wipePermitted = health.isWipePermittedWithoutOverride();
            isWipeAllowed.set(wipePermitted);
            if (wipePermitted) {
                preWipeBoxStyle.set("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
                preWipeVerdict.set("PRE-WIPE HEALTH ASSESSMENT: PASSED (" + health.score() + "/100 - " + health.status().name() + ")");
                preWipeVerdictStyle.set("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #065F46;");
                preWipeDetails.set(health.recommendation());
            } else {
                preWipeBoxStyle.set("-fx-background-color: #FEF2F2; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
                preWipeVerdict.set("PRE-WIPE HEALTH ASSESSMENT: CRITICAL WARNING (" + health.score() + "/100 - " + health.status().name() + ")");
                preWipeVerdictStyle.set("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #991B1B;");
                preWipeDetails.set(String.join(" | ", health.warnings()) + ". " + health.recommendation());
            }
        }

        // 4. Temperature & Thermal Status
        tempValue.set(report.temperatureCelsius() + " °C");
        SmartDiagnostics.ThermalStatus tStatus = report.thermalStatus();
        if (tStatus != null) {
            thermalBadge.set(tStatus.name());
            thermalBadgeStyle.set(String.format(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: %s; -fx-text-fill: %s;",
                    tStatus.getBgColor(), tStatus.getTextColor()
            ));
            tempValueStyle.set(String.format("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;", tStatus.getTextColor()));
        }
        tempDesc.set(evaluateThermalMessage(report.temperatureCelsius()));

        // 5. Power-On Hours & Lifespan
        hoursValue.set(String.format("%,d hrs", report.powerOnHours()));
        double yearsEst = calculateYearsRuntime(report.powerOnHours());
        yearsRuntime.set(yearsEst);
        hoursDesc.set(String.format("Approx. %.1f years runtime | %d power cycles", yearsEst, report.powerCycleCount()));

        // 6. Sector Defects (Bad Blocks & Reallocated Sectors)
        sectorsValue.set(String.format("%d BAD / %d REALLOC", report.badBlocks(), report.reallocatedSectors()));
        if (hasSectorDefects(report.badBlocks(), report.reallocatedSectors())) {
            sectorsValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            sectorsDesc.set("Sector defects detected on media surface.");
        } else {
            sectorsValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
            sectorsDesc.set("Media surface is clean. Zero bad sectors detected.");
        }

        // 7. Flash Wear Leveling
        wearValue.set(report.wearLevelingPercent() + "% Remaining Life");
        wearProgress.set(report.wearLevelingPercent() / 100.0);
        if (isFlashWearCritical(report.wearLevelingPercent())) {
            wearDesc.set("High flash wear. Consider device retirement.");
            wearValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #D97706;");
        } else {
            wearDesc.set("NAND flash endurance optimal for rewriting.");
            wearValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        }

        // 8. Interface Anomaly & Port Degradation Banner
        SmartDiagnostics.InterfaceAnomalyResult iface = report.interfaceAnomaly();
        if (iface != null && iface.isDegraded()) {
            String severityLabel = iface.severity().getLabel();
            String titleSuffix = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL
                    ? " — USB PORT / CABLE DEGRADATION [" + severityLabel + "]"
                    : " — INTERMITTENT LINK ANOMALY [" + severityLabel + "]";
            String borderColor = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL ? "#FCA5A5" : "#FED7AA";
            String bgColor     = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL ? "#FEF2F2" : "#FFF7ED";
            String titleColor  = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL ? "#991B1B" : "#92400E";
            String diagColor   = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL ? "#DC2626" : "#B45309";
            String promptColor = iface.severity() == SmartDiagnostics.InterfaceSeverity.CRITICAL ? "#B91C1C" : "#C2410C";

            interfaceAnomalyBoxStyle.set("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
            anomalyTitle.set("⚠️  INTERFACE ANOMALY DETECTED" + titleSuffix);
            anomalyTitleStyle.set("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
            anomalyDiagnosis.set(String.format("CRC Errors (ID 199): %d  |  Command Timeouts (ID 188): %d  —  %s",
                    iface.crcErrors(), iface.commandTimeouts(), iface.rootCauseDiagnosis()));
            anomalyDiagnosisStyle.set("-fx-font-size: 11px; -fx-text-fill: " + diagColor + ";");
            anomalyPrompt.set("→  " + iface.userPrompt());
            anomalyPromptStyle.set("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + promptColor + ";");
            interfaceAnomalyVisible.set(true);
        } else {
            interfaceAnomalyVisible.set(false);
        }

        // 9. SMART Table Data
        smartAttributes.setAll(report.attributes());
    }

    public void resetToEmptyState() {
        healthValue.set("-- / 100");
        healthScoreBadge.set("NO DEVICE");
        healthScoreBadgeStyle.set("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #FEF2F2; -fx-text-fill: #EF4444;");
        healthDesc.set("Connect a USB drive to run hardware diagnostics.");

        tempValue.set("-- °C");
        thermalBadge.set("OFFLINE");
        thermalBadgeStyle.set("-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-background-color: #F1F5F9; -fx-text-fill: #64748B;");
        tempDesc.set("No temperature sensor telemetry available.");

        hoursValue.set("-- hrs");
        yearsRuntime.set(0.0);
        hoursDesc.set("Operational runtime lifespan.");

        sectorsValue.set("0 BAD / 0 REALLOC");
        sectorsValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        sectorsDesc.set("Media surface is clean. Zero bad sectors detected.");

        wearValue.set("--% Life");
        wearValueStyle.set("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #059669;");
        wearProgress.set(0.0);
        wearDesc.set("NAND endurance optimal.");

        preWipeBoxStyle.set("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 14 20;");
        lblPreWipeVerdictWaitingState();
        isWipeAllowed.set(false);

        interfaceAnomalyVisible.set(false);

        model.set("No drive connected");
        serial.set("--");
        capacity.set("--");
        totalBlocks.set("--");
        path.set("--");
        smartAttributes.clear();
    }

    private void lblPreWipeVerdictWaitingState() {
        preWipeVerdict.set("PRE-WIPE HEALTH ASSESSMENT: WAITING FOR DEVICE");
        preWipeVerdictStyle.set("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        preWipeDetails.set("Insert a storage device to evaluate Pre-Wipe Health Score & thermal safety limits.");
    }

    // ──────────────── Property Getters ────────────────

    public ObservableList<UsbDetector.UsbDriveInfo> getDrives() { return drives; }
    public ObjectProperty<UsbDetector.UsbDriveInfo> selectedDriveProperty() { return selectedDrive; }
    public ObjectProperty<SmartDiagnostics.SmartReport> currentReportProperty() { return currentReport; }

    public StringProperty modelProperty() { return model; }
    public StringProperty serialProperty() { return serial; }
    public StringProperty capacityProperty() { return capacity; }
    public StringProperty totalBlocksProperty() { return totalBlocks; }
    public StringProperty pathProperty() { return path; }

    public StringProperty healthValueProperty() { return healthValue; }
    public StringProperty healthValueStyleProperty() { return healthValueStyle; }
    public StringProperty healthScoreBadgeProperty() { return healthScoreBadge; }
    public StringProperty healthScoreBadgeStyleProperty() { return healthScoreBadgeStyle; }
    public StringProperty healthDescProperty() { return healthDesc; }

    public StringProperty tempValueProperty() { return tempValue; }
    public StringProperty tempValueStyleProperty() { return tempValueStyle; }
    public StringProperty thermalBadgeProperty() { return thermalBadge; }
    public StringProperty thermalBadgeStyleProperty() { return thermalBadgeStyle; }
    public StringProperty tempDescProperty() { return tempDesc; }

    public StringProperty hoursValueProperty() { return hoursValue; }
    public StringProperty hoursDescProperty() { return hoursDesc; }
    public DoubleProperty yearsRuntimeProperty() { return yearsRuntime; }

    public StringProperty sectorsValueProperty() { return sectorsValue; }
    public StringProperty sectorsValueStyleProperty() { return sectorsValueStyle; }
    public StringProperty sectorsDescProperty() { return sectorsDesc; }

    public StringProperty wearValueProperty() { return wearValue; }
    public StringProperty wearValueStyleProperty() { return wearValueStyle; }
    public DoubleProperty wearProgressProperty() { return wearProgress; }
    public StringProperty wearDescProperty() { return wearDesc; }

    public BooleanProperty isWipeAllowedProperty() { return isWipeAllowed; }
    public StringProperty preWipeVerdictProperty() { return preWipeVerdict; }
    public StringProperty preWipeVerdictStyleProperty() { return preWipeVerdictStyle; }
    public StringProperty preWipeDetailsProperty() { return preWipeDetails; }
    public StringProperty preWipeBoxStyleProperty() { return preWipeBoxStyle; }

    public BooleanProperty interfaceAnomalyVisibleProperty() { return interfaceAnomalyVisible; }
    public StringProperty interfaceAnomalyBoxStyleProperty() { return interfaceAnomalyBoxStyle; }
    public StringProperty anomalyTitleProperty() { return anomalyTitle; }
    public StringProperty anomalyTitleStyleProperty() { return anomalyTitleStyle; }
    public StringProperty anomalyDiagnosisProperty() { return anomalyDiagnosis; }
    public StringProperty anomalyDiagnosisStyleProperty() { return anomalyDiagnosisStyle; }
    public StringProperty anomalyPromptProperty() { return anomalyPrompt; }
    public StringProperty anomalyPromptStyleProperty() { return anomalyPromptStyle; }

    public ObservableList<SmartDiagnostics.SmartAttribute> getSmartAttributes() { return smartAttributes; }
}
