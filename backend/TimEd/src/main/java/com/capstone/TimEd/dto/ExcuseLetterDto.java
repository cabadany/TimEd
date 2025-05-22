package com.capstone.TimEd.dto;

public class ExcuseLetterDto {
    private String id;
    private String userId;
    private String eventId;
    private String date;
    private String details;
    private String reason;
    private String status;
    private long submittedAt;
    private String userName;
    private String eventName;
    private String rejectionReason;
    
    // New fields from Realtime DB
    private String attachmentUrl;
    private String department;
    private String email;
    private String firstName;
    private String idNumber;
    
    // Constructors
    public ExcuseLetterDto() {}
    
    public ExcuseLetterDto(String id, String userId, String eventId, String date, String details, 
                      String reason, String status, long submittedAt, String userName, String eventName) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.date = date;
        this.details = details;
        this.reason = reason;
        this.status = status;
        this.submittedAt = submittedAt;
        this.userName = userName;
        this.eventName = eventName;
    }
    
    public ExcuseLetterDto(String id, String userId, String eventId, String date, String details, 
                      String reason, String status, long submittedAt, String userName, String eventName, String rejectionReason) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.date = date;
        this.details = details;
        this.reason = reason;
        this.status = status;
        this.submittedAt = submittedAt;
        this.userName = userName;
        this.eventName = eventName;
        this.rejectionReason = rejectionReason;
    }
    
    public ExcuseLetterDto(String id, String userId, String eventId, String date, String details, 
                      String reason, String status, long submittedAt, String userName, String eventName,
                      String rejectionReason, String attachmentUrl, String department, 
                      String email, String firstName, String idNumber) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.date = date;
        this.details = details;
        this.reason = reason;
        this.status = status;
        this.submittedAt = submittedAt;
        this.userName = userName;
        this.eventName = eventName;
        this.rejectionReason = rejectionReason;
        this.attachmentUrl = attachmentUrl;
        this.department = department;
        this.email = email;
        this.firstName = firstName;
        this.idNumber = idNumber;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    // Getters and Setters for new fields
    public String getAttachmentUrl() {
        return attachmentUrl;
    }
    
    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
    
    public String getDepartment() {
        return department;
    }
    
    public void setDepartment(String department) {
        this.department = department;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getIdNumber() {
        return idNumber;
    }
    
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
} 