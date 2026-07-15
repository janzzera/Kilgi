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
    @Query("SELECT * FROM journal_entries WHERE lotId = :lotId ORDER BY timestamp ASC")
    List<JournalEntryWithLines> getEntriesForLot(String lotId);

    @Transaction
    @Query("SELECT * FROM journal_entries WHERE timestamp >= :fromTimestamp AND timestamp < :toTimestamp ORDER BY timestamp ASC, entryId ASC")
    List<JournalEntryWithLines> getEntriesForPeriod(long fromTimestamp, long toTimestamp);

    @Query("SELECT MIN(timestamp) FROM journal_entries")
    Long getOldestEntryTimestamp();

    @Query("SELECT MAX(timestamp) FROM journal_entries")
    Long getLatestEntryTimestamp();
}

