package com.example.kilgi.inventory.model;

import com.example.kilgi.inventory.engine.LandedCostCalculator;
import com.example.kilgi.inventory.engine.TrueCostPerKiloEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core domain model for a vegetable procurement batch.
 */
public class ProcurementBatch {

    private final String productName;
    private final String providerName;
    private final MeasuredWholesaleUnits measuredWholesaleUnits;
    private final UnitConversion unitConversion;
    private final double baseItemPrice;
    private final double shippingFees;
    private final double storeDeliveryFees;
    private final double rawSortingLaborCost;
    private final double packagingMaterialCost;
    private final List<SpoilageEntry> spoilageEntries = new ArrayList<>();

    public ProcurementBatch(
            String productName,
            String providerName,
            MeasuredWholesaleUnits measuredWholesaleUnits,
            UnitConversion unitConversion,
            double baseItemPrice,
            double shippingFees,
            double storeDeliveryFees,
            double rawSortingLaborCost,
            double packagingMaterialCost
    ) {
        this.productName = requireText(productName, "productName");
        this.providerName = requireText(providerName, "providerName");
        if (measuredWholesaleUnits == null) {
            throw new IllegalArgumentException("measuredWholesaleUnits cannot be null.");
        }
        if (unitConversion == null) {
            throw new IllegalArgumentException("unitConversion cannot be null.");
        }
        if (!measuredWholesaleUnits.getUnitName().equalsIgnoreCase(unitConversion.getUnitName())) {
            throw new IllegalArgumentException("unitConversion unitName must match measuredWholesaleUnits unitName.");
        }
        validateNonNegative(baseItemPrice, "baseItemPrice");
        validateNonNegative(shippingFees, "shippingFees");
        validateNonNegative(storeDeliveryFees, "storeDeliveryFees");
        validateNonNegative(rawSortingLaborCost, "rawSortingLaborCost");
        validateNonNegative(packagingMaterialCost, "packagingMaterialCost");

        this.measuredWholesaleUnits = measuredWholesaleUnits;
        this.unitConversion = unitConversion;
        this.baseItemPrice = baseItemPrice;
        this.shippingFees = shippingFees;
        this.storeDeliveryFees = storeDeliveryFees;
        this.rawSortingLaborCost = rawSortingLaborCost;
        this.packagingMaterialCost = packagingMaterialCost;
    }

    public String getProductName() {
        return productName;
    }

    public String getProviderName() {
        return providerName;
    }

    public int getPurchasedUnitCount() {
        return measuredWholesaleUnits.getUnitCount();
    }

    public MeasuredWholesaleUnits getMeasuredWholesaleUnits() {
        return measuredWholesaleUnits;
    }

    public UnitConversion getUnitConversion() {
        return unitConversion;
    }

    public double getBaseItemPrice() {
        return baseItemPrice;
    }

    public double getShippingFees() {
        return shippingFees;
    }

    public double getStoreDeliveryFees() {
        return storeDeliveryFees;
    }

    public double getRawSortingLaborCost() {
        return rawSortingLaborCost;
    }

    public double getPackagingMaterialCost() {
        return packagingMaterialCost;
    }

    public List<SpoilageEntry> getSpoilageEntries() {
        return Collections.unmodifiableList(spoilageEntries);
    }

    public List<Double> getPurchasedUnitWeightsKg() {
        return measuredWholesaleUnits.getUnitWeightsKg();
    }

    public double getInitialWeightKg() {
        return unitConversion.toKilograms(measuredWholesaleUnits.getUnitWeightsKg());
    }

    public double getAverageUnitWeightKg() {
        return unitConversion.getAverageKilogramsPerUnit(measuredWholesaleUnits.getUnitWeightsKg());
    }

    public double getTotalLandedCost() {
        return LandedCostCalculator.calculateTotalLandedCost(this);
    }

    public double getTotalSpoilageWeightKg() {
        double totalSpoilageKg = 0;
        for (SpoilageEntry entry : spoilageEntries) {
            totalSpoilageKg += entry.getLostWeightKg();
        }
        return totalSpoilageKg;
    }

    public double getRemainingSellableWeightKg() {
        return Math.max(0, getInitialWeightKg() - getTotalSpoilageWeightKg());
    }

    public boolean hasSellableInventory() {
        return getRemainingSellableWeightKg() > 0;
    }

    public double getTrueCostPerKilo() {
        return TrueCostPerKiloEngine.calculate(getTotalLandedCost(), getRemainingSellableWeightKg());
    }

    public void logSpoilage(double lostWeightKg, String reason) {
        logSpoilage(new SpoilageEntry(lostWeightKg, reason));
    }

    public void logSpoilage(SpoilageEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry cannot be null.");
        }
        double projectedSpoilage = getTotalSpoilageWeightKg() + entry.getLostWeightKg();
        if (projectedSpoilage > getInitialWeightKg()) {
            throw new IllegalArgumentException("Total spoilage cannot exceed the batch weight.");
        }
        spoilageEntries.add(entry);
    }

    public double getInitialCostPerKilo() {
        return TrueCostPerKiloEngine.calculate(getTotalLandedCost(), getInitialWeightKg());
    }

    private static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }
}

