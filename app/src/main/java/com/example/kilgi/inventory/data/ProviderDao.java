package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProviderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProviderEntity provider);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProviderEntity> providers);

    @Query("SELECT * FROM providers WHERE providerId = :providerId LIMIT 1")
    LiveData<ProviderEntity> getById(String providerId);

    @Query("SELECT * FROM providers WHERE providerId = :providerId LIMIT 1")
    ProviderEntity getByIdSync(String providerId);

    @Query("SELECT * FROM providers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    LiveData<List<ProviderEntity>> getActiveProvidersForUser(String userId);

    @Query("SELECT * FROM providers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    List<ProviderEntity> getActiveProvidersForUserSync(String userId);
}

