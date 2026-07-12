package com.example.kilgi.inventory.service;

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

public class ModuleOneAccountingTest {

    @Test
    public void initialLotEntry_balancesInventoryAgainstPurchaseAndFreightSources() {
        LotEntity lot = createLot();

        LedgerEntryDraft draft = JournalEntryFactory.buildInitialLotEntry(lot);

        assertNotNull(draft.getEntry());
        assertEquals(3, draft.getLines().size());
        assertEquals(170.0, sumByType(draft.getLines(), JournalLineType.DEBIT), 0.0001);
        assertEquals(170.0, sumByType(draft.getLines(), JournalLineType.CREDIT), 0.0001);
        assertEquals("12000", draft.getLines().get(0).accountCode);
        assertEquals("20100", draft.getLines().get(1).accountCode);
        assertEquals("10100", draft.getLines().get(2).accountCode);
    }

    @Test
    public void valuation_normalShrinkageInflatesTrueCostPerKiloWithoutWriteOff() {
        LotEntity lot = createLot();
        List<BatchExpenseEntity> expenses = Collections.singletonList(
                new BatchExpenseEntity("exp-1", lot.lotId, "Porter fee", 10.0, PaymentSource.CASH.getStoredValue(), 2L)
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
                new BatchExpenseEntity("exp-1", lot.lotId, "Porter fee", 10.0, PaymentSource.CASH.getStoredValue(), 2L)
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


