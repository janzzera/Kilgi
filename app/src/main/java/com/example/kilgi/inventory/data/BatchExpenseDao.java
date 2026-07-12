package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BatchExpenseDao {

    @Insert
    void insert(BatchExpenseEntity expense);

    @Query("SELECT * FROM batch_expenses WHERE lotId = :lotId ORDER BY timestamp ASC")
    List<BatchExpenseEntity> getByLotId(String lotId);
}

