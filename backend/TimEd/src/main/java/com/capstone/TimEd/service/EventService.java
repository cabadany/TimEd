package com.capstone.TimEd.service;

import com.capstone.TimEd.dto.Eventdto;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.Event;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final Firestore firestore = FirestoreClient.getFirestore();
    private final FirebaseApp firebaseApp;
    private final CollectionReference eventsCollection = firestore.collection("events"); // Correct reference to 'events' collection
    private final CollectionReference departmentsCollection = firestore.collection("departments"); // Correct reference to 'departments' collection
    public EventService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
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
            // Create a SimpleDateFormat to format the date as a String
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); // Adjust date format as needed
            for (Event event : events) {
                String formattedDate = dateFormat.format(event.getDate()); // Convert Date to String (yyyy-MM-dd)
                
                Eventdto eventDto = new Eventdto(
                        event.getEventId(),        // Event ID
                        event.getEventName(),      // Event Name
                        event.getStatus(),         // Event Status (Upcoming, Completed, etc.)
                        formattedDate,             // Event Date (formatted as String)
                        event.getDuration(),       // Event Duration (HH:mm:ss)
                        event.getDepartmentId()    // Department ID (linked department)
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
            // Delete the event from Firestore using the eventId
            firestore.collection("events")
                    .document(eventId)
                    .delete();

            return "Event deleted successfully";
            
        } catch (Exception e) {
            return "Failed to delete event: " + e.getMessage();
        }
    }// Method to update an event's details

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
            // Add event to Firestore and let Firestore generate the document ID
            DocumentReference eventRef = firestore.collection("events").add(event).get();

            // Get the auto-generated ID and set it to the event object
            event.setEventId(eventRef.getId());

            // Optionally, update the event document with the generated eventId (if you want to persist the eventId)
            eventRef.set(event);

            return "Event added successfully with ID: " + eventRef.getId();

        } catch (Exception e) {
            return "Failed to add event: " + e.getMessage();
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

                // Format the date
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = dateFormat.format(event.getDate());

                // Create an EventDTO
                Eventdto eventDto = new Eventdto(
                    event.getEventId(),
                    event.getEventName(),
                    event.getStatus(),
                    formattedDate,
                    event.getDuration(),
                    event.getDepartmentId(),
                    event.getDepartment() != null ? event.getDepartment().getName() : "Unknown Department"  // Use department name
                );

                eventDtos.add(eventDto);  // Add the event DTO to the list
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error fetching all events: " + e.getMessage());
        }

        return eventDtos;  // Return the list of EventDTOs
    }

}
