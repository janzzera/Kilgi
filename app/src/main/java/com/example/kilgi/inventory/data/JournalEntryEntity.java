package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "journal_entries",
        foreignKeys = {
                @ForeignKey(
                        entity = UserEntity.class,
                        parentColumns = "userId",
                        childColumns = "userId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = LotEntity.class,
                        parentColumns = "lotId",
                        childColumns = "lotId",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {@Index("userId"), @Index("lotId"), @Index(value = {"referenceType", "referenceId"})}
)
public class JournalEntryEntity {

    @PrimaryKey
    @NonNull
    public final String entryId;
    @NonNull
    public final String userId;
    @Nullable
    public final String lotId;
    public final String referenceType;
    public final String referenceId;
    @NonNull
    public final String eventType;
    @NonNull
    public final String description;
    public final long timestamp;

    public JournalEntryEntity(
            @NonNull String entryId,
            @NonNull String userId,
            @Nullable String lotId,
            String referenceType,
            String referenceId,
            @NonNull String eventType,
            @NonNull String description,
            long timestamp
    ) {
        this.entryId = entryId;
        this.userId = userId;
        this.lotId = lotId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
    }
}

