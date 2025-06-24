package com.capstone.TimEd.dto;

public class OtpRequest {
    private String schoolId;
    private String otp;

    // Default constructor
    public OtpRequest() {}

    public OtpRequest(String schoolId, String otp) {
        this.schoolId = schoolId;
        this.otp = otp;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
} 