package com.example.timed_mobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EventDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val titleView = findViewById<TextView>(R.id.detail_event_title)
        val dateView = findViewById<TextView>(R.id.detail_event_date)
        val statusView = findViewById<TextView>(R.id.detail_event_status)
        val timeInButton = findViewById<Button>(R.id.detail_time_in_button)

        val title = intent.getStringExtra("eventTitle") ?: "No title"
        val date = intent.getStringExtra("eventDate") ?: "No date"
        val status = intent.getStringExtra("eventStatus") ?: "unknown"

        titleView.text = title
        dateView.text = date
        statusView.text = "Status: $status"

        if (status.lowercase() == "ongoing") {
            timeInButton.visibility = Button.VISIBLE
            timeInButton.setOnClickListener {
                // You can link to your TimeInActivity here
            }
        } else {
            timeInButton.visibility = Button.GONE
        }
    }
}