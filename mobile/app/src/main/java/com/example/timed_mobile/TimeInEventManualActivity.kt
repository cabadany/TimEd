package com.example.timed_mobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeInEventManualActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var codeInput: TextInputEditText
    private lateinit var verifyButton: Button
    private lateinit var returnToScanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_manual_page)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        // Initialize views
        backButton = findViewById(R.id.icon_back_button)
        codeInputLayout = findViewById(R.id.code_input_layout)
        codeInput = findViewById(R.id.code_input)
        verifyButton = findViewById(R.id.btn_verify_code)
        returnToScanButton = findViewById(R.id.btn_return_to_scan)

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Start bottom wave animation
        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        val bottomDrawable = bottomWave.drawable
        if (bottomDrawable is AnimatedVectorDrawable) {
            bottomDrawable.start()
        }

        // Set up back button with animation
        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish() // Return to previous screen
            }, 50)
        }

        // Set up verify button in TimeInEventManualActivity.kt
        verifyButton.setOnClickListener {
            if (validateInputs()) {
                // Show a loading state on button
                verifyButton.isEnabled = false
                verifyButton.text = "Verifying..."

                // Get the entered code
                val eventCode = codeInput.text.toString().trim()

                // Simulate verification process
                Handler(Looper.getMainLooper()).postDelayed({
                    // Simulate code verification
                    if (eventCode.equals("TEST123", ignoreCase = true) ||
                        eventCode.equals("WILD123", ignoreCase = true)) {
                        // Valid test codes
                        logAttendanceToFirebase(eventCode)
                        showSuccessDialog()
                    } else {
                        // Invalid code
                        showErrorDialog("Invalid event code. Please try again.")
                    }
                }, 1000)
            }
        }

        // Set up return to scanner button
        returnToScanButton.setOnClickListener {
            finish() // Simply go back to QR scanner activity
        }
    }

    /**
     * Validate user inputs before proceeding
     */
    private fun validateInputs(): Boolean {
        val code = codeInput.text.toString().trim()

        // Check if code field is empty
        if (TextUtils.isEmpty(code)) {
            codeInputLayout.error = "Please enter an event code"
            codeInput.requestFocus()
            return false
        }

        // Validate code format (example: must be at least 4 characters)
        if (code.length < 4) {
            codeInputLayout.error = "Code must be at least 4 characters"
            codeInput.requestFocus()
            return false
        }

        // Clear any previous errors
        codeInputLayout.error = null
        return true
    }

    /**
     * Log attendance record to Firebase
     */
    private fun logAttendanceToFirebase(eventCode: String) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val database = FirebaseDatabase.getInstance().reference
                val timestamp = System.currentTimeMillis()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(timestamp))

                val attendanceData = hashMapOf(
                    "eventCode" to eventCode,
                    "timestamp" to timestamp,
                    "date" to formattedDate,
                    "method" to "manual",
                    "userId" to userId
                )

                database.child("eventAttendance").child(userId).push().setValue(attendanceData)
            }
        } catch (e: Exception) {
            // Silent failure - we're logging attendance as a best-effort operation
        }
    }

    /**
     * Show success dialog after verification
     */
    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        titleText.text = "Successfully Verified"
        messageText.text = "Your attendance has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish() // Return to main activity after success
        }

        dialog.show()
    }

    /**
     * Show error dialog for invalid code
     */
    private fun showErrorDialog(errorMessage: String) {
        // Re-enable button
        verifyButton.isEnabled = true
        verifyButton.text = "Verify Code"

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
}