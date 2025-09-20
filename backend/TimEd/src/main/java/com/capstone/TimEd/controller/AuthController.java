package com.capstone.TimEd.controller;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.capstone.TimEd.dto.AuthResponse;
import com.capstone.TimEd.dto.LoginRequest;
import com.capstone.TimEd.dto.PasswordSyncRequest;
import com.capstone.TimEd.dto.RegisterRequest;
import com.capstone.TimEd.dto.UpdatePasswordRequest;
import com.capstone.TimEd.model.User;
import com.capstone.TimEd.service.AuthService;
import com.capstone.TimEd.service.UserService;
import com.capstone.TimEd.service.OtpService;
import com.capstone.TimEd.dto.OtpRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	   @Autowired
	    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private OtpService otpService;
    @GetMapping("/getSample")
    public String getSample() {
        return "Samplsse";
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // Adjust the register request handling if needed
        AuthResponse response = authService.register(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // Adjust the login request handling
        AuthResponse response = authService.login(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyToken(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        boolean isValid = authService.verifyToken(token);
        
        if (isValid) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.status(401).body(false);
        }
    }
    
    @PostMapping("/sync-password")
    public ResponseEntity<String> syncPassword(@RequestBody PasswordSyncRequest request)
            throws InterruptedException, ExecutionException {

        // Get the user using their Firebase UID or schoolId
        User user = userService.getUserBySchoolId(request.getSchoolId());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // Hash the new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        // Update password in Firestore
        user.setPassword(hashedPassword);
        userService.updateUser(user.getUserId(), user);

        return ResponseEntity.ok("Password synced to Firestore.");
    }
    @GetMapping("/email-by-schoolId")
    public ResponseEntity<String> getEmailBySchoolId(@RequestParam String schoolId) {
        try {
            User user = userService.getUserBySchoolId(schoolId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No user with given schoolId");
            }
            return ResponseEntity.ok(user.getEmail());
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch email");
        }
    }
    
    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdatePasswordRequest request) {
        try {
            // Get the user by School ID
            User user = userService.getUserBySchoolId(request.getSchoolId());
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Hash the new password using bcrypt
            String hashedPassword = passwordEncoder.encode(request.getNewPassword());
            
            // Set the hashed password to the user
            user.setPassword(hashedPassword);

            // Update the user in Firestore using the identifier (schoolId)
            userService.updateUser(request.getSchoolId(), user);

            return ResponseEntity.ok("Password updated successfully in Firestore");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating password");
        }
    }
    
    @PostMapping("/login-by-schoolId")
    public ResponseEntity<AuthResponse> loginBySchoolId(@RequestBody LoginRequest request) {
        // Call the service layer method that handles the logic
        AuthResponse authResponse = authService.loginBySchoolId(request);

        if (authResponse.getToken() != null) {
            return ResponseEntity.ok(authResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authResponse);
        }
    }

    @PostMapping("/generate-otp")
    public ResponseEntity<?> generateOtp(@RequestBody OtpRequest request) {
        try {
            System.out.println("=== OTP GENERATION START ===");
            System.out.println("School ID: " + request.getSchoolId());
            
            // Check if user is admin
            System.out.println("Fetching user from Firestore...");
            User user = userService.getUserBySchoolId(request.getSchoolId());
            
            if (user == null) {
                System.err.println("ERROR: User not found with school ID: " + request.getSchoolId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with the provided school ID");
            }
            
            System.out.println("User found - Email: " + user.getEmail() + ", Role: " + user.getRole());
            
            if (!"ADMIN".equals(user.getRole())) {
                System.err.println("ERROR: Non-admin user attempted OTP generation");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("OTP generation is only available for admin users");
            }

            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                System.err.println("ERROR: User has no email address");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User email address is not available");
            }

            System.out.println("Attempting to generate OTP for: " + user.getEmail());
            otpService.generateOtp(request.getSchoolId());
            System.out.println("=== OTP GENERATION SUCCESS ===");
            return ResponseEntity.ok("OTP sent successfully");
            
        } catch (Exception e) {
            System.err.println("=== OTP GENERATION FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate OTP: " + e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest request) {
        try {
            boolean isValid = otpService.verifyOtp(request.getSchoolId(), request.getOtp());
            if (isValid) {
                return ResponseEntity.ok("OTP verified successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to verify OTP: " + e.getMessage());
        }
    }

}
