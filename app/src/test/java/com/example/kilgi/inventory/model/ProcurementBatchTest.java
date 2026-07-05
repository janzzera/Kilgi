package com.example.kilgi.inventory.model;

import com.example.kilgi.inventory.engine.LandedCostCalculator;
import com.example.kilgi.inventory.engine.TrueCostPerKiloEngine;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ProcurementBatchTest {

    @Test
    public void landedCost_sumsEveryProcurementCost() {
        double landedCost = LandedCostCalculator.calculateTotalLandedCost(5000, 300, 200, 150, 50);

        assertEquals(5700, landedCost, 0.0001);
    }

    @Test
    public void batch_calculatesInitialAndTrueCostPerKilo() {
        ProcurementBatch batch = createTomatoBatch();

        assertEquals(100, batch.getInitialWeightKg(), 0.0001);
        assertEquals(25, batch.getAverageUnitWeightKg(), 0.0001);
        assertEquals(57, batch.getInitialCostPerKilo(), 0.0001);

        batch.logSpoilage(10, "Rotten tomatoes");

        assertEquals(10, batch.getTotalSpoilageWeightKg(), 0.0001);
        assertEquals(90, batch.getRemainingSellableWeightKg(), 0.0001);
        assertEquals(63.3333, batch.getTrueCostPerKilo(), 0.0001);
    }

    @Test
    public void batch_supportsMultipleSpoilageLogs() {
        ProcurementBatch batch = createTomatoBatch();

        batch.logSpoilage(5, "Bruised during unloading");
        batch.logSpoilage(new SpoilageEntry(7.5, "Overripe produce", 1720195200000L));

        assertEquals(12.5, batch.getTotalSpoilageWeightKg(), 0.0001);
        assertEquals(87.5, batch.getRemainingSellableWeightKg(), 0.0001);
        assertEquals(65.142857, batch.getTrueCostPerKilo(), 0.0001);
        assertEquals(2, batch.getSpoilageEntries().size());
    }

    @Test
    public void batch_rejectsSpoilageThatExceedsInventory() {
        ProcurementBatch batch = createTomatoBatch();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> batch.logSpoilage(101, "Everything spoiled")
        );

        assertEquals("Total spoilage cannot exceed the batch weight.", exception.getMessage());
    }

    @Test
    public void trueCostEngine_requiresSellableWeight() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> TrueCostPerKiloEngine.calculate(5700, 0)
        );

        assertEquals("remainingSellableWeightKg must be greater than zero.", exception.getMessage());
    }

    @Test
    public void unitConversion_usesActualWeightsInsteadOfAssumingEqualUnits() {
        UnitConversion unitConversion = new UnitConversion("crate");
        MeasuredWholesaleUnits measuredUnits = new MeasuredWholesaleUnits(
                "crate",
                Arrays.asList(20.0, 22.5, 25.0)
        );

        assertEquals(67.5, unitConversion.toKilograms(measuredUnits.getUnitWeightsKg()), 0.0001);
        assertEquals(22.5, unitConversion.getAverageKilogramsPerUnit(measuredUnits.getUnitWeightsKg()), 0.0001);
        assertEquals(1.333333, unitConversion.toEstimatedUnits(30, measuredUnits.getUnitWeightsKg()), 0.0001);
    }

    @Test
    public void batch_tracksDifferentWeightPerSack() {
        ProcurementBatch batch = createTomatoBatch();

        assertEquals(4, batch.getPurchasedUnitCount());
        assertEquals(Arrays.asList(24.5, 25.0, 26.0, 24.5), batch.getPurchasedUnitWeightsKg());
    }

    @Test
    public void batch_rejectsMismatchedUnitNames() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProcurementBatch(
                        "Tomato",
                        "Davao Growers Cooperative",
                        new MeasuredWholesaleUnits("crate", Arrays.asList(24.5, 25.0)),
                        new UnitConversion("sack"),
                        5000,
                        300,
                        200,
                        150,
                        50
                )
        );

        assertEquals("unitConversion unitName must match measuredWholesaleUnits unitName.", exception.getMessage());
    }

    private ProcurementBatch createTomatoBatch() {
        return new ProcurementBatch(
                "Tomato",
                "Davao Growers Cooperative",
                new MeasuredWholesaleUnits("sack", Arrays.asList(24.5, 25.0, 26.0, 24.5)),
                new UnitConversion("sack"),
                5000,
                300,
                200,
                150,
                50
        );
    }
}



