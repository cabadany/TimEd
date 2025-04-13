package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window


class HomeActivity : AppCompatActivity() {
    private lateinit var homeIcon: ImageView
    private lateinit var calendarIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var timeInButton: Button
    private lateinit var timeOutButton: Button
    private lateinit var excuseLetterText: TextView

    // Add these methods to HomeActivity class:

    private fun showTimeOutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Set up the dialog view
        dialog.setContentView(R.layout.time_out_confirmation_dialog)

        // Set transparent background and dim amount
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Set up button click listeners
        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)
        val noButton = dialog.findViewById<Button>(R.id.btn_no)

        yesButton.setOnClickListener {
            dialog.dismiss()
            // Show processing delay of 3 seconds
            timeOutButton.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                showTimeOutSuccessPopup()
                timeOutButton.isEnabled = true
            }, 3000)
        }

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimeOutSuccessPopup() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Set the custom layout
        dialog.setContentView(R.layout.success_popup_time_out)

        // Update title and message for time-out
        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)

        titleText.text = "Successfully Timed - Out"
        messageText.text = "Thank you. It has been recorded."

        // Set transparent background and dim amount
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Find and setup close button
        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


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
            // Navigate to schedule screen
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
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

        timeOutButton.setOnClickListener {
            showTimeOutConfirmationDialog()
        }

        // In HomeActivity.kt, update the profile icon click listener:
        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

    }

    // In the onCreate method of HomeActivity.kt, update the timeOutButton click listener:



}

