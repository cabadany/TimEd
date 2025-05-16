package com.capstone.TimEd.controller;

import com.capstone.TimEd.service.AttendanceService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.capstone.TimEd.service.CertificateService;
import com.capstone.TimEd.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
	  private final CertificateService certificateService;
	  
	    private final EmailService emailService;
    private final AttendanceService attendanceService;
 
    @Autowired
    public AttendanceController(
            AttendanceService attendanceService,
            CertificateService certificateService,
            EmailService emailService) {
        this.attendanceService = attendanceService;
        this.certificateService = certificateService;
        this.emailService = emailService;
    }
    @PostMapping("/{eventId}/{userId}")	
    public ResponseEntity<String> markAttendance(
            @PathVariable String eventId,
            @PathVariable String userId) {
        try {
            System.out.println("Marking attendance for userId: " + userId + " in eventId: " + eventId);
            String result = attendanceService.markAttendance(eventId, userId);
            System.out.println("Attendance marked successfully: " + result);

            // Only generate and send certificate if this is a new time-in
            if (!result.contains("Already timed in")) {
                try {
                    // Get user details directly from Firestore
                    DocumentReference userRef = FirestoreClient.getFirestore()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .document(userId);
                    
                    DocumentSnapshot userDoc = userRef.get().get();
                    if (userDoc.exists()) {
                        Map<String, String> userAttendance = new HashMap<>();
                        userAttendance.put("userId", userId);
                        userAttendance.put("timeIn", userDoc.getString("timeIn"));
                        userAttendance.put("timeOut", userDoc.getString("timeOut"));
                        userAttendance.put("manualEntry", String.valueOf(userDoc.getBoolean("manualEntry")));
                        
                        // Get user email
                        DocumentSnapshot fullUserDoc = FirestoreClient.getFirestore()
                            .collection("users")
                            .document(userId)
                            .get()
                            .get();
                        
                        if (fullUserDoc.exists()) {
                            String facultyEmail = fullUserDoc.getString("email");
                            String firstName = fullUserDoc.getString("firstName");
                            String lastName = fullUserDoc.getString("lastName");
                            
                            userAttendance.put("email", facultyEmail);
                            userAttendance.put("firstName", firstName);
                            userAttendance.put("lastName", lastName);
                            
                            System.out.println("Generating certificate for faculty member: " + firstName + " " + lastName);
                            byte[] certificatePdf = certificateService.generateCertificate(userAttendance, eventId);
                            System.out.println("Certificate generated successfully");
                            
                            System.out.println("Sending certificate email to: " + facultyEmail);
                            emailService.sendCertificateEmail(facultyEmail, eventId, certificatePdf);
                            System.out.println("Certificate email sent successfully to faculty member");
                        }
                    }
                } catch (Exception e) {
                    // Log certificate error but don't fail the time-in
                    System.err.println("Error generating/sending certificate: " + e.getMessage());
                    e.printStackTrace();
                    // Still return success for the time-in
                    return ResponseEntity.ok(result + " (Note: Certificate generation failed)");
                }
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
        	  System.err.println("Error marking attendance: " + e.getMessage());
              e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body("Error marking attendance: " + e.getMessage());
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
                
                // Only generate and send certificate if this is a new time-in
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
                        emailService.sendCertificateEmail(userAttendance.get("email"), eventId, certificatePdf);
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
                return ResponseEntity.status(404).body(null);  // If no attendees found, return 404
            }

            return ResponseEntity.ok(attendees); // Return the list of attendees
        } catch (Exception e) {
            // Log the error for debugging purposes (you can use a logger here)
            System.err.println("Error fetching attendees: " + e.getMessage());
            
            // Return a 500 Internal Server Error response
            return ResponseEntity
                    .status(500)
                    .body(Collections.emptyList());  // Return an empty list if error occurs
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
            String joinUrl = "http://localhost:5173/join-event/" + eventId + "?userId=" + userId;
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
                // Send certificate only to specific user
                for (Map<String, String> attendee : attendees) {
                    if (attendee.get("userId").equals(userId)) {
                        byte[] certificatePdf = certificateService.generateCertificate(attendee, eventId);
                        emailService.sendCertificateEmail(attendee.get("email"), eventId, certificatePdf);
                        return ResponseEntity.ok("Certificate sent successfully to user: " + userId);
                    }
                }
                return ResponseEntity.status(404).body("User not found in event attendees");
            }

            // If no userId provided, send to all attendees (admin function)
            for (Map<String, String> attendee : attendees) {
                byte[] certificatePdf = certificateService.generateCertificate(attendee, eventId);
                emailService.sendCertificateEmail(attendee.get("email"), eventId, certificatePdf);
            }

            return ResponseEntity.ok("Certificates sent successfully to all attendees");
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error sending certificates: " + e.getMessage());
        }
    }
}
