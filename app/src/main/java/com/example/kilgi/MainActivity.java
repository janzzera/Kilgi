package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.widget.AdapterView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.BatchValuationEngine;
import com.example.kilgi.inventory.service.BatchValuationSnapshot;
import com.example.kilgi.inventory.service.CustomerCollectionResult;
import com.example.kilgi.inventory.service.LotFilterUtils;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.example.kilgi.inventory.service.ProviderSettlementResult;
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

public class MainActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MaterialToolbar topAppBar;
    private ScrollView contentScrollView;
    private Spinner monthFilterSpinner;
    private Spinner yearFilterSpinner;
    private Spinner dayFilterSpinner;
    private TextView batchStatusView;
    private TextView expenseStatusView;
    private TextView spoilageStatusView;
    private TextView selectedLotLabelView;
    private TextView lotsEmptyStateView;
    private TableLayout lotsTableLayout;
    private TableLayout lotDetailsTableLayout;
    private Button addExpenseButton;
    private Button logSpoilageButton;
    private Button addProviderButton;
    private Button addCustomerButton;
    private Button logRetailSaleButton;
    private Button createWholesaleInvoiceButton;
    private Button collectCustomerPaymentButton;
    private Button settleProviderButton;
    private TextView salesStatusView;
    private TextView salesSummaryView;
    private View lotSectionView;

    private ModuleOneRepository repository;
    private String initialLotId;
    private String currentSelectedLotId;
    private ArrayAdapter<String> monthFilterAdapter;
    private ArrayAdapter<String> dayFilterAdapter;
    private ArrayAdapter<Integer> yearFilterAdapter;
    private final List<String> dayFilterOptions = new ArrayList<>();
    private final List<Integer> yearFilterOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialLotId = getIntent().getStringExtra(JournalActivity.EXTRA_LOT_ID);
        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        bindViews();
        setupTopAppBar();
        setupFilterSpinners();
        bindActions();
        updateLotActionButtons(false);
        loadInitialDashboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        contentScrollView = findViewById(R.id.content_scroll);
        monthFilterSpinner = findViewById(R.id.spinner_filter_month);
        yearFilterSpinner = findViewById(R.id.spinner_filter_year);
        dayFilterSpinner = findViewById(R.id.spinner_filter_day);
        batchStatusView = findViewById(R.id.text_batch_status);
        expenseStatusView = findViewById(R.id.text_expense_status);
        spoilageStatusView = findViewById(R.id.text_spoilage_status);
        selectedLotLabelView = findViewById(R.id.text_selected_lot_label);
        lotsEmptyStateView = findViewById(R.id.text_lots_empty_state);
        lotsTableLayout = findViewById(R.id.table_lots);
        lotDetailsTableLayout = findViewById(R.id.table_lot_details);
        addExpenseButton = findViewById(R.id.button_add_expense);
        logSpoilageButton = findViewById(R.id.button_log_spoilage);
        addProviderButton = findViewById(R.id.button_add_provider);
        addCustomerButton = findViewById(R.id.button_add_customer);
        logRetailSaleButton = findViewById(R.id.button_log_retail_sale);
        createWholesaleInvoiceButton = findViewById(R.id.button_create_wholesale_invoice);
        collectCustomerPaymentButton = findViewById(R.id.button_collect_customer_payment);
        settleProviderButton = findViewById(R.id.button_settle_provider);
        salesStatusView = findViewById(R.id.text_sales_status);
        salesSummaryView = findViewById(R.id.text_sales_summary);
        lotSectionView = findViewById(R.id.section_lot);
    }

    private void setupTopAppBar() {
        topAppBar.setTitle(R.string.lot_screen_title);
        topAppBar.inflateMenu(R.menu.main_sections_menu);
        topAppBar.setOnMenuItemClickListener(item -> handleMenuNavigation(item.getItemId()));
    }

    private void setupFilterSpinners() {
        monthFilterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildMonthOptions()
        );
        monthFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthFilterSpinner.setAdapter(monthFilterAdapter);

        yearFilterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                yearFilterOptions
        );
        yearFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearFilterSpinner.setAdapter(yearFilterAdapter);

        dayFilterAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                dayFilterOptions
        );
        dayFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dayFilterSpinner.setAdapter(dayFilterAdapter);

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        updateYearOptions(currentYear, currentYear, currentYear);
        monthFilterSpinner.setSelection(calendar.get(Calendar.MONTH) + 1);
        updateDayOptions(currentYear, calendar.get(Calendar.MONTH) + 1, LotFilterUtils.ALL_DAYS);
        dayFilterSpinner.setSelection(0);
        bindFilterSpinnerDependencies();
    }

    private List<String> buildMonthOptions() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.filter_all_months));
        String[] monthNames = new DateFormatSymbols(Locale.getDefault()).getMonths();
        for (int month = 0; month < 12; month++) {
            options.add(monthNames[month]);
        }
        return options;
    }

    private List<String> buildDayOptions(int year, int monthOfYear) {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.filter_all_days));
        int maxDays = getMaxDaysForMonth(year, monthOfYear);
        for (int day = 1; day <= maxDays; day++) {
            options.add(String.valueOf(day));
        }
        return options;
    }

    private void bindFilterSpinnerDependencies() {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                syncDayFilterOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                syncDayFilterOptions();
            }
        };
        monthFilterSpinner.setOnItemSelectedListener(listener);
        yearFilterSpinner.setOnItemSelectedListener(listener);
    }

    private void syncDayFilterOptions() {
        updateDayOptions(getSelectedYearFilter(), getSelectedMonthFilter(), getSelectedDayFilter());
    }

    private void updateDayOptions(int year, int monthOfYear, int preferredDay) {
        List<String> updatedOptions = buildDayOptions(year, monthOfYear);
        if (!updatedOptions.equals(dayFilterOptions)) {
            dayFilterOptions.clear();
            dayFilterOptions.addAll(updatedOptions);
            dayFilterAdapter.notifyDataSetChanged();
        }

        int selectedDay = preferredDay;
        if (selectedDay > getMaxDaysForMonth(year, monthOfYear)) {
            selectedDay = LotFilterUtils.ALL_DAYS;
        }
        dayFilterSpinner.setSelection(Math.max(0, selectedDay));
    }

    private int getMaxDaysForMonth(int year, int monthOfYear) {
        if (monthOfYear == LotFilterUtils.ALL_MONTHS) {
            return 31;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, Math.max(0, monthOfYear - 1));
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void bindActions() {
        Button createLotButton = findViewById(R.id.button_create_lot);
        Button applyFilterButton = findViewById(R.id.button_apply_lot_filter);

        createLotButton.setOnClickListener(v -> showCreateLotDialog());
        applyFilterButton.setOnClickListener(v -> refreshDashboardAsync(currentSelectedLotId, null, null, null));
        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());
        logSpoilageButton.setOnClickListener(v -> showLogSpoilageDialog());
        addProviderButton.setOnClickListener(v -> showAddProviderDialog());
        addCustomerButton.setOnClickListener(v -> showAddCustomerDialog());
        logRetailSaleButton.setOnClickListener(v -> showRetailSaleDialog());
        createWholesaleInvoiceButton.setOnClickListener(v -> showWholesaleInvoiceDialog());
        collectCustomerPaymentButton.setOnClickListener(v -> showCustomerCollectionDialog());
        settleProviderButton.setOnClickListener(v -> showProviderSettlementDialog());
    }

    private boolean handleMenuNavigation(int itemId) {
        if (itemId == R.id.menu_lot) {
            scrollToSection(lotSectionView);
            return true;
        }
        if (itemId == R.id.menu_journal) {
            Intent intent = new Intent(this, JournalActivity.class);
            if (!TextUtils.isEmpty(currentSelectedLotId)) {
                intent.putExtra(JournalActivity.EXTRA_LOT_ID, currentSelectedLotId);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void scrollToSection(View sectionView) {
        if (sectionView == null || contentScrollView == null) {
            return;
        }
        contentScrollView.post(() -> contentScrollView.smoothScrollTo(0, sectionView.getTop()));
    }

    private void loadInitialDashboard() {
        int yearFilter = getSelectedYearFilter();
        int monthFilter = getSelectedMonthFilter();
        int dayFilter = getSelectedDayFilter();
        ioExecutor.execute(() -> {
            String targetLotId = initialLotId;
            if (TextUtils.isEmpty(targetLotId)) {
                LotEntity latestLot = repository.getLatestLot();
                targetLotId = latestLot == null ? null : latestLot.lotId;
            }
            refreshDashboardOnWorker(targetLotId, null, null, null, null, yearFilter, monthFilter, dayFilter);
        });
    }

    private void refreshDashboardAsync(String targetLotId, String batchStatus, String expenseStatus, String spoilageStatus) {
        int yearFilter = getSelectedYearFilter();
        int monthFilter = getSelectedMonthFilter();
        int dayFilter = getSelectedDayFilter();
        ioExecutor.execute(() -> refreshDashboardOnWorker(
                targetLotId,
                batchStatus,
                expenseStatus,
                spoilageStatus,
                null,
                yearFilter,
                monthFilter,
                dayFilter
        ));
    }

    private int getSelectedYearFilter() {
        Integer selectedYear = (Integer) yearFilterSpinner.getSelectedItem();
        return selectedYear == null ? Calendar.getInstance().get(Calendar.YEAR) : selectedYear;
    }

    private int getSelectedMonthFilter() {
        int selectedPosition = monthFilterSpinner.getSelectedItemPosition();
        return selectedPosition <= 0 ? LotFilterUtils.ALL_MONTHS : selectedPosition;
    }

    private int getSelectedDayFilter() {
        int selectedPosition = dayFilterSpinner.getSelectedItemPosition();
        return selectedPosition <= 0 ? LotFilterUtils.ALL_DAYS : selectedPosition;
    }

    private void showCreateLotDialog() {
        ioExecutor.execute(() -> {
            List<ProviderEntity> providers = repository.getProviders();
            runOnUiThread(() -> showCreateLotDialog(providers));
        });
    }

    private void showCreateLotDialog(List<ProviderEntity> providers) {
        if (providers == null || providers.isEmpty()) {
            batchStatusView.setText(R.string.no_providers_available);
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_lot, null, false);
        Spinner providerSpinner = dialogView.findViewById(R.id.spinner_provider);
        EditText vegetableTypeInput = dialogView.findViewById(R.id.edit_vegetable_type);
        EditText totalSacksInput = dialogView.findViewById(R.id.edit_total_sacks);
        EditText rawKilosInput = dialogView.findViewById(R.id.edit_raw_kilos);
        EditText baseUnitPriceInput = dialogView.findViewById(R.id.edit_base_unit_price);
        Spinner purchaseSourceSpinner = dialogView.findViewById(R.id.spinner_purchase_payment_source);
        EditText standardFreightInput = dialogView.findViewById(R.id.edit_standard_freight);
        Spinner freightSourceSpinner = dialogView.findViewById(R.id.spinner_freight_payment_source);

        providerSpinner.setAdapter(buildProviderAdapter(providers));
        purchaseSourceSpinner.setAdapter(buildPaymentSourceAdapter());
        freightSourceSpinner.setAdapter(buildPaymentSourceAdapter());
        purchaseSourceSpinner.setSelection(PaymentSource.ACCOUNTS_PAYABLE.ordinal());
        freightSourceSpinner.setSelection(PaymentSource.CASH.ordinal());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_create_lot_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                ProviderEntity provider = (ProviderEntity) providerSpinner.getSelectedItem();
                if (provider == null) {
                    throw new IllegalArgumentException(getString(R.string.no_providers_available));
                }
                String vegetableType = InventoryInputParser.requireText(vegetableTypeInput.getText().toString(), "Vegetable type");
                int totalSacks = InventoryInputParser.parseRequiredPositiveInt(totalSacksInput.getText().toString(), "Total sacks purchased");
                double rawKilos = InventoryInputParser.parseRequiredPositiveDouble(rawKilosInput.getText().toString(), "Raw kilograms received");
                double baseUnitPrice = InventoryInputParser.parseOptionalNonNegativeDouble(baseUnitPriceInput.getText().toString(), "Base unit price per kilo");
                double standardFreight = InventoryInputParser.parseOptionalNonNegativeDouble(standardFreightInput.getText().toString(), "Standard freight");
                PaymentSource purchaseSource = (PaymentSource) purchaseSourceSpinner.getSelectedItem();
                PaymentSource freightSource = (PaymentSource) freightSourceSpinner.getSelectedItem();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();

                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        LotEntity lot = repository.createLot(
                                provider.providerId,
                                vegetableType,
                                totalSacks,
                                rawKilos,
                                baseUnitPrice,
                                purchaseSource,
                                standardFreight,
                                freightSource
                        );
                        refreshDashboardOnWorker(
                                lot.lotId,
                                getString(R.string.batch_created_message, abbreviateLotId(lot.lotId)),
                                getString(R.string.expense_status_idle),
                                getString(R.string.spoilage_status_idle),
                                null,
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(exception.getMessage(), null, null, null);
                    }
                });
            } catch (IllegalArgumentException exception) {
                batchStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddExpenseDialog() {
        if (TextUtils.isEmpty(currentSelectedLotId)) {
            expenseStatusView.setText(getString(R.string.no_active_lot_placeholder));
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null, false);
        TextView selectedLotView = dialogView.findViewById(R.id.text_selected_lot);
        Spinner expenseAccountSpinner = dialogView.findViewById(R.id.spinner_expense_account);
        EditText expenseAmountInput = dialogView.findViewById(R.id.edit_expense_amount);
        Spinner paymentSourceSpinner = dialogView.findViewById(R.id.spinner_expense_payment_source);

        selectedLotView.setText(getString(R.string.dialog_selected_lot, abbreviateLotId(currentSelectedLotId), currentSelectedLotId));
        expenseAccountSpinner.setAdapter(buildLotExpenseAccountAdapter());
        expenseAccountSpinner.setSelection(getDefaultLotExpenseAccountIndex());
        paymentSourceSpinner.setAdapter(buildPaymentSourceAdapter());
        paymentSourceSpinner.setSelection(PaymentSource.CASH.ordinal());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_expense_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                AccountingAccount expenseAccount = (AccountingAccount) expenseAccountSpinner.getSelectedItem();
                double amount = InventoryInputParser.parseRequiredPositiveDouble(expenseAmountInput.getText().toString(), "Expense amount");
                PaymentSource paymentSource = (PaymentSource) paymentSourceSpinner.getSelectedItem();
                String lotId = currentSelectedLotId;
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();

                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        repository.addExpense(lotId, expenseAccount, amount, paymentSource);
                        refreshDashboardOnWorker(
                                lotId,
                                null,
                                getString(R.string.expense_added_message, expenseAccount == null ? "" : expenseAccount.getName()),
                                null,
                                null,
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, exception.getMessage(), null, null);
                    }
                });
            } catch (IllegalArgumentException exception) {
                expenseStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showLogSpoilageDialog() {
        if (TextUtils.isEmpty(currentSelectedLotId)) {
            spoilageStatusView.setText(getString(R.string.no_active_lot_placeholder));
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_spoilage, null, false);
        TextView selectedLotView = dialogView.findViewById(R.id.text_selected_lot);
        EditText spoilageKilosInput = dialogView.findViewById(R.id.edit_spoilage_kilos);
        Spinner lossTypeSpinner = dialogView.findViewById(R.id.spinner_loss_type);

        selectedLotView.setText(getString(R.string.dialog_selected_lot, abbreviateLotId(currentSelectedLotId), currentSelectedLotId));
        lossTypeSpinner.setAdapter(buildLossTypeAdapter());
        lossTypeSpinner.setSelection(LossType.NORMAL.ordinal());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_log_spoilage_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double kilosLost = InventoryInputParser.parseRequiredPositiveDouble(spoilageKilosInput.getText().toString(), "Spoilage kilos");
                LossType lossType = (LossType) lossTypeSpinner.getSelectedItem();
                String lotId = currentSelectedLotId;
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();

                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        repository.logSpoilage(lotId, kilosLost, lossType);
                        String message = lossType == LossType.NORMAL
                                ? getString(R.string.spoilage_logged_message, formatWeight(kilosLost))
                                : getString(R.string.abnormal_spoilage_logged_message, formatWeight(kilosLost));
                        refreshDashboardOnWorker(
                                lotId,
                                null,
                                null,
                                message,
                                null,
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, exception.getMessage(), null);
                    }
                });
            } catch (IllegalArgumentException exception) {
                spoilageStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private ArrayAdapter<PaymentSource> buildPaymentSourceAdapter() {
        ArrayAdapter<PaymentSource> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                PaymentSource.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter<AccountingAccount> buildLotExpenseAccountAdapter() {
        ArrayAdapter<AccountingAccount> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                AccountingCatalog.getLotExpenseAccounts()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private int getDefaultLotExpenseAccountIndex() {
        List<AccountingAccount> accounts = AccountingCatalog.getLotExpenseAccounts();
        for (int index = 0; index < accounts.size(); index++) {
            if (AccountingCatalog.FREIGHT_IN_CODE.equals(accounts.get(index).getCode())) {
                return index;
            }
        }
        return 0;
    }

    private ArrayAdapter<LossType> buildLossTypeAdapter() {
        ArrayAdapter<LossType> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                LossType.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void refreshDashboardOnWorker(
            String targetLotId,
            String batchStatus,
            String expenseStatus,
            String spoilageStatus,
            String salesStatus,
            int yearFilter,
            int monthFilter,
            int dayFilter
    ) {
        try {
            List<LotEntity> allLots = repository.getAllLots();
            FilterYearRange yearRange = resolveLotYearRange(allLots, yearFilter);
            List<LotRecord> filteredLots = buildLotRecords(allLots, yearFilter, monthFilter, dayFilter);
            String salesSummary = buildSalesSummaryText();
            String effectiveLotId = resolveSelectedLotId(targetLotId, filteredLots);
            LotRecord selectedRecord = findRecord(filteredLots, effectiveLotId);
            String batchMessage = batchStatus == null
                    ? getString(R.string.lots_status_count, filteredLots.size())
                    : batchStatus;

            runOnUiThread(() -> {
                updateYearOptions(yearRange.minYear, yearRange.maxYear, yearFilter);
                syncDayFilterOptions();
                currentSelectedLotId = selectedRecord == null ? null : selectedRecord.details.lot.lotId;
                batchStatusView.setText(batchMessage);
                if (expenseStatus != null) {
                    expenseStatusView.setText(expenseStatus);
                }
                if (spoilageStatus != null) {
                    spoilageStatusView.setText(spoilageStatus);
                }
                if (salesStatus != null) {
                    salesStatusView.setText(salesStatus);
                }
                salesSummaryView.setText(salesSummary);
                renderLotsTable(filteredLots, currentSelectedLotId);
                renderSelectedLotDetails(selectedRecord);
                lotsEmptyStateView.setVisibility(filteredLots.isEmpty() ? View.VISIBLE : View.GONE);
                lotsEmptyStateView.setText(filteredLots.isEmpty()
                        ? getString(R.string.no_lots_filtered_placeholder)
                        : getString(R.string.no_lots_placeholder));
                updateLotActionButtons(selectedRecord != null);
            });
        } catch (Exception exception) {
            postStatuses(exception.getMessage(), exception.getMessage(), exception.getMessage(), exception.getMessage());
        }
    }

    private FilterYearRange resolveLotYearRange(List<LotEntity> lots, int selectedYear) {
        int minYear = selectedYear;
        int maxYear = selectedYear;
        Calendar calendar = Calendar.getInstance();

        for (LotEntity lot : lots) {
            calendar.setTimeInMillis(lot.timestamp);
            int lotYear = calendar.get(Calendar.YEAR);
            minYear = Math.min(minYear, lotYear);
            maxYear = Math.max(maxYear, lotYear);
        }

        return new FilterYearRange(minYear, maxYear);
    }

    private void updateYearOptions(int minYear, int maxYear, int selectedYear) {
        int safeMinYear = Math.min(minYear, selectedYear);
        int safeMaxYear = Math.max(maxYear, selectedYear);

        List<Integer> updatedYears = new ArrayList<>();
        for (int year = safeMaxYear; year >= safeMinYear; year--) {
            updatedYears.add(year);
        }

        if (!updatedYears.equals(yearFilterOptions)) {
            yearFilterOptions.clear();
            yearFilterOptions.addAll(updatedYears);
            yearFilterAdapter.notifyDataSetChanged();
        }

        int selectedIndex = yearFilterOptions.indexOf(selectedYear);
        if (selectedIndex >= 0) {
            yearFilterSpinner.setSelection(selectedIndex);
        }
    }

    private List<LotRecord> buildLotRecords(List<LotEntity> lots, int yearFilter, int monthFilter, int dayFilter) {
        List<LotRecord> records = new ArrayList<>();
        for (LotEntity lot : lots) {
            if (!LotFilterUtils.matchesYearMonthAndDay(lot.timestamp, yearFilter, monthFilter, dayFilter)) {
                continue;
            }
            LotWithDetails lotWithDetails = repository.getLotWithDetails(lot.lotId);
            BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(
                    lotWithDetails.lot,
                    lotWithDetails.expenses,
                    lotWithDetails.spoilageLogs
            );
            records.add(new LotRecord(lotWithDetails, snapshot, sumExpenses(lotWithDetails)));
        }
        return records;
    }

    private String resolveSelectedLotId(String targetLotId, List<LotRecord> filteredLots) {
        if (!TextUtils.isEmpty(targetLotId) && findRecord(filteredLots, targetLotId) != null) {
            return targetLotId;
        }
        if (!TextUtils.isEmpty(currentSelectedLotId) && findRecord(filteredLots, currentSelectedLotId) != null) {
            return currentSelectedLotId;
        }
        return filteredLots.isEmpty() ? null : filteredLots.get(0).details.lot.lotId;
    }

    private LotRecord findRecord(List<LotRecord> records, String lotId) {
        if (TextUtils.isEmpty(lotId)) {
            return null;
        }
        for (LotRecord record : records) {
            if (lotId.equals(record.details.lot.lotId)) {
                return record;
            }
        }
        return null;
    }

    private void renderLotsTable(List<LotRecord> filteredLots, String selectedLotId) {
        lotsTableLayout.removeAllViews();
        lotsTableLayout.addView(createLotsHeaderRow());
        for (int index = 0; index < filteredLots.size(); index++) {
            LotRecord record = filteredLots.get(index);
            boolean isSelected = record.details.lot.lotId.equals(selectedLotId);
            lotsTableLayout.addView(createLotRow(record, index, isSelected));
        }
    }

    private TableRow createLotsHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(0xFFE0E0E0);
        row.addView(createCell(getString(R.string.lot_table_header_date), true));
        row.addView(createCell(getString(R.string.lot_table_header_id), true));
        row.addView(createCell(getString(R.string.lot_table_header_vegetable), true));
        row.addView(createCell(getString(R.string.lot_table_header_provider), true));
        row.addView(createCell(getString(R.string.lot_table_header_usable), true));
        row.addView(createCell(getString(R.string.lot_table_header_cost), true));
        return row;
    }

    private TableRow createLotRow(LotRecord record, int index, boolean selected) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(selected
                ? 0xFFE8F0FE
                : (index % 2 == 0 ? 0xFFF8F8F8 : 0xFFFFFFFF));
        row.setOnClickListener(v -> refreshDashboardAsync(
                record.details.lot.lotId,
                getString(R.string.batch_status_loaded, abbreviateLotId(record.details.lot.lotId)),
                null,
                null
        ));

        row.addView(createCell(dateFormat.format(record.details.lot.timestamp), false));
        row.addView(createCell(abbreviateLotId(record.details.lot.lotId), false));
        row.addView(createCell(record.details.lot.vegetableType, false));
        row.addView(createCell(record.details.lot.providerName, false));
        row.addView(createCell(formatWeight(record.snapshot.getNetUsableKilograms()), false));
        row.addView(createCell(currencyFormat.format(record.snapshot.getTotalCapitalizedCost()), false));
        return row;
    }

    private TextView createCell(String text, boolean header) {
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

    private void renderSelectedLotDetails(LotRecord record) {
        lotDetailsTableLayout.removeAllViews();
        if (record == null) {
            selectedLotLabelView.setText(R.string.no_active_lot_placeholder);
            return;
        }

        LotEntity lot = record.details.lot;
        selectedLotLabelView.setText(getString(
                R.string.lot_selected_label,
                abbreviateLotId(lot.lotId),
                lot.vegetableType
        ));

        addDetailRow(R.string.lot_detail_date, dateFormat.format(lot.timestamp));
        addDetailRow(R.string.lot_detail_provider, lot.providerName + " (" + lot.providerId + ")");
        addDetailRow(R.string.lot_detail_vegetable, lot.vegetableType);
        addDetailRow(R.string.lot_detail_sacks, String.valueOf(lot.totalSacksPurchased));
        addDetailRow(R.string.lot_detail_raw_kilos, formatWeight(lot.rawKilosReceived));
        addDetailRow(R.string.lot_detail_purchase_cost, currencyFormat.format(record.snapshot.getPurchaseCost()));
        addDetailRow(R.string.lot_detail_standard_freight, currencyFormat.format(lot.standardFreight));
        addDetailRow(R.string.lot_detail_expense_count, String.valueOf(record.details.expenses.size()));
        addDetailRow(R.string.lot_detail_expense_total, currencyFormat.format(record.expenseTotal));
        addDetailRow(R.string.lot_detail_normal_spoilage, formatWeight(record.snapshot.getNormalLossKilos()));
        addDetailRow(R.string.lot_detail_abnormal_spoilage, formatWeight(record.snapshot.getAbnormalLossKilos()));
        addDetailRow(R.string.lot_detail_write_off, currencyFormat.format(record.snapshot.getAbnormalWriteOffValue()));
        addDetailRow(R.string.lot_detail_net_usable, formatWeight(record.snapshot.getNetUsableKilograms()));
        addDetailRow(R.string.lot_detail_capitalized_cost, currencyFormat.format(record.snapshot.getTotalCapitalizedCost()));
        addDetailRow(R.string.lot_detail_true_cost, formatCurrency(record.snapshot.getTrueCostPerKilo()));
        addDetailRow(R.string.lot_detail_purchase_source, lot.purchasePaymentSource);
        addDetailRow(R.string.lot_detail_freight_source, lot.freightPaymentSource);
    }

    private void addDetailRow(int labelResId, String value) {
        TableRow row = new TableRow(this);
        row.addView(createDetailCell(getString(labelResId), true));
        row.addView(createDetailCell(value, false));
        lotDetailsTableLayout.addView(row);
    }

    private TextView createDetailCell(String text, boolean key) {
        TextView view = new TextView(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                key ? 0.9f : 1.1f
        );
        view.setLayoutParams(params);
        view.setPadding(0, dp(8), dp(12), dp(8));
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        if (key) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private void updateLotActionButtons(boolean hasSelectedLot) {
        addExpenseButton.setEnabled(hasSelectedLot);
        logSpoilageButton.setEnabled(hasSelectedLot);
    }

    private void postStatuses(String batchStatus, String expenseStatus, String spoilageStatus, String salesStatus) {
        runOnUiThread(() -> {
            if (batchStatus != null) {
                batchStatusView.setText(batchStatus);
            }
            if (expenseStatus != null) {
                expenseStatusView.setText(expenseStatus);
            }
            if (spoilageStatus != null) {
                spoilageStatusView.setText(spoilageStatus);
            }
            if (salesStatus != null) {
                salesStatusView.setText(salesStatus);
            }
        });
    }

    private void showAddProviderDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_provider, null, false);
        EditText nameInput = dialogView.findViewById(R.id.edit_provider_name);
        EditText contactInput = dialogView.findViewById(R.id.edit_provider_contact);
        EditText addressInput = dialogView.findViewById(R.id.edit_provider_address);
        EditText notesInput = dialogView.findViewById(R.id.edit_provider_notes);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_provider_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String displayName = InventoryInputParser.requireText(nameInput.getText().toString(), "Provider name");
                String contact = contactInput.getText().toString();
                String address = addressInput.getText().toString();
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        ProviderEntity provider = repository.createProvider(displayName, contact, address, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.provider_created_message, provider.displayName),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showAddCustomerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null, false);
        EditText nameInput = dialogView.findViewById(R.id.edit_customer_name);
        EditText contactInput = dialogView.findViewById(R.id.edit_customer_contact);
        EditText addressInput = dialogView.findViewById(R.id.edit_customer_address);
        EditText notesInput = dialogView.findViewById(R.id.edit_customer_notes);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_add_customer_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String displayName = InventoryInputParser.requireText(nameInput.getText().toString(), "Customer name");
                String contact = contactInput.getText().toString();
                String address = addressInput.getText().toString();
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        CustomerEntity customer = repository.createCustomer(displayName, contact, address, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.customer_created_message, customer.displayName),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showRetailSaleDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_retail_sale, null, false);
        EditText amountInput = dialogView.findViewById(R.id.edit_retail_sale_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_retail_sale_notes);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_log_retail_sale_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double amount = InventoryInputParser.parseRequiredPositiveDouble(amountInput.getText().toString(), "Retail sale amount");
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        repository.recordRetailSale(amount, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.retail_sale_logged_message, currencyFormat.format(amount)),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showWholesaleInvoiceDialog() {
        ioExecutor.execute(() -> {
            List<CustomerEntity> customers = repository.getCustomers();
            runOnUiThread(() -> showWholesaleInvoiceDialog(customers));
        });
    }

    private void showWholesaleInvoiceDialog(List<CustomerEntity> customers) {
        if (customers == null || customers.isEmpty()) {
            salesStatusView.setText(R.string.no_customers_available);
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_wholesale_invoice, null, false);
        Spinner customerSpinner = dialogView.findViewById(R.id.spinner_invoice_customer);
        EditText descriptionInput = dialogView.findViewById(R.id.edit_invoice_description);
        EditText amountInput = dialogView.findViewById(R.id.edit_invoice_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_invoice_notes);

        customerSpinner.setAdapter(buildCustomerAdapter(customers));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_wholesale_invoice_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                CustomerEntity customer = (CustomerEntity) customerSpinner.getSelectedItem();
                if (customer == null) {
                    throw new IllegalArgumentException(getString(R.string.no_customers_available));
                }
                String description = InventoryInputParser.requireText(descriptionInput.getText().toString(), "Invoice description");
                double amount = InventoryInputParser.parseRequiredPositiveDouble(amountInput.getText().toString(), "Invoice amount");
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        var invoice = repository.createWholesaleInvoice(customer.customerId, description, amount, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.wholesale_invoice_created_message, invoice.invoiceNumber, customer.displayName),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showCustomerCollectionDialog() {
        ioExecutor.execute(() -> {
            List<CustomerEntity> customers = repository.getCustomers();
            runOnUiThread(() -> showCustomerCollectionDialog(customers));
        });
    }

    private void showCustomerCollectionDialog(List<CustomerEntity> customers) {
        if (customers == null || customers.isEmpty()) {
            salesStatusView.setText(R.string.no_customers_available);
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_collect_customer_payment, null, false);
        Spinner customerSpinner = dialogView.findViewById(R.id.spinner_collection_customer);
        TextView breakdownView = dialogView.findViewById(R.id.text_customer_collection_breakdown);
        EditText amountInput = dialogView.findViewById(R.id.edit_customer_collection_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_customer_collection_notes);

        customerSpinner.setAdapter(buildCustomerAdapter(customers));
        bindCustomerBreakdownLoader(customerSpinner, breakdownView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_collect_payment_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                CustomerEntity customer = (CustomerEntity) customerSpinner.getSelectedItem();
                if (customer == null) {
                    throw new IllegalArgumentException(getString(R.string.no_customers_available));
                }
                double amount = InventoryInputParser.parseRequiredPositiveDouble(amountInput.getText().toString(), "Collection amount");
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        CustomerCollectionResult result = repository.collectCustomerPayment(customer.customerId, amount, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.customer_collection_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), customer.displayName, result.allocations.size()),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showProviderSettlementDialog() {
        ioExecutor.execute(() -> {
            List<ProviderEntity> providers = repository.getProviders();
            runOnUiThread(() -> showProviderSettlementDialog(providers));
        });
    }

    private void showProviderSettlementDialog(List<ProviderEntity> providers) {
        if (providers == null || providers.isEmpty()) {
            salesStatusView.setText(R.string.no_providers_available);
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settle_provider_payment, null, false);
        Spinner providerSpinner = dialogView.findViewById(R.id.spinner_settlement_provider);
        TextView breakdownView = dialogView.findViewById(R.id.text_provider_settlement_breakdown);
        EditText amountInput = dialogView.findViewById(R.id.edit_provider_settlement_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_provider_settlement_notes);

        providerSpinner.setAdapter(buildProviderAdapter(providers));
        bindProviderBreakdownLoader(providerSpinner, breakdownView);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_settle_provider_title)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                ProviderEntity provider = (ProviderEntity) providerSpinner.getSelectedItem();
                if (provider == null) {
                    throw new IllegalArgumentException(getString(R.string.no_providers_available));
                }
                double amount = InventoryInputParser.parseRequiredPositiveDouble(amountInput.getText().toString(), "Provider payment amount");
                String notes = notesInput.getText().toString();
                int yearFilter = getSelectedYearFilter();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        ProviderSettlementResult result = repository.settleProviderBalance(provider.providerId, amount, notes);
                        refreshDashboardOnWorker(
                                currentSelectedLotId,
                                null,
                                null,
                                null,
                                getString(R.string.provider_settlement_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), provider.displayName, result.allocations.size()),
                                yearFilter,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, null, exception.getMessage());
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private ArrayAdapter<ProviderEntity> buildProviderAdapter(List<ProviderEntity> providers) {
        ArrayAdapter<ProviderEntity> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                providers
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter<CustomerEntity> buildCustomerAdapter(List<CustomerEntity> customers) {
        ArrayAdapter<CustomerEntity> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                customers
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bindCustomerBreakdownLoader(Spinner spinner, TextView breakdownView) {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomerEntity customer = (CustomerEntity) parent.getItemAtPosition(position);
                if (customer == null) {
                    breakdownView.setText(R.string.no_customers_available);
                    return;
                }
                ioExecutor.execute(() -> {
                    List<OpenCustomerInvoice> invoices = repository.getOpenInvoicesForCustomer(customer.customerId);
                    String breakdown = buildCustomerBreakdownText(invoices);
                    runOnUiThread(() -> breakdownView.setText(breakdown));
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                breakdownView.setText(R.string.loading_balance_breakdown);
            }
        };
        spinner.setOnItemSelectedListener(listener);
        if (spinner.getCount() > 0) {
            listener.onItemSelected(spinner, null, spinner.getSelectedItemPosition(), spinner.getSelectedItemId());
        }
    }

    private void bindProviderBreakdownLoader(Spinner spinner, TextView breakdownView) {
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ProviderEntity provider = (ProviderEntity) parent.getItemAtPosition(position);
                if (provider == null) {
                    breakdownView.setText(R.string.no_providers_available);
                    return;
                }
                ioExecutor.execute(() -> {
                    List<OpenProviderLotPayable> payables = repository.getOpenLotPayablesForProvider(provider.providerId);
                    String breakdown = buildProviderBreakdownText(payables);
                    runOnUiThread(() -> breakdownView.setText(breakdown));
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                breakdownView.setText(R.string.loading_balance_breakdown);
            }
        };
        spinner.setOnItemSelectedListener(listener);
        if (spinner.getCount() > 0) {
            listener.onItemSelected(spinner, null, spinner.getSelectedItemPosition(), spinner.getSelectedItemId());
        }
    }

    private String buildSalesSummaryText() {
        List<CustomerLedgerSummary> customerSummaries = repository.getCustomerLedgerSummaries();
        List<ProviderLedgerSummary> providerSummaries = repository.getProviderLedgerSummaries();
        if (customerSummaries.isEmpty() && providerSummaries.isEmpty()) {
            return getString(R.string.sales_summary_empty);
        }

        StringBuilder builder = new StringBuilder();
        if (!customerSummaries.isEmpty()) {
            builder.append(getString(R.string.sales_summary_section_customers));
            for (CustomerLedgerSummary summary : customerSummaries) {
                builder.append("\n");
                builder.append(getString(
                        R.string.sales_summary_customer_line,
                        summary.displayName,
                        summary.openInvoiceCount,
                        currencyFormat.format(summary.outstandingBalance)
                ));
            }
        }
        if (!providerSummaries.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(getString(R.string.sales_summary_section_providers));
            for (ProviderLedgerSummary summary : providerSummaries) {
                builder.append("\n");
                builder.append(getString(
                        R.string.sales_summary_provider_line,
                        summary.displayName,
                        summary.openLotCount,
                        currencyFormat.format(summary.outstandingBalance)
                ));
            }
        }
        return builder.toString();
    }

    private String buildCustomerBreakdownText(List<OpenCustomerInvoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return getString(R.string.sales_summary_empty);
        }
        StringBuilder builder = new StringBuilder(getString(R.string.customer_invoice_breakdown_title));
        for (OpenCustomerInvoice invoice : invoices) {
            builder.append("\n");
            builder.append(getString(
                    R.string.customer_invoice_breakdown_line,
                    invoice.invoiceNumber,
                    invoice.description,
                    currencyFormat.format(invoice.outstandingBalance)
            ));
        }
        return builder.toString();
    }

    private String buildProviderBreakdownText(List<OpenProviderLotPayable> payables) {
        if (payables == null || payables.isEmpty()) {
            return getString(R.string.sales_summary_empty);
        }
        StringBuilder builder = new StringBuilder(getString(R.string.provider_payable_breakdown_title));
        for (OpenProviderLotPayable payable : payables) {
            builder.append("\n");
            builder.append(getString(
                    R.string.provider_payable_breakdown_line,
                    abbreviateLotId(payable.lotId),
                    payable.vegetableType,
                    currencyFormat.format(payable.outstandingBalance)
            ));
        }
        return builder.toString();
    }

    private double sumExpenses(LotWithDetails lotWithDetails) {
        double total = 0;
        for (int index = 0; index < lotWithDetails.expenses.size(); index++) {
            total += lotWithDetails.expenses.get(index).amount;
        }
        return total;
    }

    private String abbreviateLotId(String lotId) {
        if (TextUtils.isEmpty(lotId)) {
            return "-";
        }
        return lotId.length() <= 8 ? lotId : lotId.substring(0, 8);
    }

    private String formatWeight(double kilos) {
        return String.format(Locale.US, "%.2f", kilos);
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return getString(R.string.no_sellable_inventory);
        }
        return currencyFormat.format(value);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private static final class LotRecord {
        private final LotWithDetails details;
        private final BatchValuationSnapshot snapshot;
        private final double expenseTotal;

        private LotRecord(LotWithDetails details, BatchValuationSnapshot snapshot, double expenseTotal) {
            this.details = details;
            this.snapshot = snapshot;
            this.expenseTotal = expenseTotal;
        }
    }

    private static final class FilterYearRange {
        private final int minYear;
        private final int maxYear;

        private FilterYearRange(int minYear, int maxYear) {
            this.minYear = minYear;
            this.maxYear = maxYear;
        }
    }
}

