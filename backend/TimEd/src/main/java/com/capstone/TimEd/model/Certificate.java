package com.capstone.TimEd.model;

import java.util.List;
import java.util.Map;

public class Certificate {
    private String id;
    private String eventId;
    private String eventName;
    private String title;
    private String subtitle;
    private String recipientText;
    private String recipientName;
    private String description;
    private List<Map<String, String>> signatories;
    private String certificateNumber;
    private String backgroundColor;
    private String borderColor;
    private String headerColor;
    private String textColor;
    private String fontFamily;

    // Default constructor
    public Certificate() {
    }

    // Constructor with fields
    public Certificate(String id, String eventId, String eventName, String title, String subtitle,
                     String recipientText, String recipientName, String description,
                     List<Map<String, String>> signatories, String certificateNumber,
                     String backgroundColor, String borderColor, String headerColor,
                     String textColor, String fontFamily) {
        this.id = id;
        this.eventId = eventId;
        this.eventName = eventName;
        this.title = title;
        this.subtitle = subtitle;
        this.recipientText = recipientText;
        this.recipientName = recipientName;
        this.description = description;
        this.signatories = signatories;
        this.certificateNumber = certificateNumber;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.headerColor = headerColor;
        this.textColor = textColor;
        this.fontFamily = fontFamily;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getRecipientText() {
        return recipientText;
    }

    public void setRecipientText(String recipientText) {
        this.recipientText = recipientText;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Map<String, String>> getSignatories() {
        return signatories;
    }

    public void setSignatories(List<Map<String, String>> signatories) {
        this.signatories = signatories;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public String getHeaderColor() {
        return headerColor;
    }

    public void setHeaderColor(String headerColor) {
        this.headerColor = headerColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }
} 