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

    public static class IncomeStatement {
        public final Map<String, Double> revenues = new TreeMap<>();
        public final Map<String, Double> expenses = new TreeMap<>();
        public double totalRevenue = 0;
        public double totalExpense = 0;

        public double getNetIncome() {
            return totalRevenue - totalExpense;
        }
    }

    public static class EquityStatement {
        public double ownerCapital = 0;
        public double priorRetainedEarnings = 0;
        public double currentNetIncome = 0;
        public double drawings = 0;

        public double getTotalEquity() {
            return ownerCapital + priorRetainedEarnings + currentNetIncome - drawings;
        }
    }

    public static class BalanceSheet {
        public final Map<String, Double> currentAssets = new TreeMap<>();
        public final Map<String, Double> nonCurrentAssets = new TreeMap<>();
        public final Map<String, Double> currentLiabilities = new TreeMap<>();
        public double totalAssets = 0;
        public double totalLiabilities = 0;
        public EquityStatement equity;
    }

    public static IncomeStatement calculateIncomeStatement(List<JournalEntryWithLines> periodEntries) {
        IncomeStatement is = new IncomeStatement();
        for (JournalEntryWithLines entry : periodEntries) {
            for (JournalLineEntity line : entry.lines) {
                AccountingAccount account = AccountingCatalog.findByCode(line.accountCode);
                if (account == null) continue;

                if (account.getCategory() == AccountingAccount.Category.REVENUE) {
                    double val = line.amount;
                    if (JournalLineType.DEBIT.name().equals(line.lineType)) val = -val;
                    is.revenues.put(account.getName(), is.revenues.getOrDefault(account.getName(), 0.0) + val);
                } else if (account.getCategory() == AccountingAccount.Category.EXPENSE || account.getCategory() == AccountingAccount.Category.COST) {
                    double val = line.amount;
                    if (JournalLineType.CREDIT.name().equals(line.lineType)) val = -val;
                    is.expenses.put(account.getName(), is.expenses.getOrDefault(account.getName(), 0.0) + val);
                }
            }
        }
        for (double val : is.revenues.values()) is.totalRevenue += val;
        for (double val : is.expenses.values()) is.totalExpense += val;
        return is;
    }

    public static EquityStatement calculateEquityStatement(
            List<JournalEntryWithLines> allEntriesUpTo,
            long periodStartTimestamp
    ) {
        EquityStatement es = new EquityStatement();
        
        for (JournalEntryWithLines entry : allEntriesUpTo) {
            boolean isPrior = entry.entry.timestamp < periodStartTimestamp;
            
            for (JournalLineEntity line : entry.lines) {
                AccountingAccount account = AccountingCatalog.findByCode(line.accountCode);
                if (account == null) continue;

                double amount = line.amount;
                boolean isDebit = JournalLineType.DEBIT.name().equals(line.lineType);

                if (account.getCategory() == AccountingAccount.Category.REVENUE) {
                    double net = isDebit ? -amount : amount;
                    if (isPrior) es.priorRetainedEarnings += net;
                    else es.currentNetIncome += net;
                } else if (account.getCategory() == AccountingAccount.Category.EXPENSE || account.getCategory() == AccountingAccount.Category.COST) {
                    double net = isDebit ? amount : -amount;
                    if (isPrior) es.priorRetainedEarnings -= net;
                    else es.currentNetIncome -= net;
                } else if (account.getCategory() == AccountingAccount.Category.OWNER_EQUITY) {
                    if (line.accountCode.equals(AccountingCatalog.CAPITAL.getCode())) {
                        double net = isDebit ? -amount : amount;
                        es.ownerCapital += net;
                    } else if (line.accountCode.equals(AccountingCatalog.DRAWING.getCode())) {
                        double net = isDebit ? amount : -amount;
                        es.drawings += net;
                    }
                }
            }
        }
        
        return es;
    }

    public static BalanceSheet calculateBalanceSheet(List<JournalEntryWithLines> allEntriesUpTo, EquityStatement equity) {
        BalanceSheet bs = new BalanceSheet();
        bs.equity = equity;

        for (JournalEntryWithLines entry : allEntriesUpTo) {
            for (JournalLineEntity line : entry.lines) {
                AccountingAccount account = AccountingCatalog.findByCode(line.accountCode);
                if (account == null) continue;

                double amount = line.amount;
                boolean isDebit = JournalLineType.DEBIT.name().equals(line.lineType);

                if (account.getCategory() == AccountingAccount.Category.ASSET) {
                    double net = isDebit ? amount : -amount;
                    if (line.accountCode.startsWith("11")) { // Simple rule: 11xxx are non-current
                        bs.nonCurrentAssets.put(account.getName(), bs.nonCurrentAssets.getOrDefault(account.getName(), 0.0) + net);
                    } else {
                        bs.currentAssets.put(account.getName(), bs.currentAssets.getOrDefault(account.getName(), 0.0) + net);
                    }
                } else if (account.getCategory() == AccountingAccount.Category.LIABILITY) {
                    double net = isDebit ? -amount : amount;
                    bs.currentLiabilities.put(account.getName(), bs.currentLiabilities.getOrDefault(account.getName(), 0.0) + net);
                }
            }
        }

        for (double v : bs.currentAssets.values()) bs.totalAssets += v;
        for (double v : bs.nonCurrentAssets.values()) bs.totalAssets += v;
        for (double v : bs.currentLiabilities.values()) bs.totalLiabilities += v;

        return bs;
    }
}
