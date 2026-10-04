package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.repository.CustomerRepository;
import com.example.kilgi.inventory.repository.UserRepository;
import com.example.kilgi.inventory.service.CustomerCollectionResult;

import java.util.List;

public class CustomerViewModel extends AndroidViewModel {

    private final CustomerRepository repository;
    private final LiveData<List<CustomerEntity>> activeCustomers;
    private final LiveData<List<CustomerLedgerSummary>> ledgerSummaries;

    public CustomerViewModel(@NonNull Application application) {
        super(application);
        repository = new CustomerRepository(application);
        activeCustomers = repository.getActiveCustomers(UserRepository.LOCAL_USER_ID);
        ledgerSummaries = repository.getCustomerLedgerSummaries(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<CustomerEntity>> getActiveCustomers() {
        return activeCustomers;
    }

    public LiveData<CustomerEntity> getCustomerById(String customerId) {
        return repository.getCustomerById(customerId);
    }

    public LiveData<List<CustomerLedgerSummary>> getLedgerSummaries() {
        return ledgerSummaries;
    }

    public LiveData<List<OpenCustomerInvoice>> getOpenInvoicesForCustomer(String customerId) {
        return repository.getOpenInvoicesForCustomer(customerId);
    }

    public void createCustomer(String displayName, String contactNumber, String address, String notes, CustomerRepository.Callback<CustomerEntity> callback) {
        repository.createCustomer(displayName, contactNumber, address, notes, UserRepository.LOCAL_USER_ID, callback);
    }

    public void collectCustomerPayment(String customerId, double totalAmount, String notes, List<OpenCustomerInvoice> openInvoices, CustomerRepository.Callback<CustomerCollectionResult> callback) {
        repository.collectCustomerPayment(customerId, totalAmount, notes, UserRepository.LOCAL_USER_ID, openInvoices, callback);
    }
}
