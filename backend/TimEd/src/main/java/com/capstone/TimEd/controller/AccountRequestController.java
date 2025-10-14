package com.capstone.TimEd.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.TimEd.dto.AccountRequestDto;
import com.capstone.TimEd.dto.ReviewRequestDto;
import com.capstone.TimEd.model.AccountRequest;
import com.capstone.TimEd.service.AccountRequestService;

@RestController
@RequestMapping("/api/account-requests")
public class AccountRequestController {

    @Autowired
    private AccountRequestService accountRequestService;

    // Create a new account request (for mobile app users)
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAccountRequest(@RequestBody AccountRequestDto requestDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate required fields
            if (requestDto.getFirstName() == null || requestDto.getFirstName().trim().isEmpty() ||
                requestDto.getLastName() == null || requestDto.getLastName().trim().isEmpty() ||
                requestDto.getEmail() == null || requestDto.getEmail().trim().isEmpty() ||
                requestDto.getSchoolId() == null || requestDto.getSchoolId().trim().isEmpty() ||
                requestDto.getDepartment() == null || requestDto.getDepartment().trim().isEmpty() ||
                requestDto.getPassword() == null || requestDto.getPassword().trim().isEmpty()) {
                
                response.put("success", false);
                response.put("message", "All fields are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate email format
            if (!isValidEmail(requestDto.getEmail())) {
                response.put("success", false);
                response.put("message", "Invalid email format");
                return ResponseEntity.badRequest().body(response);
            }
            
            String requestId = accountRequestService.createAccountRequest(requestDto);
            
            response.put("success", true);
            response.put("message", "Account request created successfully");
            response.put("requestId", requestId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create account request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get all account requests (for admin)
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllAccountRequests() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<AccountRequest> requests = accountRequestService.getAllAccountRequests();
            
            response.put("success", true);
            response.put("requests", requests);
            response.put("count", requests.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve account requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get pending account requests only (for admin)
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingAccountRequests() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<AccountRequest> requests = accountRequestService.getPendingAccountRequests();
            
            response.put("success", true);
            response.put("requests", requests);
            response.put("count", requests.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve pending account requests: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Get account request by ID
    @GetMapping("/{requestId}")
    public ResponseEntity<Map<String, Object>> getAccountRequestById(@PathVariable String requestId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            AccountRequest request = accountRequestService.getAccountRequestById(requestId);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Account request not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("request", request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to retrieve account request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Review account request (approve or reject)
    @PutMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewAccountRequest(@RequestBody ReviewRequestDto reviewDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate required fields
            if (reviewDto.getRequestId() == null || reviewDto.getRequestId().trim().isEmpty() ||
                reviewDto.getAction() == null || reviewDto.getAction().trim().isEmpty() ||
                reviewDto.getReviewedBy() == null || reviewDto.getReviewedBy().trim().isEmpty()) {
                
                response.put("success", false);
                response.put("message", "Request ID, action, and reviewer are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate action
            if (!"APPROVE".equals(reviewDto.getAction()) && !"REJECT".equals(reviewDto.getAction())) {
                response.put("success", false);
                response.put("message", "Action must be either 'APPROVE' or 'REJECT'");
                return ResponseEntity.badRequest().body(response);
            }
            
            // If rejecting, rejection reason is required
            if ("REJECT".equals(reviewDto.getAction()) && 
                (reviewDto.getRejectionReason() == null || reviewDto.getRejectionReason().trim().isEmpty())) {
                response.put("success", false);
                response.put("message", "Rejection reason is required when rejecting a request");
                return ResponseEntity.badRequest().body(response);
            }
            
            String result = accountRequestService.reviewAccountRequest(reviewDto);
            
            response.put("success", true);
            response.put("message", result);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to review account request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Send reminder email for pending account request
    @PostMapping("/send-pending-reminder")
    public ResponseEntity<Map<String, Object>> sendPendingReminder(@RequestParam String requestId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String message = accountRequestService.sendPendingReminderEmail(requestId);

            response.put("success", true);
            response.put("message", message);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send reminder email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Helper method to validate email format
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
} 