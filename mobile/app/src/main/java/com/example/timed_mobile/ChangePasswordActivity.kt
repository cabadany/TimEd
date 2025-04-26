package com.example.timed_mobile

import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var reenterPasswordInput: TextInputEditText
    private lateinit var updatePasswordButton: Button
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_page)

        // Start top wave animation
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

        updatePasswordButton.setOnClickListener {
            updatePassword()
        }

        backButton.setOnClickListener {
            finish()
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

        FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showSuccessDialog()
            } else {
                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
            }
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