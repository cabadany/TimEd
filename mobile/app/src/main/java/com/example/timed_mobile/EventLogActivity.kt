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
import com.example.timed_mobile.model.EventLogModel
import com.google.firebase.firestore.FirebaseFirestore

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
        backButton = findViewById(R.id.icon_back_button_event_log)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_event_log)
        progressBar = findViewById(R.id.progress_bar_event_log)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation_event_log)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        backButton.setOnClickListener {
            finish()
        }

        adapter = EventLogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold)
        swipeRefreshLayout.setOnRefreshListener {
            fetchEventLogs(isRefreshing = true)
        }

        fetchEventLogs(isRefreshing = false)
    }

    private fun fetchEventLogs(isRefreshing: Boolean = false) {
        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user session. Please log in again.", Toast.LENGTH_SHORT).show()
            if (isRefreshing) swipeRefreshLayout.isRefreshing = false else progressBar.visibility = View.GONE
            showEmptyText("User session not found.")
            return
        }

        Log.d(TAG, "Fetching CURRENT time-ins for userId: $userId")

        if (!isRefreshing) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.GONE
            emptyText.alpha = 0f
        }

        FirebaseFirestore.getInstance()
            .collection("events")
            .get()
            .addOnSuccessListener { eventSnapshots ->
                val logs = mutableListOf<EventLogModel>()
                val totalEvents = eventSnapshots.size()
                var processedCount = 0

                if (totalEvents == 0) {
                    showEmptyText("No events found.")
                    swipeRefreshLayout.isRefreshing = false
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                for (eventDoc in eventSnapshots) {
                    val eventId = eventDoc.id
                    val eventName = eventDoc.getString("eventName") ?: "Unknown Event"

                    FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { attendeeDoc ->
                            Log.d(TAG, "Checked event: $eventId → attendee exists: ${attendeeDoc.exists()}")

                            if (attendeeDoc.exists()) {
                                val timestamp = attendeeDoc.getString("timestamp") ?: "No timestamp"
                                val hasTimedOut = attendeeDoc.getBoolean("hasTimedOut") ?: false

                                if (!hasTimedOut) { // ✅ Only include if still timed-in
                                    logs.add(
                                        EventLogModel(
                                            eventId = eventId,
                                            eventName = eventName,
                                            timeInTimestamp = timestamp,
                                            status = "Timed-In: Haven’t Timed-Out"
                                        )
                                    )
                                }
                            }

                            processedCount++
                            if (processedCount == totalEvents) {
                                if (logs.isEmpty()) {
                                    showEmptyText("You are not currently timed-in to any event.")
                                } else {
                                    emptyText.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                    adapter.submitList(logs.sortedByDescending { it.timeInTimestamp })
                                }

                                if (isRefreshing) swipeRefreshLayout.isRefreshing = false
                                else progressBar.visibility = View.GONE
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed checking attendees in $eventId", e)
                            processedCount++
                            if (processedCount == totalEvents) {
                                if (logs.isEmpty()) {
                                    showEmptyText("You are not currently timed-in to any event.")
                                } else {
                                    emptyText.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                    adapter.submitList(logs.sortedByDescending { it.timeInTimestamp })
                                }

                                if (isRefreshing) swipeRefreshLayout.isRefreshing = false
                                else progressBar.visibility = View.GONE
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch events", e)
                Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = View.GONE
                showEmptyText("Error loading logs.")
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