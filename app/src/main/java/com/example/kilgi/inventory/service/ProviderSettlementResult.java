package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.data.ProviderPaymentAllocationEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;

import java.util.Collections;
import java.util.List;

public class ProviderSettlementResult {

    public final ProviderPaymentEntity payment;
    public final List<ProviderPaymentAllocationEntity> allocations;

    public ProviderSettlementResult(ProviderPaymentEntity payment, List<ProviderPaymentAllocationEntity> allocations) {
        this.payment = payment;
        this.allocations = allocations == null ? Collections.emptyList() : Collections.unmodifiableList(allocations);
    }

    public double getTotalAllocatedAmount() {
        double total = 0;
        for (ProviderPaymentAllocationEntity allocation : allocations) {
            total += allocation.amountApplied;
        }
        return total;
    }
}

