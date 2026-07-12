package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "spoilage_logs",
        foreignKeys = @ForeignKey(
                entity = LotEntity.class,
                parentColumns = "lotId",
                childColumns = "lotId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("lotId")
)
public class SpoilageLogEntity {

    @PrimaryKey
    @NonNull
    public final String logId;
    @NonNull
    public final String lotId;
    public final double kilosLost;
    @NonNull
    public final String lossType;
    public final long timestamp;

    public SpoilageLogEntity(
            @NonNull String logId,
            @NonNull String lotId,
            double kilosLost,
            @NonNull String lossType,
            long timestamp
    ) {
        this.logId = logId;
        this.lotId = lotId;
        this.kilosLost = kilosLost;
        this.lossType = lossType;
        this.timestamp = timestamp;
    }
}

