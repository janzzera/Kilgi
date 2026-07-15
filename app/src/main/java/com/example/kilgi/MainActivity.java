package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
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

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LossType;
import com.example.kilgi.inventory.data.LotEntity;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.data.PaymentSource;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.BatchValuationEngine;
import com.example.kilgi.inventory.service.BatchValuationSnapshot;
import com.example.kilgi.inventory.service.LotFilterUtils;
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

public class MainActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MaterialToolbar topAppBar;
    private ScrollView contentScrollView;
    private Spinner monthFilterSpinner;
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
    private View lotSectionView;

    private ModuleOneRepository repository;
    private String initialLotId;
    private String currentSelectedLotId;

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
        lotSectionView = findViewById(R.id.section_lot);
    }

    private void setupTopAppBar() {
        topAppBar.setTitle(R.string.lot_screen_title);
        topAppBar.inflateMenu(R.menu.main_sections_menu);
        topAppBar.setOnMenuItemClickListener(item -> handleMenuNavigation(item.getItemId()));
    }

    private void setupFilterSpinners() {
        monthFilterSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildMonthOptions()
        ));
        ((ArrayAdapter<?>) monthFilterSpinner.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        dayFilterSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildDayOptions()
        ));
        ((ArrayAdapter<?>) dayFilterSpinner.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Calendar calendar = Calendar.getInstance();
        monthFilterSpinner.setSelection(calendar.get(Calendar.MONTH) + 1);
        dayFilterSpinner.setSelection(0);
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

    private List<String> buildDayOptions() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.filter_all_days));
        for (int day = 1; day <= 31; day++) {
            options.add(String.valueOf(day));
        }
        return options;
    }

    private void bindActions() {
        Button createLotButton = findViewById(R.id.button_create_lot);
        Button applyFilterButton = findViewById(R.id.button_apply_lot_filter);

        createLotButton.setOnClickListener(v -> showCreateLotDialog());
        applyFilterButton.setOnClickListener(v -> refreshDashboardAsync(currentSelectedLotId, null, null, null));
        addExpenseButton.setOnClickListener(v -> showAddExpenseDialog());
        logSpoilageButton.setOnClickListener(v -> showLogSpoilageDialog());
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
        int monthFilter = getSelectedMonthFilter();
        int dayFilter = getSelectedDayFilter();
        ioExecutor.execute(() -> {
            String targetLotId = initialLotId;
            if (TextUtils.isEmpty(targetLotId)) {
                LotEntity latestLot = repository.getLatestLot();
                targetLotId = latestLot == null ? null : latestLot.lotId;
            }
            refreshDashboardOnWorker(targetLotId, null, null, null, monthFilter, dayFilter);
        });
    }

    private void refreshDashboardAsync(String targetLotId, String batchStatus, String expenseStatus, String spoilageStatus) {
        int monthFilter = getSelectedMonthFilter();
        int dayFilter = getSelectedDayFilter();
        ioExecutor.execute(() -> refreshDashboardOnWorker(
                targetLotId,
                batchStatus,
                expenseStatus,
                spoilageStatus,
                monthFilter,
                dayFilter
        ));
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
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_lot, null, false);
        EditText providerIdInput = dialogView.findViewById(R.id.edit_provider_id);
        EditText providerNameInput = dialogView.findViewById(R.id.edit_provider_name);
        EditText vegetableTypeInput = dialogView.findViewById(R.id.edit_vegetable_type);
        EditText totalSacksInput = dialogView.findViewById(R.id.edit_total_sacks);
        EditText rawKilosInput = dialogView.findViewById(R.id.edit_raw_kilos);
        EditText baseUnitPriceInput = dialogView.findViewById(R.id.edit_base_unit_price);
        Spinner purchaseSourceSpinner = dialogView.findViewById(R.id.spinner_purchase_payment_source);
        EditText standardFreightInput = dialogView.findViewById(R.id.edit_standard_freight);
        Spinner freightSourceSpinner = dialogView.findViewById(R.id.spinner_freight_payment_source);

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
                String providerId = InventoryInputParser.requireText(providerIdInput.getText().toString(), "Provider ID");
                String providerName = InventoryInputParser.requireText(providerNameInput.getText().toString(), "Provider name");
                String vegetableType = InventoryInputParser.requireText(vegetableTypeInput.getText().toString(), "Vegetable type");
                int totalSacks = InventoryInputParser.parseRequiredPositiveInt(totalSacksInput.getText().toString(), "Total sacks purchased");
                double rawKilos = InventoryInputParser.parseRequiredPositiveDouble(rawKilosInput.getText().toString(), "Raw kilograms received");
                double baseUnitPrice = InventoryInputParser.parseOptionalNonNegativeDouble(baseUnitPriceInput.getText().toString(), "Base unit price per kilo");
                double standardFreight = InventoryInputParser.parseOptionalNonNegativeDouble(standardFreightInput.getText().toString(), "Standard freight");
                PaymentSource purchaseSource = (PaymentSource) purchaseSourceSpinner.getSelectedItem();
                PaymentSource freightSource = (PaymentSource) freightSourceSpinner.getSelectedItem();
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();

                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        LotEntity lot = repository.createLot(
                                providerId,
                                providerName,
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
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(exception.getMessage(), null, null);
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
        EditText expenseLabelInput = dialogView.findViewById(R.id.edit_expense_label);
        EditText expenseAmountInput = dialogView.findViewById(R.id.edit_expense_amount);
        Spinner paymentSourceSpinner = dialogView.findViewById(R.id.spinner_expense_payment_source);

        selectedLotView.setText(getString(R.string.dialog_selected_lot, abbreviateLotId(currentSelectedLotId), currentSelectedLotId));
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
                String expenseLabel = InventoryInputParser.requireText(expenseLabelInput.getText().toString(), "Expense label");
                double amount = InventoryInputParser.parseRequiredPositiveDouble(expenseAmountInput.getText().toString(), "Expense amount");
                PaymentSource paymentSource = (PaymentSource) paymentSourceSpinner.getSelectedItem();
                String lotId = currentSelectedLotId;
                int monthFilter = getSelectedMonthFilter();
                int dayFilter = getSelectedDayFilter();

                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        repository.addExpense(lotId, expenseLabel, amount, paymentSource);
                        refreshDashboardOnWorker(
                                lotId,
                                null,
                                getString(R.string.expense_added_message, expenseLabel),
                                null,
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, exception.getMessage(), null);
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
                                monthFilter,
                                dayFilter
                        );
                    } catch (Exception exception) {
                        postStatuses(null, null, exception.getMessage());
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
            int monthFilter,
            int dayFilter
    ) {
        try {
            List<LotRecord> filteredLots = buildLotRecords(repository.getAllLots(), monthFilter, dayFilter);
            String effectiveLotId = resolveSelectedLotId(targetLotId, filteredLots);
            LotRecord selectedRecord = findRecord(filteredLots, effectiveLotId);
            String batchMessage = batchStatus == null
                    ? getString(R.string.lots_status_count, filteredLots.size())
                    : batchStatus;

            runOnUiThread(() -> {
                currentSelectedLotId = selectedRecord == null ? null : selectedRecord.details.lot.lotId;
                batchStatusView.setText(batchMessage);
                if (expenseStatus != null) {
                    expenseStatusView.setText(expenseStatus);
                }
                if (spoilageStatus != null) {
                    spoilageStatusView.setText(spoilageStatus);
                }
                renderLotsTable(filteredLots, currentSelectedLotId);
                renderSelectedLotDetails(selectedRecord);
                lotsEmptyStateView.setVisibility(filteredLots.isEmpty() ? View.VISIBLE : View.GONE);
                lotsEmptyStateView.setText(filteredLots.isEmpty()
                        ? getString(R.string.no_lots_filtered_placeholder)
                        : getString(R.string.no_lots_placeholder));
                updateLotActionButtons(selectedRecord != null);
            });
        } catch (Exception exception) {
            postStatuses(exception.getMessage(), exception.getMessage(), exception.getMessage());
        }
    }

    private List<LotRecord> buildLotRecords(List<LotEntity> lots, int monthFilter, int dayFilter) {
        List<LotRecord> records = new ArrayList<>();
        for (LotEntity lot : lots) {
            if (!LotFilterUtils.matchesMonthAndDay(lot.timestamp, monthFilter, dayFilter)) {
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

    private void postStatuses(String batchStatus, String expenseStatus, String spoilageStatus) {
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
}

