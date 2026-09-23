package com.sanitizer.audio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sanitizer.util.AppLogger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Configuration model and persistence manager for Operator Bench Audio & Visual Alarms.
 */
public class AudioAlarmConfig {

    private static final String MODULE = "AudioAlarmConfig";
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + File.separator + ".sanitizer" + File.separator + "audio_alarms.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum AlarmMode {
        CHIME_AND_SPEECH("Chime + Voice Announcement (Recommended)"),
        TTS_SPEECH_ONLY("Voice Announcement Only"),
        CHIME_ONLY("Chime Only"),
        MUTED("Muted (Silent)");

        private final String displayName;

        AlarmMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private AlarmMode alarmMode = AlarmMode.CHIME_AND_SPEECH;
    private double volume = 0.85; // 0.0 to 1.0
    private boolean enablePassCompleteAlarm = true;
    private boolean enableJobCompleteAlarm = true;
    private boolean enableThermalAlertAlarm = true;
    private boolean enableVerificationFailureAlarm = true;
    private boolean visualFlashEnabled = true;
    private String speechTemplate = "Drive {drive} sanitization complete: {standard} verified. Zero residual entropy confirmed.";

    public AudioAlarmConfig() {}

    public AlarmMode getAlarmMode() {
        return alarmMode;
    }

    public void setAlarmMode(AlarmMode alarmMode) {
        this.alarmMode = alarmMode != null ? alarmMode : AlarmMode.CHIME_AND_SPEECH;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
    }

    public boolean isEnablePassCompleteAlarm() {
        return enablePassCompleteAlarm;
    }

    public void setEnablePassCompleteAlarm(boolean enablePassCompleteAlarm) {
        this.enablePassCompleteAlarm = enablePassCompleteAlarm;
    }

    public boolean isEnableJobCompleteAlarm() {
        return enableJobCompleteAlarm;
    }

    public void setEnableJobCompleteAlarm(boolean enableJobCompleteAlarm) {
        this.enableJobCompleteAlarm = enableJobCompleteAlarm;
    }

    public boolean isEnableThermalAlertAlarm() {
        return enableThermalAlertAlarm;
    }

    public void setEnableThermalAlertAlarm(boolean enableThermalAlertAlarm) {
        this.enableThermalAlertAlarm = enableThermalAlertAlarm;
    }

    public boolean isEnableVerificationFailureAlarm() {
        return enableVerificationFailureAlarm;
    }

    public void setEnableVerificationFailureAlarm(boolean enableVerificationFailureAlarm) {
        this.enableVerificationFailureAlarm = enableVerificationFailureAlarm;
    }

    public boolean isVisualFlashEnabled() {
        return visualFlashEnabled;
    }

    public void setVisualFlashEnabled(boolean visualFlashEnabled) {
        this.visualFlashEnabled = visualFlashEnabled;
    }

    public String getSpeechTemplate() {
        return speechTemplate;
    }

    public void setSpeechTemplate(String speechTemplate) {
        this.speechTemplate = speechTemplate != null && !speechTemplate.isBlank()
                ? speechTemplate
                : "Drive {drive} sanitization complete: {standard} verified.";
    }

    /**
     * Formats the speech announcement template replacing variables: {drive}, {standard}, {entropy}, {status}.
     */
    public String formatAnnouncement(String drive, String standard, double entropy, String status) {
        String template = this.speechTemplate;
        if (template == null || template.isBlank()) {
            template = "Drive {drive} sanitization complete: {standard} verified.";
        }
        return template
                .replace("{drive}", drive != null ? cleanDriveName(drive) : "Target Drive")
                .replace("{standard}", standard != null ? standard : "NIST SP 800-88")
                .replace("{entropy}", String.format("%.4f", entropy))
                .replace("{status}", status != null ? status : "PASSED");
    }

    private String cleanDriveName(String drive) {
        // Remove /dev/ or \\.\ prefixes for natural spoken clarity (e.g. /dev/rdisk4 -> disk 4)
        String clean = drive.replaceAll("^/dev/[r]?", "").replaceAll("^\\\\\\\\\\.\\\\", "");
        return clean.replaceAll("(?i)disk(\\d+)", "disk $1");
    }

    /**
     * Loads saved configuration from ~/.sanitizer/audio_alarms.json or returns default instance.
     */
    public static AudioAlarmConfig load() {
        File file = new File(CONFIG_FILE_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                AudioAlarmConfig config = GSON.fromJson(reader, AudioAlarmConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Could not load audio alarm config: " + e.getMessage() + ", using defaults.");
            }
        }
        return new AudioAlarmConfig();
    }

    /**
     * Saves current configuration to ~/.sanitizer/audio_alarms.json.
     */
    public synchronized void save() {
        try {
            File file = new File(CONFIG_FILE_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            AppLogger.info(MODULE, "Audio alarm configuration saved to: " + CONFIG_FILE_PATH);
        } catch (Exception e) {
            AppLogger.error(MODULE, "Failed to save audio alarm configuration: " + e.getMessage(), e);
        }
    }
}
