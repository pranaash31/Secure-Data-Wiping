package com.sanitizer.util;

import com.sanitizer.audio.AudioAlarmConfig;
import com.sanitizer.audio.TextToSpeechEngine;

import javax.sound.sampled.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * High-performance, zero-dependency Audio & Sound Design Engine.
 * Programmatically synthesizes 16-bit PCM harmonic chimes and alert sirens
 * with ADSR exponential decay envelopes and integrates cross-platform Text-to-Speech (TTS)
 * voice announcements for operator wiping benches.
 */
public class SoundManager {

    private static final String MODULE = "SoundManager";
    private static final float SAMPLE_RATE = 44100f;
    private static final AtomicBoolean muted = new AtomicBoolean(false);
    private static volatile AudioAlarmConfig config = AudioAlarmConfig.load();
    private static final List<Consumer<String>> visualFlashListeners = new CopyOnWriteArrayList<>();

    private static final ExecutorService audioExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SoundEngine-Worker");
        t.setDaemon(true);
        return t;
    });

    // Pre-synthesized audio buffers for zero-latency instant playback
    private static final byte[] SUCCESS_CHIME_BUFFER = synthesizeSuccessChime();
    private static final byte[] ALERT_SOUND_BUFFER = synthesizeAlertSound();
    private static final byte[] START_TONE_BUFFER = synthesizeStartTone();
    private static final byte[] ABORT_TONE_BUFFER = synthesizeAbortTone();
    private static final byte[] PASS_COMPLETE_CHIME_BUFFER = synthesizePassChime();

    public static AudioAlarmConfig getConfig() {
        if (config == null) {
            config = AudioAlarmConfig.load();
        }
        return config;
    }

    public static void setConfig(AudioAlarmConfig newConfig) {
        if (newConfig != null) {
            config = newConfig;
            muted.set(newConfig.getAlarmMode() == AudioAlarmConfig.AlarmMode.MUTED);
        }
    }

    public static void addVisualFlashListener(Consumer<String> listener) {
        if (listener != null) visualFlashListeners.add(listener);
    }

    public static void removeVisualFlashListener(Consumer<String> listener) {
        visualFlashListeners.remove(listener);
    }

    public static void triggerVisualFlash(String alertType) {
        if (getConfig().isVisualFlashEnabled()) {
            for (Consumer<String> listener : visualFlashListeners) {
                try {
                    listener.accept(alertType);
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isMuted() {
        return muted.get() || getConfig().getAlarmMode() == AudioAlarmConfig.AlarmMode.MUTED;
    }

    public static void setMuted(boolean isMuted) {
        muted.set(isMuted);
    }

    public static boolean toggleMute() {
        boolean newState = !muted.get();
        muted.set(newState);
        return newState;
    }

    /**
     * Plays a satisfying, harmonic ascending major chord chime (C5 -> E5 -> G5 -> C6)
     * with an acoustic exponential bell resonance.
     */
    public static void playSuccessChime() {
        playBufferAsync(SUCCESS_CHIME_BUFFER);
    }

    /**
     * Plays an urgent double-pulse warning siren (440Hz -> 220Hz alert buzz)
     * for premature drive disconnections or verification failures.
     */
    public static void playAlertSound() {
        playBufferAsync(ALERT_SOUND_BUFFER);
    }

    /**
     * Plays a subtle, high-tech mechanical activation confirmation blip.
     */
    public static void playStartTone() {
        playBufferAsync(START_TONE_BUFFER);
    }

    /**
     * Plays a descending muted notification tone for user abort actions.
     */
    public static void playAbortTone() {
        playBufferAsync(ABORT_TONE_BUFFER);
    }

    /**
     * Plays an audible pass-progression tone (F5 -> A5 ascending interval).
     */
    public static void playPassChime() {
        playBufferAsync(PASS_COMPLETE_CHIME_BUFFER);
    }

    /**
     * Announces wiping pass completion (Chime and/or Voice).
     */
    public static void playPassComplete(String driveName, int passNum, int totalPasses) {
        AudioAlarmConfig cfg = getConfig();
        if (!cfg.isEnablePassCompleteAlarm() || isMuted()) return;

        triggerVisualFlash("PASS_COMPLETE");

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            playPassChime();
        }

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            String speech = String.format("Pass %d of %d complete on %s.", passNum, totalPasses, cleanDriveName(driveName));
            TextToSpeechEngine.getInstance().speakAsync(speech, cfg.getVolume());
        }
    }

    /**
     * Plays full sanitization & certificate completion alarm (Harmonic Chime + Voice Attestation).
     */
    public static void playSanitizationComplete(String driveName, String standardName, double residualEntropy) {
        AudioAlarmConfig cfg = getConfig();
        if (!cfg.isEnableJobCompleteAlarm() || isMuted()) return;

        triggerVisualFlash("JOB_COMPLETE");

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            playSuccessChime();
        }

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            String speech = cfg.formatAnnouncement(driveName, standardName, residualEntropy, "PASSED");
            TextToSpeechEngine.getInstance().speakAsync(speech, cfg.getVolume());
        }
    }

    /**
     * Plays audio warning siren + voice announcement for thermal throttling events (>65°C).
     */
    public static void playThermalAlert(String driveName, int currentTemp, int maxTemp) {
        AudioAlarmConfig cfg = getConfig();
        if (!cfg.isEnableThermalAlertAlarm() || isMuted()) return;

        triggerVisualFlash("THERMAL_WARNING");
        playAlertSound();

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            String speech = String.format("Warning: Thermal throttle activated on %s. Temperature reached %d degrees Celsius.",
                    cleanDriveName(driveName), currentTemp);
            TextToSpeechEngine.getInstance().speakAsync(speech, cfg.getVolume());
        }
    }

    /**
     * Plays alert siren and voice notification for verification failures or premature drive removal.
     */
    public static void playVerificationFailure(String driveName, String reason) {
        AudioAlarmConfig cfg = getConfig();
        if (!cfg.isEnableVerificationFailureAlarm() || isMuted()) return;

        triggerVisualFlash("VERIFICATION_FAILED");
        playAlertSound();

        if (cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY ||
            cfg.getAlarmMode() == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            String speech = String.format("Alert: Sanitization verification failed on %s. %s",
                    cleanDriveName(driveName), reason != null ? reason : "Residual data detected.");
            TextToSpeechEngine.getInstance().speakAsync(speech, cfg.getVolume());
        }
    }

    /**
     * Previews/tests the alarm based on the specified or current alarm mode.
     */
    public static void testAlarm(AudioAlarmConfig.AlarmMode testMode) {
        AudioAlarmConfig.AlarmMode mode = testMode != null ? testMode : getConfig().getAlarmMode();
        triggerVisualFlash("TEST_ALARM");

        if (mode == AudioAlarmConfig.AlarmMode.CHIME_ONLY || mode == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            playSuccessChime();
        }

        if (mode == AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY || mode == AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH) {
            String previewSpeech = getConfig().formatAnnouncement("disk 2", "NIST SP 800-88 Rev. 1 Clear", 0.0000, "PASSED");
            TextToSpeechEngine.getInstance().speakAsync(previewSpeech, getConfig().getVolume());
        }
    }

    public static void speak(String text) {
        if (!isMuted()) {
            TextToSpeechEngine.getInstance().speakAsync(text, getConfig().getVolume());
        }
    }

    private static String cleanDriveName(String drive) {
        if (drive == null) return "drive";
        return drive.replaceAll("^/dev/[r]?", "").replaceAll("^\\\\\\\\\\.\\\\", "").replaceAll("(?i)disk(\\d+)", "disk $1");
    }

    private static void playBufferAsync(byte[] pcmData) {
        if (isMuted() || pcmData == null || pcmData.length == 0) return;

        audioExecutor.submit(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    return;
                }

                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format, pcmData.length);
                    line.start();
                    line.write(pcmData, 0, pcmData.length);
                    line.drain();
                }
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Audio playback error: " + e.getMessage());
            }
        });
    }

    /**
     * Synthesizes an ascending harmonic 4-tone chime:
     * C5 (523.25Hz), E5 (659.25Hz), G5 (783.99Hz), C6 (1046.50Hz).
     */
    private static byte[] synthesizeSuccessChime() {
        double durationSec = 1.0;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        double[] freqs = {523.25, 659.25, 783.99, 1046.50};
        double[] startTimes = {0.0, 0.08, 0.16, 0.24};
        double[] decayRates = {0.25, 0.30, 0.38, 0.55};

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double sampleValue = 0.0;

            for (int n = 0; n < freqs.length; n++) {
                if (t >= startTimes[n]) {
                    double noteT = t - startTimes[n];
                    double envelope = Math.exp(-noteT / decayRates[n]);
                    double wave = Math.sin(2 * Math.PI * freqs[n] * noteT)
                            + 0.25 * Math.sin(2 * Math.PI * (freqs[n] * 2) * noteT);
                    sampleValue += wave * envelope * 0.28;
                }
            }

            sampleValue = Math.max(-1.0, Math.min(1.0, sampleValue));
            short shortSample = (short) (sampleValue * 32767.0);

            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a 2-tone pass completion interval (F5 698.46Hz -> A5 880.00Hz).
     */
    private static byte[] synthesizePassChime() {
        double durationSec = 0.45;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        double[] freqs = {698.46, 880.00};
        double[] startTimes = {0.0, 0.10};
        double[] decayRates = {0.20, 0.30};

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double sampleValue = 0.0;

            for (int n = 0; n < freqs.length; n++) {
                if (t >= startTimes[n]) {
                    double noteT = t - startTimes[n];
                    double envelope = Math.exp(-noteT / decayRates[n]);
                    double wave = Math.sin(2 * Math.PI * freqs[n] * noteT);
                    sampleValue += wave * envelope * 0.3;
                }
            }

            sampleValue = Math.max(-1.0, Math.min(1.0, sampleValue));
            short shortSample = (short) (sampleValue * 32767.0);

            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a double warning alert buzzer pulse (440Hz -> 260Hz).
     */
    private static byte[] synthesizeAlertSound() {
        double durationSec = 0.55;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double sampleValue = 0.0;

            if (t < 0.20) {
                double envelope = Math.sin(Math.PI * (t / 0.20));
                sampleValue = Math.sin(2 * Math.PI * 440.0 * t) * envelope * 0.5;
            } else if (t >= 0.28 && t < 0.48) {
                double pulseT = t - 0.28;
                double envelope = Math.sin(Math.PI * (pulseT / 0.20));
                sampleValue = Math.sin(2 * Math.PI * 260.0 * pulseT) * envelope * 0.6;
            }

            sampleValue = Math.max(-1.0, Math.min(1.0, sampleValue));
            short shortSample = (short) (sampleValue * 32767.0);

            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a crisp mechanical activation blip (1100 Hz for 45ms).
     */
    private static byte[] synthesizeStartTone() {
        double durationSec = 0.06;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double envelope = Math.exp(-t / 0.02);
            double sampleValue = Math.sin(2 * Math.PI * 1100.0 * t) * envelope * 0.4;

            short shortSample = (short) (sampleValue * 32767.0);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }
        return buffer;
    }

    /**
     * Synthesizes a soft descending notification tone (480Hz -> 320Hz).
     */
    private static byte[] synthesizeAbortTone() {
        double durationSec = 0.3;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double freq = 480.0 - (t / durationSec) * 160.0;
            double envelope = Math.exp(-t / 0.12);
            double sampleValue = Math.sin(2 * Math.PI * freq * t) * envelope * 0.4;

            short shortSample = (short) (sampleValue * 32767.0);
            buffer[i * 2] = (byte) (shortSample & 0xFF);
            buffer[i * 2 + 1] = (byte) ((shortSample >> 8) & 0xFF);
        }
        return buffer;
    }
}
