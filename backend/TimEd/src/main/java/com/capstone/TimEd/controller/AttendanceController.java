package com.capstone.TimEd.controller;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.TimEd.model.Certificate;
import com.capstone.TimEd.service.AttendanceService;
import com.capstone.TimEd.service.CertificateService;
import com.capstone.TimEd.service.EmailService;
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

            if (!result.contains("Already timed in")) {
                try {
                    List<QueryDocumentSnapshot> attendeeDocs = FirestoreClient.getFirestore()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("type", "event_time_in")
                        .get()
                        .get()
                        .getDocuments();

                    System.out.println("Fetched " + attendeeDocs.size() + " attendee documents");

                    if (!attendeeDocs.isEmpty()) {
                        DocumentSnapshot userDoc = attendeeDocs.get(0);

                        Map<String, String> userAttendance = new HashMap<>();
                        userAttendance.put("userId", userId);
                        userAttendance.put("timeIn", userDoc.getString("timestamp"));
                        userAttendance.put("timeOut", "");
                        userAttendance.put("manualEntry", "false");

                        String facultyEmail = userDoc.getString("email");
                        String firstName = userDoc.getString("firstName");
                        String lastName = userDoc.contains("lastName") ? userDoc.getString("lastName") : "";

                        userAttendance.put("email", facultyEmail);
                        userAttendance.put("firstName", firstName);
                        userAttendance.put("lastName", lastName);

                        System.out.println("Generating certificate for: " + firstName + " " + lastName);

                        Certificate certificateTemplate = certificateService.getCertificateByEventId(eventId);
                        byte[] certificatePdf = certificateService.generateCertificate(userAttendance, eventId);
                        emailService.sendCertificateEmail(facultyEmail, eventId, certificatePdf);

                        System.out.println("Certificate email sent to " + facultyEmail);
                    } else {
                        System.out.println("No matching attendee document found for userId: " + userId);
                    }
                } catch (Exception e) {
                    System.err.println("Error generating/sending certificate: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.ok(result + " (Note: Certificate generation failed)");
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
                for (Map<String, String> attendee : attendees) {
                    if (attendee.get("userId").equals(userId)) {
                        byte[] certificatePdf = certificateService.generateCertificate(attendee, eventId);
                        emailService.sendCertificateEmail(attendee.get("email"), eventId, certificatePdf);
                        return ResponseEntity.ok("Certificate sent successfully to user: " + userId);
                    }
                }
                return ResponseEntity.status(404).body("User not found in event attendees");
            }

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