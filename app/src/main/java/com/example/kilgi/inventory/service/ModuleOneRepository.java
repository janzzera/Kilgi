package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotDao;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.SpoilageLogEntity;

import java.util.Collections;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates Room persistence, costing, and automated ledger posting.
 */
public class ModuleOneRepository {

    private final KilgiDatabase database;
    private final LotDao lotDao;
    private final JournalDao journalDao;

    public ModuleOneRepository(KilgiDatabase database) {
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null.");
        }
        this.database = database;
        this.lotDao = database.lotDao();
        this.journalDao = database.journalDao();
    }

    public LotEntity createLot(
            String providerId,
            String providerName,
            String vegetableType,
            int totalSacksPurchased,
            double rawKilosReceived,
            double baseUnitPrice,
            PaymentSource purchasePaymentSource,
            double standardFreight,
            PaymentSource freightPaymentSource
    ) {
        validateRequiredText(providerId, "Provider ID");
        validateRequiredText(providerName, "Provider name");
        validateRequiredText(vegetableType, "Vegetable type");
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
                providerId.trim(),
                providerName.trim(),
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
            persistJournalDraft(JournalEntryFactory.buildInitialLotEntry(lot));
        });
        return lot;
    }

    public BatchExpenseEntity addExpense(String lotId, AccountingAccount expenseAccount, double amount, PaymentSource paymentSource) {
        LotEntity lot = requireLot(lotId);
        if (expenseAccount == null) {
            throw new IllegalArgumentException("Expense account is required.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
        }
        if (paymentSource == null) {
            throw new IllegalArgumentException("Payment source is required.");
        }

        BatchExpenseEntity expense = new BatchExpenseEntity(
                UUID.randomUUID().toString(),
                lot.lotId,
                expenseAccount.getCode(),
                expenseAccount.getName(),
                amount,
                paymentSource.getStoredValue(),
                System.currentTimeMillis()
        );

        database.runInTransaction(() -> {
            database.batchExpenseDao().insert(expense);
            persistJournalDraft(JournalEntryFactory.buildExpenseEntry(lot, expense));
        });
        return expense;
    }

    public SpoilageLogEntity logSpoilage(String lotId, double kilosLost, LossType lossType) {
        LotWithDetails lotWithDetails = requireLotWithDetails(lotId);
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

        SpoilageLogEntity log = new SpoilageLogEntity(
                UUID.randomUUID().toString(),
                lotWithDetails.lot.lotId,
                kilosLost,
                lossType.name(),
                System.currentTimeMillis()
        );

        database.runInTransaction(() -> {
            database.spoilageLogDao().insert(log);
            if (lossType == LossType.ABNORMAL) {
                Double unitCost = snapshotBeforeLog.getTrueCostPerKilo();
                if (unitCost == null) {
                    throw new IllegalStateException("No usable inventory remains to write off.");
                }
                double writeOffAmount = unitCost * kilosLost;
                persistJournalDraft(JournalEntryFactory.buildAbnormalLossEntry(lotWithDetails.lot, log, writeOffAmount));
            }
        });
        return log;
    }

    public LotWithDetails getLotWithDetails(String lotId) {
        return requireLotWithDetails(lotId);
    }

    public LotEntity getLatestLot() {
        return lotDao.getLatestLot();
    }

    public List<LotEntity> getAllLots() {
        List<LotEntity> lots = lotDao.getAllLots();
        return lots == null ? Collections.emptyList() : lots;
    }

    public List<JournalEntryWithLines> getJournalEntries(String lotId) {
        List<JournalEntryWithLines> entries = journalDao.getEntriesForLot(lotId);
        return entries == null ? Collections.emptyList() : entries;
    }

    public List<JournalEntryWithLines> getJournalEntriesForPeriod(int monthOfYear, int year) {
        if (monthOfYear < 1 || monthOfYear > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("Year must be a valid four-digit value.");
        }

        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(year, monthOfYear - 1, 1, 0, 0, 0);

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);

        List<JournalEntryWithLines> entries = journalDao.getEntriesForPeriod(
                start.getTimeInMillis(),
                end.getTimeInMillis()
        );
        return entries == null ? Collections.emptyList() : entries;
    }

    public long getOldestJournalEntryTimestamp() {
        Long timestamp = journalDao.getOldestEntryTimestamp();
        return timestamp == null ? System.currentTimeMillis() : timestamp;
    }

    public long getLatestJournalEntryTimestamp() {
        Long timestamp = journalDao.getLatestEntryTimestamp();
        return timestamp == null ? System.currentTimeMillis() : timestamp;
    }

    private LotEntity requireLot(String lotId) {
        validateRequiredText(lotId, "Lot ID");
        LotEntity lot = lotDao.getLotById(lotId.trim());
        if (lot == null) {
            throw new IllegalArgumentException("No lot found for ID: " + lotId.trim());
        }
        return lot;
    }

    private LotWithDetails requireLotWithDetails(String lotId) {
        validateRequiredText(lotId, "Lot ID");
        LotWithDetails lot = lotDao.getLotWithDetails(lotId.trim());
        if (lot == null || lot.lot == null) {
            throw new IllegalArgumentException("No lot found for ID: " + lotId.trim());
        }
        return lot;
    }

    private void persistJournalDraft(LedgerEntryDraft draft) {
        journalDao.insertEntry(draft.getEntry());
        journalDao.insertLines(draft.getLines());
    }

    private static void validateRequiredText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}

