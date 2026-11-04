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
import androidx.lifecycle.ViewModelProvider;

import com.example.kilgi.inventory.data.UserEntity;
import com.example.kilgi.inventory.network.NetworkUtils;
import com.example.kilgi.inventory.repository.UserRepository;
import com.example.kilgi.inventory.viewmodel.UserViewModel;

public class UserSetupActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText displayNameInput;
    private EditText businessNameInput;
    private EditText emailAddressInput;
    private EditText mobileNumberInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private UserViewModel userViewModel;
    private UserEntity currentUser;

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

        usernameInput = findViewById(R.id.edit_username);
        displayNameInput = findViewById(R.id.edit_display_name);
        businessNameInput = findViewById(R.id.edit_business_name);
        emailAddressInput = findViewById(R.id.edit_email_address);
        mobileNumberInput = findViewById(R.id.edit_mobile_number);
        passwordInput = findViewById(R.id.edit_password);
        confirmPasswordInput = findViewById(R.id.edit_confirm_password);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getLocalUser().observe(this, userResource -> {
            if (userResource != null && userResource.data != null) {
                currentUser = userResource.data;
                if (usernameInput.getText().length() == 0 && currentUser.username != null && !"local_owner".equals(currentUser.username)) {
                    usernameInput.setText(currentUser.username);
                }
                if (displayNameInput.getText().length() == 0 && currentUser.displayName != null && !"Local Owner".equals(currentUser.displayName)) {
                    displayNameInput.setText(currentUser.displayName);
                }
                if (businessNameInput.getText().length() == 0 && currentUser.businessName != null && !"Kilgi Demo Business".equals(currentUser.businessName)) {
                    businessNameInput.setText(currentUser.businessName);
                }
                if (emailAddressInput.getText().length() == 0 && currentUser.emailAddress != null) {
                    emailAddressInput.setText(currentUser.emailAddress);
                }
                if (mobileNumberInput.getText().length() == 0 && currentUser.mobileNumber != null) {
                    mobileNumberInput.setText(currentUser.mobileNumber);
                }
            }
        });

        findViewById(R.id.button_save_setup).setOnClickListener(v -> saveSetup());
    }

    private void saveSetup() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "An active internet connection is required to complete initial user setup.", Toast.LENGTH_LONG).show();
            return;
        }

        String username = usernameInput.getText().toString().trim();
        String displayName = displayNameInput.getText().toString().trim();
        String businessName = businessNameInput.getText().toString().trim();
        String emailAddress = emailAddressInput.getText().toString().trim();
        String mobileNumber = mobileNumberInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(displayName) || TextUtils.isEmpty(businessName) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser != null ? currentUser.userId : UserRepository.LOCAL_USER_ID;
        long createdAt = currentUser != null ? currentUser.createdAt : System.currentTimeMillis();

        UserEntity updatedUser = new UserEntity(
                userId,
                username,
                displayName,
                businessName,
                TextUtils.isEmpty(emailAddress) ? null : emailAddress,
                TextUtils.isEmpty(mobileNumber) ? null : mobileNumber,
                password,
                "none",
                "ACTIVE",
                createdAt,
                System.currentTimeMillis()
        );

        userViewModel.registerOrSetupUser(updatedUser, new UserRepository.Callback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity result) {
                MainActivity.isUserAuthenticated = true;
                Toast.makeText(UserSetupActivity.this, "Setup complete!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(UserSetupActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onError(Throwable throwable) {
                Toast.makeText(UserSetupActivity.this, "Setup failed: " + throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
