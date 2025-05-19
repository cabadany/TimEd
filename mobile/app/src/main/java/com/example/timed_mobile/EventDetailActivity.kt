package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EventDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.event_detail)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) topDrawable.start()

        // Initialize the class member 'backButton'
        backButton = findViewById<ImageView>(R.id.icon_back_button)

        // Set the OnClickListener on the initialized class member 'backButton'
        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
                // Delay finish until animation can play
                view.postDelayed({
                    finish()
                }, 150) // Adjusted delay for animation
            } else {
                finish() // If not AVD, finish immediately
            }
        }


        // Get references to views
        val titleView = findViewById<TextView>(R.id.detail_event_title)
        val dateView = findViewById<TextView>(R.id.detail_event_date)
        val statusView = findViewById<TextView>(R.id.detail_event_status)
        val descriptionView = findViewById<TextView>(R.id.detail_event_description)
        val timeInButton = findViewById<Button>(R.id.detail_time_in_button)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)

        // Get event data from intent
        val title = intent.getStringExtra("eventTitle") ?: "No Title Provided"
        val date = intent.getStringExtra("eventDate") ?: "No Date Provided"
        val status = intent.getStringExtra("eventStatus") ?: "Unknown"
        val description = intent.getStringExtra("eventDescription") ?: "No Description Available"

        // Display event data in views
        titleView.text = title
        dateView.text = date
        statusView.text = "Status: $status"
        descriptionView.text = description

        // Conditionally show/hide the Time In button
        if (status.lowercase() == "ongoing") {
            timeInButton.visibility = Button.VISIBLE
            timeInButton.setOnClickListener {
                val intent = Intent(this, TimeInActivity::class.java)
                intent.putExtra("eventTitle", title)
                startActivity(intent)
            }
        } else {
            timeInButton.visibility = Button.GONE
        }

        // Handle back button click
        backButton.setOnClickListener {
            finish()
        }
    }
}