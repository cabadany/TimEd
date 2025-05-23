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
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val departmentMap = document.get("department")
                val departmentName = if (departmentMap is Map<*, *>) departmentMap["name"].toString() else "N/A"
                val idNumber = document.getString("schoolId") ?: "N/A"
                val email = document.getString("email") ?: "No email"
                val profilePictureUrl = document.getString("profilePictureUrl")

                teacherName.text = "$firstName $lastName"
                teacherId.text = idNumber
                profileEmail.text = email
                profileDepartment.text = departmentName

                val timeLogsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("timeLogs").child(userId)

                timeLogsRef.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            var timeInImageUrl: String? = null
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                                val todayStart = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    timeInImageUrl = child.child("imageUrl").getValue(String::class.java)
                                    break
                                }
                            }

                            val imageToLoad = when {
                                !timeInImageUrl.isNullOrEmpty() -> timeInImageUrl
                                !profilePictureUrl.isNullOrEmpty() -> profilePictureUrl
                                else -> null
                            }

                            if (!imageToLoad.isNullOrEmpty()) {
                                Glide.with(this@ProfileActivity)
                                    .load(imageToLoad)
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(profileImage)
                            } else {
                                profileImage.setImageResource(R.drawable.profile_placeholder)
                            }
                        }

                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                        }
                    })
            }
            .addOnFailureListener {
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
            getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        dialog.show()
    }
}