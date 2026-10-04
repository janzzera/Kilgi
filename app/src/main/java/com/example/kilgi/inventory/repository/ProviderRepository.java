package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LotDao;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.ProviderDao;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.data.ProviderPaymentAllocationEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;
import com.example.kilgi.inventory.data.SalesDao;
import com.example.kilgi.inventory.service.PaymentAllocationEngine;
import com.example.kilgi.inventory.service.ProviderSettlementResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProviderRepository {

    private final KilgiDatabase database;
    private final ProviderDao providerDao;
    private final SalesDao salesDao;
    private final LotDao lotDao;
    private final JournalDao journalDao;

    public ProviderRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.providerDao = database.providerDao();
        this.salesDao = database.salesDao();
        this.lotDao = database.lotDao();
        this.journalDao = database.journalDao();
    }

    public LiveData<List<ProviderEntity>> getActiveProviders(String userId) {
        return providerDao.getActiveProvidersForUser(userId);
    }

    public LiveData<ProviderEntity> getProviderById(String providerId) {
        return providerDao.getById(providerId);
    }

    public LiveData<List<ProviderLedgerSummary>> getProviderLedgerSummaries(String userId) {
        return salesDao.getProviderLedgerSummaries(userId);
    }

    public LiveData<List<OpenProviderLotPayable>> getOpenLotPayablesForProvider(String providerId) {
        return salesDao.getOpenLotPayablesForProvider(providerId);
    }

    public void createProvider(String displayName, String contactNumber, String address, String notes, String userId, Callback<ProviderEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (displayName == null || displayName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Provider name is required.");
                }
                long now = System.currentTimeMillis();
                ProviderEntity provider = new ProviderEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        displayName.trim(),
                        normalizeOptionalText(contactNumber),
                        normalizeOptionalText(address),
                        normalizeOptionalText(notes),
                        1,
                        now,
                        now
                );
                providerDao.insert(provider);
                if (callback != null) callback.onSuccess(provider);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void settleProviderBalance(String providerId, double totalAmount, String notes, String userId, List<OpenProviderLotPayable> openPayables, Callback<ProviderSettlementResult> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (providerId == null || providerId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Provider is required.");
                }
                if (totalAmount <= 0) {
                    throw new IllegalArgumentException("Provider payment amount must be greater than zero.");
                }
                long now = System.currentTimeMillis();
                ProviderEntity provider = providerDao.getByIdSync(providerId.trim());
                if (provider == null || provider.isActive != 1) {
                    throw new IllegalArgumentException("No provider found for the selected record.");
                }

                List<PaymentAllocationEngine.AllocationStep> plan = PaymentAllocationEngine.allocate(totalAmount, openPayables);
                ProviderPaymentEntity payment = new ProviderPaymentEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        provider.providerId,
                        totalAmount,
                        normalizeOptionalText(notes),
                        now
                );

                List<ProviderPaymentAllocationEntity> allocations = new ArrayList<>();
                List<LotEntity> allocatedLots = new ArrayList<>();
                for (int index = 0; index < plan.size(); index++) {
                    PaymentAllocationEngine.AllocationStep step = plan.get(index);
                    allocations.add(new ProviderPaymentAllocationEntity(
                            UUID.randomUUID().toString(),
                            payment.paymentId,
                            step.getReferenceId(),
                            step.getAmountApplied(),
                            index,
                            now
                    ));
                    LotEntity lot = lotDao.getLotByIdSync(step.getReferenceId(), userId);
                    if (lot != null) {
                        allocatedLots.add(lot);
                    }
                }

                database.runInTransaction(() -> {
                    salesDao.insertProviderPayment(payment);
                    salesDao.insertProviderPaymentAllocations(allocations);
                    LedgerEntryDraft draft = JournalEntryFactory.buildProviderSettlementEntry(payment, provider, allocations, allocatedLots);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                ProviderSettlementResult result = new ProviderSettlementResult(payment, allocations);
                if (callback != null) callback.onSuccess(result);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable throwable);
    }
}
