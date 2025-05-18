package com.capstone.TimEd.service;

import com.capstone.TimEd.dto.ExcuseLetterDto;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.model.ExcuseLetter;
import com.capstone.TimEd.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.*;
import com.google.firebase.database.Query;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class ExcuseLetterService {
    // Keep Firestore references for User and Event data if needed
    private final Firestore firestore = FirestoreClient.getFirestore();
    private final FirebaseApp firebaseApp;
    private final CollectionReference usersCollection = firestore.collection("users");
    private final CollectionReference eventsCollection = firestore.collection("events");
    
    // Firebase Realtime Database reference
    private final DatabaseReference dbRef;
    private final DatabaseReference excuseLettersRef;
    
    public ExcuseLetterService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
        
        // Initialize Firebase Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference();
        excuseLettersRef = dbRef.child("excuseLetters");
    }

    // Helper method to validate excuse letter data
    private ValidationResult validateExcuseLetter(ExcuseLetter letter) {
        ValidationResult result = new ValidationResult();
        
        if (letter == null) {
            result.addError("Excuse letter data cannot be null");
            return result;
        }
        
        // Check ID Number
        if (letter.getIdNumber() == null || letter.getIdNumber().trim().isEmpty()) {
            result.addError("ID Number is required");
        } else if (letter.getIdNumber().trim().equalsIgnoreCase("N/A")) {
            result.addError("ID Number cannot be 'N/A'");
        }
        
        // Check First Name
        if (letter.getFirstName() == null || letter.getFirstName().trim().isEmpty()) {
            result.addError("First Name is required");
        } else if (letter.getFirstName().trim().equalsIgnoreCase("Unknown")) {
            result.addError("First Name cannot be 'Unknown'");
        }
        
        // Check Date
        if (letter.getDate() == null || letter.getDate().trim().isEmpty()) {
            result.addError("Date is required");
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                sdf.setLenient(false);
                sdf.parse(letter.getDate().trim());
            } catch (Exception e) {
                result.addError("Invalid date format. Use dd/MM/yyyy");
            }
        }
        
        // Check Reason
        if (letter.getReason() == null || letter.getReason().trim().isEmpty()) {
            result.addError("Reason is required");
        }
        
        // Check Details
        if (letter.getDetails() == null || letter.getDetails().trim().isEmpty()) {
            result.addError("Details are required");
        } else if (letter.getDetails().trim().length() < 10) {
            result.addError("Details must be at least 10 characters long");
        }
        
        // Check Email
        if (letter.getEmail() == null || letter.getEmail().trim().isEmpty()) {
            result.addError("Email is required");
        } else if (!letter.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            result.addError("Invalid email format");
        }
        
        // Check User ID
        if (letter.getUserId() == null || letter.getUserId().trim().isEmpty()) {
            result.addError("User ID is required");
        }
        
        return result;
    }

    // Helper class for validation results
    private static class ValidationResult {
        private final List<String> errors;
        
        public ValidationResult() {
            this.errors = new ArrayList<>();
        }
        
        public void addError(String error) {
            errors.add(error);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }

    // Add a new excuse letter
    public String addExcuseLetter(ExcuseLetter excuseLetter) {
        try {
            // Validate the excuse letter
            ValidationResult validationResult = validateExcuseLetter(excuseLetter);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getErrorMessage());
            }
            
            // Set default status to "Pending" if not provided
            if (excuseLetter.getStatus() == null || excuseLetter.getStatus().isEmpty()) {
                excuseLetter.setStatus("Pending");
            }
            
            // Set submittedAt timestamp if not provided
            if (excuseLetter.getSubmittedAt() == 0) {
                excuseLetter.setSubmittedAt(System.currentTimeMillis());
            }
            
            // Get the user's reference
            DatabaseReference userExcuseLettersRef = excuseLettersRef.child(excuseLetter.getUserId());
            
            // Generate a new push key under the user's reference
            String key = userExcuseLettersRef.push().getKey();
            excuseLetter.setId(key);
            
            // Save to Realtime Database under the user's node
            CountDownLatch countDownLatch = new CountDownLatch(1);
            userExcuseLettersRef.child(key).setValue(excuseLetter, (error, ref) -> {
                countDownLatch.countDown();
            });
            
            // Wait for operation to complete
            boolean completed = countDownLatch.await(30, TimeUnit.SECONDS);
            
            if (completed) {
                System.out.println("Excuse letter added successfully with ID: " + key);
                return key;
            } else {
                throw new RuntimeException("Timeout occurred while saving excuse letter");
            }
        } catch (IllegalArgumentException e) {
            return "Failed to add excuse letter: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("Error adding excuse letter: " + e.getMessage());
            e.printStackTrace();
            return "Failed to add excuse letter: " + e.getMessage();
        }
    }

    // Get all excuse letters with pagination and optional status filter
    public List<ExcuseLetterDto> getPaginatedExcuseLetters(int page, int size, String statusFilter, String startDateStr, String endDateStr) 
            throws ExecutionException, InterruptedException {
        List<ExcuseLetterDto> excuseLetterDtos = new ArrayList<>();
        List<ExcuseLetter> allLetters = new ArrayList<>();

        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            
            excuseLettersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        
                        for (DataSnapshot letterSnapshot : userSnapshot.getChildren()) {
                            ExcuseLetter letter = letterSnapshot.getValue(ExcuseLetter.class);
                            if (letter != null) {
                                ValidationResult validationResult = validateExcuseLetter(letter);
                                if (validationResult.isValid()) {
                                    letter.setId(letterSnapshot.getKey());
                                    letter.setUserId(userId);
                                    
                                    // Apply status filter if provided
                                    if (statusFilter == null || statusFilter.isEmpty() || 
                                        statusFilter.equalsIgnoreCase("All") || 
                                        letter.getStatus().equals(statusFilter)) {
                                        
                                        // Apply date filter if provided
                                        if (startDateStr != null && !startDateStr.isEmpty() && 
                                            endDateStr != null && !endDateStr.isEmpty()) {
                                            try {
                                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                                Date startDate = sdf.parse(startDateStr);
                                                Date endDate = sdf.parse(endDateStr);
                                                long startDateMs = startDate.getTime();
                                                long endDateMs = endDate.getTime() + (24 * 60 * 60 * 1000 - 1);
                                                
                                                if (letter.getSubmittedAt() >= startDateMs && 
                                                    letter.getSubmittedAt() <= endDateMs) {
                                                    allLetters.add(letter);
                                                }
                                            } catch (Exception e) {
                                                System.err.println("Error parsing date filter: " + e.getMessage());
                                            }
                                        } else {
                                            allLetters.add(letter);
                                        }
                                    }
                                } else {
                                    System.err.println("Invalid excuse letter found: " + validationResult.getErrorMessage());
                                }
                            }
                        }
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error fetching letters: " + databaseError.getMessage());
                    countDownLatch.countDown();
                }
            });
            
            countDownLatch.await(30, TimeUnit.SECONDS);
            
            // Sort by submission time (newest first)
            allLetters.sort((l1, l2) -> Long.compare(l2.getSubmittedAt(), l1.getSubmittedAt()));
            
            // Apply pagination
            int totalSize = allLetters.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalSize);
            
            if (startIndex < totalSize) {
                List<ExcuseLetter> paginatedLetters = allLetters.subList(startIndex, endIndex);
                
                for (ExcuseLetter letter : paginatedLetters) {
                    ExcuseLetterDto dto = new ExcuseLetterDto(
                        letter.getId(),
                        letter.getUserId(),
                        letter.getEventId(),
                        letter.getDate(),
                        letter.getDetails(),
                        letter.getReason(),
                        letter.getStatus(),
                        letter.getSubmittedAt(),
                        letter.getFirstName(),
                        "", // eventName is not used in the new structure
                        letter.getRejectionReason(),
                        letter.getAttachmentUrl(),
                        letter.getDepartment(),
                        letter.getEmail(),
                        letter.getFirstName(),
                        letter.getIdNumber()
                    );
                    
                    excuseLetterDtos.add(dto);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching excuse letters: " + e.getMessage());
            e.printStackTrace();
        }

        return excuseLetterDtos;
    }

    // Get excuse letter by ID and userId
    public ExcuseLetter getExcuseLetterById(String userId, String letterId) throws ExecutionException, InterruptedException {
        if (userId == null || userId.trim().isEmpty() || letterId == null || letterId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Letter ID cannot be null or empty");
        }
        
        try {
            final ExcuseLetter[] letter = {null};
            CountDownLatch countDownLatch = new CountDownLatch(1);
            
            excuseLettersRef.child(userId).child(letterId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        letter[0] = dataSnapshot.getValue(ExcuseLetter.class);
                        if (letter[0] != null) {
                            letter[0].setId(dataSnapshot.getKey());
                            letter[0].setUserId(userId);
                        }
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error retrieving letter: " + databaseError.getMessage());
                    countDownLatch.countDown();
                }
            });
            
            countDownLatch.await(30, TimeUnit.SECONDS);
            return letter[0];
        } catch (Exception e) {
            System.err.println("Error retrieving excuse letter: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve excuse letter: " + e.getMessage(), e);
        }
    }

    // Update the status of an excuse letter
    public ExcuseLetter updateExcuseLetterStatus(String userId, String letterId, String status, String rejectionReason) 
            throws ExecutionException, InterruptedException {
        if (userId == null || userId.trim().isEmpty() || letterId == null || letterId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Letter ID cannot be null or empty");
        }
        
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        if (!status.equals("Pending") && !status.equals("Approved") && !status.equals("Rejected")) {
            throw new IllegalArgumentException("Invalid status. Must be Pending, Approved, or Rejected.");
        }
        
        try {
            // Update in Realtime Database
            DatabaseReference letterRef = excuseLettersRef.child(userId).child(letterId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            
            if ("Rejected".equals(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                updates.put("rejectionReason", rejectionReason);
            } else if (!"Rejected".equals(status)) {
                updates.put("rejectionReason", null);
            }
            
            CountDownLatch countDownLatch = new CountDownLatch(1);
            letterRef.updateChildren(updates, (error, ref) -> {
                countDownLatch.countDown();
            });
            
            boolean completed = countDownLatch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                throw new RuntimeException("Timeout occurred while updating excuse letter status");
            }
            
            // Return the updated letter
            return getExcuseLetterById(userId, letterId);
        } catch (Exception e) {
            System.err.println("Error updating excuse letter: " + e.getMessage());
            throw new RuntimeException("Failed to update excuse letter status: " + e.getMessage(), e);
        }
    }

    // Delete an excuse letter
    public String deleteExcuseLetter(String userId, String letterId) {
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            excuseLettersRef.child(userId).child(letterId).removeValue((error, ref) -> {
                countDownLatch.countDown();
            });
            
            boolean completed = countDownLatch.await(30, TimeUnit.SECONDS);
            
            if (completed) {
                return "Excuse letter deleted successfully";
            } else {
                throw new RuntimeException("Timeout occurred while deleting excuse letter");
            }
        } catch (Exception e) {
            System.err.println("Error deleting excuse letter: " + e.getMessage());
            return "Failed to delete excuse letter: " + e.getMessage();
        }
    }
    
    // Get excuse letters by user ID
    public List<ExcuseLetterDto> getExcuseLettersByUserId(String userId) throws ExecutionException, InterruptedException {
        List<ExcuseLetterDto> userLetters = new ArrayList<>();
        
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            
            excuseLettersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot letterSnapshot : dataSnapshot.getChildren()) {
                        ExcuseLetter letter = letterSnapshot.getValue(ExcuseLetter.class);
                        if (letter != null) {
                            ValidationResult validationResult = validateExcuseLetter(letter);
                            if (validationResult.isValid()) {
                                letter.setId(letterSnapshot.getKey());
                                letter.setUserId(userId);
                                
                                ExcuseLetterDto dto = new ExcuseLetterDto(
                                    letter.getId(),
                                    letter.getUserId(),
                                    letter.getEventId(),
                                    letter.getDate(),
                                    letter.getDetails(),
                                    letter.getReason(),
                                    letter.getStatus(),
                                    letter.getSubmittedAt(),
                                    letter.getFirstName(),
                                    "", // eventName is not used in the new structure
                                    letter.getRejectionReason(),
                                    letter.getAttachmentUrl(),
                                    letter.getDepartment(),
                                    letter.getEmail(),
                                    letter.getFirstName(),
                                    letter.getIdNumber()
                                );
                                
                                userLetters.add(dto);
                            } else {
                                System.err.println("Invalid excuse letter found: " + validationResult.getErrorMessage());
                            }
                        }
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error fetching user's excuse letters: " + databaseError.getMessage());
                    countDownLatch.countDown();
                }
            });
            
            countDownLatch.await(30, TimeUnit.SECONDS);
            
            // Sort by submission time (newest first)
            userLetters.sort((l1, l2) -> Long.compare(l2.getSubmittedAt(), l1.getSubmittedAt()));
        } catch (Exception e) {
            System.err.println("Error fetching user's excuse letters: " + e.getMessage());
        }
        
        return userLetters;
    }
} 