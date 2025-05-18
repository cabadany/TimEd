package com.example.timed_mobile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.adapter.EventLogAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.timed_mobile.model.EventLogModel

class EventLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: EventLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_log)

        recyclerView = findViewById(R.id.recycler_event_logs)
        emptyText = findViewById(R.id.text_empty)
        adapter = EventLogAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchEventLogs()
    }

    private fun fetchEventLogs() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("attendanceRecords")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", "event_time_in")
            .get()
            .addOnSuccessListener { documents ->
                val logs = documents.map { doc ->
                    EventLogModel(
                        eventId = doc.getString("eventId") ?: doc.id,
                        eventName = doc.getString("eventName") ?: "Unknown Event",
                        timeInTimestamp = doc.getString("timestamp") ?: "-",
                        status = if (doc.getBoolean("hasTimedOut") == true)
                            "Timed-Out"
                        else
                            "Timed-In: Haven't Timed-Out"
                    )
                }

                if (logs.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                } else {
                    emptyText.visibility = View.GONE
                    adapter.submitList(logs)
                }
            }
            .addOnFailureListener { e ->
                Log.e("EventLogActivity", "Failed to fetch logs", e)
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Error loading logs."
            }
    }
}