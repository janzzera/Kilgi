package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.repository.UserRepository;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final LiveData<UserEntity> localUser;

    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
        localUser = repository.getUser(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<UserEntity> getLocalUser() {
        return localUser;
    }

    public LiveData<UserEntity> getUser(String userId) {
        return repository.getUser(userId);
    }

    public void updateUser(UserEntity user) {
        repository.updateUser(user);
    }
}
