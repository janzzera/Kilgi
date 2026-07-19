package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "customer_payments",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "userId",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = CustomerEntity.class,
                        parentColumns = "customerId",
                        childColumns = "customerId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("userId"), @Index("customerId")}
)
public class CustomerPaymentEntity {

    @PrimaryKey
    @NonNull
    public final String paymentId;
    @NonNull
    public final String userId;
    @NonNull
    public final String customerId;
    public final double totalAmount;
    public final String notes;
    public final long timestamp;

    public CustomerPaymentEntity(
            @NonNull String paymentId,
            @NonNull String userId,
            @NonNull String customerId,
            double totalAmount,
            String notes,
            long timestamp
    ) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.notes = notes;
        this.timestamp = timestamp;
    }
}

