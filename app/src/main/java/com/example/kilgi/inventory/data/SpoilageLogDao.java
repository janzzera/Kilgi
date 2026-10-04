package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SpoilageLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SpoilageLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SpoilageLogEntity> logs);

    @Query("SELECT * FROM spoilage_logs WHERE lotId = :lotId ORDER BY timestamp ASC")
    LiveData<List<SpoilageLogEntity>> getByLotId(String lotId);
}

