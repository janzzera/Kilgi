package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.repository.ProviderRepository;
import com.example.kilgi.inventory.repository.UserRepository;
import com.example.kilgi.inventory.service.ProviderSettlementResult;

import java.util.List;

public class ProviderViewModel extends AndroidViewModel {

    private final ProviderRepository repository;
    private final LiveData<List<ProviderEntity>> activeProviders;
    private final LiveData<List<ProviderLedgerSummary>> ledgerSummaries;

    public ProviderViewModel(@NonNull Application application) {
        super(application);
        repository = new ProviderRepository(application);
        activeProviders = repository.getActiveProviders(UserRepository.LOCAL_USER_ID);
        ledgerSummaries = repository.getProviderLedgerSummaries(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<ProviderEntity>> getActiveProviders() {
        return activeProviders;
    }

    public LiveData<ProviderEntity> getProviderById(String providerId) {
        return repository.getProviderById(providerId);
    }

    public LiveData<List<ProviderLedgerSummary>> getLedgerSummaries() {
        return ledgerSummaries;
    }

    public LiveData<List<OpenProviderLotPayable>> getOpenLotPayablesForProvider(String providerId) {
        return repository.getOpenLotPayablesForProvider(providerId);
    }

    public void createProvider(String displayName, String contactNumber, String address, String notes, ProviderRepository.Callback<ProviderEntity> callback) {
        repository.createProvider(displayName, contactNumber, address, notes, UserRepository.LOCAL_USER_ID, callback);
    }

    public void settleProviderBalance(String providerId, double totalAmount, String notes, List<OpenProviderLotPayable> openPayables, ProviderRepository.Callback<ProviderSettlementResult> callback) {
        repository.settleProviderBalance(providerId, totalAmount, notes, UserRepository.LOCAL_USER_ID, openPayables, callback);
    }
}
