package com.sanitizer.gui.components;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.engine.WipeMetrics;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, real-time Sector Heatmap / Matrix Grid visualizer (Defrag/Disk style).
 * Features Colorblind-Optimized Palettes for Deuteranopia, Protanopia, Tritanopia,
 * and High-Contrast Monochrome (WCAG AAA ≥10:1).
 */
public class SectorHeatmapComponent extends VBox {

    public enum BlockState {
        DIRTY,       // Raw data to be wiped
        ACTIVE_HEAD, // Live write head actively overwriting
        PATTERN,     // Cryptographic random pattern (DoD Pass 2)
        ZEROED,      // Verified zeroed / sanitized (0x00)
        BAD_SECTOR,  // Defective Bad Sector (Hardware I/O Fault)
        IDLE         // Idle / Ready
    }

    private final int totalBlocks;
    private final List<Rectangle> blockNodes = new ArrayList<>();
    private final List<BlockState> blockStates = new ArrayList<>();
    private final List<Tooltip> blockTooltips = new ArrayList<>();

    private final FlowPane gridPane = new FlowPane();
    private final Label lblHeaderInfo = new Label();
    private final HBox legendBar = new HBox(12);
    private long totalDriveBytes = 100L * 1024 * 1024; // Default baseline 100MB

    private HeatmapPalette currentPalette = AccessibilityManager.getHeatmapPalette();
    private int activeHeadIndex = -1;
    private Timeline pulseTimeline;
    private boolean pulseState = false;

    public SectorHeatmapComponent(int totalBlocks, double blockWidth, double blockHeight, double hgap, double vgap) {
        this.totalBlocks = totalBlocks;
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);

        gridPane.setHgap(hgap);
        gridPane.setVgap(vgap);
        gridPane.setAlignment(Pos.CENTER_LEFT);
        gridPane.setMaxWidth(Double.MAX_VALUE);

        lblHeaderInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        // Build the sector block rectangles
        for (int i = 0; i < totalBlocks; i++) {
            Rectangle rect = new Rectangle(blockWidth, blockHeight);
            rect.setArcWidth(4);
            rect.setArcHeight(4);

            Tooltip tooltip = new Tooltip(getTooltipText(i, BlockState.DIRTY));
            tooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(rect, tooltip);

            blockNodes.add(rect);
            blockStates.add(BlockState.DIRTY);
            blockTooltips.add(tooltip);
            applyBlockStyle(rect, BlockState.DIRTY);

            gridPane.getChildren().add(rect);
        }

        // Initialize Legend Bar
        refreshLegendBar();

        getChildren().addAll(lblHeaderInfo, gridPane, legendBar);
        initPulseAnimation();
        reset(totalDriveBytes);

        // Listen for global palette changes from AccessibilityManager
        AccessibilityManager.addPaletteListener(this::setPalette);
    }

    public HeatmapPalette getPalette() {
        return currentPalette;
    }

    /**
     * Dynamically switches the active colorblind-optimized heatmap palette and re-renders all blocks.
     */
    public void setPalette(HeatmapPalette newPalette) {
        if (newPalette == null || this.currentPalette == newPalette) {
            return;
        }
        this.currentPalette = newPalette;

        // Re-apply styles to all existing blocks
        for (int i = 0; i < totalBlocks; i++) {
            applyBlockStyle(blockNodes.get(i), blockStates.get(i));
        }

        // Re-render legend bar swatches
        refreshLegendBar();
    }

    private void initPulseAnimation() {
        pulseTimeline = new Timeline(new KeyFrame(Duration.millis(350), ev -> {
            if (activeHeadIndex >= 0 && activeHeadIndex < totalBlocks) {
                pulseState = !pulseState;
                Rectangle headNode = blockNodes.get(activeHeadIndex);
                if (pulseState) {
                    headNode.setStyle(String.format("-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 1.5; " +
                            "-fx-effect: dropshadow(three-pass-box, %s, 6, 0, 0, 0);",
                            currentPalette.getActiveHeadFill(), currentPalette.getActiveHeadStroke(), currentPalette.getActiveHeadGlow()));
                } else {
                    headNode.setStyle(String.format("-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 1.0; " +
                            "-fx-effect: dropshadow(three-pass-box, %s, 3, 0, 0, 0);",
                            currentPalette.getActiveHeadFill(), currentPalette.getActiveHeadStroke(), currentPalette.getActiveHeadGlow()));
                }
            }
        }));
        pulseTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public void reset(long driveSizeBytes) {
        this.totalDriveBytes = Math.max(1024 * 1024, driveSizeBytes);
        this.activeHeadIndex = -1;
        pulseTimeline.stop();

        for (int i = 0; i < totalBlocks; i++) {
            setBlockState(i, BlockState.DIRTY);
        }
        lblHeaderInfo.setText(String.format("SECTOR LBA MATRIX (%d BLOCKS | ~%s / BLOCK)",
                totalBlocks, formatSize(totalDriveBytes / totalBlocks)));
    }

    public void updateProgress(WipeMetrics metrics) {
        if (metrics == null) return;

        double pct = Math.max(0.0, Math.min(100.0, metrics.overallPercent()));
        int completedBlockCount = (int) Math.floor((pct / 100.0) * totalBlocks);
        int currentHead = Math.min(totalBlocks - 1, completedBlockCount);

        this.activeHeadIndex = (pct >= 100.0) ? -1 : currentHead;

        BlockState completedState = (metrics.currentPass() == 2 && metrics.totalPasses() == 3)
                ? BlockState.PATTERN
                : BlockState.ZEROED;

        for (int i = 0; i < totalBlocks; i++) {
            if (i < completedBlockCount) {
                setBlockState(i, completedState);
            } else if (i == currentHead && pct < 100.0) {
                setBlockState(i, BlockState.ACTIVE_HEAD);
            } else {
                setBlockState(i, BlockState.DIRTY);
            }
        }

        if (activeHeadIndex >= 0 && pulseTimeline.getStatus() != Animation.Status.RUNNING) {
            pulseTimeline.play();
        }

        long lbaOffset = (long) ((pct / 100.0) * (totalDriveBytes / 512));
        lblHeaderInfo.setText(String.format("SECTOR LBA MATRIX: Block #%d/%d | LBA ~0x%08X | %s",
                Math.min(totalBlocks, completedBlockCount + 1), totalBlocks, lbaOffset, metrics.formattedPassSummary()));
    }

    public void setCompleted() {
        pulseTimeline.stop();
        activeHeadIndex = -1;
        for (int i = 0; i < totalBlocks; i++) {
            setBlockState(i, BlockState.ZEROED);
        }
        lblHeaderInfo.setText(String.format("SECTOR LBA MATRIX: 100%% VERIFIED SANITIZED (%d/%d BLOCKS ZEROED 0x00)",
                totalBlocks, totalBlocks));
    }

    public void setAborted() {
        pulseTimeline.stop();
        if (activeHeadIndex >= 0 && activeHeadIndex < totalBlocks) {
            Rectangle headNode = blockNodes.get(activeHeadIndex);
            headNode.setStyle(String.format("-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 1.5;",
                    currentPalette.getDirtyFill(), currentPalette.getDirtyStroke()));
        }
        lblHeaderInfo.setText("SECTOR LBA MATRIX: OPERATION HALTED (GRANULAR ABORT)");
    }

    /**
     * Marks a specific byte offset or LBA range as a defective bad sector on the grid.
     */
    public void markBadSector(long byteOffset, String errorReason) {
        long bytesPerBlock = Math.max(1, totalDriveBytes / totalBlocks);
        int blockIndex = (int) Math.min(totalBlocks - 1, Math.max(0, byteOffset / bytesPerBlock));

        setBlockState(blockIndex, BlockState.BAD_SECTOR);
        long startLba = (long) blockIndex * (bytesPerBlock / 512);
        long endLba = startLba + (bytesPerBlock / 512) - 1;

        Tooltip tooltip = blockTooltips.get(blockIndex);
        tooltip.setText(String.format(
                "⚠️ DEFECTIVE BAD LBA DETECTED!\nLBA Range: 0x%08X - 0x%08X\nByte Offset: %s\nFault Error: %s\nAction: QUARANTINE / PHYSICAL DESTRUCTION REQUIRED",
                startLba, endLba, formatSize(byteOffset), errorReason != null ? errorReason : "I/O Hardware Error"
        ));

        lblHeaderInfo.setText(String.format(
                "⚠️ HARDWARE DEFECT DETECTED: Bad LBA @ ~0x%08X | Offset: %s (%s)",
                startLba, formatSize(byteOffset), errorReason != null ? errorReason : "POSIX EIO Fault"
        ));
    }

    private void setBlockState(int index, BlockState state) {
        blockStates.set(index, state);
        Rectangle rect = blockNodes.get(index);
        applyBlockStyle(rect, state);
        Tooltip tooltip = blockTooltips.get(index);
        tooltip.setText(getTooltipText(index, state));
    }

    private void applyBlockStyle(Rectangle rect, BlockState state) {
        switch (state) {
            case DIRTY -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 0.8;",
                    currentPalette.getDirtyFill(), currentPalette.getDirtyStroke()
            ));
            case ACTIVE_HEAD -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 1.2; -fx-effect: dropshadow(three-pass-box, %s, 5, 0, 0, 0);",
                    currentPalette.getActiveHeadFill(), currentPalette.getActiveHeadStroke(), currentPalette.getActiveHeadGlow()
            ));
            case PATTERN -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 0.8;",
                    currentPalette.getPatternFill(), currentPalette.getPatternStroke()
            ));
            case ZEROED -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 0.8;",
                    currentPalette.getZeroedFill(), currentPalette.getZeroedStroke()
            ));
            case BAD_SECTOR -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 1.6; -fx-effect: dropshadow(three-pass-box, %s, 6, 0, 0, 0);",
                    currentPalette.getBadSectorFill(), currentPalette.getBadSectorStroke(), currentPalette.getBadSectorGlow()
            ));
            case IDLE -> rect.setStyle(String.format(
                    "-fx-fill: %s; -fx-stroke: %s; -fx-stroke-width: 0.8;",
                    currentPalette.getIdleFill(), currentPalette.getIdleStroke()
            ));
        }
    }

    private String getTooltipText(int blockIndex, BlockState state) {
        long bytesPerBlock = Math.max(1, totalDriveBytes / totalBlocks);
        long startByte = (long) blockIndex * bytesPerBlock;
        long endByte = startByte + bytesPerBlock - 1;
        long startLba = startByte / 512;
        long endLba = endByte / 512;

        String stateStr = switch (state) {
            case DIRTY -> "RAW DATA (UNSANITIZED)";
            case ACTIVE_HEAD -> "ACTIVE WRITE HEAD (OVERWRITING)";
            case PATTERN -> "PATTERN OVERWRITE (0xFF / PSEUDO-RANDOM)";
            case ZEROED -> "VERIFIED ZEROED (0x00 FILL COMPLIANT)";
            case BAD_SECTOR -> "⚠️ DEFECTIVE BAD LBA (HARDWARE FAULT)";
            case IDLE -> "IDLE / READY";
        };

        return String.format("Sector Block #%d\nLBA Range: 0x%08X - 0x%08X\nByte Offset: %s - %s\nStatus: %s\nPalette: %s",
                blockIndex + 1, startLba, endLba, formatSize(startByte), formatSize(endByte), stateStr, currentPalette.getDisplayName());
    }

    private void refreshLegendBar() {
        legendBar.getChildren().clear();
        legendBar.setAlignment(Pos.CENTER_LEFT);
        legendBar.setPadding(new Insets(2, 0, 0, 0));

        legendBar.getChildren().addAll(
                createLegendItem(currentPalette.getDirtyFill(), "Raw Data"),
                createLegendItem(currentPalette.getActiveHeadFill(), "Write Head"),
                createLegendItem(currentPalette.getPatternFill(), "Pattern Pass"),
                createLegendItem(currentPalette.getZeroedFill(), "Zeroed 0x00"),
                createLegendItem(currentPalette.getBadSectorFill(), "⚠️ Bad LBA (Defect)")
        );
    }

    private HBox createLegendItem(String colorHex, String text) {
        HBox item = new HBox(4);
        item.setAlignment(Pos.CENTER_LEFT);

        Rectangle box = new Rectangle(7, 7);
        box.setArcWidth(2);
        box.setArcHeight(2);
        box.setStyle("-fx-fill: " + colorHex + ";");

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 9px; -fx-text-fill: #64748B;");

        item.getChildren().addAll(box, label);
        return item;
    }

    /**
     * Creates a compact palette switcher ComboBox for embedding in toolbar headers.
     */
    public HBox createPaletteSelectorWidget() {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("🎨 Palette:");
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        ComboBox<HeatmapPalette> cmb = new ComboBox<>();
        cmb.getItems().addAll(HeatmapPalette.values());
        cmb.setValue(this.currentPalette);
        cmb.setStyle("-fx-font-size: 10px; -fx-pref-width: 220px;");

        cmb.setOnAction(e -> {
            HeatmapPalette p = cmb.getValue();
            if (p != null) {
                AccessibilityManager.setHeatmapPalette(p);
            }
        });

        row.getChildren().addAll(lbl, cmb);
        return row;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
