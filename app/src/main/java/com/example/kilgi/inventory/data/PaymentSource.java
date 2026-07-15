package com.example.kilgi.inventory.data;

import androidx.annotation.NonNull;

import com.example.kilgi.inventory.accounting.AccountingCatalog;

/**
 * Source account used to settle a procurement-related obligation.
 */
public enum PaymentSource {
    CASH("CASH", AccountingCatalog.CASH_CODE, AccountingCatalog.CASH_NAME),
    ACCOUNTS_PAYABLE("AP", AccountingCatalog.ACCOUNTS_PAYABLE_CODE, AccountingCatalog.ACCOUNTS_PAYABLE_NAME);

    private final String storedValue;
    private final String accountCode;
    private final String accountName;

    PaymentSource(String storedValue, String accountCode, String accountName) {
        this.storedValue = storedValue;
        this.accountCode = accountCode;
        this.accountName = accountName;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public static PaymentSource fromStoredValue(String value) {
        for (PaymentSource source : values()) {
            if (source.storedValue.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unsupported payment source: " + value);
    }

    @Override
    @NonNull
    public String toString() {
        return storedValue;
    }
}

