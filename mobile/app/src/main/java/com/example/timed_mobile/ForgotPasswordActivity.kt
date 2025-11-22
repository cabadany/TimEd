package com.example.timed_mobile

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.view.animation.AnimationUtils
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPasswordActivity : WifiSecurityActivity() {
    
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_page)
        setupAnimations()

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        val emailInput = findViewById<EditText>(R.id.input_email)
        val sendButton = findViewById<Button>(R.id.btnSendReset)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)

        backButton.setOnClickListener {
            finish()
        }

        // Implement actual Firebase forgot password functionality
        sendButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Missing Email",
                    message = "Please enter your email to continue."
                )
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Invalid Email",
                    message = "Please enter a valid email address."
                )
                return@setOnClickListener
            }

            // First, check if the email exists in Firestore users collection
            performWifiChecksAndProceed {
                checkEmailExistsAndSendReset(email)
            }
        }
    }

    private fun setupAnimations() {
        val title = findViewById<TextView>(R.id.titleForgot)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)
        val formElements = listOf<View>(
            findViewById(R.id.outline_email),
            findViewById(R.id.btnSendReset)
        )

        val animSlideDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        backButton.startAnimation(animSlideDown)
        title.startAnimation(animSlideDown)

        formElements.forEachIndexed { index, view ->
            val animSlideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_form_element)
            animSlideUp.startOffset = (index * 100).toLong()
            view.startAnimation(animSlideUp)
        }

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun checkEmailExistsAndSendReset(email: String) {
        // Disable the button to prevent multiple clicks
        val sendButton = findViewById<Button>(R.id.btnSendReset)
        sendButton.isEnabled = false
        sendButton.text = "Sending..."

        // Check if email exists in Firestore users collection
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Re-enable button
                    sendButton.isEnabled = true
                    sendButton.text = "Send Reset Email"
                    
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Email Not Found",
                        message = "No account is associated with this email address."
                    )
                    return@addOnSuccessListener
                }

                // Email exists in Firestore, now send Firebase Auth reset email
                sendPasswordResetEmail(email)
            }
            .addOnFailureListener { e ->
                // Re-enable button
                sendButton.isEnabled = true
                sendButton.text = "Send Reset Email"
                
                Log.e("FORGOT_PASSWORD", "Error checking email in Firestore", e)
                UiDialogs.showErrorPopup(
                    this,
                    title = "Error",
                    message = "Unable to verify email. Please try again later."
                )
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                val sendButton = findViewById<Button>(R.id.btnSendReset)
                sendButton.isEnabled = true
                sendButton.text = "Send Reset Email"

                if (task.isSuccessful) {
                    Log.d("FORGOT_PASSWORD", "Password reset email sent successfully")
                    UiDialogs.showForgotPasswordSuccess(this, email) {
                        // Close the forgot password screen after user acknowledges
                        finish()
                    }
                } else {
                    Log.e("FORGOT_PASSWORD", "Failed to send password reset email", task.exception)
                    val errorMessage = when (task.exception) {
                        is com.google.firebase.FirebaseNetworkException ->
                            "A network error occurred. Please check your connection. If debugging, ensure your App Check debug token is registered in Firebase."
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                            "No account is associated with this email address."
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                            "Please enter a valid email address."
                        else ->
                            "Unable to send reset email. Please try again later."
                    }

                    UiDialogs.showErrorPopup(
                        this,
                        title = "Reset Failed",
                        message = errorMessage
                    )
                }
            }
    }
}
