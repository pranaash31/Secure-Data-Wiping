package com.sanitizer.alert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Thread-safe manager for loading and persisting AlertConfig in ~/.sanitizer/alert_config.json.
 */
public class AlertConfigManager {

    private static final String MODULE = "AlertConfigMgr";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_PATH = System.getProperty("user.home") + File.separator + ".sanitizer" + File.separator + "alert_config.json";

    private static AlertConfigManager instance;
    private AlertConfig activeConfig;

    private AlertConfigManager() {
        this.activeConfig = loadConfig();
    }

    public static synchronized AlertConfigManager getInstance() {
        if (instance == null) {
            instance = new AlertConfigManager();
        }
        return instance;
    }

    public synchronized AlertConfig getConfig() {
        if (activeConfig == null) {
            activeConfig = loadConfig();
        }
        return activeConfig;
    }

    public synchronized void saveConfig(AlertConfig config) {
        if (config == null) return;
        this.activeConfig = config;
        try {
            File file = new File(CONFIG_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            String json = GSON.toJson(config);
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
            AppLogger.info(MODULE, "Saved alert configuration to: " + CONFIG_PATH);
        } catch (IOException e) {
            AppLogger.error(MODULE, "Failed to persist alert configuration", e);
        }
    }

    private AlertConfig loadConfig() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            AlertConfig defaultConfig = new AlertConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            AlertConfig loaded = GSON.fromJson(json, AlertConfig.class);
            return loaded != null ? loaded : new AlertConfig();
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Failed to parse " + CONFIG_PATH + ", reverting to defaults: " + e.getMessage());
            return new AlertConfig();
        }
    }
}
