package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

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