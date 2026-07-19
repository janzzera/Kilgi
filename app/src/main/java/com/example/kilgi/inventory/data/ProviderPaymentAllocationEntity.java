package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "provider_payment_allocations",
        foreignKeys = {
                @ForeignKey(
                        entity = ProviderPaymentEntity.class,
                        parentColumns = "paymentId",
                        childColumns = "paymentId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = LotEntity.class,
                        parentColumns = "lotId",
                        childColumns = "lotId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("paymentId"), @Index("lotId")}
)
public class ProviderPaymentAllocationEntity {

    @PrimaryKey
    @NonNull
    public final String allocationId;
    @NonNull
    public final String paymentId;
    @NonNull
    public final String lotId;
    public final double amountApplied;
    public final int allocationOrder;
    public final long timestamp;

    public ProviderPaymentAllocationEntity(
            @NonNull String allocationId,
            @NonNull String paymentId,
            @NonNull String lotId,
            double amountApplied,
            int allocationOrder,
            long timestamp
    ) {
        this.allocationId = allocationId;
        this.paymentId = paymentId;
        this.lotId = lotId;
        this.amountApplied = amountApplied;
        this.allocationOrder = allocationOrder;
        this.timestamp = timestamp;
    }
}

