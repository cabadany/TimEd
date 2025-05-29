package com.example.timed_mobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.View // Added for animation
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils // Added for animation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeInEventManualActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var codeInputLayout: TextInputLayout
    private lateinit var codeInput: TextInputEditText
    private lateinit var verifyButton: Button
    private lateinit var returnToScanButton: Button

    // Views for animation
    private lateinit var titleName: TextView
    private lateinit var manualCodeSubtitle: TextView
    private lateinit var manualCodeInstructions: TextView


    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_manual_page)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        if (userId.isNullOrEmpty()) {
            val prefs = getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
            userId = prefs.getString("userId", null)
            userEmail = prefs.getString("email", null)
            userFirstName = prefs.getString("firstName", null)
        }

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user session. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize all views, including those for animation
        backButton = findViewById(R.id.icon_back_button)
        titleName = findViewById(R.id.titleName) // Initialize for animation
        manualCodeSubtitle = findViewById(R.id.manual_code_subtitle) // Initialize for animation
        manualCodeInstructions = findViewById(R.id.manual_code_instructions) // Initialize for animation
        codeInputLayout = findViewById(R.id.code_input_layout)
        codeInput = findViewById(R.id.code_input)
        verifyButton = findViewById(R.id.btn_verify_code)
        returnToScanButton = findViewById(R.id.btn_return_to_scan)

        codeInput.setMaxLines(Integer.MAX_VALUE)
        codeInput.setHorizontallyScrolling(false)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        // --- START OF ENTRY ANIMATION CODE ---
        var currentDelay = 100L
        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val animSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        val animSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_content)

        fun animateView(view: View, animation: android.view.animation.Animation, delay: Long) {
            animation.startOffset = delay
            view.startAnimation(animation)
        }

        // 1. Back Button
        animateView(backButton, animFadeIn, currentDelay)
        currentDelay += 100L

        // 2. Title "WILDTIMed"
        animateView(titleName, animSlideDown, currentDelay)
        currentDelay += 100L

        // 3. Subtitle "Manually type the code"
        animateView(manualCodeSubtitle, animSlideDown, currentDelay)
        currentDelay += 100L

        // 4. Instructions "QR Code from the Event"
        animateView(manualCodeInstructions, animSlideDown, currentDelay)
        currentDelay += 150L

        // 5. Code Input Layout
        animateView(codeInputLayout, animSlideUp, currentDelay) // Or fade_in
        currentDelay += 100L

        // 6. Verify Code Button
        animateView(verifyButton, animSlideUp, currentDelay)
        currentDelay += 100L

        // 7. Return to Scan Button
        animateView(returnToScanButton, animSlideUp, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---


        backButton.setOnClickListener {
            (it as? ImageView)?.drawable?.let { drawable ->
                if (drawable is AnimatedVectorDrawable) drawable.start()
            }
            it.postDelayed({ finish() }, 50)
        }

        verifyButton.setOnClickListener {
            if (validateInputs()) {
                performTimeIn()
            }
        }

        returnToScanButton.setOnClickListener { finish() }

        // Consider removing this auto-verify on focus change if it's causing UX issues
        // or if the button press is the primary intended action.
        // If kept, ensure it doesn't trigger prematurely or too often.
        codeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = codeInput.text.toString().trim()
                if (code.isNotEmpty()) {
                    // It might be better to just validate and not auto-submit here
                    // validateInputs()
                    // If you want to auto-submit, ensure performTimeIn() is desired on focus loss
                    // if (validateInputs()) {
                    //     performTimeIn()
                    // }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val code = codeInput.text.toString().trim()
        if (TextUtils.isEmpty(code)) {
            codeInputLayout.error = "Please enter an event ID"
            codeInput.requestFocus() // Keep focus if error
            return false
        }
        // Optional: Add length check if event codes have a fixed length
        // if (code.length != 8) { // Example length
        //     codeInputLayout.error = "Event ID must be 8 characters"
        //     codeInput.requestFocus()
        //     return false
        // }
        codeInputLayout.error = null
        return true
    }

    private fun performTimeIn() {
        verifyButton.isEnabled = false
        verifyButton.text = "Verifying..." // Consider using a string resource
        val eventId = codeInput.text.toString().trim().uppercase(Locale.ROOT) // Standardize to uppercase
        val db = FirebaseFirestore.getInstance()

        // First, check if the event itself exists
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { eventDocument ->
                if (!eventDocument.exists()) {
                    showErrorDialog("Event ID '$eventId' not found. Please check the code.")
                    verifyButton.isEnabled = true
                    verifyButton.text = "Verify Code" // Reset button
                    return@addOnSuccessListener
                }

                // Event exists, now check if user already timed in for this event in "attendanceRecords"
                // Assuming "attendanceRecords" is the central collection for all attendance.
                db.collection("attendanceRecords")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("eventId", eventId)
                    .whereEqualTo("type", "event_time_in") // Ensure it's an event time-in
                    .limit(1) // We only need to know if one exists
                    .get()
                    .addOnSuccessListener { existingRecords ->
                        if (!existingRecords.isEmpty) {
                            showErrorDialog("You have already timed in for this event.")
                            verifyButton.isEnabled = true
                            verifyButton.text = "Verify Code" // Reset button
                        } else {
                            // Not timed in yet, proceed to log attendance
                            logAttendanceToFirestore(eventId, eventDocument.getString("eventName") ?: "Unknown Event")
                        }
                    }
                    .addOnFailureListener { e ->
                        showErrorDialog("Failed to check existing records: ${e.message}")
                        verifyButton.isEnabled = true
                        verifyButton.text = "Verify Code" // Reset button
                    }
            }
            .addOnFailureListener { e ->
                showErrorDialog("Failed to verify event ID: ${e.message}")
                verifyButton.isEnabled = true
                verifyButton.text = "Verify Code" // Reset button
            }
    }

    private fun logAttendanceToFirestore(eventId: String, eventName: String) {
        val db = FirebaseFirestore.getInstance()
        // val timestamp = System.currentTimeMillis() // Using Firestore server timestamp is better
        // val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val attendanceData = hashMapOf(
            "userId" to userId,
            "firstName" to userFirstName,
            "email" to userEmail,
            "eventId" to eventId, // Add eventId to the record
            "eventName" to eventName, // Add eventName to the record
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(), // Use server timestamp
            "type" to "event_time_in_manual", // Differentiate manual time-in
            "hasTimedOut" to false,
            "selfieUrl" to null // Manual time-in typically won't have a selfie
        )

        // Log to the central "attendanceRecords" collection
        db.collection("attendanceRecords")
            .add(attendanceData) // Use add() to generate a unique document ID
            .addOnSuccessListener { documentReference ->
                // Optionally, trigger certificate email if applicable for manual time-ins
                userEmail?.let { email ->
                    userFirstName?.let { name ->
                        // You might need a modified triggerCertificateEmail or a flag to indicate manual
                        // For now, let's assume manual time-ins might also get certificates
                        // This depends on your app's logic for certificates.
                        // triggerCertificateEmail(eventName, eventId, email, name, documentReference.id)
                        // For simplicity, just show success for now.
                    }
                }
                showSuccessDialog(eventName)
            }
            .addOnFailureListener { e ->
                showErrorDialog("Failed to save attendance: ${e.message}")
                verifyButton.isEnabled = true
                verifyButton.text = "Verify Code" // Reset button
            }
    }

    private fun showSuccessDialog(eventName: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in) // Re-use existing success popup
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        // Customize message for manual time-in
        titleText.text = getString(R.string.popup_title_success_manual_time_in) // "Successfully Timed In"
        messageText.text = getString(R.string.popup_message_success_manual_time_in, eventName) // "Your attendance for '$eventName' has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK) // Indicate success to previous activity if needed
            finish()
        }

        dialog.show()
    }

    private fun showErrorDialog(errorMessage: String) {
        if (!isFinishing && !isDestroyed) {
            verifyButton.isEnabled = true
            verifyButton.text = "Verify Code" // Reset button text
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }
}