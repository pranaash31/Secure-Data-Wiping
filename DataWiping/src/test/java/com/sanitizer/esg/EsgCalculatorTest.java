package com.sanitizer.esg;

import com.sanitizer.db.AuditDb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ESG & Sustainability Engine Unit Tests")
class EsgCalculatorTest {

    @Test
    @DisplayName("Verify exact baseline benchmark calculation for 500 GB drive")
    void testExact500GbBaseline() {
        EsgCalculator.EsgMetrics metrics = EsgCalculator.calculate("500 GB");

        assertThat(metrics.capacityGb()).isEqualTo(500.0);
        assertThat(metrics.eWasteDivertedKg()).isEqualTo(1.4);
        assertThat(metrics.co2EmissionsSavedKg()).isEqualTo(12.6);
        assertThat(metrics.treesEquivalent()).isGreaterThan(0.2);
        assertThat(metrics.energySavedKwh()).isGreaterThan(18.0);

        assertThat(metrics.impactStatement()).isEqualTo(
                "By securely sanitizing this 500 GB drive for reuse, you prevented 1.4 kg of e-waste and saved 12.6 kg of CO₂ emissions compared to physical shredding."
        );
    }

    @Test
    @DisplayName("Verify capacity string parsing (TB, GB, MB, PB, raw)")
    void testCapacityParsing() {
        assertThat(EsgCalculator.parseCapacityToGb("1 TB")).isEqualTo(1000.0);
        assertThat(EsgCalculator.parseCapacityToGb("2.0 TB")).isEqualTo(2000.0);
        assertThat(EsgCalculator.parseCapacityToGb("256 GB")).isEqualTo(256.0);
        assertThat(EsgCalculator.parseCapacityToGb("128GB")).isEqualTo(128.0);
        assertThat(EsgCalculator.parseCapacityToGb("512 MB")).isEqualTo(0.512);
        assertThat(EsgCalculator.parseCapacityToGb("1 PB")).isEqualTo(1000000.0);
        assertThat(EsgCalculator.parseCapacityToGb("")).isEqualTo(500.0);
        assertThat(EsgCalculator.parseCapacityToGb(null)).isEqualTo(500.0);
    }

    @Test
    @DisplayName("Verify dynamic scaling for larger and smaller drive capacities")
    void testDynamicCapacityScaling() {
        EsgCalculator.EsgMetrics metrics1Tb = EsgCalculator.calculate("1 TB");
        assertThat(metrics1Tb.eWasteDivertedKg()).isGreaterThan(1.4);
        assertThat(metrics1Tb.co2EmissionsSavedKg()).isGreaterThan(12.6);

        EsgCalculator.EsgMetrics metrics128Gb = EsgCalculator.calculate("128 GB");
        assertThat(metrics128Gb.eWasteDivertedKg()).isLessThan(1.4);
        assertThat(metrics128Gb.co2EmissionsSavedKg()).isLessThan(12.6);
        assertThat(metrics128Gb.eWasteDivertedKg()).isGreaterThanOrEqualTo(0.2);
        assertThat(metrics128Gb.co2EmissionsSavedKg()).isGreaterThanOrEqualTo(1.5);
    }

    @Test
    @DisplayName("Verify aggregate corporate ESG calculations across multiple audit records")
    void testAggregateCalculation() {
        List<AuditDb.AuditRecord> records = List.of(
                new AuditDb.AuditRecord(1, "2026-09-17 12:00:00", "SanDisk Ultra", "SN-1", "500 GB", "DoD 5220.22-M", "SUCCESS", "sig1"),
                new AuditDb.AuditRecord(2, "2026-09-17 12:05:00", "Kingston DataTraveler", "SN-2", "500 GB", "NIST SP 800-88", "SUCCESS", "sig2"),
                new AuditDb.AuditRecord(3, "2026-09-17 12:10:00", "Samsung BAR Plus", "SN-3", "1 TB", "DoD 5220.22-M", "SUCCESS", "sig3")
        );

        EsgCalculator.EsgMetrics agg = EsgCalculator.calculateAggregate(records);
        assertThat(agg.capacityGb()).isEqualTo(2000.0);
        assertThat(agg.eWasteDivertedKg()).isGreaterThan(4.0);
        assertThat(agg.co2EmissionsSavedKg()).isGreaterThan(40.0);
        assertThat(agg.impactStatement()).contains("Across 3 sanitized devices (2.0 TB total storage)");
    }
}
