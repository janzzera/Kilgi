package com.example.kilgi.inventory.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;
import com.example.kilgi.inventory.repository.SalesRepository;
import com.example.kilgi.inventory.repository.UserRepository;

import java.util.List;

public class SalesViewModel extends AndroidViewModel {

    private final SalesRepository repository;
    private final LiveData<List<RetailSaleEntity>> retailSales;
    private final LiveData<List<WholesaleInvoiceEntity>> wholesaleInvoices;

    public SalesViewModel(@NonNull Application application) {
        super(application);
        repository = new SalesRepository(application);
        retailSales = repository.getRetailSales(UserRepository.LOCAL_USER_ID);
        wholesaleInvoices = repository.getWholesaleInvoices(UserRepository.LOCAL_USER_ID);
    }

    public LiveData<List<RetailSaleEntity>> getRetailSales() {
        return retailSales;
    }

    public LiveData<List<WholesaleInvoiceEntity>> getWholesaleInvoices() {
        return wholesaleInvoices;
    }

    public LiveData<WholesaleInvoiceEntity> getInvoiceById(String invoiceId) {
        return repository.getInvoiceById(invoiceId);
    }

    public void recordRetailSale(double totalAmount, String notes, SalesRepository.Callback<RetailSaleEntity> callback) {
        repository.recordRetailSale(totalAmount, notes, UserRepository.LOCAL_USER_ID, callback);
    }

    public void createWholesaleInvoice(String customerId, String description, double totalAmount, String notes, SalesRepository.Callback<WholesaleInvoiceEntity> callback) {
        repository.createWholesaleInvoice(customerId, description, totalAmount, notes, UserRepository.LOCAL_USER_ID, callback);
    }
}
