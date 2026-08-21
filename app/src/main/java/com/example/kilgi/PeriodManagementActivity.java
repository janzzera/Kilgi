package com.example.kilgi;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kilgi.inventory.data.AccountingPeriodEntity;
import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.service.ModuleOneRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeriodManagementActivity extends AppCompatActivity {

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private ModuleOneRepository repository;
    private PeriodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_period_management);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        RecyclerView recyclerView = findViewById(R.id.recycler_periods);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PeriodAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.button_add_period).setOnClickListener(v -> showCreatePeriodDialog());

        loadPeriods();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    private void loadPeriods() {
        ioExecutor.execute(() -> {
            List<AccountingPeriodEntity> periods = repository.getAccountingPeriods();
            runOnUiThread(() -> adapter.setPeriods(periods));
        });
    }

    private void showCreatePeriodDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_period, null);
        EditText nameInput = view.findViewById(R.id.edit_period_name);
        Button startBtn = view.findViewById(R.id.button_start_date);
        Button endBtn = view.findViewById(R.id.button_end_date);

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        
        startBtn.setText(dateFormat.format(start.getTimeInMillis()));
        endBtn.setText(dateFormat.format(end.getTimeInMillis()));

        startBtn.setOnClickListener(v -> {
            new DatePickerDialog(this, (d, y, m, day) -> {
                start.set(y, m, day, 0, 0, 0);
                startBtn.setText(dateFormat.format(start.getTimeInMillis()));
            }, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH)).show();
        });

        endBtn.setOnClickListener(v -> {
            new DatePickerDialog(this, (d, y, m, day) -> {
                end.set(y, m, day, 23, 59, 59);
                endBtn.setText(dateFormat.format(end.getTimeInMillis()));
            }, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_new_period_title)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_save, (d, w) -> {
                    String name = nameInput.getText().toString();
                    if (name.isEmpty()) return;
                    ioExecutor.execute(() -> {
                        repository.createAccountingPeriod(name, start.getTimeInMillis(), end.getTimeInMillis());
                        loadPeriods();
                    });
                }).show();
    }

    private void showCloseChecklist(AccountingPeriodEntity period) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_close_checklist, null);
        CheckBox chkSpoilage = view.findViewById(R.id.check_spoilage);
        CheckBox chkRecon = view.findViewById(R.id.check_reconciliation);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pre_close_checklist_title)
                .setView(view)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.button_lock_period, (d, w) -> {
                    if (chkSpoilage.isChecked() && chkRecon.isChecked()) {
                        ioExecutor.execute(() -> {
                            repository.closeAccountingPeriod(period.periodId);
                            loadPeriods();
                        });
                    } else {
                        Toast.makeText(this, "Please complete all checklist items first.", Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private class PeriodAdapter extends RecyclerView.Adapter<PeriodAdapter.ViewHolder> {
        private final List<AccountingPeriodEntity> list = new ArrayList<>();

        public void setPeriods(List<AccountingPeriodEntity> periods) {
            list.clear();
            list.addAll(periods);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_accounting_period, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AccountingPeriodEntity p = list.get(position);
            holder.name.setText(p.periodName);
            holder.status.setText(p.isClosed ? R.string.status_locked : R.string.status_open);
            holder.status.setTextColor(p.isClosed ? 0xFFD32F2F : 0xFF388E3C);
            String dateRange = dateFormat.format(p.startDate) + " - " + dateFormat.format(p.endDate);
            holder.dates.setText(dateRange);
            
            holder.lockBtn.setVisibility(p.isClosed ? View.GONE : View.VISIBLE);
            holder.lockBtn.setOnClickListener(v -> showCloseChecklist(p));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, status, dates;
            Button lockBtn;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.text_period_name);
                status = v.findViewById(R.id.text_period_status);
                dates = v.findViewById(R.id.text_period_dates);
                lockBtn = v.findViewById(R.id.button_lock);
            }
        }
    }
}
