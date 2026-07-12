package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.SpoilageLogEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Replays lot-related events to derive current inventory value and per-kilo cost.
 */
public final class BatchValuationEngine {

    private BatchValuationEngine() {
    }

    public static BatchValuationSnapshot calculate(
            LotEntity lot,
            List<BatchExpenseEntity> expenses,
            List<SpoilageLogEntity> spoilageLogs
    ) {
        if (lot == null) {
            throw new IllegalArgumentException("lot cannot be null.");
        }

        List<TimelineEvent> timeline = new ArrayList<>();
        double expenseTotal = 0;
        for (BatchExpenseEntity expense : safeExpenses(expenses)) {
            expenseTotal += expense.amount;
            timeline.add(TimelineEvent.forExpense(expense));
        }
        for (SpoilageLogEntity log : safeLogs(spoilageLogs)) {
            timeline.add(TimelineEvent.forSpoilage(log));
        }
        timeline.sort(Comparator.comparingLong((TimelineEvent event) -> event.timestamp)
                .thenComparingInt(event -> event.sortOrder));

        double capitalizedCost = getInitialCapitalizedCost(lot);
        double usableKilos = lot.rawKilosReceived;
        double normalLossKilos = 0;
        double abnormalLossKilos = 0;
        double abnormalWriteOffValue = 0;

        for (TimelineEvent event : timeline) {
            if (event.expense != null) {
                capitalizedCost += event.expense.amount;
                continue;
            }

            SpoilageLogEntity log = event.spoilage;
            if (log.kilosLost > usableKilos + 0.0000001d) {
                throw new IllegalStateException("Spoilage exceeds remaining usable kilograms for lot " + lot.lotId + ".");
            }

            LossType lossType = LossType.fromStoredValue(log.lossType);
            if (lossType == LossType.NORMAL) {
                normalLossKilos += log.kilosLost;
                usableKilos -= log.kilosLost;
            } else {
                if (usableKilos <= 0) {
                    throw new IllegalStateException("Cannot write off inventory when no usable kilograms remain.");
                }
                double currentUnitCost = capitalizedCost / usableKilos;
                double writeOffValue = currentUnitCost * log.kilosLost;
                abnormalLossKilos += log.kilosLost;
                abnormalWriteOffValue += writeOffValue;
                capitalizedCost -= writeOffValue;
                usableKilos -= log.kilosLost;
            }
        }

        usableKilos = Math.max(0, usableKilos);
        capitalizedCost = Math.max(0, capitalizedCost);
        Double trueCostPerKilo = usableKilos > 0 ? capitalizedCost / usableKilos : null;

        return new BatchValuationSnapshot(
                lot.rawKilosReceived * lot.baseUnitPrice,
                expenseTotal + lot.standardFreight,
                normalLossKilos,
                abnormalLossKilos,
                abnormalWriteOffValue,
                capitalizedCost,
                usableKilos,
                trueCostPerKilo
        );
    }

    public static double getInitialCapitalizedCost(LotEntity lot) {
        if (lot == null) {
            throw new IllegalArgumentException("lot cannot be null.");
        }
        return (lot.rawKilosReceived * lot.baseUnitPrice) + lot.standardFreight;
    }

    private static List<BatchExpenseEntity> safeExpenses(List<BatchExpenseEntity> expenses) {
        return expenses == null ? new ArrayList<>() : expenses;
    }

    private static List<SpoilageLogEntity> safeLogs(List<SpoilageLogEntity> logs) {
        return logs == null ? new ArrayList<>() : logs;
    }

    private static final class TimelineEvent {
        private final long timestamp;
        private final int sortOrder;
        private final BatchExpenseEntity expense;
        private final SpoilageLogEntity spoilage;

        private TimelineEvent(long timestamp, int sortOrder, BatchExpenseEntity expense, SpoilageLogEntity spoilage) {
            this.timestamp = timestamp;
            this.sortOrder = sortOrder;
            this.expense = expense;
            this.spoilage = spoilage;
        }

        private static TimelineEvent forExpense(BatchExpenseEntity expense) {
            return new TimelineEvent(expense.timestamp, 0, expense, null);
        }

        private static TimelineEvent forSpoilage(SpoilageLogEntity spoilage) {
            int order = LossType.fromStoredValue(spoilage.lossType) == LossType.NORMAL ? 1 : 2;
            return new TimelineEvent(spoilage.timestamp, order, null, spoilage);
        }
    }
}

