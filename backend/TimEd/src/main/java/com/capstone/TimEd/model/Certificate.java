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

    // Style properties
    private String backgroundColor;
    private String borderColor;
    private String headerColor;
    private String textColor;
    private String fontFamily;
    private int fontSize = 12; // Default font size
    private String dateFormat = "MMMM dd, yyyy";
    private boolean showBorder = true;
    private int borderWidth = 2;
    private String borderStyle = "solid"; // solid, dashed, dotted
    private Map<String, Float> margins; // top, right, bottom, left

    // Frame and decoration properties
    private String frameStyle = "classic"; // none, classic, double, ornate, modern, blueWave
    private boolean showDecorations = true;
    private boolean decorativeCorners = true;
    private boolean showRibbon = false;
    private String ribbonPosition = "bottom-center";
    private String ribbonColor = "#D4AF37";
    private boolean showSeal = false;
    private String sealPosition = "bottom-right";
    private String sealColor = "#C0C0C0";

    // Image properties
    private String backgroundImage; // Base64 encoded image
    private float backgroundImageOpacity = 0.3f;
    private String logoImage; // Base64 encoded image
    private float logoWidth = 100;
    private float logoHeight = 100;
    private String logoPosition = "top-center"; // top-left, top-center, top-right
    private String watermarkImage; // Base64 encoded image
    private float watermarkImageOpacity = 0.1f;
    private Map<String, String> signatureImages; // Map of signatory name to Base64 encoded signature image

    // QR Code properties
    private boolean showQRCode = true;
    private String qrCodePosition = "bottom-right"; // bottom-right, bottom-left, top-right, top-left

    // Default constructor
    public Certificate() {
        // Set default values
        this.backgroundColor = "#ffffff";
        this.borderColor = "#000000";
        this.headerColor = "#000000";
        this.textColor = "#000000";
        this.fontFamily = "Times New Roman";
        this.fontSize = 12;
        this.dateFormat = "MMMM dd, yyyy";
        this.showBorder = true;
        this.borderWidth = 1;
        this.backgroundImageOpacity = 0.3f;
        this.watermarkImageOpacity = 0.1f;
        this.logoPosition = "top-center";
        this.logoWidth = 150f;
        this.logoHeight = 150f;
        this.showQRCode = false;
        this.qrCodePosition = "bottom-right";
    }

    // Constructor with fields
    public Certificate(String id, String eventId, String eventName, String title, String subtitle,
            String recipientText, String recipientName, String description,
            List<Map<String, String>> signatories, String certificateNumber,
            String backgroundColor, String borderColor, String headerColor,
            String textColor, String fontFamily) {
        this(); // Call default constructor for default values
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

    // Getters and Setters for all fields
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

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public boolean isShowBorder() {
        return showBorder;
    }

    public void setShowBorder(boolean showBorder) {
        this.showBorder = showBorder;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    public Map<String, Float> getMargins() {
        return margins;
    }

    public void setMargins(Map<String, Float> margins) {
        this.margins = margins;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public float getBackgroundImageOpacity() {
        return backgroundImageOpacity;
    }

    public void setBackgroundImageOpacity(float backgroundImageOpacity) {
        this.backgroundImageOpacity = backgroundImageOpacity;
    }

    public String getLogoImage() {
        return logoImage;
    }

    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }

    public float getLogoWidth() {
        return logoWidth;
    }

    public void setLogoWidth(float logoWidth) {
        this.logoWidth = logoWidth;
    }

    public float getLogoHeight() {
        return logoHeight;
    }

    public void setLogoHeight(float logoHeight) {
        this.logoHeight = logoHeight;
    }

    public String getLogoPosition() {
        return logoPosition;
    }

    public void setLogoPosition(String logoPosition) {
        this.logoPosition = logoPosition;
    }

    public String getWatermarkImage() {
        return watermarkImage;
    }

    public void setWatermarkImage(String watermarkImage) {
        this.watermarkImage = watermarkImage;
    }

    public float getWatermarkImageOpacity() {
        return watermarkImageOpacity;
    }

    public void setWatermarkImageOpacity(float watermarkImageOpacity) {
        this.watermarkImageOpacity = watermarkImageOpacity;
    }

    public Map<String, String> getSignatureImages() {
        return signatureImages;
    }

    public void setSignatureImages(Map<String, String> signatureImages) {
        this.signatureImages = signatureImages;
    }

    public boolean isShowQRCode() {
        return showQRCode;
    }

    public void setShowQRCode(boolean showQRCode) {
        this.showQRCode = showQRCode;
    }

    public String getQrCodePosition() {
        return qrCodePosition;
    }

    public void setQrCodePosition(String qrCodePosition) {
        this.qrCodePosition = qrCodePosition;
    }

    // New styling field getters and setters
    public String getBorderStyle() {
        return borderStyle;
    }

    public void setBorderStyle(String borderStyle) {
        this.borderStyle = borderStyle;
    }

    public String getFrameStyle() {
        return frameStyle;
    }

    public void setFrameStyle(String frameStyle) {
        this.frameStyle = frameStyle;
    }

    public boolean isShowDecorations() {
        return showDecorations;
    }

    public void setShowDecorations(boolean showDecorations) {
        this.showDecorations = showDecorations;
    }

    public boolean isDecorativeCorners() {
        return decorativeCorners;
    }

    public void setDecorativeCorners(boolean decorativeCorners) {
        this.decorativeCorners = decorativeCorners;
    }

    public boolean isShowRibbon() {
        return showRibbon;
    }

    public void setShowRibbon(boolean showRibbon) {
        this.showRibbon = showRibbon;
    }

    public String getRibbonPosition() {
        return ribbonPosition;
    }

    public void setRibbonPosition(String ribbonPosition) {
        this.ribbonPosition = ribbonPosition;
    }

    public String getRibbonColor() {
        return ribbonColor;
    }

    public void setRibbonColor(String ribbonColor) {
        this.ribbonColor = ribbonColor;
    }

    public boolean isShowSeal() {
        return showSeal;
    }

    public void setShowSeal(boolean showSeal) {
        this.showSeal = showSeal;
    }

    public String getSealPosition() {
        return sealPosition;
    }

    public void setSealPosition(String sealPosition) {
        this.sealPosition = sealPosition;
    }

    public String getSealColor() {
        return sealColor;
    }

    public void setSealColor(String sealColor) {
        this.sealColor = sealColor;
    }
}