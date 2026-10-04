package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRetailSale(RetailSaleEntity sale);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRetailSales(List<RetailSaleEntity> sales);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWholesaleInvoice(WholesaleInvoiceEntity invoice);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertWholesaleInvoices(List<WholesaleInvoiceEntity> invoices);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCustomerPayment(CustomerPaymentEntity payment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCustomerPayments(List<CustomerPaymentEntity> payments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCustomerPaymentAllocations(List<CustomerPaymentAllocationEntity> allocations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProviderPayment(ProviderPaymentEntity payment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProviderPayments(List<ProviderPaymentEntity> payments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProviderPaymentAllocations(List<ProviderPaymentAllocationEntity> allocations);

    @Query("SELECT * FROM retail_sales WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<RetailSaleEntity>> getRetailSalesForUser(String userId);

    @Query("SELECT * FROM wholesale_invoices WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<WholesaleInvoiceEntity>> getWholesaleInvoicesForUser(String userId);

    @Query("SELECT * FROM wholesale_invoices WHERE invoiceId = :invoiceId LIMIT 1")
    LiveData<WholesaleInvoiceEntity> getInvoiceById(String invoiceId);

    @Query(
            "SELECT c.customerId AS customerId, c.displayName AS displayName, " +
                    "SUM(CASE WHEN balances.outstandingBalance > 0.0000001 THEN 1 ELSE 0 END) AS openInvoiceCount, " +
                    "COALESCE(SUM(CASE WHEN balances.outstandingBalance > 0 THEN balances.outstandingBalance ELSE 0 END), 0) AS outstandingBalance " +
                    "FROM customers c " +
                    "LEFT JOIN (" +
                    "    SELECT i.invoiceId AS invoiceId, i.customerId AS customerId, " +
                    "           i.totalAmount - COALESCE(SUM(a.amountApplied), 0) AS outstandingBalance " +
                    "    FROM wholesale_invoices i " +
                    "    LEFT JOIN customer_payment_allocations a ON a.invoiceId = i.invoiceId " +
                    "    GROUP BY i.invoiceId, i.customerId, i.totalAmount" +
                    ") balances ON balances.customerId = c.customerId " +
                    "WHERE c.userId = :userId AND c.isActive = 1 " +
                    "GROUP BY c.customerId, c.displayName " +
                    "ORDER BY c.displayName COLLATE NOCASE ASC"
    )
    LiveData<List<CustomerLedgerSummary>> getCustomerLedgerSummaries(String userId);

    @Query(
            "SELECT i.invoiceId AS invoiceId, i.customerId AS customerId, i.invoiceNumber AS invoiceNumber, " +
                    "i.description AS description, i.totalAmount AS totalAmount, i.timestamp AS timestamp, " +
                    "i.totalAmount - COALESCE(SUM(a.amountApplied), 0) AS outstandingBalance " +
                    "FROM wholesale_invoices i " +
                    "LEFT JOIN customer_payment_allocations a ON a.invoiceId = i.invoiceId " +
                    "WHERE i.customerId = :customerId " +
                    "GROUP BY i.invoiceId, i.customerId, i.invoiceNumber, i.description, i.totalAmount, i.timestamp " +
                    "HAVING outstandingBalance > 0.0000001 " +
                    "ORDER BY i.timestamp ASC, i.invoiceNumber ASC"
    )
    LiveData<List<OpenCustomerInvoice>> getOpenInvoicesForCustomer(String customerId);

    @Query(
            "SELECT p.providerId AS providerId, p.displayName AS displayName, " +
                    "SUM(CASE WHEN balances.outstandingBalance > 0.0000001 THEN 1 ELSE 0 END) AS openLotCount, " +
                    "COALESCE(SUM(CASE WHEN balances.outstandingBalance > 0 THEN balances.outstandingBalance ELSE 0 END), 0) AS outstandingBalance " +
                    "FROM providers p " +
                    "LEFT JOIN (" +
                    "    SELECT l.lotId AS lotId, l.providerId AS providerId, " +
                    "           ((CASE WHEN l.purchasePaymentSource = 'AP' THEN (l.rawKilosReceived * l.baseUnitPrice) ELSE 0 END) + " +
                    "            (CASE WHEN l.freightPaymentSource = 'AP' THEN l.standardFreight ELSE 0 END) - COALESCE(SUM(a.amountApplied), 0)) AS outstandingBalance " +
                    "    FROM lots l " +
                    "    LEFT JOIN provider_payment_allocations a ON a.lotId = l.lotId " +
                    "    GROUP BY l.lotId, l.providerId, l.rawKilosReceived, l.baseUnitPrice, l.purchasePaymentSource, l.standardFreight, l.freightPaymentSource" +
                    ") balances ON balances.providerId = p.providerId " +
                    "WHERE p.userId = :userId AND p.isActive = 1 " +
                    "GROUP BY p.providerId, p.displayName " +
                    "ORDER BY p.displayName COLLATE NOCASE ASC"
    )
    LiveData<List<ProviderLedgerSummary>> getProviderLedgerSummaries(String userId);

    @Query(
            "SELECT l.lotId AS lotId, l.providerId AS providerId, l.providerName AS providerName, l.vegetableType AS vegetableType, l.timestamp AS timestamp, " +
                    "       ((CASE WHEN l.purchasePaymentSource = 'AP' THEN (l.rawKilosReceived * l.baseUnitPrice) ELSE 0 END) + " +
                    "        (CASE WHEN l.freightPaymentSource = 'AP' THEN l.standardFreight ELSE 0 END)) AS originalPayableAmount, " +
                    "       ((CASE WHEN l.purchasePaymentSource = 'AP' THEN (l.rawKilosReceived * l.baseUnitPrice) ELSE 0 END) + " +
                    "        (CASE WHEN l.freightPaymentSource = 'AP' THEN l.standardFreight ELSE 0 END) - COALESCE(SUM(a.amountApplied), 0)) AS outstandingBalance " +
                    "FROM lots l " +
                    "LEFT JOIN provider_payment_allocations a ON a.lotId = l.lotId " +
                    "WHERE l.providerId = :providerId " +
                    "GROUP BY l.lotId, l.providerId, l.providerName, l.vegetableType, l.timestamp, l.rawKilosReceived, l.baseUnitPrice, l.purchasePaymentSource, l.standardFreight, l.freightPaymentSource " +
                    "HAVING originalPayableAmount > 0.0000001 AND outstandingBalance > 0.0000001 " +
                    "ORDER BY l.timestamp ASC, l.lotId ASC"
    )
    LiveData<List<OpenProviderLotPayable>> getOpenLotPayablesForProvider(String providerId);
}

