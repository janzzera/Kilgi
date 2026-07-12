package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "journal_entries",
        foreignKeys = @ForeignKey(
                entity = LotEntity.class,
                parentColumns = "lotId",
                childColumns = "lotId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("lotId")
)
public class JournalEntryEntity {

    @PrimaryKey
    @NonNull
    public final String entryId;
    @NonNull
    public final String lotId;
    @NonNull
    public final String eventType;
    @NonNull
    public final String description;
    public final long timestamp;

    public JournalEntryEntity(
            @NonNull String entryId,
            @NonNull String lotId,
            @NonNull String eventType,
            @NonNull String description,
            long timestamp
    ) {
        this.entryId = entryId;
        this.lotId = lotId;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
    }
}

