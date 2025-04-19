package com.capstone.TimEd.model;


import com.google.cloud.firestore.annotation.PropertyName;

public class User {
    private String userId;
    private String firstName;
    private String lastName;
    private String department;
    private String email;
    private String schoolId;
    private String password;
    private String role;

    // Default constructor
    public User() {}

    // Explicit PropertyName annotations to ensure correct field mapping
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @PropertyName("department")
    public String getDepartment() {
    	return department;
    }
    
    @PropertyName("department")
    public void setDepartment(String department) {
    	this.department=department;
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

    @PropertyName("schoolId")
    public String getSchoolId() {
        return schoolId;
    }

    @PropertyName("schoolId")
    public void setSchoolId(String email) {
        this.schoolId = email;
    }
    
    @PropertyName("email")
    public String getEmail() {
    	return email;
    }
    
    @PropertyName("email")
    public void setEmail(String email1) {
    	this.email=email1;
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


    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + schoolId + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

