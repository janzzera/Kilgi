package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.UserDao;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.api.ApiService;
import com.example.kilgi.inventory.data.common.Resource;
import com.example.kilgi.inventory.network.ApiClient;
import com.example.kilgi.inventory.network.NetworkBoundResource;

import retrofit2.Call;
import retrofit2.Response;

import com.example.kilgi.inventory.data.common.AppExecutors;
import com.example.kilgi.inventory.sync.SyncManager;

public class UserRepository {

    public static final String LOCAL_USER_ID = "local-owner";

    private final UserDao userDao;
    private final ApiService apiService;
    private final Context appContext;

    public UserRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.userDao = KilgiDatabase.getInstance(context).userDao();
        this.apiService = ApiClient.getApiService(context);
        ensureLocalUserExists();
    }

    public LiveData<Resource<UserEntity>> getUser(String userId) {
        return new NetworkBoundResource<UserEntity, UserEntity>() {
            @Override
            protected LiveData<UserEntity> loadFromDb() {
                return userDao.getById(userId);
            }

            @Override
            protected boolean shouldFetch(UserEntity data) {
                return data == null && !LOCAL_USER_ID.equals(userId);
            }

            @Override
            protected Call<UserEntity> createCall() {
                return apiService.getUserById(userId);
            }

            @Override
            protected void saveCallResult(UserEntity item) {
                if (item != null) {
                    userDao.insert(item);
                }
            }
        }.asLiveData();
    }

    public void updateUser(UserEntity user) {
        updateUser(user, null);
    }

    public void updateUser(UserEntity user, Runnable onComplete) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            userDao.update(user);
            SyncManager.scheduleSync(appContext);
            if (onComplete != null) {
                AppExecutors.runOnMainThread(onComplete);
            }
        });
    }

    public void registerOrSetupUser(UserEntity user, Callback<UserEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Response<UserEntity> response = apiService.registerUser(user).execute();
                UserEntity serverUser = response.isSuccessful() ? response.body() : null;

                if (serverUser == null) {
                    Response<UserEntity> updateResponse = apiService.updateUser(user.userId, user).execute();
                    if (updateResponse.isSuccessful()) {
                        serverUser = updateResponse.body();
                    }
                }

                if (serverUser != null) {
                    UserEntity localUpdated = new UserEntity(
                            LOCAL_USER_ID,
                            serverUser.username != null ? serverUser.username : user.username,
                            serverUser.displayName != null ? serverUser.displayName : user.displayName,
                            serverUser.businessName != null ? serverUser.businessName : user.businessName,
                            serverUser.emailAddress,
                            serverUser.mobileNumber,
                            serverUser.passwordHash,
                            serverUser.passwordSalt,
                            "ACTIVE",
                            user.createdAt,
                            System.currentTimeMillis()
                    );
                    localUpdated.isSynced = true;
                    userDao.insert(localUpdated);

                    if (callback != null) {
                        AppExecutors.runOnMainThread(() -> callback.onSuccess(localUpdated));
                    }
                } else {
                    String errorMsg = "Server setup failed (HTTP " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errBody = response.errorBody().string();
                            if (!errBody.trim().isEmpty()) {
                                errorMsg += ": " + errBody;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    String finalErrorMsg = errorMsg;
                    if (callback != null) {
                        AppExecutors.runOnMainThread(() -> callback.onError(new Exception(finalErrorMsg)));
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    AppExecutors.runOnMainThread(() -> callback.onError(e));
                }
            }
        });
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }

    public void insertUser(UserEntity user) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            userDao.insert(user);
            SyncManager.scheduleSync(appContext);
        });
    }

    public void ensureLocalUserExists() {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity existing = userDao.getByIdSync(LOCAL_USER_ID);
            if (existing == null) {
                long now = System.currentTimeMillis();
                userDao.insert(new UserEntity(
                        LOCAL_USER_ID,
                        "local_owner",
                        "Local Owner",
                        "Kilgi Demo Business",
                        null,
                        null,
                        "PENDING_LOGIN_SETUP",
                        "PENDING_LOGIN_SETUP",
                        "ACTIVE",
                        now,
                        now
                ));
            }
        });
    }
}
