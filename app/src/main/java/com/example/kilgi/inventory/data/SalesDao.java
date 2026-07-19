package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SalesDao {

    @Insert
    void insertRetailSale(RetailSaleEntity sale);

    @Insert
    void insertWholesaleInvoice(WholesaleInvoiceEntity invoice);

    @Insert
    void insertCustomerPayment(CustomerPaymentEntity payment);

    @Insert
    void insertCustomerPaymentAllocations(List<CustomerPaymentAllocationEntity> allocations);

    @Insert
    void insertProviderPayment(ProviderPaymentEntity payment);

    @Insert
    void insertProviderPaymentAllocations(List<ProviderPaymentAllocationEntity> allocations);

    @Query("SELECT * FROM retail_sales WHERE userId = :userId ORDER BY timestamp DESC")
    List<RetailSaleEntity> getRetailSalesForUser(String userId);

    @Query("SELECT * FROM wholesale_invoices WHERE userId = :userId ORDER BY timestamp DESC")
    List<WholesaleInvoiceEntity> getWholesaleInvoicesForUser(String userId);

    @Query("SELECT * FROM wholesale_invoices WHERE invoiceId = :invoiceId LIMIT 1")
    WholesaleInvoiceEntity getInvoiceById(String invoiceId);

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
    List<CustomerLedgerSummary> getCustomerLedgerSummaries(String userId);

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
    List<OpenCustomerInvoice> getOpenInvoicesForCustomer(String customerId);

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
    List<ProviderLedgerSummary> getProviderLedgerSummaries(String userId);

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
    List<OpenProviderLotPayable> getOpenLotPayablesForProvider(String providerId);
}

