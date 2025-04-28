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
}
