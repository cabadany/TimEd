package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var editButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var attendanceSheetButton: Button
    private lateinit var logoutText: TextView
    private lateinit var teacherName: TextView
    private lateinit var teacherId: TextView
    private lateinit var backButton: ImageView
    private lateinit var profileEmail: TextView
    private lateinit var profileDepartment: TextView
    private lateinit var profileImage: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        editButton = findViewById(R.id.btn_edit_profile)
        changePasswordButton = findViewById(R.id.btn_change_password)
        attendanceSheetButton = findViewById(R.id.btn_attendance_sheet)
        logoutText = findViewById(R.id.logout_text)
        teacherName = findViewById(R.id.profile_name)
        teacherId = findViewById(R.id.profile_id_number)
        backButton = findViewById(R.id.icon_back_button)
        profileEmail = findViewById(R.id.profile_email)
        profileDepartment = findViewById(R.id.profile_department)
        profileImage = findViewById(R.id.profile_image)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
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

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid
        Log.d("FIREBASE_UID", "Logged-in UID: $currentUserId")

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val department = document.getString("department") ?: "N/A"
                    val email = document.getString("email") ?: "No email"
                    val profilePhotoUrl = document.getString("profileImageUrl")

                    teacherName.text = "$firstName $lastName"
                    teacherId.text = department
                    profileEmail.text = email
                    profileDepartment.text = department

                    if (!profilePhotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePhotoUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                } else {
                    Log.e("FIRESTORE", "No profile found for UID: $currentUserId")
                    Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("FIRESTORE", "Failed to retrieve profile", it)
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

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
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        dialog.show()
    }
}