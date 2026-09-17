package com.sanitizer.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-performance, zero-dependency Audio & Sound Design Engine.
 * Programmatically synthesizes 16-bit PCM harmonic chimes and alert sirens
 * with ADSR exponential decay envelopes for instantaneous, non-blocking playback.
 */
public class SoundManager {

    private static final String MODULE = "SoundManager";
    private static final float SAMPLE_RATE = 44100f;
    private static final AtomicBoolean muted = new AtomicBoolean(false);

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

    public static boolean isMuted() {
        return muted.get();
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

    private static void playBufferAsync(byte[] pcmData) {
        if (muted.get() || pcmData == null || pcmData.length == 0) return;

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
                    // Sine fundamental + subtle 2nd harmonic overtone for warmth
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
     * Synthesizes a double warning alert buzzer pulse (440Hz -> 260Hz).
     */
    private static byte[] synthesizeAlertSound() {
        double durationSec = 0.55;
        int totalSamples = (int) (SAMPLE_RATE * durationSec);
        byte[] buffer = new byte[totalSamples * 2];

        for (int i = 0; i < totalSamples; i++) {
            double t = i / SAMPLE_RATE;
            double sampleValue = 0.0;

            // Pulse 1: 0.00s - 0.20s (440 Hz)
            if (t < 0.20) {
                double envelope = Math.sin(Math.PI * (t / 0.20));
                sampleValue = Math.sin(2 * Math.PI * 440.0 * t) * envelope * 0.5;
            }
            // Pulse 2: 0.28s - 0.48s (260 Hz)
            else if (t >= 0.28 && t < 0.48) {
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
