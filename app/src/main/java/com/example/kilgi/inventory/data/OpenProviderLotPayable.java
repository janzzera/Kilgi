package com.example.kilgi.inventory.data;

import com.example.kilgi.inventory.service.PaymentAllocationEngine;

public class OpenProviderLotPayable implements PaymentAllocationEngine.OpenBalanceItem {
    public String lotId;
    public String providerId;
    public String providerName;
    public String vegetableType;
    public long timestamp;
    public double originalPayableAmount;
    public double outstandingBalance;

    @Override
    public String getReferenceId() {
        return lotId;
    }

    @Override
    public double getOutstandingBalance() {
        return outstandingBalance;
    }
}

