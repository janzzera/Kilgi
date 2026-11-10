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
import androidx.lifecycle.ViewModelProvider;

import com.example.kilgi.inventory.data.TokenManager;
import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.data.api.AuthResponse;
import com.example.kilgi.inventory.network.NetworkUtils;
import com.example.kilgi.inventory.repository.UserRepository;
import com.example.kilgi.inventory.security.PasswordHasher;
import com.example.kilgi.inventory.viewmodel.UserViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordInput;
    private TextView businessNameLabel;
    private UserViewModel userViewModel;
    private TokenManager tokenManager;
    private UserEntity currentUser;

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

        passwordInput = findViewById(R.id.edit_login_password);
        businessNameLabel = findViewById(R.id.text_login_business_name);
        tokenManager = new TokenManager(this);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getLocalUser().observe(this, userResource -> {
            if (userResource != null && userResource.data != null) {
                currentUser = userResource.data;
                if (currentUser.businessName != null) {
                    businessNameLabel.setText(currentUser.businessName);
                }
            }
        });

        findViewById(R.id.button_login).setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "No user account set up on this device.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isOnline = NetworkUtils.isNetworkAvailable(this);
        boolean hasValidToken = tokenManager.isTokenValid();

        if (isOnline && !hasValidToken) {
            userViewModel.loginOnline(currentUser.username, password, new UserRepository.Callback<AuthResponse>() {
                @Override
                public void onSuccess(AuthResponse result) {
                    MainActivity.isUserAuthenticated = true;
                    Toast.makeText(LoginActivity.this, "Online login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }

                @Override
                public void onError(Throwable throwable) {
                    if (PasswordHasher.verifyPasswordHash(password, currentUser.passwordHash, currentUser.passwordSalt)) {
                        MainActivity.isUserAuthenticated = true;
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        passwordInput.setText("");
                    }
                }
            });
        } else {
            if (PasswordHasher.verifyPasswordHash(password, currentUser.passwordHash, currentUser.passwordSalt)) {
                MainActivity.isUserAuthenticated = true;
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                passwordInput.setText("");
            }
        }
    }
}
