package com.example.kilgi.inventory.data.api;

public class LoginRequest {
    public final String username;
    public final String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
