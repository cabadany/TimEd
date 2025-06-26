package com.capstone.TimEd.dto;

public class ReviewRequestDto {
    private String requestId;
    private String action; // "APPROVE" or "REJECT"
    private String rejectionReason; // Only required for rejection
    private String reviewedBy; // Admin who is reviewing

    // Default constructor
    public ReviewRequestDto() {}

    // Constructor
    public ReviewRequestDto(String requestId, String action, String rejectionReason, String reviewedBy) {
        this.requestId = requestId;
        this.action = action;
        this.rejectionReason = rejectionReason;
        this.reviewedBy = reviewedBy;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
} 