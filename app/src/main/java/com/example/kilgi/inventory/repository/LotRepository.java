package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotDao;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.ProviderDao;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.service.BatchValuationEngine;
import com.example.kilgi.inventory.service.BatchValuationSnapshot;

import java.util.List;
import java.util.UUID;

public class LotRepository {

    private final KilgiDatabase database;
    private final LotDao lotDao;
    private final ProviderDao providerDao;
    private final JournalDao journalDao;

    public LotRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.lotDao = database.lotDao();
        this.providerDao = database.providerDao();
        this.journalDao = database.journalDao();
    }

    public LiveData<List<LotEntity>> getAllLots(String userId) {
        return lotDao.getAllLotsForUser(userId);
    }

    public LiveData<LotEntity> getLotById(String lotId, String userId) {
        return lotDao.getLotById(lotId, userId);
    }

    public LiveData<LotEntity> getLatestLot(String userId) {
        return lotDao.getLatestLot(userId);
    }

    public LiveData<LotWithDetails> getLotWithDetails(String lotId, String userId) {
        return lotDao.getLotWithDetails(lotId, userId);
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
            String userId,
            Callback<LotEntity> callback
    ) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                ProviderEntity provider = providerDao.getByIdSync(providerId);
                if (provider == null || provider.isActive != 1) {
                    throw new IllegalArgumentException("No provider found for the selected record.");
                }
                if (vegetableType == null || vegetableType.trim().isEmpty()) {
                    throw new IllegalArgumentException("Vegetable type is required.");
                }
                if (totalSacksPurchased <= 0) {
                    throw new IllegalArgumentException("Total sacks purchased must be greater than zero.");
                }
                if (rawKilosReceived <= 0) {
                    throw new IllegalArgumentException("Raw kilograms received must be greater than zero.");
                }
                if (baseUnitPrice < 0 || standardFreight < 0) {
                    throw new IllegalArgumentException("Baseline amounts cannot be negative.");
                }
                if (purchasePaymentSource == null || freightPaymentSource == null) {
                    throw new IllegalArgumentException("Payment source is required.");
                }

                long now = System.currentTimeMillis();
                LotEntity lot = new LotEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        provider.providerId,
                        provider.displayName,
                        vegetableType.trim(),
                        totalSacksPurchased,
                        rawKilosReceived,
                        baseUnitPrice,
                        purchasePaymentSource.getStoredValue(),
                        standardFreight,
                        freightPaymentSource.getStoredValue(),
                        now
                );

                database.runInTransaction(() -> {
                    lotDao.insert(lot);
                    LedgerEntryDraft draft = JournalEntryFactory.buildInitialLotEntry(lot);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                if (callback != null) callback.onSuccess(lot);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void deleteLot(String lotId, String userId, Callback<Void> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                LotEntity lot = lotDao.getLotByIdSync(lotId, userId);
                if (lot == null) {
                    throw new IllegalArgumentException("No lot found for ID: " + lotId);
                }
                database.runInTransaction(() -> {
                    journalDao.deleteByLotId(lotId);
                    lotDao.deleteById(lotId);
                });
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void addExpense(String lotId, String userId, AccountingAccount expenseAccount, double amount, PaymentSource paymentSource, Callback<BatchExpenseEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                LotEntity lot = lotDao.getLotByIdSync(lotId, userId);
                if (lot == null) {
                    throw new IllegalArgumentException("No lot found for ID: " + lotId);
                }
                if (expenseAccount == null) {
                    throw new IllegalArgumentException("Expense account is required.");
                }
                if (amount <= 0) {
                    throw new IllegalArgumentException("Expense amount must be greater than zero.");
                }
                if (paymentSource == null) {
                    throw new IllegalArgumentException("Payment source is required.");
                }

                long now = System.currentTimeMillis();
                BatchExpenseEntity expense = new BatchExpenseEntity(
                        UUID.randomUUID().toString(),
                        lot.lotId,
                        expenseAccount.getCode(),
                        expenseAccount.getName(),
                        amount,
                        paymentSource.getStoredValue(),
                        now
                );

                database.runInTransaction(() -> {
                    database.batchExpenseDao().insert(expense);
                    LedgerEntryDraft draft = JournalEntryFactory.buildExpenseEntry(lot, expense);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                if (callback != null) callback.onSuccess(expense);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void logSpoilage(String lotId, String userId, double kilosLost, LossType lossType, Callback<SpoilageLogEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                LotWithDetails lotWithDetails = lotDao.getLotWithDetailsSync(lotId, userId);
                if (lotWithDetails == null || lotWithDetails.lot == null) {
                    throw new IllegalArgumentException("No lot found for ID: " + lotId);
                }
                if (kilosLost <= 0) {
                    throw new IllegalArgumentException("Spoilage kilos must be greater than zero.");
                }
                if (lossType == null) {
                    throw new IllegalArgumentException("Loss type is required.");
                }

                BatchValuationSnapshot snapshotBeforeLog = BatchValuationEngine.calculate(
                        lotWithDetails.lot,
                        lotWithDetails.expenses,
                        lotWithDetails.spoilageLogs
                );
                if (kilosLost > snapshotBeforeLog.getNetUsableKilograms() + 0.0000001d) {
                    throw new IllegalArgumentException("Spoilage cannot exceed current usable kilograms.");
                }

                long now = System.currentTimeMillis();
                SpoilageLogEntity log = new SpoilageLogEntity(
                        UUID.randomUUID().toString(),
                        lotWithDetails.lot.lotId,
                        kilosLost,
                        lossType.name(),
                        now
                );

                database.runInTransaction(() -> {
                    database.spoilageLogDao().insert(log);
                    if (lossType == LossType.ABNORMAL) {
                        Double unitCost = snapshotBeforeLog.getTrueCostPerKilo();
                        if (unitCost == null) {
                            throw new IllegalStateException("No usable inventory remains to write off.");
                        }
                        double writeOffAmount = unitCost * kilosLost;
                        LedgerEntryDraft draft = JournalEntryFactory.buildAbnormalLossEntry(lotWithDetails.lot, log, writeOffAmount);
                        journalDao.insertEntry(draft.getEntry());
                        journalDao.insertLines(draft.getLines());
                    }
                });

                if (callback != null) callback.onSuccess(log);
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
