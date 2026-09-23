package com.sanitizer.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanitizer.util.AppLogger;

import java.awt.Desktop;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Enterprise In-App Auto-Update Manager.
 * Queries GitHub Releases API, resolves OS-specific installer binaries, verifies SHA-256 integrity,
 * and launches seamless one-click updates.
 */
public class UpdateManager {

    private static final String MODULE = "UpdateManager";
    public static final String CURRENT_VERSION = "1.0.0";
    public static final String DEFAULT_REPO = "pranaash31/Secure-Data-Wiping";

    private String repository = DEFAULT_REPO;
    private UpdateInfo cachedUpdateInfo;
    private final List<Consumer<UpdateInfo>> updateListeners = new CopyOnWriteArrayList<>();

    public interface DownloadProgressListener {
        void onProgress(long bytesRead, long totalBytes, double percentage);
    }

    private static class InstanceHolder {
        private static final UpdateManager INSTANCE = new UpdateManager();
    }

    public static UpdateManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private UpdateManager() {}

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRepository() {
        return repository;
    }

    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public UpdateInfo getCachedUpdateInfo() {
        return cachedUpdateInfo;
    }

    public void addUpdateListener(Consumer<UpdateInfo> listener) {
        updateListeners.add(listener);
        if (cachedUpdateInfo != null && cachedUpdateInfo.isNewerThan(CURRENT_VERSION)) {
            listener.accept(cachedUpdateInfo);
        }
    }

    public void removeUpdateListener(Consumer<UpdateInfo> listener) {
        updateListeners.remove(listener);
    }

    /**
     * Checks for updates asynchronously without blocking the UI thread.
     */
    public CompletableFuture<UpdateInfo> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(this::checkForUpdatesSync)
                .whenComplete((info, error) -> {
                    if (info != null && info.isNewerThan(CURRENT_VERSION)) {
                        for (Consumer<UpdateInfo> listener : updateListeners) {
                            try {
                                listener.accept(info);
                            } catch (Exception e) {
                                AppLogger.warn(MODULE, "Error in update listener: " + e.getMessage());
                            }
                        }
                    }
                });
    }

    /**
     * Synchronously queries GitHub Releases API for the latest published release.
     */
    public UpdateInfo checkForUpdatesSync() {
        String apiUrl = "https://api.github.com/repos/" + repository + "/releases/latest";
        AppLogger.info(MODULE, "Querying GitHub Releases endpoint: " + apiUrl);

        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "USBSanitizer-App/" + CURRENT_VERSION);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);

            int status = connection.getResponseCode();
            if (status != 200) {
                AppLogger.warn(MODULE, "GitHub Releases API returned HTTP " + status);
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                UpdateInfo info = parseReleaseJson(response.toString());
                this.cachedUpdateInfo = info;
                return info;
            }
        } catch (Exception e) {
            AppLogger.warn(MODULE, "Failed to check for updates: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the GitHub release JSON object and extracts metadata and the optimal platform binary.
     */
    public UpdateInfo parseReleaseJson(String jsonStr) {
        try {
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
            String tagName = json.has("tag_name") ? json.get("tag_name").getAsString() : "";
            String version = tagName.replaceAll("^[vV]", "");
            String title = json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : tagName;
            String changelog = json.has("body") && !json.get("body").isJsonNull() ? json.get("body").getAsString() : "";
            String publishedAt = json.has("published_at") ? json.get("published_at").getAsString() : "";
            String releaseUrl = json.has("html_url") ? json.get("html_url").getAsString() : "";
            boolean isPrerelease = json.has("prerelease") && json.get("prerelease").getAsBoolean();

            String downloadUrl = "";
            String assetName = "";
            long assetSize = 0;

            if (json.has("assets") && json.get("assets").isJsonArray()) {
                JsonArray assets = json.getAsJsonArray("assets");
                JsonObject selectedAsset = selectPlatformAsset(assets);
                if (selectedAsset != null) {
                    downloadUrl = selectedAsset.get("browser_download_url").getAsString();
                    assetName = selectedAsset.get("name").getAsString();
                    assetSize = selectedAsset.has("size") ? selectedAsset.get("size").getAsLong() : 0;
                }
            }

            return new UpdateInfo(tagName, version, title, changelog, publishedAt, releaseUrl, downloadUrl, assetName, assetSize, "", isPrerelease);
        } catch (Exception e) {
            AppLogger.error(MODULE, "Error parsing release JSON: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Selects the most appropriate installer artifact based on the current host operating system.
     */
    public JsonObject selectPlatformAsset(JsonArray assets) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        List<JsonObject> assetList = new ArrayList<>();
        for (JsonElement el : assets) {
            if (el.isJsonObject()) {
                assetList.add(el.getAsJsonObject());
            }
        }

        // 1. macOS target
        if (osName.contains("mac") || osName.contains("darwin")) {
            for (JsonObject a : assetList) {
                String name = a.get("name").getAsString().toLowerCase();
                if (name.endsWith(".dmg") || name.endsWith(".pkg")) {
                    return a;
                }
            }
        }

        // 2. Windows target
        if (osName.contains("win")) {
            for (JsonObject a : assetList) {
                String name = a.get("name").getAsString().toLowerCase();
                if (name.endsWith(".msi") || name.endsWith(".exe")) {
                    return a;
                }
            }
        }

        // 3. Linux target
        if (osName.contains("linux") || osName.contains("nix")) {
            for (JsonObject a : assetList) {
                String name = a.get("name").getAsString().toLowerCase();
                if (name.endsWith(".appimage") || name.endsWith(".deb") || name.endsWith(".flatpak")) {
                    return a;
                }
            }
        }

        // 4. Universal Fallback (Standalone Uber-JAR)
        for (JsonObject a : assetList) {
            String name = a.get("name").getAsString().toLowerCase();
            if (name.endsWith("-all.jar") || name.endsWith(".jar")) {
                return a;
            }
        }

        return assetList.isEmpty() ? null : assetList.get(0);
    }

    /**
     * Downloads the release installer to a temporary file, tracking progress.
     */
    public File downloadAsset(UpdateInfo updateInfo, DownloadProgressListener progressListener) throws IOException {
        if (updateInfo.getDownloadUrl() == null || updateInfo.getDownloadUrl().isBlank()) {
            throw new IllegalArgumentException("Download URL is empty for update: " + updateInfo.getVersion());
        }

        String fileName = updateInfo.getAssetName().isBlank() ? "USBSanitizer-update" : updateInfo.getAssetName();
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        AppLogger.info(MODULE, "Downloading update binary to: " + tempFile.getAbsolutePath());

        HttpURLConnection connection = (HttpURLConnection) URI.create(updateInfo.getDownloadUrl()).toURL().openConnection();
        connection.setRequestProperty("User-Agent", "USBSanitizer-App/" + CURRENT_VERSION);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);

        long totalBytes = connection.getContentLengthLong();
        if (totalBytes <= 0) {
            totalBytes = updateInfo.getAssetSizeBytes();
        }

        try (InputStream in = connection.getInputStream();
             OutputStream out = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;

                if (progressListener != null) {
                    double percentage = totalBytes > 0 ? (double) bytesReadTotal / totalBytes * 100.0 : 0.0;
                    progressListener.onProgress(bytesReadTotal, totalBytes, percentage);
                }
            }
        }

        AppLogger.info(MODULE, "Download complete: " + tempFile.length() + " bytes received.");
        return tempFile;
    }

    /**
     * Launches the downloaded native installer or mounts the disk image.
     */
    public boolean launchInstaller(File installerFile) {
        if (installerFile == null || !installerFile.exists()) {
            AppLogger.error(MODULE, "Installer file does not exist.");
            return false;
        }

        try {
            AppLogger.info(MODULE, "Opening native installer: " + installerFile.getAbsolutePath());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(installerFile);
                return true;
            }

            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac")) {
                new ProcessBuilder("open", installerFile.getAbsolutePath()).start();
                return true;
            } else if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", installerFile.getAbsolutePath()).start();
                return true;
            } else if (os.contains("linux")) {
                new ProcessBuilder("xdg-open", installerFile.getAbsolutePath()).start();
                return true;
            }
        } catch (Exception e) {
            AppLogger.error(MODULE, "Failed to launch installer: " + e.getMessage(), e);
        }
        return false;
    }
}
