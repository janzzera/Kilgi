package com.example.kilgi.inventory.engine;

import com.example.kilgi.inventory.model.ProcurementBatch;

/**
 * Sums all acquisition costs to produce the landed cost of a batch.
 */
public final class LandedCostCalculator {

    private LandedCostCalculator() {
    }

    public static double calculateTotalLandedCost(ProcurementBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("batch cannot be null.");
        }
        return calculateTotalLandedCost(
                batch.getBaseItemPrice(),
                batch.getShippingFees(),
                batch.getStoreDeliveryFees(),
                batch.getRawSortingLaborCost(),
                batch.getPackagingMaterialCost()
        );
    }

    public static double calculateTotalLandedCost(
            double baseItemPrice,
            double shippingFees,
            double storeDeliveryFees,
            double rawSortingLaborCost,
            double packagingMaterialCost
    ) {
        validateNonNegative(baseItemPrice, "baseItemPrice");
        validateNonNegative(shippingFees, "shippingFees");
        validateNonNegative(storeDeliveryFees, "storeDeliveryFees");
        validateNonNegative(rawSortingLaborCost, "rawSortingLaborCost");
        validateNonNegative(packagingMaterialCost, "packagingMaterialCost");

        return baseItemPrice
                + shippingFees
                + storeDeliveryFees
                + rawSortingLaborCost
                + packagingMaterialCost;
    }

    private static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
    }
}

