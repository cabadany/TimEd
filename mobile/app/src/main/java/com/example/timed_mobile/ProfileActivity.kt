package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

import android.graphics.Color

class ProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var homeIcon: ImageView
    private lateinit var calendarIcon: ImageView
    private lateinit var editButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var attendanceSheetButton: Button
    private lateinit var logoutText: TextView

    private lateinit var teacherName: TextView
    private lateinit var teacherId: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        backButton = findViewById(R.id.icon_back_button)
        homeIcon = findViewById(R.id.bottom_nav_home)
        calendarIcon = findViewById(R.id.bottom_nav_calendar)
        editButton = findViewById(R.id.btn_edit_profile)
        changePasswordButton = findViewById(R.id.btn_change_password)
        attendanceSheetButton = findViewById(R.id.btn_attendance_sheet)
        logoutText = findViewById(R.id.logout_text)
        teacherName = findViewById(R.id.profile_name)
        teacherId = findViewById(R.id.profile_id_number)

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Navigation buttons
        backButton.setOnClickListener { finish() }
        homeIcon.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        calendarIcon.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
            finish()
        }
        editButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        changePasswordButton.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
        attendanceSheetButton.setOnClickListener {
            showAttendanceDownloadDialog()
        }
        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Load user profile from Firebase
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val database = FirebaseDatabase.getInstance().getReference("users").child(userId)

            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "N/A"
                    val department = snapshot.child("department").getValue(String::class.java) ?: "N/A"

                    teacherName.text = name
                    teacherId.text = department // Showing department instead of student id
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Existing methods for Attendance Download and Logout Confirmation Dialogs
    private fun showAttendanceDownloadDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.attendance_download_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val noButton = dialog.findViewById<Button>(R.id.btn_no)
        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)

        noButton.setOnClickListener { dialog.dismiss() }
        yesButton.setOnClickListener {
            dialog.dismiss()
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadSuccessDialog()
            }, 1500)
        }
        dialog.show()
    }

    private fun showDownloadSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.success_popup_attendance_download)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val okButton = dialog.findViewById<Button>(R.id.btn_ok)
        okButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.logout_confirmation_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
        val logoutButton = dialog.findViewById<Button>(R.id.btn_logout)

        cancelButton.setOnClickListener { dialog.dismiss() }
        logoutButton.setOnClickListener {
            // Sign out user from Firebase
            FirebaseAuth.getInstance().signOut()

            // Show logout success message
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()

            dialog.dismiss()

            // Navigate to login screen and clear back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        dialog.show()
    }
}