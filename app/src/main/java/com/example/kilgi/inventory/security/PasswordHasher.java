package com.example.kilgi.inventory.security;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PasswordHasher {

    private static final String TAG = "PasswordHasher";
    private static final String HMAC_SHA512_ALGORITHM = "HmacSHA512";
    private static final int SALT_BYTE_LENGTH = 16;

    private PasswordHasher() {
    }

    public static String[] hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        try {
            byte[] salt = new byte[SALT_BYTE_LENGTH];
            new SecureRandom().nextBytes(salt);

            SecretKeySpec secretKey = new SecretKeySpec(salt, HMAC_SHA512_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA512_ALGORITHM);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(password.getBytes(StandardCharsets.UTF_8));

            String hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP);
            String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);

            return new String[]{hashBase64, saltBase64};
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "Error hashing password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public static boolean verifyPasswordHash(String password, String storedHashBase64, String storedSaltBase64) {
        if (password == null || storedHashBase64 == null || storedSaltBase64 == null) {
            return false;
        }
        try {
            byte[] storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP);
            byte[] storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP);

            SecretKeySpec secretKey = new SecretKeySpec(storedSalt, HMAC_SHA512_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA512_ALGORITHM);
            mac.init(secretKey);

            byte[] computedHash = mac.doFinal(password.getBytes(StandardCharsets.UTF_8));

            return MessageDigest.isEqual(computedHash, storedHash);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying password hash", e);
            return false;
        }
    }
}
