package com.example.timed_mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.timed_mobile.adapter.EventLogAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.timed_mobile.model.EventLogModel
import com.google.firebase.firestore.Query

class EventLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: EventLogAdapter
    private lateinit var backButton: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private val FADE_DURATION = 300L
    private val TAG = "EventLogActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_log)

        // Initialize views
        recyclerView = findViewById(R.id.recycler_event_logs)
        emptyText = findViewById(R.id.text_empty)
        backButton = findViewById(R.id.icon_back_button_event_log) // Ensure this ID matches your event_log.xml
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_event_log) // Ensure this ID matches
        progressBar = findViewById(R.id.progress_bar_event_log) // Ensure this ID matches

        val topWave = findViewById<ImageView>(R.id.top_wave_animation_event_log) // Ensure this ID matches
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        backButton.setOnClickListener {
            finish()
        }

        adapter = EventLogAdapter() // You might want to pass a click listener here if items are interactive
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold) // Customize colors
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe to refresh triggered.")
            fetchEventLogs(isRefreshing = true)
        }

        // Initial fetch of event logs
        fetchEventLogs(isRefreshing = false)
    }

    private fun fetchEventLogs(isRefreshing: Boolean = false) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            if (isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            } else {
                progressBar.visibility = View.GONE
            }
            showEmptyText("User not authenticated. Please log in.")
            return
        }

        Log.d(TAG, "Fetching event logs for UID: $userId")

        if (!isRefreshing) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.GONE
            emptyText.alpha = 0f // Reset alpha for potential fade-in
        }

        FirebaseFirestore.getInstance()
            .collection("attendanceRecords")
            .whereEqualTo("userId", userId)
            // Consider if you want to filter by "type" == "event_time_in" or show all types.
            // If you only want "event_time_in", uncomment the line below.
            // .whereEqualTo("type", "event_time_in")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp, newest first
            .get()
            .addOnSuccessListener { documents ->
                val logs = documents.mapNotNull { doc ->
                    // Robust mapping with defaults
                    val eventId = doc.getString("eventId") ?: doc.id
                    val eventName = doc.getString("eventName") ?: "Unknown Event"
                    // Assuming timestamp is stored as a String. If it's a Firestore Timestamp, use doc.getTimestamp("timestamp")
                    val timestamp = doc.getString("timestamp") ?: "No timestamp"
                    val type = doc.getString("type") ?: "unknown_type"
                    val hasTimedOut = doc.getBoolean("hasTimedOut") ?: false

                    // Determine status based on type and hasTimedOut
                    val status = when {
                        type == "event_time_out" -> "Timed-Out" // Explicit time-out record
                        type == "event_time_in" && hasTimedOut -> "Timed-Out" // Time-in record that has been marked as timed out
                        type == "event_time_in" && !hasTimedOut -> "Timed-In: Active" // Time-in record, not yet timed out
                        else -> "Status Unknown"
                    }

                    EventLogModel(
                        eventId = eventId,
                        eventName = eventName,
                        timeInTimestamp = timestamp, // This field might need renaming if it can hold time-out timestamps too
                        status = status
                    )
                }

                if (logs.isEmpty()) {
                    showEmptyText("No event logs found.")
                } else {
                    emptyText.visibility = View.GONE
                    emptyText.alpha = 0f
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(logs)
                }

                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch event logs", e)
                Toast.makeText(this, "Failed to load logs: ${e.message}", Toast.LENGTH_LONG).show()
                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    progressBar.visibility = View.GONE
                }
                showEmptyText("Error loading logs. Swipe to try again.")
            }
    }

    private fun showEmptyText(message: String) {
        recyclerView.visibility = View.GONE
        emptyText.text = message
        emptyText.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    emptyText.visibility = View.VISIBLE
                }
            })
    }
}