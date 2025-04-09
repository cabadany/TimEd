package com.capstone.TimEd.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.dto.AuthResponse;
import com.capstone.TimEd.dto.LoginRequest;
import com.capstone.TimEd.dto.RegisterRequest;
import com.capstone.TimEd.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

@Service
public class AuthService {

    @Autowired
    private FirebaseAuth firebaseAuth;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        try {
            // Using schoolId as a custom identifier, create user in Firestore
            String customUid = request.getSchoolId();  // Using schoolId as UID

            // Create user in Firebase Auth with a custom UID
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setUid(customUid) // your custom school ID
                    .setEmail(request.getSchoolId() + "@dummy.email")
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getFirstName() + " " + request.getLastName())
                    .setEmailVerified(false);

            UserRecord userRecord = firebaseAuth.createUser(createRequest);
            
            // Create custom claims for role
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", request.getRole() != null ? request.getRole() : "USER");
            firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);
            
            // Create a custom token for the user
            String token = firebaseAuth.createCustomToken(userRecord.getUid(), claims);
            
            // Store additional user data in Firestore
            User user = new User();
            user.setUserId(userRecord.getUid());
            user.setSchoolId(request.getSchoolId());
            user.setFirstName(request.getFirstName());  // Ensure first name is set
            user.setLastName(request.getLastName());    // Ensure last name is set
            user.setRole(request.getRole() != null ? request.getRole() : "USER");  // Ensure role is set
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Ensure password is set

            userService.createUser(user);
            
            // Return the response
            return new AuthResponse(token, userRecord.getUid(), userRecord.getUid(), user.getRole());
            
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            return new AuthResponse("Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Get user by schoolId (custom UID)
            User user = userService.getUserBySchoolId(request.getSchoolId()); // Corrected method name
            if (user == null) {
                return new AuthResponse("User not found with schoolId: " + request.getSchoolId());
            }
            
            // Verify password using BCrypt
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return new AuthResponse("Invalid password");
            }
            
            // Try to get the user from Firebase Auth by custom UID
            UserRecord userRecord = firebaseAuth.getUser(request.getSchoolId());
            
            // Create custom claims based on user role
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());
            
            // Create a custom token
            String token = firebaseAuth.createCustomToken(userRecord.getUid(), claims);
            
            return new AuthResponse(token, user.getUserId(), user.getSchoolId(), user.getRole());
            
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            return new AuthResponse("Login failed: " + e.getMessage());
        }
    }

    public boolean verifyToken(String idToken) {
        try {
            firebaseAuth.verifyIdToken(idToken);
            return true;
        } catch (FirebaseAuthException e) {
            return false;
        }
    }
}
