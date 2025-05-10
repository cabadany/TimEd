package com.capstone.TimEd.dto;

public class PasswordSyncRequest {
    private String schoolId;
    private String newPassword;

    // Getter for schoolId
    public String getSchoolId() {
        return schoolId;
    }

    // Setter for schoolId
    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    // Getter for newPassword
    public String getNewPassword() {
        return newPassword;
    }

    // Setter for newPassword
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}


