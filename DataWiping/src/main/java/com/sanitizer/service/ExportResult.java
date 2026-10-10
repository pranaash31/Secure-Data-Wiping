package com.sanitizer.service;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Result record encapsulating the output of an audit export or report generation task.
 */
public record ExportResult(
        boolean success,
        String message,
        List<File> exportedFiles,
        String primaryFilePath,
        Throwable error
) {
    public static ExportResult success(String message, File file) {
        List<File> files = (file != null) ? List.of(file) : Collections.emptyList();
        String path = (file != null) ? file.getAbsolutePath() : null;
        return new ExportResult(true, message, files, path, null);
    }

    public static ExportResult success(String message, List<File> files) {
        String primary = (files != null && !files.isEmpty()) ? files.get(0).getAbsolutePath() : null;
        return new ExportResult(true, message, (files != null) ? files : Collections.emptyList(), primary, null);
    }

    public static ExportResult success(String message, String primaryFilePath) {
        File file = (primaryFilePath != null && !primaryFilePath.isBlank()) ? new File(primaryFilePath) : null;
        List<File> files = (file != null) ? List.of(file) : Collections.emptyList();
        return new ExportResult(true, message, files, primaryFilePath, null);
    }

    public static ExportResult failure(String message, Throwable error) {
        return new ExportResult(false, message, Collections.emptyList(), null, error);
    }
}
