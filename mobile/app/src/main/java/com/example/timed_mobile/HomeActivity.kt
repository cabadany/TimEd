package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    private lateinit var homeIcon: ImageView
    private lateinit var calendarIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var timeInButton: Button
    private lateinit var timeOutButton: Button
    private lateinit var excuseLetterText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        // Initialize views
        homeIcon = findViewById(R.id.bottom_nav_home)
        calendarIcon = findViewById(R.id.bottom_nav_calendar)
        profileIcon = findViewById(R.id.bottom_nav_profile)
        timeInButton = findViewById(R.id.btntime_in)
        timeOutButton = findViewById(R.id.btntime_out)
        excuseLetterText = findViewById(R.id.excuse_letter_text_button)

        // Set click listeners for navigation icons
        homeIcon.setOnClickListener {
            // Already on home screen, just show a toast
            Toast.makeText(
                this@HomeActivity,
                "You are already on the Home screen",
                Toast.LENGTH_SHORT
            ).show()
        }

        calendarIcon.setOnClickListener {
            // Navigate to calendar screen
            Toast.makeText(this@HomeActivity, "Navigating to Calendar", Toast.LENGTH_SHORT).show()
            // Intent intent = Intent(this, CalendarActivity::class.java)
            // startActivity(intent)
        }

        profileIcon.setOnClickListener {
            // Navigate to profile screen
            Toast.makeText(this@HomeActivity, "Navigating to Profile", Toast.LENGTH_SHORT).show()
            // Intent intent = Intent(this, ProfileActivity::class.java)
            // startActivity(intent)
        }

        // Implement click listeners for time buttons
        timeInButton.setOnClickListener {
            // Navigate to time-in page
            val intent = Intent(this, TimeInActivity::class.java)
            startActivity(intent)
        }

        timeOutButton.setOnClickListener {
            Toast.makeText(this, "Time-Out recorded", Toast.LENGTH_SHORT).show()
            // Implement time-out functionality here
        }

        // Implement click listener for excuse letter
        excuseLetterText.setOnClickListener {
            Toast.makeText(this, "Creating excuse letter", Toast.LENGTH_SHORT).show()
            // Navigate to excuse letter creation screen
            // Intent intent = Intent(this, ExcuseLetterActivity::class.java)
            // startActivity(intent)
        }
    }
}