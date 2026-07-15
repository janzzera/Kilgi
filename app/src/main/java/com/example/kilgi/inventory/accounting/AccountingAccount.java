package com.example.kilgi.inventory.accounting;

import androidx.annotation.NonNull;

import com.example.kilgi.inventory.data.JournalLineType;

/**
 * Immutable chart-of-accounts record used by journal builders and UI displays.
 */
public final class AccountingAccount {

    public enum Category {
        ASSET("Assets"),
        LIABILITY("Liabilities"),
        OWNER_EQUITY("Owner's Equity"),
        REVENUE("Revenues"),
        COST("Costs"),
        EXPENSE("Expenses");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String code;
    private final String name;
    private final Category category;
    private final JournalLineType normalBalance;

    public AccountingAccount(
            @NonNull String code,
            @NonNull String name,
            @NonNull Category category,
            @NonNull JournalLineType normalBalance
    ) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.normalBalance = normalBalance;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Category getCategory() {
        return category;
    }

    @NonNull
    public JournalLineType getNormalBalance() {
        return normalBalance;
    }
}
