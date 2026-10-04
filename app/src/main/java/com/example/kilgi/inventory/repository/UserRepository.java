package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.UserDao;
import com.example.kilgi.inventory.data.UserEntity;

public class UserRepository {

    public static final String LOCAL_USER_ID = "local-owner";

    private final UserDao userDao;
    private final KilgiDatabase database;

    public UserRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.userDao = database.userDao();
        ensureLocalUserExists();
    }

    public LiveData<UserEntity> getUser(String userId) {
        return userDao.getById(userId);
    }

    public void updateUser(UserEntity user) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> userDao.update(user));
    }

    public void insertUser(UserEntity user) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> userDao.insert(user));
    }

    public void ensureLocalUserExists() {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            // Check if local owner exists, insert default if missing
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
        });
    }
}
