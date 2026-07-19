package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "wholesale_invoices",
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
        indices = {@Index("userId"), @Index("customerId"), @Index(value = {"customerId", "timestamp"})}
)
public class WholesaleInvoiceEntity {

    @PrimaryKey
    @NonNull
    public final String invoiceId;
    @NonNull
    public final String userId;
    @NonNull
    public final String customerId;
    @NonNull
    public final String invoiceNumber;
    @NonNull
    public final String description;
    public final double totalAmount;
    public final String notes;
    public final long timestamp;

    public WholesaleInvoiceEntity(
            @NonNull String invoiceId,
            @NonNull String userId,
            @NonNull String customerId,
            @NonNull String invoiceNumber,
            @NonNull String description,
            double totalAmount,
            String notes,
            long timestamp
    ) {
        this.invoiceId = invoiceId;
        this.userId = userId;
        this.customerId = customerId;
        this.invoiceNumber = invoiceNumber;
        this.description = description;
        this.totalAmount = totalAmount;
        this.notes = notes;
        this.timestamp = timestamp;
    }
}

