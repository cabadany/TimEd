package com.example.timed_mobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class TimeInActivity : AppCompatActivity() {
    private lateinit var backButton: ImageView
    private lateinit var timeInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        // Initialize views
        backButton = findViewById(R.id.icon_back_button)
        timeInButton = findViewById(R.id.btntime_in)

        // Set click listener for back button
        backButton.setOnClickListener {
            finish()
        }

        // Set click listener for time-in button
        timeInButton.setOnClickListener {
            // Show a loading state or disable button to prevent multiple clicks
            timeInButton.isEnabled = false

            // Set a 3-second delay before showing the success popup
            Handler(Looper.getMainLooper()).postDelayed({
                showSuccessPopup()
            }, 1000) // 1 seconds delay
        }
    }

    private fun showSuccessPopup() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Set the custom layout
        val view = LayoutInflater.from(this).inflate(R.layout.success_popup_time_in, null)
        dialog.setContentView(view)

        // Set transparent background for dialog (to show shadow overlay)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dim amount to exactly 50% as specified
        dialog.window?.setDimAmount(0.5f)

        // Find the close button and set its click listener
        val closeButton = view.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            // Return to home page
            finish()
        }

        dialog.show()
    }
}