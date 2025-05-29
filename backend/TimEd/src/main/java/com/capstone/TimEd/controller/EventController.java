package com.capstone.TimEd.controller;

import com.capstone.TimEd.config.QRCodeGenerator;
import com.capstone.TimEd.dto.Eventdto;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.service.EventService;
import com.capstone.TimEd.service.CertificateService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/events")
public class EventController {
    
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;
    
    private final Firestore firestore = FirestoreClient.getFirestore();
    private final EventService eventService;
    private final CertificateService certificateService;

    @Autowired
    public EventController(EventService eventService, CertificateService certificateService) {
        this.eventService = eventService;
        this.certificateService = certificateService;
    }

    @PostMapping("/createEvent")
    public String saveEvent(@RequestBody Map<String, Object> eventData) throws ExecutionException, InterruptedException {
        try {
            // Create Event object manually to avoid automatic date conversion
            Event event = new Event();
            event.setEventName((String) eventData.get("eventName"));
            event.setDepartmentId((String) eventData.get("departmentId"));
            event.setDuration((String) eventData.get("duration"));
            event.setStatus((String) eventData.get("status"));
            event.setDescription((String) eventData.get("description"));
            
            // Handle date parsing manually to preserve Philippines timezone
            String dateString = (String) eventData.get("date");
            if (dateString != null) {
                System.out.println("Received date string from frontend: " + dateString);
                
                // Parse the date string as Philippines time
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                inputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                
                java.util.Date parsedDate = inputFormat.parse(dateString);
                event.setDate(parsedDate);
                
                System.out.println("Parsed date as Philippines time: " + parsedDate);
                System.out.println("Parsed date in string format: " + inputFormat.format(parsedDate));
            }
            
            return eventService.addEvent(event);
        } catch (Exception e) {
            System.err.println("Error in saveEvent: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create event: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/department")
    public Department getDepartmentForEvent(@PathVariable("id") String eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            return event.getDepartment(); // Return the department object for the event
        } catch (Exception e) {
            // Handle error (maybe log and return 404)
            return null;
        }
    }
    
    /**
     * Get certificate ID for a specific event
     * This is a compatibility endpoint that redirects to the certificate service
     */
    @GetMapping("/getCertificateId/{eventId}")
    public ResponseEntity<?> getCertificateId(@PathVariable String eventId) {
        try {
            // Redirect to the certificate service to fetch certificate by event ID
            return certificateService.getCertificateByEventId(eventId) != null
                ? ResponseEntity.ok(certificateService.getCertificateByEventId(eventId))
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No certificate found for event ID: " + eventId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificate: " + e.getMessage());
        }
    }
    
    @GetMapping("/qr/{eventId}")
    public ResponseEntity<byte[]> generateQrCode(@PathVariable String eventId) {
        try {
            String qrUrl = frontendBaseUrl + "/qr-join/" + eventId;

            byte[] image = QRCodeGenerator.generateQRCodeImage(qrUrl, 300, 300);

            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .body(image);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/getAll")
    public List<Eventdto> getAllEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            return eventService.getPaginatedEvents(page, size);
        } catch (Exception e) {
            System.err.println("Error fetching paginated events: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @GetMapping("/getByDateRange")
    public List<Eventdto> getEventsByDateRange(
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        try {
            return eventService.getEventsByDateRange(startDate, endDate);
        } catch (Exception e) {
            System.err.println("Error fetching events by date range: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @DeleteMapping("/deleteEvent/{id}")
    public String deleteEvent(@PathVariable String id) throws ExecutionException, InterruptedException {
        return eventService.deleteEvent(id);
    }
    @PutMapping("/update/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String eventId,
            @RequestBody Event updatedEvent
    ) {
        try {
            Event event = eventService.updateEvent(eventId, updatedEvent);
            return ResponseEntity.ok(event);  // Returning the updated event with department info
        } catch (RuntimeException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to update event: " + e.getMessage());
        }
    }

    @GetMapping("/getJoinUrl/{eventId}")
    public String getEventJoinUrl(@PathVariable String eventId) {
        // Generate the event join URL based on eventId
        return "https://yourapi.com/join/" + eventId;
    }

    @GetMapping("/getPaginated")
    public ResponseEntity<Map<String, Object>> getPaginatedEvents(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            List<Eventdto> events = eventService.getPaginatedEvents(page, size);
            int totalEvents = eventService.getTotalEventCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", events);
            response.put("totalElements", totalEvents);
            response.put("totalPages", (int) Math.ceil((double) totalEvents / size));
            response.put("currentPage", page);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error fetching paginated events: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/updateStatus/{eventId}")
    public ResponseEntity<?> updateEventStatus(
            @PathVariable String eventId,
            @RequestBody Map<String, String> statusUpdate
    ) {
        try {
            String status = statusUpdate.get("status");
            if (status == null) {
                return ResponseEntity.status(400).body("Status is required");
            }
            
            String result = eventService.updateEventStatus(eventId, status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update event status: " + e.getMessage());
        }
    }
}
