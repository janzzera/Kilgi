package com.example.kilgi.inventory.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;
import com.example.kilgi.inventory.data.api.ApiService;
import com.example.kilgi.inventory.network.ApiClient;

import java.util.List;

import retrofit2.Response;

public class DataSyncWorker extends Worker {

    private static final String TAG = "DataSyncWorker";

    public DataSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync of unsynced offline records...");
        KilgiDatabase db = KilgiDatabase.getInstance(getApplicationContext());
        ApiService apiService = ApiClient.getApiService(getApplicationContext());

        boolean hasErrors = false;

        // 1. Sync Users
        try {
            List<UserEntity> unsyncedUsers = db.userDao().getUnsyncedUsersSync();
            for (UserEntity user : unsyncedUsers) {
                Response<UserEntity> response = apiService.registerUser(user).execute();
                if (response.isSuccessful()) {
                    user.isSynced = true;
                    db.userDao().insert(user);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing users", e);
            hasErrors = true;
        }

        // 2. Sync Providers
        try {
            List<ProviderEntity> unsyncedProviders = db.providerDao().getUnsyncedProvidersSync();
            for (ProviderEntity provider : unsyncedProviders) {
                Response<ProviderEntity> response = apiService.createProvider(provider).execute();
                if (response.isSuccessful()) {
                    provider.isSynced = true;
                    db.providerDao().insert(provider);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing providers", e);
            hasErrors = true;
        }

        // 3. Sync Customers
        try {
            List<CustomerEntity> unsyncedCustomers = db.customerDao().getUnsyncedCustomersSync();
            for (CustomerEntity customer : unsyncedCustomers) {
                Response<CustomerEntity> response = apiService.createCustomer(customer).execute();
                if (response.isSuccessful()) {
                    customer.isSynced = true;
                    db.customerDao().insert(customer);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing customers", e);
            hasErrors = true;
        }

        // 4. Sync Lots
        try {
            List<LotEntity> unsyncedLots = db.lotDao().getUnsyncedLotsSync();
            for (LotEntity lot : unsyncedLots) {
                Response<LotEntity> response = apiService.createLot(lot).execute();
                if (response.isSuccessful()) {
                    lot.isSynced = true;
                    db.lotDao().insert(lot);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing lots", e);
            hasErrors = true;
        }

        // 5. Sync Batch Expenses
        try {
            List<BatchExpenseEntity> unsyncedExpenses = db.batchExpenseDao().getUnsyncedExpensesSync();
            for (BatchExpenseEntity expense : unsyncedExpenses) {
                Response<BatchExpenseEntity> response = apiService.createBatchExpense(expense).execute();
                if (response.isSuccessful()) {
                    expense.isSynced = true;
                    db.batchExpenseDao().insert(expense);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing batch expenses", e);
            hasErrors = true;
        }

        // 6. Sync Spoilage Logs
        try {
            List<SpoilageLogEntity> unsyncedLogs = db.spoilageLogDao().getUnsyncedSpoilageLogsSync();
            for (SpoilageLogEntity log : unsyncedLogs) {
                Response<SpoilageLogEntity> response = apiService.createSpoilageLog(log).execute();
                if (response.isSuccessful()) {
                    log.isSynced = true;
                    db.spoilageLogDao().insert(log);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing spoilage logs", e);
            hasErrors = true;
        }

        // 7. Sync Retail Sales
        try {
            List<RetailSaleEntity> unsyncedSales = db.salesDao().getUnsyncedRetailSalesSync();
            for (RetailSaleEntity sale : unsyncedSales) {
                Response<RetailSaleEntity> response = apiService.createRetailSale(sale).execute();
                if (response.isSuccessful()) {
                    sale.isSynced = true;
                    db.salesDao().insertRetailSale(sale);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing retail sales", e);
            hasErrors = true;
        }

        // 8. Sync Wholesale Invoices
        try {
            List<WholesaleInvoiceEntity> unsyncedInvoices = db.salesDao().getUnsyncedWholesaleInvoicesSync();
            for (WholesaleInvoiceEntity invoice : unsyncedInvoices) {
                Response<WholesaleInvoiceEntity> response = apiService.createWholesaleInvoice(invoice).execute();
                if (response.isSuccessful()) {
                    invoice.isSynced = true;
                    db.salesDao().insertWholesaleInvoice(invoice);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing wholesale invoices", e);
            hasErrors = true;
        }

        // 9. Sync Customer Payments
        try {
            List<CustomerPaymentEntity> unsyncedPayments = db.salesDao().getUnsyncedCustomerPaymentsSync();
            for (CustomerPaymentEntity payment : unsyncedPayments) {
                Response<CustomerPaymentEntity> response = apiService.createCustomerPayment(payment).execute();
                if (response.isSuccessful()) {
                    payment.isSynced = true;
                    db.salesDao().insertCustomerPayment(payment);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing customer payments", e);
            hasErrors = true;
        }

        // 10. Sync Provider Payments
        try {
            List<ProviderPaymentEntity> unsyncedPayments = db.salesDao().getUnsyncedProviderPaymentsSync();
            for (ProviderPaymentEntity payment : unsyncedPayments) {
                Response<ProviderPaymentEntity> response = apiService.createProviderPayment(payment).execute();
                if (response.isSuccessful()) {
                    payment.isSynced = true;
                    db.salesDao().insertProviderPayment(payment);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing provider payments", e);
            hasErrors = true;
        }

        // 11. Sync Accounting Periods
        try {
            List<AccountingPeriodEntity> unsyncedPeriods = db.accountingPeriodDao().getUnsyncedPeriodsSync();
            for (AccountingPeriodEntity period : unsyncedPeriods) {
                Response<AccountingPeriodEntity> response = apiService.createAccountingPeriod(period).execute();
                if (response.isSuccessful()) {
                    period.isSynced = true;
                    db.accountingPeriodDao().insert(period);
                } else {
                    hasErrors = true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing accounting periods", e);
            hasErrors = true;
        }

        Log.d(TAG, "Completed background sync job. Has errors: " + hasErrors);
        return hasErrors ? Result.retry() : Result.success();
    }
}
