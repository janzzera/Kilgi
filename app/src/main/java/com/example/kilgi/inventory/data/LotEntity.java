package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lots")
public class LotEntity {

    @PrimaryKey
    @NonNull
    public final String lotId;
    @NonNull
    public final String providerId;
    @NonNull
    public final String providerName;
    @NonNull
    public final String vegetableType;
    public final int totalSacksPurchased;
    public final double rawKilosReceived;
    public final double baseUnitPrice;
    @NonNull
    public final String purchasePaymentSource;
    public final double standardFreight;
    @NonNull
    public final String freightPaymentSource;
    public final long timestamp;

    public LotEntity(
            @NonNull String lotId,
            @NonNull String providerId,
            @NonNull String providerName,
            @NonNull String vegetableType,
            int totalSacksPurchased,
            double rawKilosReceived,
            double baseUnitPrice,
            @NonNull String purchasePaymentSource,
            double standardFreight,
            @NonNull String freightPaymentSource,
            long timestamp
    ) {
        this.lotId = lotId;
        this.providerId = providerId;
        this.providerName = providerName;
        this.vegetableType = vegetableType;
        this.totalSacksPurchased = totalSacksPurchased;
        this.rawKilosReceived = rawKilosReceived;
        this.baseUnitPrice = baseUnitPrice;
        this.purchasePaymentSource = purchasePaymentSource;
        this.standardFreight = standardFreight;
        this.freightPaymentSource = freightPaymentSource;
        this.timestamp = timestamp;
    }
}

