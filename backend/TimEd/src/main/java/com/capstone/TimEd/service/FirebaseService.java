package com.capstone.TimEd.service;

import com.capstone.TimEd.model.Event;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.api.core.ApiFuture;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class FirebaseService {

    private Firestore firestore;

    public FirebaseService() {
        // Initialize Firestore
        this.firestore = FirestoreClient.getFirestore();
    }

    // Method to get Events by departmentId
    public List<Event> getEventsByDepartmentId(String departmentId) throws ExecutionException, InterruptedException {
        CollectionReference eventsCollection = firestore.collection("events");

        // Query Firestore for events where the departmentId matches
        Query query = eventsCollection.whereEqualTo("departmentId", departmentId);

        // Execute the query and get the ApiFuture
        ApiFuture<com.google.cloud.firestore.QuerySnapshot> future = query.get();

        // Get the list of documents and convert them to Event objects
        List<Event> events = future.get().getDocuments().stream()
                .map(document -> document.toObject(Event.class))
                .collect(Collectors.toList());

        return events; // Return the list of events
    }
}
