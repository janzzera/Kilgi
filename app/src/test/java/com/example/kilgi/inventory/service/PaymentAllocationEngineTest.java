package com.example.kilgi.inventory.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PaymentAllocationEngineTest {

    @Test
    public void allocate_appliesPaymentInChronologicalOrderUntilExhausted() {
        List<PaymentAllocationEngine.OpenBalanceItem> items = Arrays.asList(
                item("oldest", 100.0),
                item("middle", 60.0),
                item("newest", 40.0)
        );

        List<PaymentAllocationEngine.AllocationStep> steps = PaymentAllocationEngine.allocate(130.0, items);

        assertEquals(2, steps.size());
        assertEquals("oldest", steps.get(0).getReferenceId());
        assertEquals(100.0, steps.get(0).getAmountApplied(), 0.0001);
        assertEquals("middle", steps.get(1).getReferenceId());
        assertEquals(30.0, steps.get(1).getAmountApplied(), 0.0001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allocate_rejectsPaymentThatExceedsOutstandingBalance() {
        PaymentAllocationEngine.allocate(300.0, Arrays.asList(
                item("oldest", 100.0),
                item("newest", 50.0)
        ));
    }

    private static PaymentAllocationEngine.OpenBalanceItem item(String referenceId, double outstandingBalance) {
        return new PaymentAllocationEngine.OpenBalanceItem() {
            @Override
            public String getReferenceId() {
                return referenceId;
            }

            @Override
            public double getOutstandingBalance() {
                return outstandingBalance;
            }
        };
    }
}

