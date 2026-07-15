package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.accounting.AccountingAccount;
import com.example.kilgi.inventory.accounting.AccountingCatalog;
import com.example.kilgi.inventory.data.JournalEntryWithLines;
import com.example.kilgi.inventory.data.JournalLineEntity;
import com.example.kilgi.inventory.data.JournalLineType;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.LotWithDetails;
import com.example.kilgi.inventory.input.InventoryInputParser;
import com.example.kilgi.inventory.service.BatchValuationEngine;
import com.example.kilgi.inventory.service.BatchValuationSnapshot;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.text.NumberFormat;
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
    private EditText selectedLotIdInput;
    private TextView journalStatusView;
    private TextView journalLotSummaryView;
    private TextView chartOfAccountsView;
    private TextView journalEmptyStateView;
    private LinearLayout journalEntriesContainer;

    private ModuleOneRepository repository;
    private String initialLotId;

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
        selectedLotIdInput = findViewById(R.id.edit_selected_lot_id);
        journalStatusView = findViewById(R.id.text_journal_status);
        journalLotSummaryView = findViewById(R.id.text_journal_lot_summary);
        chartOfAccountsView = findViewById(R.id.text_chart_of_accounts);
        journalEmptyStateView = findViewById(R.id.text_journal_empty_state);
        journalEntriesContainer = findViewById(R.id.layout_journal_entries);
    }

    private void setupTopAppBar() {
        topAppBar.setTitle(R.string.journal_screen_title);
        topAppBar.inflateMenu(R.menu.main_sections_menu);
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_lot) {
                Intent intent = new Intent(this, MainActivity.class);
                String lotId = selectedLotIdInput.getText().toString().trim();
                if (!lotId.isEmpty()) {
                    intent.putExtra(EXTRA_LOT_ID, lotId);
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

    private void bindActions() {
        Button loadJournalButton = findViewById(R.id.button_load_journal);
        loadJournalButton.setOnClickListener(v -> loadSelectedJournal());
    }

    private void loadInitialJournal() {
        ioExecutor.execute(() -> refreshJournalOnWorker(initialLotId, null));
    }

    private void loadSelectedJournal() {
        try {
            String lotId = InventoryInputParser.requireText(selectedLotIdInput.getText().toString(), "Selected lot ID");
            ioExecutor.execute(() -> refreshJournalOnWorker(
                    lotId,
                    getString(R.string.journal_status_loaded, lotId)
            ));
        } catch (IllegalArgumentException exception) {
            journalStatusView.setText(exception.getMessage());
        }
    }

    private void refreshJournalOnWorker(String requestedLotId, String statusMessage) {
        try {
            String effectiveLotId = requestedLotId;
            if (TextUtils.isEmpty(effectiveLotId)) {
                if (repository.getLatestLot() != null) {
                    effectiveLotId = repository.getLatestLot().lotId;
                }
            }

            String journalLotSummary = getString(R.string.journal_lot_summary_placeholder);
            List<JournalEntryWithLines> entries = null;
            if (!TextUtils.isEmpty(effectiveLotId)) {
                LotWithDetails lotWithDetails = repository.getLotWithDetails(effectiveLotId);
                BatchValuationSnapshot snapshot = BatchValuationEngine.calculate(
                        lotWithDetails.lot,
                        lotWithDetails.expenses,
                        lotWithDetails.spoilageLogs
                );
                journalLotSummary = buildJournalLotSummary(lotWithDetails, snapshot);
                entries = repository.getJournalEntries(effectiveLotId);
            }

            String finalEffectiveLotId = effectiveLotId;
            String finalJournalLotSummary = journalLotSummary;
            List<JournalEntryWithLines> finalEntries = entries;
            runOnUiThread(() -> {
                if (!TextUtils.isEmpty(finalEffectiveLotId)) {
                    selectedLotIdInput.setText(finalEffectiveLotId);
                }
                journalStatusView.setText(statusMessage == null
                        ? getString(R.string.journal_status_idle)
                        : statusMessage);
                journalLotSummaryView.setText(finalJournalLotSummary);
                renderJournalEntries(finalEntries);
            });
        } catch (Exception exception) {
            runOnUiThread(() -> {
                journalStatusView.setText(exception.getMessage());
                journalLotSummaryView.setText(R.string.journal_lot_summary_placeholder);
                renderJournalEntries(null);
            });
        }
    }

    private void renderJournalEntries(List<JournalEntryWithLines> entries) {
        journalEntriesContainer.removeAllViews();
        boolean hasEntries = entries != null && !entries.isEmpty();
        journalEmptyStateView.setVisibility(hasEntries ? View.GONE : View.VISIBLE);
        if (!hasEntries) {
            return;
        }

        for (int index = 0; index < entries.size(); index++) {
            journalEntriesContainer.addView(createJournalEntryCard(entries.get(index), index));
        }
    }

    private View createJournalEntryCard(JournalEntryWithLines entry, int index) {
        MaterialCardView cardView = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        if (index > 0) {
            cardParams.topMargin = dp(12);
        }
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(dp(1));
        cardView.setRadius(dp(16));
        cardView.setUseCompatPadding(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        cardView.addView(content);

        TextView titleView = new TextView(this);
        titleView.setText(entry.entry.eventType);
        titleView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        content.addView(titleView);

        TextView descriptionView = new TextView(this);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descriptionParams.topMargin = dp(4);
        descriptionView.setLayoutParams(descriptionParams);
        descriptionView.setText(entry.entry.description);
        descriptionView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        content.addView(descriptionView);

        TextView metaView = new TextView(this);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(6);
        metaView.setLayoutParams(metaParams);
        metaView.setText(getString(
                R.string.journal_entry_meta,
                abbreviateLotId(entry.entry.lotId),
                dateTimeFormat.format(entry.entry.timestamp)
        ));
        metaView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        content.addView(metaView);

        LinearLayout linesContainer = new LinearLayout(this);
        LinearLayout.LayoutParams linesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linesParams.topMargin = dp(12);
        linesContainer.setLayoutParams(linesParams);
        linesContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(linesContainer);

        for (int lineIndex = 0; lineIndex < entry.lines.size(); lineIndex++) {
            linesContainer.addView(createJournalLineView(entry.lines.get(lineIndex), lineIndex));
        }

        return cardView;
    }

    private View createJournalLineView(JournalLineEntity line, int lineIndex) {
        LinearLayout lineLayout = new LinearLayout(this);
        lineLayout.setOrientation(LinearLayout.VERTICAL);
        if (lineIndex > 0) {
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lineParams.topMargin = dp(10);
            lineLayout.setLayoutParams(lineParams);
        }

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        lineLayout.addView(topRow);

        TextView sideView = new TextView(this);
        sideView.setText(JournalLineType.valueOf(line.lineType) == JournalLineType.DEBIT ? "DR" : "CR");
        sideView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        sideView.setPadding(dp(10), dp(4), dp(10), dp(4));
        sideView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        topRow.addView(sideView);

        TextView accountView = new TextView(this);
        LinearLayout.LayoutParams accountParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        accountParams.leftMargin = dp(12);
        accountView.setLayoutParams(accountParams);
        accountView.setText(getString(R.string.journal_line_account, line.accountCode, resolveAccountName(line)));
        accountView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        topRow.addView(accountView);

        TextView amountView = new TextView(this);
        amountView.setText(currencyFormat.format(line.amount));
        amountView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        topRow.addView(amountView);

        String lineDetails = buildLineDetails(line);
        if (!lineDetails.isEmpty()) {
            TextView detailsView = new TextView(this);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            detailsParams.topMargin = dp(4);
            detailsView.setLayoutParams(detailsParams);
            detailsView.setText(lineDetails);
            detailsView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
            lineLayout.addView(detailsView);
        }

        return lineLayout;
    }

    private String buildJournalLotSummary(LotWithDetails lotWithDetails, BatchValuationSnapshot snapshot) {
        return getString(
                R.string.journal_lot_summary,
                lotWithDetails.lot.vegetableType,
                lotWithDetails.lot.providerName,
                abbreviateLotId(lotWithDetails.lot.lotId),
                formatWeight(snapshot.getNetUsableKilograms()),
                currencyFormat.format(snapshot.getTotalCapitalizedCost())
        );
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

    private String abbreviateLotId(String lotId) {
        if (TextUtils.isEmpty(lotId)) {
            return "-";
        }
        return lotId.length() <= 8 ? lotId : lotId.substring(0, 8);
    }

    private String formatWeight(double kilos) {
        return String.format(Locale.US, "%.2f kg", kilos);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}

