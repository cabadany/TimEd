package com.capstone.TimEd.model;

import java.util.Date;  // Import Date class

public class Event {
    private String eventId;
    private String eventName;
    private String status;
    private Date date;  // Change the date to Date type
    private String duration;

    public Event() {
    }

    // Constructor
    public Event(String eventId, String eventName, String status, Date date, String duration) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.date = date;
        this.duration = duration;
    }

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
}
