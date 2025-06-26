package com.capstone.TimEd.model;

import com.google.cloud.firestore.annotation.PropertyName;
import java.util.Date;

public class AccountRequest {
    private String requestId;
    private String firstName;
    private String lastName;
    private String email;
    private String schoolId;
    private String department;
    private String password; // Hashed password
    private String status; // PENDING, APPROVED, REJECTED
    private Date requestDate;
    private Date reviewDate;
    private String reviewedBy; // Admin who reviewed the request
    private String rejectionReason;

    // Default constructor
    public AccountRequest() {}

    // Constructor
    public AccountRequest(String requestId, String firstName, String lastName, String email, 
                         String schoolId, String department, String password) {
        this.requestId = requestId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.schoolId = schoolId;
        this.department = department;
        this.password = password;
        this.status = "PENDING";
        this.requestDate = new Date();
    }

    // Getters and Setters
    @PropertyName("requestId")
    public String getRequestId() {
        return requestId;
    }

    @PropertyName("requestId")
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @PropertyName("firstName")
    public String getFirstName() {
        return firstName;
    }

    @PropertyName("firstName")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @PropertyName("lastName")
    public String getLastName() {
        return lastName;
    }

    @PropertyName("lastName")
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("schoolId")
    public String getSchoolId() {
        return schoolId;
    }

    @PropertyName("schoolId")
    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    @PropertyName("department")
    public String getDepartment() {
        return department;
    }

    @PropertyName("department")
    public void setDepartment(String department) {
        this.department = department;
    }

    @PropertyName("password")
    public String getPassword() {
        return password;
    }

    @PropertyName("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("requestDate")
    public Date getRequestDate() {
        return requestDate;
    }

    @PropertyName("requestDate")
    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    @PropertyName("reviewDate")
    public Date getReviewDate() {
        return reviewDate;
    }

    @PropertyName("reviewDate")
    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    @PropertyName("reviewedBy")
    public String getReviewedBy() {
        return reviewedBy;
    }

    @PropertyName("reviewedBy")
    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    @PropertyName("rejectionReason")
    public String getRejectionReason() {
        return rejectionReason;
    }

    @PropertyName("rejectionReason")
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
} 