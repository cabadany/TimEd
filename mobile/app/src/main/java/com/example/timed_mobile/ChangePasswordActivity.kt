package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var reenterPasswordInput: TextInputEditText
    private lateinit var updatePasswordButton: Button
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_page)

        // Initialize views
        backButton = findViewById(R.id.icon_back_button)
        newPasswordInput = findViewById(R.id.new_password_input)
        reenterPasswordInput = findViewById(R.id.reenter_password_input)
        updatePasswordButton = findViewById(R.id.btn_update_password)
        newPasswordLayout = findViewById(R.id.new_password_layout)
        reenterPasswordLayout = findViewById(R.id.reenter_password_layout)

        // Set up back button
        backButton.setOnClickListener {
            finish()
        }

        // Set up update password button
        updatePasswordButton.setOnClickListener {
            updatePassword()
        }
    }

    private fun updatePassword() {
        val newPassword = newPasswordInput.text.toString().trim()
        val reenterPassword = reenterPasswordInput.text.toString().trim()

        // Reset error states
        newPasswordLayout.error = null
        reenterPasswordLayout.error = null

        // Validate inputs
        if (newPassword.isEmpty()) {
            newPasswordLayout.error = "Please enter a new password"
            return
        }

        if (newPassword.length < 6) {
            newPasswordLayout.error = "Password must be at least 6 characters"
            return
        }

        if (reenterPassword.isEmpty()) {
            reenterPasswordLayout.error = "Please re-enter your new password"
            return
        }

        if (newPassword != reenterPassword) {
            reenterPasswordLayout.error = "Passwords do not match"
            return
        }

        // Show success dialog
        showPasswordUpdatedDialog()
    }

    private fun showPasswordUpdatedDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        // Set up the dialog view
        dialog.setContentView(R.layout.success_popup_changepassword)

        // Update title and message for password update
        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)

        titleText.text = "Password Updated"
        messageText.text = "Your password has been successfully changed."

        // Set transparent background and dim amount
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        // Find and setup close button
        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            // Return to ProfileActivity
            finish()
        }

        dialog.show()
    }
}