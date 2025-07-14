package com.capstone.TimEd.model;

import java.util.Date;

public class Event {

    private String eventId;  // Firebase UID
    private String eventName;
    private Date date;
    private String duration;
    private String status;
    private String departmentId; // Reference to Department
    private Department department;  // This will hold the full department object (to be set manually)
    private String certificateId; // Reference to Certificate
    private String description; // Description of the event
    private String location; // Location of the event

    // Constructors
    public Event() {}

    public Event(String eventId, String eventName, Date date, String duration, String status, String departmentId, String description, String location) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.date = date;
        this.duration = duration;
        this.status = status;
        this.departmentId = departmentId;
        this.description = description;
        this.location = location;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
    
    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
