package com.capstone.TimEd.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.dto.AccountRequestDto;
import com.capstone.TimEd.dto.ReviewRequestDto;
import com.capstone.TimEd.model.AccountRequest;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import java.util.Map;
import java.util.HashMap;

@Service
public class AccountRequestService {

    @Autowired
    private Firestore firestore;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private BrevoEmailService brevoEmailService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    private static final String COLLECTION_NAME = "accountRequests";

    // Create a new account request
    public String createAccountRequest(AccountRequestDto requestDto) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        // Check if user already exists with this schoolId
        if (userAlreadyExists(requestDto.getSchoolId())) {
            throw new RuntimeException("User already exists with School ID: " + requestDto.getSchoolId());
        }

        // Check if there's already a pending request for this schoolId
        if (pendingRequestExists(requestDto.getSchoolId())) {
            throw new RuntimeException("Account request already pending for School ID: " + requestDto.getSchoolId());
        }

        // Create new account request
        DocumentReference docRef = db.collection(COLLECTION_NAME).document();
        String requestId = docRef.getId();

        AccountRequest request = new AccountRequest(
            requestId,
            requestDto.getFirstName(),
            requestDto.getLastName(),
            requestDto.getEmail(),
            requestDto.getSchoolId(),
            requestDto.getDepartment(),
            passwordEncoder.encode(requestDto.getPassword()) // Hash the password
        );

        ApiFuture<WriteResult> result = docRef.set(request);
        result.get(); // Wait for completion

        return requestId;
    }

    // Get all account requests
    public List<AccountRequest> getAllAccountRequests() throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        try {
            // Try with orderBy first
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .orderBy("requestDate", Query.Direction.DESCENDING)
                .get();
            
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<AccountRequest> requests = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                AccountRequest request = doc.toObject(AccountRequest.class);
                requests.add(request);
            }

            return requests;
        } catch (Exception e) {
            // If orderBy fails, fallback to simple query and sort in memory
            System.out.println("OrderBy failed, falling back to simple query: " + e.getMessage());
            
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
            
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<AccountRequest> requests = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                AccountRequest request = doc.toObject(AccountRequest.class);
                requests.add(request);
            }

            // Sort in memory by requestDate (most recent first)
            requests.sort((a, b) -> {
                if (a.getRequestDate() == null && b.getRequestDate() == null) return 0;
                if (a.getRequestDate() == null) return 1;
                if (b.getRequestDate() == null) return -1;
                return b.getRequestDate().compareTo(a.getRequestDate());
            });

            return requests;
        }
    }

    // Get pending account requests only
    public List<AccountRequest> getPendingAccountRequests() throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        try {
            // Try with orderBy first (requires composite index)
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "PENDING")
                .orderBy("requestDate", Query.Direction.DESCENDING)
                .get();
            
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<AccountRequest> requests = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                AccountRequest request = doc.toObject(AccountRequest.class);
                requests.add(request);
            }

            return requests;
        } catch (Exception e) {
            // If composite index doesn't exist, fallback to simple query and sort in memory
            System.out.println("Composite index not available, falling back to simple query: " + e.getMessage());
            
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "PENDING")
                .get();
            
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<AccountRequest> requests = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                AccountRequest request = doc.toObject(AccountRequest.class);
                requests.add(request);
            }

            // Sort in memory by requestDate (most recent first)
            requests.sort((a, b) -> {
                if (a.getRequestDate() == null && b.getRequestDate() == null) return 0;
                if (a.getRequestDate() == null) return 1;
                if (b.getRequestDate() == null) return -1;
                return b.getRequestDate().compareTo(a.getRequestDate());
            });

            return requests;
        }
    }

    // Get account request by ID
    public AccountRequest getAccountRequestById(String requestId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION_NAME).document(requestId).get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(AccountRequest.class);
        }

        return null;
    }

    // Review account request (approve or reject)
    public String reviewAccountRequest(ReviewRequestDto reviewDto) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        // Get the account request
        AccountRequest request = getAccountRequestById(reviewDto.getRequestId());
        if (request == null) {
            throw new RuntimeException("Account request not found");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Account request has already been reviewed");
        }

        // Update request status
        request.setStatus(reviewDto.getAction());
        request.setReviewDate(new Date());
        request.setReviewedBy(reviewDto.getReviewedBy());

        if ("REJECT".equals(reviewDto.getAction())) {
            request.setRejectionReason(reviewDto.getRejectionReason());
        }

        // Save updated request
        ApiFuture<WriteResult> result = db.collection(COLLECTION_NAME)
            .document(reviewDto.getRequestId())
            .set(request);
        result.get();

        // Handle the action
        if ("APPROVE".equals(reviewDto.getAction())) {
            // Create user account
            createUserFromRequest(request);
            // Send approval email
            sendApprovalEmail(request);
            return "Account request approved and user created successfully";
        } else if ("REJECT".equals(reviewDto.getAction())) {
            // Send rejection email
            sendRejectionEmail(request, reviewDto.getRejectionReason());
            return "Account request rejected successfully";
        } else {
            throw new RuntimeException("Invalid action: " + reviewDto.getAction());
        }
    }

    // Send reminder email for pending account request
    public String sendPendingReminderEmail(String requestId) throws InterruptedException, ExecutionException {
        AccountRequest request = getAccountRequestById(requestId);

        if (request == null) {
            throw new RuntimeException("Account request not found");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Reminder emails can only be sent for pending requests");
        }

        try {
            sendPendingReminderEmailInternal(request);
            return "Reminder email sent successfully";
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reminder email: " + e.getMessage());
        }
    }

    // Create user account from approved request
    private void createUserFromRequest(AccountRequest request) throws InterruptedException, ExecutionException {
        // Create user directly through UserService since we already have hashed password
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setSchoolId(request.getSchoolId());
        user.setPassword(request.getPassword()); // Already hashed from account request
        user.setRole("USER"); // Default role for mobile users
        user.setVerified(true); // Account is verified since admin approved the request
        
        // Map department name to department object and ID
        if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
            try {
                Department department = departmentService.getDepartmentByName(request.getDepartment());
                if (department != null) {
                    user.setDepartment(department);
                    user.setDepartmentId(department.getDepartmentId());
                } else {
                    // Log warning if department not found, but don't fail the request
                    System.out.println("Warning: Department '" + request.getDepartment() + "' not found for user " + request.getEmail());
                    user.setDepartmentId(null);
                }
            } catch (Exception e) {
                // Log error but don't fail the request
                System.out.println("Error mapping department for user " + request.getEmail() + ": " + e.getMessage());
                user.setDepartmentId(null);
            }
        } else {
            user.setDepartmentId(null);
        }
        
        // Create Firebase user first
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "USER");
            
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setDisplayName(request.getFirstName() + " " + request.getLastName())
                    .setEmailVerified(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
            FirebaseAuth.getInstance().setCustomUserClaims(userRecord.getUid(), claims);
            
            // Set the Firebase UID
            user.setUserId(userRecord.getUid());
            
            // Save user to Firestore and get the document ID
            String documentId = userService.createUser(user);
            
        } catch (FirebaseAuthException e) {
            throw new RuntimeException("Failed to create Firebase user: " + e.getMessage());
        }
    }

    // Send approval email
    private void sendApprovalEmail(AccountRequest request) {
        String subject = "Congratulations! Your TimEd account is now verified";
        String htmlContent = createApprovalEmailHtml(request);
        String textContent = createApprovalEmailText(request);

        try {
            brevoEmailService.sendNotificationEmail(
                request.getEmail(),
                subject,
                htmlContent,
                textContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send approval email: " + e.getMessage());
        }
    }

    // Send rejection email
    private void sendRejectionEmail(AccountRequest request, String reason) {
        String subject = "TimEd Account Creation Request - Update";
        String htmlContent = createRejectionEmailHtml(request, reason);
        String textContent = createRejectionEmailText(request, reason);

        try {
            brevoEmailService.sendNotificationEmail(
                request.getEmail(),
                subject,
                htmlContent,
                textContent
            );
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }

    // Send pending reminder email
    private void sendPendingReminderEmailInternal(AccountRequest request) throws Exception {
        String subject = "Reminder: Your TimEd account request is pending";
        String htmlContent = createPendingReminderEmailHtml(request);
        String textContent = createPendingReminderEmailText(request);

        brevoEmailService.sendNotificationEmail(
            request.getEmail(),
            subject,
            htmlContent,
            textContent
        );
    }

    // Check if user already exists
    private boolean userAlreadyExists(String schoolId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("users")
            .whereEqualTo("schoolId", schoolId)
            .limit(1)
            .get();

        return !future.get().getDocuments().isEmpty();
    }

    // Check if pending request exists
    private boolean pendingRequestExists(String schoolId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
            .whereEqualTo("schoolId", schoolId)
            .whereEqualTo("status", "PENDING")
            .limit(1)
            .get();

        return !future.get().getDocuments().isEmpty();
    }

    // Create approval email HTML
    private String createApprovalEmailHtml(AccountRequest request) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="text-align: center; border-bottom: 2px solid #28a745; padding-bottom: 20px; margin-bottom: 20px;">
                        <h1 style="color: #28a745; margin: 0;">TimEd Account Verified!</h1>
                    </div>
                    
                    <h2 style="color: #28a745;">Congratulations, %s!</h2>
                    
                    <p>Your TimEd account creation request has been <strong>approved</strong>!</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Account Details:</strong></p>
                        <p style="margin: 5px 0 0 0;"><strong>Name:</strong> %s %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>School ID:</strong> %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Email:</strong> %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Department:</strong> %s</p>
                    </div>
                    
                    <p><strong>You can now log in to the TimEd mobile application using your School ID and password.</strong></p>
                    
                    <p>If you have any questions, please don't hesitate to contact us.</p>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                        <p style="color: #6c757d; font-size: 14px;">
                            Best regards,<br>
                            <strong>TimEd Team</strong><br>
                            <em>Your Attendance Management System</em>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            request.getFirstName(),
            request.getFirstName(), request.getLastName(),
            request.getSchoolId(),
            request.getEmail(),
            request.getDepartment()
        );
    }

    // Create approval email text
    private String createApprovalEmailText(AccountRequest request) {
        return String.format("""
            Congratulations, %s!
            
            Your TimEd account creation request has been approved!
            
            Account Details:
            Name: %s %s
            School ID: %s
            Email: %s
            Department: %s
            
            You can now log in to the TimEd mobile application using your School ID and password.
            
            If you have any questions, please don't hesitate to contact us.
            
            Best regards,
            TimEd Team
            Your Attendance Management System
            """,
            request.getFirstName(),
            request.getFirstName(), request.getLastName(),
            request.getSchoolId(),
            request.getEmail(),
            request.getDepartment()
        );
    }

    // Create rejection email HTML
    private String createRejectionEmailHtml(AccountRequest request, String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="text-align: center; border-bottom: 2px solid #dc3545; padding-bottom: 20px; margin-bottom: 20px;">
                        <h1 style="color: #dc3545; margin: 0;">TimEd Account Request Update</h1>
                    </div>
                    
                    <h2>Hello %s,</h2>
                    
                    <p>Thank you for your interest in TimEd. Unfortunately, your account creation request has been <strong>rejected</strong>.</p>
                    
                    <div style="background-color: #f8d7da; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #dc3545;">
                        <p style="margin: 0;"><strong>Reason for rejection:</strong></p>
                        <p style="margin: 5px 0 0 0;">%s</p>
                    </div>
                    
                    <p>If you believe this is an error or would like to appeal this decision, please contact the administrator.</p>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                        <p style="color: #6c757d; font-size: 14px;">
                            Best regards,<br>
                            <strong>TimEd Team</strong><br>
                            <em>Your Attendance Management System</em>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            request.getFirstName(),
            reason != null ? reason : "No specific reason provided"
        );
    }

    // Create rejection email text
    private String createRejectionEmailText(AccountRequest request, String reason) {
        return String.format("""
            Hello %s,
            
            Thank you for your interest in TimEd. Unfortunately, your account creation request has been rejected.
            
            Reason for rejection: %s
            
            If you believe this is an error or would like to appeal this decision, please contact the administrator.
            
            Best regards,
            TimEd Team
            Your Attendance Management System
            """,
            request.getFirstName(),
            reason != null ? reason : "No specific reason provided"
        );
    }

    // Create pending reminder email HTML
    private String createPendingReminderEmailHtml(AccountRequest request) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="text-align: center; border-bottom: 2px solid #0d6efd; padding-bottom: 20px; margin-bottom: 20px;">
                        <h1 style="color: #0d6efd; margin: 0;">TimEd Account Request Update</h1>
                    </div>
                    
                    <h2>Hello %s,</h2>
                    
                    <p>This is a friendly reminder that your TimEd account creation request is still <strong>pending review</strong>.</p>
                    <p>Our administrators are working on processing requests as quickly as possible. We'll notify you as soon as a decision has been made.</p>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #0d6efd;">
                        <p style="margin: 0;"><strong>Request Details:</strong></p>
                        <p style="margin: 5px 0 0 0;"><strong>Name:</strong> %s %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>School ID:</strong> %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Email:</strong> %s</p>
                        <p style="margin: 5px 0 0 0;"><strong>Department:</strong> %s</p>
                    </div>
                    
                    <p>If you have any questions or need to update your request details, please reach out to us.</p>
                    
                    <div style="text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6;">
                        <p style="color: #6c757d; font-size: 14px;">
                            Best regards,<br>
                            <strong>TimEd Team</strong><br>
                            <em>Your Attendance Management System</em>
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
            request.getFirstName(),
            request.getFirstName(), request.getLastName(),
            request.getSchoolId(),
            request.getEmail(),
            request.getDepartment() != null ? request.getDepartment() : "Not specified"
        );
    }

    // Create pending reminder email text
    private String createPendingReminderEmailText(AccountRequest request) {
        return String.format("""
            Hello %s,
            
            This is a friendly reminder that your TimEd account creation request is still pending review. Our administrators are working on processing requests as quickly as possible and we will notify you as soon as a decision has been made.
            
            Request Details:
            Name: %s %s
            School ID: %s
            Email: %s
            Department: %s
            
            If you have any questions or need to update your request details, please reach out to us.
            
            Best regards,
            TimEd Team
            Your Attendance Management System
            """,
            request.getFirstName(),
            request.getFirstName(), request.getLastName(),
            request.getSchoolId(),
            request.getEmail(),
            request.getDepartment() != null ? request.getDepartment() : "Not specified"
        );
    }
} 