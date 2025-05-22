package com.capstone.TimEd.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.dto.AuthResponse;
import com.capstone.TimEd.dto.LoginRequest;
import com.capstone.TimEd.dto.RegisterRequest;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

@Service
public class AuthService {
	private DepartmentService departmentService;
    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired  // This annotation is optional if you're using constructor injection
    public AuthService(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }
    

    public AuthResponse register(RegisterRequest request) {
        try {
            // === Validate Required Fields ===
            if (isNullOrBlank(request.getSchoolId()) || isNullOrBlank(request.getPassword()) ||
                isNullOrBlank(request.getFirstName()) || isNullOrBlank(request.getLastName())) {
                return new AuthResponse("Missing required fields");
            }

            // === Check for Existing User ===
            User existing = userService.getUserBySchoolId(request.getSchoolId());
            if (existing != null) {
                return new AuthResponse("User already exists with School ID: " + request.getSchoolId());
            }

            String email = (request.getEmail() != null && !request.getEmail().isBlank())
                    ? request.getEmail()
                    : request.getSchoolId() + "@dummy.email";

            // === Firebase User Creation ===
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getFirstName() + " " + request.getLastName())
                    .setEmailVerified(false);

            UserRecord userRecord = firebaseAuth.createUser(createRequest);

            // === Set Custom Claims ===
            Map<String, Object> claims = new HashMap<>();
            String role = request.getRole() != null && !request.getRole().isBlank() ? request.getRole() : "USER";
            claims.put("role", role);
            firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);

            String token = firebaseAuth.createCustomToken(userRecord.getUid(), claims);

            // === Handle Department ===
            Department department = null;
            if (request.getDepartmentId() != null && !request.getDepartmentId().isBlank()) {
                department = departmentService.getDepartment(request.getDepartmentId());  // Use the service to fetch the department by ID
                if (department == null) {
                    return new AuthResponse("Department not found with ID: " + request.getDepartmentId());
                }
            }

            // === Save User in Firestore ===
            User user = new User();
            user.setUserId(userRecord.getUid());
            user.setSchoolId(request.getSchoolId());
            user.setEmail(email);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash the password
            user.setDepartment(department); // Set the department

            userService.createUser(user); // This method saves the user in Firestore

            return new AuthResponse(token, userRecord.getUid(), request.getSchoolId(), role);

        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            return new AuthResponse("Registration failed: " + e.getMessage());
        }
    }

  
    public AuthResponse login(LoginRequest request) {
        try {
            User user = userService.getUserBySchoolId(request.getSchoolId());
            if (user == null) {
                return new AuthResponse("User not found with schoolId: " + request.getSchoolId());
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return new AuthResponse("Invalid password");
            }

            UserRecord userRecord = firebaseAuth.getUser(user.getUserId()); // use actual UID now

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());

            String token = firebaseAuth.createCustomToken(userRecord.getUid(), claims);

            return new AuthResponse(token, user.getUserId(), user.getSchoolId(), user.getRole());

        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            return new AuthResponse("Login failed: " + e.getMessage());
        }
    }
    public AuthResponse loginBySchoolId(LoginRequest request) {
        try {
            // Step 1: Get user by schoolId
            User user = userService.getUserBySchoolId(request.getSchoolId());
            if (user == null) {
                return new AuthResponse("User not found with schoolId: " + request.getSchoolId());
            }

            // Step 2: Use the Firebase Admin SDK to get user data based on userId
            UserRecord userRecord = firebaseAuth.getUser(user.getUserId());

            // Step 3: Prepare additional claims if needed
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", user.getRole());

            // Step 4: Generate Firebase Custom Token
            String token = firebaseAuth.createCustomToken(userRecord.getUid(), claims);

            // Step 5: Return the AuthResponse with token, user info, etc.
            return new AuthResponse(token, user.getUserId(), user.getSchoolId(), user.getRole());

        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            // If anything goes wrong, catch the exception and return a failure message
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

    private boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
