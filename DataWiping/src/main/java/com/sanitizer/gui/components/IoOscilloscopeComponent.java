package com.sanitizer.gui.components;

import com.sanitizer.detector.DeviceType;
import com.sanitizer.engine.WipeMetrics;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.StrokeLineCap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-Time Rolling Waveform Oscilloscope & Multi-Channel IO Telemetry Visualizer.
 * Displays live write throughput (MB/s), estimated IOPS (Ops/sec), buffer flush latencies (ms),
 * and physical bus interface saturation (%) alongside drive thermal telemetry.
 */
public class IoOscilloscopeComponent extends VBox {

    public enum DisplayMode {
        QUAD_TRACE("📊 Multi-Trace Quad", "All channels overlaid/stacked"),
        THROUGHPUT("🚀 Write Speed (MB/s)", "Throughput bandwidth waveform"),
        IOPS("⚡ IOPS (Ops/sec)", "I/O transaction burst frequency"),
        LATENCY("⏱️ Buffer Latency (ms)", "Flush & sync latency oscilloscope"),
        SATURATION("📶 Bus Saturation (%)", "Interface bandwidth utilization");

        private final String label;
        private final String description;

        DisplayMode(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }

    public record IoSample(
            long timestampMillis,
            double throughputMBs,
            double iops,
            double latencyMs,
            double busSaturationPct,
            String timeLabel
    ) {}

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_DATA_POINTS = 120;

    private final List<IoSample> samples = new ArrayList<>();

    // Device / Interface Profile
    private String busInterfaceName = "USB 3.0 / SATA";
    private double interfaceMaxMBs = 450.0; // Standard USB 3.0 / SATA III throughput ceiling
    private DisplayMode currentMode = DisplayMode.QUAD_TRACE;
    private boolean isPaused = false;

    // Stat Tracking
    private double peakThroughput = 0.0;
    private double sumThroughput = 0.0;
    private double peakIops = 0.0;
    private double sumIops = 0.0;
    private double peakLatency = 0.0;
    private double sumLatency = 0.0;
    private double peakSaturation = 0.0;
    private int sampleCount = 0;
    private long totalBytesRecorded = 0;

    // UI Stat Ribbon Labels
    private final Label lblThroughputVal = new Label("-- MB/s");
    private final Label lblIopsVal = new Label("-- IOPS");
    private final Label lblLatencyVal = new Label("-- ms");
    private final Label lblSaturationVal = new Label("-- %");
    private final Label lblPeakSpeedBadge = new Label("Peak: -- MB/s");
    private final Label lblAvgSpeedBadge = new Label("Avg: -- MB/s");
    private final Label lblInterfaceBadge = new Label("Bus: USB 3.0 (450 MB/s Cap)");

    // Mode Selector Buttons
    private final List<Button> modeButtons = new ArrayList<>();
    private final HBox modeRibbon = new HBox(8);

    // Canvas
    private final Canvas canvas = new Canvas(600, 175);

    public IoOscilloscopeComponent() {
        this("USB 3.0 / SATA", 450.0);
    }

    public IoOscilloscopeComponent(String busName, double maxMBs) {
        this.busInterfaceName = busName;
        this.interfaceMaxMBs = maxMBs > 0 ? maxMBs : 450.0;
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

        Label iconBadge = new Label("⚡ I/O OSCILLOSCOPE & SPARKLINE");
        iconBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284C7; " +
                "-fx-background-color: #F0F9FF; -fx-background-radius: 6px; -fx-padding: 4 8; -fx-border-color: #BAE6FD; -fx-border-radius: 6px;");

        HBox statPills = new HBox(8);
        statPills.setAlignment(Pos.CENTER_LEFT);

        HBox tputBox = createMetricBadge("🚀 Speed", lblThroughputVal, "#0284C7", "#F0F9FF");
        HBox iopsBox = createMetricBadge("⚡ IOPS", lblIopsVal, "#059669", "#ECFDF5");
        HBox latBox = createMetricBadge("⏱ Latency", lblLatencyVal, "#D97706", "#FEF3C7");
        HBox satBox = createMetricBadge("📶 Bus Sat", lblSaturationVal, "#7C3AED", "#F5F3FF");

        statPills.getChildren().addAll(tputBox, iopsBox, latBox, satBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblPeakSpeedBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284C7; -fx-background-color: #F0F9FF; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblAvgSpeedBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 3 8; -fx-background-radius: 6px;");
        lblInterfaceBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 3 8; -fx-background-radius: 6px;");

        header.getChildren().addAll(iconBadge, statPills, spacer, lblPeakSpeedBadge, lblAvgSpeedBadge, lblInterfaceBadge);

        // ── Mode Selector Ribbon ──────────────────────────────────────────
        modeRibbon.setAlignment(Pos.CENTER_LEFT);
        for (DisplayMode mode : DisplayMode.values()) {
            Button btn = new Button(mode.getLabel());
            btn.getStyleClass().add("filter-chip-button");
            btn.setStyle(mode == currentMode ? getActiveModeStyle() : getInactiveModeStyle());
            btn.setOnAction(e -> setDisplayMode(mode));
            modeButtons.add(btn);
            modeRibbon.getChildren().add(btn);
        }

        Region modeSpacer = new Region();
        HBox.setHgrow(modeSpacer, Priority.ALWAYS);

        Button btnFreeze = new Button("⏸ Freeze");
        btnFreeze.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;");
        btnFreeze.setOnAction(e -> {
            isPaused = !isPaused;
            btnFreeze.setText(isPaused ? "▶ Resume" : "⏸ Freeze");
            btnFreeze.setStyle(isPaused ?
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;" :
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;");
        });

        Button btnClear = new Button("🧹 Reset");
        btnClear.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;");
        btnClear.setOnAction(e -> reset());

        modeRibbon.getChildren().addAll(modeSpacer, btnFreeze, btnClear);

        // ── Canvas Container ─────────────────────────────────────────────
        StackPane canvasContainer = new StackPane(canvas);
        canvasContainer.setStyle("-fx-background-color: #0B132B; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        canvasContainer.setPadding(new Insets(6));

        // Auto resize canvas with layout width
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

        Label legTput = createLegendItem("#38BDF8", "Throughput (MB/s)");
        Label legIops = createLegendItem("#34D399", "IOPS (Ops/s)");
        Label legLat = createLegendItem("#FBBF24", "Flush Latency (ms)");
        Label legSat = createLegendItem("#A78BFA", "Bus Saturation (%)");

        Region legSpacer = new Region();
        HBox.setHgrow(legSpacer, Priority.ALWAYS);

        Label lblTimeScale = new Label("Window: Rolling 120s Waveform • 60 FPS Engine");
        lblTimeScale.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        legend.getChildren().addAll(legTput, legIops, legLat, legSat, legSpacer, lblTimeScale);

        getChildren().addAll(header, modeRibbon, canvasContainer, legend);
    }

    private HBox createMetricBadge(String labelText, Label valLabel, String textHex, String bgHex) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(String.format("-fx-background-color: %s; -fx-padding: 3 8; -fx-background-radius: 6px;", bgHex));

        Label prefix = new Label(labelText + ":");
        prefix.setStyle(String.format("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: %s;", textHex));

        valLabel.setStyle(String.format("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: %s;", textHex));
        box.getChildren().addAll(prefix, valLabel);
        return box;
    }

    private Label createLegendItem(String colorHex, String text) {
        Label lbl = new Label("● " + text);
        lbl.setStyle(String.format("-fx-font-size: 10px; -fx-text-fill: %s; -fx-font-weight: bold;", colorHex));
        return lbl;
    }

    private String getActiveModeStyle() {
        return "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #0284C7; -fx-text-fill: #FFFFFF; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;";
    }

    private String getInactiveModeStyle() {
        return "-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6px; -fx-padding: 4 10; -fx-cursor: hand;";
    }

    public void setDisplayMode(DisplayMode mode) {
        this.currentMode = mode;
        for (int i = 0; i < DisplayMode.values().length; i++) {
            DisplayMode m = DisplayMode.values()[i];
            if (i < modeButtons.size()) {
                modeButtons.get(i).setStyle(m == mode ? getActiveModeStyle() : getInactiveModeStyle());
            }
        }
        redraw();
    }

    /**
     * Updates device bus interface context and theoretical max bandwidth ceiling.
     */
    public synchronized void setDeviceContext(String model, String systemPath, long capacityBytes) {
        DeviceType dt = DeviceType.fromDrive(model, systemPath, capacityBytes);
        String m = (model != null ? model.toLowerCase() : "");
        String p = (systemPath != null ? systemPath.toLowerCase() : "");

        if (dt == DeviceType.NVME_SSD || m.contains("nvme") || p.contains("nvme")) {
            this.busInterfaceName = "NVMe PCIe 4.0 (3,500 MB/s Cap)";
            this.interfaceMaxMBs = 3500.0;
        } else if (dt == DeviceType.MAGNETIC_HDD) {
            this.busInterfaceName = "SATA III HDD (180 MB/s Cap)";
            this.interfaceMaxMBs = 180.0;
        } else if (m.contains("usb 2") || m.contains("usb2") || (capacityBytes > 0 && capacityBytes < 8L * 1024 * 1024 * 1024 && !m.contains("3."))) {
            this.busInterfaceName = "USB 2.0 (40 MB/s Cap)";
            this.interfaceMaxMBs = 40.0;
        } else {
            this.busInterfaceName = "USB 3.2 / SATA (450 MB/s Cap)";
            this.interfaceMaxMBs = 450.0;
        }

        Platform.runLater(() -> {
            lblInterfaceBadge.setText("Bus: " + busInterfaceName);
            redraw();
        });
    }

    /**
     * Ingests a new live telemetry sample from WipeMetrics.
     */
    public synchronized void addSample(WipeMetrics metrics) {
        if (metrics == null || isPaused) return;
        addSample(metrics.speedMBs(), metrics.iops(), metrics.latencyMs(), metrics.busSaturationPercent());
    }

    /**
     * Appends a raw multi-channel IO telemetry point to the time series.
     */
    public synchronized void addSample(double speedMBs, double iops, double latencyMs, double busSaturationPct) {
        if (isPaused) return;

        long now = System.currentTimeMillis();
        String timeStr = LocalTime.now().format(TIME_FMT);

        // Sanitize values
        double s = Math.max(0.0, speedMBs);
        double i = (iops > 0.0) ? iops : ((s * 1024.0 * 1024.0) / 4096.0);
        double l = (latencyMs > 0.0) ? latencyMs : Math.min(200.0, Math.max(0.5, (2.0 / Math.max(0.1, s)) * 100.0));
        double sat = (busSaturationPct > 0.0) ? busSaturationPct : Math.min(100.0, (s / interfaceMaxMBs) * 100.0);

        samples.add(new IoSample(now, s, i, l, sat, timeStr));
        if (samples.size() > MAX_DATA_POINTS) {
            samples.remove(0);
        }

        // Aggregate statistics
        sampleCount++;
        sumThroughput += s;
        sumIops += i;
        sumLatency += l;
        if (s > peakThroughput) peakThroughput = s;
        if (i > peakIops) peakIops = i;
        if (l > peakLatency) peakLatency = l;
        if (sat > peakSaturation) peakSaturation = sat;

        updateStats(s, i, l, sat);

        if (Platform.isFxApplicationThread()) {
            redraw();
        } else {
            Platform.runLater(this::redraw);
        }
    }

    public synchronized void reset() {
        samples.clear();
        peakThroughput = 0.0;
        sumThroughput = 0.0;
        peakIops = 0.0;
        sumIops = 0.0;
        peakLatency = 0.0;
        sumLatency = 0.0;
        peakSaturation = 0.0;
        sampleCount = 0;
        totalBytesRecorded = 0;

        lblThroughputVal.setText("-- MB/s");
        lblIopsVal.setText("-- IOPS");
        lblLatencyVal.setText("-- ms");
        lblSaturationVal.setText("-- %");
        lblPeakSpeedBadge.setText("Peak: -- MB/s");
        lblAvgSpeedBadge.setText("Avg: -- MB/s");

        redraw();
    }

    private void updateStats(double latestSpeed, double latestIops, double latestLat, double latestSat) {
        lblThroughputVal.setText(String.format("%.1f MB/s", latestSpeed));
        if (latestIops >= 1000.0) {
            lblIopsVal.setText(String.format("%,.0f IOPS", latestIops));
        } else {
            lblIopsVal.setText(String.format("%.0f IOPS", latestIops));
        }
        lblLatencyVal.setText(String.format("%.1f ms", latestLat));
        lblSaturationVal.setText(String.format("%.1f%%", latestSat));

        lblPeakSpeedBadge.setText(String.format("Peak: %.1f MB/s", peakThroughput));
        double avg = (sampleCount > 0) ? (sumThroughput / sampleCount) : latestSpeed;
        lblAvgSpeedBadge.setText(String.format("Avg: %.1f MB/s", avg));
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);

        // Dark Phosphor Oscilloscope Canvas Background
        gc.setFill(Color.rgb(11, 19, 43)); // #0B132B
        gc.fillRoundRect(0, 0, w, h, 8, 8);

        double paddingLeft = 52;
        double paddingRight = 20;
        double paddingTop = 18;
        double paddingBottom = 26;

        double graphW = w - paddingLeft - paddingRight;
        double graphH = h - paddingTop - paddingBottom;

        if (graphW <= 10 || graphH <= 10) return;

        // 1. Draw Oscilloscope Grid & Graduation Markings
        gc.setLineWidth(0.8);
        gc.setStroke(Color.rgb(30, 41, 59, 0.8)); // Subtle dark blue-grey grid
        gc.setFont(javafx.scene.text.Font.font("Monospaced", 9));

        int numHorizontalLines = 4;
        for (int row = 0; row <= numHorizontalLines; row++) {
            double y = paddingTop + (graphH / numHorizontalLines) * row;
            gc.strokeLine(paddingLeft, y, paddingLeft + graphW, y);
        }

        int numVerticalLines = 6;
        for (int col = 0; col <= numVerticalLines; col++) {
            double x = paddingLeft + (graphW / numVerticalLines) * col;
            gc.strokeLine(x, paddingTop, x, paddingTop + graphH);
        }

        // Draw Time-Axis Graduation Labels at bottom
        gc.setFill(Color.rgb(148, 163, 184));
        String[] timeLabels = {"-120s", "-100s", "-80s", "-60s", "-40s", "-20s", "NOW"};
        for (int i = 0; i < timeLabels.length; i++) {
            double x = paddingLeft + (graphW / (timeLabels.length - 1)) * i - 12;
            gc.fillText(timeLabels[i], x, paddingTop + graphH + 16);
        }

        List<IoSample> ptsCopy;
        synchronized (this) {
            ptsCopy = new ArrayList<>(samples);
        }

        if (ptsCopy.isEmpty()) {
            gc.setFill(Color.rgb(100, 116, 139));
            gc.setFont(javafx.scene.text.Font.font("System", 11));
            gc.fillText("⚡ Awaiting active drive I/O operations & bus streaming...", paddingLeft + (graphW / 2) - 150, paddingTop + (graphH / 2) + 4);
            return;
        }

        // 2. Dispatch drawing based on selected Display Mode
        switch (currentMode) {
            case QUAD_TRACE -> renderQuadTrace(gc, ptsCopy, paddingLeft, paddingTop, graphW, graphH);
            case THROUGHPUT -> renderThroughputFocus(gc, ptsCopy, paddingLeft, paddingTop, graphW, graphH);
            case IOPS -> renderIopsFocus(gc, ptsCopy, paddingLeft, paddingTop, graphW, graphH);
            case LATENCY -> renderLatencyFocus(gc, ptsCopy, paddingLeft, paddingTop, graphW, graphH);
            case SATURATION -> renderSaturationFocus(gc, ptsCopy, paddingLeft, paddingTop, graphW, graphH);
        }
    }

    /**
     * Renders all 4 telemetry waveforms overlaid with distinctive neon phosphorescent traces.
     */
    private void renderQuadTrace(GraphicsContext gc, List<IoSample> pts, double pl, double pt, double gw, double gh) {
        int count = pts.size();
        double[] xs = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (count == 1) ? pl + (gw / 2) : pl + ((double) i / (count - 1)) * gw;
        }

        // Dynamic scale ceilings
        double maxTput = Math.max(50.0, peakThroughput * 1.15);
        double maxIops = Math.max(2000.0, peakIops * 1.15);
        double maxLat = Math.max(50.0, peakLatency * 1.15);

        // Draw Y-Axis (Left: Speed MB/s, Right: Latency ms)
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillText(String.format("%.0fM", maxTput), pl - 45, pt + 5);
        gc.fillText(String.format("%.0fM", maxTput / 2), pl - 45, pt + (gh / 2) + 3);
        gc.fillText("0M", pl - 30, pt + gh + 3);

        // 1. Throughput Trace (Electric Cyan with Area Gradient)
        double[] yTput = new double[count];
        for (int i = 0; i < count; i++) {
            yTput[i] = pt + gh - (pts.get(i).throughputMBs() / maxTput * gh);
        }
        drawTraceWithGradient(gc, xs, yTput, count, pl, pt, gh,
                Color.rgb(56, 189, 248),
                Color.rgb(56, 189, 248, 0.25),
                Color.rgb(56, 189, 248, 0.02),
                2.2);

        // 2. IOPS Trace (Emerald Green Line)
        double[] yIops = new double[count];
        for (int i = 0; i < count; i++) {
            yIops[i] = pt + gh - (pts.get(i).iops() / maxIops * gh);
        }
        drawTraceLine(gc, xs, yIops, count, Color.rgb(52, 211, 153), 1.6, false);

        // 3. Latency Trace (Amber Line with Alert Spikes)
        double[] yLat = new double[count];
        for (int i = 0; i < count; i++) {
            yLat[i] = pt + gh - (Math.min(maxLat, pts.get(i).latencyMs()) / maxLat * gh);
        }
        drawTraceLine(gc, xs, yLat, count, Color.rgb(251, 191, 36), 1.5, true);

        // 4. Bus Saturation Trace (Violet Line)
        double[] ySat = new double[count];
        for (int i = 0; i < count; i++) {
            ySat[i] = pt + gh - (Math.min(100.0, pts.get(i).busSaturationPct()) / 100.0 * gh);
        }
        drawTraceLine(gc, xs, ySat, count, Color.rgb(167, 139, 250), 1.3, false);

        // Render Glowing Live Probe Head
        if (count > 0) {
            double lastX = xs[count - 1];
            double lastYTput = yTput[count - 1];
            gc.setFill(Color.WHITE);
            gc.fillOval(lastX - 4, lastYTput - 4, 8, 8);
            gc.setStroke(Color.rgb(56, 189, 248));
            gc.setLineWidth(2.0);
            gc.strokeOval(lastX - 4, lastYTput - 4, 8, 8);
        }
    }

    /**
     * Renders Throughput Waveform in Full Detail.
     */
    private void renderThroughputFocus(GraphicsContext gc, List<IoSample> pts, double pl, double pt, double gw, double gh) {
        int count = pts.size();
        double maxScale = Math.max(40.0, Math.max(peakThroughput * 1.2, interfaceMaxMBs * 0.5));

        // Draw Y Axis Labels
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillText(String.format("%.0f MB/s", maxScale), pl - 50, pt + 5);
        gc.fillText(String.format("%.0f MB/s", maxScale * 0.5), pl - 50, pt + (gh / 2) + 3);
        gc.fillText("0 MB/s", pl - 45, pt + gh + 3);

        // Interface Theoretical Max Cap Line (Red Dashed)
        if (interfaceMaxMBs <= maxScale) {
            double capY = pt + gh - (interfaceMaxMBs / maxScale * gh);
            gc.save();
            gc.setStroke(Color.rgb(239, 68, 68, 0.7));
            gc.setLineWidth(1.2);
            gc.setLineDashes(4, 4);
            gc.strokeLine(pl, capY, pl + gw, capY);
            gc.setFill(Color.rgb(248, 113, 113));
            gc.fillText("Interface Max: " + interfaceMaxMBs + " MB/s", pl + 10, capY - 3);
            gc.restore();
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (count == 1) ? pl + (gw / 2) : pl + ((double) i / (count - 1)) * gw;
            ys[i] = pt + gh - (pts.get(i).throughputMBs() / maxScale * gh);
        }

        drawTraceWithGradient(gc, xs, ys, count, pl, pt, gh,
                Color.rgb(56, 189, 248),
                Color.rgb(56, 189, 248, 0.35),
                Color.rgb(56, 189, 248, 0.01),
                2.5);

        // Peak marker point
        for (int i = 0; i < count; i++) {
            if (pts.get(i).throughputMBs() == peakThroughput && peakThroughput > 0) {
                gc.setFill(Color.rgb(56, 189, 248));
                gc.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 10));
                gc.fillText(String.format("Peak %.1f MB/s", peakThroughput), xs[i] - 30, ys[i] - 8);
            }
        }
    }

    /**
     * Renders IOPS (I/O Operations/Sec) Waveform.
     */
    private void renderIopsFocus(GraphicsContext gc, List<IoSample> pts, double pl, double pt, double gw, double gh) {
        int count = pts.size();
        double maxScale = Math.max(1000.0, peakIops * 1.25);

        gc.setFill(Color.rgb(52, 211, 153));
        gc.fillText(String.format("%,.0f", maxScale), pl - 45, pt + 5);
        gc.fillText(String.format("%,.0f", maxScale * 0.5), pl - 45, pt + (gh / 2) + 3);
        gc.fillText("0 IOPS", pl - 45, pt + gh + 3);

        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (count == 1) ? pl + (gw / 2) : pl + ((double) i / (count - 1)) * gw;
            ys[i] = pt + gh - (pts.get(i).iops() / maxScale * gh);
        }

        drawTraceWithGradient(gc, xs, ys, count, pl, pt, gh,
                Color.rgb(52, 211, 153),
                Color.rgb(52, 211, 153, 0.35),
                Color.rgb(52, 211, 153, 0.01),
                2.5);
    }

    /**
     * Renders Buffer Flush / Sync Latency Waveform with Safe & Alert Threshold Lines.
     */
    private void renderLatencyFocus(GraphicsContext gc, List<IoSample> pts, double pl, double pt, double gw, double gh) {
        int count = pts.size();
        double maxScale = Math.max(50.0, Math.max(peakLatency * 1.2, 120.0));

        gc.setFill(Color.rgb(251, 191, 36));
        gc.fillText(String.format("%.0f ms", maxScale), pl - 45, pt + 5);
        gc.fillText(String.format("%.0f ms", maxScale * 0.5), pl - 45, pt + (gh / 2) + 3);
        gc.fillText("0 ms", pl - 35, pt + gh + 3);

        // Latency Warning Line (100 ms)
        if (100.0 <= maxScale) {
            double warnY = pt + gh - (100.0 / maxScale * gh);
            gc.save();
            gc.setStroke(Color.rgb(239, 68, 68, 0.8));
            gc.setLineWidth(1.2);
            gc.setLineDashes(4, 4);
            gc.strokeLine(pl, warnY, pl + gw, warnY);
            gc.setFill(Color.rgb(248, 113, 113));
            gc.fillText("I/O Flush Jitter Threshold (100 ms)", pl + 10, warnY - 3);
            gc.restore();
        }

        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (count == 1) ? pl + (gw / 2) : pl + ((double) i / (count - 1)) * gw;
            ys[i] = pt + gh - (Math.min(maxScale, pts.get(i).latencyMs()) / maxScale * gh);
        }

        drawTraceWithGradient(gc, xs, ys, count, pl, pt, gh,
                Color.rgb(251, 191, 36),
                Color.rgb(251, 191, 36, 0.30),
                Color.rgb(251, 191, 36, 0.01),
                2.2);
    }

    /**
     * Renders Bus Saturation % Waveform.
     */
    private void renderSaturationFocus(GraphicsContext gc, List<IoSample> pts, double pl, double pt, double gw, double gh) {
        int count = pts.size();

        gc.setFill(Color.rgb(167, 139, 250));
        gc.fillText("100%", pl - 38, pt + 5);
        gc.fillText("50%", pl - 35, pt + (gh / 2) + 3);
        gc.fillText("0%", pl - 30, pt + gh + 3);

        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (count == 1) ? pl + (gw / 2) : pl + ((double) i / (count - 1)) * gw;
            ys[i] = pt + gh - (Math.min(100.0, pts.get(i).busSaturationPct()) / 100.0 * gh);
        }

        drawTraceWithGradient(gc, xs, ys, count, pl, pt, gh,
                Color.rgb(167, 139, 250),
                Color.rgb(167, 139, 250, 0.35),
                Color.rgb(167, 139, 250, 0.01),
                2.5);
    }

    private void drawTraceWithGradient(GraphicsContext gc, double[] xs, double[] ys, int count,
                                       double pl, double pt, double gh,
                                       Color strokeColor, Color gradStart, Color gradEnd, double lineWidth) {
        if (count == 0) return;

        // Fill area under curve
        gc.save();
        gc.beginPath();
        gc.moveTo(xs[0], pt + gh);
        for (int i = 0; i < count; i++) {
            gc.lineTo(xs[i], ys[i]);
        }
        gc.lineTo(xs[count - 1], pt + gh);
        gc.closePath();

        LinearGradient areaGrad = new LinearGradient(
                0, pt, 0, pt + gh, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, gradStart),
                new Stop(1.0, gradEnd)
        );
        gc.setFill(areaGrad);
        gc.fill();
        gc.restore();

        // Stroke line
        drawTraceLine(gc, xs, ys, count, strokeColor, lineWidth, false);
    }

    private void drawTraceLine(GraphicsContext gc, double[] xs, double[] ys, int count,
                              Color strokeColor, double lineWidth, boolean dashed) {
        if (count == 0) return;
        gc.save();
        gc.setLineWidth(lineWidth);
        gc.setStroke(strokeColor);
        gc.setLineCap(StrokeLineCap.ROUND);
        if (dashed) {
            gc.setLineDashes(4, 3);
        }

        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) {
            gc.lineTo(xs[i], ys[i]);
        }
        gc.stroke();
        gc.restore();
    }

    // Getters for telemetry stats
    public synchronized double getPeakThroughput() { return peakThroughput; }
    public synchronized double getAvgThroughput() { return sampleCount > 0 ? (sumThroughput / sampleCount) : 0.0; }
    public synchronized double getPeakIops() { return peakIops; }
    public synchronized double getAvgIops() { return sampleCount > 0 ? (sumIops / sampleCount) : 0.0; }
    public synchronized double getPeakLatency() { return peakLatency; }
    public synchronized double getAvgLatency() { return sampleCount > 0 ? (sumLatency / sampleCount) : 0.0; }
    public synchronized double getPeakSaturation() { return peakSaturation; }
    public synchronized int getSampleCount() { return sampleCount; }
    public synchronized String getBusInterfaceName() { return busInterfaceName; }
    public synchronized double getInterfaceMaxMBs() { return interfaceMaxMBs; }
    public synchronized List<IoSample> getSamples() { return java.util.Collections.unmodifiableList(new ArrayList<>(samples)); }
    public DisplayMode getCurrentMode() { return currentMode; }
}
