package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProviderDao {

    @Insert
    void insert(ProviderEntity provider);

    @Query("SELECT * FROM providers WHERE providerId = :providerId LIMIT 1")
    ProviderEntity getById(String providerId);

    @Query("SELECT * FROM providers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    List<ProviderEntity> getActiveProvidersForUser(String userId);
}

