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

    // Constructors
    public Event() {}

    public Event(String eventId, String eventName, Date date, String duration, String status, String departmentId) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.date = date;
        this.duration = duration;
        this.status = status;
        this.departmentId = departmentId;
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
}
