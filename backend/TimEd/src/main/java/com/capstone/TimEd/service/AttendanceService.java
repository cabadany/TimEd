package com.capstone.TimEd.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class AttendanceService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final FirebaseApp firebaseApp;

    public AttendanceService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
        // Da, we initialize FirebaseApp before using Firestore.
    }

    public String markAttendance(String eventId, String userId) {
        try {
            String now = Instant.now().toString();

            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("attended", true);
            attendanceData.put("timeIn", now);

            firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId)
                    .set(attendanceData);

            Map<String, Object> userSide = new HashMap<>();
            userSide.put("eventId", eventId);
            userSide.put("timeIn", now);

            firestore.collection("users")
                    .document(userId)
                    .collection("attendedEvents")
                    .document(eventId)
                    .set(userSide);

            return "Attendance marked (time-in) for user " + userId + " at event " + eventId;

        } catch (Exception e) {
            return "Failed to mark attendance: " + e.getMessage();
        }
    }

    public String markTimeOut(String eventId, String userId) {
        try {
            String now = Instant.now().toString();

            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("timeOut", now);

            firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId)
                    .update(updateMap);

            firestore.collection("users")
                    .document(userId)
                    .collection("attendedEvents")
                    .document(eventId)
                    .update(updateMap);

            return "Time-out recorded for user " + userId + " at event " + eventId;

        } catch (Exception e) {
            return "Failed to mark time-out: " + e.getMessage();
        }
    }

    public List<Map<String, String>> getAttendees(String eventId) {
        List<Map<String, String>> attendees = new ArrayList<>();

        try {
            CollectionReference attendeesRef = firestore
                    .collection("events")
                    .document(eventId)
                    .collection("attendees");

            ApiFuture<QuerySnapshot> query = attendeesRef.get();
            List<QueryDocumentSnapshot> docs = query.get().getDocuments();

            for (QueryDocumentSnapshot doc : docs) {
                String userId = doc.getId();
                String timeIn = doc.getString("timeIn");
                String timeOut = doc.getString("timeOut");

                DocumentReference userRef = firestore.collection("users").document(userId);
                ApiFuture<DocumentSnapshot> userQuery = userRef.get();
                DocumentSnapshot userDoc = userQuery.get();

                if (userDoc.exists()) {
                    Map<String, String> userDetails = new HashMap<>();
                    userDetails.put("userId", userId);
                    userDetails.put("timeIn", timeIn);
                    userDetails.put("timeOut", timeOut != null ? timeOut : "N/A");
                    userDetails.put("email", userDoc.getString("email"));
                    userDetails.put("firstName", userDoc.getString("firstName"));
                    userDetails.put("lastName", userDoc.getString("lastName"));
                    userDetails.put("department", userDoc.getString("department"));

                    attendees.add(userDetails);
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching attendees: " + e.getMessage());
        }

        return attendees;
    }
}
