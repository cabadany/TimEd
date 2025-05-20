package com.example.timed_mobile

import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var reenterPasswordInput: TextInputEditText
    private lateinit var updatePasswordButton: Button
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout
    private lateinit var backButton: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var teacherName: TextView
    private lateinit var teacherId: TextView

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        newPasswordInput = findViewById(R.id.new_password_input)
        reenterPasswordInput = findViewById(R.id.reenter_password_input)
        updatePasswordButton = findViewById(R.id.btn_update_password)
        newPasswordLayout = findViewById(R.id.new_password_layout)
        reenterPasswordLayout = findViewById(R.id.reenter_password_layout)
        backButton = findViewById(R.id.icon_back_button)
        profileImage = findViewById(R.id.profile_image)
        teacherName = findViewById(R.id.teacher_name)
        teacherId = findViewById(R.id.teacher_id)

        updatePasswordButton.setOnClickListener {
            updatePassword()
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

        loadCurrentUserInfo()
    }

    private fun loadCurrentUserInfo() {
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(userId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val schoolId = document.getString("schoolId") ?: ""
                    val profilePhotoUrl = document.getString("profileImageUrl")

                    teacherName.text = "$firstName $lastName"
                    teacherId.text = schoolId

                    if (!profilePhotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profilePhotoUrl)
                            .placeholder(R.drawable.profile_placeholder)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.profile_placeholder)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePassword() {
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = reenterPasswordInput.text.toString().trim()

        newPasswordLayout.error = null
        reenterPasswordLayout.error = null

        if (newPassword.isEmpty()) {
            newPasswordLayout.error = "Enter a new password"
            return
        }

        if (newPassword.length < 6) {
            newPasswordLayout.error = "Password must be at least 6 characters"
            return
        }

        if (confirmPassword != newPassword) {
            reenterPasswordLayout.error = "Passwords do not match"
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && userId != null) {
            currentUser.updatePassword(newPassword).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update Firestore 'users' document password only if document exists and we have permission
                    FirebaseFirestore.getInstance().collection("users")
                        .document(userId!!)
                        .update(mapOf("password" to newPassword))
                        .addOnSuccessListener {
                            showSuccessDialog()
                        }
                        .addOnFailureListener { e ->
                            e.printStackTrace()
                            Toast.makeText(this, "Password updated but failed to save in Firestore", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_changepassword)

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}