package com.example.timed_mobile

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView // Ensure this import is present
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.timed_mobile.HomeActivity
import com.example.timed_mobile.tutorial.EventTutorialState

class EventDetailActivity : WifiSecurityActivity() {

    private lateinit var backButton: ImageView
    private lateinit var descriptionView: TextView
    private lateinit var venueView: TextView // Add a class-level variable for the venue TextView
    private lateinit var timeInButton: Button
    private var lastShownTutorialAction: String? = null
    private var tutorialDialog: AlertDialog? = null

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
                view.postDelayed({ finish() }, 150)
            } else {
                finish()
            }
        }

        val titleView = findViewById<TextView>(R.id.detail_event_title)
        val dateView = findViewById<TextView>(R.id.detail_event_date)
        val statusView = findViewById<TextView>(R.id.detail_event_status)
        descriptionView = findViewById(R.id.detail_event_description)
        venueView = findViewById(R.id.detail_event_venue) // Initialize the venue TextView
    timeInButton = findViewById(R.id.detail_time_in_button)
        val contentScrollView = findViewById<ScrollView>(R.id.content_scroll_view)

        val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"
        val date = intent.getStringExtra("eventDate") ?: "No Date Provided"
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        val venue = intent.getStringExtra("eventVenue") ?: "No Venue Provided" // Get venue from intent
        val descriptionFromIntent = intent.getStringExtra("eventDescription") ?: ""

        titleView.text = title
        dateView.text = date
        statusView.text = "Status: $status"
        venueView.text = venue // Set the venue text

        if (descriptionFromIntent.isNotBlank()) {
            descriptionView.text = descriptionFromIntent
        } else {
            fetchEventDetailsFromFirestore(title) // This will now fetch description and venue
        }

        if (status.lowercase() == "ongoing") {
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

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_from_bottom)

        fadeIn.startOffset = 200L
        contentScrollView.startAnimation(fadeIn)

        if (timeInButton.visibility == Button.VISIBLE) {
            slideUp.startOffset = 400L
            timeInButton.startAnimation(slideUp)
        }
    }

    override fun onResume() {
        super.onResume()
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        if (status.lowercase() == "ongoing") {
            val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
            val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"

            if (userId != null) {
                checkUserAttendanceStatus(title, userId, timeInButton, sharedPrefs)
            }
        }
        timeInButton.post { maybeShowEventTutorialPrompt() }
    }

    private fun fetchEventDetailsFromFirestore(title: String) {
        FirebaseFirestore.getInstance().collection("events")
            .whereEqualTo("eventName", title)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val doc = result.documents.first()
                    val fetchedDescription = doc.getString("description") ?: "No description available"
                    val fetchedVenue = doc.getString("venue") ?: "No venue available" // Fetch venue
                    descriptionView.text = fetchedDescription
                    venueView.text = fetchedVenue // Update venue view
                } else {
                    descriptionView.text = "Description not found."
                    venueView.text = "Venue not found."
                }
            }
            .addOnFailureListener {
                descriptionView.text = "Error loading description."
                venueView.text = "Error loading venue."
                Toast.makeText(this, "Failed to load details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserAttendanceStatus(eventTitle: String, userId: String, timeInButton: Button, sharedPrefs: SharedPreferences) {
        FirebaseFirestore.getInstance().collection("events")
            .whereEqualTo("eventName", eventTitle)
            .limit(1)
            .get()
            .addOnSuccessListener { eventResult ->
                if (!eventResult.isEmpty) {
                    val eventDoc = eventResult.documents.first()
                    val eventId = eventDoc.id

                    FirebaseFirestore.getInstance()
                        .collection("events")
                        .document(eventId)
                        .collection("attendees")
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { attendeeSnapshot ->
                            if (!attendeeSnapshot.isEmpty) {
                                val attendeeDoc = attendeeSnapshot.documents.first()
                                val hasTimedOut = attendeeDoc.getBoolean("hasTimedOut") ?: false

                                if (hasTimedOut) {
                                    timeInButton.visibility = Button.GONE
                                } else {
                                    timeInButton.text = "Time Out"
                                    timeInButton.visibility = Button.VISIBLE
                                    timeInButton.setOnClickListener {
                                        showTimeOutConfirmation(eventId, eventTitle, userId, attendeeDoc.reference)
                                    }
                                }
                            } else {
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
                                        putExtra("eventVenue", venueView.text.toString()) // Pass venue to next activity
                                    }
                                    startActivity(intent)
                                }
                            }
                            timeInButton.post { maybeShowEventTutorialPrompt() }
                        }
                        .addOnFailureListener {
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
                                    putExtra("eventVenue", venueView.text.toString()) // Pass venue to next activity
                                }
                                startActivity(intent)
                            }
                            timeInButton.post { maybeShowEventTutorialPrompt() }
                        }
                } else {
                    timeInButton.visibility = Button.GONE
                    timeInButton.post { maybeShowEventTutorialPrompt() }
                }
            }
            .addOnFailureListener {
                timeInButton.visibility = Button.GONE
                timeInButton.post { maybeShowEventTutorialPrompt() }
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
                val baseMessage = "Successfully timed out from $eventTitle"
                if (EventTutorialState.isActive(this)) {
                    EventTutorialState.completeStep(this, HomeActivity.TOTAL_EVENT_TUTORIAL_STEPS, markCompleted = true)
                    EventTutorialState.setActive(this, false)
                    Toast.makeText(this, "$baseMessage • Event guide completed! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, baseMessage, Toast.LENGTH_SHORT).show()
                }
                timeInButton.visibility = Button.GONE
                lastShownTutorialAction = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error timing out: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun maybeShowEventTutorialPrompt() {
        if (!EventTutorialState.isActive(this)) {
            tutorialDialog?.dismiss()
            tutorialDialog = null
            lastShownTutorialAction = null
            return
        }

        val expected = EventTutorialState.getExpectedAction(this) ?: return
        if (expected == lastShownTutorialAction) return

        val target = when (expected) {
            HomeActivity.ACTION_EVENT_TIME_IN,
            HomeActivity.ACTION_EVENT_TIME_OUT -> timeInButton.takeIf { it.visibility == View.VISIBLE }
            else -> null
        } ?: return

        val message = when (expected) {
            HomeActivity.ACTION_EVENT_TIME_IN -> "Here's the Time-In button. Tap it when you're ready and I'll line up the next step for you."
            HomeActivity.ACTION_EVENT_TIME_OUT -> "When the activity wraps up, use Time-Out right here to finish logging your attendance."
            else -> return
        }

        lastShownTutorialAction = expected
        pulseView(target)

        tutorialDialog?.dismiss()
        tutorialDialog = AlertDialog.Builder(this)
            .setTitle("Event Guide")
            .setMessage(message)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { tutorialDialog = null }
            .create()
        tutorialDialog?.show()
    }

    private fun pulseView(target: View) {
        target.animate().cancel()
        target.scaleX = 1f
        target.scaleY = 1f
        target.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(180)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    override fun onPause() {
        super.onPause()
        tutorialDialog?.dismiss()
        tutorialDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        tutorialDialog?.dismiss()
        tutorialDialog = null
    }
}