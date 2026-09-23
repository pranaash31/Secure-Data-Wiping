package com.sanitizer.audio;

import com.sanitizer.util.AppLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cross-platform Text-To-Speech (TTS) Engine.
 * Synthesizes voice announcements natively on macOS, Windows, and Linux.
 */
public class TextToSpeechEngine {

    private static final String MODULE = "TextToSpeechEngine";

    private static final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "TTS-Voice-Worker");
        t.setDaemon(true);
        return t;
    });

    private static class InstanceHolder {
        private static final TextToSpeechEngine INSTANCE = new TextToSpeechEngine();
    }

    public static TextToSpeechEngine getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private TextToSpeechEngine() {}

    /**
     * Speaks the provided text asynchronously on the background worker thread.
     */
    public void speakAsync(String text, double volume) {
        if (text == null || text.isBlank() || volume <= 0.0) {
            return;
        }

        // Sanitize string to prevent command injection
        String sanitizedText = text.replaceAll("[^a-zA-Z0-9.,:;!?'% -]", "").trim();
        if (sanitizedText.isEmpty()) {
            return;
        }

        ttsExecutor.submit(() -> {
            try {
                executeSpeechCommand(sanitizedText, volume);
            } catch (Exception e) {
                AppLogger.warn(MODULE, "TTS synthesis failed: " + e.getMessage());
            }
        });
    }

    private void executeSpeechCommand(String text, double volume) {
        String os = System.getProperty("os.name", "").toLowerCase();
        ProcessBuilder pb = null;

        if (os.contains("mac") || os.contains("darwin")) {
            // macOS native 'say' command with default voice
            pb = new ProcessBuilder("say", text);
        } else if (os.contains("win")) {
            // Windows PowerShell System.Speech SAPI
            int winVolume = (int) Math.round(volume * 100);
            String psScript = String.format(
                    "Add-Type -AssemblyName System.Speech; " +
                    "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "$synth.Volume = %d; " +
                    "$synth.Speak('%s');",
                    winVolume, text.replace("'", "''")
            );
            pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", psScript);
        } else if (os.contains("linux") || os.contains("nix")) {
            // Linux spd-say or espeak
            if (isCommandAvailable("spd-say")) {
                pb = new ProcessBuilder("spd-say", "-r", "0", text);
            } else if (isCommandAvailable("espeak")) {
                pb = new ProcessBuilder("espeak", text);
            }
        }

        if (pb != null) {
            try {
                Process process = pb.start();
                process.waitFor();
            } catch (Exception e) {
                AppLogger.warn(MODULE, "Could not execute platform TTS command: " + e.getMessage());
            }
        }
    }

    private boolean isCommandAvailable(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
