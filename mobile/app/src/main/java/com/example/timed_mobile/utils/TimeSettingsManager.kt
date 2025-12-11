package com.example.timed_mobile.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimeSettingsManager {
    private const val TAG = "TimeSettingsManager"

    // Default values
    private var startTimeStr: String = "07:00"
    private var endTimeStr: String = "17:00"
    private var lateThresholdStr: String = "09:00"
    
    // Hardcoded break window for now (as per plan)
    private const val BREAK_START_STR = "12:00"
    private const val BREAK_END_STR = "13:00"

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        fetchTimeSettings()
    }

    private fun fetchTimeSettings() {
        val db = FirebaseDatabase.getInstance()
        val settingsRef = db.getReference("settings")

        // Listen for Time Window
        settingsRef.child("timeWindow").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val start = snapshot.child("start").getValue(String::class.java)
                val end = snapshot.child("end").getValue(String::class.java)
                
                if (start != null) startTimeStr = start
                if (end != null) endTimeStr = end
                
                Log.d(TAG, "Time Window updated: $startTimeStr - $endTimeStr")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load time window", error.toException())
            }
        })

        // Listen for Late Threshold
        settingsRef.child("lateThreshold").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val threshold = snapshot.getValue(String::class.java)
                if (threshold != null) {
                    lateThresholdStr = threshold
                    Log.d(TAG, "Late Threshold updated: $lateThresholdStr")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load late threshold", error.toException())
            }
        })
    }

    fun isTimeInAllowed(): Boolean {
        val now = Calendar.getInstance()
        val startTime = parseTime(startTimeStr)
        
        // Scenario A: Too Early
        if (now.before(startTime)) {
            Log.d(TAG, "Time In Blocked: Too Early (Now: ${formatTime(now)}, Start: $startTimeStr)")
            return false
        }

        // Scenario B: Too Late -> ALLOWED (User request)
        // We do not check if now.after(endTime) for blocking purposes.

        // Break Window Check
        if (isInBreak()) {
            Log.d(TAG, "Time In Blocked: In Break Window")
            return false
        }

        return true
    }

    fun isTooEarlyToTimeOut(): Boolean {
        val now = Calendar.getInstance()
        val endTime = parseTime(endTimeStr)

        // If now is before the allowed end time, it's too early to time out
        return now.before(endTime)
    }

    fun isLate(): Boolean {
        val now = Calendar.getInstance()
        val thresholdTime = parseTime(lateThresholdStr)
        return now.after(thresholdTime)
    }

    fun isInBreak(): Boolean {
        val now = Calendar.getInstance()
        val breakStart = parseTime(BREAK_START_STR)
        val breakEnd = parseTime(BREAK_END_STR)

        return now.after(breakStart) && now.before(breakEnd)
    }
    
    fun getStartTime(): String = convertTo12HourFormat(startTimeStr)
    fun getEndTime(): String = convertTo12HourFormat(endTimeStr)
    fun getTimeWindowString(): Pair<String, String> {
        return Pair(getStartTime(), getEndTime())
    }
    fun getBreakWindow(): String = "${convertTo12HourFormat(BREAK_START_STR)} - ${convertTo12HourFormat(BREAK_END_STR)}"

    private fun parseTime(timeStr: String): Calendar {
        val cal = Calendar.getInstance()
        try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeStr", e)
        }
        return cal
    }
    
    private fun formatTime(cal: Calendar): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(cal.time)
    }

    private fun convertTo12HourFormat(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateObj = sdf24.parse(time24)
            val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf12.format(dateObj!!)
        } catch (e: Exception) {
            time24 // Fallback
        }
    }
}
