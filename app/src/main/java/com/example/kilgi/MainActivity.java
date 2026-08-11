package com.example.kilgi;

import android.content.Intent;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.BatchValuationEngine;
import com.example.kilgi.inventory.service.BatchValuationSnapshot;
import com.example.kilgi.inventory.service.LotFilterUtils;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int LOTS_PAGE_SIZE = 10;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private Button fromDateFilterButton;
    private Button toDateFilterButton;
    private Button lotSortToggleButton;
    private Button previousLotPageButton;
    private Button nextLotPageButton;
    private TextView batchStatusView;
    private TextView expenseStatusView;
    private TextView spoilageStatusView;
    private TextView selectedLotLabelView;
    private TextView lotsEmptyStateView;
    private TextView filterRangeSummaryView;
    private TextView lotPageSummaryView;
    private TableLayout lotsTableLayout;
    private TableLayout lotDetailsTableLayout;
    private Button addExpenseButton;
    private Button logSpoilageButton;
    private BottomNavigationView bottomNavigationView;

    private ModuleOneRepository repository;
    private String initialLotId;
    private String currentSelectedLotId;
    private Long selectedFromDateMillis;
    private Long selectedToDateMillis;
    private long resolvedFromDateMillis;
    private long resolvedToDateMillis;
    private boolean lotSortAscending;
    private int currentLotPageIndex;
    private volatile boolean isDashboardRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initialLotId = getIntent().getStringExtra(JournalActivity.EXTRA_LOT_ID);
        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        bindViews();
        setupNavigation();
        initializeLotFilterState();
        bindActions();
        updateLotActionButtons(false);
        loadInitialDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_inventory);
        loadInitialDashboard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void bindViews() {
        MaterialToolbar topAppBar = findViewById(R.id.top_app_bar);
        fromDateFilterButton = findViewById(R.id.button_filter_from_date);
        toDateFilterButton = findViewById(R.id.button_filter_to_date);
        previousLotPageButton = findViewById(R.id.button_previous_lot_page);
        nextLotPageButton = findViewById(R.id.button_next_lot_page);
        batchStatusView = findViewById(R.id.text_batch_status);
        expenseStatusView = findViewById(R.id.text_expense_status);
        spoilageStatusView = findViewById(R.id.text_spoilage_status);
        selectedLotLabelView = findViewById(R.id.text_selected_lot_label);
        lotsEmptyStateView = findViewById(R.id.text_lots_empty_state);
        filterRangeSummaryView = findViewById(R.id.text_filter_range_summary);
        lotPageSummaryView = findViewById(R.id.text_lot_page_summary);
        lotsTableLayout = findViewById(R.id.table_lots);
        lotDetailsTableLayout = findViewById(R.id.table_lot_details);
        addExpenseButton = findViewById(R.id.button_add_expense);
        logSpoilageButton = findViewById(R.id.button_log_spoilage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        topAppBar.setTitle(R.string.lot_screen_title);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_inventory) {
                return true;
            } else if (itemId == R.id.nav_sales) {
                startActivity(new Intent(this, SalesActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            } else if (itemId == R.id.nav_journal) {
                startActivity(new Intent(this, JournalActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            }
            return false;
        });
    }

    private void initializeLotFilterState() {
        lotSortAscending = false;
        currentLotPageIndex = 0;
        resolvedFromDateMillis = startOfDay(System.currentTimeMillis());
        resolvedToDateMillis = endOfDay(System.currentTimeMillis());
        updateDateRangeViews(resolvedFromDateMillis, resolvedToDateMillis);
        updateLotPaginationControls(0, 0);
    }

    private void showLotDatePicker(boolean selectingFromDate) {
        long seedMillis = selectingFromDate
                ? (selectedFromDateMillis != null ? selectedFromDateMillis : resolvedFromDateMillis)
                : (selectedToDateMillis != null ? selectedToDateMillis : resolvedToDateMillis);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(seedMillis);
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> handleSelectedFilterDate(selectingFromDate, year, month, dayOfMonth),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void handleSelectedFilterDate(boolean selectingFromDate, int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month, dayOfMonth);

        long normalizedMillis = selectingFromDate
                ? startOfDay(calendar.getTimeInMillis())
                : endOfDay(calendar.getTimeInMillis());

        runOnUiThread(() -> {
            if (selectingFromDate) {
                selectedFromDateMillis = normalizedMillis;
                long activeTo = selectedToDateMillis != null ? selectedToDateMillis : resolvedToDateMillis;
                if (normalizedMillis > activeTo) {
                    selectedToDateMillis = endOfDay(normalizedMillis);
                }
            } else {
                selectedToDateMillis = normalizedMillis;
                long activeFrom = selectedFromDateMillis != null ? selectedFromDateMillis : resolvedFromDateMillis;
                if (normalizedMillis < activeFrom) {
                    selectedFromDateMillis = startOfDay(normalizedMillis);
                }
            }

            updateDateRangeViews(
                    selectedFromDateMillis != null ? selectedFromDateMillis : resolvedFromDateMillis,
                    selectedToDateMillis != null ? selectedToDateMillis : resolvedToDateMillis
            );
        });
    }

    private void bindActions() {
        findViewById(R.id.button_create_lot).setOnClickListener(v -> showCreateLotDialog());
        fromDateFilterButton.setOnClickListener(v -> showLotDatePicker(true));
        toDateFilterButton.setOnClickListener(v -> showLotDatePicker(false));
        findViewById(R.id.button_apply_lot_filter).setOnClickListener(v -> {
            currentLotPageIndex = 0;
            refreshDashboardAsync(currentSelectedLotId, null, null, null, null, true);
        });
        previousLotPageButton.setOnClickListener(v -> loadLotPage(currentLotPageIndex - 1));
        nextLotPageButton.setOnClickListener(v -> loadLotPage(currentLotPageIndex + 1));
        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());
        logSpoilageButton.setOnClickListener(v -> showLogSpoilageDialog());
    }

    private void loadLotPage(int pageIndex) {
        refreshDashboardAsync(null, null, null, null, pageIndex, false);
    }

    private void loadInitialDashboard() {
        ioExecutor.execute(() -> {
            String targetLotId = initialLotId;
            if (TextUtils.isEmpty(targetLotId)) {
                LotEntity latestLot = repository.getLatestLot();
                targetLotId = latestLot == null ? null : latestLot.lotId;
            }
            refreshDashboardOnWorker(targetLotId, null, null, null, null, null, true);
        });
    }

    private void refreshDashboardAsync(String targetLotId, String batchStatus, String expenseStatus, String spoilageStatus) {
        refreshDashboardAsync(targetLotId, batchStatus, expenseStatus, spoilageStatus, null, true);
    }

    private void refreshDashboardAsync(
            String targetLotId,
            String batchStatus,
            String expenseStatus,
            String spoilageStatus,
            Integer requestedPageIndex,
            boolean keepTargetLotVisible
    ) {
        ioExecutor.execute(() -> refreshDashboardOnWorker(
                targetLotId,
                batchStatus,
                expenseStatus,
                spoilageStatus,
                null,
                requestedPageIndex,
                keepTargetLotVisible
        ));
    }

    private void showCreateLotDialog() {
        showCreateLotDialog(new ArrayList<>());
    }

    private void showCreateLotDialog(List<ProviderEntity> providers) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_lot, null, false);
        Spinner providerSpinner = dialogView.findViewById(R.id.spinner_provider);
        EditText vegetableTypeInput = dialogView.findViewById(R.id.edit_vegetable_type);
        EditText totalSacksInput = dialogView.findViewById(R.id.edit_total_sacks);
        EditText rawKilosInput = dialogView.findViewById(R.id.edit_raw_kilos);
        EditText baseUnitPriceInput = dialogView.findViewById(R.id.edit_base_unit_price);
        Spinner purchaseSourceSpinner = dialogView.findViewById(R.id.spinner_purchase_payment_source);
        EditText standardFreightInput = dialogView.findViewById(R.id.edit_standard_freight);
        Spinner freightSourceSpinner = dialogView.findViewById(R.id.spinner_freight_payment_source);

        providerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers));
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
                double baseUnitPrice = InventoryInputParser.parseRequiredPositiveDouble(baseUnitPriceInput.getText().toString(), "Base unit price per kilo");
                double standardFreight = InventoryInputParser.parseOptionalNonNegativeDouble(standardFreightInput.getText().toString(), "Standard freight");
                PaymentSource purchaseSource = (PaymentSource) purchaseSourceSpinner.getSelectedItem();
                PaymentSource freightSource = (PaymentSource) freightSourceSpinner.getSelectedItem();
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
                                null,
                                true
                        );
                    } catch (Exception exception) {
                        postStatuses(exception.getMessage(), null, null, null);
                    }
                });
            } catch (IllegalArgumentException exception) {
                batchStatusView.setText(exception.getMessage());
            }
        }));
        
        if (providers.isEmpty()) {
            ioExecutor.execute(() -> {
                List<ProviderEntity> fetched = repository.getProviders();
                runOnUiThread(() -> {
                    providerSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fetched));
                });
            });
        }
        
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
                                null,
                                true
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
                                null,
                                true
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
            Integer requestedPageIndex,
            boolean keepTargetLotVisible
    ) {
        if (isDashboardRefreshing) return;
        isDashboardRefreshing = true;

        try {
            List<LotEntity> allLots = repository.getAllLots();

            boolean shouldResetFilter = false;
            if (keepTargetLotVisible && !TextUtils.isEmpty(targetLotId)) {
                LotEntity target = null;
                for (LotEntity lot : allLots) {
                    if (lot.lotId.equals(targetLotId)) {
                        target = lot;
                        break;
                    }
                }
                if (target != null) {
                    ActiveLotDateRange currentRange = resolveActiveLotDateRange(allLots);
                    if (!LotFilterUtils.matchesDateRange(target.timestamp, currentRange.fromMillis, currentRange.toMillis)) {
                        shouldResetFilter = true;
                    }
                }
            }

            final boolean finalShouldResetFilter = shouldResetFilter;
            ActiveLotDateRange activeDateRange = resolveActiveLotDateRange(allLots);
            List<LotRecord> filteredLots = buildLotRecords(allLots, activeDateRange.fromMillis, activeDateRange.toMillis, lotSortAscending);
            int pageIndex;
            List<LotRecord> visibleLots;
            String effectiveLotId;

            if (keepTargetLotVisible) {
                effectiveLotId = resolveSelectedLotId(targetLotId, filteredLots);
                pageIndex = resolvePageIndex(filteredLots, effectiveLotId, requestedPageIndex);
                visibleLots = paginateLotRecords(filteredLots, pageIndex);
            } else {
                pageIndex = clampPageIndex(requestedPageIndex == null ? currentLotPageIndex : requestedPageIndex, filteredLots.size());
                visibleLots = paginateLotRecords(filteredLots, pageIndex);
                effectiveLotId = resolveSelectedLotId(targetLotId, visibleLots);
            }

            LotRecord selectedRecord = findRecord(visibleLots, effectiveLotId);
            String batchMessage = batchStatus == null
                    ? getString(R.string.lots_status_count, filteredLots.size())
                    : batchStatus;

            runOnUiThread(() -> {
                if (finalShouldResetFilter) {
                    selectedFromDateMillis = null;
                    selectedToDateMillis = null;
                }
                resolvedFromDateMillis = activeDateRange.fromMillis;
                resolvedToDateMillis = activeDateRange.toMillis;
                currentLotPageIndex = pageIndex;
                updateDateRangeViews(resolvedFromDateMillis, resolvedToDateMillis);
                updateLotPaginationControls(pageIndex, filteredLots.size());
                currentSelectedLotId = selectedRecord == null ? null : selectedRecord.details.lot.lotId;
                batchStatusView.setText(batchMessage);
                if (expenseStatus != null) {
                    expenseStatusView.setText(expenseStatus);
                }
                if (spoilageStatus != null) {
                    spoilageStatusView.setText(spoilageStatus);
                }
                renderLotsTable(visibleLots, currentSelectedLotId);
                updateSortToggleButton();
                renderSelectedLotDetails(selectedRecord);
                lotsEmptyStateView.setVisibility(filteredLots.isEmpty() ? View.VISIBLE : View.GONE);
                lotsEmptyStateView.setText(filteredLots.isEmpty()
                        ? getString(R.string.no_lots_filtered_placeholder)
                        : getString(R.string.no_lots_placeholder));
                updateLotActionButtons(selectedRecord != null);
            });
        } catch (Exception exception) {
            postStatuses(exception.getMessage(), exception.getMessage(), exception.getMessage(), exception.getMessage());
        } finally {
            isDashboardRefreshing = false;
        }
    }

    private ActiveLotDateRange resolveActiveLotDateRange(List<LotEntity> lots) {
        long defaultFrom = lots.isEmpty()
                ? startOfDay(System.currentTimeMillis())
                : startOfDay(findEarliestLotTimestamp(lots));
        long defaultTo = lots.isEmpty()
                ? endOfDay(System.currentTimeMillis())
                : endOfDay(findLatestLotTimestamp(lots));

        long from = selectedFromDateMillis != null ? selectedFromDateMillis : defaultFrom;
        long to = selectedToDateMillis != null ? selectedToDateMillis : defaultTo;
        return new ActiveLotDateRange(Math.min(from, to), Math.max(from, to));
    }

    private long findEarliestLotTimestamp(List<LotEntity> lots) {
        long earliest = System.currentTimeMillis();
        for (LotEntity lot : lots) {
            earliest = Math.min(earliest, lot.timestamp);
        }
        return earliest;
    }

    private long findLatestLotTimestamp(List<LotEntity> lots) {
        long latest = System.currentTimeMillis();
        for (LotEntity lot : lots) {
            latest = Math.max(latest, lot.timestamp);
        }
        return latest;
    }

    private List<LotRecord> buildLotRecords(List<LotEntity> lots, long fromMillis, long toMillis, boolean sortAscending) {
        List<LotRecord> records = new ArrayList<>();
        for (LotEntity lot : lots) {
            if (!LotFilterUtils.matchesDateRange(lot.timestamp, fromMillis, toMillis)) {
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
        Comparator<LotRecord> comparator = Comparator.comparingLong(record -> record.details.lot.timestamp);
        records.sort(sortAscending ? comparator : comparator.reversed());
        return records;
    }

    private int resolvePageIndex(List<LotRecord> filteredLots, String lotId, Integer requestedPageIndex) {
        int lotIndex = findRecordIndex(filteredLots, lotId);
        if (lotIndex >= 0) {
            return lotIndex / LOTS_PAGE_SIZE;
        }
        return clampPageIndex(requestedPageIndex == null ? currentLotPageIndex : requestedPageIndex, filteredLots.size());
    }

    private int clampPageIndex(int requestedPageIndex, int totalItems) {
        int totalPages = getTotalPages(totalItems);
        if (totalPages <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(requestedPageIndex, totalPages - 1));
    }

    private int getTotalPages(int totalItems) {
        return totalItems <= 0 ? 0 : ((totalItems - 1) / LOTS_PAGE_SIZE) + 1;
    }

    private List<LotRecord> paginateLotRecords(List<LotRecord> filteredLots, int pageIndex) {
        if (filteredLots.isEmpty()) {
            return new ArrayList<>();
        }
        int safePageIndex = clampPageIndex(pageIndex, filteredLots.size());
        int startIndex = safePageIndex * LOTS_PAGE_SIZE;
        int endIndex = Math.min(filteredLots.size(), startIndex + LOTS_PAGE_SIZE);
        return new ArrayList<>(filteredLots.subList(startIndex, endIndex));
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

    private int findRecordIndex(List<LotRecord> records, String lotId) {
        if (TextUtils.isEmpty(lotId)) {
            return -1;
        }
        for (int index = 0; index < records.size(); index++) {
            if (lotId.equals(records.get(index).details.lot.lotId)) {
                return index;
            }
        }
        return -1;
    }

    private LotRecord findRecord(List<LotRecord> records, String lotId) {
        if (TextUtils.isEmpty(lotId) || records == null) {
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
        if (filteredLots == null) return;

        for (int index = 0; index < filteredLots.size(); index++) {
            LotRecord record = filteredLots.get(index);
            boolean isSelected = TextUtils.equals(record.details.lot.lotId, selectedLotId);
            lotsTableLayout.addView(createLotRow(record, index, isSelected));
        }
    }

    private void updateDateRangeViews(long fromMillis, long toMillis) {
        String fromLabel = dateFormat.format(fromMillis);
        String toLabel = dateFormat.format(toMillis);
        fromDateFilterButton.setText(fromLabel);
        toDateFilterButton.setText(toLabel);
        filterRangeSummaryView.setText(getString(R.string.filter_range_summary, fromLabel, toLabel));
    }

    private void updateSortToggleButton() {
        if (lotSortToggleButton == null) return;
        lotSortToggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, lotSortAscending
                ? R.drawable.outline_arrow_upward_alt_24
                : R.drawable.outline_arrow_downward_alt_24, 0);
    }

    private void updateLotPaginationControls(int pageIndex, int totalItems) {
        int totalPages = getTotalPages(totalItems);
        if (totalPages <= 0) {
            lotPageSummaryView.setText(R.string.lot_page_summary_empty);
            previousLotPageButton.setEnabled(false);
            nextLotPageButton.setEnabled(false);
            return;
        }

        lotPageSummaryView.setText(getString(R.string.lot_page_summary, pageIndex + 1, totalPages));
        previousLotPageButton.setEnabled(pageIndex > 0);
        nextLotPageButton.setEnabled(pageIndex < totalPages - 1);
    }

    private TableRow createLotsHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(0xFFE0E0E0);
        row.addView(createLotSortToggleButton(getString(R.string.lot_table_header_date)));
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
        row.setOnLongClickListener(v -> {
            showDeleteLotConfirmation(record.details.lot.lotId);
            return true;
        });

        row.addView(createCell(dateFormat.format(record.details.lot.timestamp)
                .replaceAll(",?\\s*\\b\\d{4}\\b", "")
                .replaceAll("\\b\\d{4}\\b\\s*\\.?", "")
                .trim(), false));
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

    private Button createLotSortToggleButton(String text) {
        Button button = new Button(this);
        // Remove button styling to match TextView appearance
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
        lotSortToggleButton = button;
        lotSortToggleButton.setOnClickListener(v -> {
            lotSortAscending = !lotSortAscending;
            currentLotPageIndex = 0;
            updateSortToggleButton();
            refreshDashboardAsync(currentSelectedLotId, null, null, null, null, true);
        });
        updateSortToggleButton();

        return button;
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

    private void showDeleteLotConfirmation(String lotId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_lot_title)
                .setMessage(getString(R.string.dialog_delete_lot_message, abbreviateLotId(lotId)))
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> {
                    ioExecutor.execute(() -> {
                        try {
                            repository.deleteLot(lotId);
                            String message = getString(R.string.lot_deleted_message, abbreviateLotId(lotId));
                            refreshDashboardOnWorker(null, message, null, null, null, null, false);
                        } catch (Exception exception) {
                            postStatuses(exception.getMessage(), null, null, null);
                        }
                    });
                })
                .show();
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
        });
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

    private long startOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long endOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
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

    private static final class ActiveLotDateRange {
        private final long fromMillis;
        private final long toMillis;

        private ActiveLotDateRange(long fromMillis, long toMillis) {
            this.fromMillis = fromMillis;
            this.toMillis = toMillis;
        }
    }
}
