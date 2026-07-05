package com.example.kilgi.inventory.engine;

/**
 * Recomputes the unit cost after spoilage reduces the sellable quantity.
 */
public final class TrueCostPerKiloEngine {

    private TrueCostPerKiloEngine() {
    }

    public static double calculate(double totalLandedCost, double remainingSellableWeightKg) {
        if (totalLandedCost < 0) {
            throw new IllegalArgumentException("totalLandedCost cannot be negative.");
        }
        if (remainingSellableWeightKg <= 0) {
            throw new IllegalStateException("remainingSellableWeightKg must be greater than zero.");
        }
        return totalLandedCost / remainingSellableWeightKg;
    }
}

