package com.example.kilgi;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.AccountingCatalog;
import com.example.kilgi.inventory.accounting.AccountingSummaryService;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.JournalEntryEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JournalActivity extends AppCompatActivity {

    public static final String EXTRA_LOT_ID = "com.example.kilgi.extra.LOT_ID";
    private static final int JOURNAL_PAGE_SIZE = 15;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final DateFormat dateTimeFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MaterialToolbar topAppBar;
    private ScrollView contentScrollView;
    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private TextView journalStatusView;
    private TextView journalPeriodSummaryView;
    private TextView chartOfAccountsView;
    private TextView journalEmptyStateView;
    private TableLayout journalEntriesTable;
    private LinearLayout tAccountsLayout;
    private TableLayout trialBalanceTable;
    private Button previousJournalPageButton;
    private Button nextJournalPageButton;
    private TextView journalPageSummaryView;
    private Button journalSortToggleButton;

    private ModuleOneRepository repository;
    private String initialLotId;
    private ArrayAdapter<String> monthAdapter;
    private ArrayAdapter<Integer> yearAdapter;
    private final List<Integer> yearOptions = new ArrayList<>();
    private boolean hasResumedOnce;
    private boolean journalSortAscending;
    private int currentJournalPageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_journal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialLotId = getIntent().getStringExtra(EXTRA_LOT_ID);
        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        bindViews();
        chartOfAccountsView.setText(buildChartOfAccountsText());
        setupTopAppBar();
        setupPeriodSpinners();
        bindActions();
        loadInitialJournal();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasResumedOnce) {
            loadSelectedJournal();
            return;
        }
        hasResumedOnce = true;
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        contentScrollView = findViewById(R.id.content_scroll);
        monthSpinner = findViewById(R.id.spinner_journal_month);
        yearSpinner = findViewById(R.id.spinner_journal_year);
        journalStatusView = findViewById(R.id.text_journal_status);
        journalPeriodSummaryView = findViewById(R.id.text_journal_period_summary);
        chartOfAccountsView = findViewById(R.id.text_chart_of_accounts);
        journalEmptyStateView = findViewById(R.id.text_journal_empty_state);
        journalEntriesTable = findViewById(R.id.table_journal_entries);
        tAccountsLayout = findViewById(R.id.layout_t_accounts);
        trialBalanceTable = findViewById(R.id.table_trial_balance);
        previousJournalPageButton = findViewById(R.id.button_previous_journal_page);
        nextJournalPageButton = findViewById(R.id.button_next_journal_page);
        journalPageSummaryView = findViewById(R.id.text_journal_page_summary);
    }

    private void setupTopAppBar() {
        topAppBar.setTitle(R.string.journal_screen_title);
        topAppBar.inflateMenu(R.menu.main_sections_menu);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_lot) {
                Intent intent = new Intent(this, MainActivity.class);
                if (!TextUtils.isEmpty(initialLotId)) {
                    intent.putExtra(EXTRA_LOT_ID, initialLotId);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == R.id.menu_journal) {
                contentScrollView.post(() -> contentScrollView.smoothScrollTo(0, 0));
                return true;
            }
            return false;
        });
    }

    private void setupPeriodSpinners() {
        monthAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildMonthOptions()
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        yearAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                yearOptions
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        Calendar calendar = Calendar.getInstance();
        monthSpinner.setSelection(calendar.get(Calendar.MONTH));
        updateYearOptions(calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR), calendar.get(Calendar.YEAR));
    }

    private List<String> buildMonthOptions() {
        List<String> options = new ArrayList<>();
        String[] monthNames = new DateFormatSymbols(Locale.getDefault()).getMonths();
        for (int month = 0; month < 12; month++) {
            options.add(monthNames[month]);
        }
        return options;
    }

    private void bindActions() {
        Button loadJournalButton = findViewById(R.id.button_load_journal);
        Button addAdjustmentButton = findViewById(R.id.button_add_journal_adjustment);
        
        loadJournalButton.setOnClickListener(v -> {
            currentJournalPageIndex = 0;
            loadSelectedJournal();
        });
        addAdjustmentButton.setOnClickListener(v -> showAddAdjustmentDialog());
        previousJournalPageButton.setOnClickListener(v -> loadJournalPage(currentJournalPageIndex - 1));
        nextJournalPageButton.setOnClickListener(v -> loadJournalPage(currentJournalPageIndex + 1));
    }

    private void loadJournalPage(int pageIndex) {
        int monthOfYear = monthSpinner.getSelectedItemPosition() + 1;
        Integer selectedYear = (Integer) yearSpinner.getSelectedItem();
        int year = selectedYear == null ? Calendar.getInstance().get(Calendar.YEAR) : selectedYear;
        ioExecutor.execute(() -> refreshJournalOnWorker(
                monthOfYear,
                year,
                null,
                getMinAvailableYear(),
                getMaxAvailableYear(),
                pageIndex
        ));
    }

    private void loadInitialJournal() {
        ioExecutor.execute(() -> {
            PeriodSelection selection = resolveInitialPeriodSelection();
            journalSortAscending = false;
            currentJournalPageIndex = 0;
            refreshJournalOnWorker(
                    selection.monthOfYear,
                    selection.year,
                    getString(R.string.journal_status_loaded, formatPeriodLabel(selection.monthOfYear, selection.year)),
                    selection.minYear,
                    selection.maxYear,
                    0
            );
        });
    }

    private void loadSelectedJournal() {
        int monthOfYear = monthSpinner.getSelectedItemPosition() + 1;
        Integer selectedYear = (Integer) yearSpinner.getSelectedItem();
        int year = selectedYear == null ? Calendar.getInstance().get(Calendar.YEAR) : selectedYear;
        ioExecutor.execute(() -> refreshJournalOnWorker(
                monthOfYear,
                year,
                getString(R.string.journal_status_loaded, formatPeriodLabel(monthOfYear, year)),
                getMinAvailableYear(),
                getMaxAvailableYear(),
                currentJournalPageIndex
        ));
    }

    private PeriodSelection resolveInitialPeriodSelection() {
        Calendar selectedCalendar = Calendar.getInstance();

        if (!TextUtils.isEmpty(initialLotId)) {
            try {
                LotEntity lot = repository.getLotWithDetails(initialLotId).lot;
                if (lot != null) {
                    selectedCalendar.setTimeInMillis(lot.timestamp);
                }
            } catch (Exception ignored) {
            }
        } else {
            LotEntity latestLot = repository.getLatestLot();
            if (latestLot != null) {
                selectedCalendar.setTimeInMillis(latestLot.timestamp);
            }
        }

        long oldestTimestamp = repository.getOldestJournalEntryTimestamp();
        long latestTimestamp = repository.getLatestJournalEntryTimestamp();
        Calendar oldestCalendar = Calendar.getInstance();
        oldestCalendar.setTimeInMillis(oldestTimestamp);
        Calendar latestCalendar = Calendar.getInstance();
        latestCalendar.setTimeInMillis(latestTimestamp);

        int selectedYear = selectedCalendar.get(Calendar.YEAR);
        int minYear = Math.min(oldestCalendar.get(Calendar.YEAR), selectedYear);
        int maxYear = Math.max(latestCalendar.get(Calendar.YEAR), selectedYear);
        return new PeriodSelection(
                selectedCalendar.get(Calendar.MONTH) + 1,
                selectedYear,
                minYear,
                maxYear
        );
    }

    private void refreshJournalOnWorker(int monthOfYear, int year, String statusMessage, int minYear, int maxYear, int requestedPageIndex) {
        try {
            List<JournalEntryWithLines> allEntries = repository.getJournalEntriesForPeriod(monthOfYear, year);
            List<JournalEntryWithLines> allEntriesUpTo = repository.getJournalEntriesUpTo(monthOfYear, year);
            
            allEntries.sort((a, b) -> {
                int cmp = Long.compare(a.entry.timestamp, b.entry.timestamp);
                return journalSortAscending ? cmp : -cmp;
            });

            int pageIndex = clampPageIndex(requestedPageIndex, allEntries.size());
            List<JournalEntryWithLines> pagedEntries = paginateJournalEntries(allEntries, pageIndex);
            List<JournalRow> rows = buildJournalRows(pagedEntries);

            List<AccountingSummaryService.TAccount> tAccounts = AccountingSummaryService.calculateTAccounts(allEntries);
            AccountingSummaryService.TrialBalance trialBalance = AccountingSummaryService.calculateTrialBalance(allEntriesUpTo);

            String periodLabel = formatPeriodLabel(monthOfYear, year);
            String summary = getString(R.string.journal_period_summary, periodLabel, allEntries.size(), allEntries.size());

            runOnUiThread(() -> {
                updateYearOptions(minYear, maxYear, year);
                monthSpinner.setSelection(monthOfYear - 1);
                currentJournalPageIndex = pageIndex;
                
                journalStatusView.setText(statusMessage == null
                        ? getString(R.string.journal_status_idle)
                        : statusMessage);
                journalPeriodSummaryView.setText(summary);
                
                updateJournalPaginationControls(pageIndex, allEntries.size());
                renderJournalTable(rows);
                updateSortToggleButton();
                renderTAccounts(tAccounts);
                renderTrialBalance(trialBalance);
            });
        } catch (Exception exception) {
            runOnUiThread(() -> {
                journalStatusView.setText(exception.getMessage());
                journalPeriodSummaryView.setText(R.string.journal_period_summary_placeholder);
                renderJournalTable(null);
                renderTAccounts(null);
                renderTrialBalance(null);
                updateJournalPaginationControls(0, 0);
            });
        }
    }

    private int clampPageIndex(int requestedPageIndex, int totalItems) {
        int totalPages = getTotalPages(totalItems);
        if (totalPages <= 0) return 0;
        return Math.max(0, Math.min(requestedPageIndex, totalPages - 1));
    }

    private int getTotalPages(int totalItems) {
        return totalItems <= 0 ? 0 : ((totalItems - 1) / JOURNAL_PAGE_SIZE) + 1;
    }

    private List<JournalEntryWithLines> paginateJournalEntries(List<JournalEntryWithLines> allEntries, int pageIndex) {
        if (allEntries.isEmpty()) return new ArrayList<>();
        int startIndex = pageIndex * JOURNAL_PAGE_SIZE;
        int endIndex = Math.min(allEntries.size(), startIndex + JOURNAL_PAGE_SIZE);
        return new ArrayList<>(allEntries.subList(startIndex, endIndex));
    }

    private void updateJournalPaginationControls(int pageIndex, int totalItems) {
        int totalPages = getTotalPages(totalItems);
        if (totalPages <= 0) {
            journalPageSummaryView.setText(R.string.journal_page_summary_empty);
            previousJournalPageButton.setEnabled(false);
            nextJournalPageButton.setEnabled(false);
            return;
        }

        journalPageSummaryView.setText(getString(R.string.journal_page_summary, pageIndex + 1, totalPages));
        previousJournalPageButton.setEnabled(pageIndex > 0);
        nextJournalPageButton.setEnabled(pageIndex < totalPages - 1);
    }

    private void updateSortToggleButton() {
        if (journalSortToggleButton == null) return;
        journalSortToggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, journalSortAscending
                ? R.drawable.outline_arrow_upward_alt_24
                : R.drawable.outline_arrow_downward_alt_24, 0);
    }

    private void renderJournalTable(List<JournalRow> rows) {
        journalEntriesTable.removeAllViews();
        journalEntriesTable.addView(createJournalHeaderRow());
        boolean hasEntries = rows != null && !rows.isEmpty();
        journalEmptyStateView.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
        if (!hasEntries) {
            return;
        }

        for (int index = 0; index < rows.size(); index++) {
            journalEntriesTable.addView(createJournalDataRow(rows.get(index), index));
        }
    }

    private TableRow createJournalHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(0xFFE0E0E0);
        row.addView(createJournalSortToggleButton(getString(R.string.journal_table_header_date)));
        row.addView(createJournalCell(getString(R.string.journal_table_header_account), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_debit), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_credit), true));
        return row;
    }

    private TableRow createJournalDataRow(JournalRow rowData, int index) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(index % 2 == 0 ? 0xFFF8F8F8 : 0xFFFFFFFF);
        row.addView(createJournalCell(rowData.dateTimeLabel, false));
        row.addView(createJournalCell(rowData.accountLabel, false));
        row.addView(createJournalCell(rowData.debitLabel, false));
        row.addView(createJournalCell(rowData.creditLabel, false));
        return row;
    }

    private Button createJournalSortToggleButton(String text) {
        Button button = new Button(this);
        button.setBackground(null);
        button.setElevation(0);
        button.setAllCaps(false);
        button.setTextColor(new TextView(this).getTextColors());
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);

        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        button.setLayoutParams(params);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setText(text);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        journalSortToggleButton = button;
        journalSortToggleButton.setOnClickListener(v -> {
            journalSortAscending = !journalSortAscending;
            currentJournalPageIndex = 0;
            updateSortToggleButton();
            loadSelectedJournal();
        });
        updateSortToggleButton();

        return button;
    }

    private void renderTAccounts(List<AccountingSummaryService.TAccount> tAccounts) {
        tAccountsLayout.removeAllViews();
        if (tAccounts == null || tAccounts.isEmpty()) {
            return;
        }

        for (AccountingSummaryService.TAccount account : tAccounts) {
            tAccountsLayout.addView(createTAccountCard(account));
        }
    }

    private View createTAccountCard(AccountingSummaryService.TAccount account) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(getString(R.string.journal_line_account, account.accountCode, account.accountName));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        card.addView(title);

        View hLine = new View(this);
        hLine.setBackgroundColor(0xFF000000);
        LinearLayout.LayoutParams hLineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2));
        hLineParams.setMargins(0, dp(8), 0, 0);
        card.addView(hLine, hLineParams);

        LinearLayout colsLayout = new LinearLayout(this);
        colsLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout debitCol = new LinearLayout(this);
        debitCol.setOrientation(LinearLayout.VERTICAL);
        debitCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        debitCol.setPadding(0, dp(4), dp(8), 0);
        
        TextView debitHeader = new TextView(this);
        debitHeader.setText(R.string.t_account_debit_label);
        debitHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        debitHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        debitCol.addView(debitHeader);

        for (JournalLineEntity line : account.debitLines) {
            TextView lineView = new TextView(this);
            lineView.setText(currencyFormat.format(line.amount));
            lineView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            debitCol.addView(lineView);
        }

        View vLine = new View(this);
        vLine.setBackgroundColor(0xFF000000);
        colsLayout.addView(debitCol);
        colsLayout.addView(vLine, new LinearLayout.LayoutParams(dp(2), LinearLayout.LayoutParams.MATCH_PARENT));

        LinearLayout creditCol = new LinearLayout(this);
        creditCol.setOrientation(LinearLayout.VERTICAL);
        creditCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        creditCol.setPadding(dp(8), dp(4), 0, 0);
        creditCol.setGravity(android.view.Gravity.END);

        TextView creditHeader = new TextView(this);
        creditHeader.setText(R.string.t_account_credit_label);
        creditHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        creditHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        creditCol.addView(creditHeader);

        for (JournalLineEntity line : account.creditLines) {
            TextView lineView = new TextView(this);
            lineView.setText(currencyFormat.format(line.amount));
            lineView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            creditCol.addView(lineView);
        }
        colsLayout.addView(creditCol);

        card.addView(colsLayout);

        View footerLine = new View(this);
        footerLine.setBackgroundColor(0xFFCCCCCC);
        card.addView(footerLine, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout summaryLayout = new LinearLayout(this);
        summaryLayout.setOrientation(LinearLayout.HORIZONTAL);
        summaryLayout.setPadding(0, dp(4), 0, 0);

        TextView debitTotal = new TextView(this);
        debitTotal.setText(currencyFormat.format(account.totalDebit));
        debitTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        debitTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        debitTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView creditTotal = new TextView(this);
        creditTotal.setText(currencyFormat.format(account.totalCredit));
        creditTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        creditTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        creditTotal.setGravity(android.view.Gravity.END);
        creditTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        summaryLayout.addView(debitTotal);
        summaryLayout.addView(creditTotal);
        card.addView(summaryLayout);

        double net = account.getNetBalance();
        TextView netView = new TextView(this);
        String direction = net >= 0 ? "(Dr)" : "(Cr)";
        netView.setText(getString(R.string.t_account_net_label, currencyFormat.format(Math.abs(net)), direction));
        netView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        netView.setTypeface(null, android.graphics.Typeface.BOLD);
        netView.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        netView.setPadding(0, dp(8), 0, 0);
        card.addView(netView);

        return card;
    }

    private void renderTrialBalance(AccountingSummaryService.TrialBalance trialBalance) {
        trialBalanceTable.removeAllViews();
        trialBalanceTable.addView(createTrialBalanceHeaderRow());
        if (trialBalance == null || trialBalance.entries.isEmpty()) {
            return;
        }

        for (int i = 0; i < trialBalance.entries.size(); i++) {
            trialBalanceTable.addView(createTrialBalanceDataRow(trialBalance.entries.get(i), i));
        }

        trialBalanceTable.addView(createTrialBalanceTotalsRow(trialBalance.totalDebits, trialBalance.totalCredits));
    }

    private void showAddAdjustmentDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_adjustment, null);
        Button dateButton = dialogView.findViewById(R.id.button_adjustment_date);
        Spinner debitSpinner = dialogView.findViewById(R.id.spinner_debit_account);
        Spinner creditSpinner = dialogView.findViewById(R.id.spinner_credit_account);
        EditText amountInput = dialogView.findViewById(R.id.edit_adjustment_amount);
        EditText memoInput = dialogView.findViewById(R.id.edit_adjustment_memo);

        final Calendar calendar = Calendar.getInstance();
        dateButton.setText(dateTimeFormat.format(calendar.getTimeInMillis()));
        dateButton.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                dateButton.setText(dateTimeFormat.format(calendar.getTimeInMillis()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        List<AccountingAccount> allAccounts = AccountingCatalog.getAllAccounts();
        ArrayAdapter<AccountingAccount> accountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allAccounts);
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        debitSpinner.setAdapter(accountAdapter);
        creditSpinner.setAdapter(accountAdapter);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_adjustment_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                AccountingAccount debit = (AccountingAccount) debitSpinner.getSelectedItem();
                AccountingAccount credit = (AccountingAccount) creditSpinner.getSelectedItem();
                double amount = InventoryInputParser.parseRequiredPositiveDouble(amountInput.getText().toString(), "Adjustment amount");
                String memo = InventoryInputParser.requireText(memoInput.getText().toString(), "Memo");

                if (debit.getCode().equals(credit.getCode())) {
                    throw new IllegalArgumentException("Debit and Credit accounts must be different.");
                }

                ioExecutor.execute(() -> {
                    try {
                        repository.postManualAdjustment(calendar.getTimeInMillis(), debit, credit, amount, memo);
                        runOnUiThread(() -> {
                            dialog.dismiss();
                            journalStatusView.setText(R.string.adjustment_posted_message);
                            loadSelectedJournal();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> journalStatusView.setText(e.getMessage()));
                    }
                });
            } catch (Exception e) {
                journalStatusView.setText(e.getMessage());
            }
        }));

        dialog.show();
    }

    private TableRow createTrialBalanceHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(0xFFE0E0E0);
        row.addView(createJournalCell(getString(R.string.trial_balance_header_account), true));
        row.addView(createJournalCell(getString(R.string.trial_balance_header_debit), true));
        row.addView(createJournalCell(getString(R.string.trial_balance_header_credit), true));
        return row;
    }

    private TableRow createTrialBalanceDataRow(AccountingSummaryService.TrialBalanceEntry entry, int index) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(index % 2 == 0 ? 0xFFF8F8F8 : 0xFFFFFFFF);
        row.addView(createJournalCell(entry.accountCode + " " + entry.accountName, false));
        row.addView(createJournalCell(entry.debit > 0 ? currencyFormat.format(entry.debit) : "-", false));
        row.addView(createJournalCell(entry.credit > 0 ? currencyFormat.format(entry.credit) : "-", false));
        return row;
    }

    private TableRow createTrialBalanceTotalsRow(double totalDebit, double totalCredit) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(0xFFEEEEEE);
        TextView totalLabel = createJournalCell(getString(R.string.trial_balance_totals), true);
        totalLabel.setGravity(android.view.Gravity.END);
        row.addView(totalLabel);
        row.addView(createJournalCell(currencyFormat.format(totalDebit), true));
        row.addView(createJournalCell(currencyFormat.format(totalCredit), true));
        return row;
    }

    private TextView createJournalCell(String text, boolean header) {
        TextView view = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        );
        view.setLayoutParams(params);
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, header ? 14 : 13);
        if (header) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private List<JournalRow> buildJournalRows(List<JournalEntryWithLines> entries) {
        List<JournalRow> rows = new ArrayList<>();
        if (entries == null) {
            return rows;
        }
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            JournalEntryWithLines entryWithLines = entries.get(entryIndex);
            JournalEntryEntity entry = entryWithLines.entry;
            if (entry == null) continue;
            
            if (entryWithLines.lines == null || entryWithLines.lines.isEmpty()) {
                rows.add(new JournalRow(
                        formatDate(entry.timestamp),
                        "-",
                        "-",
                        "-"
                ));
                continue;
            }
            for (int lineIndex = 0; lineIndex < entryWithLines.lines.size(); lineIndex++) {
                JournalLineEntity line = entryWithLines.lines.get(lineIndex);
                boolean isDebit = isDebitLine(line);
                rows.add(new JournalRow(
                        formatDate(entry.timestamp),
                        getString(R.string.journal_line_account, line.accountCode, resolveAccountName(line)),
                        isDebit ? currencyFormat.format(line.amount) : "-",
                        isDebit ? "-" : currencyFormat.format(line.amount)
                ));
            }
        }
        return rows;
    }

    private String formatDate(long timestamp) {
        return dateTimeFormat.format(timestamp)
                .replaceAll(",?\\s*\\b\\d{4}\\b", "")
                .replaceAll("\\b\\d{4}\\b\\s*\\.?", "")
                .trim();
    }

    private boolean isDebitLine(JournalLineEntity line) {
        try {
            return JournalLineType.valueOf(line.lineType) == JournalLineType.DEBIT;
        } catch (Exception exception) {
            return false;
        }
    }

    private String resolveAccountName(JournalLineEntity line) {
        AccountingAccount account = AccountingCatalog.findByCode(line.accountCode);
        return account != null ? account.getName() : line.accountName;
    }

    private String buildChartOfAccountsText() {
        StringBuilder builder = new StringBuilder();
        for (AccountingAccount.Category category : AccountingAccount.Category.values()) {
            appendCategorySection(builder, category);
        }
        return builder.toString();
    }

    private void appendCategorySection(StringBuilder builder, AccountingAccount.Category category) {
        List<AccountingAccount> accounts = AccountingCatalog.getAccountsByCategory(category);
        if (accounts.isEmpty()) return;
        
        if (builder.length() > 0) builder.append("\n\n");
        
        builder.append(getString(R.string.journal_chart_category, category.getDisplayName()));
        for (AccountingAccount account : accounts) {
            builder.append("\n");
            builder.append(getString(
                    R.string.journal_chart_account_line,
                    account.getCode(),
                    account.getName(),
                    getNormalBalanceLabel(account.getNormalBalance())
            ));
        }
    }

    private String getNormalBalanceLabel(JournalLineType normalBalance) {
        return normalBalance == JournalLineType.DEBIT
                ? getString(R.string.journal_chart_normal_debit)
                : getString(R.string.journal_chart_normal_credit);
    }

    private void updateYearOptions(int minYear, int maxYear, int selectedYear) {
        int safeMinYear = Math.min(minYear, selectedYear);
        int safeMaxYear = Math.max(maxYear, selectedYear);

        List<Integer> updatedYears = new ArrayList<>();
        for (int year = safeMaxYear; year >= safeMinYear; year--) {
            updatedYears.add(year);
        }

        if (!updatedYears.equals(yearOptions)) {
            yearOptions.clear();
            yearOptions.addAll(updatedYears);
            yearAdapter.notifyDataSetChanged();
        }

        int selectedIndex = yearOptions.indexOf(selectedYear);
        if (selectedIndex >= 0) {
            yearSpinner.setSelection(selectedIndex);
        }
    }

    private int getMinAvailableYear() {
        return yearOptions.isEmpty() ? Calendar.getInstance().get(Calendar.YEAR) : yearOptions.get(yearOptions.size() - 1);
    }

    private int getMaxAvailableYear() {
        return yearOptions.isEmpty() ? Calendar.getInstance().get(Calendar.YEAR) : yearOptions.get(0);
    }

    private String formatPeriodLabel(int monthOfYear, int year) {
        String[] monthNames = new DateFormatSymbols(Locale.getDefault()).getMonths();
        String monthLabel = monthNames[Math.max(0, Math.min(11, monthOfYear - 1))];
        return monthLabel + " " + year;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private static final class PeriodSelection {
        private final int monthOfYear;
        private final int year;
        private final int minYear;
        private final int maxYear;

        private PeriodSelection(int monthOfYear, int year, int minYear, int maxYear) {
            this.monthOfYear = monthOfYear;
            this.year = year;
            this.minYear = minYear;
            this.maxYear = maxYear;
        }
    }

    private static final class JournalRow {
        private final String dateTimeLabel;
        private final String accountLabel;
        private final String debitLabel;
        private final String creditLabel;

        private JournalRow(
                String dateTimeLabel,
                String accountLabel,
                String debitLabel,
                String creditLabel
        ) {
            this.dateTimeLabel = dateTimeLabel;
            this.accountLabel = accountLabel;
            this.debitLabel = debitLabel;
            this.creditLabel = creditLabel;
        }
    }
}
