package com.example.kilgi.inventory.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Applies a payment amount across chronologically ordered open balances.
 */
public final class PaymentAllocationEngine {

    private static final double EPSILON = 0.0000001d;

    private PaymentAllocationEngine() {
    }

    public static List<AllocationStep> allocate(double paymentAmount, List<? extends OpenBalanceItem> openItems) {
        if (paymentAmount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (openItems == null || openItems.isEmpty()) {
            throw new IllegalArgumentException("There are no open balances to allocate.");
        }

        List<AllocationStep> allocations = new ArrayList<>();
        double remaining = paymentAmount;
        for (int index = 0; index < openItems.size() && remaining > EPSILON; index++) {
            OpenBalanceItem item = openItems.get(index);
            if (item == null || item.getOutstandingBalance() <= EPSILON) {
                continue;
            }
            double applied = Math.min(remaining, item.getOutstandingBalance());
            if (applied <= EPSILON) {
                continue;
            }
            allocations.add(new AllocationStep(item.getReferenceId(), applied, index));
            remaining -= applied;
        }

        if (remaining > EPSILON) {
            throw new IllegalArgumentException("Payment exceeds the total outstanding balance.");
        }
        return Collections.unmodifiableList(allocations);
    }

    public interface OpenBalanceItem {
        String getReferenceId();

        double getOutstandingBalance();
    }

    public static final class AllocationStep {
        private final String referenceId;
        private final double amountApplied;
        private final int priorityIndex;

        public AllocationStep(String referenceId, double amountApplied, int priorityIndex) {
            this.referenceId = referenceId;
            this.amountApplied = amountApplied;
            this.priorityIndex = priorityIndex;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public double getAmountApplied() {
            return amountApplied;
        }

        public int getPriorityIndex() {
            return priorityIndex;
        }
    }
}

