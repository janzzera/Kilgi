package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.repository.JournalRepository;
import com.example.kilgi.inventory.repository.UserRepository;

import java.util.List;

public class JournalViewModel extends AndroidViewModel {

    private final JournalRepository repository;
    private final LiveData<Long> oldestTimestamp;
    private final LiveData<Long> latestTimestamp;

    public JournalViewModel(@NonNull Application application) {
        super(application);
        repository = new JournalRepository(application);
        oldestTimestamp = repository.getOldestEntryTimestamp(UserRepository.LOCAL_USER_ID);
        latestTimestamp = repository.getLatestEntryTimestamp(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesForLot(String lotId) {
        return repository.getEntriesForLot(lotId, UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesForPeriod(int monthOfYear, int year) {
        return repository.getEntriesForPeriod(UserRepository.LOCAL_USER_ID, monthOfYear, year);
    }

    public LiveData<List<JournalEntryWithLines>> getEntriesUpTo(int monthOfYear, int year) {
        return repository.getEntriesUpTo(UserRepository.LOCAL_USER_ID, monthOfYear, year);
    }

    public LiveData<Long> getOldestTimestamp() {
        return oldestTimestamp;
    }

    public LiveData<Long> getLatestTimestamp() {
        return latestTimestamp;
    }

    public List<JournalEntryWithLines> getEntriesForPeriodSync(int monthOfYear, int year) {
        return repository.getEntriesForPeriodSync(UserRepository.LOCAL_USER_ID, monthOfYear, year);
    }

    public List<JournalEntryWithLines> getEntriesUpToSync(int monthOfYear, int year) {
        return repository.getEntriesUpToSync(UserRepository.LOCAL_USER_ID, monthOfYear, year);
    }

    public Long getOldestTimestampSync() {
        return repository.getOldestEntryTimestampSync(UserRepository.LOCAL_USER_ID);
    }

    public Long getLatestTimestampSync() {
        return repository.getLatestEntryTimestampSync(UserRepository.LOCAL_USER_ID);
    }

    public void postManualAdjustment(long timestamp, AccountingAccount debit, AccountingAccount credit, double amount, String memo, JournalRepository.Callback<Void> callback) {
        repository.postManualAdjustment(timestamp, debit, credit, amount, memo, UserRepository.LOCAL_USER_ID, callback);
    }
}
