package com.capstone.TimEd.controller;

import com.capstone.TimEd.dto.Eventdto;
import com.capstone.TimEd.model.Department;
import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.service.EventService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/events")
public class EventController {
	 @Autowired
	    public EventController(EventService eventService) {
	        this.eventService = eventService;
	    }
    @Autowired private final Firestore firestore = FirestoreClient.getFirestore();
    private EventService eventService;

    @PostMapping("/createEvent")
    public String saveEvent(@RequestBody Event event) throws ExecutionException, InterruptedException {
        return eventService.addEvent(event);
    }


    @GetMapping("/{id}/department")
    public Department getDepartmentForEvent(@PathVariable("id") String eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            return event.getDepartment(); // Return the department object for the event
        } catch (Exception e) {
            // Handle error (maybe log and return 404)
            return null;
        }
    }

    @GetMapping("/getAll")
    public List<Eventdto> getAllEvents() {
        try {
            return eventService.getAllEvents();  // Returns a list of Eventdto, not Event
        } catch (Exception e) {
            // Log error and return an empty list or handle error as appropriate
            System.err.println("Error fetching all events: " + e.getMessage());
            return new ArrayList<>(); // Return an empty list in case of an error
        }
    }
    @DeleteMapping("/deleteEvent/{id}")
    public String deleteEvent(@PathVariable String id) throws ExecutionException, InterruptedException {
        return eventService.deleteEvent(id);
    }
    
 // Method to update an event's details and return the updated event
    public Event updateEvent(String eventId, Event updatedEvent) throws ExecutionException, InterruptedException {
        try {
            // Update the event document with new values from updatedEvent object
            Map<String, Object> updateMap = new HashMap<>();
            
            // Here you can add any fields that can be updated
            updateMap.put("eventName", updatedEvent.getEventName());
            updateMap.put("status", updatedEvent.getStatus());
            updateMap.put("date", updatedEvent.getDate());
            updateMap.put("duration", updatedEvent.getDuration());
            updateMap.put("departmentId", updatedEvent.getDepartmentId());
            
            // Apply updates in Firestore
            firestore.collection("events")
                    .document(eventId)
                    .update(updateMap);
            
            // Return the updated event from Firestore
            DocumentReference eventRef = firestore.collection("events").document(eventId);
            ApiFuture<DocumentSnapshot> future = eventRef.get();
            DocumentSnapshot document = future.get();
            
            if (document.exists()) {
                return document.toObject(Event.class);  // Return updated event
            } else {
                throw new RuntimeException("Event not found");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to update event: " + e.getMessage());
        }
    }

}
