package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "batch_expenses",
        foreignKeys = @ForeignKey(
                entity = LotEntity.class,
                parentColumns = "lotId",
                childColumns = "lotId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("lotId")
)
public class BatchExpenseEntity {

    @PrimaryKey
    @NonNull
    public final String expenseId;
    @NonNull
    public final String lotId;
    @NonNull
    public final String expenseLabel;
    public final double amount;
    @NonNull
    public final String paymentSource;
    public final long timestamp;

    public BatchExpenseEntity(
            @NonNull String expenseId,
            @NonNull String lotId,
            @NonNull String expenseLabel,
            double amount,
            @NonNull String paymentSource,
            long timestamp
    ) {
        this.expenseId = expenseId;
        this.lotId = lotId;
        this.expenseLabel = expenseLabel;
        this.amount = amount;
        this.paymentSource = paymentSource;
        this.timestamp = timestamp;
    }
}

