package com.capstone.TimEd.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
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
            CollectionReference attendeesRef = firestore.collection("events")
                .document(eventId)
                .collection("attendees");
            
            ApiFuture<QuerySnapshot> existingQuery = attendeesRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "event_time_in")
                .get();
            
            List<QueryDocumentSnapshot> existingDocs = existingQuery.get().getDocuments();
            if (!existingDocs.isEmpty()) {
                return "Already timed in for this event. Certificate has already been generated.";
            }

            // Get event details
            DocumentReference eventRef = firestore.collection("events").document(eventId);
            ApiFuture<DocumentSnapshot> eventQuery = eventRef.get();
            DocumentSnapshot eventDoc = eventQuery.get();

            if (!eventDoc.exists()) {
                return "Event not found";
            }

            // Get user details
            DocumentReference userRef = firestore.collection("users").document(userId);
            ApiFuture<DocumentSnapshot> userQuery = userRef.get();
            DocumentSnapshot userDoc = userQuery.get();

            if (!userDoc.exists()) {
                return "User not found";
            }

            // Create attendance record
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("userId", userId);
            attendanceData.put("eventId", eventId);
            attendanceData.put("eventName", eventDoc.getString("eventName"));
            attendanceData.put("firstName", userDoc.getString("firstName"));
            attendanceData.put("email", userDoc.getString("email"));
            attendanceData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            attendanceData.put("type", "event_time_in");
            attendanceData.put("hasTimedOut", false);
            attendanceData.put("selfieUrl", null); // Can be updated later if needed

            // Add the attendance record to the event's attendees subcollection
            attendeesRef.document(userId).set(attendanceData);

            return "Attendance marked successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error marking attendance: " + e.getMessage();
        }
    }
    
    public String markTimeOut(String eventId, String userId) {
        try {
            // Find the attendance record in the event's attendees subcollection
            DocumentReference attendeeRef = firestore.collection("events")
                .document(eventId)
                .collection("attendees")
                .document(userId);
            
            DocumentSnapshot attendeeDoc = attendeeRef.get().get();
            
            if (!attendeeDoc.exists()) {
                return "No time-in record found for this event";
            }
            
            // Check if already timed out
            Boolean hasTimedOut = attendeeDoc.getBoolean("hasTimedOut");
            if (hasTimedOut != null && hasTimedOut) {
                return "Already timed out for this event";
            }
            
            // Update the attendance record with timeout information
            Map<String, Object> updates = new HashMap<>();
            updates.put("hasTimedOut", true);
            updates.put("timeOutTimestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            // Apply the updates
            attendeeRef.update(updates);
            
            return "Time-out recorded successfully";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error recording time-out: " + e.getMessage();
        }
    }

    
    public String manualTimeIn(String eventId, String userId) {
        try {
            // Check if user has already timed in for this event in the new structure
            ApiFuture<QuerySnapshot> existingQuery = firestore.collection("attendees")
                    .whereEqualTo("eventId", eventId)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "event_time_in")
                    .get();
            
            List<QueryDocumentSnapshot> existingDocs = existingQuery.get().getDocuments();
            if (!existingDocs.isEmpty()) {
                return "User has already timed in for this event. Certificate has already been generated.";
            }

            // Get event details
            DocumentSnapshot eventDoc = firestore.collection("events").document(eventId).get().get();
            if (!eventDoc.exists()) {
                return "Event does not exist: " + eventId;
            }

            String eventName = eventDoc.getString("eventName");
            
            // Get user details
            DocumentSnapshot userDoc = firestore.collection("users").document(userId).get().get();
            if (!userDoc.exists()) {
                return "User does not exist: " + userId;
            }
            
            String email = userDoc.getString("email");
            String firstName = userDoc.getString("firstName");
            
            // Create new attendance record
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("eventId", eventId);
            attendanceData.put("eventName", eventName);
            attendanceData.put("userId", userId);
            attendanceData.put("email", email);
            attendanceData.put("firstName", firstName);
            attendanceData.put("timestamp", Instant.now().toString());
            attendanceData.put("type", "event_time_in");
            attendanceData.put("hasTimedOut", false);
            attendanceData.put("selfieUrl", null);
            attendanceData.put("manualEntry", true);

            // Add to the new 'attendees' collection
            firestore.collection("attendees").add(attendanceData);

            return "Manual time-in recorded for user " + userId + " at event " + eventId;
        } catch (Exception e) {
            return "Failed to mark manual time-in: " + e.getMessage();
        }
    }
    
    public String manualTimeOut(String eventId, String userId) {
        try {
            // Find the attendance record in the new structure
            ApiFuture<QuerySnapshot> query = firestore.collection("attendees")
                    .whereEqualTo("eventId", eventId)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "event_time_in")
                    .get();
            
            List<QueryDocumentSnapshot> docs = query.get().getDocuments();
            if (docs.isEmpty()) {
                return "Cannot time-out: No attendance record found for user " + userId;
            }
            
            // Get the first matching record (there should only be one)
            DocumentSnapshot attendanceDoc = docs.get(0);
            if (attendanceDoc.getBoolean("hasTimedOut") != null && attendanceDoc.getBoolean("hasTimedOut")) {
                return "Already timed out for this event";
            }

            String now = Instant.now().toString();
            
            // Update the attendance record
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("hasTimedOut", true);
            updateData.put("timeOutTimestamp", now);
            updateData.put("manualEntry", true);
            
            firestore.collection("attendees").document(attendanceDoc.getId()).update(updateData);

            return "Manual time-out recorded for user " + userId + " at event " + eventId;
        } catch (Exception e) {
            return "Failed to mark manual time-out: " + e.getMessage();
        }
    }

    public List<Map<String, String>> getAttendees(String eventId) {
        List<Map<String, String>> attendees = new ArrayList<>();

        try {
            // First try to get attendees from the new structure (subcollection under event)
            CollectionReference eventAttendeesRef = firestore.collection("events")
                .document(eventId)
                .collection("attendees");
            
            ApiFuture<QuerySnapshot> query = eventAttendeesRef.get();
            List<QueryDocumentSnapshot> docs = query.get().getDocuments();
            
            // Process new structure attendees
            for (QueryDocumentSnapshot doc : docs) {
                Map<String, Object> data = doc.getData();
                Map<String, String> attendee = new HashMap<>();
                
                String email = data.getOrDefault("email", "").toString();
                String userId = data.getOrDefault("userId", "").toString();
                
                // Get department information
                DocumentReference userRef = firestore.collection("users").document(userId);
                DocumentSnapshot userDoc = userRef.get().get();
                String departmentId = null;
                String departmentName = "N/A";
                
                if (userDoc.exists()) {
                    departmentId = userDoc.getString("departmentId");
                    if (departmentId != null) {
                        DocumentReference deptRef = firestore.collection("departments").document(departmentId);
                        DocumentSnapshot deptDoc = deptRef.get().get();
                        if (deptDoc.exists()) {
                            departmentName = deptDoc.getString("name");
                        }
                    }
                }
                
                // Map the new data structure to the expected format
                attendee.put("userId", userId);
                attendee.put("firstName", data.getOrDefault("firstName", "").toString());
                attendee.put("lastName", data.getOrDefault("lastName", "").toString());
                attendee.put("email", email);
                attendee.put("department", departmentName);
                attendee.put("timeIn", data.getOrDefault("timestamp", "").toString());
                
                // Handle hasTimedOut and timeOut
                Object hasTimedOutObj = data.get("hasTimedOut");
                boolean hasTimedOut = hasTimedOutObj instanceof Boolean && (Boolean) hasTimedOutObj;
                attendee.put("timeOut", hasTimedOut ? data.getOrDefault("timeOutTimestamp", "").toString() : "N/A");
                
                attendee.put("type", data.getOrDefault("type", "").toString());
                Object selfieUrl = data.get("selfieUrl");
                attendee.put("selfieUrl", selfieUrl != null ? selfieUrl.toString() : null);
                
                Object manualEntryObj = data.get("manualEntry");
                attendee.put("manualEntry", manualEntryObj instanceof Boolean && (Boolean) manualEntryObj ? "true" : "false");
                
                attendees.add(attendee);
            }

            // If no attendees found in new structure, try the old structure
            if (attendees.isEmpty()) {
                CollectionReference oldAttendeesRef = firestore.collection("attendees");
                ApiFuture<QuerySnapshot> oldQuery = oldAttendeesRef
                    .whereEqualTo("eventId", eventId)
                    .get();
                
                List<QueryDocumentSnapshot> oldDocs = oldQuery.get().getDocuments();
                
                for (QueryDocumentSnapshot doc : oldDocs) {
                    Map<String, Object> data = doc.getData();
                    Map<String, String> attendee = new HashMap<>();
                    
                    String userId = data.getOrDefault("userId", "").toString();
                    
                    // Get user details including department
                    DocumentReference userRef = firestore.collection("users").document(userId);
                    DocumentSnapshot userDoc = userRef.get().get();
                    String departmentName = "N/A";
                    
                    if (userDoc.exists()) {
                        String departmentId = userDoc.getString("departmentId");
                        if (departmentId != null) {
                            DocumentReference deptRef = firestore.collection("departments").document(departmentId);
                            DocumentSnapshot deptDoc = deptRef.get().get();
                            if (deptDoc.exists()) {
                                departmentName = deptDoc.getString("name");
                            }
                        }
                    }
                    
                    attendee.put("userId", userId);
                    attendee.put("firstName", data.getOrDefault("firstName", "").toString());
                    attendee.put("lastName", data.getOrDefault("lastName", "").toString());
                    attendee.put("email", data.getOrDefault("email", "").toString());
                    attendee.put("department", departmentName);
                    attendee.put("timeIn", data.getOrDefault("timestamp", "").toString());
                    
                    Object hasTimedOutObj = data.get("hasTimedOut");
                    boolean hasTimedOut = hasTimedOutObj instanceof Boolean && (Boolean) hasTimedOutObj;
                    attendee.put("timeOut", hasTimedOut ? data.getOrDefault("timeOutTimestamp", "").toString() : "N/A");
                    
                    attendee.put("type", data.getOrDefault("type", "event_time_in").toString());
                    attendee.put("selfieUrl", data.get("selfieUrl") != null ? data.get("selfieUrl").toString() : null);
                    attendee.put("manualEntry", data.getOrDefault("manualEntry", false).toString());
                    
                    attendees.add(attendee);
                }
            }

            return attendees;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
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
