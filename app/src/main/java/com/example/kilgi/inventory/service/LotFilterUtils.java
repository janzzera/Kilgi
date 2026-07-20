package com.example.kilgi.inventory.service;

import java.util.Calendar;

/**
 * Matches lot timestamps against lightweight month/day filters used by the lot overview screen.
 */
public final class LotFilterUtils {

    public static final int ALL_YEARS = 0;
    public static final int ALL_MONTHS = 0;
    public static final int ALL_DAYS = 0;

    private LotFilterUtils() {
    }

    public static boolean matchesMonthAndDay(long timestamp, int monthOfYear, int dayOfMonth) {
        return matchesYearMonthAndDay(timestamp, ALL_YEARS, monthOfYear, dayOfMonth);
    }

    public static boolean matchesYearMonthAndDay(long timestamp, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        int actualYear = calendar.get(Calendar.YEAR);
        int actualMonth = calendar.get(Calendar.MONTH) + 1;
        int actualDay = calendar.get(Calendar.DAY_OF_MONTH);

        boolean yearMatches = year == ALL_YEARS || actualYear == year;
        boolean monthMatches = monthOfYear == ALL_MONTHS || actualMonth == monthOfYear;
        boolean dayMatches = dayOfMonth == ALL_DAYS || actualDay == dayOfMonth;
        return yearMatches && monthMatches && dayMatches;
    }
}


