package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.data.CustomerPaymentAllocationEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;

import java.util.Collections;
import java.util.List;

public class CustomerCollectionResult {

    public final CustomerPaymentEntity payment;
    public final List<CustomerPaymentAllocationEntity> allocations;

    public CustomerCollectionResult(CustomerPaymentEntity payment, List<CustomerPaymentAllocationEntity> allocations) {
        this.payment = payment;
        this.allocations = allocations == null ? Collections.emptyList() : Collections.unmodifiableList(allocations);
    }

    public double getTotalAllocatedAmount() {
        double total = 0;
        for (CustomerPaymentAllocationEntity allocation : allocations) {
            total += allocation.amountApplied;
        }
        return total;
    }
}

