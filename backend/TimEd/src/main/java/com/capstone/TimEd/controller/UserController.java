package com.capstone.TimEd.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.TimEd.dto.AuthResponse;
import com.capstone.TimEd.dto.LoginRequest;
import com.capstone.TimEd.dto.RegisterRequest;
import com.capstone.TimEd.model.User;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.service.AuthService;
import com.capstone.TimEd.service.UserService;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private Firestore firestore;

    // GET all users
    @GetMapping("/getAll")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to retrieve users: " + e.getMessage());
        }
    }

    @GetMapping("/getUser/{userId}")
    public ResponseEntity<?> getUserByUserId(@PathVariable String userId) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + userId);
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user: " + e.getMessage());
        }
    }

    // Add endpoint to get user by schoolId
    @GetMapping("/getBySchoolId/{schoolId}")
    public ResponseEntity<?> getUserBySchoolId(@PathVariable String schoolId) {
        try {
            User user = userService.getUserBySchoolId(schoolId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with School ID: " + schoolId);
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user: " + e.getMessage());
        }
    }

    // Modify the updateUser endpoint to handle profile picture updates
    @PutMapping("/updateUser/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody User user) {
        try {
            // Get existing user to preserve profile picture if not being updated
            User existingUser = userService.getUserById(userId);
            if (existingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }

            // If profile picture URL is not provided in update, keep existing one
            if (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().trim().isEmpty()) {
                user.setProfilePictureUrl(existingUser.getProfilePictureUrl());
            }

            userService.updateUser(userId, user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error updating user: " + e.getMessage()
            ));
        }
    }

    // DELETE - delete user
    @DeleteMapping("/deleteUser/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("✅ User deleted successfully from Firestore and Firebase Auth.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("🔥 Error deleting user: " + e.getMessage());
        }
    }

    // Add a new endpoint to update profile picture URL
    @PutMapping("/updateProfilePicture/{userId}")
    public ResponseEntity<?> updateProfilePicture(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        try {
            String profilePictureUrl = request.get("profilePictureUrl");
            
            if (profilePictureUrl == null || profilePictureUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Profile picture URL is required"
                ));
            }
            
            // Get the user
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "User not found"
                ));
            }
            
            // Update the profile picture URL
            user.setProfilePictureUrl(profilePictureUrl);
            userService.updateUser(userId, user);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Profile picture updated successfully",
                "profilePictureUrl", profilePictureUrl
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Error updating profile picture: " + e.getMessage()
            ));
        }
    }

    // ADMIN ENDPOINT: Trigger data consistency fix
    @PostMapping("/admin/fix-data-consistency")
    public ResponseEntity<?> fixDataConsistency() {
        try {
            // This will be automatically handled by the modified getAllUsers method
            // Just call getAllUsers to trigger the fix
            List<User> users = userService.getAllUsers();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Data consistency check and fix completed",
                "totalUsers", users.size(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to fix data consistency: " + e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }
}
