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
    @PutMapping("/update/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String eventId,
            @RequestBody Event updatedEvent
    ) {
        try {
            Event event = eventService.updateEvent(eventId, updatedEvent);
            return ResponseEntity.ok(event);  // Returning the updated event with department info
        } catch (RuntimeException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body("Failed to update event: " + e.getMessage());
        }
    }

}
