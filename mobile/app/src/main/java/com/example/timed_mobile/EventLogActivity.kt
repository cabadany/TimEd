package com.example.timed_mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.*

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

        recyclerView = findViewById(R.id.recycler_event_logs)
        emptyText = findViewById(R.id.text_empty)
        backButton = findViewById(R.id.icon_back_button_event_log)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_event_log)
        progressBar = findViewById(R.id.progress_bar_event_log)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation_event_log)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        backButton.setOnClickListener { finish() }

        adapter = EventLogAdapter(
            onTimeOutClick = { log -> /* not needed anymore */ },
            onTimeOutConfirmed = {
                fetchEventLogs() // This will reload updated data
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold)
        swipeRefreshLayout.setOnRefreshListener { fetchEventLogs(true) }

        fetchEventLogs(false)
    }

    private fun fetchEventLogs(isRefreshing: Boolean = false) {
        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user session. Please log in again.", Toast.LENGTH_SHORT).show()
            if (isRefreshing) swipeRefreshLayout.isRefreshing = false else progressBar.visibility = View.GONE
            showEmptyText("User session not found.")
            return
        }

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
                    updateUIWithLogs(logs)
                    return@addOnSuccessListener
                }

                for (eventDoc in eventSnapshots) {
                    val eventId = eventDoc.id
                    val eventName = eventDoc.getString("eventName") ?: "Unknown Event"
                    val eventDate = eventDoc.getTimestamp("date")?.toDate()
                    val duration = eventDoc.getString("duration") ?: "01:00:00"

                    val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
                    val durationMillis = (parts.getOrNull(0) ?: 0) * 3600000L +
                            (parts.getOrNull(1) ?: 0) * 60000L +
                            (parts.getOrNull(2) ?: 0) * 1000L
                    val now = System.currentTimeMillis()
                    val isStillActive = eventDate != null && (eventDate.time + durationMillis) > now

                    FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { attendeeSnapshot ->
                            if (!attendeeSnapshot.isEmpty) {
                                for (attendeeDoc in attendeeSnapshot.documents) {
                                    val timestamp = attendeeDoc.getString("timestamp") ?: "No timestamp"
                                    val hasTimedOut = attendeeDoc.getBoolean("hasTimedOut") ?: false

                                    logs.add(
                                        EventLogModel(
                                            eventId = eventId,
                                            eventName = eventName,
                                            timeInTimestamp = timestamp,
                                            status = if (hasTimedOut) "Timed-Out" else "Timed-In",
                                            showTimeOutButton = !hasTimedOut && isStillActive,
                                            userId = userId
                                        )
                                    )
                                }
                            }

                            processedCount++
                            if (processedCount == totalEvents) {
                                updateUIWithLogs(logs)
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == totalEvents) {
                                updateUIWithLogs(logs)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading events: ${it.message}", Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = View.GONE
                showEmptyText("Failed to load event data.")
            }
    }

    private fun updateUIWithLogs(logs: List<EventLogModel>) {
        if (logs.isEmpty()) {
            showEmptyText("No logs found.")
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            adapter.submitList(logs.sortedByDescending { it.timeInTimestamp })
        }

        swipeRefreshLayout.isRefreshing = false
        progressBar.visibility = View.GONE
    }

    private fun parseDurationToMillis(duration: String): Long {
        val parts = duration.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return (hours * 3600 + minutes * 60 + seconds) * 1000L
    }

    private fun showResults(logs: List<EventLogModel>, isRefreshing: Boolean) {
        if (logs.isEmpty()) {
            showEmptyText("No event logs found.")
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(logs.sortedByDescending { it.timeInTimestamp })
        }

        if (isRefreshing) swipeRefreshLayout.isRefreshing = false
        else progressBar.visibility = View.GONE
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