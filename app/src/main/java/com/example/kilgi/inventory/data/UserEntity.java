package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    public final String userId;
    @NonNull
    public final String username;
    @NonNull
    public final String displayName;
    @NonNull
    public final String businessName;
    public final String emailAddress;
    public final String mobileNumber;
    @NonNull
    public final String passwordHash;
    @NonNull
    public final String passwordSalt;
    @NonNull
    public final String accountStatus;
    public final long createdAt;
    public final long updatedAt;

    public UserEntity(
            @NonNull String userId,
            @NonNull String username,
            @NonNull String displayName,
            @NonNull String businessName,
            String emailAddress,
            String mobileNumber,
            @NonNull String passwordHash,
            @NonNull String passwordSalt,
            @NonNull String accountStatus,
            long createdAt,
            long updatedAt
    ) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.businessName = businessName;
        this.emailAddress = emailAddress;
        this.mobileNumber = mobileNumber;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

