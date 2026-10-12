package com.example.kilgi.inventory.data.api;

public class AuthResponse {
    public final String token;
    public final String refreshToken;
    public final String userId;
    public final String username;
    public final String message;

    public AuthResponse(String token, String refreshToken, String userId, String username, String message) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
        this.message = message;
    }
}
