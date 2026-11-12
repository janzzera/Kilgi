package com.example.kilgi.inventory.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREFS_FILENAME = "secure_auth_prefs";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_TIMESTAMP = "jwt_token_timestamp";
    private static final long TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours

    private SharedPreferences securePreferences;

    public TokenManager(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePreferences = EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILENAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch(GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    public void saveToken(String accessToken) {
        saveTokens(accessToken, null);
    }

    public void saveTokens(String accessToken, @Nullable String refreshToken) {
        if (securePreferences == null)
            return;

        SharedPreferences.Editor editor = securePreferences.edit();
        editor.putString(KEY_JWT_TOKEN, accessToken);
        editor.putLong(KEY_TOKEN_TIMESTAMP, System.currentTimeMillis());
        if (refreshToken != null)
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        else
            editor.remove(KEY_REFRESH_TOKEN);

        editor.apply();
    }

    @Nullable
    public String getAccessToken() {
        if (securePreferences == null)
            return null;
        return securePreferences.getString(KEY_JWT_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        if (securePreferences == null)
            return null;
        return securePreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getTokenTimestamp() {
        if (securePreferences == null)
            return 0;
        return securePreferences.getLong(KEY_TOKEN_TIMESTAMP, 0);
    }

    public boolean isTokenValid() {
        String token = getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        long savedTime = getTokenTimestamp();
        return (System.currentTimeMillis() - savedTime) < TOKEN_EXPIRY_MS;
    }

    public void clearTokens() {
        if (securePreferences == null)
            return;

        securePreferences.edit()
                .remove(KEY_JWT_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_TOKEN_TIMESTAMP)
                .apply();
    }
}
