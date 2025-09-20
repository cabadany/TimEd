package com.example.timed_mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
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
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class EventLogActivity : WifiSecurityActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: EventLogAdapter
    private lateinit var backButton: ImageView // Class member
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // Class member, used for animation target
    private lateinit var progressBar: ProgressBar

    private val FADE_DURATION = 300L
    private val TAG = "EventLogActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_log)

        // Initialize class members first
        recyclerView = findViewById(R.id.recycler_event_logs)
        emptyText = findViewById(R.id.text_empty)
        progressBar = findViewById(R.id.progress_bar_event_log)
        backButton = findViewById(R.id.icon_back_button_event_log) // Initialize class member backButton
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout_event_log) // Initialize class member swipeRefreshLayout

        val topWave = findViewById<ImageView>(R.id.top_wave_animation_event_log)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        backButton.setOnClickListener { finish() } // Now safe to set listener

        // Views for animation (some might be the same as class members)
        val iconBackButtonForAnimation = backButton // Use the initialized class member
        val eventLogTitle = findViewById<TextView>(R.id.event_log_title)
        val swipeRefreshLayoutForAnimation = swipeRefreshLayout // Use the initialized class member

        // --- START OF ANIMATION CODE ---
        // Load animations from your existing files
        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        val animContentFadeInLong = AnimationUtils.loadAnimation(this, R.anim.fade_in_long) // Using fade_in_long

        // Apply animations with staggered delays
        var currentDelay = 100L

        // 1. Back Button
        val animBackButtonInstance = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        animBackButtonInstance.startOffset = currentDelay
        iconBackButtonForAnimation.startAnimation(animBackButtonInstance)
        currentDelay += 150L

        // 2. Title
        animSlideDownFadeIn.startOffset = currentDelay
        eventLogTitle.startAnimation(animSlideDownFadeIn)
        currentDelay += 200L

        // 3. Main Content Area (SwipeRefreshLayout)
        animContentFadeInLong.startOffset = currentDelay
        swipeRefreshLayoutForAnimation.startAnimation(animContentFadeInLong)
        // --- END OF ANIMATION CODE ---

        // MERGED: The adapter now handles the time-out click by showing a confirmation dialog.
        adapter = EventLogAdapter(
            onTimeOutClick = { log ->
                showTimeOutConfirmationDialog(log)
            },
            onTimeOutConfirmed = {
                fetchEventLogs() // This will reload updated data
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

    swipeRefreshLayout.setColorSchemeResources(R.color.primary_deep_blue, R.color.primary_medium_blue)
        swipeRefreshLayout.setOnRefreshListener { fetchEventLogs(true) }

        fetchEventLogs(false)
    }

    private fun fetchEventLogs(isRefreshing: Boolean = false) {
        val userId = intent.getStringExtra("userId")
        if (userId.isNullOrEmpty()) {
            UiDialogs.showErrorPopup(
                this,
                title = "Missing Session",
                message = "Missing user session. Please log in again."
            )
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
                                    // MERGED: Get the unique ID of the attendee document to enable time-out.
                                    val attendeeDocId = attendeeDoc.id
                                    val timestamp = attendeeDoc.getString("timestamp") ?: "No timestamp"
                                    val hasTimedOut = attendeeDoc.getBoolean("hasTimedOut") ?: false
                                    // Prefer new flag; fallback to legacy manualEntry if present
                                    val checkinMethod = attendeeDoc.getBoolean("checkinMethod")
                                        ?: (attendeeDoc.getBoolean("manualEntry") ?: false)

                                    // MERGED: Create the EventLogModel with the attendeeDocId.
                                    // Note: Your EventLogModel class must have the 'attendeeDocId' field.
                                    logs.add(
                                        EventLogModel(
                                            eventId = eventId,
                                            attendeeDocId = attendeeDocId,
                                            eventName = eventName,
                                            timeInTimestamp = timestamp,
                                            status = if (hasTimedOut) "Timed-Out" else "Timed-In",
                                            showTimeOutButton = !hasTimedOut && isStillActive,
                                            checkinMethod = checkinMethod,
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
                            Log.e(TAG, "Error fetching attendees for event $eventId", it)
                            processedCount++
                            if (processedCount == totalEvents) {
                                updateUIWithLogs(logs)
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error loading events", it)
                UiDialogs.showErrorPopup(
                    this,
                    title = "Load Error",
                    message = "Error loading events: ${it.message}"
                )
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = View.GONE
                showEmptyText("Failed to load event data.")
            }
    }

    // --- MERGED: Functions to handle the time-out process ---
    private fun showTimeOutConfirmationDialog(log: EventLogModel) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Time-Out")
            .setMessage("Are you sure you want to time out from the event '${log.eventName}'?")
            .setPositiveButton("Confirm") { dialog, _ ->
                performTimeOut(log)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTimeOut(log: EventLogModel) {
    // Keep this as a short info toast; not an error.
    Toast.makeText(this, "Processing time-out...", Toast.LENGTH_SHORT).show()

        val db = FirebaseFirestore.getInstance()
        val eventDocRef = db.collection("events").document(log.eventId)
        val attendeeDocRef = eventDocRef.collection("attendees").document(log.attendeeDocId)

        db.runTransaction { transaction ->
            val attendeeSnapshot = transaction.get(attendeeDocRef)
            if (attendeeSnapshot.getBoolean("hasTimedOut") == true) {
                return@runTransaction null
            }
            transaction.update(attendeeDocRef, "hasTimedOut", true)
            transaction.update(eventDocRef, "timeOutCount", FieldValue.increment(1))
            null
        }.addOnSuccessListener {
            Toast.makeText(this, "Successfully timed out from ${log.eventName}", Toast.LENGTH_SHORT).show()
            fetchEventLogs(true)
        }.addOnFailureListener { e ->
            UiDialogs.showErrorPopup(
                this,
                title = "Time-Out Failed",
                message = "Failed to time out: ${e.message}"
            )
            Log.e(TAG, "Time-out transaction failed for doc ${log.attendeeDocId}", e)
        }
    }
    // --- END of merged functions ---

    private fun updateUIWithLogs(logs: List<EventLogModel>) {
        if (logs.isEmpty()) {
            showEmptyText("No logs found.")
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE // Ensure empty text is hidden if logs are present
            emptyText.alpha = 0f // Reset alpha if it was animated
            adapter.submitList(logs.sortedByDescending { it.timeInTimestamp })
        }

        swipeRefreshLayout.isRefreshing = false
        progressBar.visibility = View.GONE
    }

    private fun parseDurationToMillis(duration: String): Long { // This function seems unused, consider removing if not needed
        val parts = duration.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val seconds = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return (hours * 3600 + minutes * 60 + seconds) * 1000L
    }

    private fun showResults(logs: List<EventLogModel>, isRefreshing: Boolean) { // This function seems unused, consider removing if not needed
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
                    super.onAnimationStart(animation) // Call super
                    emptyText.visibility = View.VISIBLE
                }
            })
    }
}