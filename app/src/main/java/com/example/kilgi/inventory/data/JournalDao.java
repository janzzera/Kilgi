package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface JournalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEntry(JournalEntryEntity entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<JournalLineEntity> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLines(List<JournalLineEntity> lines);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE lotId = :lotId AND userId = :userId ORDER BY timestamp ASC")
    LiveData<List<JournalEntryWithLines>> getEntriesForLot(String lotId, String userId);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND timestamp >= :fromTimestamp AND timestamp < :toTimestamp ORDER BY timestamp ASC, entryId ASC")
    LiveData<List<JournalEntryWithLines>> getEntriesForPeriod(String userId, long fromTimestamp, long toTimestamp);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND timestamp < :toTimestamp ORDER BY timestamp ASC, entryId ASC")
    LiveData<List<JournalEntryWithLines>> getEntriesUpTo(String userId, long toTimestamp);

    @Query("SELECT MIN(timestamp) FROM journal_entries WHERE userId = :userId")
    LiveData<Long> getOldestEntryTimestamp(String userId);

    @Query("SELECT MAX(timestamp) FROM journal_entries WHERE userId = :userId")
    LiveData<Long> getLatestEntryTimestamp(String userId);

    @Query("DELETE FROM journal_entries WHERE lotId = :lotId")
    void deleteByLotId(String lotId);
}

