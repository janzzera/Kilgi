package com.example.kilgi.inventory.accounting;

import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.JournalEntryEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.SpoilageLogEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds balanced journal entries for module one inventory events.
 */
public final class JournalEntryFactory {

    private JournalEntryFactory() {
    }

    public static LedgerEntryDraft buildInitialLotEntry(LotEntity lot) {
        double purchaseAmount = lot.rawKilosReceived * lot.baseUnitPrice;
        double totalCapitalized = purchaseAmount + lot.standardFreight;
        String description = "Initial lot creation for " + lot.vegetableType + " (" + lot.lotId + ")";
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                lot.lotId,
                "INITIAL_LOT",
                description,
                lot.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                lot.lotId,
                AccountingCatalog.INVENTORY_CODE,
                AccountingCatalog.INVENTORY_NAME,
                JournalLineType.DEBIT,
                totalCapitalized,
                lot.providerId,
                null,
                lot.vegetableType + " batch capitalization"
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
        String description = expense.expenseLabel + " added to lot " + lot.lotId;
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
                lot.lotId,
                "ADDITIONAL_EXPENSE",
                description,
                expense.timestamp
        );

        List<JournalLineEntity> lines = new ArrayList<>();
        lines.add(line(
                entry.entryId,
                lot.lotId,
                AccountingCatalog.INVENTORY_CODE,
                AccountingCatalog.INVENTORY_NAME,
                JournalLineType.DEBIT,
                expense.amount,
                lot.providerId,
                expense.paymentSource,
                expense.expenseLabel
        ));
        lines.add(paymentLine(
                entry.entryId,
                lot.lotId,
                PaymentSource.fromStoredValue(expense.paymentSource),
                expense.amount,
                lot.providerId,
                expense.expenseLabel
        ));
        return new LedgerEntryDraft(entry, lines);
    }

    public static LedgerEntryDraft buildAbnormalLossEntry(LotEntity lot, SpoilageLogEntity log, double writeOffAmount) {
        String description = "Abnormal spoilage write-off for lot " + lot.lotId;
        JournalEntryEntity entry = new JournalEntryEntity(
                UUID.randomUUID().toString(),
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
                "Inventory write-off"
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
                paymentSource,
                memo
        );
    }
}

