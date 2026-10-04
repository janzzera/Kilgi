package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.repository.AccountingPeriodRepository;
import com.example.kilgi.inventory.repository.UserRepository;

import java.util.List;

public class AccountingPeriodViewModel extends AndroidViewModel {

    private final AccountingPeriodRepository repository;
    private final LiveData<List<AccountingPeriodEntity>> allPeriods;

    public AccountingPeriodViewModel(@NonNull Application application) {
        super(application);
        repository = new AccountingPeriodRepository(application);
        allPeriods = repository.getAllPeriods(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<AccountingPeriodEntity>> getAllPeriods() {
        return allPeriods;
    }

    public LiveData<AccountingPeriodEntity> getPeriodById(long periodId) {
        return repository.getPeriodById(periodId);
    }

    public boolean isTimestampLocked(long timestamp) {
        return repository.isTimestampLocked(UserRepository.LOCAL_USER_ID, timestamp);
    }

    public void createAccountingPeriod(String name, long start, long end, AccountingPeriodRepository.Callback<Void> callback) {
        repository.createAccountingPeriod(name, start, end, UserRepository.LOCAL_USER_ID, callback);
    }

    public void closeAccountingPeriod(long periodId, AccountingPeriodRepository.Callback<Void> callback) {
        repository.closeAccountingPeriod(periodId, callback);
    }
}
