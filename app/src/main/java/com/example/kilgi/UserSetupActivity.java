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

    private EditText displayNameInput;
    private EditText businessNameInput;
    private EditText passwordInput;
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

        displayNameInput = findViewById(R.id.edit_display_name);
        businessNameInput = findViewById(R.id.edit_business_name);
        passwordInput = findViewById(R.id.edit_password);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.getLocalUser().observe(this, userResource -> {
            if (userResource != null && userResource.data != null) {
                currentUser = userResource.data;
                if (displayNameInput.getText().length() == 0 && currentUser.displayName != null && !"Local Owner".equals(currentUser.displayName)) {
                    displayNameInput.setText(currentUser.displayName);
                }
                if (businessNameInput.getText().length() == 0 && currentUser.businessName != null && !"Kilgi Demo Business".equals(currentUser.businessName)) {
                    businessNameInput.setText(currentUser.businessName);
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

        String userId = currentUser != null ? currentUser.userId : UserRepository.LOCAL_USER_ID;
        String username = currentUser != null ? currentUser.username : "local_owner";
        long createdAt = currentUser != null ? currentUser.createdAt : System.currentTimeMillis();

        UserEntity updatedUser = new UserEntity(
                userId,
                username,
                displayName,
                businessName,
                currentUser != null ? currentUser.emailAddress : null,
                currentUser != null ? currentUser.mobileNumber : null,
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
