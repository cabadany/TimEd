package com.capstone.TimEd.dto;

import com.google.cloud.firestore.annotation.PropertyName;

public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String department;
    private String email;
    private String schoolId;
    private String password;
    private String role;

    public RegisterRequest() {}
  
    public String getEmail() {
    	return email;
    }
    
    
 
    public String getDepartment() {
    	return department;
    }
    

    public void setDepartment(String department) {
    	this.department=department;
    }
    public void setEmail(String email1) {
    	this.email=email1;
    }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSchoolId() { return schoolId; }
    public void setSchoolId(String schoolId) { this.schoolId = schoolId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
