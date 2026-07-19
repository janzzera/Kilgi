package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface LotDao {

    @Insert
    void insert(LotEntity lot);

    @Query("SELECT * FROM lots WHERE userId = :userId ORDER BY timestamp DESC")
    List<LotEntity> getAllLotsForUser(String userId);

    @Query("SELECT * FROM lots WHERE lotId = :lotId AND userId = :userId LIMIT 1")
    LotEntity getLotById(String lotId, String userId);

    @Query("SELECT * FROM lots WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    LotEntity getLatestLot(String userId);

    @Transaction
    @Query("SELECT * FROM lots WHERE lotId = :lotId AND userId = :userId LIMIT 1")
    LotWithDetails getLotWithDetails(String lotId, String userId);
}

