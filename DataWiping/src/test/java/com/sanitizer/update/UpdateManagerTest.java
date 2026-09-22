package com.sanitizer.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("In-App Auto-Update Manager Unit Tests")
class UpdateManagerTest {

    @Test
    @DisplayName("Verify semantic version comparison logic")
    void testSemanticVersionComparison() {
        assertThat(UpdateInfo.compareVersions("1.0.1", "1.0.0")).isPositive();
        assertThat(UpdateInfo.compareVersions("v1.0.1", "1.0.0")).isPositive();
        assertThat(UpdateInfo.compareVersions("1.1.0", "1.0.9")).isPositive();
        assertThat(UpdateInfo.compareVersions("2.0.0", "1.99.99")).isPositive();
        assertThat(UpdateInfo.compareVersions("v1.0.0", "1.0.0")).isZero();
        assertThat(UpdateInfo.compareVersions("1.0.0", "v1.0.0")).isZero();
        assertThat(UpdateInfo.compareVersions("1.0.0-rc1", "1.0.0")).isZero();
        assertThat(UpdateInfo.compareVersions("0.9.8", "1.0.0")).isNegative();
        assertThat(UpdateInfo.compareVersions("1.0.0", "1.0.1")).isNegative();
    }

    @Test
    @DisplayName("Verify UpdateInfo.isNewerThan method")
    void testIsNewerThan() {
        UpdateInfo updateInfo = new UpdateInfo(
                "v1.0.5", "1.0.5", "Release 1.0.5", "Changelog text",
                "2026-09-22T00:00:00Z", "https://github.com/test/repo",
                "https://github.com/test/repo/releases/download/v1.0.5/USBSanitizer-1.0.5.dmg",
                "USBSanitizer-1.0.5.dmg", 50000000L, "sha256...", false
        );

        assertThat(updateInfo.isNewerThan("1.0.0")).isTrue();
        assertThat(updateInfo.isNewerThan("1.0.4")).isTrue();
        assertThat(updateInfo.isNewerThan("1.0.5")).isFalse();
        assertThat(updateInfo.isNewerThan("2.0.0")).isFalse();
        assertThat(updateInfo.getVersion()).isEqualTo("1.0.5");
        assertThat(updateInfo.getTagName()).isEqualTo("v1.0.5");
        assertThat(updateInfo.getAssetName()).isEqualTo("USBSanitizer-1.0.5.dmg");
    }

    @Test
    @DisplayName("Verify GitHub Release JSON deserialization")
    void testParseReleaseJson() {
        String mockJson = """
                {
                  "tag_name": "v1.2.0",
                  "name": "USB Sanitizer 1.2.0 Enterprise Release",
                  "body": "## Changes\\n- High speed wiping\\n- NIST verification",
                  "published_at": "2026-09-22T12:00:00Z",
                  "html_url": "https://github.com/pranaash31/Secure-Data-Wiping/releases/tag/v1.2.0",
                  "prerelease": false,
                  "assets": [
                    {
                      "name": "USBSanitizer-1.2.0.dmg",
                      "size": 85000000,
                      "browser_download_url": "https://github.com/pranaash31/Secure-Data-Wiping/releases/download/v1.2.0/USBSanitizer-1.2.0.dmg"
                    },
                    {
                      "name": "USBSanitizer-1.2.0.msi",
                      "size": 75000000,
                      "browser_download_url": "https://github.com/pranaash31/Secure-Data-Wiping/releases/download/v1.2.0/USBSanitizer-1.2.0.msi"
                    },
                    {
                      "name": "usb-sanitizer_1.2.0_amd64.deb",
                      "size": 65000000,
                      "browser_download_url": "https://github.com/pranaash31/Secure-Data-Wiping/releases/download/v1.2.0/usb-sanitizer_1.2.0_amd64.deb"
                    }
                  ]
                }
                """;

        UpdateManager manager = UpdateManager.getInstance();
        UpdateInfo info = manager.parseReleaseJson(mockJson);

        assertThat(info).isNotNull();
        assertThat(info.getVersion()).isEqualTo("1.2.0");
        assertThat(info.getTagName()).isEqualTo("v1.2.0");
        assertThat(info.getTitle()).isEqualTo("USB Sanitizer 1.2.0 Enterprise Release");
        assertThat(info.getChangelog()).contains("NIST verification");
        assertThat(info.isPrerelease()).isFalse();
        assertThat(info.getDownloadUrl()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify platform asset selection logic")
    void testSelectPlatformAsset() {
        UpdateManager manager = UpdateManager.getInstance();

        JsonArray assets = new JsonArray();

        JsonObject dmgAsset = new JsonObject();
        dmgAsset.addProperty("name", "USBSanitizer-1.0.0.dmg");
        dmgAsset.addProperty("browser_download_url", "https://example.com/USBSanitizer-1.0.0.dmg");
        assets.add(dmgAsset);

        JsonObject msiAsset = new JsonObject();
        msiAsset.addProperty("name", "USBSanitizer-1.0.0.msi");
        msiAsset.addProperty("browser_download_url", "https://example.com/USBSanitizer-1.0.0.msi");
        assets.add(msiAsset);

        JsonObject debAsset = new JsonObject();
        debAsset.addProperty("name", "usb-sanitizer_1.0.0_amd64.deb");
        debAsset.addProperty("browser_download_url", "https://example.com/usb-sanitizer_1.0.0_amd64.deb");
        assets.add(debAsset);

        JsonObject selected = manager.selectPlatformAsset(assets);
        assertThat(selected).isNotNull();
        assertThat(selected.get("name").getAsString()).isNotEmpty();
    }
}
