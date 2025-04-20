package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import androidx.appcompat.app.AppCompatActivity


class ProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var homeIcon: ImageView
    private lateinit var calendarIcon: ImageView
    private lateinit var editButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var attendanceSheetButton: Button
    private lateinit var logoutText: TextView

    // Add these methods to ProfileActivity class:
    private fun showAttendanceDownloadDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Set up the dialog view
        dialog.setContentView(R.layout.attendance_download_dialog)

        // Set transparent background and dim amount for modal effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Set up button click listeners
        val noButton = dialog.findViewById<Button>(R.id.btn_no)
        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)

        noButton.setOnClickListener {
            dialog.dismiss()
        }

        yesButton.setOnClickListener {
            dialog.dismiss()
            // Simulate download progress
            // In a real app, this would be an actual download process
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadSuccessDialog()
            }, 1500) // Simulate 1.5 second download
        }

        dialog.show()
    }

    private fun showDownloadSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Set up the dialog view
        dialog.setContentView(R.layout.success_popup_attendance_download)

        // Set transparent background and dim amount for modal effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Set up OK button click listener
        val okButton = dialog.findViewById<Button>(R.id.btn_ok)
        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)

        // Set up the dialog view
        dialog.setContentView(R.layout.logout_confirmation_dialog)

        // Set transparent background and dim amount for modal effect
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Set up button click listeners
        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
        val logoutButton = dialog.findViewById<Button>(R.id.btn_logout)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        logoutButton.setOnClickListener {
            dialog.dismiss()

            // Perform logout - navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        // Initialize views
        backButton = findViewById(R.id.icon_back_button)
        homeIcon = findViewById(R.id.bottom_nav_home)
        calendarIcon = findViewById(R.id.bottom_nav_calendar)
        editButton = findViewById(R.id.btn_edit_profile)
        changePasswordButton = findViewById(R.id.btn_change_password)
        attendanceSheetButton = findViewById(R.id.btn_attendance_sheet)
        editButton = findViewById(R.id.btn_edit_profile)
        logoutText = findViewById(R.id.logout_text)

        // Set click listeners
        backButton.setOnClickListener {
            finish()
        }

        homeIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        calendarIcon.setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            startActivity(intent)
            finish()
        }

        editButton.setOnClickListener {
            // Open edit profile screen or dialog
        }

        changePasswordButton.setOnClickListener {
            // Open change password screen or dialog
        }

        attendanceSheetButton.setOnClickListener {
            showAttendanceDownloadDialog()
        }

        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        // Add to ProfileActivity.kt in the onCreate method:
        changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }


        editButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }


}