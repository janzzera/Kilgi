package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountingPeriodDao {

    @Insert
    void insert(AccountingPeriodEntity period);

    @Update
    void update(AccountingPeriodEntity period);

    @Query("SELECT * FROM accounting_periods WHERE userId = :userId ORDER BY startDate DESC")
    List<AccountingPeriodEntity> getAllForUser(String userId);

    @Query("SELECT * FROM accounting_periods WHERE periodId = :periodId LIMIT 1")
    AccountingPeriodEntity getById(long periodId);

    @Query("SELECT EXISTS(SELECT 1 FROM accounting_periods WHERE userId = :userId AND isClosed = 1 AND :timestamp >= startDate AND :timestamp <= endDate)")
    boolean isTimestampLocked(String userId, long timestamp);
    
    @Query("SELECT * FROM accounting_periods WHERE userId = :userId AND :timestamp >= startDate AND :timestamp <= endDate LIMIT 1")
    AccountingPeriodEntity getPeriodForTimestamp(String userId, long timestamp);
}
