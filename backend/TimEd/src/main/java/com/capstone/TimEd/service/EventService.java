package com.capstone.TimEd.service;

import com.capstone.TimEd.model.Event;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.*;

@Service
public class EventService {

    private static final String COLLECTION_NAME = "events";
    
    // 🔨 Create or Update
    public String saveEvent(Event event) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference docRef;

        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            // Let Firebase auto-generate the document ID (default UUID)
            docRef = db.collection(COLLECTION_NAME).document(); // Firebase will generate UID
            event.setEventId(docRef.getId()); // Save the generated UID back to event object
        } else {
            // Use provided ID if it exists
            docRef = db.collection(COLLECTION_NAME).document(event.getEventId());
        }

        ApiFuture<WriteResult> future = docRef.set(event);
        return future.get().getUpdateTime().toString();
    }

    // 📄 Read All
    public List<Event> getAllEvents() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Fetch the snapshot of the "events" collection
        ApiFuture<QuerySnapshot> querySnapshotFuture = db.collection(COLLECTION_NAME).get();
        QuerySnapshot querySnapshot = querySnapshotFuture.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        List<Event> events = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            Event event = doc.toObject(Event.class);

            // Handle the Timestamp field (date)
            Timestamp timestamp = doc.getTimestamp("date");
            if (timestamp != null) {
                // Convert Timestamp to Date
                event.setDate(timestamp.toDate());  // Set the Date object directly
            }

            events.add(event);
        }

        return events;
    }


    public Event updateEvent(String eventId, Event updatedEvent) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(eventId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            // Update the existing document with new data
            docRef.set(updatedEvent);
            return updatedEvent;
        } else {
            throw new RuntimeException("Event not found with ID: " + eventId);
        }
    }
    // 🧨 Delete
    public String deleteEvent(String eventId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<WriteResult> writeResult = db.collection(COLLECTION_NAME).document(eventId).delete();
        return "Event deleted at: " + writeResult.get().getUpdateTime();
    }
}
