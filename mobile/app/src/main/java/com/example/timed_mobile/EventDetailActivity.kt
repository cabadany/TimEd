package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView // Ensure this import is present
import android.widget.TextView
import android.widget.Toast
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
            timeInButton.visibility = Button.VISIBLE
            timeInButton.setOnClickListener {
                val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
                val email = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
                val firstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)

                val intent = Intent(this, TimeInEventActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("email", email)
                    putExtra("firstName", firstName)
                    putExtra("eventTitle", title)
                    // Pass the current text from descriptionView, which might have been fetched
                    putExtra("eventDescription", descriptionView.text.toString())
                }
                startActivity(intent)
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
}