package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.repository.LotRepository;
import com.example.kilgi.inventory.repository.UserRepository;

import java.util.List;

public class LotViewModel extends AndroidViewModel {

    private final LotRepository repository;
    private final LiveData<List<LotEntity>> allLots;
    private final LiveData<LotEntity> latestLot;

    public LotViewModel(@NonNull Application application) {
        super(application);
        repository = new LotRepository(application);
        allLots = repository.getAllLots(UserRepository.LOCAL_USER_ID);
        latestLot = repository.getLatestLot(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<LotEntity>> getAllLots() {
        return allLots;
    }

    public LiveData<LotEntity> getLatestLot() {
        return latestLot;
    }

    public LiveData<LotEntity> getLotById(String lotId) {
        return repository.getLotById(lotId, UserRepository.LOCAL_USER_ID);
    }

    public LiveData<LotWithDetails> getLotWithDetails(String lotId) {
        return repository.getLotWithDetails(lotId, UserRepository.LOCAL_USER_ID);
    }

    public void createLot(
            String providerId,
            String vegetableType,
            int totalSacksPurchased,
            double rawKilosReceived,
            double baseUnitPrice,
            PaymentSource purchasePaymentSource,
            double standardFreight,
            PaymentSource freightPaymentSource,
            LotRepository.Callback<LotEntity> callback
    ) {
        repository.createLot(
                providerId,
                vegetableType,
                totalSacksPurchased,
                rawKilosReceived,
                baseUnitPrice,
                purchasePaymentSource,
                standardFreight,
                freightPaymentSource,
                UserRepository.LOCAL_USER_ID,
                callback
        );
    }

    public void deleteLot(String lotId, LotRepository.Callback<Void> callback) {
        repository.deleteLot(lotId, UserRepository.LOCAL_USER_ID, callback);
    }

    public void addExpense(String lotId, AccountingAccount expenseAccount, double amount, PaymentSource paymentSource, LotRepository.Callback<BatchExpenseEntity> callback) {
        repository.addExpense(lotId, UserRepository.LOCAL_USER_ID, expenseAccount, amount, paymentSource, callback);
    }

    public void logSpoilage(String lotId, double kilosLost, LossType lossType, LotRepository.Callback<SpoilageLogEntity> callback) {
        repository.logSpoilage(lotId, UserRepository.LOCAL_USER_ID, kilosLost, lossType, callback);
    }
}
