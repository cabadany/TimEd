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
            // Check if user has already timed in for this event
            DocumentReference attendeeRef = firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId);
            
            DocumentSnapshot attendeeDoc = attendeeRef.get().get();
            if (attendeeDoc.exists() && attendeeDoc.getBoolean("attended") != null && attendeeDoc.getBoolean("attended")) {
                return "Already timed in for this event. Certificate has already been generated.";
            }

            String now = Instant.now().toString();

            // Fetch event details
            DocumentSnapshot eventDoc = firestore.collection("events").document(eventId).get().get();

            if (!eventDoc.exists()) {
                return "Event does not exist: " + eventId;
            }

            String eventName = eventDoc.getString("eventName");
            String eventStatus = eventDoc.getString("status");
            Object eventDate = eventDoc.get("date"); // Timestamp type

            // Set on event-side
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("attended", true);
            attendanceData.put("timeIn", now);
            attendanceData.put("manualEntry", false);
            attendanceData.put("eventName", eventName);
            attendanceData.put("eventStatus", eventStatus);
            attendanceData.put("eventDate", eventDate);

            firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId)
                    .set(attendanceData);

            // Set on user-side
            Map<String, Object> userSide = new HashMap<>();
            userSide.put("eventId", eventId);
            userSide.put("timeIn", now);
            userSide.put("manualEntry", false);
            userSide.put("eventName", eventName);
            userSide.put("eventStatus", eventStatus);
            userSide.put("eventDate", eventDate);

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

            // Fetch event details
            DocumentSnapshot eventDoc = firestore.collection("events").document(eventId).get().get();

            if (!eventDoc.exists()) {
                return "Event does not exist: " + eventId;
            }

            String eventName = eventDoc.getString("eventName");
            String eventStatus = eventDoc.getString("status");
            Object eventDate = eventDoc.get("date");

            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("timeOut", now);
            updateMap.put("manualEntry", false);
            updateMap.put("eventName", eventName);
            updateMap.put("eventStatus", eventStatus);
            updateMap.put("eventDate", eventDate);

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

    
    public String manualTimeIn(String eventId, String userId) {
        try {
            String now = Instant.now().toString();
            
            // Check if attendee record exists
            DocumentReference attendeeRef = firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId);
            
            ApiFuture<DocumentSnapshot> future = attendeeRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists() && document.getBoolean("attended") != null && document.getBoolean("attended")) {
                return "User has already timed in for this event. Certificate has already been generated.";
            }
            
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("attended", true);
            attendanceData.put("timeIn", now);
            attendanceData.put("manualEntry", true);
            
            if (document.exists()) {
                // Update existing record
                attendeeRef.update(attendanceData);
            } else {
                // Create new record
                attendeeRef.set(attendanceData);
            }
            
            // Update user-side record
            DocumentReference userEventRef = firestore.collection("users")
                    .document(userId)
                    .collection("attendedEvents")
                    .document(eventId);
            
            Map<String, Object> userSide = new HashMap<>();
            userSide.put("eventId", eventId);
            userSide.put("timeIn", now);
            userSide.put("manualEntry", true);
            
            ApiFuture<DocumentSnapshot> userFuture = userEventRef.get();
            DocumentSnapshot userDocument = userFuture.get();
            
            if (userDocument.exists()) {
                userEventRef.update(userSide);
            } else {
                userEventRef.set(userSide);
            }
            
            return "Manual time-in recorded for user " + userId + " at event " + eventId;
            
        } catch (Exception e) {
            return "Failed to mark manual time-in: " + e.getMessage();
        }
    }
    
    public String manualTimeOut(String eventId, String userId) {
        try {
            String now = Instant.now().toString();
            
            // Check if attendee record exists
            DocumentReference attendeeRef = firestore.collection("events")
                    .document(eventId)
                    .collection("attendees")
                    .document(userId);
            
            ApiFuture<DocumentSnapshot> future = attendeeRef.get();
            DocumentSnapshot document = future.get();
            
            if (!document.exists()) {
                return "Cannot time-out: No attendance record found for user " + userId;
            }
            
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("timeOut", now);
            updateMap.put("manualEntry", true);
            
            attendeeRef.update(updateMap);
            
            // Update user-side record
            DocumentReference userEventRef = firestore.collection("users")
                    .document(userId)
                    .collection("attendedEvents")
                    .document(eventId);
            
            ApiFuture<DocumentSnapshot> userFuture = userEventRef.get();
            DocumentSnapshot userDocument = userFuture.get();
            
            if (userDocument.exists()) {
                userEventRef.update(updateMap);
            }
            
            return "Manual time-out recorded for user " + userId + " at event " + eventId;
            
        } catch (Exception e) {
            return "Failed to mark manual time-out: " + e.getMessage();
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
                Boolean manualEntry = doc.getBoolean("manualEntry");

                // Fetch user details
                DocumentReference userRef = firestore.collection("users").document(userId);
                ApiFuture<DocumentSnapshot> userQuery = userRef.get();
                DocumentSnapshot userDoc = userQuery.get();

                if (userDoc.exists()) {
                    Map<String, String> userDetails = new HashMap<>();
                    userDetails.put("userId", userId);
                    userDetails.put("timeIn", timeIn);
                    userDetails.put("timeOut", timeOut != null ? timeOut : "N/A");
                    userDetails.put("manualEntry", manualEntry != null && manualEntry ? "true" : "false");

                    // Safely retrieve string fields, add fallback values if they are null
                    userDetails.put("email", Optional.ofNullable(userDoc.getString("email")).orElse("N/A"));
                    userDetails.put("firstName", Optional.ofNullable(userDoc.getString("firstName")).orElse("N/A"));
                    userDetails.put("lastName", Optional.ofNullable(userDoc.getString("lastName")).orElse("N/A"));

                    // Enhanced department handling
                    String departmentId = userDoc.getString("departmentId");
                    if (departmentId != null && !departmentId.isEmpty()) {
                        // Fetch department details
                        DocumentReference deptRef = firestore.collection("departments").document(departmentId);
                        ApiFuture<DocumentSnapshot> deptQuery = deptRef.get();
                        DocumentSnapshot deptDoc = deptQuery.get();
                        
                        if (deptDoc.exists()) {
                            String deptName = deptDoc.getString("name");
                            userDetails.put("department", deptName != null ? deptName : "N/A");
                        } else {
                            // Handle department as object directly in user document
                            Object departmentObj = userDoc.get("department");
                            if (departmentObj instanceof Map) {
                                Map<String, Object> department = (Map<String, Object>) departmentObj;
                                String departmentName = (String) department.get("name");
                                userDetails.put("department", departmentName != null ? departmentName : "N/A");
                            } else if (departmentObj instanceof String) {
                                String departmentName = (String) departmentObj;
                                userDetails.put("department", !departmentName.isEmpty() ? departmentName : "N/A");
                            } else {
                                userDetails.put("department", "N/A");
                            }
                        }
                    } else {
                        // Handle department as object directly in user document
                        Object departmentObj = userDoc.get("department");
                        if (departmentObj instanceof Map) {
                            Map<String, Object> department = (Map<String, Object>) departmentObj;
                            String departmentName = (String) department.get("name");
                            userDetails.put("department", departmentName != null ? departmentName : "N/A");
                        } else if (departmentObj instanceof String) {
                            String departmentName = (String) departmentObj;
                            userDetails.put("department", !departmentName.isEmpty() ? departmentName : "N/A");
                        } else {
                            userDetails.put("department", "N/A");
                        }
                    }

                    attendees.add(userDetails);
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching attendees: " + e.getMessage());
        }

        return attendees;
    }
    
    public List<Map<String, Object>> getUserAttendedEvents(String userId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        CollectionReference attendedEventsRef = db
                .collection("users")
                .document(userId)
                .collection("attendedEvents");

        ApiFuture<QuerySnapshot> future = attendedEventsRef.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Map<String, Object>> attendedEvents = new ArrayList<>();

        for (QueryDocumentSnapshot doc : documents) {
            Map<String, Object> eventData = new HashMap<>(doc.getData());

            // Optionally fetch main event details too
            String eventId = (String) eventData.get("eventId");
            DocumentSnapshot eventDoc = db.collection("events").document(eventId).get().get();

            if (eventDoc.exists()) {
                eventData.put("eventName", eventDoc.getString("eventName"));
                eventData.put("eventDate", eventDoc.get("date"));
                eventData.put("eventStatus", eventDoc.getString("status"));
            }

            attendedEvents.add(eventData);
        }

        return attendedEvents;
    }
}
