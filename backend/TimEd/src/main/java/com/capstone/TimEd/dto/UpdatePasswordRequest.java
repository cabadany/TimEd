package com.capstone.TimEd.dto;

public class UpdatePasswordRequest {
    private String schoolId;
    private String newPassword;

    // Getters and setters
    public String getSchoolId() { return schoolId; }
    public void setSchoolId(String schoolId) { this.schoolId = schoolId; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}