package com.example.kilgi.inventory.accounting;

import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AccountingSummaryService {

    public static List<TAccount> calculateTAccounts(List<JournalEntryWithLines> periodEntries) {
        Map<String, TAccount> accountMap = new TreeMap<>(); // Sorted by account code

        for (JournalEntryWithLines entryWithLines : periodEntries) {
            for (JournalLineEntity line : entryWithLines.lines) {
                TAccount account = accountMap.get(line.accountCode);
                if (account == null) {
                    account = new TAccount(line.accountCode, line.accountName);
                    accountMap.put(line.accountCode, account);
                }

                if (JournalLineType.DEBIT.name().equals(line.lineType)) {
                    account.debitLines.add(line);
                    account.totalDebit += line.amount;
                } else {
                    account.creditLines.add(line);
                    account.totalCredit += line.amount;
                }
            }
        }

        return new ArrayList<>(accountMap.values());
    }

    public static TrialBalance calculateTrialBalance(List<JournalEntryWithLines> allEntriesUpToNow) {
        Map<String, AccountBalance> balanceMap = new TreeMap<>();

        for (JournalEntryWithLines entryWithLines : allEntriesUpToNow) {
            for (JournalLineEntity line : entryWithLines.lines) {
                AccountBalance balance = balanceMap.get(line.accountCode);
                if (balance == null) {
                    AccountingAccount accountInfo = AccountingCatalog.findByCode(line.accountCode);
                    JournalLineType normalBalance = accountInfo != null ? accountInfo.getNormalBalance() : JournalLineType.DEBIT;
                    balance = new AccountBalance(line.accountCode, line.accountName, normalBalance);
                    balanceMap.put(line.accountCode, balance);
                }

                if (JournalLineType.DEBIT.name().equals(line.lineType)) {
                    balance.currentBalance += line.amount;
                } else {
                    balance.currentBalance -= line.amount;
                }
            }
        }

        List<TrialBalanceEntry> tbEntries = new ArrayList<>();
        double totalDebits = 0;
        double totalCredits = 0;

        for (AccountBalance balance : balanceMap.values()) {
            double amount = balance.currentBalance;
            if (balance.normalBalance == JournalLineType.CREDIT) {
                amount = -amount;
            }

            if (amount == 0) continue;

            double debit = 0;
            double credit = 0;

            // In accounting, even if normal balance is Credit, if it ends up negative it shows as Debit
            // But usually we just show where it lands.
            // Let's simplify: if net balance is positive relative to its normal balance, put it in its normal column.
            // Actually, trial balance shows net debit or net credit.
            
            double netValue = balance.currentBalance; // This is always Debit - Credit
            if (netValue > 0) {
                debit = netValue;
                totalDebits += debit;
            } else if (netValue < 0) {
                credit = -netValue;
                totalCredits += credit;
            }
            
            if (debit != 0 || credit != 0) {
                tbEntries.add(new TrialBalanceEntry(balance.accountCode, balance.accountName, debit, credit));
            }
        }

        return new TrialBalance(tbEntries, totalDebits, totalCredits);
    }

    public static class TAccount {
        public final String accountCode;
        public final String accountName;
        public final List<JournalLineEntity> debitLines = new ArrayList<>();
        public final List<JournalLineEntity> creditLines = new ArrayList<>();
        public double totalDebit = 0;
        public double totalCredit = 0;

        public TAccount(String accountCode, String accountName) {
            this.accountCode = accountCode;
            this.accountName = accountName;
        }

        public double getNetBalance() {
            return totalDebit - totalCredit;
        }
    }

    private static class AccountBalance {
        public final String accountCode;
        public final String accountName;
        public final JournalLineType normalBalance;
        public double currentBalance = 0; // Cumulative Debit - Credit

        public AccountBalance(String accountCode, String accountName, JournalLineType normalBalance) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.normalBalance = normalBalance;
        }
    }

    public static class TrialBalance {
        public final List<TrialBalanceEntry> entries;
        public final double totalDebits;
        public final double totalCredits;

        public TrialBalance(List<TrialBalanceEntry> entries, double totalDebits, double totalCredits) {
            this.entries = entries;
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
        }
    }

    public static class TrialBalanceEntry {
        public final String accountCode;
        public final String accountName;
        public final double debit;
        public final double credit;

        public TrialBalanceEntry(String accountCode, String accountName, double debit, double credit) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.debit = debit;
            this.credit = credit;
        }
    }
}
