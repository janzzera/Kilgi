package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.kilgi.inventory.data.CustomerEntity;
import com.example.kilgi.inventory.data.CustomerLedgerSummary;
import com.example.kilgi.inventory.data.OpenCustomerInvoice;
import com.example.kilgi.inventory.data.OpenProviderLotPayable;
import com.example.kilgi.inventory.data.ProviderEntity;
import com.example.kilgi.inventory.data.ProviderLedgerSummary;
import com.example.kilgi.inventory.data.RetailSaleEntity;
import com.example.kilgi.inventory.data.WholesaleInvoiceEntity;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.repository.CustomerRepository;
import com.example.kilgi.inventory.repository.ProviderRepository;
import com.example.kilgi.inventory.repository.SalesRepository;
import com.example.kilgi.inventory.service.CustomerCollectionResult;
import com.example.kilgi.inventory.service.ProviderSettlementResult;
import com.example.kilgi.inventory.viewmodel.CustomerViewModel;
import com.example.kilgi.inventory.viewmodel.ProviderViewModel;
import com.example.kilgi.inventory.viewmodel.SalesViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SalesActivity extends AppCompatActivity {

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));

    private MaterialToolbar topAppBar;
    private TextView totalReceivablesView;
    private TextView totalPayablesView;
    private TextView salesStatusView;
    private TextView salesSummaryView;
    private BottomNavigationView bottomNavigationView;

    private CustomerViewModel customerViewModel;
    private ProviderViewModel providerViewModel;
    private SalesViewModel salesViewModel;

    private List<CustomerLedgerSummary> cachedCustomerSummaries = new ArrayList<>();
    private List<ProviderLedgerSummary> cachedProviderSummaries = new ArrayList<>();
    private List<CustomerEntity> cachedCustomers = new ArrayList<>();
    private List<ProviderEntity> cachedProviders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sales);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        customerViewModel = new ViewModelProvider(this).get(CustomerViewModel.class);
        providerViewModel = new ViewModelProvider(this).get(ProviderViewModel.class);
        salesViewModel = new ViewModelProvider(this).get(SalesViewModel.class);

        bindViews();
        setupNavigation();
        bindActions();
        observeViewModels();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MainActivity.isUserAuthenticated) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }
        bottomNavigationView.setSelectedItemId(R.id.nav_sales);
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
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
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

    private void observeViewModels() {
        customerViewModel.getLedgerSummaries().observe(this, summaries -> {
            if (summaries != null) {
                cachedCustomerSummaries = summaries;
                updateTotalsAndSummary();
            }
        });

        providerViewModel.getLedgerSummaries().observe(this, summaries -> {
            if (summaries != null) {
                cachedProviderSummaries = summaries;
                updateTotalsAndSummary();
            }
        });

        customerViewModel.getActiveCustomers().observe(this, customers -> {
            if (customers != null) {
                cachedCustomers = customers;
            }
        });

        providerViewModel.getActiveProviders().observe(this, providers -> {
            if (providers != null) {
                cachedProviders = providers;
            }
        });
    }

    private void updateTotalsAndSummary() {
        double totalReceivables = 0;
        for (CustomerLedgerSummary s : cachedCustomerSummaries) totalReceivables += s.outstandingBalance;

        double totalPayables = 0;
        for (ProviderLedgerSummary s : cachedProviderSummaries) totalPayables += s.outstandingBalance;

        totalReceivablesView.setText(currencyFormat.format(totalReceivables));
        totalPayablesView.setText(currencyFormat.format(totalPayables));
        salesSummaryView.setText(buildSalesSummaryText(cachedCustomerSummaries, cachedProviderSummaries));
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
                providerViewModel.createProvider(displayName, contact, address, notes, new ProviderRepository.Callback<ProviderEntity>() {
                    @Override
                    public void onSuccess(ProviderEntity provider) {
                        runOnUiThread(() -> salesStatusView.setText(getString(R.string.provider_created_message, provider.displayName)));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
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
                customerViewModel.createCustomer(displayName, contact, address, notes, new CustomerRepository.Callback<CustomerEntity>() {
                    @Override
                    public void onSuccess(CustomerEntity customer) {
                        runOnUiThread(() -> salesStatusView.setText(getString(R.string.customer_created_message, customer.displayName)));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
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
                salesViewModel.recordRetailSale(amount, notes, new SalesRepository.Callback<RetailSaleEntity>() {
                    @Override
                    public void onSuccess(RetailSaleEntity result) {
                        runOnUiThread(() -> salesStatusView.setText(getString(R.string.retail_sale_logged_message, currencyFormat.format(amount))));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        dialog.show();
    }

    private void showWholesaleInvoiceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_wholesale_invoice, null, false);
        Spinner customerSpinner = dialogView.findViewById(R.id.spinner_invoice_customer);
        EditText descriptionInput = dialogView.findViewById(R.id.edit_invoice_description);
        EditText amountInput = dialogView.findViewById(R.id.edit_invoice_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_invoice_notes);

        customerSpinner.setAdapter(buildCustomerAdapter(cachedCustomers));

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
                salesViewModel.createWholesaleInvoice(customer.customerId, description, amount, notes, new SalesRepository.Callback<WholesaleInvoiceEntity>() {
                    @Override
                    public void onSuccess(WholesaleInvoiceEntity invoice) {
                        runOnUiThread(() -> salesStatusView.setText(getString(R.string.wholesale_invoice_created_message, invoice.invoiceNumber, customer.displayName)));
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
        dialog.show();
    }

    private void showCustomerCollectionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_collect_customer_payment, null, false);
        Spinner customerSpinner = dialogView.findViewById(R.id.spinner_collection_customer);
        TextView breakdownView = dialogView.findViewById(R.id.text_customer_collection_breakdown);
        EditText amountInput = dialogView.findViewById(R.id.edit_customer_collection_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_customer_collection_notes);

        customerSpinner.setAdapter(buildCustomerAdapter(cachedCustomers));
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

                customerViewModel.getOpenInvoicesForCustomer(customer.customerId).observe(this, invoices -> {
                    if (invoices != null) {
                        dialog.dismiss();
                        customerViewModel.collectCustomerPayment(customer.customerId, amount, notes, invoices, new CustomerRepository.Callback<CustomerCollectionResult>() {
                            @Override
                            public void onSuccess(CustomerCollectionResult result) {
                                runOnUiThread(() -> salesStatusView.setText(getString(R.string.customer_collection_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), customer.displayName, result.allocations.size())));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
                            }
                        });
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
        dialog.show();
    }

    private void showProviderSettlementDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settle_provider_payment, null, false);
        Spinner providerSpinner = dialogView.findViewById(R.id.spinner_settlement_provider);
        TextView breakdownView = dialogView.findViewById(R.id.text_provider_settlement_breakdown);
        EditText amountInput = dialogView.findViewById(R.id.edit_provider_settlement_amount);
        EditText notesInput = dialogView.findViewById(R.id.edit_provider_settlement_notes);

        providerSpinner.setAdapter(buildProviderAdapter(cachedProviders));
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

                providerViewModel.getOpenLotPayablesForProvider(provider.providerId).observe(this, payables -> {
                    if (payables != null) {
                        dialog.dismiss();
                        providerViewModel.settleProviderBalance(provider.providerId, amount, notes, payables, new ProviderRepository.Callback<ProviderSettlementResult>() {
                            @Override
                            public void onSuccess(ProviderSettlementResult result) {
                                runOnUiThread(() -> salesStatusView.setText(getString(R.string.provider_settlement_logged_message, currencyFormat.format(result.getTotalAllocatedAmount()), provider.displayName, result.allocations.size())));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                runOnUiThread(() -> salesStatusView.setText(throwable.getMessage()));
                            }
                        });
                    }
                });
            } catch (IllegalArgumentException exception) {
                salesStatusView.setText(exception.getMessage());
            }
        }));
        
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
                customerViewModel.getOpenInvoicesForCustomer(customer.customerId).observe(SalesActivity.this, invoices -> {
                    String breakdown = buildCustomerBreakdownText(invoices);
                    breakdownView.setText(breakdown);
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
                providerViewModel.getOpenLotPayablesForProvider(provider.providerId).observe(SalesActivity.this, payables -> {
                    String breakdown = buildProviderBreakdownText(payables);
                    breakdownView.setText(breakdown);
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
