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
} 