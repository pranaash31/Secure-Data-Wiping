package com.sanitizer.update;

/**
 * Encapsulates release metadata retrieved from the GitHub Releases API.
 */
public class UpdateInfo {

    private final String tagName;
    private final String version;
    private final String title;
    private final String changelog;
    private final String publishedAt;
    private final String releaseUrl;
    private final String downloadUrl;
    private final String assetName;
    private final long assetSizeBytes;
    private final String sha256Digest;
    private final boolean isPrerelease;

    public UpdateInfo(String tagName, String version, String title, String changelog,
                      String publishedAt, String releaseUrl, String downloadUrl,
                      String assetName, long assetSizeBytes, String sha256Digest, boolean isPrerelease) {
        this.tagName = tagName != null ? tagName : "";
        this.version = version != null ? version : "";
        this.title = title != null ? title : "";
        this.changelog = changelog != null ? changelog : "";
        this.publishedAt = publishedAt != null ? publishedAt : "";
        this.releaseUrl = releaseUrl != null ? releaseUrl : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.assetName = assetName != null ? assetName : "";
        this.assetSizeBytes = assetSizeBytes;
        this.sha256Digest = sha256Digest != null ? sha256Digest : "";
        this.isPrerelease = isPrerelease;
    }

    public String getTagName() {
        return tagName;
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public String getReleaseUrl() {
        return releaseUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getAssetName() {
        return assetName;
    }

    public long getAssetSizeBytes() {
        return assetSizeBytes;
    }

    public String getSha256Digest() {
        return sha256Digest;
    }

    public boolean isPrerelease() {
        return isPrerelease;
    }

    /**
     * Compares this release version against an existing version string using semantic versioning.
     * Returns true if this release version is strictly newer than the given version.
     */
    public boolean isNewerThan(String currentVersion) {
        return compareVersions(this.version, currentVersion) > 0;
    }

    /**
     * Compares two semantic version strings (e.g., "1.2.0" vs "1.1.9" or "v1.0.1" vs "1.0.0").
     * Returns positive if v1 > v2, negative if v1 < v2, zero if equal.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "0.0.0";
        if (v2 == null) v2 = "0.0.0";

        // Clean prefix 'v' or 'V'
        String clean1 = v1.trim().replaceAll("^[vV]", "").split("-")[0];
        String clean2 = v2.trim().replaceAll("^[vV]", "").split("-")[0];

        String[] parts1 = clean1.split("\\.");
        String[] parts2 = clean2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = 0;
            int num2 = 0;

            if (i < parts1.length) {
                try {
                    num1 = Integer.parseInt(parts1[i].replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }
            if (i < parts2.length) {
                try {
                    num2 = Integer.parseInt(parts2[i].replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            }

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
                "version='" + version + '\'' +
                ", title='" + title + '\'' +
                ", assetName='" + assetName + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}
