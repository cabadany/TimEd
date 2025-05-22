package com.capstone.TimEd.dto;

import com.google.cloud.firestore.annotation.PropertyName;

public class Eventdto {
    private String eventId;
    private String eventName;
    private String status;
    private String date;     // Format: "yyyy-MM-dd'T'HH:mm:ss"
    private String duration; // Format: "HH:mm:ss"
    private String departmentId; // Reference to Department
    private String departmentName; // New field for the department name
    private String description; // Description of the event

    // Default constructor
    public Eventdto() {
    }

    // Constructor with parameters for easy initialization
    public Eventdto(String eventId, String eventName, String status, String date, String duration, String departmentId) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.date = date;  // Expecting a String here
        this.duration = duration;
        this.departmentId = departmentId;
    }

    public Eventdto(String eventId, String eventName, String status, String date, String duration, String departmentId, String departmentName, String description) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.date = date;
        this.duration = duration;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.description = description;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        if (date.matches("\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2})?")) {  // Accept format yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss
            this.date = date;
        } else {
            throw new IllegalArgumentException("Date must be in format yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        if (duration.matches("\\d{2}:\\d{2}:\\d{2}")) {  // Check if the duration is in the correct format
            this.duration = duration;
        } else {
            throw new IllegalArgumentException("Duration must be in format HH:mm:ss");
        }
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    public String getDepartment() {
        return departmentName;
    }

    public void setDepartment(String departmentName) {
        this.departmentName = departmentName;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
