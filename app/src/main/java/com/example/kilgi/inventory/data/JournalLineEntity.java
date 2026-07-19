package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "journal_lines",
        foreignKeys = @ForeignKey(
                entity = JournalEntryEntity.class,
                parentColumns = "entryId",
                childColumns = "entryId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("entryId"), @Index("lotId")}
)
public class JournalLineEntity {

    @PrimaryKey
    @NonNull
    public final String lineId;
    @NonNull
    public final String entryId;
    @Nullable
    public final String lotId;
    @NonNull
    public final String accountCode;
    @NonNull
    public final String accountName;
    @NonNull
    public final String lineType;
    public final double amount;
    public final String providerId;
    public final String customerId;
    public final String paymentSource;
    public final String memo;

    public JournalLineEntity(
            @NonNull String lineId,
            @NonNull String entryId,
            @Nullable String lotId,
            @NonNull String accountCode,
            @NonNull String accountName,
            @NonNull String lineType,
            double amount,
            String providerId,
            String customerId,
            String paymentSource,
            String memo
    ) {
        this.lineId = lineId;
        this.entryId = entryId;
        this.lotId = lotId;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.lineType = lineType;
        this.amount = amount;
        this.providerId = providerId;
        this.customerId = customerId;
        this.paymentSource = paymentSource;
        this.memo = memo;
    }
}

