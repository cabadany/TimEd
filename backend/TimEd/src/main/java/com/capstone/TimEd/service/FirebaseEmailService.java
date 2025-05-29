package com.capstone.TimEd.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseEmailService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final CollectionReference mailCollection = firestore.collection("mail");

    /**
     * Send certificate email using Firebase Extensions Trigger Email
     * This replaces the SMTP-based email service to avoid double-sending issues
     */
    public void sendCertificateEmail(String toEmail, String eventId, byte[] certificatePdf) {
        try {
            System.out.println("Preparing to send certificate email via Firebase to " + toEmail);

            // Check if email already sent for this user and event to prevent duplicates
            String duplicateCheckId = eventId + "_" + extractUserIdFromEmail(toEmail);
            if (isEmailAlreadySent(duplicateCheckId)) {
                System.out.println("Certificate email already sent for user " + toEmail + " in event " + eventId + ". Skipping duplicate.");
                return;
            }

            // Get event details for the email
            String eventName = getEventName(eventId);
            
            // Convert PDF to base64 for email attachment
            String base64Pdf = Base64.getEncoder().encodeToString(certificatePdf);
            
            // Create email document for Firebase Extension
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("to", Arrays.asList(toEmail));
            emailData.put("from", "noreply@timed-system.com"); // Use your verified domain
            
            // Create message content
            Map<String, Object> message = new HashMap<>();
            message.put("subject", "Your Certificate for " + eventName);
            message.put("html", createEmailHtml(eventName));
            message.put("text", createEmailText(eventName));
            
            // Add PDF attachment
            List<Map<String, Object>> attachments = new ArrayList<>();
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", "Certificate_" + eventName.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
            attachment.put("content", base64Pdf);
            attachment.put("encoding", "base64");
            attachment.put("contentType", "application/pdf");
            attachments.add(attachment);
            message.put("attachments", attachments);
            
            emailData.put("message", message);
            
            // Add metadata for tracking and duplicate prevention
            emailData.put("eventId", eventId);
            emailData.put("emailType", "certificate");
            emailData.put("timestamp", Instant.now().toEpochMilli());
            emailData.put("userId", extractUserIdFromEmail(toEmail));
            emailData.put("duplicateCheckId", duplicateCheckId); // Add unique identifier
            
            // Add to Firestore mail collection - this triggers the Firebase Extension
            ApiFuture<DocumentReference> future = mailCollection.add(emailData);
            DocumentReference docRef = future.get();
            
            System.out.println("Certificate email queued successfully via Firebase with ID: " + docRef.getId());
            System.out.println("Email will be processed by Firebase Extensions Trigger Email");
            
        } catch (Exception e) {
            System.err.println("Error queueing certificate email via Firebase: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception to avoid breaking the attendance flow
        }
    }

    /**
     * Send a simple notification email
     */
    public void sendNotificationEmail(String toEmail, String subject, String htmlContent, String textContent) {
        try {
            System.out.println("Sending notification email via Firebase to " + toEmail);
            
            Map<String, Object> emailData = new HashMap<>();
            emailData.put("to", Arrays.asList(toEmail));
            emailData.put("from", "noreply@timed-system.com");
            
            Map<String, Object> message = new HashMap<>();
            message.put("subject", subject);
            message.put("html", htmlContent);
            message.put("text", textContent);
            
            emailData.put("message", message);
            emailData.put("emailType", "notification");
            emailData.put("timestamp", Instant.now().toEpochMilli());
            
            ApiFuture<DocumentReference> future = mailCollection.add(emailData);
            DocumentReference docRef = future.get();
            
            System.out.println("Notification email queued successfully with ID: " + docRef.getId());
            
        } catch (Exception e) {
            System.err.println("Error sending notification email via Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send bulk certificate emails to multiple attendees
     */
    public void sendBulkCertificateEmails(List<Map<String, String>> attendees, String eventId, 
                                         Map<String, byte[]> certificatePdfs) {
        try {
            System.out.println("Sending bulk certificate emails for event: " + eventId);
            String eventName = getEventName(eventId);
            
            for (Map<String, String> attendee : attendees) {
                String email = attendee.get("email");
                String attendeeKey = attendee.get("firstName") + "_" + attendee.get("lastName");
                
                if (email != null && certificatePdfs.containsKey(attendeeKey)) {
                    byte[] pdf = certificatePdfs.get(attendeeKey);
                    sendCertificateEmail(email, eventId, pdf);
                    
                    // Add small delay to avoid overwhelming the system
                    Thread.sleep(100);
                }
            }
            
            System.out.println("Bulk certificate emails queued for " + attendees.size() + " attendees");
            
        } catch (Exception e) {
            System.err.println("Error sending bulk certificate emails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check email delivery status
     */
    public Map<String, Object> checkEmailStatus(String emailDocumentId) {
        try {
            DocumentReference docRef = mailCollection.document(emailDocumentId);
            Map<String, Object> docData = docRef.get().get().getData();
            
            if (docData != null && docData.containsKey("delivery")) {
                return (Map<String, Object>) docData.get("delivery");
            }
            
            return Map.of("state", "PENDING", "message", "Email is in queue");
            
        } catch (Exception e) {
            System.err.println("Error checking email status: " + e.getMessage());
            return Map.of("state", "ERROR", "message", "Failed to check status");
        }
    }

    private String createEmailHtml(String eventName) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; margin-bottom: 20px;">
                        <h1 style="color: #007bff; margin: 0;">TimEd Certificate</h1>
                    </div>
                    
                    <h2 style="color: #28a745;">Congratulations!</h2>
                    
                    <p>Thank you for attending <strong>%s</strong>.</p>
                    
                    <p>Your certificate of attendance is attached to this email as a PDF file. Please save it for your records.</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Event:</strong> %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Certificate Date:</strong> %s</p>
                    </div>
                    
                    <p>If you have any questions, please don't hesitate to contact us.</p>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                        <p style="color: #6c757d; font-size: 14px;">
                            Best regards,<br>
                            <strong>TimEd Team</strong><br>
                            <em>Your Attendance Management System</em>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, eventName, eventName, java.time.LocalDate.now().toString());
    }

    private String createEmailText(String eventName) {
        return String.format("""
            Congratulations!
            
            Thank you for attending %s.
            
            Your certificate of attendance is attached to this email as a PDF file. 
            Please save it for your records.
            
            Event: %s
            Certificate Date: %s
            
            If you have any questions, please don't hesitate to contact us.
            
            Best regards,
            TimEd Team
            Your Attendance Management System
            """, eventName, eventName, java.time.LocalDate.now().toString());
    }

    private String getEventName(String eventId) {
        try {
            DocumentReference eventDoc = firestore.collection("events").document(eventId);
            Map<String, Object> eventData = eventDoc.get().get().getData();
            
            if (eventData != null && eventData.containsKey("eventName")) {
                return (String) eventData.get("eventName");
            }
            
            return "Event #" + eventId.substring(0, Math.min(8, eventId.length()));
            
        } catch (Exception e) {
            System.err.println("Error fetching event name: " + e.getMessage());
            return "Event #" + eventId.substring(0, Math.min(8, eventId.length()));
        }
    }

    private String extractUserIdFromEmail(String email) {
        // Extract user identifier from email for tracking
        return email.split("@")[0];
    }

    /**
     * Check if an email with the same duplicateCheckId has already been sent
     */
    private boolean isEmailAlreadySent(String duplicateCheckId) {
        try {
            // Query for existing emails with the same duplicateCheckId OR same eventId + userId combination
            var query = mailCollection.whereEqualTo("duplicateCheckId", duplicateCheckId).limit(1);
            var querySnapshot = query.get().get();
            
            boolean exists = !querySnapshot.isEmpty();
            if (exists) {
                System.out.println("Found existing email with duplicateCheckId: " + duplicateCheckId);
                System.out.println("Skipping duplicate email to prevent double-sending");
            }
            return exists;
            
        } catch (Exception e) {
            System.err.println("Error checking for duplicate emails: " + e.getMessage());
            // In case of error, allow the email to be sent (safer approach)
            return false;
        }
    }
} 