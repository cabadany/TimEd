package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils // Ensure this is present if animations are used
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
// import androidx.core.net.toUri // Not used directly, can be removed if not needed elsewhere
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.example.timed_mobile.HomeActivity
import com.example.timed_mobile.tutorial.EventTutorialState
// import com.google.mlkit.vision.barcode.BarcodeScannerOptions // Not used, can be removed
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.net.URL
import java.net.HttpURLConnection

// import android.widget.Toast // Already imported
// import androidx.camera.core.ImageProxy // Already imported

class TimeInEventActivity : WifiSecurityActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner
    private lateinit var cameraContainer: CardView
    private lateinit var cameraPreviewHostFrame: FrameLayout
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var cameraPlaceholder: ImageView
    private lateinit var backButton: ImageView
    private lateinit var scanButton: Button
    private lateinit var shutterButton: ImageView
    private lateinit var selfieReminder: TextView
    private lateinit var scannerOverlay: View
    private lateinit var titleName: TextView // Added for animation

    private var imageCapture: ImageCapture? = null
    private var isScanningEnabled = false
    private var isQrScanned = false
    private var isFrontCamera = false

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    private var currentScannedEventId: String? = null
    private var currentScannedEventName: String? = null

    private lateinit var manualCodeButton: Button

    companion object {
        private const val TAG = "TimeInEventActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_page)

        // Initialize user data first
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
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Missing user session. Please log in again.") { finish() }
            return
        }

        // Ensure Firebase Auth user is authenticated before proceeding
        ensureFirebaseAuthUser {
            // Initialize the rest of the activity after authentication
            initializeActivity()
        }
    }

    private fun initializeActivity() {
        handleDeepLinkIfPresent()

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        initializeViews()

        // --- START OF ENTRY ANIMATION CODE --- (Assuming this was added in a previous step)
        var currentDelay = 100L
        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        animateView(backButton, R.anim.fade_in, currentDelay); currentDelay += 100L
        animateView(titleName, R.anim.slide_down_fade_in, currentDelay); currentDelay += 150L
        animateView(cameraContainer, R.anim.fade_in, currentDelay); currentDelay += 150L
        animateView(shutterButton, R.anim.fade_in, currentDelay); currentDelay += 100L // Or slide_up_fade_in_content
        animateView(selfieReminder, R.anim.slide_up_fade_in_content, currentDelay); currentDelay += 100L
        animateView(scanButton, R.anim.slide_up_fade_in_content, currentDelay); currentDelay += 100L
        animateView(manualCodeButton, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---

        cameraPreviewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        if (cameraPreviewView.parent == null) { // Ensure not added multiple times
            cameraPreviewHostFrame.addView(cameraPreviewView, 0)
        }

        barcodeScanner = BarcodeScanning.getClient()

        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            setupCameraPreview()
            startCameraPreviewOnly()
            scanButton.text = getString(R.string.button_start_scanning)
            selfieReminder.text = getString(R.string.timein_event_qr_scan_instruction)
            scannerOverlay.visibility = View.GONE
            shutterButton.visibility = View.GONE
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun ensureFirebaseAuthUser(onComplete: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d(TAG, "No Firebase Auth user found, signing in anonymously...")
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener { authResult ->
                    Log.d(TAG, "Anonymous sign-in successful: ${authResult.user?.uid}")
                    onComplete()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Anonymous sign-in failed", e)
                    UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Authentication failed. Please try again.") { finish() }
                }
        } else {
            Log.d(TAG, "Firebase Auth user already exists: ${currentUser.uid}")
            onComplete()
        }
    }

    private fun sendCertificateEmail(eventName: String, eventId: String, userEmail: String, userName: String) {
        Log.d(TAG, "sendCertificateEmail called for event: $eventName, email: $userEmail")

        val db = FirebaseFirestore.getInstance()
        val certificateData = hashMapOf(
            "recipientEmail" to userEmail,
            "recipientName" to userName,
            "eventId" to eventId,
            "eventName" to eventName,
            "issueDate" to FieldValue.serverTimestamp(),
            "certificateType" to "attendance",
            "status" to "pending" // Cloud function will process this
        )

        db.collection("certificates")
            .add(certificateData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Certificate request created with ID: ${documentReference.id}")
                // Optionally, trigger a mail queue if your backend uses one
                val mailQueueData = hashMapOf(
                    "to" to userEmail,
                    "template" to mapOf(
                        "name" to "event_certificate_notification", // Example template name
                        "data" to mapOf(
                            "userName" to userName,
                            "eventName" to eventName,
                            "eventId" to eventId,
                            "issueDate" to SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                        )
                    ),
                    "status" to "queued",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("mail_queue").add(mailQueueData) // Example mail queue
                    .addOnSuccessListener { Log.d(TAG, "Email queued for certificate.") }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to queue certificate email", e) }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to create certificate request", exception)
                Toast.makeText(this, "Failed to process certificate request", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleQrCodeScannedUpdated(qrContent: String) {
        Log.d(TAG, "handleQrCodeScannedUpdated called with: $qrContent")
        if (isQrScanned) {
            Log.w(TAG, "QR already processed or being processed. Ignoring.")
            return
        }
        vibrate()

        // Extract eventId using the same logic as frontend QRJoin.jsx
        val eventId = if (qrContent.contains("/")) {
            // Handle URL format like "https://timedsystem.netlify.app/qr-join/L6ViIgyOFgRnSo8OyutU"
            qrContent.substringAfterLast("/").trim()
        } else {
            // Handle the TIMED:EVENT:eventId format like frontend
            val parts = qrContent.split(":")
            if (parts.size >= 3 && parts[0] == "TIMED" && parts[1] == "EVENT") {
                parts[2].trim()
            } else {
                qrContent.trim() // Fallback to using the whole content as eventId
            }
        }
        
        if (eventId.isEmpty()) {
            showErrorDialog("Invalid QR code format.")
            Log.e(TAG, "Invalid QR code format, eventId is empty from: $qrContent")
            resetForNewScan() // Allow user to try again
            return
        }

        // Ensure we have the userId from SharedPreferences (equivalent to frontend's localStorage)
        if (userId.isNullOrEmpty()) {
            val prefs = getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
            userId = prefs.getString("userId", null)
            userEmail = prefs.getString("email", null)
            userFirstName = prefs.getString("firstName", null)
            
            if (userId.isNullOrEmpty()) {
                showErrorDialog("User session not found. Please log in again.")
                Log.e(TAG, "No userId found in SharedPreferences")
                finish()
                return
            }
        }

        Log.d(TAG, "Processing QR scan for eventId: $eventId, userId: $userId")

        isQrScanned = true // Set flag early to prevent re-entry
        scanButton.isEnabled = false // Disable button during processing
        stopQrScannerAndAnalysis() // Stop camera processing

        val db = FirebaseFirestore.getInstance()
        val eventDocRef = db.collection("events").document(eventId)

        selfieReminder.text = "Joining event...please wait" // Update UI like frontend

        eventDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val eventName = document.getString("eventName") ?: "Unnamed Event"
                    currentScannedEventId = eventId // Store for later use (e.g., selfie)
                    currentScannedEventName = eventName

                    // Show "Joining event" message for a moment, then proceed to selfie
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Skip the duplicate check - let backend handle it
                        // Directly proceed to selfie mode like frontend after showing joining message
                        selfieReminder.text = "Event '$eventName' found. Please take a selfie."
                        scanButton.text = getString(R.string.button_take_selfie)
                        scanButton.isEnabled = true
                        shutterButton.visibility = View.VISIBLE
                        switchToSelfieMode() // Switch to front camera
                    }, 1500) // Show "Joining event...please wait" for 1.5 seconds
                } else {
                    showErrorDialog("Event with ID '$eventId' not found.")
                    Log.e(TAG, "Event not found for ID: $eventId")
                    resetForNewScan()
                }
            }
            .addOnFailureListener { e ->
                showErrorDialog("Failed to fetch event info: ${e.message}")
                Log.e(TAG, "Failed to fetch event info for ID: $eventId", e)
                resetForNewScan()
            }
    }


    private fun logTimeInToFirestoreUpdated(selfieUrl: String, timestampPhoto: String) {
        Log.d(TAG, "logTimeInToFirestoreUpdated called. Selfie URL: $selfieUrl")

        val eventNameForLog = currentScannedEventName ?: "Unknown Event"
        val eventIdForLog = currentScannedEventId ?: run {
            Log.e(TAG, "currentScannedEventId is null in logTimeInToFirestoreUpdated")
            Toast.makeText(this, "Error: Event ID missing. Cannot log time-in.", Toast.LENGTH_LONG).show()
            resetForNewScan()
            return
        }

        selfieReminder.text = "Processing attendance..." // Update UI

        // First, validate that the user exists in the backend users collection
        validateUserAndCallAPI(eventIdForLog, selfieUrl, timestampPhoto)
    }
    
    private fun validateUserAndCallAPI(eventId: String, selfieUrl: String, timestamp: String) {
        Log.d(TAG, "Validating user exists in backend before API call...")
        selfieReminder.text = "Validating user..."
        
        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "Cannot validate user: userId is null or empty")
            showErrorDialog("User session error. Please log in again.")
            return
        }
        
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId!!)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc.exists()) {
                    val userEmail = userDoc.getString("email")
                    val userFirstName = userDoc.getString("firstName")
                    
                    Log.d(TAG, "User validation SUCCESS:")
                    Log.d(TAG, "- User exists in backend")
                    Log.d(TAG, "- Email: $userEmail")
                    Log.d(TAG, "- FirstName: $userFirstName")
                    
                    // Update local variables with backend data (in case they differ)
                    this.userEmail = userEmail
                    this.userFirstName = userFirstName
                    
                    // Now call the backend API
                    callBackendAttendanceAPI(eventId, selfieUrl, timestamp)
                } else {
                    Log.e(TAG, "User validation FAILED: User document does not exist in backend")
                    Log.e(TAG, "Looking for userId: $userId in users collection")
                    
                    // User doesn't exist in backend, but they're logged in locally
                    // This might be a sync issue, but we should still try the API call
                    Log.w(TAG, "User not found in backend, but trying API call anyway")
                    callBackendAttendanceAPI(eventId, selfieUrl, timestamp)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "User validation ERROR: Failed to check user existence", e)
                // Even if validation fails, try the API call
                Log.w(TAG, "Validation failed, but trying API call anyway")
                callBackendAttendanceAPI(eventId, selfieUrl, timestamp)
            }
    }

    private fun callBackendAttendanceAPI(eventId: String, selfieUrl: String, timestamp: String) {
        // Use the same API endpoint as the frontend: POST /api/attendance/{eventId}/{userId}
        val apiBaseUrl = "https://timed-utd9.onrender.com/api"
        val attendanceUrl = "$apiBaseUrl/attendance/$eventId/$userId"
        
        // Log detailed information before API call
        Log.d(TAG, "=== CALLING BACKEND API ===")
        Log.d(TAG, "API URL: $attendanceUrl")
        Log.d(TAG, "EventId: $eventId")
        Log.d(TAG, "UserId: $userId")
        Log.d(TAG, "UserEmail: $userEmail")
        Log.d(TAG, "UserFirstName: $userFirstName")
        Log.d(TAG, "SelfieUrl: $selfieUrl")
        Log.d(TAG, "========================")
        
        selfieReminder.text = "Contacting server..."
        
        // Verify all required data is present
        if (userId.isNullOrEmpty()) {
            Log.e(TAG, "ERROR: userId is null or empty!")
            runOnUiThread {
                showErrorDialog("User session error. Please log in again.")
            }
            return
        }
        
        if (eventId.isEmpty()) {
            Log.e(TAG, "ERROR: eventId is empty!")
            runOnUiThread {
                showErrorDialog("Event ID error. Please scan QR code again.")
            }
            return
        }
        
        // Create a simple HTTP client request
        Thread {
            try {
                val url = java.net.URL(attendanceUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000 // Increased timeout
                connection.readTimeout = 15000 // Increased timeout
                
                Log.d(TAG, "Making HTTP POST request to: $attendanceUrl")
                
                val responseCode = connection.responseCode
                val responseMessage = if (responseCode == 200) {
                    val inputStream = connection.inputStream
                    inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        errorStream.bufferedReader().use { it.readText() }
                    } else {
                        "HTTP Error: $responseCode"
                    }
                }
                
                Log.d(TAG, "=== BACKEND API RESPONSE ===")
                Log.d(TAG, "Response Code: $responseCode")
                Log.d(TAG, "Response Message: $responseMessage")
                Log.d(TAG, "==========================")
                
                // Handle response on UI thread
                runOnUiThread {
                    handleBackendResponse(responseCode, responseMessage, eventId)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "=== API CALL ERROR ===")
                Log.e(TAG, "Error calling backend API", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "==================")
                runOnUiThread {
                    selfieReminder.text = "Network error. Trying alternative..."
                    // Fallback to manual Firestore recording
                    recordAttendanceToFirestore(eventId, selfieUrl, timestamp)
                }
            }
        }.start()
    }
    
    private fun notifyEventTutorialTimeInCompleted() {
        if (EventTutorialState.isActive(this)) {
            EventTutorialState.completeStep(this, 3, markCompleted = false)
            EventTutorialState.setExpectedAction(this, HomeActivity.ACTION_EVENT_TIME_OUT)
        }
    }

    private fun handleBackendResponse(responseCode: Int, responseMessage: String, eventId: String) {
        when (responseCode) {
            200 -> {
                if (responseMessage.contains("Already timed in", ignoreCase = true)) {
                    AlertDialog.Builder(this)
                        .setTitle("Already Timed In")
                        .setMessage("You have already timed in for '${currentScannedEventName}' and received a certificate.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            notifyEventTutorialTimeInCompleted()
                            startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else if (responseMessage.contains("No attendee record found", ignoreCase = true)) {
                    // Attendance was marked but certificate generation failed due to Firestore eventual consistency
                    // Retry after a short delay
                    selfieReminder.text = "Attendance recorded. Processing certificate..."
                    Handler(Looper.getMainLooper()).postDelayed({
                        retryAttendanceAPI(eventId)
                    }, 2000) // Wait 2 seconds for Firestore consistency
                } else {
                    // Success - attendance recorded and certificate email sent
                    AlertDialog.Builder(this)
                        .setTitle("Time-In Recorded")
                        .setMessage("Successfully timed in for '${currentScannedEventName}'! A certificate will be sent to your email.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            notifyEventTutorialTimeInCompleted()
                            val resultIntent = Intent()
                            setResult(RESULT_OK, resultIntent)
                            startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
            else -> {
                Log.e(TAG, "Backend API error: $responseCode - $responseMessage")
                selfieReminder.text = "Server error. Trying alternative..."
                // Fallback to manual recording
                recordAttendanceToFirestore(eventId, "", "")
            }
        }
    }
    
    private fun retryAttendanceAPI(eventId: String) {
        selfieReminder.text = "Retrying certificate generation..."
        
        Log.d(TAG, "=== RETRYING API CALL ===")
        Log.d(TAG, "EventId: $eventId")
        Log.d(TAG, "UserId: $userId") 
        Log.d(TAG, "========================")
        
        // Call the backend API again to trigger certificate generation
        val apiBaseUrl = "https://timed-utd9.onrender.com/api"
        val attendanceUrl = "$apiBaseUrl/attendance/$eventId/$userId"
        
        Thread {
            try {
                val url = java.net.URL(attendanceUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000 // Increased timeout
                connection.readTimeout = 15000 // Increased timeout
                
                Log.d(TAG, "Making RETRY HTTP POST request to: $attendanceUrl")
                
                val responseCode = connection.responseCode
                val responseMessage = if (responseCode == 200) {
                    val inputStream = connection.inputStream
                    inputStream.bufferedReader().use { it.readText() }
                } else {
                    val errorStream = connection.errorStream
                    if (errorStream != null) {
                        errorStream.bufferedReader().use { it.readText() }
                    } else {
                        "HTTP Error: $responseCode"
                    }
                }
                
                Log.d(TAG, "=== RETRY API RESPONSE ===")
                Log.d(TAG, "Response Code: $responseCode")
                Log.d(TAG, "Response Message: $responseMessage")
                Log.d(TAG, "========================")
                
                runOnUiThread {
                    if (responseCode == 200 && responseMessage.contains("Already timed in", ignoreCase = true)) {
                        // Perfect! This means the attendance record is now found and certificate was processed
                        Log.d(TAG, "SUCCESS: Retry found attendance record, certificate processed!")
                        AlertDialog.Builder(this)
                            .setTitle("Time-In Recorded")
                            .setMessage("Successfully timed in for '${currentScannedEventName}'! A certificate will be sent to your email.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                notifyEventTutorialTimeInCompleted()
                                val resultIntent = Intent()
                                setResult(RESULT_OK, resultIntent)
                                startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else if (responseMessage.contains("No attendee record found", ignoreCase = true)) {
                        // Still not found after retry, but consider it successful since attendance was recorded
                        Log.w(TAG, "WARNING: Still no attendee record found after retry, but treating as success")
                        AlertDialog.Builder(this)
                            .setTitle("Time-In Recorded")
                            .setMessage("Successfully timed in for '${currentScannedEventName}'! Certificate will be processed shortly.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                notifyEventTutorialTimeInCompleted()
                                val resultIntent = Intent()
                                setResult(RESULT_OK, resultIntent)
                                startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        // Some other response, treat as success
                        Log.d(TAG, "SUCCESS: Other response received, treating as success")
                        AlertDialog.Builder(this)
                            .setTitle("Time-In Recorded")
                            .setMessage("Successfully timed in for '${currentScannedEventName}'! A certificate will be sent to your email.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                notifyEventTutorialTimeInCompleted()
                                val resultIntent = Intent()
                                setResult(RESULT_OK, resultIntent)
                                startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "=== RETRY API ERROR ===")
                Log.e(TAG, "Error in retry API call", e)
                Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "=====================")
                runOnUiThread {
                    // Even if retry fails, consider the original attendance as successful
                    AlertDialog.Builder(this)
                        .setTitle("Time-In Recorded")
                        .setMessage("Successfully timed in for '${currentScannedEventName}'! Certificate will be processed shortly.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            notifyEventTutorialTimeInCompleted()
                            val resultIntent = Intent()
                            setResult(RESULT_OK, resultIntent)
                            startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }.start()
    }
    
    private fun recordAttendanceToFirestore(eventId: String, selfieUrl: String, timestamp: String) {
        Log.d(TAG, "Using fallback Firestore recording")
        
        val record = hashMapOf(
            "userId" to userId,
            "firstName" to userFirstName,
            "email" to userEmail,
            "eventId" to eventId,
            "eventName" to currentScannedEventName,
            "timestamp" to FieldValue.serverTimestamp(),
            "selfieUrl" to selfieUrl,
            "type" to "event_time_in",
            "hasTimedOut" to false,
            // Indicate this was via QR/selfie (false = QR/selfie)
            "checkinMethod" to false
        )

        val db = FirebaseFirestore.getInstance()
        val documentId = "${userId}_${eventId}_event_time_in"
        
        db.collection("attendanceRecords")
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    AlertDialog.Builder(this)
                        .setTitle("Already Timed In")
                        .setMessage("You have already timed in for '${currentScannedEventName}' and received a certificate.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    db.collection("attendanceRecords")
                        .document(documentId)
                        .set(record)
                        .addOnSuccessListener {
                            Log.d(TAG, "Fallback: Attendance record saved to Firestore")
                            AlertDialog.Builder(this)
                                .setTitle("Time-In Recorded")
                                .setMessage("Successfully timed in for '${currentScannedEventName}'! Processing certificate...")
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    notifyEventTutorialTimeInCompleted()
                                    startActivity(Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) })
                                    finish()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Fallback: Failed to save attendance", e)
                            showErrorDialog("Failed to save attendance: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fallback: Failed to check existing record", e)
                showErrorDialog("Failed to process attendance: ${e.message}")
            }
    }


    private fun handleDeepLinkIfPresent() {
        val data: Uri? = intent?.data
        Log.d(TAG, "handleDeepLinkIfPresent: Data URI is $data")
        val eventIdFromLink = data?.getQueryParameter("eventId") ?: data?.lastPathSegment // More robust parsing

        if (eventIdFromLink != null) {
            Log.d(TAG, "Deep link detected for eventId: $eventIdFromLink")
            // Store it to be processed after views are initialized and user is confirmed
            // This is a simplified direct handling. Consider a more robust flow.
            // For now, we assume if a deep link is present, we try to process it immediately
            // if the user is already logged in.

            val prefs = getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
            val currentUserId = prefs.getString("userId", null)
            val currentUserEmail = prefs.getString("email", null)
            val currentUserFirstName = prefs.getString("firstName", null)


            if (currentUserId.isNullOrEmpty()) {
                UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Please log in to time-in for the event.") { finish() }
                return
            }
            // If user details are not yet set from intent, use from prefs
            if (userId.isNullOrEmpty()) userId = currentUserId
            if (userEmail.isNullOrEmpty()) userEmail = currentUserEmail
            if (userFirstName.isNullOrEmpty()) userFirstName = currentUserFirstName


            // Directly call the QR scanned handler logic
            // This bypasses the need for actual QR scanning if a deep link is used.
            // Ensure this is the desired behavior.
            // We need to set isQrScanned to false before calling to allow processing.
            isQrScanned = false
            handleQrCodeScannedUpdated(eventIdFromLink) // Pass the eventId as if it was scanned

        } else {
            Log.d(TAG, "No deep link data found or eventId missing in deep link.")
        }
    }

    private fun initializeViews() {
        cameraContainer = findViewById(R.id.camera_container)
        cameraPreviewHostFrame = findViewById(R.id.camera_preview_host_frame)
        cameraPlaceholder = findViewById(R.id.camera_preview_placeholder)
        backButton = findViewById(R.id.icon_back_button)
        scanButton = findViewById(R.id.scan_qr_code)
        shutterButton = findViewById(R.id.icon_shutter_camera)
        selfieReminder = findViewById(R.id.qr_scanner_click_reminder)
        scannerOverlay = findViewById(R.id.qr_scan_box_overlay)
        manualCodeButton = findViewById(R.id.manual_code_time_in)
        titleName = findViewById(R.id.titleName) // Initialize titleName
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            // Decide where to go back. If mid-process, maybe confirm.
            // For now, simple finish or go home.
            finish() // Or navigate to HomeActivity
        }

        scanButton.setOnClickListener {
            if (isQrScanned) { // This means QR was scanned, now it's "Take Selfie" or "Reset"
                if (scanButton.text.toString().equals(getString(R.string.button_take_selfie), ignoreCase = true)) {
                    if (isFrontCamera) {
                        takeSelfie()
                    } else {
                        Toast.makeText(this, "Please wait, switching to selfie camera.", Toast.LENGTH_SHORT).show()
                        switchToSelfieMode() // Ensure camera switches then user clicks again or auto-takes
                    }
                } else { // Button might say "Start New Scan" or similar if selfie was taken/failed
                    resetForNewScan()
                }
            } else { // Not yet scanned QR, so button is for "Start/Stop Scanning"
                toggleScanState()
            }
        }

        shutterButton.setOnClickListener {
            if (isFrontCamera && isQrScanned) { // Only allow selfie if QR scanned and front camera active
                takeSelfie()
            } else if (!isQrScanned) {
                Toast.makeText(this, "Please scan an event QR code first.", Toast.LENGTH_SHORT).show()
            } else { // QR scanned but not front camera
                Toast.makeText(this, "Please switch to selfie mode first (or wait).", Toast.LENGTH_SHORT).show()
            }
        }

        manualCodeButton.setOnClickListener {
            val intent = Intent(this@TimeInEventActivity, TimeInEventManualActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
            }
            startActivity(intent)
        }
    }

    private fun toggleScanState() {
        if (!isScanningEnabled) {
            scannerOverlay.visibility = View.VISIBLE
            startQrScanner()
            scanButton.text = getString(R.string.button_stop_scanning)
            isScanningEnabled = true
        } else {
            stopScanningOnly() // This keeps preview but stops analysis
            scannerOverlay.visibility = View.GONE
            scanButton.text = getString(R.string.button_start_scanning)
            isScanningEnabled = false
        }
    }

    private fun resetForNewScan() {
        Log.d(TAG, "resetForNewScan called")
        isQrScanned = false
        isScanningEnabled = false
        isFrontCamera = false // Default to back camera for new scan
        currentScannedEventId = null
        currentScannedEventName = null

        scanButton.text = getString(R.string.button_start_scanning)
        scanButton.isEnabled = true // Re-enable scan button
        // Reset scanButton's listener to the initial toggleScanState behavior
        scanButton.setOnClickListener { toggleScanState() }


        selfieReminder.text = getString(R.string.timein_event_qr_scan_instruction)
        scannerOverlay.visibility = View.GONE
        shutterButton.visibility = View.GONE
        stopScanningAnimation() // Ensure any running animation is stopped

        // Re-initialize camera for back camera preview
        setupCameraPreview() // This should ensure previewView is correctly parented
        startCameraPreviewOnly() // Start with back camera, no analysis
    }


    private fun setupCameraPreview() {
        if (cameraPlaceholder.parent == cameraPreviewHostFrame) {
            cameraPreviewHostFrame.removeView(cameraPlaceholder)
        }
        if (cameraPreviewView.parent != null) {
            (cameraPreviewView.parent as ViewGroup).removeView(cameraPreviewView)
        }
        if (cameraPreviewView.parent == null) { // Double check before adding
            cameraPreviewHostFrame.addView(cameraPreviewView, 0)
        }
    }

    private fun startCameraPreviewOnly() {
        Log.d(TAG, "startCameraPreviewOnly called. isFrontCamera: $isFrontCamera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            // Rebuild imageCapture for the current camera (front/back)
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture // Bind imageCapture for taking selfies
                )
                Log.d(TAG, "Camera preview bound with ${if (isFrontCamera) "Front" else "Back"} camera.")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed for preview only", exc)
                UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Failed to start camera preview: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startQrScanner() {
        Log.d(TAG, "startQrScanner called")
        isFrontCamera = false // QR scanning always uses back camera
        setupCameraPreview() // Ensure back camera is set up

        if (scannerOverlay.visibility != View.VISIBLE) {
            scannerOverlay.visibility = View.VISIBLE
        }
        showScanningAnimation()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            // ImageCapture is not strictly needed for QR scanning analysis but good to have bound
            val imageCaptureLocal = ImageCapture.Builder().build()
            imageCapture = imageCaptureLocal

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ::processImageForQrCode)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCaptureLocal, // Bind image capture
                    imageAnalyzer
                )
                selfieReminder.text = "Scanning for QR codes..." // Update UI
                Log.d(TAG, "Camera bound for QR scanning.")
            } catch (exc: Exception) {
                Log.e(TAG, "QR Scanner Use case binding failed", exc)
                UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Failed to start QR scanner: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanningAnimation() {
        if (scannerOverlay.visibility != View.VISIBLE) {
            scannerOverlay.visibility = View.VISIBLE
        }
        val animation = AlphaAnimation(0.3f, 0.8f).apply {
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        scannerOverlay.startAnimation(animation)
    }

    private fun stopScanningAnimation() {
        scannerOverlay.clearAnimation()
        // Visibility is handled elsewhere (e.g. GONE)
    }

    private fun stopScanningOnly() { // Keeps preview, stops analysis
        Log.d(TAG, "stopScanningOnly called")
        stopScanningAnimation()
        selfieReminder.text = "Scanning paused. Press button to resume."
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll() // Unbind everything
            startCameraPreviewOnly() // Restart preview (back camera, no analyzer)
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopScanningOnly: ${e.localizedMessage}")
        }
    }

    private fun stopQrScannerAndAnalysis() { // Stops everything related to QR scanning
        Log.d(TAG, "stopQrScannerAndAnalysis called")
        stopScanningAnimation()
        scannerOverlay.visibility = View.GONE
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
            // Do not restart preview here, as next step might be selfie or error.
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera in stopQrScannerAndAnalysis: ${e.localizedMessage}")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageForQrCode(imageProxy: ImageProxy) {
        // Log.d(TAG, "Processing image for QR. isScanningEnabled: $isScanningEnabled, isQrScanned: $isQrScanned")
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningEnabled && !isQrScanned) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcodeValue = barcodes[0].rawValue?.trim()
                        if (firstBarcodeValue != null) {
                            Log.d(TAG, "QR Code detected: $firstBarcodeValue")
                            // Ensure this runs on UI thread if it updates UI or state that affects UI
                            runOnUiThread {
                                handleQrCodeScannedUpdated(firstBarcodeValue)
                            }
                        } else {
                            Log.w(TAG, "Detected barcode has null rawValue.")
                        }
                    } else {
                        // Log.v(TAG, "No QR codes found in this frame.") // Verbose, enable if needed
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                    // Optionally show a transient error to user
                }
                .addOnCompleteListener {
                    imageProxy.close() // Crucial to close the ImageProxy
                }
        } else {
            imageProxy.close() // Always close if not processing
        }
    }

    private fun switchToSelfieMode() {
        Log.d(TAG, "switchToSelfieMode called")
        isFrontCamera = true
        stopQrScannerAndAnalysis() // Ensure back camera and analyzer are stopped
        setupCameraPreview() // Re-setup for front camera
        startCameraPreviewOnly() // Start front camera preview
        shutterButton.visibility = View.VISIBLE // Show shutter for selfie
        scanButton.text = getString(R.string.button_take_selfie) // Update button text
        scanButton.isEnabled = true
        selfieReminder.text = "Position your face for the selfie." // Update instruction
    }


    private fun takeSelfie() {
        Log.d(TAG, "takeSelfie called")
        val imageCaptureInstance = this.imageCapture ?: run {
            Toast.makeText(this, "Camera not ready for selfie.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "takeSelfie: imageCapture is null")
            return
        }

        scanButton.isEnabled = false // Disable button while taking/uploading
        shutterButton.isEnabled = false
        selfieReminder.text = "Capturing selfie..."

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(externalMediaDirs.firstOrNull() ?: filesDir, "event_selfie_${timestamp}.jpg")
        Log.d(TAG, "Selfie will be saved to: ${photoFile.absolutePath}")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCaptureInstance.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    UiDialogs.showErrorPopup(this@TimeInEventActivity, getString(R.string.popup_title_error), "Selfie capture failed: ${exc.message}")
                    scanButton.isEnabled = true // Re-enable
                    shutterButton.isEnabled = true
                    selfieReminder.text = "Capture failed. Try again."
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    selfieReminder.text = "Selfie captured. Uploading..."
                    uploadSelfieToFirebase(savedUri, timestamp)
                }
            }
        )
    }

    private fun uploadSelfieToFirebase(photoUri: Uri, timestamp: String) {
        Log.d(TAG, "uploadSelfieToFirebase called with URI: $photoUri")
        val storageRef = FirebaseStorage.getInstance().reference
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "User is not authenticated for upload.")
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Authentication error. Please retry.")
            scanButton.isEnabled = true
            shutterButton.isEnabled = true
            return
        }
        
        val currentUserId = userId ?: run {
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "User ID missing for upload.")
            Log.e(TAG, "uploadSelfieToFirebase: userId is null")
            scanButton.isEnabled = true // Re-enable
            shutterButton.isEnabled = true
            return
        }
        
        // Use Firebase Auth UID for storage path (for security rules) but keep metadata about app userId
        val firebaseUid = currentUser.uid
        val selfiePath = "event_selfies/$firebaseUid/selfie_event_${currentScannedEventId}_$timestamp.jpg"
        val selfieRef = storageRef.child(selfiePath)
        Log.d(TAG, "Uploading selfie to: $selfiePath (Firebase UID: $firebaseUid, App User ID: $currentUserId)")

        selfieRef.putFile(photoUri)
            .addOnSuccessListener {
                Log.d(TAG, "Selfie upload successful. Getting download URL.")
                selfieRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d(TAG, "Download URL: $downloadUrl")
                    logTimeInToFirestoreUpdated(downloadUrl.toString(), timestamp) // Pass timestamp if needed by log function
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get download URL", e)
                    UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Failed to get download URL: ${e.message}")
                    scanButton.isEnabled = true; shutterButton.isEnabled = true
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Selfie upload failed", e)
                UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Selfie upload failed: ${e.message}")
                scanButton.isEnabled = true; shutterButton.isEnabled = true
            }
            .addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                selfieReminder.text = "Uploading selfie: ${progress.toInt()}%"
            }
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e) // Log and continue
        }
    }

    private fun showErrorDialog(message: String) {
        if (!isFinishing && !isDestroyed) { // Avoid showing dialog if activity is finishing
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Log.w(TAG, "Activity is finishing, cannot show error dialog: $message")
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Handle this activity's camera permission result
            if (allPermissionsGranted()) {
                Log.d(TAG, "Permissions granted after request.")
                setupCameraPreview()
                startCameraPreviewOnly()
                scanButton.text = getString(R.string.button_start_scanning)
                selfieReminder.text = getString(R.string.timein_event_qr_scan_instruction)
                scannerOverlay.visibility = View.GONE
                shutterButton.visibility = View.GONE
            } else {
                Log.e(TAG, "Permissions not granted after request.")
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
                finish() // Close activity if permissions are denied
            }
        } else {
            // If it's not our request, pass it to the parent (WifiSecureActivity)
            // to handle its own requests (like location).
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        if (::cameraExecutor.isInitialized) { // Check if initialized before shutting down
            Log.d(TAG, "Shutting down cameraExecutor.")
            cameraExecutor.shutdownNow() // Use shutdownNow for more immediate stop
        } else {
            Log.w(TAG, "cameraExecutor was not initialized, no shutdown needed.")
        }
        // Release barcode scanner if it holds resources, though MLKit usually handles this.
        // if(::barcodeScanner.isInitialized) { barcodeScanner.close() } // MLKit might not need explicit close
    }
}