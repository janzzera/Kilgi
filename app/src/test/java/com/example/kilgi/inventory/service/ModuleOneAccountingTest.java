package com.example.kilgi.inventory.service;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.AccountingCatalog;
import com.example.kilgi.inventory.accounting.JournalEntryFactory;
import com.example.kilgi.inventory.accounting.LedgerEntryDraft;
import com.example.kilgi.inventory.data.BatchExpenseEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.SpoilageLogEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class ModuleOneAccountingTest {

    @Test
    public void initialLotEntry_balancesInventoryAgainstPurchaseAndFreightSources() {
        LotEntity lot = createLot();

        LedgerEntryDraft draft = JournalEntryFactory.buildInitialLotEntry(lot);

        assertNotNull(draft.getEntry());
        assertEquals(4, draft.getLines().size());
        assertEquals(170.0, sumByType(draft.getLines(), JournalLineType.DEBIT), 0.0001);
        assertEquals(170.0, sumByType(draft.getLines(), JournalLineType.CREDIT), 0.0001);
        assertEquals(AccountingCatalog.INVENTORY_CODE, draft.getLines().get(0).accountCode);
        assertEquals(AccountingCatalog.ACCOUNTS_PAYABLE_CODE, draft.getLines().get(1).accountCode);
        assertEquals(AccountingCatalog.FREIGHT_IN_CODE, draft.getLines().get(2).accountCode);
        assertEquals(AccountingCatalog.CASH_CODE, draft.getLines().get(3).accountCode);
    }

    @Test
    public void additionalExpenseEntry_usesSelectedExpenseAccountForDebit() {
        LotEntity lot = createLot();
        BatchExpenseEntity expense = new BatchExpenseEntity(
                "exp-1",
                lot.lotId,
                AccountingCatalog.FREIGHT_IN_CODE,
                AccountingCatalog.FREIGHT_IN_NAME,
                50.0,
                PaymentSource.CASH.getStoredValue(),
                2L
        );

        LedgerEntryDraft draft = JournalEntryFactory.buildExpenseEntry(lot, expense);

        assertEquals(2, draft.getLines().size());
        assertEquals(AccountingCatalog.FREIGHT_IN_CODE, draft.getLines().get(0).accountCode);
        assertEquals(AccountingCatalog.CASH_CODE, draft.getLines().get(1).accountCode);
        assertEquals(50.0, sumByType(draft.getLines(), JournalLineType.DEBIT), 0.0001);
        assertEquals(50.0, sumByType(draft.getLines(), JournalLineType.CREDIT), 0.0001);
    }

    @Test
    public void chartOfAccounts_exposesExpectedMerchandisingAccountsByCategory() {
        assertEquals(AccountingAccount.Category.ASSET, AccountingCatalog.CASH_IN_BANK.getCategory());
        assertEquals(AccountingAccount.Category.LIABILITY, AccountingCatalog.ACCOUNTS_PAYABLE.getCategory());
        assertEquals(AccountingAccount.Category.OWNER_EQUITY, AccountingCatalog.CAPITAL.getCategory());
        assertEquals(AccountingAccount.Category.REVENUE, AccountingCatalog.SALES.getCategory());
        assertEquals(AccountingAccount.Category.COST, AccountingCatalog.PURCHASES.getCategory());
        assertEquals(AccountingAccount.Category.EXPENSE, AccountingCatalog.FREIGHT_OUT.getCategory());
        assertEquals(4, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.ASSET).size());
        assertEquals(2, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.LIABILITY).size());
        assertEquals(2, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.OWNER_EQUITY).size());
        assertEquals(3, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.REVENUE).size());
        assertEquals(5, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.COST).size());
        assertEquals(8, AccountingCatalog.getAccountsByCategory(AccountingAccount.Category.EXPENSE).size());
        assertEquals(24, AccountingCatalog.getAllAccounts().size());
        assertEquals(AccountingCatalog.FREIGHT_IN_CODE, AccountingCatalog.getLotExpenseAccounts().get(0).getCode());
    }

    @Test
    public void accountLookups_andPaymentSources_resolveToCentralCatalog() {
        assertSame(AccountingCatalog.CASH_IN_BANK, AccountingCatalog.requireByCode(AccountingCatalog.CASH_CODE));
        assertSame(AccountingCatalog.ACCOUNTS_PAYABLE, AccountingCatalog.requireByCode(AccountingCatalog.ACCOUNTS_PAYABLE_CODE));
        assertEquals(AccountingCatalog.CASH_CODE, PaymentSource.CASH.getAccountCode());
        assertEquals(AccountingCatalog.CASH_NAME, PaymentSource.CASH.getAccountName());
        assertEquals(AccountingCatalog.ACCOUNTS_PAYABLE_CODE, PaymentSource.ACCOUNTS_PAYABLE.getAccountCode());
        assertEquals(AccountingCatalog.ACCOUNTS_PAYABLE_NAME, PaymentSource.ACCOUNTS_PAYABLE.getAccountName());
    }

    @Test
    public void valuation_normalShrinkageInflatesTrueCostPerKiloWithoutWriteOff() {
        LotEntity lot = createLot();
        List<BatchExpenseEntity> expenses = Collections.singletonList(
                new BatchExpenseEntity(
                        "exp-1",
                        lot.lotId,
                        AccountingCatalog.SUPPLIES_EXPENSE.getCode(),
                        AccountingCatalog.SUPPLIES_EXPENSE.getName(),
                        10.0,
                        PaymentSource.CASH.getStoredValue(),
                        2L
                )
        );
        List<SpoilageLogEntity> logs = Collections.singletonList(
                new SpoilageLogEntity("log-1", lot.lotId, 15.0, LossType.NORMAL.name(), 3L)
        );

        BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(lot, expenses, logs);

        assertEquals(150.0, snapshot.getPurchaseCost(), 0.0001);
        assertEquals(30.0, snapshot.getExpenseTotal(), 0.0001);
        assertEquals(180.0, snapshot.getTotalCapitalizedCost(), 0.0001);
        assertEquals(85.0, snapshot.getNetUsableKilograms(), 0.0001);
        assertEquals(0.0, snapshot.getAbnormalWriteOffValue(), 0.0001);
        assertEquals(2.117647, snapshot.getTrueCostPerKilo(), 0.0001);
    }

    @Test
    public void valuation_abnormalLossWritesOffInventoryAtCurrentUnitCost() {
        LotEntity lot = createLot();
        List<BatchExpenseEntity> expenses = Collections.singletonList(
                new BatchExpenseEntity(
                        "exp-1",
                        lot.lotId,
                        AccountingCatalog.SUPPLIES_EXPENSE.getCode(),
                        AccountingCatalog.SUPPLIES_EXPENSE.getName(),
                        10.0,
                        PaymentSource.CASH.getStoredValue(),
                        2L
                )
        );
        List<SpoilageLogEntity> logs = Arrays.asList(
                new SpoilageLogEntity("log-1", lot.lotId, 10.0, LossType.NORMAL.name(), 3L),
                new SpoilageLogEntity("log-2", lot.lotId, 20.0, LossType.ABNORMAL.name(), 4L)
        );

        BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(lot, expenses, logs);

        assertEquals(40.0, snapshot.getAbnormalWriteOffValue(), 0.0001);
        assertEquals(140.0, snapshot.getTotalCapitalizedCost(), 0.0001);
        assertEquals(70.0, snapshot.getNetUsableKilograms(), 0.0001);
        assertEquals(2.0, snapshot.getTrueCostPerKilo(), 0.0001);
    }

    private LotEntity createLot() {
        return new LotEntity(
                "lot-1",
                ModuleOneRepository.LOCAL_USER_ID,
                "PROV-XYZ",
                "Provider XYZ",
                "Tomatoes",
                4,
                100.0,
                1.5,
                PaymentSource.ACCOUNTS_PAYABLE.getStoredValue(),
                20.0,
                PaymentSource.CASH.getStoredValue(),
                1L
        );
    }

    private double sumByType(List<JournalLineEntity> lines, JournalLineType targetType) {
        double total = 0;
        for (JournalLineEntity line : lines) {
            if (JournalLineType.valueOf(line.lineType) == targetType) {
                total += line.amount;
            }
        }
        return total;
    }
}


