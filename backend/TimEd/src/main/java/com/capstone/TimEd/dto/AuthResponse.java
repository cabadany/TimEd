package com.capstone.TimEd.dto;

import com.google.cloud.firestore.annotation.PropertyName;

public class AuthResponse {
    private String token;
    private String userId;
    private String schoolId;
    private String role;
    private String message;
    private boolean success;

    // Default constructor
    public AuthResponse() {}

    // Success response constructor
    public AuthResponse(String token, String userId, String email, String role) {
        this.token = token;
        this.userId = userId;
        this.schoolId = email;
        this.role = role;
        this.success = true;
        this.message = "Authentication successful";
    }

    // Error response constructor
    public AuthResponse(String message) {
        this.success = false;
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getschoolId() {
        return schoolId;
    }


    public void setschoolId(String email) {
        this.schoolId = email;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
} 