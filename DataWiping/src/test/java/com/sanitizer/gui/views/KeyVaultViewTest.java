package com.sanitizer.gui.views;

import com.sanitizer.crypto.CryptoSigner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Key Vault View Unit Tests")
class KeyVaultViewTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("Verify KeyVaultView instantiates cleanly with cryptographic fingerprint")
    void testKeyVaultViewInitialization() {
        KeyVaultView view = new KeyVaultView();

        assertThat(view).isNotNull();
        assertThat(view.getRoot()).isNotNull();

        String fingerprint = CryptoSigner.getPublicKeyFingerprint();
        assertThat(fingerprint).isNotBlank().contains(":");
    }
}
