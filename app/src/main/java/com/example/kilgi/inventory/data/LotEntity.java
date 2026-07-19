package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "lots",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "userId",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ProviderEntity.class,
                        parentColumns = "providerId",
                        childColumns = "providerId",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {@Index("userId"), @Index("providerId")}
)
public class LotEntity {

    @PrimaryKey
    @NonNull
    public final String lotId;
    @NonNull
    public final String userId;
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
            @NonNull String userId,
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
        this.userId = userId;
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

