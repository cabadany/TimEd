package com.capstone.TimEd.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class UserService {
	@Autowired
	 private Firestore firestore;
    private static final String COLLECTION_NAME = "users";

    // Create a new user in Firestore
    public void createUser(User user) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        
        // Store the user in Firestore with userId (which should ideally be from Firebase Auth UID)
        ApiFuture<WriteResult> result = db.collection(COLLECTION_NAME).document(user.getUserId()).set(user);
        result.get(); // Waits for write to complete
    }
    public List<User> getAllUsers() throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<User> userList = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            User user = doc.toObject(User.class);
            userList.add(user);
        }

        return userList;
    }
    // Get user by their schoolId
    public User getUserBySchoolId(String schoolId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        // Query Firestore for user based on schoolId
        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                                            .whereEqualTo("schoolId", schoolId)
                                            .limit(1)
                                            .get();

        QuerySnapshot snapshot = future.get();

        if (!snapshot.isEmpty()) {
            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            return doc.toObject(User.class);  // Convert Firestore document to User object
        }

        return null;  // Return null if no user is found
    }
    
    public void updateUser(String identifier, User updatedUser) throws InterruptedException, ExecutionException {
        if (identifier == null || updatedUser == null) {
            throw new IllegalArgumentException("Identifier or updated user data cannot be null.");
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference userDocRef = null;

        // Check if identifier is schoolId or userId and set correct document reference
        if (isValidSchoolId(identifier)) {
            // If it's a schoolId, fetch user by schoolId
            userDocRef = getUserDocRefBySchoolId(identifier, db);
        } else {
            // If it's a userId, fetch user by userId
            userDocRef = db.collection(COLLECTION_NAME).document(identifier);
        }

        // Fetch user document
        DocumentSnapshot snapshot = userDocRef.get().get();
    
        // Check password: if not provided in updatedUser, retain existing one
        if (updatedUser.getPassword() == null) {
            String existingPassword = snapshot.getString("password");
            updatedUser.setPassword(existingPassword);
        } else {
            // If password is provided, hash it before updating
            updatedUser.setPassword(hashPassword(updatedUser.getPassword()));
        }

        // Handle department if present
        Department department = updatedUser.getDepartment();
        if (department != null && department.getDepartmentId() != null) {
            updatedUser.setDepartment(department);
            updatedUser.setDepartmentId(department.getDepartmentId());
        } else {
            updatedUser.setDepartment(null);
            updatedUser.setDepartmentId(null);
        }

        try {
            // Perform update (merge the data so existing data isn't overwritten)
            userDocRef.set(updatedUser, SetOptions.merge()).get(); // Merge instead of overwrite
        } catch (ExecutionException | InterruptedException e) {
            // Handle the exception in case the Firestore operation fails
            throw new RuntimeException("Error updating user data: " + e.getMessage(), e);
        }
    }

    public void deleteUser(String userId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot snapshot = future.get();

        if (snapshot.exists()) {
            try {
                // 1. Delete all attended events records for this user
                CollectionReference attendedEventsRef = docRef.collection("attendedEvents");
                ApiFuture<QuerySnapshot> attendedEventsFuture = attendedEventsRef.get();
                List<QueryDocumentSnapshot> attendedEvents = attendedEventsFuture.get().getDocuments();

                // Use batch write for better performance and atomicity
                WriteBatch batch = db.batch();

                // Delete all attended events documents
                for (QueryDocumentSnapshot attendedEvent : attendedEvents) {
                    batch.delete(attendedEvent.getReference());
                }

                // 2. Remove user from all events' attendees collections
                CollectionReference eventsRef = db.collection("events");
                ApiFuture<QuerySnapshot> eventsFuture = eventsRef.get();
                List<QueryDocumentSnapshot> events = eventsFuture.get().getDocuments();

                for (QueryDocumentSnapshot event : events) {
                    DocumentReference attendeeRef = event.getReference()
                        .collection("attendees")
                        .document(userId);
                    
                    // Add delete operation to batch
                    batch.delete(attendeeRef);
                }

                // 3. Delete the user document itself
                batch.delete(docRef);

                // Commit all the delete operations
                batch.commit().get();

                // 4. Delete from Firebase Auth
                try {
                    FirebaseAuth.getInstance().deleteUser(userId);
                } catch (FirebaseAuthException e) {
                    throw new RuntimeException("Failed to delete from Firebase Auth: " + e.getMessage());
                }

            } catch (Exception e) {
                throw new RuntimeException("Failed to delete user and associated data: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("User with ID " + userId + " not found.");
        }
    }
    public User getUserById(String userId) throws Exception {
        DocumentReference userRef = firestore.collection("users").document(userId);
        ApiFuture<DocumentSnapshot> future = userRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        } else {
            return null;
        }
    }
    private boolean isValidSchoolId(String identifier) {
        // Example validation logic for schoolId
        return identifier != null && identifier.matches("\\d{2}-\\d{4}-\\d{3}"); // Example format: 22-2220-751
    }

    // Utility method to get DocumentReference by schoolId
    private DocumentReference getUserDocRefBySchoolId(String schoolId, Firestore db) throws InterruptedException, ExecutionException {
        // Query to fetch the user document by schoolId
        QuerySnapshot querySnapshot = db.collection(COLLECTION_NAME)
                .whereEqualTo("schoolId", schoolId)
                .get().get();
        
        if (querySnapshot.isEmpty()) {
            throw new RuntimeException("User with School ID " + schoolId + " not found.");
        }

        return querySnapshot.getDocuments().get(0).getReference(); // Get first document reference
    }

    // Get user by their Firestore userId (custom UID)
    public User getUserByUserId(String userId) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        
        // Query Firestore based on userId (this could be the UID from Firebase Auth)
        ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION_NAME).document(userId).get();
        DocumentSnapshot documentSnapshot = future.get();
        
        if (documentSnapshot.exists()) {
            return documentSnapshot.toObject(User.class);
        }

        return null;  // Return null if no user is found
    }
    
    private String hashPassword(String password) {
        // Use BCrypt or any other hashing method you prefer
        return new BCryptPasswordEncoder().encode(password);
    }
    
    
}
