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

import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.viewmodel.UserViewModel;

public class LoginActivity extends AppCompatActivity {

    private EditText passwordInput;
    private TextView businessNameLabel;
    private UserViewModel userViewModel;
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

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getLocalUser().observe(this, user -> {
            currentUser = user;
            if (user != null && user.businessName != null) {
                businessNameLabel.setText(user.businessName);
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

        if (currentUser != null) {
            if (password.equals(currentUser.passwordHash)) {
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
