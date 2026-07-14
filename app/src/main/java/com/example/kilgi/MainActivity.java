package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MaterialToolbar topAppBar;
    private ScrollView contentScrollView;
    private EditText providerIdInput;
    private EditText providerNameInput;
    private EditText vegetableTypeInput;
    private EditText totalSacksInput;
    private EditText rawKilosInput;
    private EditText baseUnitPriceInput;
    private EditText standardFreightInput;
    private EditText selectedLotIdInput;
    private EditText expenseLabelInput;
    private EditText expenseAmountInput;
    private EditText spoilageKilosInput;
    private Spinner purchasePaymentSourceSpinner;
    private Spinner freightPaymentSourceSpinner;
    private Spinner expensePaymentSourceSpinner;
    private Spinner lossTypeSpinner;
    private TextView batchStatusView;
    private TextView expenseStatusView;
    private TextView spoilageStatusView;
    private TextView activeLotSummaryView;
    private TextView lotsOverviewView;
    private View lotSectionView;

    private ModuleOneRepository repository;
    private String initialLotId;

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
        setupSpinners();
        prefillSampleForm();
        bindActions();
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
        providerIdInput = findViewById(R.id.edit_provider_id);
        providerNameInput = findViewById(R.id.edit_provider_name);
        vegetableTypeInput = findViewById(R.id.edit_vegetable_type);
        totalSacksInput = findViewById(R.id.edit_total_sacks);
        rawKilosInput = findViewById(R.id.edit_raw_kilos);
        baseUnitPriceInput = findViewById(R.id.edit_base_unit_price);
        standardFreightInput = findViewById(R.id.edit_standard_freight);
        selectedLotIdInput = findViewById(R.id.edit_selected_lot_id);
        expenseLabelInput = findViewById(R.id.edit_expense_label);
        expenseAmountInput = findViewById(R.id.edit_expense_amount);
        spoilageKilosInput = findViewById(R.id.edit_spoilage_kilos);
        purchasePaymentSourceSpinner = findViewById(R.id.spinner_purchase_payment_source);
        freightPaymentSourceSpinner = findViewById(R.id.spinner_freight_payment_source);
        expensePaymentSourceSpinner = findViewById(R.id.spinner_expense_payment_source);
        lossTypeSpinner = findViewById(R.id.spinner_loss_type);
        batchStatusView = findViewById(R.id.text_batch_status);
        expenseStatusView = findViewById(R.id.text_expense_status);
        spoilageStatusView = findViewById(R.id.text_spoilage_status);
        activeLotSummaryView = findViewById(R.id.text_active_lot_summary);
        lotsOverviewView = findViewById(R.id.text_all_lots_summary);
        lotSectionView = findViewById(R.id.section_lot);
    }

    private void setupTopAppBar() {
        topAppBar.setTitle(R.string.lot_screen_title);
        topAppBar.inflateMenu(R.menu.main_sections_menu);
        topAppBar.setOnMenuItemClickListener(item -> handleMenuNavigation(item.getItemId()));
    }

    private void setupSpinners() {
        ArrayAdapter<PaymentSource> paymentSourceAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                PaymentSource.values()
        );
        paymentSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        purchasePaymentSourceSpinner.setAdapter(paymentSourceAdapter);
        freightPaymentSourceSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                PaymentSource.values()
        ));
        ((ArrayAdapter<?>) freightPaymentSourceSpinner.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        expensePaymentSourceSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                PaymentSource.values()
        ));
        ((ArrayAdapter<?>) expensePaymentSourceSpinner.getAdapter())
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<LossType> lossTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                LossType.values()
        );
        lossTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lossTypeSpinner.setAdapter(lossTypeAdapter);

        purchasePaymentSourceSpinner.setSelection(PaymentSource.ACCOUNTS_PAYABLE.ordinal());
        freightPaymentSourceSpinner.setSelection(PaymentSource.CASH.ordinal());
        expensePaymentSourceSpinner.setSelection(PaymentSource.CASH.ordinal());
        lossTypeSpinner.setSelection(LossType.NORMAL.ordinal());
    }

    private void prefillSampleForm() {
        providerIdInput.setText("PROV-XYZ");
        providerNameInput.setText("Provider XYZ");
        vegetableTypeInput.setText("Tomatoes");
        totalSacksInput.setText("4");
        rawKilosInput.setText("100");
        baseUnitPriceInput.setText("1.50");
        standardFreightInput.setText("20");
        expenseLabelInput.setText("Market porter fee");
        expenseAmountInput.setText("10");
        spoilageKilosInput.setText("15");
    }

    private void bindActions() {
        Button createLotButton = findViewById(R.id.button_create_lot);
        Button refreshLotButton = findViewById(R.id.button_refresh_lot);
        Button addExpenseButton = findViewById(R.id.button_add_expense);
        Button logSpoilageButton = findViewById(R.id.button_log_spoilage);

        createLotButton.setOnClickListener(v -> createLot());
        refreshLotButton.setOnClickListener(v -> refreshSelectedLot());
        addExpenseButton.setOnClickListener(v -> addExpense());
        logSpoilageButton.setOnClickListener(v -> logSpoilage());
    }

    private boolean handleMenuNavigation(int itemId) {
        if (itemId == R.id.menu_lot) {
            scrollToSection(lotSectionView);
            return true;
        }
        if (itemId == R.id.menu_journal) {
            Intent intent = new Intent(this, JournalActivity.class);
            String selectedLotId = selectedLotIdInput.getText().toString().trim();
            if (!selectedLotId.isEmpty()) {
                intent.putExtra(JournalActivity.EXTRA_LOT_ID, selectedLotId);
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
        ioExecutor.execute(() -> {
            String targetLotId = initialLotId;
            if (targetLotId == null || targetLotId.trim().isEmpty()) {
                LotEntity latestLot = repository.getLatestLot();
                targetLotId = latestLot == null ? null : latestLot.lotId;
            }
            refreshDashboardOnWorker(targetLotId, null, null, null);
        });
    }

    private void createLot() {
        try {
            String providerId = InventoryInputParser.requireText(providerIdInput.getText().toString(), "Provider ID");
            String providerName = InventoryInputParser.requireText(providerNameInput.getText().toString(), "Provider name");
            String vegetableType = InventoryInputParser.requireText(vegetableTypeInput.getText().toString(), "Vegetable type");
            int totalSacks = InventoryInputParser.parseRequiredPositiveInt(totalSacksInput.getText().toString(), "Total sacks purchased");
            double rawKilos = InventoryInputParser.parseRequiredPositiveDouble(rawKilosInput.getText().toString(), "Raw kilograms received");
            double baseUnitPrice = InventoryInputParser.parseOptionalNonNegativeDouble(baseUnitPriceInput.getText().toString(), "Base unit price per kilo");
            double standardFreight = InventoryInputParser.parseOptionalNonNegativeDouble(standardFreightInput.getText().toString(), "Standard freight");
            PaymentSource purchaseSource = (PaymentSource) purchasePaymentSourceSpinner.getSelectedItem();
            PaymentSource freightSource = (PaymentSource) freightPaymentSourceSpinner.getSelectedItem();

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
                            getString(R.string.batch_created_message, lot.lotId),
                            getString(R.string.expense_status_idle),
                            getString(R.string.spoilage_status_idle)
                    );
                } catch (Exception exception) {
                    postStatuses(exception.getMessage(), null, null);
                }
            });
        } catch (IllegalArgumentException exception) {
            batchStatusView.setText(exception.getMessage());
        }
    }

    private void refreshSelectedLot() {
        try {
            String lotId = InventoryInputParser.requireText(selectedLotIdInput.getText().toString(), "Selected lot ID");
            ioExecutor.execute(() -> refreshDashboardOnWorker(
                    lotId,
                    getString(R.string.batch_status_loaded, lotId),
                    null,
                    null
            ));
        } catch (IllegalArgumentException exception) {
            batchStatusView.setText(exception.getMessage());
        }
    }

    private void addExpense() {
        try {
            String lotId = InventoryInputParser.requireText(selectedLotIdInput.getText().toString(), "Selected lot ID");
            String expenseLabel = InventoryInputParser.requireText(expenseLabelInput.getText().toString(), "Expense label");
            double amount = InventoryInputParser.parseRequiredPositiveDouble(expenseAmountInput.getText().toString(), "Expense amount");
            PaymentSource paymentSource = (PaymentSource) expensePaymentSourceSpinner.getSelectedItem();

            ioExecutor.execute(() -> {
                try {
                    repository.addExpense(lotId, expenseLabel, amount, paymentSource);
                    refreshDashboardOnWorker(
                            lotId,
                            null,
                            getString(R.string.expense_added_message, expenseLabel),
                            null
                    );
                } catch (Exception exception) {
                    postStatuses(null, exception.getMessage(), null);
                }
            });
        } catch (IllegalArgumentException exception) {
            expenseStatusView.setText(exception.getMessage());
        }
    }

    private void logSpoilage() {
        try {
            String lotId = InventoryInputParser.requireText(selectedLotIdInput.getText().toString(), "Selected lot ID");
            double kilosLost = InventoryInputParser.parseRequiredPositiveDouble(spoilageKilosInput.getText().toString(), "Spoilage kilos");
            LossType lossType = (LossType) lossTypeSpinner.getSelectedItem();

            ioExecutor.execute(() -> {
                try {
                    repository.logSpoilage(lotId, kilosLost, lossType);
                    String message = lossType == LossType.NORMAL
                            ? getString(R.string.spoilage_logged_message, formatWeight(kilosLost))
                            : getString(R.string.abnormal_spoilage_logged_message, formatWeight(kilosLost));
                    refreshDashboardOnWorker(lotId, null, null, message);
                } catch (Exception exception) {
                    postStatuses(null, null, exception.getMessage());
                }
            });
        } catch (IllegalArgumentException exception) {
            spoilageStatusView.setText(exception.getMessage());
        }
    }

    private void refreshDashboardOnWorker(
            String targetLotId,
            String batchStatus,
            String expenseStatus,
            String spoilageStatus
    ) {
        try {
            List<LotEntity> allLots = repository.getAllLots();
            LotEntity latestLot = repository.getLatestLot();
            String effectiveLotId = targetLotId;
            if (effectiveLotId == null && latestLot != null) {
                effectiveLotId = latestLot.lotId;
            }

            String activeSummary = getString(R.string.no_active_lot_placeholder);
            if (effectiveLotId != null && !effectiveLotId.trim().isEmpty()) {
                LotWithDetails lotWithDetails = repository.getLotWithDetails(effectiveLotId);
                BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(
                        lotWithDetails.lot,
                        lotWithDetails.expenses,
                        lotWithDetails.spoilageLogs
                );
                activeSummary = buildActiveLotSummary(lotWithDetails, snapshot);
            }

            String lotsOverview = buildLotsOverview(allLots);
            String finalEffectiveLotId = effectiveLotId;
            String finalActiveSummary = activeSummary;
            runOnUiThread(() -> {
                if (finalEffectiveLotId != null) {
                    selectedLotIdInput.setText(finalEffectiveLotId);
                }
                if (batchStatus != null) {
                    batchStatusView.setText(batchStatus);
                }
                if (expenseStatus != null) {
                    expenseStatusView.setText(expenseStatus);
                    expenseLabelInput.setText("");
                    expenseAmountInput.setText("");
                }
                if (spoilageStatus != null) {
                    spoilageStatusView.setText(spoilageStatus);
                    spoilageKilosInput.setText("");
                }
                activeLotSummaryView.setText(finalActiveSummary);
                lotsOverviewView.setText(lotsOverview);
            });
        } catch (Exception exception) {
            postStatuses(exception.getMessage(), exception.getMessage(), exception.getMessage());
        }
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

    private String buildActiveLotSummary(LotWithDetails lotWithDetails, BatchValuationSnapshot snapshot) {
        LotEntity lot = lotWithDetails.lot;
        double additionalExpenseTotal = 0;
        for (int index = 0; index < lotWithDetails.expenses.size(); index++) {
            additionalExpenseTotal += lotWithDetails.expenses.get(index).amount;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Lot ID: ").append(lot.lotId)
                .append("\nProvider: ").append(lot.providerName).append(" (").append(lot.providerId).append(")")
                .append("\nVegetable: ").append(lot.vegetableType)
                .append("\nSacks purchased: ").append(lot.totalSacksPurchased)
                .append("\nRaw kilos received: ").append(formatWeight(lot.rawKilosReceived))
                .append("\nBase unit price: ").append(currencyFormat.format(lot.baseUnitPrice)).append(" / kg")
                .append("\nPurchase payment source: ").append(lot.purchasePaymentSource)
                .append("\nFreight payment source: ").append(lot.freightPaymentSource)
                .append("\nPurchase cost: ").append(currencyFormat.format(snapshot.getPurchaseCost()))
                .append("\nStandard freight: ").append(currencyFormat.format(lot.standardFreight))
                .append("\nAdditional expenses count: ").append(lotWithDetails.expenses.size())
                .append("\nAdditional expenses total: ").append(currencyFormat.format(additionalExpenseTotal))
                .append("\nNormal spoilage: ").append(formatWeight(snapshot.getNormalLossKilos()))
                .append("\nAbnormal spoilage: ").append(formatWeight(snapshot.getAbnormalLossKilos()))
                .append("\nAbnormal write-off value: ").append(currencyFormat.format(snapshot.getAbnormalWriteOffValue()))
                .append("\nTotal capitalized cost: ").append(currencyFormat.format(snapshot.getTotalCapitalizedCost()))
                .append("\nNet usable kilograms: ").append(formatWeight(snapshot.getNetUsableKilograms()))
                .append("\nTrue cost per kilo: ").append(formatCurrency(snapshot.getTrueCostPerKilo()));
        return builder.toString();
    }

    private String buildLotsOverview(List<LotEntity> lots) {
        if (lots == null || lots.isEmpty()) {
            return getString(R.string.no_lots_placeholder);
        }

        StringBuilder builder = new StringBuilder();
        for (LotEntity lot : lots) {
            try {
                LotWithDetails lotWithDetails = repository.getLotWithDetails(lot.lotId);
                BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(
                        lotWithDetails.lot,
                        lotWithDetails.expenses,
                        lotWithDetails.spoilageLogs
                );
                builder.append("• ")
                        .append(lot.lotId, 0, Math.min(8, lot.lotId.length()))
                        .append(" | ")
                        .append(lot.vegetableType)
                        .append(" | usable ")
                        .append(formatWeight(snapshot.getNetUsableKilograms()))
                        .append(" | cap cost ")
                        .append(currencyFormat.format(snapshot.getTotalCapitalizedCost()))
                        .append("\n");
            } catch (Exception exception) {
                builder.append("• ").append(lot.lotId).append(" | ").append(exception.getMessage()).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String formatWeight(double kilos) {
        return String.format(Locale.US, "%.2f kg", kilos);
    }

    private String formatCurrency(Double value) {
        if (value == null) {
            return getString(R.string.no_sellable_inventory);
        }
        return currencyFormat.format(value);
    }
}