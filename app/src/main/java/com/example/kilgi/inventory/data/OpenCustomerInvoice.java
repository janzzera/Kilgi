package com.example.kilgi.inventory.data;

import com.example.kilgi.inventory.service.PaymentAllocationEngine;

public class OpenCustomerInvoice implements PaymentAllocationEngine.OpenBalanceItem {
    public String invoiceId;
    public String customerId;
    public String invoiceNumber;
    public String description;
    public double totalAmount;
    public long timestamp;
    public double outstandingBalance;

    @Override
    public String getReferenceId() {
        return invoiceId;
    }

    @Override
    public double getOutstandingBalance() {
        return outstandingBalance;
    }
}

