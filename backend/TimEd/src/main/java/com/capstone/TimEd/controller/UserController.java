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


    @PutMapping("/updateUser/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable String userId, @RequestBody User user) {
        try {
            // Just forward entire User object to service — it includes department if present
            userService.updateUser(userId, user);
            return ResponseEntity.ok("User updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating user: " + e.getMessage());
        }
    }

    // DELETE - delete user
    @DeleteMapping("/deleteUser/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable String userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting user: " + e.getMessage());
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
                return ResponseEntity.badRequest().body("Profile picture URL is required");
            }
            
            // Get reference to user document
            DocumentReference userRef = firestore.collection("users").document(userId);
            ApiFuture<DocumentSnapshot> future = userRef.get();
            DocumentSnapshot document = future.get();
            
            if (!document.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            
            // Update only the profile picture URL field
            userRef.update("profilePictureUrl", profilePictureUrl);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile picture updated successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating profile picture: " + e.getMessage());
        }
    }
}
