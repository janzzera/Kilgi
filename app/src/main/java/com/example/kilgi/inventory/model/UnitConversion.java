package com.example.kilgi.inventory.model;

import java.util.List;

/**
 * Converts measured wholesale units such as sacks or crates into kilograms.
 *
 * <p>This model does not assume every sack has the same weight. Instead,
 * the batch provides the actual weight of each purchased unit.</p>
 */
public class UnitConversion {

    private final String unitName;

    public UnitConversion(String unitName) {
        this.unitName = requireText(unitName, "unitName");
    }

    public String getUnitName() {
        return unitName;
    }

    public double toKilograms(List<Double> unitWeightsKg) {
        validateUnitWeights(unitWeightsKg);

        double totalWeightKg = 0;
        for (Double weightKg : unitWeightsKg) {
            totalWeightKg += weightKg;
        }
        return totalWeightKg;
    }

    public double getAverageKilogramsPerUnit(List<Double> unitWeightsKg) {
        validateUnitWeights(unitWeightsKg);
        return toKilograms(unitWeightsKg) / unitWeightsKg.size();
    }

    public double toEstimatedUnits(double kilograms, List<Double> referenceUnitWeightsKg) {
        validateNonNegative(kilograms, "kilograms");
        return kilograms / getAverageKilogramsPerUnit(referenceUnitWeightsKg);
    }

    private static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
    }

    private static void validateUnitWeights(List<Double> unitWeightsKg) {
        if (unitWeightsKg == null || unitWeightsKg.isEmpty()) {
            throw new IllegalArgumentException("unitWeightsKg cannot be null or empty.");
        }

        for (Double weightKg : unitWeightsKg) {
            if (weightKg == null || weightKg <= 0) {
                throw new IllegalArgumentException("Each unit weight must be greater than zero.");
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}

