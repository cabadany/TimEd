package com.capstone.TimEd.model;

import com.google.cloud.firestore.annotation.PropertyName;

public class User {

    private String userId;          // Firebase UID
    private String firstName;
    private String lastName;
    private String departmentId;    // Reference to Department (String ID)
    private Department department;  // This will hold the full Department object
    private String email;
    private String schoolId;
    private String password;
    private String role;
    private String profilePictureUrl; // URL to the profile picture in Firebase Storage
    private boolean verified;       // Whether the user account has been verified by admin

    // Default constructor
    public User() {
        this.verified = false; // Default to unverified
    }

    // Constructor to initialize the User with fields
    public User(String userId, String firstName, String lastName, String departmentId, String email, String schoolId, String password, String role, String profilePictureUrl) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.departmentId = departmentId;
        this.email = email;
        this.schoolId = schoolId;
        this.password = password;
        this.role = role;
        this.profilePictureUrl = profilePictureUrl;
        this.verified = false; // Default to unverified
    }

    // Getters and Setters
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
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

    
    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    @PropertyName("department")
    public Department getDepartment() {
        return department;
    }

    @PropertyName("department")
    public void setDepartment(Department department) {
        this.department = department;  // Set the full department object
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

    @PropertyName("password")
    public String getPassword() {
        return password;
    }

    @PropertyName("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    @PropertyName("role")
    public void setRole(String role) {
        this.role = role;
    }

    @PropertyName("profilePictureUrl")
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    @PropertyName("profilePictureUrl")
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    @PropertyName("verified")
    public boolean isVerified() {
        return verified;
    }

    @PropertyName("verified")
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", departmentId='" + departmentId + '\'' +
                ", department=" + department +  // This will display the full department object
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", profilePictureUrl='" + profilePictureUrl + '\'' +
                ", verified=" + verified +
                '}';
    }
}
