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

    @Query("SELECT * FROM lots ORDER BY timestamp DESC")
    List<LotEntity> getAllLots();

    @Query("SELECT * FROM lots WHERE lotId = :lotId LIMIT 1")
    LotEntity getLotById(String lotId);

    @Query("SELECT * FROM lots ORDER BY timestamp DESC LIMIT 1")
    LotEntity getLatestLot();

    @Transaction
    @Query("SELECT * FROM lots WHERE lotId = :lotId LIMIT 1")
    LotWithDetails getLotWithDetails(String lotId);
}

