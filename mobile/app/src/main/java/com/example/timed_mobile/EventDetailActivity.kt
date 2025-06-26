package com.example.timed_mobile

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView // Ensure this import is present
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var descriptionView: TextView
    // private lateinit var contentScrollView: ScrollView // Can be a local variable if only used in onCreate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_detail)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        backButton = findViewById(R.id.icon_back_button)
        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
                // Consider finishing after animation or using a transition
                view.postDelayed({ finish() }, 150) // Delay to allow animation to play
            } else {
                finish()
            }
        }

        val titleView = findViewById<TextView>(R.id.detail_event_title)
        val dateView = findViewById<TextView>(R.id.detail_event_date)
        val statusView = findViewById<TextView>(R.id.detail_event_status)
        descriptionView = findViewById(R.id.detail_event_description)
        val timeInButton = findViewById<Button>(R.id.detail_time_in_button)
        val contentScrollView = findViewById<ScrollView>(R.id.content_scroll_view) // Initialize contentScrollView

        val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"
        val date = intent.getStringExtra("eventDate") ?: "No Date Provided"
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        val descriptionFromIntent = intent.getStringExtra("eventDescription") ?: ""

        titleView.text = title
        dateView.text = date
        statusView.text = "Status: $status" // Consider using a string resource for "Status: %s"

        if (descriptionFromIntent.isNotBlank()) {
            descriptionView.text = descriptionFromIntent
        } else {
            fetchEventDescriptionFromFirestore(title)
        }

        if (status.lowercase() == "ongoing") {
            // Check user's attendance status for this event
            val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
            
            if (userId != null) {
                checkUserAttendanceStatus(title, userId, timeInButton, sharedPrefs)
            } else {
                timeInButton.visibility = Button.GONE
            }
        } else {
            timeInButton.visibility = Button.GONE
        }

        // --- Add Animations ---
        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_from_bottom)

        // Start animations
        fadeIn.startOffset = 200L // Optional: delay fade-in slightly
        contentScrollView.startAnimation(fadeIn) // Animate the ScrollView containing the card

        // Only animate the button if it's visible
        if (timeInButton.visibility == Button.VISIBLE) {
            slideUp.startOffset = 400L // Optional: delay slide-up
            timeInButton.startAnimation(slideUp)
        }
        // --- End of Animations ---
    }

    override fun onResume() {
        super.onResume()
        // Refresh the attendance status when user returns to this screen
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        if (status.lowercase() == "ongoing") {
            val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
            val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"
            val timeInButton = findViewById<Button>(R.id.detail_time_in_button)
            
            if (userId != null) {
                checkUserAttendanceStatus(title, userId, timeInButton, sharedPrefs)
            }
        }
    }

    private fun fetchEventDescriptionFromFirestore(title: String) {
        FirebaseFirestore.getInstance().collection("events")
            .whereEqualTo("eventName", title)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents.first()
                    val fetchedDescription = doc.getString("description") ?: "No description available"
                    descriptionView.text = fetchedDescription
                } else {
                    descriptionView.text = "Description not found." // Consider string resource
                }
            }
            .addOnFailureListener {
                descriptionView.text = "Error loading description." // Consider string resource
                Toast.makeText(this, "Failed to load description", Toast.LENGTH_SHORT).show() // Consider string resource
            }
    }

    private fun checkUserAttendanceStatus(eventTitle: String, userId: String, timeInButton: Button, sharedPrefs: SharedPreferences) {
        // First get the event ID from the event title
        FirebaseFirestore.getInstance().collection("events")
            .whereEqualTo("eventName", eventTitle)
            .limit(1)
            .get()
            .addOnSuccessListener { eventResult ->
                if (!eventResult.isEmpty) {
                    val eventDoc = eventResult.documents.first()
                    val eventId = eventDoc.id

                    // Now check if user has attendance record for this event
                    FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { attendeeSnapshot ->
                            if (!attendeeSnapshot.isEmpty) {
                                // User has timed in for this event
                                val attendeeDoc = attendeeSnapshot.documents.first()
                                val hasTimedOut = attendeeDoc.getBoolean("hasTimedOut") ?: false
                                
                                if (hasTimedOut) {
                                    // User has already timed out
                                    timeInButton.visibility = Button.GONE
                                } else {
                                    // User has timed in but not timed out - show Time Out button
                                    timeInButton.text = "Time Out"
                                    timeInButton.visibility = Button.VISIBLE
                                    timeInButton.setOnClickListener {
                                        showTimeOutConfirmation(eventId, eventTitle, userId, attendeeDoc.reference)
                                    }
                                }
                            } else {
                                // User has not timed in yet - show Time In button
                                timeInButton.text = "Time In"
                                timeInButton.visibility = Button.VISIBLE
                                timeInButton.setOnClickListener {
                                    val email = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
                                    val firstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)

                                    val intent = Intent(this, TimeInEventActivity::class.java).apply {
                                        putExtra("userId", userId)
                                        putExtra("email", email)
                                        putExtra("firstName", firstName)
                                        putExtra("eventTitle", eventTitle)
                                        putExtra("eventDescription", descriptionView.text.toString())
                                    }
                                    startActivity(intent)
                                }
                            }
                        }
                        .addOnFailureListener {
                            // Error checking attendance - default to Time In button
                            timeInButton.text = "Time In"
                            timeInButton.visibility = Button.VISIBLE
                            timeInButton.setOnClickListener {
                                val email = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
                                val firstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)

                                val intent = Intent(this, TimeInEventActivity::class.java).apply {
                                    putExtra("userId", userId)
                                    putExtra("email", email)
                                    putExtra("firstName", firstName)
                                    putExtra("eventTitle", eventTitle)
                                    putExtra("eventDescription", descriptionView.text.toString())
                                }
                                startActivity(intent)
                            }
                        }
                } else {
                    // Event not found - hide button
                    timeInButton.visibility = Button.GONE
                }
            }
            .addOnFailureListener {
                // Error finding event - hide button
                timeInButton.visibility = Button.GONE
            }
    }

    private fun showTimeOutConfirmation(eventId: String, eventTitle: String, userId: String, attendeeRef: com.google.firebase.firestore.DocumentReference) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Time-Out")
            .setMessage("Are you sure you want to time out from \"$eventTitle\"?")
            .setPositiveButton("Yes") { _, _ ->
                performTimeOut(eventId, eventTitle, userId, attendeeRef)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTimeOut(eventId: String, eventTitle: String, userId: String, attendeeRef: com.google.firebase.firestore.DocumentReference) {
        // Create timeout timestamp with Philippines timezone
        val philippinesTimeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = philippinesTimeZone
        val timeOutTimestamp = sdf.format(java.util.Date())

        val updates = mapOf(
            "hasTimedOut" to true,
            "timeOutTimestamp" to timeOutTimestamp
        )

        attendeeRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Successfully timed out from $eventTitle", Toast.LENGTH_SHORT).show()
                // Hide the button since user has timed out
                findViewById<Button>(R.id.detail_time_in_button).visibility = Button.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error timing out: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}