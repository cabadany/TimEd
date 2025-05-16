package com.capstone.TimEd.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.capstone.TimEd.model.Certificate;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.service.CertificateService;
import com.capstone.TimEd.service.EventService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.Base64;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

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

    @PostMapping("/{eventId}/images")
    public ResponseEntity<?> uploadCertificateImages(
            @PathVariable String eventId,
            @RequestParam(required = false) MultipartFile background,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile watermark,
            @RequestParam(required = false) Map<String, MultipartFile> signatures) {
        try {
            System.out.println("Processing image upload for eventId: " + eventId);
            
            Certificate certificate = certificateService.getCertificateByEventId(eventId);
            if (certificate == null) {
                System.out.println("No existing certificate found, creating new one");
                certificate = new Certificate();
                certificate.setEventId(eventId);
                certificate.setSignatureImages(new HashMap<>()); // Initialize the map
            } else {
                System.out.println("Found existing certificate with ID: " + certificate.getId());
                if (certificate.getSignatureImages() == null) {
                    certificate.setSignatureImages(new HashMap<>()); // Initialize if null
                }
            }

            // Process background image
            if (background != null && !background.isEmpty()) {
                try {
                    System.out.println("[DEBUG] uploadCertificateImages: Processing background image for eventId: " + eventId);
                    byte[] backgroundBytes = background.getBytes();
                    System.out.println("[DEBUG] uploadCertificateImages: Original background image size: " + backgroundBytes.length + " bytes");
                    System.out.println("[DEBUG] uploadCertificateImages: Content type: " + background.getContentType());
                    
                    // Process image to ensure it's compatible with PDF generation
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(backgroundBytes);
                         ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        
                        // Read the image 
                        BufferedImage originalImage = ImageIO.read(bis);
                        if (originalImage != null) {
                            System.out.println("[DEBUG] uploadCertificateImages: Successfully read image: " + 
                                            originalImage.getWidth() + "x" + originalImage.getHeight() + 
                                            ", type: " + originalImage.getType());
                            
                            // Convert to a compatible format
                            BufferedImage convertedImg = new BufferedImage(
                                originalImage.getWidth(),
                                originalImage.getHeight(),
                                BufferedImage.TYPE_INT_ARGB
                            );
                            
                            Graphics2D g2d = convertedImg.createGraphics();
                            // Use white background for images without alpha to prevent black background issues
                            g2d.setColor(java.awt.Color.WHITE);
                            g2d.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
                            g2d.drawImage(originalImage, 0, 0, null);
                            g2d.dispose();
                            
                            System.out.println("[DEBUG] uploadCertificateImages: Converted to ARGB format");
                            
                            // Write as PNG for better transparency support
                            boolean success = ImageIO.write(convertedImg, "PNG", bos);
                            if (success) {
                                backgroundBytes = bos.toByteArray();
                                System.out.println("[DEBUG] uploadCertificateImages: Successfully converted to PNG, new size: " + backgroundBytes.length + " bytes");
                            } else {
                                System.err.println("[DEBUG] uploadCertificateImages: No image writer found for PNG format");
                            }
                        } else {
                            System.err.println("[DEBUG] uploadCertificateImages: Failed to read image with ImageIO. Will use original bytes.");
                            // Try to determine format based on first bytes
                            printImageFormat(backgroundBytes);
                        }
                    } catch (Exception e) {
                        System.err.println("[DEBUG] uploadCertificateImages: Error processing image format: " + e.getMessage());
                        e.printStackTrace();
                        // Continue with original bytes if processing fails
                    }
                    
                    // Base64 encode the image
                    String base64Background = Base64.getEncoder().encodeToString(backgroundBytes);
                    System.out.println("[DEBUG] uploadCertificateImages: Base64 encoded string length: " + base64Background.length());
                    System.out.println("[DEBUG] uploadCertificateImages: Base64 string starts with: " + 
                                    (base64Background.length() > 20 ? base64Background.substring(0, 20) + "..." : base64Background));
                    
                    certificate.setBackgroundImage(base64Background);
                    System.out.println("[DEBUG] uploadCertificateImages: Background image processed and stored in certificate object");
                } catch (Exception e) {
                    System.err.println("[DEBUG] uploadCertificateImages: Error processing background image: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Process logo image
            if (logo != null && !logo.isEmpty()) {
                try {
                    byte[] logoBytes = logo.getBytes();
                    String base64Logo = Base64.getEncoder().encodeToString(logoBytes);
                    certificate.setLogoImage(base64Logo);
                    System.out.println("Logo image processed, size: " + logoBytes.length + " bytes");
                } catch (Exception e) {
                    System.err.println("Error processing logo image: " + e.getMessage());
                }
            }

            // Process watermark image
            if (watermark != null && !watermark.isEmpty()) {
                try {
                    byte[] watermarkBytes = watermark.getBytes();
                    String base64Watermark = Base64.getEncoder().encodeToString(watermarkBytes);
                    certificate.setWatermarkImage(base64Watermark);
                    System.out.println("Watermark image processed, size: " + watermarkBytes.length + " bytes");
                } catch (Exception e) {
                    System.err.println("Error processing watermark image: " + e.getMessage());
                }
            }

            // Process signature images
            if (signatures != null && !signatures.isEmpty()) {
                Map<String, String> signatureImages = certificate.getSignatureImages();
                for (Map.Entry<String, MultipartFile> entry : signatures.entrySet()) {
                    try {
                        if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                            byte[] signatureBytes = entry.getValue().getBytes();
                            String base64Signature = Base64.getEncoder().encodeToString(signatureBytes);
                            signatureImages.put(entry.getKey(), base64Signature);
                            System.out.println("Signature image processed for: " + entry.getKey() + 
                                            ", size: " + signatureBytes.length + " bytes");
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing signature image for " + entry.getKey() + 
                                         ": " + e.getMessage());
                    }
                }
            }

            // Save the certificate using saveCertificate
            System.out.println("[DEBUG] uploadCertificateImages: Saving certificate with images...");
            Certificate savedCertificate = certificateService.saveCertificate(certificate);
            System.out.println("[DEBUG] uploadCertificateImages: Certificate saved successfully with ID: " + savedCertificate.getId());
            
            // Verify the images were saved
            Certificate verifiedCert = certificateService.getCertificate(savedCertificate.getId());
            System.out.println("[DEBUG] uploadCertificateImages: Verification - Background image present: " + 
                             (verifiedCert.getBackgroundImage() != null));
            if (verifiedCert.getBackgroundImage() != null) {
                System.out.println("[DEBUG] uploadCertificateImages: Verification - Background image length: " + 
                                verifiedCert.getBackgroundImage().length());
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error uploading images: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to upload images: " + e.getMessage());
        }
    }

    @DeleteMapping("/{eventId}/images")
    public ResponseEntity<?> deleteCertificateImages(
            @PathVariable String eventId,
            @RequestParam(required = false) boolean background,
            @RequestParam(required = false) boolean logo,
            @RequestParam(required = false) boolean watermark,
            @RequestParam(required = false) String signatory) {
        try {
            Certificate certificate = certificateService.getCertificateByEventId(eventId);
            if (certificate == null) {
                return ResponseEntity.notFound().build();
            }

            if (background) {
                certificate.setBackgroundImage(null);
            }
            if (logo) {
                certificate.setLogoImage(null);
            }
            if (watermark) {
                certificate.setWatermarkImage(null);
            }
            if (signatory != null) {
                Map<String, String> signatureImages = certificate.getSignatureImages();
                if (signatureImages != null) {
                    signatureImages.remove(signatory);
                }
            }

            certificateService.createCertificate(certificate);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete images: " + e.getMessage());
        }
    }

    // Helper method to try to identify image format from bytes
    private void printImageFormat(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 12) {
            System.out.println("[DEBUG] Image too small to identify format");
            return;
        }
        
        // Convert first bytes to hex for inspection
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(12, imageBytes.length); i++) {
            sb.append(String.format("%02X ", imageBytes[i] & 0xFF));
        }
        String hexHeader = sb.toString();
        System.out.println("[DEBUG] Image header bytes: " + hexHeader);
        
        // Try to identify common formats
        if (imageBytes[0] == (byte)0xFF && imageBytes[1] == (byte)0xD8) {
            System.out.println("[DEBUG] Detected JPEG format");
        } else if (imageBytes[0] == (byte)0x89 && imageBytes[1] == (byte)0x50 && 
                  imageBytes[2] == (byte)0x4E && imageBytes[3] == (byte)0x47) {
            System.out.println("[DEBUG] Detected PNG format");
        } else if (imageBytes[0] == (byte)0x47 && imageBytes[1] == (byte)0x49 && 
                  imageBytes[2] == (byte)0x46) {
            System.out.println("[DEBUG] Detected GIF format");
        } else if (imageBytes[0] == (byte)0x42 && imageBytes[1] == (byte)0x4D) {
            System.out.println("[DEBUG] Detected BMP format");
        } else {
            System.out.println("[DEBUG] Unknown image format");
        }
    }
} 