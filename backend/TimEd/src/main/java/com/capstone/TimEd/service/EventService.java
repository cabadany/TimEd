package com.capstone.TimEd.service;

import com.capstone.TimEd.dto.Eventdto;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.model.Certificate;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.Random;

@Service
public class EventService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final FirebaseApp firebaseApp;
    private final CollectionReference eventsCollection = firestore.collection("events"); // Correct reference to 'events' collection
    private final CollectionReference departmentsCollection = firestore.collection("departments"); // Correct reference to 'departments' collection
    
    private final CertificateService certificateService;
    
    @Autowired
    public EventService(FirebaseApp firebaseApp, CertificateService certificateService) {
        this.firebaseApp = firebaseApp;
        this.certificateService = certificateService;
        // Initialize FirebaseApp before using Firestore
    }

    // Method to get Events by departmentId
    public List<Event> getEventsByDepartmentId(String departmentId) throws ExecutionException, InterruptedException {
        List<Event> events = new ArrayList<>();

        try {
            CollectionReference eventsCollection = firestore.collection("events");

            // Query to get events by departmentId
            Query query = eventsCollection.whereEqualTo("departmentId", departmentId);

            // Execute the query and get the ApiFuture result
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            // Loop through documents and map to Event model
            for (QueryDocumentSnapshot doc : documents) {
                Event event = doc.toObject(Event.class);
                events.add(event);
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching events: " + e.getMessage());
        }

        return events; // Return list of events
    }

    public List<Eventdto> getEventDtosByDepartmentId(String departmentId) throws ExecutionException, InterruptedException {
        List<Eventdto> eventDtos = new ArrayList<>();

        List<Event> events = getEventsByDepartmentId(departmentId);

        // Check if events are found
        if (events != null && !events.isEmpty()) {
            // Create a SimpleDateFormat to format the date as a String with Philippines timezone
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Set to Philippines timezone
            
            for (Event event : events) {
                String formattedDate = dateFormat.format(event.getDate()); // Convert Date to String (yyyy-MM-dd'T'HH:mm:ss)
                
                Eventdto eventDto = new Eventdto(
                        event.getEventId(),        // Event ID
                        event.getEventName(),      // Event Name
                        event.getStatus(),         // Event Status (Upcoming, Completed, etc.)
                        formattedDate,             // Event Date (formatted as String)
                        event.getDuration(),       // Event Duration (HH:mm:ss)
                        event.getDepartmentId(),    // Department ID (linked department)
                        event.getDescription(),
                        event.getVenue()
                );
                eventDtos.add(eventDto);  // Add the Eventdto to the list
            }
        } else {
            // Handle the case where no events are found for the given departmentId
            System.out.println("No events found for departmentId: " + departmentId);
        }

        // Return the list of EventDTOs
        return eventDtos;
    }
    

    public Department getDepartmentById(String departmentId) {
        try {
            DocumentSnapshot docSnapshot = departmentsCollection.document(departmentId).get().get();
            if (docSnapshot.exists()) {
                return docSnapshot.toObject(Department.class);  // Return the Department object
            } else {
                return null;  // Return null if the department doesn't exist
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching department: " + e.getMessage());
            return null;
        }
    }
    public String deleteEvent(String eventId) {
        try {
            // First, try to find and delete the associated certificate
            System.out.println("[DEBUG] Starting event deletion for eventId: " + eventId);
            int certificatesDeleted = 0;
            
            try {
                // Query all certificates to find any with this eventId
                List<QueryDocumentSnapshot> certificates = firestore.collection("certificates")
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .get()
                    .getDocuments();
                
                System.out.println("[DEBUG] Found " + certificates.size() + " certificates with eventId: " + eventId);
                
                // Delete each certificate found
                for (QueryDocumentSnapshot cert : certificates) {
                    Certificate certificate = cert.toObject(Certificate.class);
                    certificateService.deleteCertificate(certificate.getId());
                    System.out.println("[DEBUG] Deleted certificate with ID: " + certificate.getId() + " for event: " + eventId);
                    certificatesDeleted++;
                }
                
                // Also try standard certificate lookup if none found above
                if (certificatesDeleted == 0) {
                    Certificate certificate = certificateService.getCertificateByEventId(eventId);
                    if (certificate != null) {
                        certificateService.deleteCertificate(certificate.getId());
                        System.out.println("[DEBUG] Deleted certificate with ID: " + certificate.getId() + " for event: " + eventId);
                        certificatesDeleted++;
                    }
                }
            } catch (Exception e) {
                // Log the exception but continue deleting the event
                System.err.println("[ERROR] Error deleting associated certificate: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Get the event document reference
            DocumentReference eventRef = firestore.collection("events").document(eventId);
            
            // Use batch write for better performance and atomicity
            WriteBatch batch = firestore.batch();
            
            // 1. Delete all attendees records for this event
            CollectionReference attendeesRef = eventRef.collection("attendees");
            ApiFuture<QuerySnapshot> attendeesFuture = attendeesRef.get();
            List<QueryDocumentSnapshot> attendees = attendeesFuture.get().getDocuments();
            
            System.out.println("[DEBUG] Found " + attendees.size() + " attendees to delete for event: " + eventId);
            
            // Add delete operations for all attendees
            for (QueryDocumentSnapshot attendee : attendees) {
                batch.delete(attendee.getReference());
                
                // Also remove this event from the user's attendedEvents collection
                String userId = attendee.getId();
                DocumentReference userEventRef = firestore.collection("users")
                    .document(userId)
                    .collection("attendedEvents")
                    .document(eventId);
                
                batch.delete(userEventRef);
            }
            
            // 2. Delete the event document itself
            batch.delete(eventRef);
            
            // Commit all delete operations
            batch.commit().get();
            
            System.out.println("[DEBUG] Event and all associated data deleted successfully");
            
            if (certificatesDeleted > 0) {
                return "Event, " + attendees.size() + " attendee records, and " + certificatesDeleted + " associated certificate(s) deleted successfully";
            } else {
                return "Event and " + attendees.size() + " attendee records deleted successfully (no certificates found)";
            }
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to delete event: " + e.getMessage());
            e.printStackTrace();
            return "Failed to delete event: " + e.getMessage();
        }
    }

    // Method to update an event's certificateId
    public void updateEventCertificateId(String eventId, String certificateId) throws ExecutionException, InterruptedException {
        try {
            // Update only the certificateId field
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("certificateId", certificateId);
            
            // Log the operation for debugging
            System.out.println("Updating event " + eventId + " with certificateId " + certificateId);
            
            // Update the event in Firestore
            WriteResult result = firestore.collection("events")
                    .document(eventId)
                    .update(updateMap)
                    .get();
            
            System.out.println("Event updated at: " + result.getUpdateTime());
            
        } catch (Exception e) {
            System.err.println("Error updating event certificateId: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Method to update an event's details and return the updated event
    public Event updateEvent(String eventId, Event updatedEvent) throws ExecutionException, InterruptedException {
        try {
            // Get the department object from departmentId
            DocumentReference departmentRef = firestore.collection("departments").document(updatedEvent.getDepartmentId());
            ApiFuture<DocumentSnapshot> departmentFuture = departmentRef.get();
            DocumentSnapshot departmentSnapshot = departmentFuture.get();

            if (!departmentSnapshot.exists()) {
                throw new RuntimeException("Department not found");
            }

            // Create a department object from the snapshot data
            Department department = departmentSnapshot.toObject(Department.class);

            // Update event with new values
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("eventName", updatedEvent.getEventName());
            updateMap.put("status", updatedEvent.getStatus());
            updateMap.put("date", updatedEvent.getDate());
            updateMap.put("duration", updatedEvent.getDuration());
            updateMap.put("departmentId", updatedEvent.getDepartmentId());
            updateMap.put("description", updatedEvent.getDescription());
            updateMap.put("venue", updatedEvent.getVenue());
            // Also update certificateId if it exists
            if (updatedEvent.getCertificateId() != null) {
                updateMap.put("certificateId", updatedEvent.getCertificateId());
            }

            // Apply the update in Firestore
            firestore.collection("events")
                    .document(eventId)
                    .update(updateMap);

            // Return updated event with department information
            updatedEvent.setDepartment(department);  // Set the full department object to the event
            return updatedEvent;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update event: " + e.getMessage());
        }
    }

    // Method to add a new event
    public String addEvent(Event event) {
        try {
            // Log the received date for debugging
            System.out.println("Received event date: " + event.getDate());

            // Preserve the time exactly as received from frontend
            if (event.getDate() != null) {
                // Get the timezone from the server
                TimeZone serverTimezone = TimeZone.getDefault();
                System.out.println("Server timezone: " + serverTimezone.getID());
                
                // Format with Philippines timezone for verification
                SimpleDateFormat phFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                phFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                System.out.println("Event date formatted as Philippines time: " + phFormat.format(event.getDate()));
                
                // Also log UTC format for comparison
                SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                System.out.println("Event date in UTC: " + utcFormat.format(event.getDate()));
            }
            
            // Generate a 6-character event ID (alphanumeric)
            String eventId = generateShortEventId();
            event.setEventId(eventId);
            
            // Add event to Firestore with the generated ID
            DocumentReference eventRef = firestore.collection("events").document(eventId);
            ApiFuture<WriteResult> result = eventRef.set(event);
            result.get(); // Wait for write to complete
            
            System.out.println("Event added successfully with ID: " + eventId);

            // Return just the ID
            return eventId;

        } catch (Exception e) {
            System.err.println("Error adding event: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add event: " + e.getMessage());
        }
    }

    // Helper method to generate a 6-character event ID
    private String generateShortEventId() throws ExecutionException, InterruptedException {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        
        while (true) {
            StringBuilder sb = new StringBuilder();
            // Generate a 6-character ID
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            
            String eventId = sb.toString();
            
            // Check if this ID already exists
            DocumentSnapshot doc = firestore.collection("events").document(eventId).get().get();
            if (!doc.exists()) {
                return eventId; // Return if ID is unique
            }
            // If ID exists, loop will continue and generate a new one
        }
    }

    // Method to update an event's status
    public String updateEventStatus(String eventId, String status) {
        try {
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("status", status);

            firestore.collection("events")
                    .document(eventId)
                    .update(updateMap);

            return "Event status updated successfully";

        } catch (Exception e) {
            return "Failed to update event status: " + e.getMessage();
        }
    }

    public Event getEventById(String eventId) throws Exception {
        // Fetch event by eventId
        DocumentReference eventRef = eventsCollection.document(eventId);
        DocumentSnapshot eventSnapshot = eventRef.get().get();

        if (!eventSnapshot.exists()) {
            throw new Exception("Event not found!");
        }

        Event event = eventSnapshot.toObject(Event.class); // Convert to Event object

        // If event is found, fetch its department
        if (event != null && event.getDepartmentId() != null) {
            Department department = getDepartmentById(event.getDepartmentId());
            event.setDepartment(department); // Set the department in the event
        }

        return event;
    }
    
    

    public List<Eventdto> getAllEvents() throws ExecutionException, InterruptedException {
        List<Eventdto> eventDtos = new ArrayList<>();

        try {
            // Fetch all events from Firestore
            ApiFuture<QuerySnapshot> querySnapshot = eventsCollection.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            // Iterate over each event document
            for (QueryDocumentSnapshot doc : documents) {
                // Convert the document to an Event object
                Event event = doc.toObject(Event.class);

                // Fetch the department for the event using the departmentId
                Department department = getDepartmentById(event.getDepartmentId());

                // Set the department object
                event.setDepartment(department);

                // Format the date with Philippines timezone
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Set to Philippines timezone
                String formattedDate = dateFormat.format(event.getDate());

                // Create an EventDTO
                Eventdto eventDto = new Eventdto(
                    event.getEventId(),
                    event.getEventName(),
                    event.getStatus(),
                    formattedDate,
                    event.getDuration(),
                    event.getDepartmentId(),    
                    event.getDescription(),
                    event.getDepartment() != null ? event.getDepartment().getName() : "Unknown Department",  // Use department name,
                    event.getVenue()
                );

                eventDtos.add(eventDto);  // Add the event DTO to the list
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching all events: " + e.getMessage());
        }

        return eventDtos;  // Return the list of EventDTOs
    }

    public List<Eventdto> getEventsByDateRange(String startDateStr, String endDateStr) throws ExecutionException, InterruptedException {
        List<Eventdto> eventDtos = new ArrayList<>();
        
        try {
            // Parse input dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date startDate = null;
            Date endDate = null;
            
            if (startDateStr != null && !startDateStr.isEmpty()) {
                startDate = dateFormat.parse(startDateStr);
            }
            
            if (endDateStr != null && !endDateStr.isEmpty()) {
                endDate = dateFormat.parse(endDateStr);
                // Set time to end of day for the end date
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDate);
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                endDate = calendar.getTime();
            }
            
            // Fetch all events if no date filters are provided
            if (startDate == null && endDate == null) {
                return getAllEvents();
            }
            
            // Create query based on date range
            Query query = eventsCollection;
            
            if (startDate != null) {
                query = query.whereGreaterThanOrEqualTo("date", startDate);
            }
            
            if (endDate != null) {
                query = query.whereLessThanOrEqualTo("date", endDate);
            }
            
            // Execute query
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            
            // Process results
            for (QueryDocumentSnapshot doc : documents) {
                Event event = doc.toObject(Event.class);
                
                // Get department for the event
                Department department = getDepartmentById(event.getDepartmentId());
                event.setDepartment(department);
                
                // Format date for DTO with Philippines timezone
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Set to Philippines timezone
                String formattedDate = outputFormat.format(event.getDate());
                
                // Create DTO
                Eventdto eventDto = new Eventdto(
                    event.getEventId(),
                    event.getEventName(),
                    event.getStatus(),
                    formattedDate,
                    event.getDuration(),
                    event.getDepartmentId(),
                    event.getDepartment() != null ? event.getDepartment().getName() : "Unknown Department",
                    event.getDescription(),
                    event.getVenue()
                );
                
                eventDtos.add(eventDto);
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing dates: " + e.getMessage());
            // Return all events if date parsing fails
            return getAllEvents();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching events by date range: " + e.getMessage());
        }
        
        return eventDtos;
    }

    public List<Eventdto> getPaginatedEvents(int page, int size) throws ExecutionException, InterruptedException {
        List<Eventdto> eventDtos = new ArrayList<>();
        try {
            // Create query to get events sorted by date (most recent first)
            Query query = firestore.collection("events").orderBy("date", Query.Direction.DESCENDING);
            
            // Execute query to get all document IDs first (lighter operation)
            ApiFuture<QuerySnapshot> countQuery = query.get();
            List<QueryDocumentSnapshot> allDocs = countQuery.get().getDocuments();
            
            // Calculate pagination details
            int totalCount = allDocs.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalCount);
            
            // If requested page is out of bounds, return empty list
            if (startIndex >= totalCount) {
                return new ArrayList<>();
            }
            
            // Get only the IDs of documents we need for this page
            List<String> pageDocIds = new ArrayList<>();
            for (int i = startIndex; i < endIndex; i++) {
                pageDocIds.add(allDocs.get(i).getId());
            }
            
            // Batch fetch only the events we need by ID
            List<ApiFuture<DocumentSnapshot>> futures = new ArrayList<>();
            for (String docId : pageDocIds) {
                futures.add(firestore.collection("events").document(docId).get());
            }
            
            // Process only the events for this page
            for (ApiFuture<DocumentSnapshot> future : futures) {
                DocumentSnapshot doc = future.get();
                if (doc.exists()) {
                    Event event = doc.toObject(Event.class);
                    
                    // Set document ID as eventId if not set
                    if (event.getEventId() == null) {
                        event.setEventId(doc.getId());
                    }
                    
                    // Format the date with Philippines timezone
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Manila")); // Set to Philippines timezone
                    String formattedDate = event.getDate() != null ? dateFormat.format(event.getDate()) : "";
                    
                    // Create EventDTO with minimal department info (just department ID)
                    // Avoid fetching full department details for better performance
                    Eventdto eventDto = new Eventdto(
                        event.getEventId(),
                        event.getEventName(),
                        event.getStatus(),
                        formattedDate,
                        event.getDuration(),
                        event.getDepartmentId(),
                        event.getDepartment() != null ? event.getDepartment().getName() : "Unknown Department",
                        event.getDescription(),
                        event.getVenue()
                    );
                    
                    eventDtos.add(eventDto);
                }
            }
            
            return eventDtos;
        } catch (Exception e) {
            System.err.println("Error in paginated events: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public int getTotalEventCount() throws ExecutionException, InterruptedException {
        try {
            // Get all events and count them
            // For Firestore, this is not ideal for large collections, but works for this use case
            ApiFuture<QuerySnapshot> future = firestore.collection("events").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.size();
        } catch (Exception e) {
            System.err.println("Error counting events: " + e.getMessage());
            return 0;
        }
    }

}
