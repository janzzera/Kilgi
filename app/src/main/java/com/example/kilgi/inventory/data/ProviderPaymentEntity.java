package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "provider_payments",
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
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("userId"), @Index("providerId")}
)
public class ProviderPaymentEntity {

    @PrimaryKey
    @NonNull
    public final String paymentId;
    @NonNull
    public final String userId;
    @NonNull
    public final String providerId;
    public final double totalAmount;
    public final String notes;
    public final long timestamp;

    public ProviderPaymentEntity(
            @NonNull String paymentId,
            @NonNull String userId,
            @NonNull String providerId,
            double totalAmount,
            String notes,
            long timestamp
    ) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.providerId = providerId;
        this.totalAmount = totalAmount;
        this.notes = notes;
        this.timestamp = timestamp;
    }
}

