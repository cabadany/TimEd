package com.capstone.TimEd.dto;

import com.google.cloud.firestore.annotation.PropertyName;

public class LoginRequest {
    private String schoolId;
    private String password;

    // Default constructor
    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.schoolId = email;
        this.password = password;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String email) {
        this.schoolId = email;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
} 
