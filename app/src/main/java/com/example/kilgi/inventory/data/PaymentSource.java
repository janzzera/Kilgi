package com.example.kilgi.inventory.data;

/**
 * Source account used to settle a procurement-related obligation.
 */
public enum PaymentSource {
    CASH("CASH", "10100", "Cash / Mobile Wallet"),
    ACCOUNTS_PAYABLE("AP", "20100", "Accounts Payable - Providers");

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
    public String toString() {
        return storedValue;
    }
}

