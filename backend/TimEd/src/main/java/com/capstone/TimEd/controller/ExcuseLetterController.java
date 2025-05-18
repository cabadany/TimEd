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
            // Validate required fields
            if (excuseLetter.getUserId() == null || excuseLetter.getUserId().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Validate ID Number
            if (excuseLetter.getIdNumber() == null || 
                excuseLetter.getIdNumber().trim().isEmpty() || 
                excuseLetter.getIdNumber().trim().equalsIgnoreCase("N/A")) {
                response.put("success", false);
                response.put("message", "Valid ID Number is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Set default values for optional fields
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
            List<ExcuseLetterDto> excuseLetters = excuseLetterService.getPaginatedExcuseLetters(page, size, status, startDate, endDate);
            
            response.put("content", excuseLetters);
            response.put("totalElements", excuseLetters.size());
            response.put("totalPages", (int) Math.ceil((double) excuseLetters.size() / size));
            response.put("currentPage", page);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error fetching excuse letters: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{userId}/{letterId}")
    public ResponseEntity<?> getExcuseLetterById(
            @PathVariable String userId,
            @PathVariable String letterId) {
        try {
            ExcuseLetter excuseLetter = excuseLetterService.getExcuseLetterById(userId, letterId);
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

    @PutMapping("/{userId}/{letterId}/status")
    public ResponseEntity<?> updateExcuseLetterStatus(
            @PathVariable String userId,
            @PathVariable String letterId,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason) {
        try {
            // Validate inputs
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "User ID cannot be empty"));
            }
            
            if (letterId == null || letterId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Letter ID cannot be empty"));
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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Rejection reason is required when status is Rejected"));
            }
            
            // Update the status
            ExcuseLetter updatedLetter = excuseLetterService.updateExcuseLetterStatus(userId, letterId, status, rejectionReason);
            
            if (updatedLetter == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Failed to update excuse letter - null result returned"));
            }
            
            // Return the updated letter
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Excuse letter status updated successfully");
            response.put("letter", updatedLetter);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating excuse letter status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/{letterId}")
    public ResponseEntity<Map<String, Object>> deleteExcuseLetter(
            @PathVariable String userId,
            @PathVariable String letterId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = excuseLetterService.deleteExcuseLetter(userId, letterId);
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