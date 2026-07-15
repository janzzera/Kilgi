package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.AccountingCatalog;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.JournalEntryEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.appbar.MaterialToolbar;

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

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
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

    private ModuleOneRepository repository;
    private String initialLotId;
    private ArrayAdapter<String> monthAdapter;
    private ArrayAdapter<Integer> yearAdapter;
    private final List<Integer> yearOptions = new ArrayList<>();

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
        loadJournalButton.setOnClickListener(v -> loadSelectedJournal());
    }

    private void loadInitialJournal() {
        ioExecutor.execute(() -> {
            PeriodSelection selection = resolveInitialPeriodSelection();
            refreshJournalOnWorker(
                    selection.monthOfYear,
                    selection.year,
                    getString(R.string.journal_status_loaded, formatPeriodLabel(selection.monthOfYear, selection.year)),
                    selection.minYear,
                    selection.maxYear
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
                getMaxAvailableYear()
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
                // Fall back to the latest recorded period or the current date.
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

    private void refreshJournalOnWorker(int monthOfYear, int year, String statusMessage, int minYear, int maxYear) {
        try {
            List<JournalEntryWithLines> entries = repository.getJournalEntriesForPeriod(monthOfYear, year);
            List<JournalRow> rows = buildJournalRows(entries);
            String periodLabel = formatPeriodLabel(monthOfYear, year);
            String summary = getString(R.string.journal_period_summary, periodLabel, entries.size(), rows.size());

            runOnUiThread(() -> {
                updateYearOptions(minYear, maxYear, year);
                monthSpinner.setSelection(monthOfYear - 1);
                journalStatusView.setText(statusMessage == null
                        ? getString(R.string.journal_status_idle)
                        : statusMessage);
                journalPeriodSummaryView.setText(summary);
                renderJournalTable(rows);
            });
        } catch (Exception exception) {
            runOnUiThread(() -> {
                journalStatusView.setText(exception.getMessage());
                journalPeriodSummaryView.setText(R.string.journal_period_summary_placeholder);
                renderJournalTable(null);
            });
        }
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
        row.addView(createJournalCell(getString(R.string.journal_table_header_date), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_lot), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_event), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_account), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_debit), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_credit), true));
        row.addView(createJournalCell(getString(R.string.journal_table_header_details), true));
        return row;
    }

    private TableRow createJournalDataRow(JournalRow rowData, int index) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(index % 2 == 0 ? 0xFFF8F8F8 : 0xFFFFFFFF);
        row.addView(createJournalCell(rowData.dateTimeLabel, false));
        row.addView(createJournalCell(rowData.lotLabel, false));
        row.addView(createJournalCell(rowData.eventLabel, false));
        row.addView(createJournalCell(rowData.accountLabel, false));
        row.addView(createJournalCell(rowData.debitLabel, false));
        row.addView(createJournalCell(rowData.creditLabel, false));
        row.addView(createJournalCell(rowData.detailsLabel, false));
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
            if (entry == null) {
                continue;
            }
            if (entryWithLines.lines == null || entryWithLines.lines.isEmpty()) {
                rows.add(new JournalRow(
                        dateTimeFormat.format(entry.timestamp),
                        abbreviateLotId(entry.lotId),
                        entry.eventType,
                        "-",
                        "-",
                        "-",
                        entry.description
                ));
                continue;
            }
            for (int lineIndex = 0; lineIndex < entryWithLines.lines.size(); lineIndex++) {
                JournalLineEntity line = entryWithLines.lines.get(lineIndex);
                boolean isDebit = isDebitLine(line);
                rows.add(new JournalRow(
                        dateTimeFormat.format(entry.timestamp),
                        abbreviateLotId(entry.lotId),
                        entry.eventType,
                        getString(R.string.journal_line_account, line.accountCode, resolveAccountName(line)),
                        isDebit ? currencyFormat.format(line.amount) : "-",
                        isDebit ? "-" : currencyFormat.format(line.amount),
                        buildRowDetails(entry, line)
                ));
            }
        }
        return rows;
    }

    private String buildRowDetails(JournalEntryEntity entry, JournalLineEntity line) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(entry.description)) {
            builder.append(entry.description.trim());
        }
        String lineDetails = buildLineDetails(line);
        if (!TextUtils.isEmpty(lineDetails)) {
            appendSeparator(builder);
            builder.append(lineDetails);
        }
        return builder.toString();
    }

    private String buildLineDetails(JournalLineEntity line) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(line.memo)) {
            builder.append(line.memo.trim());
        }
        if (!TextUtils.isEmpty(line.paymentSource)) {
            appendSeparator(builder);
            builder.append(getString(R.string.journal_line_payment_source, line.paymentSource));
        }
        if (!TextUtils.isEmpty(line.providerId)) {
            appendSeparator(builder);
            builder.append(getString(R.string.journal_line_provider, line.providerId));
        }
        return builder.toString();
    }

    private boolean isDebitLine(JournalLineEntity line) {
        try {
            return JournalLineType.valueOf(line.lineType) == JournalLineType.DEBIT;
        } catch (IllegalArgumentException | NullPointerException exception) {
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
        if (accounts.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
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

    private void appendSeparator(StringBuilder builder) {
        if (builder.length() > 0) {
            builder.append(" • ");
        }
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

    private String abbreviateLotId(String lotId) {
        if (TextUtils.isEmpty(lotId)) {
            return "-";
        }
        return lotId.length() <= 8 ? lotId : lotId.substring(0, 8);
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
        private final String lotLabel;
        private final String eventLabel;
        private final String accountLabel;
        private final String debitLabel;
        private final String creditLabel;
        private final String detailsLabel;

        private JournalRow(
                String dateTimeLabel,
                String lotLabel,
                String eventLabel,
                String accountLabel,
                String debitLabel,
                String creditLabel,
                String detailsLabel
        ) {
            this.dateTimeLabel = dateTimeLabel;
            this.lotLabel = lotLabel;
            this.eventLabel = eventLabel;
            this.accountLabel = accountLabel;
            this.debitLabel = debitLabel;
            this.creditLabel = creditLabel;
            this.detailsLabel = detailsLabel;
        }
    }
}

