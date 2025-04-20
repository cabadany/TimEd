package com.example.timed_mobile

import android.app.Dialog
import android.os.Bundle
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var departmentInput: TextInputEditText
    private lateinit var updateProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile_page)

        nameInput = findViewById(R.id.name_input)
        emailInput = findViewById(R.id.email_input)
        phoneInput = findViewById(R.id.phone_input)
        departmentInput = findViewById(R.id.department_input)
        updateProfileButton = findViewById(R.id.btn_update_profile)

        updateProfileButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val department = departmentInput.text.toString().trim()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val profileData = mapOf(
            "name" to name,
            "email" to email,
            "phone" to phone,
            "department" to department
        )

        database.setValue(profileData).addOnSuccessListener {
            showUpdateSuccessDialog()
        }.addOnFailureListener {
            Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUpdateSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_edit_profile)

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}