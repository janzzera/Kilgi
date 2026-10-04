package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BatchExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BatchExpenseEntity expense);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BatchExpenseEntity> expenses);

    @Query("SELECT * FROM batch_expenses WHERE lotId = :lotId ORDER BY timestamp ASC")
    LiveData<List<BatchExpenseEntity>> getByLotId(String lotId);
}

