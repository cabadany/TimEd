package com.example.timed_mobile

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import android.util.Log
import android.widget.Toast
import java.util.*

class AttendanceSheetActivity : AppCompatActivity() {

    private lateinit var dailyLogContainer: LinearLayout
    private lateinit var eventLogContainer: LinearLayout
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDB = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.attendance_sheet)

        dailyLogContainer = findViewById(R.id.daily_log_container)
        eventLogContainer = findViewById(R.id.event_log_container)

        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)

        if (userId != null) {
            loadDailyLogs(userId)
            loadEventLogs(userId)
        }
    }

    private fun loadDailyLogs(userId: String) {
        realtimeDB.child("timeLogs").child(userId)
            .orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logsByDay = mutableMapOf<String, MutableList<DataSnapshot>>()

                    for (child in snapshot.children) {
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

                        logsByDay.getOrPut(dayKey) { mutableListOf() }.add(child)
                    }

                    for ((date, logs) in logsByDay) {
                        val view = TextView(this@AttendanceSheetActivity)
                        val timeIn = logs.find { it.child("type").value == "TimeIn" }
                        val timeOut = logs.find { it.child("type").value == "TimeOut" }
                        val displayText = "📅 $date\n- Time In: ${formatTimestamp(timeIn)}\n- Time Out: ${formatTimestamp(timeOut)}"
                        view.text = displayText
                        view.setPadding(16, 16, 16, 16)
                        dailyLogContainer.addView(view)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadEventLogs(userId: String) {
        firestore.collection("events")
            .get()
            .addOnSuccessListener { eventSnapshots ->
                for (eventDoc in eventSnapshots) {
                    val eventId = eventDoc.id
                    val eventTitle = eventDoc.getString("eventName") ?: "Untitled Event"

                    firestore.collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)  // exact casing!
                        .get()
                        .addOnSuccessListener { attendeesSnapshot ->
                            for (attendeeDoc in attendeesSnapshot) {
                                val type = attendeeDoc.getString("type") ?: ""
                                val timestampString = attendeeDoc.getString("timestamp")

                                if (type == "event_time_in" && timestampString != null) {
                                    val view = TextView(this)
                                    view.text = "📌 $eventTitle\n- Time In: $timestampString\n- Time Out: —"
                                    view.setPadding(16, 16, 16, 16)
                                    eventLogContainer.addView(view)
                                }
                            }
                        }
                        .addOnFailureListener {
                            Log.e("AttendanceSheet", "Failed to load attendees for $eventId: ${it.message}")
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatTimestamp(snapshot: DataSnapshot?): String {
        val timestamp = snapshot?.child("timestamp")?.getValue(Long::class.java)
        return if (timestamp != null) dateFormat.format(Date(timestamp)) else "—"
    }

    private fun formatTime(time: Long?): String {
        return if (time != null && time != 0L) dateFormat.format(Date(time)) else "—"
    }
}