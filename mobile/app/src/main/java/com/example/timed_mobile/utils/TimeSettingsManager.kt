package com.example.timed_mobile.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeSettingsManager {
    private const val TAG = "TimeSettingsManager"
    
    // SECURITY: Force Philippine timezone (UTC+8) to prevent device time manipulation
    private val PH_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Manila")
    
    // SECURITY: Server time offset for detecting date/time manipulation
    // Positive = device is ahead of server, Negative = device is behind
    private var serverTimeOffset: Long = 0
    private var isServerTimeSynced: Boolean = false
    private const val MAX_TIME_DRIFT_MS: Long = 5 * 60 * 1000 // 5 minutes tolerance
    
    // SECURITY: Firebase real-time listener for time manipulation detection
    private var serverTimeListener: ValueEventListener? = null
    private var onTimeManipulationDetected: (() -> Unit)? = null
    private var isListenerActive: Boolean = false

    // Default values
    private var startTimeStr: String = "07:00"
    private var endTimeStr: String = "17:00"
    private var lateThresholdStr: String = "09:00"
    
    // Break Window (Fetched from Firebase)
    private var breakStartStr: String = "12:00"
    private var breakEndStr: String = "13:00"

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true
        fetchTimeSettings()
        startServerTimeListener()
        Log.d(TAG, "Initialized with Philippine timezone: ${PH_TIMEZONE.id} (UTC+8)")
    }
    
    /**
     * Starts a real-time Firebase listener on server time offset.
     * This listener is triggered automatically by Firebase when the offset changes
     * (i.e., when device time is manipulated relative to server time).
     * Much more efficient than polling!
     */
    private fun startServerTimeListener() {
        val db = FirebaseDatabase.getInstance()
        val offsetRef = db.getReference(".info/serverTimeOffset")
        
        serverTimeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newOffset = snapshot.getValue(Long::class.java) ?: 0L
                val previousOffset = serverTimeOffset
                serverTimeOffset = newOffset
                isServerTimeSynced = true
                
                // Track sync count for stability
                markSyncComplete()
                
                Log.d(TAG, "Firebase server time update. Offset: ${serverTimeOffset}ms (${serverTimeOffset / 1000}s)")
                
                // Check if this represents a significant change (time manipulation)
                // Only triggers after stable syncs (syncCount >= MIN_SYNCS_FOR_DETECTION)
                if (isListenerActive && isDeviceTimeManipulated()) {
                    Log.w(TAG, "FIREBASE LISTENER: Time manipulation detected!")
                    onTimeManipulationDetected?.invoke()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase server time listener cancelled: ${error.message}")
                isServerTimeSynced = false
            }
        }
        
        offsetRef.addValueEventListener(serverTimeListener!!)
        Log.d(TAG, "Firebase real-time server time listener STARTED")
    }
    
    /**
     * Sets the callback for time manipulation detection.
     * Call this when activity goes to foreground.
     * @param onDetected Callback triggered when time manipulation is detected
     */
    fun startTimeMonitoring(onDetected: (() -> Unit)? = null) {
        isListenerActive = true
        onTimeManipulationDetected = onDetected
        Log.d(TAG, "Time manipulation monitoring ACTIVE")
    }
    
    /**
     * Clears the callback. Call this when activity goes to background.
     */
    fun stopTimeMonitoring() {
        isListenerActive = false
        onTimeManipulationDetected = null
        Log.d(TAG, "Time manipulation monitoring INACTIVE")
    }
    
    /**
     * Resets security state for fresh verification.
     * Call this when redirecting to Splash Screen due to security violation.
     */
    fun resetSecurityState() {
        syncCount = 0
        serverTimeOffset = 0
        isServerTimeSynced = false
        isListenerActive = false
        onTimeManipulationDetected = null
        Log.w(TAG, "SECURITY STATE RESET - Fresh verification required")
    }
    
    // Keep old polling methods for backward compatibility but mark as deprecated
    @Deprecated("Use startTimeMonitoring instead - uses Firebase real-time listener")
    fun startPolling(onDetected: (() -> Unit)? = null) = startTimeMonitoring(onDetected)
    
    @Deprecated("Use stopTimeMonitoring instead")
    fun stopPolling() = stopTimeMonitoring()
    
    /**
     * Syncs with Firebase server time to detect device date/time manipulation.
     * Uses Firebase ServerValue.TIMESTAMP to get accurate server time.
     * This is called automatically on init and can be called on Activity resume.
     */
    fun syncWithServerTime() {
        val db = FirebaseDatabase.getInstance()
        val offsetRef = db.getReference(".info/serverTimeOffset")
        
        // Use addListenerForSingleValueEvent for immediate refresh when called manually
        offsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Firebase provides the offset: serverTime - deviceTime
                // So we store it and can calculate: serverTime = deviceTime + offset
                serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
                isServerTimeSynced = true
                Log.d(TAG, "Server time synced. Offset: ${serverTimeOffset}ms (${serverTimeOffset / 1000}s)")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to sync server time: ${error.message}")
                isServerTimeSynced = false
            }
        })
    }
    
    /**
     * Forces a refresh of server time sync. Call this in onResume to detect
     * device time changes made while app was in background.
     */
    fun refreshServerTimeSync() {
        Log.d(TAG, "Refreshing server time sync...")
        syncWithServerTime()
    }
    
    // Track sync count to avoid false positives on initial connection
    private var syncCount: Int = 0
    private var lastStableOffset: Long = 0
    private const val MIN_SYNCS_FOR_DETECTION: Int = 2
    
    /**
     * Checks if device time appears to be manipulated.
     * Returns true if device time differs from server time by more than 5 minutes.
     * Only triggers after stable syncs to avoid false positives.
     */
    fun isDeviceTimeManipulated(): Boolean {
        if (!isServerTimeSynced) {
            Log.w(TAG, "Server time not synced yet, cannot verify device time")
            return false // Can't determine, allow for now
        }
        
        // Don't trigger on first few syncs to allow Firebase to stabilize
        if (syncCount < MIN_SYNCS_FOR_DETECTION) {
            Log.d(TAG, "Sync count ($syncCount) below threshold ($MIN_SYNCS_FOR_DETECTION), skipping detection")
            return false
        }
        
        val absoluteOffset = abs(serverTimeOffset)
        val isManipulated = absoluteOffset > MAX_TIME_DRIFT_MS
        
        if (isManipulated) {
            Log.w(TAG, "SECURITY: Device time manipulation detected! Offset: ${absoluteOffset / 1000}s (${absoluteOffset / 60000}min)")
        }
        
        return isManipulated
    }
    
    /**
     * SECURITY CHECK: Verifies if device has automatic time enabled.
     * If AUTO_TIME is disabled, the user may be trying to manipulate time.
     * Uses Settings.Global.AUTO_TIME to check.
     * 
     * @param context Application context to access Settings
     * @return true if auto time is enabled, false if disabled (suspicious)
     */
    fun isAutoTimeEnabled(context: android.content.Context): Boolean {
        return try {
            val autoTime = android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AUTO_TIME,
                1 // Default to enabled if not found
            )
            val isEnabled = autoTime == 1
            if (!isEnabled) {
                Log.w(TAG, "SECURITY: Automatic time is DISABLED - potential manipulation!")
            }
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check AUTO_TIME setting: ${e.message}")
            true // Default to allowing if we can't check
        }
    }
    
    /**
     * Combined security check: AUTO_TIME disabled OR server time offset too large
     * @param context Application context for Settings access
     * @return true if time appears tampered, false if OK
     */
    fun isTimeTampered(context: android.content.Context): Boolean {
        // Check 1: Is auto time disabled? (Easy manipulation)
        if (!isAutoTimeEnabled(context)) {
            Log.w(TAG, "SECURITY VIOLATION: Auto time disabled")
            return true
        }
        
        // Check 2: Is server time offset too large? (Advanced manipulation)
        if (isDeviceTimeManipulated()) {
            Log.w(TAG, "SECURITY VIOLATION: Server time offset exceeded")
            return true
        }
        
        return false
    }
    
    /**
     * Returns the reason for time tampering for display in warning dialogs
     * All times shown in Philippine Time (UTC+8)
     */
    fun getTimeTamperingReason(context: android.content.Context): String {
        val currentPhTime = getCurrentPhilippineTimeFormatted()
        return when {
            !isAutoTimeEnabled(context) -> "Automatic time is disabled on your device.\n\nCurrent Philippine Time: $currentPhTime\n\nPlease enable 'Automatic date & time' in your device settings."
            isDeviceTimeManipulated() -> {
                val (deviceTime, serverTime) = getTimeMismatchDetails()
                "Your device time doesn't match Philippine Standard Time (UTC+8).\n\nYour Device: $deviceTime\nPhilippine Time: $serverTime"
            }
            else -> "Time verification failed.\n\nCurrent Philippine Time: $currentPhTime"
        }
    }
    
    /**
     * Increments sync count after successful sync
     */
    fun markSyncComplete() {
        syncCount++
        lastStableOffset = serverTimeOffset
        Log.d(TAG, "Sync complete. Count: $syncCount, Offset: ${serverTimeOffset / 1000}s")
    }
    
    /**
     * Gets the estimated server time (true Philippine time from Firebase)
     */
    fun getServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }
    
    /**
     * Returns formatted device time and server time for display in warning dialogs
     * All times formatted in Philippine Time (UTC+8)
     */
    fun getTimeMismatchDetails(): Pair<String, String> {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy h:mm:ss a", Locale.getDefault())
        dateFormat.timeZone = PH_TIMEZONE
        
        val deviceTime = System.currentTimeMillis()
        val serverTime = getServerTime()
        
        val deviceTimeStr = dateFormat.format(deviceTime)
        val serverTimeStr = dateFormat.format(serverTime)
        
        return Pair(deviceTimeStr, serverTimeStr)
    }

    // Listener for Time Window changes
    private var timeWindowChangeListener: (() -> Unit)? = null

    fun setOnTimeWindowChangeListener(listener: () -> Unit) {
        timeWindowChangeListener = listener
        // Trigger immediately with current values (defaults or fetched)
        // This ensures UI doesn't show placeholders like "--:--"
        listener()
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
                timeWindowChangeListener?.invoke()
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

        // Listen for Break Window
        settingsRef.child("breakWindow").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val start = snapshot.child("start").getValue(String::class.java)
                val end = snapshot.child("end").getValue(String::class.java)

                if (start != null) breakStartStr = start
                if (end != null) breakEndStr = end

                Log.d(TAG, "Break Window updated: $breakStartStr - $breakEndStr")
                timeWindowChangeListener?.invoke()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load break window", error.toException())
            }
        })
    }
    
    /**
     * Gets current time in Philippine timezone (Asia/Manila, UTC+8)
     * This prevents users from bypassing time checks by changing device time
     */
    fun getNowInPhilippineTime(): Calendar {
        return Calendar.getInstance(PH_TIMEZONE)
    }
    
    /**
     * Returns the Philippine timezone for use in other time operations
     */
    fun getPhilippineTimeZone(): TimeZone {
        return PH_TIMEZONE
    }
    
    /**
     * Gets today's start (midnight) in Philippine timezone
     */
    fun getTodayStartInPhilippineTime(): Long {
        return Calendar.getInstance(PH_TIMEZONE).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * Returns current Philippine time as formatted string for display in popups
     * Example: "2:30 PM (Philippine Time)"
     */
    fun getCurrentPhilippineTimeFormatted(): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = PH_TIMEZONE
        return "${sdf.format(getNowInPhilippineTime().time)} (Philippine Time)"
    }

    fun isTimeInAllowed(): Boolean {
        val now = getNowInPhilippineTime()
        val startTime = parseTime(startTimeStr)
        
        // Scenario A: Too Early
        if (now.before(startTime)) {
            Log.d(TAG, "Time In Blocked: Too Early (PH Time: ${formatTime(now)}, Start: $startTimeStr)")
            return false
        }

        // Scenario B: Too Late -> ALLOWED (User request)
        // We do not check if now.after(endTime) for blocking purposes.

        // Break Window Check - now handled separately, TimeIn during break is allowed
        // if (isInBreak()) {
        //     Log.d(TAG, "Time In Blocked: In Break Window")
        //     return false
        // }

        return true
    }

    fun isTooEarlyToTimeOut(): Boolean {
        val now = getNowInPhilippineTime()
        val endTime = parseTime(endTimeStr)

        // If now is before the allowed end time, it's too early to time out
        val tooEarly = now.before(endTime)
        Log.d(TAG, "Time Out Check: PH Time=${formatTime(now)}, EndTime=$endTimeStr, TooEarly=$tooEarly")
        return tooEarly
    }

    fun isLate(): Boolean {
        val now = getNowInPhilippineTime()
        val thresholdTime = parseTime(lateThresholdStr)
        return now.after(thresholdTime)
    }

    fun isInBreak(): Boolean {
        val now = getNowInPhilippineTime()
        val breakStart = parseTime(breakStartStr)
        val breakEnd = parseTime(breakEndStr)

        val inBreak = now.after(breakStart) && now.before(breakEnd)
        Log.d(TAG, "Break Check: PH Time=${formatTime(now)}, InBreak=$inBreak")
        return inBreak
    }
    
    fun getStartTime(): String = convertTo12HourFormat(startTimeStr)
    fun getEndTime(): String = convertTo12HourFormat(endTimeStr)
    fun getTimeWindowString(): Pair<String, String> {
        return Pair(getStartTime(), getEndTime())
    }
    fun getBreakWindow(): String = "${convertTo12HourFormat(breakStartStr)} - ${convertTo12HourFormat(breakEndStr)}"

    private fun parseTime(timeStr: String): Calendar {
        // Use Philippine timezone for parsing times
        val cal = Calendar.getInstance(PH_TIMEZONE)
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
        sdf.timeZone = PH_TIMEZONE
        return sdf.format(cal.time)
    }

    private fun convertTo12HourFormat(time24: String): String {
        return try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf24.timeZone = PH_TIMEZONE
            val dateObj = sdf24.parse(time24)
            val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
            sdf12.timeZone = PH_TIMEZONE
            sdf12.format(dateObj!!)
        } catch (e: Exception) {
            time24 // Fallback
        }
    }
}
