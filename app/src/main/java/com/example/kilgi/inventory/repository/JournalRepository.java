package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.KilgiDatabase;

import java.util.Calendar;
import java.util.List;

public class JournalRepository {

    private final KilgiDatabase database;
    private final JournalDao journalDao;

    public JournalRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.journalDao = database.journalDao();
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesForLot(String lotId, String userId) {
        return journalDao.getEntriesForLot(lotId, userId);
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesForPeriod(String userId, int monthOfYear, int year) {
        if (monthOfYear < 1 || monthOfYear > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("Year must be a valid four-digit value.");
        }

        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(year, monthOfYear - 1, 1, 0, 0, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        return journalDao.getEntriesForPeriod(userId, start.getTimeInMillis(), end.getTimeInMillis());
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesUpTo(String userId, int monthOfYear, int year) {
        if (monthOfYear < 1 || monthOfYear > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("Year must be a valid four-digit value.");
        }

        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(year, monthOfYear - 1, 1, 0, 0, 0);
        end.add(Calendar.MONTH, 1);

        return journalDao.getEntriesUpTo(userId, end.getTimeInMillis());
    }

    public LiveData<Long> getOldestEntryTimestamp(String userId) {
        return journalDao.getOldestEntryTimestamp(userId);
    }

    public LiveData<Long> getLatestEntryTimestamp(String userId) {
        return journalDao.getLatestEntryTimestamp(userId);
    }

    public List<JournalEntryWithLines> getEntriesForPeriodSync(String userId, int monthOfYear, int year) {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(year, monthOfYear - 1, 1, 0, 0, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        return journalDao.getEntriesForPeriodSync(userId, start.getTimeInMillis(), end.getTimeInMillis());
    }

    public List<JournalEntryWithLines> getEntriesUpToSync(String userId, int monthOfYear, int year) {
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(year, monthOfYear - 1, 1, 0, 0, 0);
        end.add(Calendar.MONTH, 1);

        return journalDao.getEntriesUpToSync(userId, end.getTimeInMillis());
    }

    public Long getOldestEntryTimestampSync(String userId) {
        return journalDao.getOldestEntryTimestampSync(userId);
    }

    public Long getLatestEntryTimestampSync(String userId) {
        return journalDao.getLatestEntryTimestampSync(userId);
    }

    public void postManualAdjustment(long timestamp, AccountingAccount debit, AccountingAccount credit, double amount, String memo, String userId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (amount <= 0) {
                    throw new IllegalArgumentException("Adjustment amount must be greater than zero.");
                }
                if (memo == null || memo.trim().isEmpty()) {
                    throw new IllegalArgumentException("Memo/Description is required.");
                }
                if (debit == null || credit == null) {
                    throw new IllegalArgumentException("Debit and Credit accounts are required.");
                }

                database.runInTransaction(() -> {
                    LedgerEntryDraft draft = JournalEntryFactory.buildAdjustingEntry(userId, timestamp, debit, credit, amount, memo);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }
}
