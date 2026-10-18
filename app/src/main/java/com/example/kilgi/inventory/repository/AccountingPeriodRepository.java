package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.AccountingPeriodDao;
import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.api.ApiService;
import com.example.kilgi.inventory.data.common.Resource;
import com.example.kilgi.inventory.network.ApiClient;
import com.example.kilgi.inventory.network.NetworkBoundResource;

import java.util.List;

import retrofit2.Call;

import com.example.kilgi.inventory.sync.SyncManager;

public class AccountingPeriodRepository {

    private final KilgiDatabase database;
    private final AccountingPeriodDao periodDao;
    private final ApiService apiService;
    private final Context appContext;

    public AccountingPeriodRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.database = KilgiDatabase.getInstance(context);
        this.periodDao = database.accountingPeriodDao();
        this.apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<List<AccountingPeriodEntity>>> getAllPeriods(String userId) {
        return new NetworkBoundResource<List<AccountingPeriodEntity>, List<AccountingPeriodEntity>>() {
            @Override
            protected LiveData<List<AccountingPeriodEntity>> loadFromDb() {
                return periodDao.getAllForUser(userId);
            }

            @Override
            protected boolean shouldFetch(List<AccountingPeriodEntity> data) {
                return true;
            }

            @Override
            protected Call<List<AccountingPeriodEntity>> createCall() {
                return apiService.getAccountingPeriods();
            }

            @Override
            protected void saveCallResult(List<AccountingPeriodEntity> item) {
                if (item != null) {
                    periodDao.insertAll(item);
                }
            }
        }.asLiveData();
    }

    public LiveData<Resource<AccountingPeriodEntity>> getPeriodById(long periodId) {
        return new NetworkBoundResource<AccountingPeriodEntity, AccountingPeriodEntity>() {
            @Override
            protected LiveData<AccountingPeriodEntity> loadFromDb() {
                return periodDao.getById(periodId);
            }

            @Override
            protected boolean shouldFetch(AccountingPeriodEntity data) {
                return true;
            }

            @Override
            protected Call<AccountingPeriodEntity> createCall() {
                return apiService.getAccountingPeriodById(periodId);
            }

            @Override
            protected void saveCallResult(AccountingPeriodEntity item) {
                if (item != null) {
                    periodDao.insert(item);
                }
            }
        }.asLiveData();
    }

    public boolean isTimestampLocked(String userId, long timestamp) {
        return periodDao.isTimestampLocked(userId, timestamp);
    }

    public void createAccountingPeriod(String name, long start, long end, String userId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                periodDao.insert(new AccountingPeriodEntity(name, start, end, false, null, userId));
                SyncManager.scheduleSync(appContext);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void closeAccountingPeriod(long periodId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                AccountingPeriodEntity period = periodDao.getByIdSync(periodId);
                if (period != null) {
                    period.isClosed = true;
                    period.closedAt = System.currentTimeMillis();
                    periodDao.update(period);
                    SyncManager.scheduleSync(appContext);
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
