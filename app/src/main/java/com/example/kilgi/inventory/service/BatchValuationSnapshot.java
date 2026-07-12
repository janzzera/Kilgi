package com.example.kilgi.inventory.service;

/**
 * Derived costing state of a lot after replaying expenses and spoilage events.
 */
public class BatchValuationSnapshot {

    private final double purchaseCost;
    private final double expenseTotal;
    private final double normalLossKilos;
    private final double abnormalLossKilos;
    private final double abnormalWriteOffValue;
    private final double totalCapitalizedCost;
    private final double netUsableKilograms;
    private final Double trueCostPerKilo;

    public BatchValuationSnapshot(
            double purchaseCost,
            double expenseTotal,
            double normalLossKilos,
            double abnormalLossKilos,
            double abnormalWriteOffValue,
            double totalCapitalizedCost,
            double netUsableKilograms,
            Double trueCostPerKilo
    ) {
        this.purchaseCost = purchaseCost;
        this.expenseTotal = expenseTotal;
        this.normalLossKilos = normalLossKilos;
        this.abnormalLossKilos = abnormalLossKilos;
        this.abnormalWriteOffValue = abnormalWriteOffValue;
        this.totalCapitalizedCost = totalCapitalizedCost;
        this.netUsableKilograms = netUsableKilograms;
        this.trueCostPerKilo = trueCostPerKilo;
    }

    public double getPurchaseCost() {
        return purchaseCost;
    }

    public double getExpenseTotal() {
        return expenseTotal;
    }

    public double getNormalLossKilos() {
        return normalLossKilos;
    }

    public double getAbnormalLossKilos() {
        return abnormalLossKilos;
    }

    public double getAbnormalWriteOffValue() {
        return abnormalWriteOffValue;
    }

    public double getTotalCapitalizedCost() {
        return totalCapitalizedCost;
    }

    public double getNetUsableKilograms() {
        return netUsableKilograms;
    }

    public Double getTrueCostPerKilo() {
        return trueCostPerKilo;
    }
}

