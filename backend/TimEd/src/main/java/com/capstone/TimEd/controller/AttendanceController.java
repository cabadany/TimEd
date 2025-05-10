package com.capstone.TimEd.controller;

import com.capstone.TimEd.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/{eventId}/{userId}")
    public ResponseEntity<String> markAttendance(
            @PathVariable String eventId,
            @PathVariable String userId) {
        try {
            String result = attendanceService.markAttendance(eventId, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error marking attendance: " + e.getMessage());
        }
    }
    @PostMapping("/{eventId}/{userId}/timeout")
    public ResponseEntity<String> markTimeOut(
            @PathVariable String eventId,
            @PathVariable String userId) {
        String result = attendanceService.markTimeOut(eventId, userId);
        return ResponseEntity.ok(result);
    }
    
    
    
    
    @PostMapping("/{eventId}/{userId}/manual/{actionType}")
    public ResponseEntity<String> manualAttendanceAction(
            @PathVariable String eventId,
            @PathVariable String userId,
            @PathVariable String actionType) {
        try {
            String result;
            if ("timein".equalsIgnoreCase(actionType)) {
                result = attendanceService.manualTimeIn(eventId, userId);
            } else if ("timeout".equalsIgnoreCase(actionType)) {
                result = attendanceService.manualTimeOut(eventId, userId);
            } else {
                return ResponseEntity.badRequest().body("Invalid action type. Use 'timein' or 'timeout'");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Error processing manual attendance action: " + e.getMessage());
        }
    }

    @GetMapping("/{eventId}/attendees")
    public ResponseEntity<List<Map<String, String>>> getAttendees(@PathVariable String eventId) {
        try {
            List<Map<String, String>> attendees = attendanceService.getAttendees(eventId);
            
            if (attendees.isEmpty()) {
                return ResponseEntity.status(404).body(null);  // If no attendees found, return 404
            }

            return ResponseEntity.ok(attendees); // Return the list of attendees
        } catch (Exception e) {
            // Log the error for debugging purposes (you can use a logger here)
            System.err.println("Error fetching attendees: " + e.getMessage());
            
            // Return a 500 Internal Server Error response
            return ResponseEntity
                    .status(500)
                    .body(Collections.emptyList());  // Return an empty list if error occurs
        }
    }
    
    @GetMapping("/user/{userId}/attended-events")
    public ResponseEntity<List<Map<String, Object>>> getUserAttendedEvents(@PathVariable String userId) {
        try {
            List<Map<String, Object>> events = attendanceService.getUserAttendedEvents(userId);

            if (events.isEmpty()) {
                return ResponseEntity.status(404).body(null);
            }

            return ResponseEntity.ok(events);
        } catch (Exception e) {
            System.err.println("Error fetching attended events: " + e.getMessage());
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }
}
