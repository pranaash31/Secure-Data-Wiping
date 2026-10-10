package com.sanitizer.service;

/**
 * Standardized export formats supported by enterprise audit reporting.
 */
public enum ExportFormat {
    CSV("CSV", ".csv", "CSV Comma Delimited (*.csv)"),
    JSON("JSON", ".json", "JSON Data (*.json)"),
    EXCEL("EXCEL", ".xml", "Excel Spreadsheet XML (*.xml, *.xls)");

    private final String label;
    private final String extension;
    private final String description;

    ExportFormat(String label, String extension, String description) {
        this.label = label;
        this.extension = extension;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getExtension() {
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public static ExportFormat fromString(String format) {
        if (format == null) {
            return CSV;
        }
        return switch (format.trim().toUpperCase()) {
            case "JSON" -> JSON;
            case "EXCEL", "XML", "XLS" -> EXCEL;
            default -> CSV;
        };
    }
}
