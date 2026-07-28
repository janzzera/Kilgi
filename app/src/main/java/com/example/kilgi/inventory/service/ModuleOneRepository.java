package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.CustomerDao;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.CustomerPaymentAllocationEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotDao;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.ProviderDao;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.data.ProviderPaymentAllocationEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.SalesDao;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.data.UserDao;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates Room persistence, inventory costing, party masters, and automated journal posting.
 */
public class ModuleOneRepository {

    public static final String LOCAL_USER_ID = "local-owner";

    private final KilgiDatabase database;
    private final UserDao userDao;
    private final ProviderDao providerDao;
    private final CustomerDao customerDao;
    private final LotDao lotDao;
    private final JournalDao journalDao;
    private final SalesDao salesDao;

    public ModuleOneRepository(KilgiDatabase database) {
        if (database == null) {
            throw new IllegalArgumentException("database cannot be null.");
        }
        this.database = database;
        this.userDao = database.userDao();
        this.providerDao = database.providerDao();
        this.customerDao = database.customerDao();
        this.lotDao = database.lotDao();
        this.journalDao = database.journalDao();
        this.salesDao = database.salesDao();
    }

    public ProviderEntity createProvider(String displayName, String contactNumber, String address, String notes) {
        validateRequiredText(displayName, "Provider name");
        String userId = ensureLocalUserExists();
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
        return provider;
    }

    public CustomerEntity createCustomer(String displayName, String contactNumber, String address, String notes) {
        validateRequiredText(displayName, "Customer name");
        String userId = ensureLocalUserExists();
        long now = System.currentTimeMillis();
        CustomerEntity customer = new CustomerEntity(
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
        customerDao.insert(customer);
        return customer;
    }

    public LotEntity createLot(
            String providerId,
            String vegetableType,
            int totalSacksPurchased,
            double rawKilosReceived,
            double baseUnitPrice,
            PaymentSource purchasePaymentSource,
            double standardFreight,
            PaymentSource freightPaymentSource
    ) {
        String userId = ensureLocalUserExists();
        ProviderEntity provider = requireProvider(providerId);
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
            persistJournalDraft(JournalEntryFactory.buildInitialLotEntry(lot));
        });
        return lot;
    }

    public BatchExpenseEntity addExpense(String lotId, AccountingAccount expenseAccount, double amount, PaymentSource paymentSource) {
        LotEntity lot = requireLot(lotId);
        if (expenseAccount == null) {
            throw new IllegalArgumentException("Expense account is required.");
        }
        validatePositiveAmount(amount, "Expense amount");
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
        validatePositiveAmount(kilosLost, "Spoilage kilos");
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

    public RetailSaleEntity recordRetailSale(double totalAmount, String notes) {
        validatePositiveAmount(totalAmount, "Retail sale amount");
        String userId = ensureLocalUserExists();
        RetailSaleEntity sale = new RetailSaleEntity(
                UUID.randomUUID().toString(),
                userId,
                totalAmount,
                normalizeOptionalText(notes),
                System.currentTimeMillis()
        );
        database.runInTransaction(() -> {
            salesDao.insertRetailSale(sale);
            persistJournalDraft(JournalEntryFactory.buildRetailSaleEntry(sale));
        });
        return sale;
    }

    public WholesaleInvoiceEntity createWholesaleInvoice(String customerId, String description, double totalAmount, String notes) {
        validatePositiveAmount(totalAmount, "Invoice amount");
        validateRequiredText(customerId, "Customer");
        validateRequiredText(description, "Invoice description");

        String userId = ensureLocalUserExists();
        CustomerEntity customer = requireCustomer(customerId);
        long now = System.currentTimeMillis();
        WholesaleInvoiceEntity invoice = new WholesaleInvoiceEntity(
                UUID.randomUUID().toString(),
                userId,
                customer.customerId,
                buildInvoiceNumber(now),
                description.trim(),
                totalAmount,
                normalizeOptionalText(notes),
                now
        );

        database.runInTransaction(() -> {
            salesDao.insertWholesaleInvoice(invoice);
            persistJournalDraft(JournalEntryFactory.buildWholesaleInvoiceEntry(invoice, customer));
        });
        return invoice;
    }

    public CustomerCollectionResult collectCustomerPayment(String customerId, double totalAmount, String notes) {
        validateRequiredText(customerId, "Customer");
        validatePositiveAmount(totalAmount, "Collection amount");

        String userId = ensureLocalUserExists();
        CustomerEntity customer = requireCustomer(customerId);
        List<OpenCustomerInvoice> openInvoices = getOpenInvoicesForCustomer(customerId);
        List<PaymentAllocationEngine.AllocationStep> plan = PaymentAllocationEngine.allocate(totalAmount, openInvoices);
        long now = System.currentTimeMillis();
        CustomerPaymentEntity payment = new CustomerPaymentEntity(
                UUID.randomUUID().toString(),
                userId,
                customer.customerId,
                totalAmount,
                normalizeOptionalText(notes),
                now
        );

        List<CustomerPaymentAllocationEntity> allocations = new ArrayList<>();
        List<WholesaleInvoiceEntity> invoices = new ArrayList<>();
        for (int index = 0; index < plan.size(); index++) {
            PaymentAllocationEngine.AllocationStep step = plan.get(index);
            allocations.add(new CustomerPaymentAllocationEntity(
                    UUID.randomUUID().toString(),
                    payment.paymentId,
                    step.getReferenceId(),
                    step.getAmountApplied(),
                    index,
                    now
            ));
            WholesaleInvoiceEntity invoice = salesDao.getInvoiceById(step.getReferenceId());
            if (invoice != null) {
                invoices.add(invoice);
            }
        }

        database.runInTransaction(() -> {
            salesDao.insertCustomerPayment(payment);
            salesDao.insertCustomerPaymentAllocations(allocations);
            persistJournalDraft(JournalEntryFactory.buildCustomerCollectionEntry(payment, customer, allocations, invoices));
        });
        return new CustomerCollectionResult(payment, allocations);
    }

    public ProviderSettlementResult settleProviderBalance(String providerId, double totalAmount, String notes) {
        validateRequiredText(providerId, "Provider");
        validatePositiveAmount(totalAmount, "Provider payment amount");

        String userId = ensureLocalUserExists();
        ProviderEntity provider = requireProvider(providerId);
        List<OpenProviderLotPayable> openPayables = getOpenLotPayablesForProvider(providerId);
        List<PaymentAllocationEngine.AllocationStep> plan = PaymentAllocationEngine.allocate(totalAmount, openPayables);
        long now = System.currentTimeMillis();
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
            LotEntity lot = lotDao.getLotById(step.getReferenceId(), userId);
            if (lot != null) {
                allocatedLots.add(lot);
            }
        }

        database.runInTransaction(() -> {
            salesDao.insertProviderPayment(payment);
            salesDao.insertProviderPaymentAllocations(allocations);
            persistJournalDraft(JournalEntryFactory.buildProviderSettlementEntry(payment, provider, allocations, allocatedLots));
        });
        return new ProviderSettlementResult(payment, allocations);
    }

    public List<ProviderEntity> getProviders() {
        String userId = ensureLocalUserExists();
        List<ProviderEntity> providers = providerDao.getActiveProvidersForUser(userId);
        return providers == null ? Collections.emptyList() : providers;
    }

    public List<CustomerEntity> getCustomers() {
        String userId = ensureLocalUserExists();
        List<CustomerEntity> customers = customerDao.getActiveCustomersForUser(userId);
        return customers == null ? Collections.emptyList() : customers;
    }

    public List<CustomerLedgerSummary> getCustomerLedgerSummaries() {
        String userId = ensureLocalUserExists();
        List<CustomerLedgerSummary> summaries = salesDao.getCustomerLedgerSummaries(userId);
        return summaries == null ? Collections.emptyList() : summaries;
    }

    public List<ProviderLedgerSummary> getProviderLedgerSummaries() {
        String userId = ensureLocalUserExists();
        List<ProviderLedgerSummary> summaries = salesDao.getProviderLedgerSummaries(userId);
        return summaries == null ? Collections.emptyList() : summaries;
    }

    public List<OpenCustomerInvoice> getOpenInvoicesForCustomer(String customerId) {
        validateRequiredText(customerId, "Customer");
        ensureLocalUserExists();
        List<OpenCustomerInvoice> invoices = salesDao.getOpenInvoicesForCustomer(customerId.trim());
        return invoices == null ? Collections.emptyList() : invoices;
    }

    public List<OpenProviderLotPayable> getOpenLotPayablesForProvider(String providerId) {
        validateRequiredText(providerId, "Provider");
        ensureLocalUserExists();
        List<OpenProviderLotPayable> payables = salesDao.getOpenLotPayablesForProvider(providerId.trim());
        return payables == null ? Collections.emptyList() : payables;
    }

    public LotWithDetails getLotWithDetails(String lotId) {
        return requireLotWithDetails(lotId);
    }

    public LotEntity getLatestLot() {
        String userId = ensureLocalUserExists();
        return lotDao.getLatestLot(userId);
    }

    public List<LotEntity> getAllLots() {
        String userId = ensureLocalUserExists();
        List<LotEntity> lots = lotDao.getAllLotsForUser(userId);
        return lots == null ? Collections.emptyList() : lots;
    }

    public List<JournalEntryWithLines> getJournalEntries(String lotId) {
        validateRequiredText(lotId, "Lot ID");
        String userId = ensureLocalUserExists();
        List<JournalEntryWithLines> entries = journalDao.getEntriesForLot(lotId.trim(), userId);
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

        String userId = ensureLocalUserExists();
        List<JournalEntryWithLines> entries = journalDao.getEntriesForPeriod(
                userId,
                start.getTimeInMillis(),
                end.getTimeInMillis()
        );
        return entries == null ? Collections.emptyList() : entries;
    }

    public List<JournalEntryWithLines> getJournalEntriesUpTo(int monthOfYear, int year) {
        if (monthOfYear < 1 || monthOfYear > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 9999) {
            throw new IllegalArgumentException("Year must be a valid four-digit value.");
        }

        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(year, monthOfYear - 1, 1, 0, 0, 0);
        end.add(Calendar.MONTH, 1);

        String userId = ensureLocalUserExists();
        List<JournalEntryWithLines> entries = journalDao.getEntriesUpTo(userId, end.getTimeInMillis());
        return entries == null ? Collections.emptyList() : entries;
    }

    public long getOldestJournalEntryTimestamp() {
        String userId = ensureLocalUserExists();
        Long timestamp = journalDao.getOldestEntryTimestamp(userId);
        return timestamp == null ? System.currentTimeMillis() : timestamp;
    }

    public long getLatestJournalEntryTimestamp() {
        String userId = ensureLocalUserExists();
        Long timestamp = journalDao.getLatestEntryTimestamp(userId);
        return timestamp == null ? System.currentTimeMillis() : timestamp;
    }

    private LotEntity requireLot(String lotId) {
        validateRequiredText(lotId, "Lot ID");
        String userId = ensureLocalUserExists();
        LotEntity lot = lotDao.getLotById(lotId.trim(), userId);
        if (lot == null) {
            throw new IllegalArgumentException("No lot found for ID: " + lotId.trim());
        }
        return lot;
    }

    private LotWithDetails requireLotWithDetails(String lotId) {
        validateRequiredText(lotId, "Lot ID");
        String userId = ensureLocalUserExists();
        LotWithDetails lot = lotDao.getLotWithDetails(lotId.trim(), userId);
        if (lot == null || lot.lot == null) {
            throw new IllegalArgumentException("No lot found for ID: " + lotId.trim());
        }
        return lot;
    }

    private ProviderEntity requireProvider(String providerId) {
        validateRequiredText(providerId, "Provider");
        ProviderEntity provider = providerDao.getById(providerId.trim());
        if (provider == null || !LOCAL_USER_ID.equals(provider.userId) || provider.isActive != 1) {
            throw new IllegalArgumentException("No provider found for the selected record.");
        }
        return provider;
    }

    private CustomerEntity requireCustomer(String customerId) {
        validateRequiredText(customerId, "Customer");
        CustomerEntity customer = customerDao.getById(customerId.trim());
        if (customer == null || !LOCAL_USER_ID.equals(customer.userId) || customer.isActive != 1) {
            throw new IllegalArgumentException("No customer found for the selected record.");
        }
        return customer;
    }

    private String ensureLocalUserExists() {
        UserEntity existingUser = userDao.getById(LOCAL_USER_ID);
        if (existingUser == null) {
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
        return LOCAL_USER_ID;
    }

    private void persistJournalDraft(LedgerEntryDraft draft) {
        journalDao.insertEntry(draft.getEntry());
        journalDao.insertLines(draft.getLines());
    }

    private static String buildInvoiceNumber(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return String.format(
                java.util.Locale.US,
                "INV-%04d%02d%02d-%02d%02d%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
        );
    }

    private static void validateRequiredText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private static void validatePositiveAmount(double value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero.");
        }
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

