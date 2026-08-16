package com.example.kilgi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kilgi.inventory.data.KilgiDatabase;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.service.ModuleOneRepository;

public class UserSetupActivity extends AppCompatActivity {

    private EditText displayNameInput;
    private EditText businessNameInput;
    private EditText passwordInput;
    private ModuleOneRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_setup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new ModuleOneRepository(KilgiDatabase.getInstance(this));
        displayNameInput = findViewById(R.id.edit_display_name);
        businessNameInput = findViewById(R.id.edit_business_name);
        passwordInput = findViewById(R.id.edit_password);

        findViewById(R.id.button_save_setup).setOnClickListener(v -> saveSetup());
    }

    private void saveSetup() {
        String displayName = displayNameInput.getText().toString().trim();
        String businessName = businessNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(displayName) || TextUtils.isEmpty(businessName) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            UserEntity user = repository.getUser(ModuleOneRepository.LOCAL_USER_ID);
            if (user != null) {
                UserEntity updatedUser = new UserEntity(
                        user.userId,
                        user.username,
                        displayName,
                        businessName,
                        user.emailAddress,
                        user.mobileNumber,
                        password, // We'll store as plain text for this simple implementation
                        "none",
                        "ACTIVE",
                        user.createdAt,
                        System.currentTimeMillis()
                );
                repository.updateUser(updatedUser);
                MainActivity.isUserAuthenticated = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "Setup complete!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            }
        }).start();
    }
}
