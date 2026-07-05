package com.example.kilgi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.model.MeasuredWholesaleUnits;
import com.example.kilgi.inventory.model.ProcurementBatch;
import com.example.kilgi.inventory.model.UnitConversion;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private EditText productNameInput;
    private EditText providerNameInput;
    private EditText unitNameInput;
    private EditText unitWeightsInput;
    private EditText baseItemPriceInput;
    private EditText shippingFeesInput;
    private EditText storeDeliveryFeesInput;
    private EditText rawSortingLaborInput;
    private EditText packagingMaterialCostInput;
    private EditText spoilageWeightInput;
    private EditText spoilageReasonInput;
    private TextView batchStatusView;
    private TextView spoilageStatusView;
    private TextView batchSummaryView;

    private ProcurementBatch activeBatch;

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

        bindViews();
        prefillSampleForm();
        bindActions();
        renderBatchSummary();
    }

    private void bindViews() {
        productNameInput = findViewById(R.id.edit_product_name);
        providerNameInput = findViewById(R.id.edit_provider_name);
        unitNameInput = findViewById(R.id.edit_unit_name);
        unitWeightsInput = findViewById(R.id.edit_unit_weights);
        baseItemPriceInput = findViewById(R.id.edit_base_item_price);
        shippingFeesInput = findViewById(R.id.edit_shipping_fees);
        storeDeliveryFeesInput = findViewById(R.id.edit_store_delivery_fees);
        rawSortingLaborInput = findViewById(R.id.edit_raw_sorting_labor);
        packagingMaterialCostInput = findViewById(R.id.edit_packaging_material_cost);
        spoilageWeightInput = findViewById(R.id.edit_spoilage_weight);
        spoilageReasonInput = findViewById(R.id.edit_spoilage_reason);
        batchStatusView = findViewById(R.id.text_batch_status);
        spoilageStatusView = findViewById(R.id.text_spoilage_status);
        batchSummaryView = findViewById(R.id.text_batch_summary);
    }

    private void prefillSampleForm() {
        productNameInput.setText("Tomato");
        providerNameInput.setText("Davao Growers Cooperative");
        unitNameInput.setText("sack");
        unitWeightsInput.setText("24.0, 26.5, 23.5, 26.0");
        baseItemPriceInput.setText("5000");
        shippingFeesInput.setText("300");
        storeDeliveryFeesInput.setText("200");
        rawSortingLaborInput.setText("150");
        packagingMaterialCostInput.setText("50");
        spoilageWeightInput.setText("10");
        spoilageReasonInput.setText("Rotten tomatoes");
    }

    private void bindActions() {
        Button createBatchButton = findViewById(R.id.button_create_batch);
        Button logSpoilageButton = findViewById(R.id.button_log_spoilage);

        createBatchButton.setOnClickListener(v -> createOrRecalculateBatch());
        logSpoilageButton.setOnClickListener(v -> logSpoilage());
    }

    private void createOrRecalculateBatch() {
        try {
            String productName = InventoryInputParser.requireText(productNameInput.getText().toString(), "Product name");
            String providerName = InventoryInputParser.requireText(providerNameInput.getText().toString(), "Provider / supplier name");
            String unitName = InventoryInputParser.requireText(unitNameInput.getText().toString(), "Wholesale unit name");
            List<Double> unitWeightsKg = InventoryInputParser.parseMeasuredWeights(unitWeightsInput.getText().toString(), "Unit weights");
            double baseItemPrice = InventoryInputParser.parseOptionalNonNegativeDouble(baseItemPriceInput.getText().toString(), "Base item price");
            double shippingFees = InventoryInputParser.parseOptionalNonNegativeDouble(shippingFeesInput.getText().toString(), "Shipping fees");
            double storeDeliveryFees = InventoryInputParser.parseOptionalNonNegativeDouble(storeDeliveryFeesInput.getText().toString(), "Store delivery fees");
            double rawSortingLabor = InventoryInputParser.parseOptionalNonNegativeDouble(rawSortingLaborInput.getText().toString(), "Raw sorting labor cost");
            double packagingMaterialCost = InventoryInputParser.parseOptionalNonNegativeDouble(packagingMaterialCostInput.getText().toString(), "Plastic / sack material cost");

            activeBatch = new ProcurementBatch(
                    productName,
                    providerName,
                    new MeasuredWholesaleUnits(unitName, unitWeightsKg),
                    new UnitConversion(unitName),
                    baseItemPrice,
                    shippingFees,
                    storeDeliveryFees,
                    rawSortingLabor,
                    packagingMaterialCost
            );

            batchStatusView.setText(getString(R.string.batch_created_message));
            spoilageStatusView.setText(getString(R.string.spoilage_status_idle));
            renderBatchSummary();
        } catch (IllegalArgumentException exception) {
            batchStatusView.setText(exception.getMessage());
        }
    }

    private void logSpoilage() {
        if (activeBatch == null) {
            spoilageStatusView.setText(getString(R.string.error_no_active_batch));
            return;
        }

        try {
            double spoilageWeightKg = InventoryInputParser.parseRequiredPositiveDouble(
                    spoilageWeightInput.getText().toString(),
                    "Spoilage weight"
            );
            String spoilageReason = spoilageReasonInput.getText().toString();

            activeBatch.logSpoilage(spoilageWeightKg, spoilageReason);
            spoilageStatusView.setText(getString(R.string.spoilage_logged_message));
            spoilageWeightInput.setText("");
            spoilageReasonInput.setText("");
            renderBatchSummary();
        } catch (IllegalArgumentException exception) {
            spoilageStatusView.setText(exception.getMessage());
        }
    }

    private void renderBatchSummary() {
        if (activeBatch == null) {
            batchSummaryView.setText(getString(R.string.results_placeholder));
            return;
        }

        String unitLabel = activeBatch.getUnitConversion().getUnitName();
        if (activeBatch.getPurchasedUnitCount() != 1) {
            unitLabel = unitLabel + "s";
        }

        String summary = "Product: " + activeBatch.getProductName()
                + "\nProvider: " + activeBatch.getProviderName()
                + "\nPurchased units: " + activeBatch.getPurchasedUnitCount() + " " + unitLabel
                + "\nMeasured unit weights: " + activeBatch.getPurchasedUnitWeightsKg()
                + "\nInitial weight: " + formatWeight(activeBatch.getInitialWeightKg())
                + "\nAverage unit weight: " + formatWeight(activeBatch.getAverageUnitWeightKg())
                + "\nTotal landed cost: " + currencyFormat.format(activeBatch.getTotalLandedCost())
                + "\nInitial cost per kilo: " + currencyFormat.format(activeBatch.getInitialCostPerKilo())
                + "\nTotal spoilage: " + formatWeight(activeBatch.getTotalSpoilageWeightKg())
                + "\nRemaining sellable weight: " + formatWeight(activeBatch.getRemainingSellableWeightKg())
                + "\nTrue cost per kilo: " + formatCurrency(activeBatch);

        batchSummaryView.setText(summary);
    }

    private String formatWeight(double weightKg) {
        return String.format(Locale.US, "%.2f kg", weightKg);
    }

    private String formatCurrency(ProcurementBatch batch) {
        if (!batch.hasSellableInventory()) {
            return "N/A - no sellable inventory left";
        }
        return currencyFormat.format(batch.getTrueCostPerKilo());
    }
}