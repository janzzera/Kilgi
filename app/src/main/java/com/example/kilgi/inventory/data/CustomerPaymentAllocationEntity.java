package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "customer_payment_allocations",
        foreignKeys = {
                @ForeignKey(
                        entity = CustomerPaymentEntity.class,
                        parentColumns = "paymentId",
                        childColumns = "paymentId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = WholesaleInvoiceEntity.class,
                        parentColumns = "invoiceId",
                        childColumns = "invoiceId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("paymentId"), @Index("invoiceId")}
)
public class CustomerPaymentAllocationEntity {

    @PrimaryKey
    @NonNull
    public final String allocationId;
    @NonNull
    public final String paymentId;
    @NonNull
    public final String invoiceId;
    public final double amountApplied;
    public final int allocationOrder;
    public final long timestamp;

    public CustomerPaymentAllocationEntity(
            @NonNull String allocationId,
            @NonNull String paymentId,
            @NonNull String invoiceId,
            double amountApplied,
            int allocationOrder,
            long timestamp
    ) {
        this.allocationId = allocationId;
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amountApplied = amountApplied;
        this.allocationOrder = allocationOrder;
        this.timestamp = timestamp;
    }
}

