package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.AccountingPeriodDao;
import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.data.KilgiDatabase;

import java.util.List;

public class AccountingPeriodRepository {

    private final KilgiDatabase database;
    private final AccountingPeriodDao periodDao;

    public AccountingPeriodRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.periodDao = database.accountingPeriodDao();
    }

    public LiveData<List<AccountingPeriodEntity>> getAllPeriods(String userId) {
        return periodDao.getAllForUser(userId);
    }

    public LiveData<AccountingPeriodEntity> getPeriodById(long periodId) {
        return periodDao.getById(periodId);
    }

    public boolean isTimestampLocked(String userId, long timestamp) {
        return periodDao.isTimestampLocked(userId, timestamp);
    }

    public void createAccountingPeriod(String name, long start, long end, String userId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                periodDao.insert(new AccountingPeriodEntity(name, start, end, false, null, userId));
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void closeAccountingPeriod(long periodId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AccountingPeriodEntity period = periodDao.getById(periodId).getValue();
                if (period != null) {
                    period.isClosed = true;
                    period.closedAt = System.currentTimeMillis();
                    periodDao.update(period);
                }
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
