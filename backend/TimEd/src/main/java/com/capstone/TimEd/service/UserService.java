package com.capstone.TimEd.service;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.capstone.TimEd.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

@Service
public class UserService {

    private static final String COLLECTION_NAME = "users";

    // Create a new user in Firestore
    public void createUser(User user) throws InterruptedException, ExecutionException {
        Firestore db = FirestoreClient.getFirestore();
        
        // Store the user in Firestore with userId (which should ideally be from Firebase Auth UID)
        ApiFuture<WriteResult> result = db.collection(COLLECTION_NAME).document(user.getUserId()).set(user);
        result.get(); // Waits for write to complete
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
}
