package com.example.kilgi.inventory.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.CustomerDao;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.CustomerPaymentAllocationEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;
import com.example.kilgi.inventory.data.JournalDao;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.data.SalesDao;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;
import com.example.kilgi.inventory.service.CustomerCollectionResult;
import com.example.kilgi.inventory.service.PaymentAllocationEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomerRepository {

    private final KilgiDatabase database;
    private final CustomerDao customerDao;
    private final SalesDao salesDao;
    private final JournalDao journalDao;

    public CustomerRepository(Context context) {
        this.database = KilgiDatabase.getInstance(context);
        this.customerDao = database.customerDao();
        this.salesDao = database.salesDao();
        this.journalDao = database.journalDao();
    }

    public LiveData<List<CustomerEntity>> getActiveCustomers(String userId) {
        return customerDao.getActiveCustomersForUser(userId);
    }

    public LiveData<CustomerEntity> getCustomerById(String customerId) {
        return customerDao.getById(customerId);
    }

    public LiveData<List<CustomerLedgerSummary>> getCustomerLedgerSummaries(String userId) {
        return salesDao.getCustomerLedgerSummaries(userId);
    }

    public LiveData<List<OpenCustomerInvoice>> getOpenInvoicesForCustomer(String customerId) {
        return salesDao.getOpenInvoicesForCustomer(customerId);
    }

    public void createCustomer(String displayName, String contactNumber, String address, String notes, String userId, Callback<CustomerEntity> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (displayName == null || displayName.trim().isEmpty()) {
                    throw new IllegalArgumentException("Customer name is required.");
                }
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
                if (callback != null) callback.onSuccess(customer);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void collectCustomerPayment(String customerId, double totalAmount, String notes, String userId, List<OpenCustomerInvoice> openInvoices, Callback<CustomerCollectionResult> callback) {
        KilgiDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (customerId == null || customerId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Customer is required.");
                }
                if (totalAmount <= 0) {
                    throw new IllegalArgumentException("Collection amount must be greater than zero.");
                }

                CustomerEntity customer = customerDao.getByIdSync(customerId.trim());
                if (customer == null || customer.isActive != 1) {
                    throw new IllegalArgumentException("No customer found for the selected record.");
                }

                long now = System.currentTimeMillis();
                List<PaymentAllocationEngine.AllocationStep> plan = PaymentAllocationEngine.allocate(totalAmount, openInvoices);
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
                    WholesaleInvoiceEntity invoice = salesDao.getInvoiceByIdSync(step.getReferenceId());
                    if (invoice != null) {
                        invoices.add(invoice);
                    }
                }

                database.runInTransaction(() -> {
                    salesDao.insertCustomerPayment(payment);
                    salesDao.insertCustomerPaymentAllocations(allocations);
                    LedgerEntryDraft draft = JournalEntryFactory.buildCustomerCollectionEntry(payment, customer, allocations, invoices);
                    journalDao.insertEntry(draft.getEntry());
                    journalDao.insertLines(draft.getLines());
                });

                CustomerCollectionResult result = new CustomerCollectionResult(payment, allocations);
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
