package com.capstone.TimEd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.model.Certificate;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CertificateService {

    private static final String COLLECTION_NAME = "certificates";

    @Autowired
    private Firestore firestore;

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
        
        System.out.println("Certificate created with ID: " + certificate.getId() + " for eventId: " + certificate.getEventId());
        
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
    public Certificate updateCertificate(String id, Certificate certificate) throws ExecutionException, InterruptedException {
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
        ApiFuture<QuerySnapshot> future = 
            firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("eventId", eventId)
                    .limit(1)
                    .get();
        
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        if (!documents.isEmpty()) {
            Certificate certificate = documents.get(0).toObject(Certificate.class);
            System.out.println("Found certificate: " + certificate.getId() + " for eventId: " + certificate.getEventId());
            return certificate;
        }
        
        // If no certificate found, try with the legacy format: "Event added successfully with ID: {eventId}"
        String legacyEventId = "Event added successfully with ID: " + eventId;
        ApiFuture<QuerySnapshot> legacyFuture = 
            firestore.collection(COLLECTION_NAME)
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
     * Send certificates to event attendees (stub implementation)
     */
    public String sendCertificates(String certificateId, String eventId) throws ExecutionException, InterruptedException {
        // This would be implemented to actually send certificates, but for now it's just a stub
        return "Certificates sent successfully";
    }

    public byte[] generateCertificate(Map<String, String> attendee, String eventId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Set up fonts
        PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        PdfFont subtitleFont = PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.TIMES_ROMAN);
        PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.TIMES_ITALIC);

        // Get event name from Firestore
        String eventName;
        try {
            DocumentSnapshot eventDoc = FirestoreClient.getFirestore()
                .collection("events")
                .document(eventId)
                .get()
                .get();
            eventName = eventDoc.getString("eventName");
        } catch (Exception e) {
            eventName = eventId; // Fallback to eventId if can't get name
        }

        // Add certificate content with professional design
        // Title
        Paragraph title = new Paragraph("CERTIFICATE")
                .setFont(titleFont)
                .setFontSize(36)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(50);
        document.add(title);

        // Subtitle
        Paragraph subtitle = new Paragraph("OF ACHIEVEMENT")
                .setFont(subtitleFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(50);
        document.add(subtitle);

        // Recipient intro
        document.add(new Paragraph("THIS CERTIFICATE IS PROUDLY PRESENTED TO")
                .setFont(normalFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Recipient name
        document.add(new Paragraph(attendee.get("firstName") + " " + attendee.get("lastName"))
                .setFont(titleFont)
                .setFontSize(28)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

        // Description
        document.add(new Paragraph("For actively participating in")
                .setFont(italicFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER));

        // Event name
        document.add(new Paragraph(eventName)
                .setFont(titleFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Add attendance time details
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        
        // Parse time in
        LocalDateTime timeIn;
        try {
            String timeInStr = attendee.get("timeIn");
            if (timeInStr != null && !timeInStr.equals("N/A")) {
                try {
                    timeIn = LocalDateTime.parse(timeInStr);
                } catch (Exception e) {
                    try {
                        timeIn = LocalDateTime.parse(timeInStr.substring(0, timeInStr.indexOf('.')));
                    } catch (Exception e2) {
                        timeIn = LocalDateTime.now();
                    }
                }
            } else {
                timeIn = LocalDateTime.now();
            }
        } catch (Exception e) {
            timeIn = LocalDateTime.now();
        }

        document.add(new Paragraph("Date: " + timeIn.format(formatter))
                .setFont(normalFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(50));

        // Add signatories
        document.add(new Paragraph("_______________________")
                .setFont(normalFont)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Event Representative")
                .setFont(normalFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30));

        // Add certificate number or unique identifier
        document.add(new Paragraph("Certificate ID: " + eventId + "-" + attendee.get("userId"))
                .setFont(italicFont)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30));

        document.close();
        return baos.toByteArray();
    }
} 