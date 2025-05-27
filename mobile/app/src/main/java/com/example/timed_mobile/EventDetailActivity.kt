package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var descriptionView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_detail)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) topDrawable.start()

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
        val timeInButton = findViewById<Button>(R.id.detail_time_in_button)

        val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"
        val date = intent.getStringExtra("eventDate") ?: "No Date Provided"
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        val description = intent.getStringExtra("eventDescription") ?: ""

        titleView.text = title
        dateView.text = date
        statusView.text = "Status: $status"

        if (description.isNotBlank()) {
            descriptionView.text = description
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
                    putExtra("eventDescription", description)
                }
                startActivity(intent)
            }
        } else {
            timeInButton.visibility = Button.GONE
        }

        // Redundant backButton reassignment removed since it’s already handled above
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
                    descriptionView.text = "Description not found."
                }
            }
            .addOnFailureListener {
                descriptionView.text = "Error loading description."
                Toast.makeText(this, "Failed to load description", Toast.LENGTH_SHORT).show()
            }
    }
}