package com.example.kilgi.inventory.service;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LotFilterUtilsTest {

    @Test
    public void matchesMonthAndDay_acceptsAllMonthsAndDays() {
        assertTrue(LotFilterUtils.matchesMonthAndDay(sampleTimestamp(2026, Calendar.JULY, 15), 0, 0));
    }

    @Test
    public void matchesMonthAndDay_matchesSpecificMonthAndDay() {
        assertTrue(LotFilterUtils.matchesMonthAndDay(sampleTimestamp(2026, Calendar.JULY, 15), 7, 15));
        assertTrue(LotFilterUtils.matchesMonthAndDay(sampleTimestamp(2026, Calendar.JULY, 15), 7, 0));
        assertTrue(LotFilterUtils.matchesMonthAndDay(sampleTimestamp(2026, Calendar.JULY, 15), 0, 15));
    }

    @Test
    public void matchesMonthAndDay_rejectsDifferentMonthOrDay() {
        long timestamp = sampleTimestamp(2026, Calendar.JULY, 15);
        assertFalse(LotFilterUtils.matchesMonthAndDay(timestamp, 6, 15));
        assertFalse(LotFilterUtils.matchesMonthAndDay(timestamp, 7, 14));
    }

    private long sampleTimestamp(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, dayOfMonth, 10, 30, 0);
        return calendar.getTimeInMillis();
    }
}

