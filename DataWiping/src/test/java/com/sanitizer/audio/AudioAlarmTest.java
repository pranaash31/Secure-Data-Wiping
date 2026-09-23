package com.sanitizer.audio;

import com.sanitizer.util.SoundManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Audio & Visual Completion Alarms Unit Tests")
class AudioAlarmTest {

    @Test
    @DisplayName("Verify AudioAlarmConfig default settings and volume clamping")
    void testConfigDefaultsAndVolume() {
        AudioAlarmConfig config = new AudioAlarmConfig();

        assertThat(config.getAlarmMode()).isEqualTo(AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH);
        assertThat(config.getVolume()).isEqualTo(0.85);
        assertThat(config.isEnablePassCompleteAlarm()).isTrue();
        assertThat(config.isEnableJobCompleteAlarm()).isTrue();
        assertThat(config.isEnableThermalAlertAlarm()).isTrue();
        assertThat(config.isEnableVerificationFailureAlarm()).isTrue();
        assertThat(config.isVisualFlashEnabled()).isTrue();

        config.setVolume(1.5);
        assertThat(config.getVolume()).isEqualTo(1.0);

        config.setVolume(-0.2);
        assertThat(config.getVolume()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Verify speech template variable interpolation and drive name sanitization")
    void testSpeechTemplateFormatting() {
        AudioAlarmConfig config = new AudioAlarmConfig();
        config.setSpeechTemplate("Drive {drive} sanitization complete: {standard} verified with status {status}. Residual entropy: {entropy} bits.");

        String announcement = config.formatAnnouncement("/dev/rdisk3", "NIST SP 800-88 Rev. 1 Clear", 0.0000, "PASSED");

        assertThat(announcement).contains("disk 3");
        assertThat(announcement).contains("NIST SP 800-88 Rev. 1 Clear");
        assertThat(announcement).contains("0.0000");
        assertThat(announcement).contains("PASSED");
    }

    @Test
    @DisplayName("Verify AlarmMode enum values and display names")
    void testAlarmModeEnum() {
        assertThat(AudioAlarmConfig.AlarmMode.values()).containsExactly(
                AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH,
                AudioAlarmConfig.AlarmMode.TTS_SPEECH_ONLY,
                AudioAlarmConfig.AlarmMode.CHIME_ONLY,
                AudioAlarmConfig.AlarmMode.MUTED
        );

        assertThat(AudioAlarmConfig.AlarmMode.CHIME_AND_SPEECH.getDisplayName()).contains("Chime");
        assertThat(AudioAlarmConfig.AlarmMode.MUTED.getDisplayName()).contains("Muted");
    }

    @Test
    @DisplayName("Verify SoundManager config propagation and mute state")
    void testSoundManagerConfigPropagation() {
        AudioAlarmConfig config = new AudioAlarmConfig();
        config.setAlarmMode(AudioAlarmConfig.AlarmMode.MUTED);

        SoundManager.setConfig(config);
        assertThat(SoundManager.isMuted()).isTrue();

        config.setAlarmMode(AudioAlarmConfig.AlarmMode.CHIME_ONLY);
        SoundManager.setConfig(config);
        assertThat(SoundManager.getConfig().getAlarmMode()).isEqualTo(AudioAlarmConfig.AlarmMode.CHIME_ONLY);
    }
}
