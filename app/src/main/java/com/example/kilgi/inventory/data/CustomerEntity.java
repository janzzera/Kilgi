package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "customers",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index(value = {"userId", "displayName"})}
)
public class CustomerEntity {

    @PrimaryKey
    @NonNull
    public final String customerId;
    @NonNull
    public final String userId;
    @NonNull
    public final String displayName;
    public final String contactNumber;
    public final String address;
    public final String notes;
    public final int isActive;
    public final long createdAt;
    public final long updatedAt;

    public CustomerEntity(
            @NonNull String customerId,
            @NonNull String userId,
            @NonNull String displayName,
            String contactNumber,
            String address,
            String notes,
            int isActive,
            long createdAt,
            long updatedAt
    ) {
        this.customerId = customerId;
        this.userId = userId;
        this.displayName = displayName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.notes = notes;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    @NonNull
    public String toString() {
        return displayName;
    }
}

