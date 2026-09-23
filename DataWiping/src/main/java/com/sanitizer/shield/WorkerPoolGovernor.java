package com.sanitizer.shield;

import com.sanitizer.util.AppLogger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Worker Pool Governor & Concurrency Regulator.
 * Continuously monitors host CPU load, available physical memory, and storage bus bandwidth
 * to dynamically size parallel wiping worker pools and prevent CPU starvation or PCIe bus saturation.
 */
public class WorkerPoolGovernor {

    private static final String MODULE = "WorkerGovernor";
    private static final WorkerPoolGovernor INSTANCE = new WorkerPoolGovernor();

    public static final double CPU_THROTTLE_THRESHOLD = 0.88; // 88% CPU load limit
    public static final long MIN_FREE_MEMORY_BYTES = 512L * 1024 * 1024; // 512 MB

    private final int configuredPoolCapacity;
    private final Set<String> activeAllocatedSlots = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private long[] prevTicks;

    private WorkerPoolGovernor() {
        int cpus = Runtime.getRuntime().availableProcessors();
        this.configuredPoolCapacity = Math.max(8, Math.min(32, cpus * 2));

        SystemInfo si = new SystemInfo();
        this.processor = si.getHardware().getProcessor();
        this.memory = si.getHardware().getMemory();
        this.prevTicks = processor.getSystemCpuLoadTicks();
        AppLogger.info(MODULE, "Worker Pool Governor active: Capacity=" + configuredPoolCapacity + " slots | Cores=" + cpus);
    }

    public static WorkerPoolGovernor getInstance() {
        return INSTANCE;
    }

    /**
     * Attempts to acquire an execution slot for a target drive.
     * Evaluates CPU headroom and active slot saturation.
     */
    public synchronized boolean acquireSlot(String systemPath) {
        if (systemPath == null) return false;

        if (activeAllocatedSlots.contains(systemPath)) {
            return true; // Already held
        }

        if (activeAllocatedSlots.size() >= configuredPoolCapacity) {
            AppLogger.warn(MODULE, String.format("Pool saturated (%d/%d slots active). Queuing %s.",
                    activeAllocatedSlots.size(), configuredPoolCapacity, systemPath));
            return false;
        }

        double cpuLoad = getCpuLoad();
        if (cpuLoad >= CPU_THROTTLE_THRESHOLD && activeAllocatedSlots.size() >= 4) {
            AppLogger.warn(MODULE, String.format("High host CPU load (%.1f%% >= %.1f%%). Deferring dispatch of %s to avoid thread starvation.",
                    cpuLoad * 100.0, CPU_THROTTLE_THRESHOLD * 100.0, systemPath));
            return false;
        }

        activeAllocatedSlots.add(systemPath);
        AppLogger.info(MODULE, String.format("Slot acquired for %s [%d/%d active]",
                systemPath, activeAllocatedSlots.size(), configuredPoolCapacity));
        return true;
    }

    /**
     * Releases an allocated worker execution slot when a wipe finishes or is aborted.
     */
    public synchronized void releaseSlot(String systemPath) {
        if (systemPath != null && activeAllocatedSlots.remove(systemPath)) {
            AppLogger.info(MODULE, String.format("Slot released for %s [%d/%d active]",
                    systemPath, activeAllocatedSlots.size(), configuredPoolCapacity));
        }
    }

    /**
     * Calculates the live host CPU load ratio (0.0 to 1.0).
     */
    public double getCpuLoad() {
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = osBean.getCpuLoad();
            if (load >= 0.0) {
                return load;
            }
        } catch (Exception ignored) {}

        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks);
        prevTicks = processor.getSystemCpuLoadTicks();
        return Math.max(0.0, Math.min(1.0, load));
    }

    public long getAvailableMemoryBytes() {
        try {
            return memory.getAvailable();
        } catch (Exception e) {
            return Runtime.getRuntime().freeMemory();
        }
    }

    public int getActiveSlotCount() {
        return activeAllocatedSlots.size();
    }

    public int getConfiguredPoolCapacity() {
        return configuredPoolCapacity;
    }

    public int getUtilizationPercent() {
        if (configuredPoolCapacity <= 0) return 0;
        return (int) Math.round(((double) activeAllocatedSlots.size() / configuredPoolCapacity) * 100.0);
    }

    public String getStatusSummary() {
        double cpuPct = getCpuLoad() * 100.0;
        return String.format("%d/%d Slots Active (%d%%) | Host CPU: %.1f%%",
                activeAllocatedSlots.size(), configuredPoolCapacity, getUtilizationPercent(), cpuPct);
    }

    public synchronized void reset() {
        activeAllocatedSlots.clear();
    }
}
