package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.model.Certificate;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.awt.Graphics2D;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

import com.itextpdf.layout.element.Text;
import java.util.stream.Collectors;

@Service
public class CertificateService {

    private static final String COLLECTION_NAME = "certificates";

    @Autowired
    private Firestore firestore;

    @Autowired
    private BrevoEmailService brevoEmailService;

    /**
     * Save a certificate (create new or update existing)
     */
    public Certificate saveCertificate(Certificate certificate) throws ExecutionException, InterruptedException {
        System.out.println("Saving certificate for eventId: " + certificate.getEventId());

        if (certificate.getId() == null || certificate.getId().isEmpty()) {
            // This is a new certificate
            return createCertificate(certificate);
        } else {
            // This is an existing certificate
            return updateCertificate(certificate.getId(), certificate);
        }
    }

    /**
     * Create a new certificate template
     */
    public Certificate createCertificate(Certificate certificate) throws ExecutionException, InterruptedException {
        // Add debugging
        System.out.println("Creating certificate with eventId: " + certificate.getEventId());

        // Generate a new document ID
        CollectionReference certificatesCollection = firestore.collection(COLLECTION_NAME);
        DocumentReference newCertRef = certificatesCollection.document();

        // Set the generated ID to the certificate
        certificate.setId(newCertRef.getId());

        // Write to Firestore
        ApiFuture<WriteResult> writeResult = newCertRef.set(certificate);

        // Wait for the operation to complete
        writeResult.get();

        System.out.println(
                "Certificate created with ID: " + certificate.getId() + " for eventId: " + certificate.getEventId());

        return certificate;
    }

    /**
     * Retrieve a certificate by its ID
     */
    public Certificate getCertificate(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(Certificate.class);
        } else {
            return null;
        }
    }

    /**
     * Update an existing certificate
     */
    public Certificate updateCertificate(String id, Certificate certificate)
            throws ExecutionException, InterruptedException {
        // Set the ID in case it's not already set
        certificate.setId(id);

        // Update in Firestore
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> writeResult = docRef.set(certificate);

        // Wait for the operation to complete
        writeResult.get();

        return certificate;
    }

    /**
     * Delete a certificate
     */
    public String deleteCertificate(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<WriteResult> writeResult = docRef.delete();

        // Wait for the operation to complete
        writeResult.get();

        return "Certificate successfully deleted";
    }

    /**
     * Get all certificates
     */
    public List<Certificate> getAllCertificates() throws ExecutionException, InterruptedException {
        List<Certificate> certificates = new ArrayList<>();

        // Query Firestore
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        // Convert each document to a Certificate object
        for (QueryDocumentSnapshot document : documents) {
            Certificate certificate = document.toObject(Certificate.class);
            certificates.add(certificate);
        }

        return certificates;
    }

    /**
     * Get certificate by event ID
     */
    public Certificate getCertificateByEventId(String eventId) throws ExecutionException, InterruptedException {
        System.out.println("Searching for certificate with eventId: " + eventId);

        // First try with the exact eventId
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (!documents.isEmpty()) {
            Certificate certificate = documents.get(0).toObject(Certificate.class);
            System.out
                    .println("Found certificate: " + certificate.getId() + " for eventId: " + certificate.getEventId());
            return certificate;
        }

        // If no certificate found, try with the legacy format: "Event added
        // successfully with ID: {eventId}"
        String legacyEventId = "Event added successfully with ID: " + eventId;
        ApiFuture<QuerySnapshot> legacyFuture = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", legacyEventId)
                .limit(1)
                .get();

        List<QueryDocumentSnapshot> legacyDocuments = legacyFuture.get().getDocuments();

        if (!legacyDocuments.isEmpty()) {
            Certificate certificate = legacyDocuments.get(0).toObject(Certificate.class);
            System.out.println("Found certificate with legacy eventId format: " + certificate.getId());

            // Auto-fix the certificate by updating its eventId to the correct format
            try {
                certificate.setEventId(eventId);
                updateCertificate(certificate.getId(), certificate);
                System.out.println("Fixed legacy eventId format for certificate: " + certificate.getId());
            } catch (Exception e) {
                System.err.println("Error fixing legacy eventId format: " + e.getMessage());
            }

            return certificate;
        }

        System.out.println("No certificate found for eventId: " + eventId);
        return null;
    }

    /**
     * Send certificates to event attendees
     */
    public String sendCertificates(String certificateId, String eventId)
            throws ExecutionException, InterruptedException {
        System.out.println("Starting to send certificates for eventId: " + eventId);

        try {
            // Get the certificate template
            Certificate template = getCertificate(certificateId);
            if (template == null) {
                throw new RuntimeException("Certificate template not found with ID: " + certificateId);
            }

            // Get all attendees for this event from Firestore
            CollectionReference attendeesRef = firestore.collection("events").document(eventId).collection("attendees");
            ApiFuture<QuerySnapshot> attendeesFuture = attendeesRef.get();
            List<QueryDocumentSnapshot> attendeeDocs = attendeesFuture.get().getDocuments();

            if (attendeeDocs.isEmpty()) {
                return "No attendees found for this event";
            }

            System.out.println("Found " + attendeeDocs.size() + " attendees");

            // Get all users to lookup firstName/lastName by email or userId
            CollectionReference usersRef = firestore.collection("users");
            ApiFuture<QuerySnapshot> usersFuture = usersRef.get();
            List<QueryDocumentSnapshot> userDocs = usersFuture.get().getDocuments();

            // Create lookup maps keyed by email and by userId
            Map<String, Map<String, Object>> usersByEmail = new HashMap<>();
            Map<String, Map<String, Object>> usersByUserId = new HashMap<>();
            for (QueryDocumentSnapshot userDoc : userDocs) {
                String email = userDoc.getString("email");
                String userId = userDoc.getString("userId");
                Map<String, Object> data = userDoc.getData();
                if (email != null) {
                    usersByEmail.put(email.toLowerCase(), data);
                }
                if (userId != null) {
                    usersByUserId.put(userId, data);
                }
            }

            int successCount = 0;
            int failCount = 0;

            // Process each attendee
            for (QueryDocumentSnapshot attendeeDoc : attendeeDocs) {
                try {
                    String email = attendeeDoc.getString("email");
                    String attendeeUserId = attendeeDoc.getString("userId");
                    if (email == null || email.isEmpty()) {
                        // Try to backfill email from user record via userId
                        if (attendeeUserId != null && usersByUserId.containsKey(attendeeUserId)) {
                            Map<String, Object> userData = usersByUserId.get(attendeeUserId);
                            email = (String) userData.getOrDefault("email", "");
                        }
                    }
                    if (email == null || email.isEmpty()) {
                        System.out.println("Skipping attendee with no email");
                        failCount++;
                        continue;
                    }

                    // Look up user data by userId first, then email to get firstName and lastName
                    Map<String, Object> userData = null;
                    if (attendeeUserId != null && usersByUserId.containsKey(attendeeUserId)) {
                        userData = usersByUserId.get(attendeeUserId);
                    }
                    if (userData == null) {
                        userData = usersByEmail.get(email.toLowerCase());
                    }

                    String firstName = "";
                    String lastName = "";

                    if (userData != null) {
                        firstName = (String) userData.getOrDefault("firstName", "");
                        lastName = (String) userData.getOrDefault("lastName", "");
                    }

                    // Fallback to attendee data if user not found
                    if (firstName.isEmpty()) {
                        firstName = attendeeDoc.getString("firstName");
                        if (firstName == null)
                            firstName = "";
                    }
                    if (lastName.isEmpty()) {
                        lastName = attendeeDoc.getString("lastName");
                        if (lastName == null)
                            lastName = "";
                    }

                    // Format name as "FirstName LastName"
                    String fullName;
                    if (!firstName.isEmpty() && !lastName.isEmpty()) {
                        fullName = firstName + " " + lastName;
                    } else if (!firstName.isEmpty()) {
                        fullName = firstName;
                    } else if (!lastName.isEmpty()) {
                        fullName = lastName;
                    } else {
                        fullName = "Attendee";
                    }

                    System.out.println("Processing certificate for: " + fullName + " (" + email + ")");

                    // Create attendee map for certificate generation
                    Map<String, String> attendeeMap = new HashMap<>();
                    attendeeMap.put("name", fullName);
                    attendeeMap.put("email", email);
                    attendeeMap.put("firstName", firstName);
                    attendeeMap.put("lastName", lastName);

                    // Generate certificate PDF
                    byte[] certificatePdf = generateCertificate(attendeeMap, eventId);

                    // Send email with certificate
                    brevoEmailService.sendCertificateEmail(email, eventId, certificatePdf);

                    successCount++;
                    System.out.println("Certificate sent successfully to: " + email);

                } catch (Exception e) {
                    System.err.println("Error sending certificate to attendee: " + e.getMessage());
                    e.printStackTrace();
                    failCount++;
                }
            }

            String result = String.format("Certificates sent: %d successful, %d failed", successCount, failCount);
            System.out.println(result);
            return result;

        } catch (Exception e) {
            System.err.println("Error in sendCertificates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send certificates: " + e.getMessage());
        }
    }

    private Color parseColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return ColorConstants.BLACK;
        }
        try {
            // Remove the # from hex color
            String hex = hexColor.substring(1);
            // Parse the RGB values
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new DeviceRgb(r / 255f, g / 255f, b / 255f);
        } catch (Exception e) {
            System.err.println("Error parsing color: " + hexColor + ". Using black.");
            return ColorConstants.BLACK;
        }
    }

    private byte[] processImage(byte[] imageBytes) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            System.err.println("[DEBUG] processImage: Image bytes are empty or null");
            throw new IOException("Image bytes are empty or null");
        }

        System.out.println("[DEBUG] processImage: Processing image of size " + imageBytes.length + " bytes");
        System.out.println("[DEBUG] processImage: First few bytes: " + bytesToHex(imageBytes, 20));

        try {
            // Try to detect image format and get reader
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage bufferedImage = ImageIO.read(bis);

            if (bufferedImage == null) {
                System.err.println(
                        "[DEBUG] processImage: ImageIO could not decode the image format. Attempting alternative processing...");

                // Try forcing a common format by using iText directly
                return imageBytes;
            }

            System.out.println("[DEBUG] processImage: Successfully read image: " + bufferedImage.getWidth() + "x"
                    + bufferedImage.getHeight() + ", type: " + bufferedImage.getType());

            // Create a new RGB image with alpha support to ensure compatibility
            BufferedImage convertedImg = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);

            // Draw the original image onto the new one
            Graphics2D g2d = convertedImg.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
            g2d.drawImage(bufferedImage, 0, 0, null);
            g2d.dispose();

            System.out.println("[DEBUG] processImage: Converted image to ARGB format");

            // Create a new ByteArrayOutputStream
            ByteArrayOutputStream processedImage = new ByteArrayOutputStream();

            // Write the image as PNG (more compatible format)
            boolean success = ImageIO.write(convertedImg, "PNG", processedImage);

            if (!success) {
                System.err.println(
                        "[DEBUG] processImage: No appropriate image writer found for PNG format, using original bytes");
                return imageBytes;
            }

            byte[] result = processedImage.toByteArray();
            System.out
                    .println("[DEBUG] processImage: Successfully converted to PNG, size: " + result.length + " bytes");
            System.out.println("[DEBUG] processImage: First few bytes of processed image: " + bytesToHex(result, 20));

            return result;
        } catch (Exception e) {
            System.err.println("[DEBUG] processImage: Failed to process image: " + e.getMessage());
            e.printStackTrace();

            // If processing fails, return the original image bytes instead of throwing
            // exception
            return imageBytes;
        }
    }

    // Helper method to convert bytes to hex for debugging
    private String bytesToHex(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(bytes.length, limit);
        for (int i = 0; i < max; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
        }
        if (bytes.length > limit) {
            sb.append("...");
        }
        return sb.toString();
    }

    public byte[] generateCertificate(Map<String, String> attendee, String eventId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Create PDF with landscape orientation
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Set page size to landscape (swap width and height)
        // PageSize pageSize = PageSize.A4.rotate();
        // pdf.setDefaultPageSize(pageSize);

        Document document = new Document(pdf);

        try {
            System.out.println("Starting certificate generation for event: " + eventId);

            // Get the certificate template for this event
            Certificate template = getCertificateByEventId(eventId);
            System.out.println("Template found: " + (template != null ? "yes" : "no"));

            if (template != null) {
                System.out.println("Using template settings:");
                System.out.println("- Title: " + template.getTitle());
                System.out.println("- Background image: " + (template.getBackgroundImage() != null));
                System.out.println("- Logo image: " + (template.getLogoImage() != null));
                System.out.println("- Watermark: " + (template.getWatermarkImage() != null));
            }

            // Set up fonts based on template or defaults
            PdfFont titleFont;
            PdfFont normalFont;
            PdfFont italicFont;

            try {
                // Try to use the template font if specified
                if (template != null && template.getFontFamily() != null) {
                    String fontFamily = template.getFontFamily().toLowerCase();
                    switch (fontFamily) {
                        case "times new roman":
                            titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                            normalFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
                            italicFont = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
                            break;
                        case "helvetica":
                            titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                            normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                            italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
                            break;
                        case "courier":
                            titleFont = PdfFontFactory.createFont(StandardFonts.COURIER_BOLD);
                            normalFont = PdfFontFactory.createFont(StandardFonts.COURIER);
                            italicFont = PdfFontFactory.createFont(StandardFonts.COURIER_OBLIQUE);
                            break;
                        default:
                            // Default to Times Roman if font not recognized
                            System.out
                                    .println("Unsupported font family: " + fontFamily + ". Using Times Roman instead.");
                            titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                            normalFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
                            italicFont = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
                    }
                } else {
                    // Default fonts if no template or font specified
                    titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                    normalFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
                    italicFont = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
                }
            } catch (Exception e) {
                System.err.println("Error loading fonts: " + e.getMessage() + ". Using default fonts.");
                titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
                normalFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
                italicFont = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);
            }

            System.out.println("Fonts initialized successfully");

            // Apply background image if available
            if (template != null && template.getBackgroundImage() != null) {
                System.out.println("[DEBUG] generateCertificate: Background image found in template");

                // Use a dedicated method to handle background image with multiple fallbacks
                handleBackgroundImage(document, pdf, template);
            } else {
                System.out.println("[DEBUG] generateCertificate: No background image in template");
            }

            // Add logo if available
            if (template != null && template.getLogoImage() != null) {
                try {
                    byte[] logoImageBytes = Base64.getDecoder().decode(template.getLogoImage());
                    ImageData logoImageData = ImageDataFactory.create(logoImageBytes);
                    Image logoImage = new Image(logoImageData);

                    // Calculate logo position
                    float pageWidth = pdf.getDefaultPageSize().getWidth();
                    float pageHeight = pdf.getDefaultPageSize().getHeight();
                    float logoX = 0, logoY = 0;

                    switch (template.getLogoPosition()) {
                        case "top-center":
                            logoX = (pageWidth - template.getLogoWidth()) / 2;
                            logoY = pageHeight - template.getLogoHeight() - 20;
                            break;
                        case "top-right":
                            logoX = pageWidth - template.getLogoWidth() - 20;
                            logoY = pageHeight - template.getLogoHeight() - 20;
                            break;
                        case "top-left":
                        default:
                            logoX = 20;
                            logoY = pageHeight - template.getLogoHeight() - 20;
                            break;
                    }

                    logoImage.setFixedPosition(logoX, logoY);
                    logoImage.setWidth(template.getLogoWidth());
                    logoImage.setHeight(template.getLogoHeight());
                    document.add(logoImage);
                    System.out.println("Logo added successfully");
                } catch (Exception e) {
                    System.err.println("Error adding logo: " + e.getMessage());
                }
            }

            // Add watermark if available
            if (template != null && template.getWatermarkImage() != null) {
                try {
                    byte[] watermarkImageBytes = Base64.getDecoder().decode(template.getWatermarkImage());
                    ImageData watermarkImageData = ImageDataFactory.create(watermarkImageBytes);
                    Image watermarkImage = new Image(watermarkImageData);

                    float pageWidth = pdf.getDefaultPageSize().getWidth();
                    float pageHeight = pdf.getDefaultPageSize().getHeight();
                    watermarkImage.setFixedPosition((pageWidth - watermarkImage.getImageWidth()) / 2,
                            (pageHeight - watermarkImage.getImageHeight()) / 2);
                    watermarkImage.setOpacity(template.getWatermarkImageOpacity());
                    document.add(watermarkImage);
                    System.out.println("Watermark added successfully");
                } catch (Exception e) {
                    System.err.println("Error adding watermark: " + e.getMessage());
                }
            }

            // Add certificate content
            // Title
            String titleText = template != null && template.getTitle() != null ? template.getTitle() : "CERTIFICATE";
            Color titleColor = template != null && template.getHeaderColor() != null
                    ? parseColor(template.getHeaderColor())
                    : ColorConstants.BLACK;
            Paragraph title = new Paragraph(titleText)
                    .setFont(titleFont)
                    .setFontSize(36)
                    .setFontColor(titleColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(50);
            document.add(title);

            // Subtitle
            String subtitleText = template != null && template.getSubtitle() != null ? template.getSubtitle()
                    : "OF ACHIEVEMENT";
            Paragraph subtitle = new Paragraph(subtitleText)
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setFontColor(titleColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(50);
            document.add(subtitle);

            // Recipient intro
            Color textColor = template != null && template.getTextColor() != null ? parseColor(template.getTextColor())
                    : ColorConstants.BLACK;
            String recipientText = template != null && template.getRecipientText() != null ? template.getRecipientText()
                    : "THIS CERTIFICATE IS PROUDLY PRESENTED TO";
            document.add(new Paragraph(recipientText)
                    .setFont(normalFont)
                    .setFontSize(14)
                    .setFontColor(textColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Recipient name - format as "FirstName LastName"
            String lastName = attendee.get("lastName") != null ? attendee.get("lastName") : "";
            String firstName = attendee.get("firstName") != null ? attendee.get("firstName") : "";
            String recipientName;
            if (!firstName.isEmpty() && !lastName.isEmpty()) {
                recipientName = firstName + " " + lastName;
            } else if (!firstName.isEmpty()) {
                recipientName = firstName;
            } else if (!lastName.isEmpty()) {
                recipientName = lastName;
            } else {
                recipientName = attendee.get("name") != null ? attendee.get("name") : "Attendee";
            }
            document.add(new Paragraph(recipientName)
                    .setFont(titleFont)
                    .setFontSize(28)
                    .setFontColor(titleColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30));

            // Description
            String description = template != null && template.getDescription() != null ? template.getDescription()
                    : "For actively participating in";
            document.add(new Paragraph(description)
                    .setFont(italicFont)
                    .setFontSize(16)
                    .setFontColor(textColor)
                    .setTextAlignment(TextAlignment.CENTER));

            // Get event details from Firestore
            String eventName;
            String eventDate = "";
            try {
                System.out.println("Fetching event details for ID: " + eventId);

                // First attempt: direct lookup with the provided ID
                DocumentSnapshot eventDoc = FirestoreClient.getFirestore()
                        .collection("events")
                        .document(eventId)
                        .get()
                        .get();

                // If document doesn't exist or doesn't have eventName, try a query approach
                if (!eventDoc.exists() || eventDoc.getString("eventName") == null) {
                    System.out.println("Direct lookup failed. Trying to find event by eventId field...");

                    // Try to find events where eventId field matches our input
                    ApiFuture<QuerySnapshot> queryFuture = FirestoreClient.getFirestore()
                            .collection("events")
                            .whereEqualTo("eventId", eventId)
                            .limit(1)
                            .get();

                    List<QueryDocumentSnapshot> documents = queryFuture.get().getDocuments();

                    if (!documents.isEmpty()) {
                        eventDoc = documents.get(0);
                        System.out.println("Found event through query by eventId field");
                    } else {
                        System.out.println("No event found with eventId field = " + eventId);
                    }
                }

                // Get event name with fallbacks
                if (eventDoc.exists()) {
                    System.out.println("Event document data: " + eventDoc.getData());

                    eventName = eventDoc.getString("eventName");
                    System.out.println("Extracted eventName: " + eventName);

                    if (eventName == null || eventName.isEmpty()) {
                        eventName = eventDoc.getString("name");
                        if (eventName == null || eventName.isEmpty()) {
                            eventName = eventDoc.getString("title");
                            if (eventName == null || eventName.isEmpty()) {
                                // Last resort - try to get any field that might have event name info
                                Map<String, Object> eventData = eventDoc.getData();
                                for (String key : eventData.keySet()) {
                                    if (key.toLowerCase().contains("name") || key.toLowerCase().contains("title")) {
                                        Object value = eventData.get(key);
                                        if (value != null && value instanceof String) {
                                            eventName = (String) value;
                                            System.out.println("Using alternative field for event name: " + key);
                                            break;
                                        }
                                    }
                                }

                                // If we still don't have a name, use a friendly format
                                if (eventName == null || eventName.isEmpty()) {
                                    // Check if the certificate template has an eventName field
                                    if (template != null && template.getEventName() != null
                                            && !template.getEventName().isEmpty()) {
                                        eventName = template.getEventName();
                                        System.out.println("Using certificate template's eventName: " + eventName);
                                    } else {
                                        eventName = "Event #" + eventId.substring(0, Math.min(8, eventId.length()));
                                        System.out.println("No event name found, using formatted ID: " + eventName);
                                    }
                                }
                            }
                        }
                    }

                    System.out.println("Final event name to use: " + eventName);

                    // Get event date
                    if (eventDoc.contains("date")) {
                        eventDate = eventDoc.getString("date");
                        System.out.println("Found event date: " + eventDate);
                    } else if (eventDoc.contains("eventDate")) {
                        eventDate = eventDoc.getString("eventDate");
                        System.out.println("Found event date: " + eventDate);
                    } else {
                        // Try to find date in timestamp fields
                        Object timestamp = eventDoc.get("timestamp");
                        if (timestamp != null) {
                            if (timestamp instanceof com.google.cloud.Timestamp) {
                                com.google.cloud.Timestamp ts = (com.google.cloud.Timestamp) timestamp;
                                eventDate = java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                                        .format(java.time.LocalDateTime.ofInstant(
                                                ts.toDate().toInstant(),
                                                java.time.ZoneId.systemDefault()));
                                System.out.println("Converted timestamp to date: " + eventDate);
                            } else {
                                eventDate = timestamp.toString();
                            }
                        } else {
                            System.out.println("No date field found in event document");
                        }
                    }
                } else {
                    // No event document found, check if certificate template has event name
                    if (template != null && template.getEventName() != null && !template.getEventName().isEmpty()) {
                        eventName = template.getEventName();
                        System.out.println("Using certificate template's eventName instead: " + eventName);
                    } else {
                        throw new Exception(
                                "Could not find event document or certificate eventName for ID: " + eventId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching event details: " + e.getMessage());
                // First check if the certificate template has an eventName field
                if (template != null && template.getEventName() != null && !template.getEventName().isEmpty()) {
                    eventName = template.getEventName();
                    System.out.println("Using certificate template's eventName as fallback: " + eventName);
                } else {
                    // Use a more user-friendly fallback instead of just the raw ID
                    eventName = "Event #" + eventId.substring(0, Math.min(8, eventId.length()));
                }
                eventDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                System.out.println("Using fallback event name: " + eventName);
            }

            // Event name
            document.add(new Paragraph(eventName)
                    .setFont(titleFont)
                    .setFontSize(24)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            // Add event date with more prominence
            if (eventDate != null && !eventDate.isEmpty()) {
                // Create a styled date section
                Paragraph dateSection = new Paragraph()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);

                // Add "Date:" text with normal font
                Text dateLabel = new Text("Date: ").setFont(normalFont).setFontSize(14);

                // Add the actual date with italic/bold font for emphasis
                Text dateValue = new Text(eventDate).setFont(italicFont).setFontSize(14);

                // Combine the label and value in the paragraph
                dateSection.add(dateLabel).add(dateValue);

                // Add to document
                document.add(dateSection);

                System.out.println("Added event date to certificate: " + eventDate);
            } else {
                System.out.println("No event date available to display");
            }

            // Add signatories if available
            if (template != null && template.getSignatories() != null && !template.getSignatories().isEmpty()) {
                float signatureY = 150f;
                for (Map<String, String> signatory : template.getSignatories()) {
                    // Add signature image if available
                    if (template.getSignatureImages() != null &&
                            template.getSignatureImages().containsKey(signatory.get("name"))) {
                        try {
                            byte[] signatureBytes = Base64.getDecoder().decode(
                                    template.getSignatureImages().get(signatory.get("name")));
                            ImageData signatureImageData = ImageDataFactory.create(signatureBytes);
                            Image signatureImage = new Image(signatureImageData);
                            float signatureX = (pdf.getDefaultPageSize().getWidth() - 100) / 2;
                            signatureImage.setFixedPosition(signatureX, signatureY);
                            signatureImage.scaleToFit(100, 50);
                            document.add(signatureImage);
                            signatureY -= 20;
                        } catch (Exception e) {
                            System.err.println("Error adding signature image: " + e.getMessage());
                        }
                    }

                    // Add signatory name and title
                    document.add(new Paragraph(signatory.get("name"))
                            .setFont(titleFont)
                            .setFontSize(12)
                            .setTextAlignment(TextAlignment.CENTER));

                    document.add(new Paragraph(signatory.get("title"))
                            .setFont(normalFont)
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(20));
                }
            } else {
                // Default signature line
                LineSeparator line = new LineSeparator(new SolidLine());
                line.setWidth(UnitValue.createPercentValue(40));
                line.setHorizontalAlignment(HorizontalAlignment.CENTER);
                line.setMarginBottom(5);
                document.add(line);

                document.add(new Paragraph("Event Representative")
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(30));
            }

            // Add QR code if enabled
            if (template != null && template.isShowQRCode()) {
                try {
                    BarcodeQRCode qrCode = new BarcodeQRCode("Certificate for " + recipientName +
                            " - Event: " + eventName);
                    PdfFormXObject qrCodeImage = qrCode.createFormXObject(ColorConstants.BLACK, pdf);
                    float qrX = 0, qrY = 0;
                    switch (template.getQrCodePosition()) {
                        case "bottom-left":
                            qrX = 20;
                            qrY = 20;
                            break;
                        case "top-right":
                            qrX = pdf.getDefaultPageSize().getWidth() - 100 - 20;
                            qrY = pdf.getDefaultPageSize().getHeight() - 100 - 20;
                            break;
                        case "top-left":
                            qrX = 20;
                            qrY = pdf.getDefaultPageSize().getHeight() - 100 - 20;
                            break;
                        case "bottom-right":
                        default:
                            qrX = pdf.getDefaultPageSize().getWidth() - 100 - 20;
                            qrY = 20;
                            break;
                    }
                    Image qrCodeImg = new Image(qrCodeImage);
                    qrCodeImg.setFixedPosition(qrX, qrY);
                    qrCodeImg.scaleToFit(100, 100);
                    document.add(qrCodeImg);
                    System.out.println("QR code added successfully");
                } catch (Exception e) {
                    System.err.println("Error adding QR code: " + e.getMessage());
                }
            }

            document.close();
            byte[] pdfBytes = baos.toByteArray();
            System.out.println("Generated PDF size: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            System.err.println("Error generating certificate: " + e.getMessage());
            e.printStackTrace();
            document.close();
            byte[] pdfBytes = baos.toByteArray();
            System.out.println("Generated PDF size (after error): " + pdfBytes.length + " bytes");
            return pdfBytes;
        }
    }

    // Helper method to convert any image format to JPEG
    private byte[] convertToJpeg(byte[] imageBytes) {
        try {
            // Read the image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new IOException("Cannot read image format");
            }

            // Create a new buffered image with RGB format (no alpha)
            BufferedImage jpegImage = new BufferedImage(
                    originalImage.getWidth(),
                    originalImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);

            // Fill with white background and draw original image on top
            Graphics2D g2d = jpegImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, jpegImage.getWidth(), jpegImage.getHeight());
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();

            // Convert to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(jpegImage, "jpg", baos);

            if (!success) {
                throw new IOException("No suitable JPEG writer found");
            }

            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("[DEBUG] convertToJpeg: Error converting image: " + e.getMessage());

            // If all else fails, try force conversion to JPEG using a solid color image
            try {
                // Create a small colored rectangle as fallback
                BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = img.createGraphics();
                g2d.setColor(java.awt.Color.WHITE);
                g2d.fillRect(0, 0, 10, 10);
                g2d.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                System.err.println("[DEBUG] convertToJpeg: Used fallback solid image");
                return baos.toByteArray();
            } catch (Exception e2) {
                System.err.println("[DEBUG] convertToJpeg: Even fallback failed: " + e2.getMessage());
                throw new RuntimeException("Could not create any valid image format", e);
            }
        }
    }

    private void handleBackgroundImage(Document document, PdfDocument pdf, Certificate template) {
        try {
            System.out.println("[DEBUG] handleBackgroundImage: Starting background image processing");

            // Get the base64 image string and sanitize it
            String base64Image = template.getBackgroundImage();
            System.out.println("[DEBUG] handleBackgroundImage: Original base64 image length: " + base64Image.length());

            // Sanitize the base64 string to ensure it's properly formatted
            base64Image = sanitizeBase64(base64Image);
            System.out.println("[DEBUG] handleBackgroundImage: Sanitized base64 length: " + base64Image.length());

            // Attempt to decode the base64 data
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Image);
                System.out.println(
                        "[DEBUG] handleBackgroundImage: Decoded base64 data, size: " + imageBytes.length + " bytes");
                System.out.println("[DEBUG] handleBackgroundImage: First few bytes: " + bytesToHex(imageBytes, 16));
            } catch (IllegalArgumentException e) {
                System.err.println("[DEBUG] handleBackgroundImage: Failed to decode base64 data: " + e.getMessage());
                // If we can't decode, use a default colored rectangle instead
                addColoredBackground(document, pdf, template);
                return;
            }

            // Check if the decoded data actually resembles an image
            if (!isValidImageData(imageBytes)) {
                System.err.println("[DEBUG] handleBackgroundImage: Decoded data doesn't appear to be a valid image");

                // Try aggressive sanitization - remove non-alphabetic characters
                String sanitizedAgain = base64Image.replaceAll("[^A-Za-z0-9+/=]", "");
                if (!sanitizedAgain.equals(base64Image)) {
                    System.out.println("[DEBUG] handleBackgroundImage: Attempting more aggressive base64 sanitization");
                    try {
                        byte[] newImageBytes = Base64.getDecoder().decode(sanitizedAgain);
                        if (isValidImageData(newImageBytes)) {
                            System.out.println("[DEBUG] handleBackgroundImage: Aggressive sanitization worked!");
                            imageBytes = newImageBytes;
                        }
                    } catch (Exception e) {
                        System.err.println("[DEBUG] handleBackgroundImage: Aggressive sanitization failed");
                    }
                }
            }

            // If image data is too small, use colored background
            if (imageBytes.length < 8) {
                System.err.println("[DEBUG] handleBackgroundImage: Image data too small to be valid");
                addColoredBackground(document, pdf, template);
                return;
            }

            // Try different approaches in sequence
            boolean success = false;

            // Approach 1: Try to create a Java BufferedImage and convert to PNG
            success = tryBufferedImageApproach(document, pdf, template, imageBytes);
            if (success)
                return;

            // Approach 2: Try creating a new solid-color image with the right dimensions
            success = tryCreateNewImageApproach(document, pdf, template);
            if (success)
                return;

            // Approach 3: Try with explicit format type
            success = tryExplicitFormatApproach(document, pdf, template, imageBytes);
            if (success)
                return;

            // Approach 4: Create a solid color background as last resort
            addColoredBackground(document, pdf, template);

        } catch (Exception e) {
            System.err.println("[DEBUG] handleBackgroundImage: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            // Fallback to colored background
            try {
                addColoredBackground(document, pdf, template);
            } catch (Exception e2) {
                System.err.println("[DEBUG] handleBackgroundImage: Even fallback failed: " + e2.getMessage());
            }
        }
    }

    private boolean tryBufferedImageApproach(Document document, PdfDocument pdf, Certificate template,
            byte[] imageBytes) {
        try {
            System.out.println("[DEBUG] tryBufferedImageApproach: Converting image using BufferedImage");

            // Create a new image from scratch
            int width = 800; // Default width if can't determine
            int height = 600; // Default height if can't determine

            // Try to read the image data
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage != null) {
                width = originalImage.getWidth();
                height = originalImage.getHeight();
                System.out
                        .println("[DEBUG] tryBufferedImageApproach: Successfully read image: " + width + "x" + height);
            } else {
                System.out.println("[DEBUG] tryBufferedImageApproach: Could not read image, using default dimensions");
                return false;
            }

            // Create a new image with RGB format
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = newImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE); // White background
            g2d.fillRect(0, 0, width, height);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();

            // Save as PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean writeSuccess = ImageIO.write(newImage, "png", baos);
            if (!writeSuccess) {
                System.err.println("[DEBUG] tryBufferedImageApproach: Failed to write PNG");
                return false;
            }

            // Create temp file for more reliable handling
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("certificate_bg_", ".png");
            java.nio.file.Files.write(tempFile, baos.toByteArray());
            System.out.println("[DEBUG] tryBufferedImageApproach: Created temp file: " + tempFile);

            // Add to PDF
            try {
                ImageData imageData = ImageDataFactory.create(tempFile.toUri().toURL());
                Image image = new Image(imageData);
                image.setFixedPosition(0, 0);
                image.setWidth(pdf.getDefaultPageSize().getWidth());
                image.setHeight(pdf.getDefaultPageSize().getHeight());

                // Set opacity
                float opacity = template.getBackgroundImageOpacity();
                if (opacity <= 0 || opacity > 1)
                    opacity = 0.3f;
                image.setOpacity(opacity);

                document.add(image);
                java.nio.file.Files.deleteIfExists(tempFile);
                System.out.println("[DEBUG] tryBufferedImageApproach: Success!");
                return true;
            } catch (Exception e) {
                System.err.println("[DEBUG] tryBufferedImageApproach: Failed: " + e.getMessage());
                java.nio.file.Files.deleteIfExists(tempFile);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] tryBufferedImageApproach: Error: " + e.getMessage());
            return false;
        }
    }

    private boolean tryCreateNewImageApproach(Document document, PdfDocument pdf, Certificate template) {
        try {
            System.out.println("[DEBUG] tryCreateNewImageApproach: Creating a new image from scratch");

            // Create a new RGB image with dimensions matching the PDF page
            int width = (int) pdf.getDefaultPageSize().getWidth();
            int height = (int) pdf.getDefaultPageSize().getHeight();

            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = newImage.createGraphics();

            // Fill with background color from template
            java.awt.Color bgColor = java.awt.Color.WHITE; // Default
            if (template.getBackgroundColor() != null && !template.getBackgroundColor().isEmpty()) {
                try {
                    bgColor = java.awt.Color.decode(template.getBackgroundColor());
                } catch (Exception e) {
                    System.err.println("[DEBUG] tryCreateNewImageApproach: Invalid color format: "
                            + template.getBackgroundColor());
                }
            }

            g2d.setColor(bgColor);
            g2d.fillRect(0, 0, width, height);

            // Add some visual elements to make it look nice
            int margin = width / 20;
            g2d.setColor(new java.awt.Color(bgColor.getRed(),
                    bgColor.getGreen(),
                    bgColor.getBlue(),
                    50)); // Transparent version of bg color

            // Draw a decorative border
            g2d.fillRect(margin, margin, width - 2 * margin, height - 2 * margin);

            g2d.dispose();

            // Save as PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean writeSuccess = ImageIO.write(newImage, "png", baos);
            if (!writeSuccess) {
                System.err.println("[DEBUG] tryCreateNewImageApproach: Failed to write PNG");
                return false;
            }

            // Create temp file for more reliable handling
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("certificate_bg_new_", ".png");
            java.nio.file.Files.write(tempFile, baos.toByteArray());
            System.out.println("[DEBUG] tryCreateNewImageApproach: Created temp file: " + tempFile);

            // Add to PDF
            try {
                ImageData imageData = ImageDataFactory.create(tempFile.toUri().toURL());
                Image image = new Image(imageData);
                image.setFixedPosition(0, 0);
                image.setWidth(pdf.getDefaultPageSize().getWidth());
                image.setHeight(pdf.getDefaultPageSize().getHeight());

                // Set opacity
                float opacity = template.getBackgroundImageOpacity();
                if (opacity <= 0 || opacity > 1)
                    opacity = 0.3f;
                image.setOpacity(opacity);

                document.add(image);
                java.nio.file.Files.deleteIfExists(tempFile);
                System.out.println("[DEBUG] tryCreateNewImageApproach: Success!");
                return true;
            } catch (Exception e) {
                System.err.println("[DEBUG] tryCreateNewImageApproach: Failed: " + e.getMessage());
                java.nio.file.Files.deleteIfExists(tempFile);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] tryCreateNewImageApproach: Error: " + e.getMessage());
            return false;
        }
    }

    private boolean tryExplicitFormatApproach(Document document, PdfDocument pdf, Certificate template,
            byte[] imageBytes) {
        try {
            System.out.println("[DEBUG] tryExplicitFormatApproach: Trying with explicit format types");

            // List of common image formats to try
            String[] formatTypes = {
                    "JPEG", "PNG", "GIF", "BMP", "TIFF"
            };

            // Try each format
            for (String formatName : formatTypes) {
                try {
                    System.out.println("[DEBUG] tryExplicitFormatApproach: Trying " + formatName + " format");

                    // Create temporary file with the specific extension
                    java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("certificate_bg_",
                            "." + formatName.toLowerCase());
                    java.nio.file.Files.write(tempFile, imageBytes);

                    // Try to create an image from the temp file
                    ImageData imageData = ImageDataFactory.create(tempFile.toUri().toURL());
                    Image image = new Image(imageData);
                    image.setFixedPosition(0, 0);
                    image.setWidth(pdf.getDefaultPageSize().getWidth());
                    image.setHeight(pdf.getDefaultPageSize().getHeight());

                    // Set opacity
                    float opacity = template.getBackgroundImageOpacity();
                    if (opacity <= 0 || opacity > 1)
                        opacity = 0.3f;
                    image.setOpacity(opacity);

                    document.add(image);

                    // Clean up the temp file
                    java.nio.file.Files.deleteIfExists(tempFile);

                    System.out.println("[DEBUG] tryExplicitFormatApproach: Success with " + formatName + " format!");
                    return true;
                } catch (Exception e) {
                    System.err.println("[DEBUG] tryExplicitFormatApproach: Failed with format " + formatName + ": "
                            + e.getMessage());
                    // Continue to next format
                }
            }

            // Direct approach using raw bytes
            try {
                System.out.println("[DEBUG] tryExplicitFormatApproach: Trying direct byte array approach");
                ImageData imageData = ImageDataFactory.create(imageBytes);
                Image image = new Image(imageData);
                image.setFixedPosition(0, 0);
                image.setWidth(pdf.getDefaultPageSize().getWidth());
                image.setHeight(pdf.getDefaultPageSize().getHeight());

                // Set opacity
                float opacity = template.getBackgroundImageOpacity();
                if (opacity <= 0 || opacity > 1)
                    opacity = 0.3f;
                image.setOpacity(opacity);

                document.add(image);
                System.out.println("[DEBUG] tryExplicitFormatApproach: Success with direct byte array approach!");
                return true;
            } catch (Exception e) {
                System.err.println(
                        "[DEBUG] tryExplicitFormatApproach: Failed with direct byte array approach: " + e.getMessage());
            }

            // If we've tried all formats and none worked
            return false;
        } catch (Exception e) {
            System.err.println("[DEBUG] tryExplicitFormatApproach: Error: " + e.getMessage());
            return false;
        }
    }

    private void addColoredBackground(Document document, PdfDocument pdf, Certificate template) {
        try {
            System.out.println("[DEBUG] addColoredBackground: Creating colored rectangle as background");

            // Extract background color from template or use default
            com.itextpdf.kernel.colors.Color bgColor;
            if (template.getBackgroundColor() != null && !template.getBackgroundColor().isEmpty()) {
                bgColor = parseColor(template.getBackgroundColor());
            } else {
                bgColor = ColorConstants.WHITE;
            }

            // Create a rectangle for the background
            Rectangle rect = new Rectangle(0, 0, pdf.getDefaultPageSize().getWidth(),
                    pdf.getDefaultPageSize().getHeight());
            PdfCanvas canvas = new PdfCanvas(pdf.getFirstPage());
            canvas.saveState()
                    .setFillColor(bgColor)
                    .rectangle(rect)
                    .fill()
                    .restoreState();

            System.out.println("[DEBUG] addColoredBackground: Added colored background successfully");
        } catch (Exception e) {
            System.err.println("[DEBUG] addColoredBackground: Failed: " + e.getMessage());
        }
    }

    /**
     * Sanitizes a base64 string to ensure it's properly formatted for decoding
     */
    private String sanitizeBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            System.err.println("[DEBUG] sanitizeBase64: Input is null or empty");
            return "";
        }

        // Log first and last few characters for debugging
        String start = base64String.length() > 20 ? base64String.substring(0, 20) : base64String;
        String end = base64String.length() > 20 ? base64String.substring(base64String.length() - 20) : "";
        System.out.println("[DEBUG] sanitizeBase64: String starts with: " + start + "...");
        System.out.println("[DEBUG] sanitizeBase64: String ends with: ..." + end);

        // Remove any data URI prefix (e.g., "data:image/png;base64,")
        if (base64String.contains(",")) {
            base64String = base64String.substring(base64String.indexOf(",") + 1);
            System.out.println("[DEBUG] sanitizeBase64: Removed data URI prefix");
        }

        // Remove any whitespace, newlines, carriage returns
        base64String = base64String.replaceAll("\\s", "");
        System.out.println("[DEBUG] sanitizeBase64: Removed whitespace");

        // Remove any non-base64 characters
        base64String = base64String.replaceAll("[^A-Za-z0-9+/=]", "");
        System.out.println("[DEBUG] sanitizeBase64: Removed non-base64 characters");

        // Make sure padding is correct - base64 must be a multiple of 4
        int padLength = 4 - (base64String.length() % 4);
        if (padLength < 4) {
            for (int i = 0; i < padLength; i++) {
                base64String += "=";
            }
            System.out.println("[DEBUG] sanitizeBase64: Added " + padLength + " padding character(s)");
        }

        return base64String;
    }

    private boolean isValidImageData(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 4) {
            System.err.println("[DEBUG] isValidImageData: Image data is null or too small");
            return false;
        }

        System.out.println("[DEBUG] isValidImageData: Checking " + imageBytes.length + " bytes");

        // Check for common image format signatures
        try {
            // JPEG signature check (starts with FF D8 FF)
            if (imageBytes[0] == (byte) 0xFF &&
                    imageBytes[1] == (byte) 0xD8 &&
                    imageBytes[2] == (byte) 0xFF) {
                System.out.println("[DEBUG] isValidImageData: Found JPEG signature");
                return true;
            }

            // PNG signature check (starts with 89 50 4E 47 0D 0A 1A 0A)
            if (imageBytes[0] == (byte) 0x89 &&
                    imageBytes[1] == (byte) 0x50 &&
                    imageBytes[2] == (byte) 0x4E &&
                    imageBytes[3] == (byte) 0x47) {
                System.out.println("[DEBUG] isValidImageData: Found PNG signature");
                return true;
            }

            // GIF signature check (starts with GIF87a or GIF89a)
            if (imageBytes[0] == 'G' &&
                    imageBytes[1] == 'I' &&
                    imageBytes[2] == 'F') {
                System.out.println("[DEBUG] isValidImageData: Found GIF signature");
                return true;
            }

            // BMP signature check (starts with BM)
            if (imageBytes[0] == 'B' &&
                    imageBytes[1] == 'M') {
                System.out.println("[DEBUG] isValidImageData: Found BMP signature");
                return true;
            }

            // Try to load image using ImageIO as final check
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage img = ImageIO.read(bis);
                if (img != null) {
                    System.out.println("[DEBUG] isValidImageData: ImageIO successfully read image format");
                    return true;
                }
            }

            System.err.println("[DEBUG] isValidImageData: No valid image signature found");
            System.err.println("[DEBUG] isValidImageData: First bytes: " + bytesToHex(imageBytes, 16));
            return false;

        } catch (Exception e) {
            System.err.println("[DEBUG] isValidImageData: Error checking image data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clean up orphaned certificates - certificates with non-existent events
     * This can be called manually or periodically to ensure database consistency
     */
    public String cleanupOrphanedCertificates() throws ExecutionException, InterruptedException {
        System.out.println("[DEBUG] Starting cleanup of orphaned certificates");
        int orphanCount = 0;

        try {
            // 1. Get all certificates with eventId
            List<Certificate> certificates = getAllCertificates();
            List<Certificate> certificatesWithEventId = certificates.stream()
                    .filter(cert -> cert.getEventId() != null && !cert.getEventId().isEmpty())
                    .collect(Collectors.toList());

            System.out.println("[DEBUG] Found " + certificatesWithEventId.size() + " certificates with eventId out of "
                    + certificates.size() + " total");

            // 2. Get all event IDs
            CollectionReference eventsCollection = firestore.collection("events");
            ApiFuture<QuerySnapshot> eventsFuture = eventsCollection.get();
            List<String> validEventIds = new ArrayList<>();
            for (DocumentSnapshot doc : eventsFuture.get().getDocuments()) {
                validEventIds.add(doc.getId());
            }

            System.out.println("[DEBUG] Found " + validEventIds.size() + " valid event IDs");

            // 3. Find and delete orphaned certificates
            List<Certificate> orphanedCerts = certificatesWithEventId.stream()
                    .filter(cert -> !validEventIds.contains(cert.getEventId()))
                    .collect(Collectors.toList());

            System.out.println("[DEBUG] Found " + orphanedCerts.size() + " orphaned certificates to delete");

            // 4. Delete each orphaned certificate
            for (Certificate orphan : orphanedCerts) {
                try {
                    System.out.println("[DEBUG] Deleting orphaned certificate: " + orphan.getId() +
                            " for event ID: " + orphan.getEventId() +
                            " (Event name: " + orphan.getEventName() + ")");
                    deleteCertificate(orphan.getId());
                    orphanCount++;
                } catch (Exception e) {
                    System.err.println(
                            "[ERROR] Failed to delete orphaned certificate " + orphan.getId() + ": " + e.getMessage());
                }
            }

            return "Successfully cleaned up " + orphanCount + " orphaned certificates";

        } catch (Exception e) {
            System.err.println("[ERROR] Error cleaning up orphaned certificates: " + e.getMessage());
            e.printStackTrace();
            return "Failed to clean up orphaned certificates: " + e.getMessage();
        }
    }
}