package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface JournalDao {

    @Insert
    void insertEntry(JournalEntryEntity entry);

    @Insert
    void insertLines(List<JournalLineEntity> lines);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE lotId = :lotId AND userId = :userId ORDER BY timestamp ASC")
    List<JournalEntryWithLines> getEntriesForLot(String lotId, String userId);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND timestamp >= :fromTimestamp AND timestamp < :toTimestamp ORDER BY timestamp ASC, entryId ASC")
    List<JournalEntryWithLines> getEntriesForPeriod(String userId, long fromTimestamp, long toTimestamp);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND timestamp < :toTimestamp ORDER BY timestamp ASC, entryId ASC")
    List<JournalEntryWithLines> getEntriesUpTo(String userId, long toTimestamp);

    @Query("SELECT MIN(timestamp) FROM journal_entries WHERE userId = :userId")
    Long getOldestEntryTimestamp(String userId);

    @Query("SELECT MAX(timestamp) FROM journal_entries WHERE userId = :userId")
    Long getLatestEntryTimestamp(String userId);
}

