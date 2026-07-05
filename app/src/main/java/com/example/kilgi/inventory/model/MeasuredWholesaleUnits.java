package com.example.kilgi.inventory.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the actual measured kilogram weight of each purchased wholesale unit in a batch.
 */
public class MeasuredWholesaleUnits {

    private final String unitName;
    private final List<Double> unitWeightsKg;

    public MeasuredWholesaleUnits(String unitName, List<Double> unitWeightsKg) {
        this.unitName = requireText(unitName, "unitName");
        if (unitWeightsKg == null || unitWeightsKg.isEmpty()) {
            throw new IllegalArgumentException("unitWeightsKg cannot be null or empty.");
        }

        List<Double> validatedWeights = new ArrayList<>();
        for (Double weightKg : unitWeightsKg) {
            if (weightKg == null || weightKg <= 0) {
                throw new IllegalArgumentException("Each unit weight must be greater than zero.");
            }
            validatedWeights.add(weightKg);
        }
        this.unitWeightsKg = Collections.unmodifiableList(validatedWeights);
    }

    public String getUnitName() {
        return unitName;
    }

    public List<Double> getUnitWeightsKg() {
        return unitWeightsKg;
    }

    public int getUnitCount() {
        return unitWeightsKg.size();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}

