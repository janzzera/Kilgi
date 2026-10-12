package com.example.kilgi.inventory.data.api;

import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // ==========================================
    // 1. Authentication & Users
    // ==========================================
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("users")
    Call<UserEntity> registerUser(@Body UserEntity user);

    @GET("users/{id}")
    Call<UserEntity> getUserById(@Path("id") String userId);

    @PUT("users/{id}")
    Call<UserEntity> updateUser(@Path("id") String userId, @Body UserEntity user);


    // ==========================================
    // 2. Providers
    // ==========================================
    @GET("providers")
    Call<List<ProviderEntity>> getProviders();

    @GET("providers/{id}")
    Call<ProviderEntity> getProviderById(@Path("id") String providerId);

    @POST("providers")
    Call<ProviderEntity> createProvider(@Body ProviderEntity provider);

    @PUT("providers/{id}")
    Call<ProviderEntity> updateProvider(@Path("id") String providerId, @Body ProviderEntity provider);

    @DELETE("providers/{id}")
    Call<Void> deleteProvider(@Path("id") String providerId);


    // ==========================================
    // 3. Customers
    // ==========================================
    @GET("customers")
    Call<List<CustomerEntity>> getCustomers();

    @GET("customers/{id}")
    Call<CustomerEntity> getCustomerById(@Path("id") String customerId);

    @POST("customers")
    Call<CustomerEntity> createCustomer(@Body CustomerEntity customer);

    @PUT("customers/{id}")
    Call<CustomerEntity> updateCustomer(@Path("id") String customerId, @Body CustomerEntity customer);

    @DELETE("customers/{id}")
    Call<Void> deleteCustomer(@Path("id") String customerId);


    // ==========================================
    // 4. Lots / Procurement Batches
    // ==========================================
    @GET("lots")
    Call<List<LotEntity>> getLots();

    @GET("lots/{id}")
    Call<LotEntity> getLotById(@Path("id") String lotId);

    @POST("lots")
    Call<LotEntity> createLot(@Body LotEntity lot);

    @PUT("lots/{id}")
    Call<LotEntity> updateLot(@Path("id") String lotId, @Body LotEntity lot);

    @DELETE("lots/{id}")
    Call<Void> deleteLot(@Path("id") String lotId);


    // ==========================================
    // 5. Batch Expenses
    // ==========================================
    @GET("batchexpenses")
    Call<List<BatchExpenseEntity>> getBatchExpenses();

    @GET("batchexpenses/lot/{lotId}")
    Call<List<BatchExpenseEntity>> getExpensesByLotId(@Path("lotId") String lotId);

    @POST("batchexpenses")
    Call<BatchExpenseEntity> createBatchExpense(@Body BatchExpenseEntity expense);

    @DELETE("batchexpenses/{id}")
    Call<Void> deleteBatchExpense(@Path("id") String expenseId);


    // ==========================================
    // 6. Spoilage Logs
    // ==========================================
    @GET("spoilagelogs")
    Call<List<SpoilageLogEntity>> getSpoilageLogs();

    @GET("spoilagelogs/lot/{lotId}")
    Call<List<SpoilageLogEntity>> getSpoilageByLotId(@Path("lotId") String lotId);

    @POST("spoilagelogs")
    Call<SpoilageLogEntity> createSpoilageLog(@Body SpoilageLogEntity log);

    @DELETE("spoilagelogs/{id}")
    Call<Void> deleteSpoilageLog(@Path("id") String logId);


    // ==========================================
    // 7. Journal Entries & Accounting
    // ==========================================
    @GET("journalentries")
    Call<List<JournalEntryWithLines>> getJournalEntries();

    @GET("journalentries/{id}")
    Call<JournalEntryWithLines> getJournalEntryById(@Path("id") String entryId);

    @GET("journalentries/lot/{lotId}")
    Call<List<JournalEntryWithLines>> getJournalEntriesByLotId(@Path("lotId") String lotId);

    @POST("journalentries")
    Call<JournalEntryWithLines> createJournalEntry(@Body JournalEntryWithLines entry);


    // ==========================================
    // 8. Retail Sales
    // ==========================================
    @GET("retailsales")
    Call<List<RetailSaleEntity>> getRetailSales();

    @POST("retailsales")
    Call<RetailSaleEntity> createRetailSale(@Body RetailSaleEntity sale);


    // ==========================================
    // 9. Wholesale Invoices
    // ==========================================
    @GET("wholesaleinvoices")
    Call<List<WholesaleInvoiceEntity>> getWholesaleInvoices();

    @GET("wholesaleinvoices/{id}")
    Call<WholesaleInvoiceEntity> getInvoiceById(@Path("id") String invoiceId);

    @POST("wholesaleinvoices")
    Call<WholesaleInvoiceEntity> createWholesaleInvoice(@Body WholesaleInvoiceEntity invoice);


    // ==========================================
    // 10. Customer Payments
    // ==========================================
    @GET("customerpayments")
    Call<List<CustomerPaymentEntity>> getCustomerPayments();

    @POST("customerpayments")
    Call<CustomerPaymentEntity> createCustomerPayment(@Body CustomerPaymentEntity payment);


    // ==========================================
    // 11. Provider Payments
    // ==========================================
    @GET("providerpayments")
    Call<List<ProviderPaymentEntity>> getProviderPayments();

    @POST("providerpayments")
    Call<ProviderPaymentEntity> createProviderPayment(@Body ProviderPaymentEntity payment);


    // ==========================================
    // 12. Accounting Periods
    // ==========================================
    @GET("accountingperiods")
    Call<List<AccountingPeriodEntity>> getAccountingPeriods();

    @GET("accountingperiods/{id}")
    Call<AccountingPeriodEntity> getAccountingPeriodById(@Path("id") long periodId);

    @POST("accountingperiods")
    Call<AccountingPeriodEntity> createAccountingPeriod(@Body AccountingPeriodEntity period);

    @PUT("accountingperiods/{id}")
    Call<AccountingPeriodEntity> updateAccountingPeriod(@Path("id") long periodId, @Body AccountingPeriodEntity period);
}
