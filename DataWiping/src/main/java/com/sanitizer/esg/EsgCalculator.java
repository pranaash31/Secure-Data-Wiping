package com.sanitizer.esg;

import com.sanitizer.db.AuditDb;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise ESG (Environmental, Social, Governance) & Carbon Footprint Calculator.
 * <p>
 * Models IT Asset Disposition (ITAD) Scope 3 greenhouse gas mitigation and
 * circular economy e-waste prevention metrics against standard physical drive shredding.
 * <p>
 * Benchmark Baseline:
 * Standard 500 GB Storage Media Drive (HDD/SSD/Flash):
 * - Prevents 1.4 kg of hazardous e-waste (drive chassis, PCB silicon, neodymium, casing, lifecycle packaging).
 * - Saves 12.6 kg of CO₂ emissions (avoided virgin manufacturing extraction + physical demanufacturing shredding energy).
 */
public class EsgCalculator {

    private static final Pattern CAPACITY_PATTERN = Pattern.compile("([0-9.]+)\\s*([A-Za-z]+)?");

    /**
     * Immutable container for ESG & Sustainability calculations.
     */
    public record EsgMetrics(
            double capacityGb,
            double eWasteDivertedKg,
            double co2EmissionsSavedKg,
            double treesEquivalent,
            double energySavedKwh,
            String impactStatement
    ) {}

    /**
     * Calculates ESG impact metrics for a given storage capacity representation.
     *
     * @param capacityStr capacity string such as "500 GB", "1 TB", "256 GB", "2.0 TB", "128 GB", "32 GB", or raw numbers.
     * @return Calculated {@link EsgMetrics} object.
     */
    public static EsgMetrics calculate(String capacityStr) {
        double capacityGb = parseCapacityToGb(capacityStr);
        return calculateFromGb(capacityGb, capacityStr != null && !capacityStr.isBlank() ? capacityStr.trim() : formatGb(capacityGb));
    }

    /**
     * Calculates ESG impact metrics directly from capacity in gigabytes.
     *
     * @param capacityGb Capacity in Gigabytes.
     * @return Calculated {@link EsgMetrics} object.
     */
    public static EsgMetrics calculateFromGb(double capacityGb) {
        return calculateFromGb(capacityGb, formatGb(capacityGb));
    }

    private static EsgMetrics calculateFromGb(double capacityGb, String displayCapacity) {
        double safeGb = Math.max(1.0, capacityGb);

        // ITAD Scale Factor: normalized to 500 GB baseline (1.4 kg e-waste, 12.6 kg CO2)
        // Exact benchmark matching for 500 GB
        double eWasteKg;
        double co2Kg;

        if (Math.abs(safeGb - 500.0) < 1.0) {
            eWasteKg = 1.4;
            co2Kg = 12.6;
        } else {
            eWasteKg = Math.round((1.4 * Math.pow(safeGb / 500.0, 0.45)) * 10.0) / 10.0;
            co2Kg = Math.round((12.6 * Math.pow(safeGb / 500.0, 0.60)) * 10.0) / 10.0;
        }

        // Ensure reasonable minimum bounds
        eWasteKg = Math.max(0.2, eWasteKg);
        co2Kg = Math.max(1.5, co2Kg);

        // Equivalencies:
        // 1 tree seedling grown for 10 years absorbs ~60 kg CO2
        double trees = Math.round((co2Kg / 60.0) * 100.0) / 100.0;
        // 1 kg CO2 avoided is equivalent to ~1.5 kWh of clean manufacturing power
        double energyKwh = Math.round((co2Kg * 1.5) * 10.0) / 10.0;

        String statement = String.format(
                Locale.US,
                "By securely sanitizing this %s drive for reuse, you prevented %.1f kg of e-waste and saved %.1f kg of CO₂ emissions compared to physical shredding.",
                displayCapacity,
                eWasteKg,
                co2Kg
        );

        return new EsgMetrics(safeGb, eWasteKg, co2Kg, trees, energyKwh, statement);
    }

    /**
     * Aggregates ESG sustainability metrics across a collection of audit records.
     *
     * @param records List of historical or batch audit records.
     * @return Aggregate {@link EsgMetrics} across all drives.
     */
    public static EsgMetrics calculateAggregate(List<AuditDb.AuditRecord> records) {
        if (records == null || records.isEmpty()) {
            return new EsgMetrics(0, 0, 0, 0, 0, "No records available for ESG calculation.");
        }

        double totalGb = 0;
        double totalEWaste = 0;
        double totalCo2 = 0;

        for (AuditDb.AuditRecord r : records) {
            EsgMetrics m = calculate(r.capacity());
            totalGb += m.capacityGb();
            totalEWaste += m.eWasteDivertedKg();
            totalCo2 += m.co2EmissionsSavedKg();
        }

        totalEWaste = Math.round(totalEWaste * 10.0) / 10.0;
        totalCo2 = Math.round(totalCo2 * 10.0) / 10.0;
        double totalTrees = Math.round((totalCo2 / 60.0) * 100.0) / 100.0;
        double totalEnergy = Math.round((totalCo2 * 1.5) * 10.0) / 10.0;

        String statement = String.format(
                Locale.US,
                "Across %d sanitized devices (%s total storage), your organization prevented %.1f kg of e-waste and mitigated %.1f kg of CO₂ emissions (equivalent to %.2f tree seedlings grown for 10 years).",
                records.size(),
                formatGb(totalGb),
                totalEWaste,
                totalCo2,
                totalTrees
        );

        return new EsgMetrics(totalGb, totalEWaste, totalCo2, totalTrees, totalEnergy, statement);
    }

    /**
     * Parses standard drive capacity strings into numerical Gigabytes.
     */
    public static double parseCapacityToGb(String capacityStr) {
        if (capacityStr == null || capacityStr.isBlank()) {
            return 500.0; // Standard default
        }

        String cleaned = capacityStr.trim().toUpperCase(Locale.ROOT);
        Matcher matcher = CAPACITY_PATTERN.matcher(cleaned);

        if (!matcher.find()) {
            return 500.0;
        }

        try {
            double val = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);

            if (unit == null || unit.startsWith("GB") || unit.equals("G")) {
                return val;
            } else if (unit.startsWith("TB") || unit.equals("T")) {
                return val * 1000.0;
            } else if (unit.startsWith("MB") || unit.equals("M")) {
                return Math.max(0.1, val / 1000.0);
            } else if (unit.startsWith("PB") || unit.equals("P")) {
                return val * 1000000.0;
            }
            return val;
        } catch (Exception e) {
            return 500.0;
        }
    }

    public static String formatGb(double gb) {
        if (gb >= 1000.0) {
            return String.format(Locale.US, "%.1f TB", gb / 1000.0);
        }
        return String.format(Locale.US, "%.0f GB", gb);
    }
}
