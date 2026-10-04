package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface LotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LotEntity lot);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LotEntity> lots);

    @Query("SELECT * FROM lots WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<LotEntity>> getAllLotsForUser(String userId);

    @Query("SELECT * FROM lots WHERE lotId = :lotId AND userId = :userId LIMIT 1")
    LiveData<LotEntity> getLotById(String lotId, String userId);

    @Query("SELECT * FROM lots WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    LiveData<LotEntity> getLatestLot(String userId);

    @Transaction
    @Query("SELECT * FROM lots WHERE lotId = :lotId AND userId = :userId LIMIT 1")
    LiveData<LotWithDetails> getLotWithDetails(String lotId, String userId);

    @Query("DELETE FROM lots WHERE lotId = :lotId")
    void deleteById(String lotId);
}

