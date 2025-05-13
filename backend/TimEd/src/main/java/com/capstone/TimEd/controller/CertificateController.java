package com.capstone.TimEd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.capstone.TimEd.model.Certificate;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.service.CertificateService;
import com.capstone.TimEd.service.EventService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    @Autowired
    private CertificateService certificateService;
    
    @Autowired
    private EventService eventService;

    /**
     * Create a new certificate template
     */
    @PostMapping
    public ResponseEntity<?> createCertificate(@RequestBody Certificate certificate) {
        try {
            // Clean up eventId if it has the legacy format
            if (certificate.getEventId() != null && certificate.getEventId().startsWith("Event added successfully with ID: ")) {
                String actualEventId = certificate.getEventId().replace("Event added successfully with ID: ", "");
                System.out.println("Converting legacy eventId format in create request to: " + actualEventId);
                certificate.setEventId(actualEventId);
            }
            
            Certificate createdCertificate = certificateService.createCertificate(certificate);
            
            // If the certificate has an eventId, update the event with this certificate's ID
            if (createdCertificate.getEventId() != null && !createdCertificate.getEventId().isEmpty()) {
                try {
                    Event event = eventService.getEventById(createdCertificate.getEventId());
                    if (event != null) {
                        event.setCertificateId(createdCertificate.getId());
                        eventService.updateEventCertificateId(event.getEventId(), createdCertificate.getId());
                        System.out.println("Updated event " + event.getEventId() + " with certificateId " + createdCertificate.getId());
                    }
                } catch (Exception e) {
                    // Log error but don't fail the certificate creation
                    System.err.println("Error linking certificate to event: " + e.getMessage());
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCertificate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating certificate: " + e.getMessage());
        }
    }

    /**
     * Get a certificate by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCertificate(@PathVariable String id) {
        try {
            Certificate certificate = certificateService.getCertificate(id);
            if (certificate != null) {
                return ResponseEntity.ok(certificate);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Certificate not found with ID: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificate: " + e.getMessage());
        }
    }

    /**
     * Update an existing certificate
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCertificate(@PathVariable String id, @RequestBody Certificate certificate) {
        try {
            // Clean up eventId if it has the legacy format
            if (certificate.getEventId() != null && certificate.getEventId().startsWith("Event added successfully with ID: ")) {
                String actualEventId = certificate.getEventId().replace("Event added successfully with ID: ", "");
                System.out.println("Converting legacy eventId format in update request to: " + actualEventId);
                certificate.setEventId(actualEventId);
            }
            
            Certificate updatedCertificate = certificateService.updateCertificate(id, certificate);
            
            // If the certificate has an eventId, update the event with this certificate's ID
            if (updatedCertificate.getEventId() != null && !updatedCertificate.getEventId().isEmpty()) {
                try {
                    Event event = eventService.getEventById(updatedCertificate.getEventId());
                    if (event != null) {
                        event.setCertificateId(updatedCertificate.getId());
                        eventService.updateEventCertificateId(event.getEventId(), updatedCertificate.getId());
                        System.out.println("Updated event " + event.getEventId() + " with certificateId " + updatedCertificate.getId());
                    }
                } catch (Exception e) {
                    // Log error but don't fail the certificate update
                    System.err.println("Error linking certificate to event: " + e.getMessage());
                }
            }
            
            return ResponseEntity.ok(updatedCertificate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating certificate: " + e.getMessage());
        }
    }

    /**
     * Delete a certificate
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCertificate(@PathVariable String id) {
        try {
            String result = certificateService.deleteCertificate(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting certificate: " + e.getMessage());
        }
    }

    /**
     * Get all certificates
     */
    @GetMapping
    public ResponseEntity<?> getAllCertificates() {
        try {
            List<Certificate> certificates = certificateService.getAllCertificates();
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificates: " + e.getMessage());
        }
    }

    /**
     * Get certificate by event ID
     */
    @GetMapping("/getByEventId/{eventId}")
    public ResponseEntity<?> getCertificateByEventId(@PathVariable String eventId) {
        try {
            Certificate certificate = certificateService.getCertificateByEventId(eventId);
            if (certificate != null) {
                return ResponseEntity.ok(certificate);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No certificate found for event ID: " + eventId);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving certificate: " + e.getMessage());
        }
    }

    /**
     * Send certificates to event attendees
     */
    @PostMapping("/sendCertificates")
    public ResponseEntity<?> sendCertificates(@RequestBody Map<String, String> requestBody) {
        try {
            String certificateId = requestBody.get("certificateId");
            String eventId = requestBody.get("eventId");
            
            if (certificateId == null || eventId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Certificate ID and Event ID are required");
            }
            
            String result = certificateService.sendCertificates(certificateId, eventId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error sending certificates: " + e.getMessage());
        }
    }
    
    /**
     * Link a certificate to an event
     */
    @PostMapping("/linkToEvent")
    public ResponseEntity<?> linkCertificateToEvent(@RequestBody Map<String, String> requestBody) {
        try {
            String certificateId = requestBody.get("certificateId");
            String eventId = requestBody.get("eventId");
            
            if (certificateId == null || eventId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Certificate ID and Event ID are required");
            }
            
            // Extract the actual event ID if it contains the legacy format
            if (eventId.startsWith("Event added successfully with ID: ")) {
                String actualEventId = eventId.replace("Event added successfully with ID: ", "");
                System.out.println("Converting legacy eventId format to: " + actualEventId);
                eventId = actualEventId;
            }
            
            // Update the event's certificateId field
            try {
                eventService.updateEventCertificateId(eventId, certificateId);
                return ResponseEntity.ok("Certificate successfully linked to event");
            } catch (Exception e) {
                System.err.println("Error when linking certificate to event: " + e.getMessage());
                // Check if certificate exists
                Certificate certificate = certificateService.getCertificate(certificateId);
                if (certificate != null) {
                    // Update certificate with correct eventId
                    certificate.setEventId(eventId);
                    certificateService.updateCertificate(certificateId, certificate);
                    return ResponseEntity.ok("Certificate updated with correct eventId, but event could not be updated");
                }
                throw e;
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error linking certificate to event: " + e.getMessage());
        }
    }
} 