package com.sanitizer.gui.components;

import com.sanitizer.detector.DeviceType;
import com.sanitizer.detector.ThermalPolicy;
import com.sanitizer.detector.ThermalPolicyManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.StrokeLineCap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Real-Time Thermal Sparkline & Temperature Graph Component.
 * Visualizes live time-series temperature curves, threshold guidelines,
 * peak/average thermal statistics, and milestone markers for Auto-Pause and Cooldown events.
 */
public class ThermalGraphComponent extends VBox {

    public record ThermalPoint(long timestampMillis, int tempCelsius, String timeLabel) {}

    public record ThermalEvent(long timestampMillis, int tempCelsius, String type, String label, String timeLabel) {}

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_DATA_POINTS = 120;

    private final List<ThermalPoint> points = new ArrayList<>();
    private final List<ThermalEvent> events = new ArrayList<>();

    private ThermalPolicy policy;

    // Stat Labels
    private final Label lblCurrentTemp = new Label("-- °C");
    private final Label lblCurrentStatusBadge = new Label("NORMAL");
    private final Label lblPeakTemp = new Label("Peak: -- °C");
    private final Label lblAvgTemp = new Label("Avg: -- °C");
    private final Label lblMinTemp = new Label("Min: -- °C");
    private final Label lblPauseEvents = new Label("Pauses: 0");
    private final Label lblPolicyBadge = new Label("Policy: USB (55°C / 42°C)");

    // Rendering Canvas
    private final Canvas canvas = new Canvas(600, 160);

    private int peakTemp = -1;
    private int minTemp = 999;
    private double sumTemp = 0;
    private int sampleCount = 0;
    private int pauseCount = 0;

    public ThermalGraphComponent() {
        this(ThermalPolicyManager.getInstance().getPolicy(DeviceType.USB_FLASH));
    }

    public ThermalGraphComponent(ThermalPolicy initialPolicy) {
        this.policy = (initialPolicy != null) ? initialPolicy : ThermalPolicyManager.getInstance().getPolicy(DeviceType.USB_FLASH);
        buildUi();
        redraw();
    }

    private void buildUi() {
        setSpacing(10);
        setPadding(new Insets(14, 16, 14, 16));
        getStyleClass().add("card");
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        // ── Top Header & Stat Ribbon ──────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconBadge = new Label("📈 LIVE THERMAL GRAPH");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB; " +
                "-fx-background-color: #EFF6FF; -fx-background-radius: 6px; -fx-padding: 4 8;");

        HBox currentBox = new HBox(6);
        currentBox.setAlignment(Pos.CENTER_LEFT);
        lblCurrentTemp.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        lblCurrentStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 2 6; -fx-background-radius: 4px;");
        currentBox.getChildren().addAll(lblCurrentTemp, lblCurrentStatusBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblPeakTemp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444; -fx-background-color: #FEF2F2; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblAvgTemp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3B82F6; -fx-background-color: #EFF6FF; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblMinTemp.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981; -fx-background-color: #ECFDF5; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblPauseEvents.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7C3AED; -fx-background-color: #F5F3FF; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblPolicyBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 3 8; -fx-background-radius: 6px;");

        header.getChildren().addAll(iconBadge, currentBox, spacer, lblPeakTemp, lblAvgTemp, lblMinTemp, lblPauseEvents, lblPolicyBadge);

        // ── Canvas Container ─────────────────────────────────────────────
        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        canvasContainer.setPadding(new Insets(6));

        // Auto resize canvas width with layout
        widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.doubleValue() > 60) {
                canvas.setWidth(newVal.doubleValue() - 50);
                redraw();
            }
        });

        // ── Legend Ribbon ────────────────────────────────────────────────
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setStyle("-fx-padding: 2 4;");

        Label legNormal = createLegendItem("#10B981", "Normal Range");
        Label legElevated = createLegendItem("#F59E0B", "Elevated Thermal Zone");
        Label legPause = createLegendItem("#EF4444", "Auto-Pause Safeguard");
        Label legEvent = createLegendItem("#7C3AED", "⏸ Cooldown Milestone");

        Region legSpacer = new Region();
        HBox.setHgrow(legSpacer, Priority.ALWAYS);

        Label lblRange = new Label("Scale: 20°C – 90°C");
        lblRange.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        legend.getChildren().addAll(legNormal, legElevated, legPause, legEvent, legSpacer, lblRange);

        getChildren().addAll(header, canvasContainer, legend);
    }

    private Label createLegendItem(String colorHex, String text) {
        Label lbl = new Label("● " + text);
        lbl.setStyle(String.format("-fx-font-size: 10px; -fx-text-fill: %s; -fx-font-weight: bold;", colorHex));
        return lbl;
    }

    /**
     * Appends a new live temperature sample to the time-series curve.
     */
    public synchronized void addSample(int tempCelsius) {
        long now = System.currentTimeMillis();
        String timeStr = LocalTime.now().format(TIME_FMT);

        points.add(new ThermalPoint(now, tempCelsius, timeStr));
        if (points.size() > MAX_DATA_POINTS) {
            points.remove(0);
        }

        // Stats calculations
        sampleCount++;
        sumTemp += tempCelsius;
        if (tempCelsius > peakTemp) peakTemp = tempCelsius;
        if (tempCelsius < minTemp) minTemp = tempCelsius;

        updateStats(tempCelsius);

        if (Platform.isFxApplicationThread()) {
            redraw();
        } else {
            Platform.runLater(this::redraw);
        }
    }

    /**
     * Records a critical thermal event (e.g. AUTO_PAUSE, RESUME, WARNING).
     */
    public synchronized void recordEvent(int tempCelsius, String type, String label) {
        long now = System.currentTimeMillis();
        String timeStr = LocalTime.now().format(TIME_FMT);
        ThermalEvent ev = new ThermalEvent(now, tempCelsius, type, label, timeStr);
        events.add(ev);

        if ("AUTO_PAUSE".equalsIgnoreCase(type)) {
            pauseCount++;
        }

        // Also add point if points list is empty or current differs
        if (points.isEmpty() || points.get(points.size() - 1).tempCelsius() != tempCelsius) {
            points.add(new ThermalPoint(now, tempCelsius, timeStr));
            if (points.size() > MAX_DATA_POINTS) points.remove(0);
        }

        updateStats(tempCelsius);

        if (Platform.isFxApplicationThread()) {
            redraw();
        } else {
            Platform.runLater(this::redraw);
        }
    }

    public synchronized void setPolicy(ThermalPolicy policy) {
        if (policy == null) return;
        this.policy = policy;
        lblPolicyBadge.setText(String.format("Policy: %s (%d°C / %d°C)",
                policy.deviceType().getShortBadge(), policy.autoPauseCelsius(), policy.resumeCelsius()));
        if (Platform.isFxApplicationThread()) {
            redraw();
        } else {
            Platform.runLater(this::redraw);
        }
    }

    public synchronized void reset() {
        points.clear();
        events.clear();
        peakTemp = -1;
        minTemp = 999;
        sumTemp = 0;
        sampleCount = 0;
        pauseCount = 0;

        lblCurrentTemp.setText("-- °C");
        lblCurrentStatusBadge.setText("NORMAL");
        lblCurrentStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 2 6; -fx-background-radius: 4px;");
        lblPeakTemp.setText("Peak: -- °C");
        lblAvgTemp.setText("Avg: -- °C");
        lblMinTemp.setText("Min: -- °C");
        lblPauseEvents.setText("Pauses: 0");

        redraw();
    }

    private void updateStats(int latestTemp) {
        lblCurrentTemp.setText(latestTemp + " °C");
        lblPeakTemp.setText("Peak: " + peakTemp + " °C");
        lblMinTemp.setText("Min: " + (minTemp == 999 ? "--" : minTemp + " °C"));
        double avg = sampleCount > 0 ? (sumTemp / sampleCount) : latestTemp;
        lblAvgTemp.setText(String.format("Avg: %.1f °C", avg));
        lblPauseEvents.setText("Pauses: " + pauseCount);

        // Status badge
        if (latestTemp >= policy.autoPauseCelsius()) {
            lblCurrentStatusBadge.setText("CRITICAL (PAUSED)");
            lblCurrentStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 2 6; -fx-background-radius: 4px;");
        } else if (latestTemp >= policy.warningCelsius()) {
            lblCurrentStatusBadge.setText("ELEVATED");
            lblCurrentStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #FEF3C7; -fx-text-fill: #D97706; -fx-padding: 2 6; -fx-background-radius: 4px;");
        } else {
            lblCurrentStatusBadge.setText("NORMAL");
            lblCurrentStatusBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-padding: 2 6; -fx-background-radius: 4px;");
        }
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);

        double paddingLeft = 45;
        double paddingRight = 20;
        double paddingTop = 15;
        double paddingBottom = 25;

        double graphW = w - paddingLeft - paddingRight;
        double graphH = h - paddingTop - paddingBottom;

        if (graphW <= 10 || graphH <= 10) return;

        // Temperature Scale: 20°C to 90°C
        double minDisplayTemp = 20.0;
        double maxDisplayTemp = 90.0;

        // 1. Draw Grid Lines & Y-Axis Labels
        gc.setLineWidth(1.0);
        gc.setStroke(Color.rgb(226, 232, 240, 0.8));
        gc.setFill(Color.rgb(148, 163, 184));
        gc.setFont(javafx.scene.text.Font.font(9));

        int[] gridTemps = {20, 40, 60, 80};
        for (int gt : gridTemps) {
            double y = paddingTop + graphH - ((gt - minDisplayTemp) / (maxDisplayTemp - minDisplayTemp) * graphH);
            gc.strokeLine(paddingLeft, y, paddingLeft + graphW, y);
            gc.fillText(gt + "°C", paddingLeft - 32, y + 3);
        }

        // 2. Draw Auto-Pause Reference Threshold Line (Red Dashed)
        double pauseY = paddingTop + graphH - ((policy.autoPauseCelsius() - minDisplayTemp) / (maxDisplayTemp - minDisplayTemp) * graphH);
        gc.save();
        gc.setStroke(Color.rgb(239, 68, 68, 0.85));
        gc.setLineWidth(1.5);
        gc.setLineDashes(4, 4);
        gc.strokeLine(paddingLeft, pauseY, paddingLeft + graphW, pauseY);
        gc.setFill(Color.rgb(220, 38, 38));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 9));
        gc.fillText("Auto-Pause: " + policy.autoPauseCelsius() + "°C", paddingLeft + 8, pauseY - 3);
        gc.restore();

        // 3. Draw Safe Resume Reference Threshold Line (Green Dashed)
        double resumeY = paddingTop + graphH - ((policy.resumeCelsius() - minDisplayTemp) / (maxDisplayTemp - minDisplayTemp) * graphH);
        gc.save();
        gc.setStroke(Color.rgb(16, 185, 129, 0.75));
        gc.setLineWidth(1.2);
        gc.setLineDashes(3, 3);
        gc.strokeLine(paddingLeft, resumeY, paddingLeft + graphW, resumeY);
        gc.setFill(Color.rgb(5, 150, 105));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.NORMAL, 9));
        gc.fillText("Resume: " + policy.resumeCelsius() + "°C", paddingLeft + graphW - 85, resumeY - 3);
        gc.restore();

        List<ThermalPoint> ptsCopy;
        List<ThermalEvent> evsCopy;
        synchronized (this) {
            ptsCopy = new ArrayList<>(points);
            evsCopy = new ArrayList<>(events);
        }

        if (ptsCopy.isEmpty()) {
            gc.setFill(Color.rgb(148, 163, 184));
            gc.setFont(javafx.scene.text.Font.font(11));
            gc.fillText("Awaiting live thermal telemetry stream...", paddingLeft + (graphW / 2) - 110, paddingTop + (graphH / 2));
            return;
        }

        // 4. Calculate Coordinates for Points
        int count = ptsCopy.size();
        double[] xs = new double[count];
        double[] ys = new double[count];

        for (int i = 0; i < count; i++) {
            ThermalPoint p = ptsCopy.get(i);
            xs[i] = (count == 1) ? paddingLeft + (graphW / 2) : paddingLeft + ((double) i / (count - 1)) * graphW;
            double clampedTemp = Math.max(minDisplayTemp, Math.min(maxDisplayTemp, p.tempCelsius()));
            ys[i] = paddingTop + graphH - ((clampedTemp - minDisplayTemp) / (maxDisplayTemp - minDisplayTemp) * graphH);
        }

        // 5. Fill Area Under Curve (Gradient)
        gc.save();
        gc.beginPath();
        gc.moveTo(xs[0], paddingTop + graphH);
        for (int i = 0; i < count; i++) {
            gc.lineTo(xs[i], ys[i]);
        }
        gc.lineTo(xs[count - 1], paddingTop + graphH);
        gc.closePath();

        LinearGradient areaGrad = new LinearGradient(
                0, paddingTop, 0, paddingTop + graphH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(239, 68, 68, 0.35)),
                new Stop(0.4, Color.rgb(245, 158, 11, 0.20)),
                new Stop(1.0, Color.rgb(37, 99, 235, 0.05))
        );
        gc.setFill(areaGrad);
        gc.fill();
        gc.restore();

        // 6. Stroke The Main Thermal Curve
        gc.save();
        gc.setLineWidth(2.5);
        gc.setLineCap(StrokeLineCap.ROUND);
        LinearGradient strokeGrad = new LinearGradient(
                0, paddingTop, 0, paddingTop + graphH, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(220, 38, 38)),
                new Stop(0.4, Color.rgb(217, 119, 6)),
                new Stop(1.0, Color.rgb(37, 99, 235))
        );
        gc.setStroke(strokeGrad);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) {
            gc.lineTo(xs[i], ys[i]);
        }
        gc.stroke();
        gc.restore();

        // 7. Draw Sample Dots & Event Markers
        for (int i = 0; i < count; i++) {
            ThermalPoint p = ptsCopy.get(i);
            double x = xs[i];
            double y = ys[i];

            // Render dot for latest point or peak point
            if (i == count - 1 || p.tempCelsius() == peakTemp) {
                Color dotColor = p.tempCelsius() >= policy.autoPauseCelsius() ? Color.rgb(239, 68, 68)
                        : (p.tempCelsius() >= policy.warningCelsius() ? Color.rgb(245, 158, 11) : Color.rgb(37, 99, 235));

                gc.setFill(Color.WHITE);
                gc.fillOval(x - 4, y - 4, 8, 8);
                gc.setStroke(dotColor);
                gc.setLineWidth(2.0);
                gc.strokeOval(x - 4, y - 4, 8, 8);

                // Value label on peak and latest point
                gc.setFill(dotColor);
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
                gc.fillText(p.tempCelsius() + "°C", x - 10, y - 8);
            }
        }

        // 8. Render Milestone Pins for Events
        for (ThermalEvent ev : evsCopy) {
            // Find closest x
            for (int i = 0; i < count; i++) {
                if (Math.abs(ptsCopy.get(i).timestampMillis() - ev.timestampMillis()) < 3000 || i == count - 1) {
                    double ex = xs[i];
                    double ey = ys[i];

                    gc.save();
                    boolean isPause = "AUTO_PAUSE".equalsIgnoreCase(ev.type());
                    Color pinColor = isPause ? Color.rgb(239, 68, 68) : Color.rgb(16, 185, 129);

                    // Vertical guideline
                    gc.setStroke(pinColor);
                    gc.setLineWidth(1.0);
                    gc.setLineDashes(2, 2);
                    gc.strokeLine(ex, paddingTop, ex, paddingTop + graphH);

                    // Badge marker
                    gc.setFill(pinColor);
                    gc.fillRoundRect(ex - 22, paddingTop + 2, 44, 14, 4, 4);
                    gc.setFill(Color.WHITE);
                    gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 8));
                    gc.fillText(isPause ? "⏸ PAUSE" : "▶ RESUME", ex - 18, paddingTop + 12);

                    gc.restore();
                    break;
                }
            }
        }

        // 9. Time Axis Labels at start and end
        gc.setFill(Color.rgb(148, 163, 184));
        gc.setFont(javafx.scene.text.Font.font(9));
        if (count > 0) {
            gc.fillText(ptsCopy.get(0).timeLabel(), paddingLeft, paddingTop + graphH + 16);
            if (count > 1) {
                gc.fillText(ptsCopy.get(count - 1).timeLabel(), paddingLeft + graphW - 35, paddingTop + graphH + 16);
            }
        }
    }

    public int getPeakTemp() { return peakTemp; }
    public int getMinTemp() { return minTemp == 999 ? -1 : minTemp; }
    public double getAvgTemp() { return sampleCount > 0 ? (sumTemp / sampleCount) : 0; }
    public int getSampleCount() { return sampleCount; }
    public int getPauseCount() { return pauseCount; }
    public List<ThermalPoint> getPoints() { return Collections.unmodifiableList(points); }
    public List<ThermalEvent> getEvents() { return Collections.unmodifiableList(events); }
}
