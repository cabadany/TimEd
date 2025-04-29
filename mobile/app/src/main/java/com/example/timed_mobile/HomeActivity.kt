package com.example.timed_mobile

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.timed_mobile.model.TimeOutRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class HomeActivity : AppCompatActivity() {
    private lateinit var homeIcon: ImageView
    private lateinit var calendarIcon: ImageView
    private lateinit var profileIcon: ImageView
    private lateinit var timeInButton: Button
    private lateinit var timeOutButton: Button
    private lateinit var excuseLetterText: TextView

    private lateinit var database: DatabaseReference

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

        // Initialize database reference
        database = FirebaseDatabase.getInstance().reference

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Set click listeners with animation
        setupAnimatedClickListener(homeIcon) {
            Toast.makeText(this, "You are already on the Home screen", Toast.LENGTH_SHORT).show()
        }

        setupAnimatedClickListener(calendarIcon) {
            startActivity(Intent(this, ScheduleActivity::class.java))
            // Consider adding overridePendingTransition here if needed
        }

        setupAnimatedClickListener(profileIcon) {
            startActivity(Intent(this, ProfileActivity::class.java))
            // Consider adding overridePendingTransition here if needed
        }

        // Original listeners for other buttons
        timeInButton.setOnClickListener {
            startActivity(Intent(this, TimeInActivity::class.java))
        }

        timeOutButton.setOnClickListener {
            showTimeOutConfirmationDialog()
        }

        excuseLetterText.setOnClickListener {
            startActivity(Intent(this, ExcuseLetterActivity::class.java))
        }
    }

    // Helper function for click animation
    private fun setupAnimatedClickListener(view: View, onClickAction: () -> Unit) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.85f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.85f)
        scaleDownX.duration = 150
        scaleDownY.duration = 150
        scaleDownX.interpolator = AccelerateDecelerateInterpolator()
        scaleDownY.interpolator = AccelerateDecelerateInterpolator()

        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)
        scaleUpX.duration = 150
        scaleUpY.duration = 150
        scaleUpX.interpolator = AccelerateDecelerateInterpolator()
        scaleUpY.interpolator = AccelerateDecelerateInterpolator()

        val scaleDown = AnimatorSet()
        scaleDown.play(scaleDownX).with(scaleDownY)

        val scaleUp = AnimatorSet()
        scaleUp.play(scaleUpX).with(scaleUpY)

        view.setOnClickListener {
            scaleDown.start()
            // Execute the actual click action after the scale down animation
            view.postDelayed({
                scaleUp.start() // Start scaling back up
                onClickAction() // Perform the navigation/action
            }, 150) // Delay should match scaleDown duration
        }
    }

    private fun showTimeOutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.time_out_confirmation_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)
        val noButton = dialog.findViewById<Button>(R.id.btn_no)

        yesButton.setOnClickListener {
            dialog.dismiss()
            timeOutButton.isEnabled = false

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val timeOutRecord = TimeOutRecord(
                    timestamp = System.currentTimeMillis(),
                    status = "Timed Out"
                )
                database.child("TimeOuts").child(userId).push().setValue(timeOutRecord)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Time-out recorded successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to record time-out: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "No logged-in user.", Toast.LENGTH_SHORT).show()
            }

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
        dialog.setContentView(R.layout.success_popup_time_out)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        titleText.text = "Successfully Timed - Out"
        messageText.text = "Thank you. It has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}