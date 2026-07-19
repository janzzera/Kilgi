package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "retail_sales",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("userId")
)
public class RetailSaleEntity {

    @PrimaryKey
    @NonNull
    public final String saleId;
    @NonNull
    public final String userId;
    public final double totalAmount;
    public final String notes;
    public final long timestamp;

    public RetailSaleEntity(
            @NonNull String saleId,
            @NonNull String userId,
            double totalAmount,
            String notes,
            long timestamp
    ) {
        this.saleId = saleId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.notes = notes;
        this.timestamp = timestamp;
    }
}

