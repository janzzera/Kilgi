package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.CustomerDao;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.SalesDao;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class SalesRepository {

    private final KilgiDatabase database;
    private final SalesDao salesDao;
    private final CustomerDao customerDao;
    private final JournalDao journalDao;

    public SalesRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.salesDao = database.salesDao();
        this.customerDao = database.customerDao();
        this.journalDao = database.journalDao();
    }

    public LiveData<List<RetailSaleEntity>> getRetailSales(String userId) {
        return salesDao.getRetailSalesForUser(userId);
    }

    public LiveData<List<WholesaleInvoiceEntity>> getWholesaleInvoices(String userId) {
        return salesDao.getWholesaleInvoicesForUser(userId);
    }

    public LiveData<WholesaleInvoiceEntity> getInvoiceById(String invoiceId) {
        return salesDao.getInvoiceById(invoiceId);
    }

    public void recordRetailSale(double totalAmount, String notes, String userId, Callback<RetailSaleEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (totalAmount <= 0) {
                    throw new IllegalArgumentException("Retail sale amount must be greater than zero.");
                }
                long now = System.currentTimeMillis();
                RetailSaleEntity sale = new RetailSaleEntity(
                        UUID.randomUUID().toString(),
                        userId,
                        totalAmount,
                        normalizeOptionalText(notes),
                        now
                );

                database.runInTransaction(() -> {
                    salesDao.insertRetailSale(sale);
                    LedgerEntryDraft draft = JournalEntryFactory.buildRetailSaleEntry(sale);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                if (callback != null) callback.onSuccess(sale);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void createWholesaleInvoice(String customerId, String description, double totalAmount, String notes, String userId, Callback<WholesaleInvoiceEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (customerId == null || customerId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Customer is required.");
                }
                if (description == null || description.trim().isEmpty()) {
                    throw new IllegalArgumentException("Invoice description is required.");
                }
                if (totalAmount <= 0) {
                    throw new IllegalArgumentException("Invoice amount must be greater than zero.");
                }

                CustomerEntity customer = customerDao.getByIdSync(customerId.trim());
                if (customer == null || customer.isActive != 1) {
                    throw new IllegalArgumentException("No customer found for the selected record.");
                }

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
                    LedgerEntryDraft draft = JournalEntryFactory.buildWholesaleInvoiceEntry(invoice, customer);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                if (callback != null) callback.onSuccess(invoice);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
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
