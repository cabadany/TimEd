package com.capstone.TimEd.service;

import com.capstone.TimEd.model.Event;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.*;

@Service
public class EventService {

    private static final String COLLECTION_NAME = "events";

    // 🔨 Create or Update
    public String saveEvent(Event event) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }

        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME)
                .document(event.getEventId())
                .set(event);

        return future.get().getUpdateTime().toString();
    }

    // 👀 Read by ID
    public Event getEventById(String eventId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(eventId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(Event.class);
        } else {
            return null;
        }
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
