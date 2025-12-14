package com.capstone.TimEd.controller;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.TimEd.service.AttendanceService;
import com.capstone.TimEd.service.CertificateService;
import com.capstone.TimEd.service.EmailService;
import com.capstone.TimEd.service.FirebaseEmailService;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private final CertificateService certificateService;
    private final EmailService emailService;
    private final FirebaseEmailService firebaseEmailService;
    private final AttendanceService attendanceService;

    @Autowired
    public AttendanceController(
            AttendanceService attendanceService,
            CertificateService certificateService,
            EmailService emailService,
            FirebaseEmailService firebaseEmailService) {
        this.attendanceService = attendanceService;
        this.certificateService = certificateService;
        this.emailService = emailService;
        this.firebaseEmailService = firebaseEmailService;
    }

    @PostMapping("/{eventId}/{userId}/refresh-selfie")
    public ResponseEntity<String> refreshSelfie(
            @PathVariable String eventId,
            @PathVariable String userId) {
        String result = attendanceService.refreshSelfie(eventId, userId);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{eventId}/{userId}")
    public ResponseEntity<String> markAttendance(
            @PathVariable String eventId,
            @PathVariable String userId,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, String> payload) {
        try {
            System.out.println("Marking attendance for userId: " + userId + " in eventId: " + eventId);

            String requestFirstName = null;
            String requestLastName = null;
            if (payload != null) {
                requestFirstName = payload.get("firstName");
                requestLastName = payload.get("lastName");
                System.out.println("Received user details from client - firstName: " + requestFirstName + ", lastName: " + requestLastName);
            }

            String result = attendanceService.markAttendance(eventId, userId, requestFirstName, requestLastName);
            System.out.println("Attendance marked result: " + result);

        // Only proceed if the user hasn't already timed in
        if (!result.contains("Already timed in")) {
            try {
                System.out.println("Processing certificate email for userId: " + userId + " in eventId: " + eventId);
                
                List<QueryDocumentSnapshot> attendeeDocs = FirestoreClient.getFirestore()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("type", "event_time_in")
                        .get()
                        .get()
                        .getDocuments();

                if (!attendeeDocs.isEmpty()) {
                    DocumentSnapshot userDoc = attendeeDocs.get(0);

                    String email = userDoc.getString("email");
                    // Use request names if available, otherwise fallback to document
                    String firstName = (requestFirstName != null && !requestFirstName.isEmpty()) ? requestFirstName : userDoc.getString("firstName");
                    String lastName = (requestLastName != null && !requestLastName.isEmpty()) ? requestLastName : (userDoc.contains("lastName") ? userDoc.getString("lastName") : "");

                    if (email == null || email.isEmpty()) {
                        System.err.println("Email is missing for userId: " + userId);
                        return ResponseEntity.ok(result + " (Note: Email not found)");
                    }

                    Map<String, String> userAttendance = new HashMap<>();
                    userAttendance.put("userId", userId);
                    userAttendance.put("email", email);
                    userAttendance.put("firstName", firstName);
                    userAttendance.put("lastName", lastName);
                    userAttendance.put("manualEntry", "false");
                    userAttendance.put("checkinMethod", "false"); // QR code check-in
                    userAttendance.put("timeIn", userDoc.getString("timestamp"));
                    userAttendance.put("timeOut", "");

                    System.out.println("Generating certificate using - FirstName: '" + firstName + "', LastName: '" + lastName + "'");
                    byte[] certificatePdf = certificateService.generateCertificate(userAttendance, eventId);

                    System.out.println("Sending certificate via Firebase to " + email);
                    firebaseEmailService.sendCertificateEmail(email, eventId, certificatePdf);
                    System.out.println("Certificate email successfully queued via Firebase for " + email);
                } else {
                    System.err.println("No attendee record found for userId: " + userId + " on first attempt. Retrying after delay...");
                    
                    // Retry after a short delay to handle Firestore eventual consistency
                    try {
                        Thread.sleep(2000); // Wait 2 seconds for Firestore consistency
                        
                        List<QueryDocumentSnapshot> retryAttendeeDocs = FirestoreClient.getFirestore()
                                .collection("events")
                                .document(eventId)
                                .collection("attendees")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("type", "event_time_in")
                                .get()
                                .get()
                                .getDocuments();
                        
                        if (!retryAttendeeDocs.isEmpty()) {
                            System.out.println("Retry successful: Found attendee record for userId: " + userId);
                            DocumentSnapshot userDoc = retryAttendeeDocs.get(0);

                            String email = userDoc.getString("email");
                            String firstName = userDoc.getString("firstName");
                            String lastName = userDoc.contains("lastName") ? userDoc.getString("lastName") : "";

                            if (email != null && !email.isEmpty()) {
                                Map<String, String> userAttendance = new HashMap<>();
                                userAttendance.put("userId", userId);
                                userAttendance.put("email", email);
                                userAttendance.put("firstName", firstName);
                                userAttendance.put("lastName", lastName);
                                userAttendance.put("manualEntry", "false");
                                userAttendance.put("checkinMethod", "false"); // QR code check-in
                                userAttendance.put("timeIn", userDoc.getString("timestamp"));
                                userAttendance.put("timeOut", "");

                                System.out.println("Generating certificate for " + firstName + " " + lastName + " (retry)");
                                byte[] certificatePdf = certificateService.generateCertificate(userAttendance, eventId);

                                System.out.println("Sending certificate via Firebase to " + email + " (retry)");
                                firebaseEmailService.sendCertificateEmail(email, eventId, certificatePdf);
                                System.out.println("Certificate email successfully queued via Firebase for " + email + " (retry)");
                                
                                return ResponseEntity.ok(result); // Success: certificate sent
                            } else {
                                System.err.println("Email is missing for userId: " + userId + " (retry)");
                                return ResponseEntity.ok(result + " (Note: Email not found)");
                            }
                        } else {
                            System.err.println("Retry failed: Still no attendee record found for userId: " + userId);
                            return ResponseEntity.ok(result + " (Note: No attendee record found)");
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Retry interrupted for userId: " + userId);
                        return ResponseEntity.ok(result + " (Note: Certificate processing interrupted)");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error during certificate generation or email sending: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.ok(result + " (Note: Certificate/email failed)");
            }
        }

        return ResponseEntity.ok(result);
    } catch (Exception e) {
        System.err.println("Error marking attendance: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error marking attendance: " + e.getMessage());
    }
}

    @PostMapping("/{eventId}/{userId}/timeout")
    public ResponseEntity<String> markTimeOut(
            @PathVariable String eventId,
            @PathVariable String userId) {
        String result = attendanceService.markTimeOut(eventId, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{eventId}/{userId}/manual/{actionType}")
    public ResponseEntity<String> manualAttendanceAction(
            @PathVariable String eventId,
            @PathVariable String userId,
            @PathVariable String actionType) {
        try {
            String result;
            if ("timein".equalsIgnoreCase(actionType)) {
                result = attendanceService.manualTimeIn(eventId, userId);

                if (!result.contains("already timed in")) {
                    List<Map<String, String>> attendees = attendanceService.getAttendees(eventId);
                    Map<String, String> userAttendance = null;
                    for (Map<String, String> attendee : attendees) {
                        if (attendee.get("userId").equals(userId)) {
                            userAttendance = attendee;
                            break;
                        }
                    }

                    if (userAttendance != null) {
                        byte[] certificatePdf = certificateService.generateCertificate(userAttendance, eventId);
                        firebaseEmailService.sendCertificateEmail(userAttendance.get("email"), eventId, certificatePdf);
                    }
                }
            } else if ("timeout".equalsIgnoreCase(actionType)) {
                result = attendanceService.manualTimeOut(eventId, userId);
            } else {
                return ResponseEntity.badRequest().body("Invalid action type. Use 'timein' or 'timeout'");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error processing manual attendance action: " + e.getMessage());
        }
    }

    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<List<Map<String, String>>> getAttendees(@PathVariable String eventId) {
        try {
            List<Map<String, String>> attendees = attendanceService.getAttendees(eventId);

            if (attendees.isEmpty()) {
                return ResponseEntity.status(404).body(null);
            }

            return ResponseEntity.ok(attendees);
        } catch (Exception e) {
            System.err.println("Error fetching attendees: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    @GetMapping("/user/{userId}/attended-events")
    public ResponseEntity<List<Map<String, Object>>> getUserAttendedEvents(@PathVariable String userId) {
        try {
            List<Map<String, Object>> events = attendanceService.getUserAttendedEvents(userId);

            if (events.isEmpty()) {
                return ResponseEntity.status(404).body(null);
            }

            return ResponseEntity.ok(events);
        } catch (Exception e) {
            System.err.println("Error fetching attended events: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    public String generateEventQrCode(String eventId, String userId) {
        try {
            String joinUrl = frontendBaseUrl + "/join-event/" + eventId + "?userId=" + userId;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(joinUrl, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    @PostMapping("/{eventId}/send-certificates")
    public ResponseEntity<String> sendCertificates(
            @PathVariable String eventId,
            @RequestParam(required = false) String userId) {
        try {
            List<Map<String, String>> attendees = attendanceService.getAttendees(eventId);

            if (attendees.isEmpty()) {
                return ResponseEntity.status(404).body("No attendees found for this event");
            }

            if (userId != null && !userId.isEmpty()) {
                for (Map<String, String> attendee : attendees) {
                    if (attendee.get("userId").equals(userId)) {
                        byte[] certificatePdf = certificateService.generateCertificate(attendee, eventId);
                        firebaseEmailService.sendCertificateEmail(attendee.get("email"), eventId, certificatePdf);
                        return ResponseEntity.ok("Certificate sent successfully to user: " + userId);
                    }
                }
                return ResponseEntity.status(404).body("User not found in event attendees");
            }

            for (Map<String, String> attendee : attendees) {
                byte[] certificatePdf = certificateService.generateCertificate(attendee, eventId);
                firebaseEmailService.sendCertificateEmail(attendee.get("email"), eventId, certificatePdf);
            }

            return ResponseEntity.ok("Certificates sent successfully to all attendees");
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error sending certificates: " + e.getMessage());
        }
    }
}
