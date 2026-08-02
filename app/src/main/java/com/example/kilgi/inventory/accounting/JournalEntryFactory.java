package com.example.kilgi.inventory.accounting;

import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerPaymentAllocationEntity;
import com.example.kilgi.inventory.data.CustomerPaymentEntity;
import com.example.kilgi.inventory.data.JournalEntryEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderPaymentAllocationEntity;
import com.example.kilgi.inventory.data.ProviderPaymentEntity;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.SpoilageLogEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds balanced journal entries for module one inventory events.
 */
public final class JournalEntryFactory {

    private JournalEntryFactory() {
    }

    public static LedgerEntryDraft buildInitialLotEntry(LotEntity lot) {
        double purchaseAmount = lot.rawKilosReceived * lot.baseUnitPrice;
        String description = "Initial lot creation for " + lot.vegetableType + " (" + lot.lotId + ")";
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                lot.userId,
                lot.lotId,
                "LOT",
                lot.lotId,
                "INITIAL_LOT",
                description,
                lot.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                lot.lotId,
                AccountingCatalog.PURCHASES_CODE,
                AccountingCatalog.PURCHASES_NAME,
                JournalLineType.DEBIT,
                purchaseAmount,
                lot.providerId,
                null,
                null,
                lot.vegetableType + " purchase capitalization"
        ));
        lines.add(paymentLine(
                entry.entryId,
                lot.lotId,
                PaymentSource.fromStoredValue(lot.purchasePaymentSource),
                purchaseAmount,
                lot.providerId,
                "Provider purchase value"
        ));
        if (lot.standardFreight > 0) {
            lines.add(line(
                    entry.entryId,
                    lot.lotId,
                    AccountingCatalog.FREIGHT_IN_CODE,
                    AccountingCatalog.FREIGHT_IN_NAME,
                    JournalLineType.DEBIT,
                    lot.standardFreight,
                    lot.providerId,
                    null,
                    null,
                    "Freight-in capitalization"
            ));
            lines.add(paymentLine(
                    entry.entryId,
                    lot.lotId,
                    PaymentSource.fromStoredValue(lot.freightPaymentSource),
                    lot.standardFreight,
                    lot.providerId,
                    "Standard freight"
            ));
        }
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildExpenseEntry(LotEntity lot, BatchExpenseEntity expense) {
        String description = expense.expenseAccountName + " added to lot " + lot.lotId;
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                lot.userId,
                lot.lotId,
                "LOT",
                lot.lotId,
                "ADDITIONAL_EXPENSE",
                description,
                expense.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                lot.lotId,
                expense.expenseAccountCode,
                expense.expenseAccountName,
                JournalLineType.DEBIT,
                expense.amount,
                lot.providerId,
                null,
                expense.paymentSource,
                expense.expenseAccountName
        ));
        lines.add(paymentLine(
                entry.entryId,
                lot.lotId,
                PaymentSource.fromStoredValue(expense.paymentSource),
                expense.amount,
                lot.providerId,
                expense.expenseAccountName
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildAbnormalLossEntry(LotEntity lot, SpoilageLogEntity log, double writeOffAmount) {
        String description = "Abnormal spoilage write-off for lot " + lot.lotId;
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                lot.userId,
                lot.lotId,
                "LOT",
                lot.lotId,
                "ABNORMAL_SPOILAGE_WRITE_OFF",
                description,
                log.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                lot.lotId,
                AccountingCatalog.LOSS_CODE,
                AccountingCatalog.LOSS_NAME,
                JournalLineType.DEBIT,
                writeOffAmount,
                lot.providerId,
                null,
                null,
                log.kilosLost + "kg abnormal loss"
        ));
        lines.add(line(
                entry.entryId,
                lot.lotId,
                AccountingCatalog.INVENTORY_CODE,
                AccountingCatalog.INVENTORY_NAME,
                JournalLineType.CREDIT,
                writeOffAmount,
                lot.providerId,
                null,
                null,
                "Inventory write-off"
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildRetailSaleEntry(RetailSaleEntity sale) {
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                sale.userId,
                null,
                "RETAIL_SALE",
                sale.saleId,
                "RETAIL_EOD_SALE",
                "End-of-day retail bulk sale",
                sale.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.CASH_CODE,
                AccountingCatalog.CASH_NAME,
                JournalLineType.DEBIT,
                sale.totalAmount,
                null,
                null,
                PaymentSource.CASH.getStoredValue(),
                sale.notes
        ));
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.SALES_CODE,
                AccountingCatalog.SALES_NAME,
                JournalLineType.CREDIT,
                sale.totalAmount,
                null,
                null,
                null,
                sale.notes
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildWholesaleInvoiceEntry(WholesaleInvoiceEntity invoice, CustomerEntity customer) {
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                invoice.userId,
                null,
                "WHOLESALE_INVOICE",
                invoice.invoiceId,
                "WHOLESALE_ON_ACCOUNT_SALE",
                "Wholesale invoice " + invoice.invoiceNumber + " for " + customer.displayName,
                invoice.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.ACCOUNTS_RECEIVABLE_CODE,
                AccountingCatalog.ACCOUNTS_RECEIVABLE_NAME,
                JournalLineType.DEBIT,
                invoice.totalAmount,
                null,
                customer.customerId,
                null,
                invoice.invoiceNumber + " • " + invoice.description
        ));
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.SALES_CODE,
                AccountingCatalog.SALES_NAME,
                JournalLineType.CREDIT,
                invoice.totalAmount,
                null,
                customer.customerId,
                null,
                invoice.invoiceNumber + " • " + invoice.description
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildCustomerCollectionEntry(
            CustomerPaymentEntity payment,
            CustomerEntity customer,
            List<CustomerPaymentAllocationEntity> allocations,
            List<WholesaleInvoiceEntity> invoices
    ) {
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                payment.userId,
                null,
                "CUSTOMER_COLLECTION",
                payment.paymentId,
                "WHOLESALE_COLLECTION",
                "Collection from " + customer.displayName,
                payment.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.CASH_CODE,
                AccountingCatalog.CASH_NAME,
                JournalLineType.DEBIT,
                payment.totalAmount,
                null,
                customer.customerId,
                PaymentSource.CASH.getStoredValue(),
                payment.notes
        ));

        Map<String, WholesaleInvoiceEntity> invoicesById = mapInvoicesById(invoices);
        for (CustomerPaymentAllocationEntity allocation : allocations) {
            WholesaleInvoiceEntity invoice = invoicesById.get(allocation.invoiceId);
            String memo = invoice == null
                    ? "A/R settlement"
                    : invoice.invoiceNumber + " • " + invoice.description;
            lines.add(line(
                    entry.entryId,
                    null,
                    AccountingCatalog.ACCOUNTS_RECEIVABLE_CODE,
                    AccountingCatalog.ACCOUNTS_RECEIVABLE_NAME,
                    JournalLineType.CREDIT,
                    allocation.amountApplied,
                    null,
                    customer.customerId,
                    null,
                    memo
            ));
        }
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildProviderSettlementEntry(
            ProviderPaymentEntity payment,
            ProviderEntity provider,
            List<ProviderPaymentAllocationEntity> allocations,
            List<LotEntity> lots
    ) {
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                payment.userId,
                null,
                "PROVIDER_SETTLEMENT",
                payment.paymentId,
                "PROVIDER_AP_SETTLEMENT",
                "Provider settlement for " + provider.displayName,
                payment.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        Map<String, LotEntity> lotsById = mapLotsById(lots);
        for (ProviderPaymentAllocationEntity allocation : allocations) {
            LotEntity lot = lotsById.get(allocation.lotId);
            String memo = lot == null
                    ? "Accounts payable settlement"
                    : lot.vegetableType + " lot " + lot.lotId;
            lines.add(line(
                    entry.entryId,
                    allocation.lotId,
                    AccountingCatalog.ACCOUNTS_PAYABLE_CODE,
                    AccountingCatalog.ACCOUNTS_PAYABLE_NAME,
                    JournalLineType.DEBIT,
                    allocation.amountApplied,
                    provider.providerId,
                    null,
                    null,
                    memo
            ));
        }
        lines.add(line(
                entry.entryId,
                null,
                AccountingCatalog.CASH_CODE,
                AccountingCatalog.CASH_NAME,
                JournalLineType.CREDIT,
                payment.totalAmount,
                provider.providerId,
                null,
                PaymentSource.CASH.getStoredValue(),
                payment.notes
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildAdjustingEntry(
            String userId,
            long timestamp,
            AccountingAccount debitAccount,
            AccountingAccount creditAccount,
            double amount,
            String memo
    ) {
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                userId,
                null,
                "ADJUSTMENT",
                null,
                "MANUAL_ADJUSTING_ENTRY",
                memo,
                timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                null,
                debitAccount.getCode(),
                debitAccount.getName(),
                JournalLineType.DEBIT,
                amount,
                null,
                null,
                null,
                memo
        ));
        lines.add(line(
                entry.entryId,
                null,
                creditAccount.getCode(),
                creditAccount.getName(),
                JournalLineType.CREDIT,
                amount,
                null,
                null,
                null,
                memo
        ));

        return new LedgerEntryDraft(entry, lines);
    }

    private static JournalLineEntity paymentLine(
            String entryId,
            String lotId,
            PaymentSource paymentSource,
            double amount,
            String providerId,
            String memo
    ) {
        return line(
                entryId,
                lotId,
                paymentSource.getAccountCode(),
                paymentSource.getAccountName(),
                JournalLineType.CREDIT,
                amount,
                providerId,
                null,
                paymentSource.getStoredValue(),
                memo
        );
    }

    private static JournalLineEntity line(
            String entryId,
            String lotId,
            String accountCode,
            String accountName,
            JournalLineType lineType,
            double amount,
            String providerId,
            String customerId,
            String paymentSource,
            String memo
    ) {
        return new JournalLineEntity(
                UUID.randomUUID().toString(),
                entryId,
                lotId,
                accountCode,
                accountName,
                lineType.name(),
                amount,
                providerId,
                customerId,
                paymentSource,
                memo
        );
    }

    private static Map<String, WholesaleInvoiceEntity> mapInvoicesById(List<WholesaleInvoiceEntity> invoices) {
        Map<String, WholesaleInvoiceEntity> map = new LinkedHashMap<>();
        if (invoices == null) {
            return map;
        }
        for (WholesaleInvoiceEntity invoice : invoices) {
            map.put(invoice.invoiceId, invoice);
        }
        return map;
    }

    private static Map<String, LotEntity> mapLotsById(List<LotEntity> lots) {
        Map<String, LotEntity> map = new LinkedHashMap<>();
        if (lots == null) {
            return map;
        }
        for (LotEntity lot : lots) {
            map.put(lot.lotId, lot);
        }
        return map;
    }
}

