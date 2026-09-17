package com.sanitizer.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SoundManager Audio & Sound Design Unit Tests")
class SoundManagerTest {

    @Test
    @DisplayName("Verify mute toggles and audio trigger methods execute safely without errors")
    void testSoundManagerMuteAndPlayback() {
        // Initial state
        boolean initialMute = SoundManager.isMuted();

        // Test toggle
        boolean toggled = SoundManager.toggleMute();
        assertThat(toggled).isEqualTo(!initialMute);
        assertThat(SoundManager.isMuted()).isEqualTo(toggled);

        // Mute sound to test headless / CI safe execution
        SoundManager.setMuted(true);
        assertThat(SoundManager.isMuted()).isTrue();

        // Ensure triggers execute safely when muted
        SoundManager.playSuccessChime();
        SoundManager.playAlertSound();
        SoundManager.playStartTone();
        SoundManager.playAbortTone();

        // Unmute and test non-blocking playback triggers
        SoundManager.setMuted(false);
        assertThat(SoundManager.isMuted()).isFalse();

        SoundManager.playStartTone();
        SoundManager.playSuccessChime();
        SoundManager.playAlertSound();
        SoundManager.playAbortTone();

        // Restore initial state
        SoundManager.setMuted(initialMute);
    }
}
