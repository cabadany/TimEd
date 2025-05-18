package com.capstone.TimEd.controller;

import com.capstone.TimEd.dto.ExcuseLetterDto;
import com.capstone.TimEd.model.ExcuseLetter;
import com.capstone.TimEd.service.ExcuseLetterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/excuse-letters")
public class ExcuseLetterController {

    @Autowired
    private ExcuseLetterService excuseLetterService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createExcuseLetter(@RequestBody ExcuseLetter excuseLetter) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Set required fields if not present in the request
            if (excuseLetter.getAttachmentUrl() == null) {
                excuseLetter.setAttachmentUrl("");
            }
            
            if (excuseLetter.getDepartment() == null) {
                excuseLetter.setDepartment("N/A");
            }
            
            String result = excuseLetterService.addExcuseLetter(excuseLetter);
            
            if (result.startsWith("Failed")) {
                response.put("success", false);
                response.put("message", result);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            response.put("success", true);
            response.put("id", result);
            response.put("message", "Excuse letter created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating excuse letter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<Map<String, Object>> getAllExcuseLetters(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Fetching excuse letters with parameters:");
            System.out.println("Page: " + page);
            System.out.println("Size: " + size);
            System.out.println("Status: " + status);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            
            List<ExcuseLetterDto> excuseLetters = excuseLetterService.getPaginatedExcuseLetters(page, size, status, startDate, endDate);
            int totalCount = excuseLetterService.getTotalExcuseLetterCount(status);
            
            System.out.println("Found " + excuseLetters.size() + " letters out of total " + totalCount);
            
            response.put("content", excuseLetters);
            response.put("totalElements", totalCount);
            response.put("totalPages", (int) Math.ceil((double) totalCount / size));
            response.put("currentPage", page);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error fetching excuse letters: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExcuseLetterById(@PathVariable String id) {
        try {
            ExcuseLetter excuseLetter = excuseLetterService.getExcuseLetterById(id);
            if (excuseLetter != null) {
                return ResponseEntity.ok(excuseLetter);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Excuse letter not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving excuse letter: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateExcuseLetterStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason) {
        try {
            // Log the request details for troubleshooting
            System.out.println("Updating excuse letter status - ID: " + id + ", New Status: " + status);
            if (rejectionReason != null && !rejectionReason.isEmpty()) {
                System.out.println("Rejection reason provided: " + rejectionReason);
            }
            
            // Validate inputs
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Excuse letter ID cannot be empty"));
            }
            
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Status cannot be empty"));
            }
            
            // Validate status value
            if (!status.equals("Pending") && !status.equals("Approved") && !status.equals("Rejected")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid status. Status must be Pending, Approved, or Rejected."));
            }
            
            // Check if rejection reason is provided for rejected status
            if ("Rejected".equals(status) && (rejectionReason == null || rejectionReason.trim().isEmpty())) {
                System.out.println("Warning: Rejection reason not provided for rejected letter");
            }
            
            // First verify the excuse letter exists
            ExcuseLetter existingLetter = excuseLetterService.getExcuseLetterById(id);
            if (existingLetter == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Excuse letter with ID " + id + " not found"));
            }
            
            // Try to update
            ExcuseLetter updatedLetter = excuseLetterService.updateExcuseLetterStatus(id, status, rejectionReason);
            
            if (updatedLetter == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to update excuse letter - null result returned"));
            }
            
            // Check if status was actually updated
            if (!updatedLetter.getStatus().equals(status)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                            "message", "Status update may have failed - expected: " + status + ", actual: " + updatedLetter.getStatus(),
                            "letter", updatedLetter
                        ));
            }
            
            // Return the updated letter
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Excuse letter status updated successfully");
            response.put("letter", updatedLetter);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // For validation errors
            System.err.println("Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Log the full stack trace for server-side debugging
            System.err.println("Error updating excuse letter status: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating excuse letter status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteExcuseLetter(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = excuseLetterService.deleteExcuseLetter(id);
            if (result.contains("successfully")) {
                response.put("success", true);
                response.put("message", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting excuse letter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getExcuseLettersByUserId(@PathVariable String userId) {
        try {
            List<ExcuseLetterDto> userLetters = excuseLetterService.getExcuseLettersByUserId(userId);
            return ResponseEntity.ok(userLetters);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error retrieving user's excuse letters: " + e.getMessage()));
        }
    }
} 