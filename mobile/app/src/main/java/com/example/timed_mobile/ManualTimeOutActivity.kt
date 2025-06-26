package com.example.timed_mobile

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ManualTimeOutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_time_out)

        val eventName = intent.getStringExtra("eventName") ?: "Unknown Event"
        val eventId = intent.getStringExtra("eventId") ?: return

        val textView = findViewById<TextView>(R.id.text_event_info)
        textView.text = "You're timing out for event:\n$eventName\n(ID: $eventId)"

        // Update Firestore
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("attendanceRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("hasTimedOut", false)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    textView.text = "You’ve already timed out for this event or it doesn’t exist."
                    return@addOnSuccessListener
                }

                val doc = documents.first()
                val recordRef = doc.reference
                // Create timestamp in Philippines timezone to match backend format
                val philippinesTimeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.timeZone = philippinesTimeZone
                val timeOutTimestamp = sdf.format(Date())

                recordRef.update(
                    mapOf(
                        "hasTimedOut" to true,
                        "timestampOut" to timeOutTimestamp
                    )
                ).addOnSuccessListener {
                    textView.text = "Successfully timed out of event: $eventName"
                }.addOnFailureListener {
                    textView.text = "Failed to time out: ${it.message}"
                }
            }
            .addOnFailureListener {
                textView.text = "Error fetching event record: ${it.message}"
            }
    }
}