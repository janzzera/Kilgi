package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.api.AuthResponse;
import com.example.kilgi.inventory.data.common.Resource;
import com.example.kilgi.inventory.repository.UserRepository;

public class UserViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final LiveData<Resource<UserEntity>> localUser;

    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
        localUser = repository.getLocalUser();
    }

    public LiveData<Resource<UserEntity>> getLocalUser() {
        return localUser;
    }

    public LiveData<Resource<UserEntity>> getUser(String userId) {
        return repository.getUser(userId);
    }

    public void updateUser(UserEntity user) {
        repository.updateUser(user);
    }

    public void updateUser(UserEntity user, Runnable onComplete) {
        repository.updateUser(user, onComplete);
    }

    public void registerOrSetupUser(UserEntity user, UserRepository.Callback<UserEntity> callback) {
        repository.registerOrSetupUser(user, callback);
    }

    public void loginOnline(String username, String password, UserRepository.Callback<AuthResponse> callback) {
        repository.loginOnline(username, password, callback);
    }
}
