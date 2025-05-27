package com.example.timed_mobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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

        backButton = findViewById(R.id.icon_back_button)
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

        codeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = codeInput.text.toString().trim()
                if (code.isNotEmpty()) {
                    if (validateInputs()) {
                        performTimeIn()
                    }
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val code = codeInput.text.toString().trim()
        if (TextUtils.isEmpty(code)) {
            codeInputLayout.error = "Please enter an event ID"
            codeInput.requestFocus()
            return false
        }
        codeInputLayout.error = null
        return true
    }

    private fun performTimeIn() {
        verifyButton.isEnabled = false
        verifyButton.text = "Verifying..."
        val eventId = codeInput.text.toString().trim()
        val db = FirebaseFirestore.getInstance()

        db.collection("events")
            .document(eventId)
            .collection("attendees")
            .document(userId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    showErrorDialog("You have already timed in for this event.")
                } else {
                    logAttendanceToFirestore(eventId)
                }
            }
            .addOnFailureListener {
                showErrorDialog("Failed to check for duplicates: ${it.message}")
            }
    }

    private fun logAttendanceToFirestore(eventId: String) {
        val db = FirebaseFirestore.getInstance()
        val timestamp = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val attendanceData = hashMapOf(
            "userId" to userId,
            "firstName" to userFirstName,
            "email" to userEmail,
            "timestamp" to formattedDate,
            "type" to "event_time_in",
            "hasTimedOut" to false
        )

        db.collection("events")
            .document(eventId)
            .collection("attendees")
            .document(userId!!)
            .set(attendanceData)
            .addOnSuccessListener {
                showSuccessDialog()
            }
            .addOnFailureListener {
                showErrorDialog("Failed to save attendance: ${it.message}")
            }
    }

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
            finish()
        }

        dialog.show()
    }

    private fun showErrorDialog(errorMessage: String) {
        verifyButton.isEnabled = true
        verifyButton.text = "Verify Code"
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
}