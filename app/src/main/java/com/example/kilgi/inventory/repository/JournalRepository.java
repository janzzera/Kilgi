package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.api.ApiService;
import com.example.kilgi.inventory.data.common.Resource;
import com.example.kilgi.inventory.network.ApiClient;
import com.example.kilgi.inventory.network.NetworkBoundResource;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;

public class JournalRepository {

    private final KilgiDatabase database;
    private final JournalDao journalDao;
    private final ApiService apiService;

    public JournalRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.journalDao = database.journalDao();
        this.apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<List<JournalEntryWithLines>>> getEntriesForLot(String lotId, String userId) {
        return new NetworkBoundResource<List<JournalEntryWithLines>, List<JournalEntryWithLines>>() {
            @Override
            protected LiveData<List<JournalEntryWithLines>> loadFromDb() {
                return journalDao.getEntriesForLot(lotId, userId);
            }

            @Override
            protected boolean shouldFetch(List<JournalEntryWithLines> data) {
                return true;
            }

            @Override
            protected Call<List<JournalEntryWithLines>> createCall() {
                return apiService.getJournalEntriesByLotId(lotId);
            }

            @Override
            protected void saveCallResult(List<JournalEntryWithLines> item) {
                if (item != null) {
                    database.runInTransaction(() -> {
                        for (JournalEntryWithLines entry : item) {
                            if (entry.entry != null) {
                                journalDao.insertEntry(entry.entry);
                            }
                            if (entry.lines != null) {
                                journalDao.insertLines(entry.lines);
                            }
                        }
                    });
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<JournalEntryWithLines>>> getEntriesForPeriod(String userId, int monthOfYear, int year) {
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

        long fromTimestamp = start.getTimeInMillis();
        long toTimestamp = end.getTimeInMillis();

        return new NetworkBoundResource<List<JournalEntryWithLines>, List<JournalEntryWithLines>>() {
            @Override
            protected LiveData<List<JournalEntryWithLines>> loadFromDb() {
                return journalDao.getEntriesForPeriod(userId, fromTimestamp, toTimestamp);
            }

            @Override
            protected boolean shouldFetch(List<JournalEntryWithLines> data) {
                return true;
            }

            @Override
            protected Call<List<JournalEntryWithLines>> createCall() {
                return apiService.getJournalEntries();
            }

            @Override
            protected void saveCallResult(List<JournalEntryWithLines> item) {
                if (item != null) {
                    database.runInTransaction(() -> {
                        for (JournalEntryWithLines entry : item) {
                            if (entry.entry != null) {
                                journalDao.insertEntry(entry.entry);
                            }
                            if (entry.lines != null) {
                                journalDao.insertLines(entry.lines);
                            }
                        }
                    });
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<List<JournalEntryWithLines>>> getEntriesUpTo(String userId, int monthOfYear, int year) {
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

        long toTimestamp = end.getTimeInMillis();

        return new NetworkBoundResource<List<JournalEntryWithLines>, List<JournalEntryWithLines>>() {
            @Override
            protected LiveData<List<JournalEntryWithLines>> loadFromDb() {
                return journalDao.getEntriesUpTo(userId, toTimestamp);
            }

            @Override
            protected boolean shouldFetch(List<JournalEntryWithLines> data) {
                return true;
            }

            @Override
            protected Call<List<JournalEntryWithLines>> createCall() {
                return apiService.getJournalEntries();
            }

            @Override
            protected void saveCallResult(List<JournalEntryWithLines> item) {
                if (item != null) {
                    database.runInTransaction(() -> {
                        for (JournalEntryWithLines entry : item) {
                            if (entry.entry != null) {
                                journalDao.insertEntry(entry.entry);
                            }
                            if (entry.lines != null) {
                                journalDao.insertLines(entry.lines);
                            }
                        }
                    });
                }
            }
        }.asLiveData();
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
