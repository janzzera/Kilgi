package com.example.kilgi.inventory.accounting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.kilgi.inventory.data.JournalLineType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AccountingCatalog {

    public static final AccountingAccount CASH_IN_BANK = account(
            "10100",
            "Cash in Bank",
            AccountingAccount.Category.ASSET,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount ACCOUNTS_RECEIVABLE = account(
            "10300",
            "Accounts Receivable",
            AccountingAccount.Category.ASSET,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount OFFICE_EQUIPMENT = account(
            "11500",
            "Office Equipment",
            AccountingAccount.Category.ASSET,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount INVENTORY = account(
            "12000",
            "Merchandise Inventory - Vegetables",
            AccountingAccount.Category.ASSET,
            JournalLineType.DEBIT
    );

    public static final AccountingAccount ACCOUNTS_PAYABLE = account(
            "20100",
            "Accounts Payable",
            AccountingAccount.Category.LIABILITY,
            JournalLineType.CREDIT
    );
    public static final AccountingAccount NOTES_PAYABLE = account(
            "20200",
            "Notes Payable",
            AccountingAccount.Category.LIABILITY,
            JournalLineType.CREDIT
    );

    public static final AccountingAccount CAPITAL = account(
            "30100",
            "Capital",
            AccountingAccount.Category.OWNER_EQUITY,
            JournalLineType.CREDIT
    );
    public static final AccountingAccount DRAWING = account(
            "30200",
            "Drawing",
            AccountingAccount.Category.OWNER_EQUITY,
            JournalLineType.DEBIT
    );

    public static final AccountingAccount SALES = account(
            "40100",
            "Sales",
            AccountingAccount.Category.REVENUE,
            JournalLineType.CREDIT
    );
    public static final AccountingAccount SALES_DISCOUNT = account(
            "40200",
            "Sales Discount",
            AccountingAccount.Category.REVENUE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount SALES_RETURNS_AND_ALLOWANCES = account(
            "40300",
            "Sales Returns and Allowances",
            AccountingAccount.Category.REVENUE,
            JournalLineType.DEBIT
    );

    public static final AccountingAccount PURCHASES = account(
            "50100",
            "Purchases",
            AccountingAccount.Category.COST,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount FREIGHT_IN = account(
            "50200",
            "Freight-In",
            AccountingAccount.Category.COST,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount PURCHASE_DISCOUNT = account(
            "50300",
            "Purchase Discount",
            AccountingAccount.Category.COST,
            JournalLineType.CREDIT
    );
    public static final AccountingAccount PURCHASE_RETURNS_AND_ALLOWANCES = account(
            "50400",
            "Purchase Returns and Allowances",
            AccountingAccount.Category.COST,
            JournalLineType.CREDIT
    );
    public static final AccountingAccount COGS = account(
            "50500",
            "Cost of Goods Sold (COGS)",
            AccountingAccount.Category.COST,
            JournalLineType.DEBIT
    );

    public static final AccountingAccount TAXES_AND_LICENSES_EXPENSE = account(
            "60100",
            "Taxes and Licenses Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount FREIGHT_OUT = account(
            "60200",
            "Freight-Out",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount RENT_EXPENSE = account(
            "60300",
            "Rent Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount UTILITIES_EXPENSE = account(
            "60400",
            "Utilities Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount ADVERTISING_EXPENSE = account(
            "60500",
            "Advertising Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount SALARIES_EXPENSE = account(
            "60600",
            "Salaries Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount SUPPLIES_EXPENSE = account(
            "60700",
            "Supplies Expense",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );
    public static final AccountingAccount INVENTORY_LOSS = account(
            "60800",
            "Inventory Loss - Spoilage/Waste",
            AccountingAccount.Category.EXPENSE,
            JournalLineType.DEBIT
    );

    public static final String CASH_CODE = CASH_IN_BANK.getCode();
    public static final String CASH_NAME = CASH_IN_BANK.getName();
    public static final String ACCOUNTS_RECEIVABLE_CODE = ACCOUNTS_RECEIVABLE.getCode();
    public static final String ACCOUNTS_RECEIVABLE_NAME = ACCOUNTS_RECEIVABLE.getName();
    public static final String INVENTORY_CODE = INVENTORY.getCode();
    public static final String INVENTORY_NAME = INVENTORY.getName();
    public static final String PURCHASES_CODE = PURCHASES.getCode();
    public static final String PURCHASES_NAME = PURCHASES.getName();
    public static final String ACCOUNTS_PAYABLE_CODE = ACCOUNTS_PAYABLE.getCode();
    public static final String ACCOUNTS_PAYABLE_NAME = ACCOUNTS_PAYABLE.getName();
    public static final String SALES_CODE = SALES.getCode();
    public static final String SALES_NAME = SALES.getName();
    public static final String COGS_CODE = COGS.getCode();
    public static final String COGS_NAME = COGS.getName();
    public static final String FREIGHT_IN_CODE = FREIGHT_IN.getCode();
    public static final String FREIGHT_IN_NAME = FREIGHT_IN.getName();
    public static final String LOSS_CODE = INVENTORY_LOSS.getCode();
    public static final String LOSS_NAME = INVENTORY_LOSS.getName();

    private static final List<AccountingAccount> LOT_EXPENSE_ACCOUNTS = Collections.unmodifiableList(Arrays.asList(
            FREIGHT_IN,
            TAXES_AND_LICENSES_EXPENSE,
            FREIGHT_OUT,
            RENT_EXPENSE,
            UTILITIES_EXPENSE,
            ADVERTISING_EXPENSE,
            SALARIES_EXPENSE,
            SUPPLIES_EXPENSE
    ));

    private static final List<AccountingAccount> ALL_ACCOUNTS = Collections.unmodifiableList(Arrays.asList(
            CASH_IN_BANK,
            ACCOUNTS_RECEIVABLE,
            OFFICE_EQUIPMENT,
            INVENTORY,
            ACCOUNTS_PAYABLE,
            NOTES_PAYABLE,
            CAPITAL,
            DRAWING,
            SALES,
            SALES_DISCOUNT,
            SALES_RETURNS_AND_ALLOWANCES,
            PURCHASES,
            FREIGHT_IN,
            PURCHASE_DISCOUNT,
            PURCHASE_RETURNS_AND_ALLOWANCES,
            COGS,
            TAXES_AND_LICENSES_EXPENSE,
            FREIGHT_OUT,
            RENT_EXPENSE,
            UTILITIES_EXPENSE,
            ADVERTISING_EXPENSE,
            SALARIES_EXPENSE,
            SUPPLIES_EXPENSE,
            INVENTORY_LOSS
    ));

    private static final Map<String, AccountingAccount> ACCOUNTS_BY_CODE = buildAccountsByCode();

    private AccountingCatalog() {
    }

    @NonNull
    public static List<AccountingAccount> getAllAccounts() {
        return ALL_ACCOUNTS;
    }

    @NonNull
    public static List<AccountingAccount> getAccountsByCategory(@NonNull AccountingAccount.Category category) {
        List<AccountingAccount> matches = new ArrayList<>();
        for (AccountingAccount account : ALL_ACCOUNTS) {
            if (account.getCategory() == category) {
                matches.add(account);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    @NonNull
    public static List<AccountingAccount> getLotExpenseAccounts() {
        return LOT_EXPENSE_ACCOUNTS;
    }

    @Nullable
    public static AccountingAccount findByCode(String accountCode) {
        if (accountCode == null) {
            return null;
        }
        return ACCOUNTS_BY_CODE.get(accountCode.trim());
    }

    @NonNull
    public static AccountingAccount requireByCode(String accountCode) {
        AccountingAccount account = findByCode(accountCode);
        if (account == null) {
            throw new IllegalArgumentException("Unsupported account code: " + accountCode);
        }
        return account;
    }

    private static AccountingAccount account(
            String code,
            String name,
            AccountingAccount.Category category,
            JournalLineType normalBalance
    ) {
        return new AccountingAccount(code, name, category, normalBalance);
    }

    private static Map<String, AccountingAccount> buildAccountsByCode() {
        Map<String, AccountingAccount> accountsByCode = new LinkedHashMap<>();
        for (AccountingAccount account : ALL_ACCOUNTS) {
            accountsByCode.put(account.getCode(), account);
        }
        return Collections.unmodifiableMap(accountsByCode);
    }
}

