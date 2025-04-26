package com.capstone.TimEd.controller;

import com.capstone.TimEd.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.ok(attendees);
        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(null); // or use Optional.of(Collections.emptyList()) if needed
        }
    }
}
