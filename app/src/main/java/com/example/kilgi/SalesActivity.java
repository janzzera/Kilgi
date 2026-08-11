package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.CustomerCollectionResult;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.example.kilgi.inventory.service.ProviderSettlementResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MaterialToolbar topAppBar;
    private TextView totalReceivablesView;
    private TextView totalPayablesView;
    private TextView salesStatusView;
    private TextView salesSummaryView;
    private BottomNavigationView bottomNavigationView;

    private ModuleOneRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sales);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Bottom padding handled by Nav
            return insets;
        });

        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        bindViews();
        setupNavigation();
        bindActions();
        refreshSalesData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.nav_sales);
        refreshSalesData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.top_app_bar);
        totalReceivablesView = findViewById(R.id.text_total_receivables);
        totalPayablesView = findViewById(R.id.text_total_payables);
        salesStatusView = findViewById(R.id.text_sales_status);
        salesSummaryView = findViewById(R.id.text_sales_summary);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_inventory) {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            } else if (itemId == R.id.nav_sales) {
                return true;
            } else if (itemId == R.id.nav_journal) {
                startActivity(new Intent(this, JournalActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            }
            return false;
        });
    }

    private void bindActions() {
        findViewById(R.id.button_add_provider).setOnClickListener(v -> showAddProviderDialog());
        findViewById(R.id.button_add_customer).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.button_log_retail_sale).setOnClickListener(v -> showRetailSaleDialog());
        findViewById(R.id.button_create_wholesale_invoice).setOnClickListener(v -> showWholesaleInvoiceDialog());
        findViewById(R.id.button_collect_customer_payment).setOnClickListener(v -> showCustomerCollectionDialog());
        findViewById(R.id.button_settle_provider).setOnClickListener(v -> showProviderSettlementDialog());
    }

    private void refreshSalesData() {
        ioExecutor.execute(() -> {
            List<CustomerLedgerSummary> customerSummaries = repository.getCustomerLedgerSummaries();
            List<ProviderLedgerSummary> providerSummaries = repository.getProviderLedgerSummaries();
            
            double totalReceivables = 0;
            for (CustomerLedgerSummary s : customerSummaries) totalReceivables += s.outstandingBalance;
            
            double totalPayables = 0;
            for (ProviderLedgerSummary s : providerSummaries) totalPayables += s.outstandingBalance;

            final double finalReceivables = totalReceivables;
            final double finalPayables = totalPayables;
            final String summaryText = buildSalesSummaryText(customerSummaries, providerSummaries);

            runOnUiThread(() -> {
                totalReceivablesView.setText(currencyFormat.format(finalReceivables));
                totalPayablesView.setText(currencyFormat.format(finalPayables));
                salesSummaryView.setText(summaryText);
            });
        });
    }

    private String buildSalesSummaryText(List<CustomerLedgerSummary> customerSummaries, List<ProviderLedgerSummary> providerSummaries) {
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        ProviderEntity provider = repository.createProvider(displayName, contact, address, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.provider_created_message, provider.displayName));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        CustomerEntity customer = repository.createCustomer(displayName, contact, address, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.customer_created_message, customer.displayName));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        repository.recordRetailSale(amount, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.retail_sale_logged_message, currencyFormat.format(amount)));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showWholesaleInvoiceDialog() {
        showWholesaleInvoiceDialog(new ArrayList<>());
    }

    private void showWholesaleInvoiceDialog(List<CustomerEntity> customers) {
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        var invoice = repository.createWholesaleInvoice(customer.customerId, description, amount, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.wholesale_invoice_created_message, invoice.invoiceNumber, customer.displayName));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
        if (customers.isEmpty()) {
            ioExecutor.execute(() -> {
                List<CustomerEntity> fetched = repository.getCustomers();
                runOnUiThread(() -> customerSpinner.setAdapter(buildCustomerAdapter(fetched)));
            });
        }
        
        dialog.show();
    }

    private void showCustomerCollectionDialog() {
        showCustomerCollectionDialog(new ArrayList<>());
    }

    private void showCustomerCollectionDialog(List<CustomerEntity> customers) {
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        CustomerCollectionResult result = repository.collectCustomerPayment(customer.customerId, amount, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.customer_collection_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), customer.displayName, result.allocations.size()));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
        if (customers.isEmpty()) {
            ioExecutor.execute(() -> {
                List<CustomerEntity> fetched = repository.getCustomers();
                runOnUiThread(() -> customerSpinner.setAdapter(buildCustomerAdapter(fetched)));
            });
        }
        
        dialog.show();
    }

    private void showProviderSettlementDialog() {
        showProviderSettlementDialog(new ArrayList<>());
    }

    private void showProviderSettlementDialog(List<ProviderEntity> providers) {
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
                dialog.dismiss();
                ioExecutor.execute(() -> {
                    try {
                        ProviderSettlementResult result = repository.settleProviderBalance(provider.providerId, amount, notes);
                        runOnUiThread(() -> {
                            salesStatusView.setText(getString(R.string.provider_settlement_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), provider.displayName, result.allocations.size()));
                            refreshSalesData();
                        });
                    } catch (Exception exception) {
                        runOnUiThread(() -> salesStatusView.setText(exception.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
        if (providers.isEmpty()) {
            ioExecutor.execute(() -> {
                List<ProviderEntity> fetched = repository.getProviders();
                runOnUiThread(() -> providerSpinner.setAdapter(buildProviderAdapter(fetched)));
            });
        }
        
        dialog.show();
    }

    private ArrayAdapter<ProviderEntity> buildProviderAdapter(List<ProviderEntity> providers) {
        ArrayAdapter<ProviderEntity> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, providers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter<CustomerEntity> buildCustomerAdapter(List<CustomerEntity> customers) {
        ArrayAdapter<CustomerEntity> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, customers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bindCustomerBreakdownLoader(Spinner spinner, TextView breakdownView) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CustomerEntity customer = (CustomerEntity) parent.getItemAtPosition(position);
                if (customer == null) return;
                ioExecutor.execute(() -> {
                    List<OpenCustomerInvoice> invoices = repository.getOpenInvoicesForCustomer(customer.customerId);
                    String breakdown = buildCustomerBreakdownText(invoices);
                    runOnUiThread(() -> breakdownView.setText(breakdown));
                });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindProviderBreakdownLoader(Spinner spinner, TextView breakdownView) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ProviderEntity provider = (ProviderEntity) parent.getItemAtPosition(position);
                if (provider == null) return;
                ioExecutor.execute(() -> {
                    List<OpenProviderLotPayable> payables = repository.getOpenLotPayablesForProvider(provider.providerId);
                    String breakdown = buildProviderBreakdownText(payables);
                    runOnUiThread(() -> breakdownView.setText(breakdown));
                });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private String buildCustomerBreakdownText(List<OpenCustomerInvoice> invoices) {
        if (invoices == null || invoices.isEmpty()) return getString(R.string.sales_summary_empty);
        StringBuilder builder = new StringBuilder(getString(R.string.customer_invoice_breakdown_title));
        for (OpenCustomerInvoice invoice : invoices) {
            builder.append("\n").append(getString(R.string.customer_invoice_breakdown_line, invoice.invoiceNumber, invoice.description, currencyFormat.format(invoice.outstandingBalance)));
        }
        return builder.toString();
    }

    private String buildProviderBreakdownText(List<OpenProviderLotPayable> payables) {
        if (payables == null || payables.isEmpty()) return getString(R.string.sales_summary_empty);
        StringBuilder builder = new StringBuilder(getString(R.string.provider_payable_breakdown_title));
        for (OpenProviderLotPayable payable : payables) {
            builder.append("\n").append(getString(R.string.provider_payable_breakdown_line, abbreviateLotId(payable.lotId), payable.vegetableType, currencyFormat.format(payable.outstandingBalance)));
        }
        return builder.toString();
    }

    private String abbreviateLotId(String lotId) {
        if (TextUtils.isEmpty(lotId)) return "-";
        return lotId.length() <= 8 ? lotId : lotId.substring(0, 8);
    }
}
