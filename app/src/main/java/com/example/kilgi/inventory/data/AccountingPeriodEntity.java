package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounting_periods")
public class AccountingPeriodEntity {

    @PrimaryKey(autoGenerate = true)
    public long periodId;

    @NonNull
    public String periodName;

    public long startDate; // Start timestamp inclusive

    public long endDate; // End timestamp inclusive

    public boolean isClosed;

    public Long closedAt; // Timestamp when it was locked

    public String userId;

    public AccountingPeriodEntity(
            @NonNull String periodName,
            long startDate,
            long endDate,
            boolean isClosed,
            Long closedAt,
            String userId
    ) {
        this.periodName = periodName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isClosed = isClosed;
        this.closedAt = closedAt;
        this.userId = userId;
    }
}
