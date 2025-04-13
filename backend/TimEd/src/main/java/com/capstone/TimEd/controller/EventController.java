package com.capstone.TimEd.controller;

import com.capstone.TimEd.model.Event;
import com.capstone.TimEd.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping("/createEvent")
    public String saveEvent(@RequestBody Event event) throws ExecutionException, InterruptedException {
        return eventService.saveEvent(event);
    }

    @GetMapping("/get/{id}")
    public Event getEvent(@PathVariable String id) throws ExecutionException, InterruptedException {
        return eventService.getEventById(id);
    }

    @GetMapping("/getAll")
    public List<Event> getAllEvents() throws ExecutionException, InterruptedException {
        return eventService.getAllEvents();
    }

    @DeleteMapping("/deleteEvent/{id}")
    public String deleteEvent(@PathVariable String id) throws ExecutionException, InterruptedException {
        return eventService.deleteEvent(id);
    }
    
    @PutMapping("/updateEvents/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable("id") String eventId, @RequestBody Event updatedEvent) {
        try {
            Event event = eventService.updateEvent(eventId, updatedEvent);
            return ResponseEntity.ok(event);
        } catch (RuntimeException | ExecutionException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
