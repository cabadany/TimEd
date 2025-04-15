package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var editProfilePicButton: ImageView
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var departmentInput: TextInputEditText
    private lateinit var updateProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile_page)

        // Initialize views
        backButton = findViewById(R.id.icon_back_button)
        editProfilePicButton = findViewById(R.id.edit_profile_pic_button)
        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        departmentInput = findViewById(R.id.department_input)
        updateProfileButton = findViewById(R.id.btn_update_profile)

        // Set up back button click listener
        backButton.setOnClickListener {
            finish()
        }

        // Set up profile picture edit button
        editProfilePicButton.setOnClickListener {
            // In a real app, this would open a gallery picker or camera
            Toast.makeText(this, "Change profile picture", Toast.LENGTH_SHORT).show()
        }

        // Set up update profile button
        updateProfileButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val department = departmentInput.text.toString().trim()

        // Basic validation
        if (name.isEmpty()) {
            nameInput.error = "Name cannot be empty"
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Valid email address required"
            return
        }

        if (phone.isEmpty()) {
            phoneInput.error = "Phone number cannot be empty"
            return
        }

        if (department.isEmpty()) {
            departmentInput.error = "Department cannot be empty"
            return
        }

        // Show success dialog
        showUpdateSuccessDialog()
    }

    private fun showUpdateSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Set up the dialog view
        dialog.setContentView(R.layout.success_popup_edit_profile)

        // Update title and message for profile update
        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)

        titleText.text = "Profile Updated"
        messageText.text = "Your profile has been successfully updated."

        // Set transparent background and dim amount
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Find and setup close button
        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            // Return to ProfileActivity with updated info
            finish()
        }

        dialog.show()
    }
}