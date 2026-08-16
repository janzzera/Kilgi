package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.accounting.AccountingSummaryService;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private Spinner monthSpinner;
    private Spinner yearSpinner;
    private TextView statusView;
    private TextView isPeriodView;
    private TextView bsDateView;
    private TableLayout isTable;
    private TableLayout esTable;
    private TableLayout bsTable;
    private BottomNavigationView bottomNavigationView;

    private ModuleOneRepository repository;
    private final List<Integer> yearOptions = new ArrayList<>();
    private ArrayAdapter<Integer> yearAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        bindViews();
        setupNavigation();
        setupPeriodSpinners();
        
        findViewById(R.id.button_load_reports).setOnClickListener(v -> loadReports());
        
        loadInitialReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MainActivity.isUserAuthenticated) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        bottomNavigationView.setSelectedItemId(R.id.nav_reports);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void bindViews() {
        monthSpinner = findViewById(R.id.spinner_report_month);
        yearSpinner = findViewById(R.id.spinner_report_year);
        statusView = findViewById(R.id.text_reports_status);
        isPeriodView = findViewById(R.id.text_income_statement_period);
        bsDateView = findViewById(R.id.text_balance_sheet_date);
        isTable = findViewById(R.id.table_income_statement);
        esTable = findViewById(R.id.table_equity_statement);
        bsTable = findViewById(R.id.table_balance_sheet);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_inventory) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            } else if (itemId == R.id.nav_sales) {
                startActivity(new Intent(this, SalesActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            } else if (itemId == R.id.nav_journal) {
                startActivity(new Intent(this, JournalActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            } else if (itemId == R.id.nav_reports) {
                return true;
            }
            return false;
        });
    }

    private void setupPeriodSpinners() {
        String[] monthNames = new DateFormatSymbols(Locale.getDefault()).getMonths();
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, monthNames);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearOptions);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        Calendar cal = Calendar.getInstance();
        monthSpinner.setSelection(cal.get(Calendar.MONTH));
        updateYearOptions(cal.get(Calendar.YEAR), cal.get(Calendar.YEAR), cal.get(Calendar.YEAR));
    }

    private void updateYearOptions(int min, int max, int selected) {
        int sMin = Math.min(min, selected);
        int sMax = Math.max(max, selected);
        List<Integer> years = new ArrayList<>();
        for (int y = sMax; y >= sMin; y--) years.add(y);
        if (!years.equals(yearOptions)) {
            yearOptions.clear();
            yearOptions.addAll(years);
            yearAdapter.notifyDataSetChanged();
        }
        int idx = yearOptions.indexOf(selected);
        if (idx >= 0) yearSpinner.setSelection(idx);
    }

    private void loadInitialReports() {
        ioExecutor.execute(() -> {
            long oldest = repository.getOldestJournalEntryTimestamp();
            long latest = repository.getLatestJournalEntryTimestamp();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(latest);
            final int m = c.get(Calendar.MONTH) + 1;
            final int y = c.get(Calendar.YEAR);
            
            Calendar oc = Calendar.getInstance();
            oc.setTimeInMillis(oldest);
            final int minYear = oc.get(Calendar.YEAR);

            runOnUiThread(() -> {
                updateYearOptions(minYear, y, y);
                monthSpinner.setSelection(m - 1);
                loadReports();
            });
        });
    }

    private void loadReports() {
        int month = monthSpinner.getSelectedItemPosition() + 1;
        Integer yr = (Integer) yearSpinner.getSelectedItem();
        if (yr == null) yr = Calendar.getInstance().get(Calendar.YEAR);
        final int finalYear = yr;

        ioExecutor.execute(() -> {
            try {
                List<JournalEntryWithLines> periodEntries = repository.getJournalEntriesForPeriod(month, finalYear);
                List<JournalEntryWithLines> allUpTo = repository.getJournalEntriesUpTo(month, finalYear);

                AccountingSummaryService.IncomeStatement is = AccountingSummaryService.calculateIncomeStatement(periodEntries);
                AccountingSummaryService.EquityStatement es = AccountingSummaryService.calculateEquityStatement(allUpTo, is.getNetIncome());
                AccountingSummaryService.BalanceSheet bs = AccountingSummaryService.calculateBalanceSheet(allUpTo, es.getEndingCapital());

                String periodLabel = formatPeriodLabel(month, finalYear);

                runOnUiThread(() -> {
                    statusView.setText("Reports generated for " + periodLabel);
                    isPeriodView.setText(getString(R.string.income_statement_period, periodLabel));
                    bsDateView.setText(getString(R.string.balance_sheet_as_of, periodLabel));
                    renderIncomeStatement(is);
                    renderEquityStatement(es);
                    renderBalanceSheet(bs);
                });
            } catch (Exception e) {
                runOnUiThread(() -> statusView.setText(e.getMessage()));
            }
        });
    }

    private void renderIncomeStatement(AccountingSummaryService.IncomeStatement is) {
        isTable.removeAllViews();
        
        // Revenues Section
        isTable.addView(createHeaderRow(getString(R.string.label_revenues)));
        for (Map.Entry<String, Double> entry : is.revenues.entrySet()) {
            isTable.addView(createDataRow(entry.getKey(), entry.getValue(), 1));
        }
        isTable.addView(createTotalRow("", is.totalRevenue, 2));

        // Expenses Section
        isTable.addView(createHeaderRow(getString(R.string.label_expenses)));
        for (Map.Entry<String, Double> entry : is.expenses.entrySet()) {
            isTable.addView(createDataRow(entry.getKey(), entry.getValue(), 1));
        }
        isTable.addView(createTotalRow("", is.totalExpense, 2));

        // Net Income
        double net = is.getNetIncome();
        String label = net >= 0 ? getString(R.string.label_net_income) : getString(R.string.label_net_loss);
        isTable.addView(createTotalRow(label, Math.abs(net), 3));
    }

    private void renderEquityStatement(AccountingSummaryService.EquityStatement es) {
        esTable.removeAllViews();
        esTable.addView(createDataRow(getString(R.string.label_capital_beginning), es.beginningCapital, 2));
        
        String netLabel = es.netIncome >= 0 ? getString(R.string.label_add_net_income) : getString(R.string.label_less_net_loss);
        esTable.addView(createDataRow(netLabel, Math.abs(es.netIncome), 2));
        
        double subTotal = es.beginningCapital + es.netIncome;
        esTable.addView(createDataRow("Total", subTotal, 2));
        
        esTable.addView(createDataRow(getString(R.string.label_less_drawings), es.drawings, 2));
        esTable.addView(createTotalRow(getString(R.string.label_capital_end), es.getEndingCapital(), 3));
    }

    private void renderBalanceSheet(AccountingSummaryService.BalanceSheet bs) {
        bsTable.removeAllViews();
        
        // ASSETS
        bsTable.addView(createHeaderRow(getString(R.string.label_assets)));
        bsTable.addView(createHeaderRow(getString(R.string.label_current_assets)));
        for (Map.Entry<String, Double> entry : bs.currentAssets.entrySet()) {
            bsTable.addView(createDataRow(entry.getKey(), entry.getValue(), 1));
        }
        
        bsTable.addView(createHeaderRow(getString(R.string.label_non_current_assets)));
        for (Map.Entry<String, Double> entry : bs.nonCurrentAssets.entrySet()) {
            if (entry.getValue() < 0) { // Accumulated Depreciation
                 bsTable.addView(createDataRow(getString(R.string.label_less, entry.getKey()), Math.abs(entry.getValue()), 1));
            } else {
                 bsTable.addView(createDataRow(entry.getKey(), entry.getValue(), 1));
            }
        }
        bsTable.addView(createTotalRow(getString(R.string.label_total_assets), bs.totalAssets, 3));

        // LIABILITIES AND EQUITY
        bsTable.addView(createHeaderRow(getString(R.string.label_liabilities_equity)));
        bsTable.addView(createHeaderRow(getString(R.string.label_liabilities)));
        bsTable.addView(createHeaderRow(getString(R.string.label_current_liabilities)));
        for (Map.Entry<String, Double> entry : bs.currentLiabilities.entrySet()) {
            bsTable.addView(createDataRow(entry.getKey(), entry.getValue(), 1));
        }
        bsTable.addView(createTotalRow("Total Liabilities", bs.totalLiabilities, 2));
        
        bsTable.addView(createHeaderRow("Owner's Equity:"));
        bsTable.addView(createDataRow("Capital, End", bs.endingCapital, 1));
        
        bsTable.addView(createTotalRow(getString(R.string.label_total_liabilities_equity), bs.totalLiabilities + bs.endingCapital, 3));
    }

    private TableRow createHeaderRow(String text) {
        TableRow row = new TableRow(this);
        TextView view = new TextView(this);
        view.setText(text);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(4));
        row.addView(view);
        return row;
    }

    private TableRow createDataRow(String label, double value, int col) {
        TableRow row = new TableRow(this);
        TextView lView = new TextView(this);
        lView.setText(label);
        lView.setPadding(dp(col == 1 ? 16 : 0), dp(4), 0, dp(4));
        row.addView(lView);

        TextView vView = new TextView(this);
        vView.setText(currencyFormat.format(value));
        vView.setGravity(Gravity.END);
        vView.setPadding(dp(8), dp(4), 0, dp(4));
        row.addView(vView);
        return row;
    }

    private TableRow createTotalRow(String label, double value, int style) {
        TableRow row = new TableRow(this);
        TextView lView = new TextView(this);
        lView.setText(label);
        lView.setTypeface(null, android.graphics.Typeface.BOLD);
        lView.setPadding(0, dp(8), 0, dp(8));
        row.addView(lView);

        TextView vView = new TextView(this);
        vView.setText(currencyFormat.format(value));
        vView.setGravity(Gravity.END);
        vView.setTypeface(null, android.graphics.Typeface.BOLD);
        vView.setPadding(dp(8), dp(8), 0, dp(8));
        // Style 3 could mean double underline, but we'll just use bold for now.
        row.addView(vView);
        return row;
    }

    private String formatPeriodLabel(int month, int year) {
        String[] monthNames = new DateFormatSymbols(Locale.getDefault()).getMonths();
        return monthNames[month - 1] + " " + year;
    }

    private int dp(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}
