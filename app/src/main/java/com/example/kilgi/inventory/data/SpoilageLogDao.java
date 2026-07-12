package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SpoilageLogDao {

    @Insert
    void insert(SpoilageLogEntity log);

    @Query("SELECT * FROM spoilage_logs WHERE lotId = :lotId ORDER BY timestamp ASC")
    List<SpoilageLogEntity> getByLotId(String lotId);
}

