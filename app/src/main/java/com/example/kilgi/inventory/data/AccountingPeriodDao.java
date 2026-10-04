package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountingPeriodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AccountingPeriodEntity period);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AccountingPeriodEntity> periods);

    @Update
    void update(AccountingPeriodEntity period);

    @Query("SELECT * FROM accounting_periods WHERE userId = :userId ORDER BY startDate DESC")
    LiveData<List<AccountingPeriodEntity>> getAllForUser(String userId);

    @Query("SELECT * FROM accounting_periods WHERE periodId = :periodId LIMIT 1")
    LiveData<AccountingPeriodEntity> getById(long periodId);

    @Query("SELECT EXISTS(SELECT 1 FROM accounting_periods WHERE userId = :userId AND isClosed = 1 AND :timestamp >= startDate AND :timestamp <= endDate)")
    boolean isTimestampLocked(String userId, long timestamp);
    
    @Query("SELECT * FROM accounting_periods WHERE userId = :userId AND :timestamp >= startDate AND :timestamp <= endDate LIMIT 1")
    LiveData<AccountingPeriodEntity> getPeriodForTimestamp(String userId, long timestamp);
}
