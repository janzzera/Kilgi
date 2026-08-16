package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.service.ModuleOneRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordInput;
    private TextView businessNameLabel;
    private ModuleOneRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        passwordInput = findViewById(R.id.edit_login_password);
        businessNameLabel = findViewById(R.id.text_login_business_name);

        findViewById(R.id.button_login).setOnClickListener(v -> attemptLogin());

        loadUserData();
    }

    private void loadUserData() {
        new Thread(() -> {
            UserEntity user = repository.getUser(ModuleOneRepository.LOCAL_USER_ID);
            if (user != null) {
                runOnUiThread(() -> businessNameLabel.setText(user.businessName));
            }
        }).start();
    }

    private void attemptLogin() {
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            UserEntity user = repository.getUser(ModuleOneRepository.LOCAL_USER_ID);
            if (user != null) {
                if (user.passwordHash.equals(password)) {
                    MainActivity.isUserAuthenticated = true;
                    runOnUiThread(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        passwordInput.setText("");
                    });
                }
            }
        }).start();
    }
}
