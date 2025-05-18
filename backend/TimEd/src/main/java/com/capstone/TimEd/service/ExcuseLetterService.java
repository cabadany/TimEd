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

    // Add a new excuse letter
    public String addExcuseLetter(ExcuseLetter excuseLetter) {
        try {
            // Set default status to "Pending" if not provided
            if (excuseLetter.getStatus() == null || excuseLetter.getStatus().isEmpty()) {
                excuseLetter.setStatus("Pending");
            }
            
            // Set submittedAt timestamp if not provided
            if (excuseLetter.getSubmittedAt() == 0) {
                excuseLetter.setSubmittedAt(System.currentTimeMillis());
            }
            
            // Generate a new push key
            String key = excuseLettersRef.push().getKey();
            excuseLetter.setId(key);
            
            // Save to Realtime Database
            CountDownLatch countDownLatch = new CountDownLatch(1);
            excuseLettersRef.child(key).setValue(excuseLetter, (error, ref) -> {
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
            // Create query based on filters
            Query query = excuseLettersRef;
            
            // Apply status filter if provided
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
                query = query.orderByChild("status").equalTo(statusFilter);
            }
            
            // Fetch data from Realtime Database
            CountDownLatch countDownLatch = new CountDownLatch(1);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ExcuseLetter letter = snapshot.getValue(ExcuseLetter.class);
                        if (letter != null) {
                            // Make sure ID is set (may not be included in the value)
                            letter.setId(snapshot.getKey());
                            
                            // Apply date filter manually
                            if (startDateStr != null && !startDateStr.isEmpty() && endDateStr != null && !endDateStr.isEmpty()) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                    Date startDate = sdf.parse(startDateStr);
                                    Date endDate = sdf.parse(endDateStr);
                                    long startDateMs = startDate.getTime();
                                    long endDateMs = endDate.getTime() + (24 * 60 * 60 * 1000 - 1); // End of the end date
                                    
                                    if (letter.getSubmittedAt() >= startDateMs && letter.getSubmittedAt() <= endDateMs) {
                                        allLetters.add(letter);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error parsing date filter: " + e.getMessage());
                                    allLetters.add(letter);
                                }
                            } else {
                                allLetters.add(letter);
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
            
            // Wait for data retrieval
            countDownLatch.await(30, TimeUnit.SECONDS);
            
            // Sort by submission time (newest first)
            allLetters.sort((l1, l2) -> Long.compare(l2.getSubmittedAt(), l1.getSubmittedAt()));
            
            // Apply pagination
            int totalSize = allLetters.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalSize);
            
            System.out.println("Total letters found: " + totalSize);
            
            if (startIndex < totalSize) {
                List<ExcuseLetter> paginatedLetters = allLetters.subList(startIndex, endIndex);
                
                for (ExcuseLetter letter : paginatedLetters) {
                    // For letters from Realtime DB, we already have firstName and other user details
                    String userName = letter.getFirstName() != null ? letter.getFirstName() : "Unknown";
                    
                    if (userName.equals("Unknown") && letter.getUserId() != null && !letter.getUserId().isEmpty()) {
                        try {
                            // Try to get user from Firestore if needed
                            DocumentSnapshot userDoc = usersCollection.document(letter.getUserId()).get().get();
                            if (userDoc.exists()) {
                                User user = userDoc.toObject(User.class);
                                userName = user.getFirstName() + " " + user.getLastName();
                            }
                        } catch (Exception e) {
                            System.err.println("Error fetching user: " + e.getMessage());
                        }
                    }
                    
                    // Get event information if needed
                    String eventName = "";
                    if (letter.getEventId() != null && !letter.getEventId().isEmpty()) {
                        try {
                            DocumentSnapshot eventDoc = eventsCollection.document(letter.getEventId()).get().get();
                            if (eventDoc.exists()) {
                                Event event = eventDoc.toObject(Event.class);
                                eventName = event.getEventName();
                            }
                        } catch (Exception e) {
                            System.err.println("Error fetching event: " + e.getMessage());
                        }
                    }
                    
                    ExcuseLetterDto dto = new ExcuseLetterDto(
                        letter.getId(),
                        letter.getUserId(),
                        letter.getEventId(),
                        letter.getDate(),
                        letter.getDetails(),
                        letter.getReason(),
                        letter.getStatus(),
                        letter.getSubmittedAt(),
                        userName,
                        eventName,
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

    // Get total count of excuse letters (for pagination)
    public int getTotalExcuseLetterCount(String statusFilter) throws ExecutionException, InterruptedException {
        try {
            // Create a latch to wait for the async operation
            CountDownLatch countDownLatch = new CountDownLatch(1);
            final int[] count = {0};
            
            // Query to get count
            Query query = excuseLettersRef;
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equalsIgnoreCase("All")) {
                query = query.orderByChild("status").equalTo(statusFilter);
            }
            
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    count[0] = (int) dataSnapshot.getChildrenCount();
                    countDownLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error getting count: " + databaseError.getMessage());
                    countDownLatch.countDown();
                }
            });
            
            // Wait for data retrieval
            countDownLatch.await(30, TimeUnit.SECONDS);
            return count[0];
        } catch (Exception e) {
            System.err.println("Error getting total excuse letter count: " + e.getMessage());
            return 0;
        }
    }

    // Get excuse letter by ID
    public ExcuseLetter getExcuseLetterById(String id) throws ExecutionException, InterruptedException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Excuse letter ID cannot be null or empty");
        }
        
        try {
            final ExcuseLetter[] letter = {null};
            CountDownLatch countDownLatch = new CountDownLatch(1);
            
            System.out.println("Fetching excuse letter with ID: " + id);
            excuseLettersRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        letter[0] = dataSnapshot.getValue(ExcuseLetter.class);
                        if (letter[0] != null) {
                            letter[0].setId(dataSnapshot.getKey());
                            System.out.println("Found letter with status: " + letter[0].getStatus());
                        } else {
                            System.err.println("Error mapping data to ExcuseLetter object, dataSnapshot value: " + dataSnapshot.getValue());
                        }
                    } else {
                        System.err.println("No data found for letter ID: " + id);
                    }
                    countDownLatch.countDown();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.err.println("Error retrieving letter: " + databaseError.getMessage());
                    countDownLatch.countDown();
                }
            });
            
            // Wait for data retrieval
            countDownLatch.await(30, TimeUnit.SECONDS);
            
            if (letter[0] == null) {
                System.err.println("No excuse letter found with ID: " + id);
            }
            
            return letter[0];
        } catch (Exception e) {
            System.err.println("Error retrieving excuse letter: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve excuse letter: " + e.getMessage(), e);
        }
    }

    // Update the status of an excuse letter
    public ExcuseLetter updateExcuseLetterStatus(String id, String status, String rejectionReason) throws ExecutionException, InterruptedException {
        // Validate inputs
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Excuse letter ID cannot be null or empty");
        }
        
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        // Validate status
        if (!status.equals("Pending") && !status.equals("Approved") && !status.equals("Rejected")) {
            throw new IllegalArgumentException("Invalid status. Status must be Pending, Approved, or Rejected.");
        }
        
        try {
            System.out.println("Starting status update for letter ID: " + id + ", new status: " + status);
            
            // First get the current letter
            ExcuseLetter letter = getExcuseLetterById(id);
            
            if (letter == null) {
                throw new IllegalArgumentException("Excuse letter with ID " + id + " not found.");
            }
            
            System.out.println("Current letter status: " + letter.getStatus() + ", updating to: " + status);
            
            // Update fields
            letter.setStatus(status);
            
            // Add rejection reason if status is Rejected
            if ("Rejected".equals(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                letter.setRejectionReason(rejectionReason);
                System.out.println("Setting rejection reason: " + rejectionReason);
            } else if (!"Rejected".equals(status)) {
                // Clear any existing rejection reason if not rejected
                letter.setRejectionReason(null);
            }
            
            // Update in Realtime Database
            CountDownLatch countDownLatch = new CountDownLatch(1);
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            
            if ("Rejected".equals(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
                updates.put("rejectionReason", rejectionReason);
            } else if (!"Rejected".equals(status)) {
                updates.put("rejectionReason", null);
            }
            
            // Track errors in the callback
            final DatabaseError[] updateError = {null};
            
            excuseLettersRef.child(id).updateChildren(updates, (error, ref) -> {
                if (error != null) {
                    System.err.println("Error in updateChildren callback: " + error.getMessage());
                    updateError[0] = error;
                } else {
                    System.out.println("Successfully updated status for letter ID: " + id);
                }
                countDownLatch.countDown();
            });
            
            // Wait for operation to complete
            boolean completed = countDownLatch.await(30, TimeUnit.SECONDS);
            
            if (!completed) {
                throw new RuntimeException("Timeout occurred while updating excuse letter status");
            }
            
            if (updateError[0] != null) {
                throw new RuntimeException("Database error: " + updateError[0].getMessage());
            }
            
            // Return the updated letter
            return getExcuseLetterById(id);
        } catch (Exception e) {
            System.err.println("Error updating excuse letter: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update excuse letter status: " + e.getMessage(), e);
        }
    }

    // Delete an excuse letter
    public String deleteExcuseLetter(String id) {
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            excuseLettersRef.child(id).removeValue((error, ref) -> {
                countDownLatch.countDown();
            });
            
            // Wait for operation to complete
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
            Query query = excuseLettersRef.orderByChild("userId").equalTo(userId);
            
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ExcuseLetter letter = snapshot.getValue(ExcuseLetter.class);
                        if (letter != null) {
                            letter.setId(snapshot.getKey());
                            
                            // For letters from Realtime DB, we already have firstName and other user details
                            String userName = letter.getFirstName() != null ? letter.getFirstName() : "Unknown";
                            
                            // Get event information if needed
                            String eventName = "";
                            if (letter.getEventId() != null && !letter.getEventId().isEmpty()) {
                                try {
                                    DocumentSnapshot eventDoc = eventsCollection.document(letter.getEventId()).get().get();
                                    if (eventDoc.exists()) {
                                        Event event = eventDoc.toObject(Event.class);
                                        eventName = event.getEventName();
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error fetching event: " + e.getMessage());
                                }
                            }
                            
                            ExcuseLetterDto dto = new ExcuseLetterDto(
                                letter.getId(),
                                letter.getUserId(),
                                letter.getEventId(),
                                letter.getDate(),
                                letter.getDetails(),
                                letter.getReason(),
                                letter.getStatus(),
                                letter.getSubmittedAt(),
                                userName,
                                eventName,
                                letter.getRejectionReason(),
                                letter.getAttachmentUrl(),
                                letter.getDepartment(),
                                letter.getEmail(),
                                letter.getFirstName(),
                                letter.getIdNumber()
                            );
                            
                            userLetters.add(dto);
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
            
            // Wait for data retrieval
            countDownLatch.await(30, TimeUnit.SECONDS);
            
            // Sort by submission time (newest first)
            userLetters.sort((l1, l2) -> Long.compare(l2.getSubmittedAt(), l1.getSubmittedAt()));
        } catch (Exception e) {
            System.err.println("Error fetching user's excuse letters: " + e.getMessage());
        }
        
        return userLetters;
    }
} 