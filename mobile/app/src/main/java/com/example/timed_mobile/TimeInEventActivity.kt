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
import androidx.core.net.toUri
import com.google.firebase.firestore.FieldValue
// Removed duplicate import: import com.example.timed_mobile.TimeInActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import android.widget.Toast
import androidx.camera.core.ImageProxy

class TimeInEventActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner
    private lateinit var cameraContainer: CardView // This is the CardView R.id.camera_container
    private lateinit var cameraPreviewHostFrame: FrameLayout // This is R.id.camera_preview_host_frame
    private lateinit var cameraPreviewView: PreviewView // Programmatically created
    private lateinit var cameraPlaceholder: ImageView // R.id.camera_preview_placeholder
    private lateinit var backButton: ImageView
    private lateinit var scanButton: Button
    private lateinit var shutterButton: ImageView // R.id.icon_shutter_camera
    private lateinit var selfieReminder: TextView
    private lateinit var scannerOverlay: View // This will be R.id.qr_scan_box_overlay from XML

    private var imageCapture: ImageCapture? = null
    private var isScanningEnabled = false
    private var isQrScanned = false
    private var isFrontCamera = false

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    // Store event details temporarily after QR scan
    private var currentScannedEventId: String? = null
    private var currentScannedEventName: String? = null

    private lateinit var manualCodeButton: Button

    companion object {
        private const val TAG = "TimeInEventActivity" // Ensure this TAG is used for Logcat filtering
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_page)

        // Run deep link parser
        handleDeepLinkIfPresent()

        // Existing session check
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

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        initializeViews()

        // Ensure the cameraPreviewView is attached once
        cameraPreviewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        cameraPreviewHostFrame.addView(cameraPreviewView, 0)

        barcodeScanner = BarcodeScanning.getClient() // Accepts all supported formats

        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            setupCameraPreview()
            startCameraPreviewOnly()
            scanButton.text = "Start Scanning"
            selfieReminder.text = "Position QR code within the frame to scan"
            scannerOverlay.visibility = View.GONE
            shutterButton.visibility = View.GONE
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun handleDeepLinkIfPresent() {
        val data: Uri? = intent?.data
        val eventId = data?.getQueryParameter("eventId")

        if (eventId != null) {
            val prefs = getSharedPreferences("TimedAppPrefs", MODE_PRIVATE)
            val userId = prefs.getString("userId", null)

            if (userId.isNullOrEmpty()) {
                Toast.makeText(this, "Not logged in. Please log in first.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val db = FirebaseFirestore.getInstance()
            val log = hashMapOf(
                "userId" to userId,
                "eventId" to eventId,
                "timeIn" to FieldValue.serverTimestamp()
            )

            db.collection("timeLogs")
                .add(log)
                .addOnSuccessListener {
                    Toast.makeText(this, "Timed in successfully for event $eventId!", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to time in: ${it.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    private fun initializeViews() {
        cameraContainer = findViewById(R.id.camera_container)
        cameraPreviewHostFrame = findViewById(R.id.camera_preview_host_frame) // Initialize this
        cameraPlaceholder = findViewById(R.id.camera_preview_placeholder)
        backButton = findViewById(R.id.icon_back_button)
        scanButton = findViewById(R.id.scan_qr_code)
        shutterButton = findViewById(R.id.icon_shutter_camera) // Initialize shutter button
        selfieReminder = findViewById(R.id.qr_scanner_click_reminder)
        scannerOverlay = findViewById(R.id.qr_scan_box_overlay)
        manualCodeButton = findViewById(R.id.manual_code_time_in)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            val intent = if (currentScannedEventId != null && currentScannedEventName != null) {
                // Redirect to event details screen if QR already scanned
                Intent(this@TimeInEventActivity, EventDetailActivity::class.java).apply {
                    putExtra("eventId", currentScannedEventId)
                    putExtra("eventName", currentScannedEventName)
                    putExtra("userId", userId)
                    putExtra("email", userEmail)
                    putExtra("firstName", userFirstName)
                }
            } else {
                // Fallback to home if no event scanned yet
                Intent(this@TimeInEventActivity, HomeActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("email", userEmail)
                    putExtra("firstName", userFirstName)
                }
            }

            startActivity(intent)
            finish()
        }

        scanButton.setOnClickListener {
            if (isQrScanned) {
                if (scanButton.text.toString().equals("Take Selfie", ignoreCase = true) && isFrontCamera) {
                    takeSelfie()
                } else if (scanButton.text.toString().equals("Take Selfie", ignoreCase = true) && !isFrontCamera) {
                    Toast.makeText(this, "Please wait, switching to selfie camera.", Toast.LENGTH_SHORT).show()
                    switchToSelfieMode()
                } else {
                    resetForNewScan()
                }
            } else {
                toggleScanState()
            }
        }

        shutterButton.setOnClickListener {
            if (isFrontCamera && isQrScanned) {
                takeSelfie()
            } else if (!isQrScanned) {
                Toast.makeText(this, "Please scan a QR code first.", Toast.LENGTH_SHORT).show()
            } else if (!isFrontCamera) {
                Toast.makeText(this, "Switching to selfie mode...", Toast.LENGTH_SHORT).show()
                switchToSelfieMode()
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
            scannerOverlay.visibility = View.VISIBLE // Make XML overlay visible
            startQrScanner() // This will also call showScanningAnimation
            scanButton.text = "Stop Scanning"
            isScanningEnabled = true
        } else {
            stopScanningOnly()
            scannerOverlay.visibility = View.GONE // Hide XML overlay
            scanButton.text = "Start Scanning"
            isScanningEnabled = false
        }
    }

    private fun resetForNewScan() {
        isQrScanned = false
        isScanningEnabled = false
        isFrontCamera = false
        currentScannedEventId = null
        currentScannedEventName = null
        scanButton.text = "Start Scanning"
        scanButton.setOnClickListener { // Reset scan button listener
            if (isQrScanned) resetForNewScan() else toggleScanState()
        }
        selfieReminder.text = "Position QR code within the frame to scan"
        scannerOverlay.visibility = View.GONE // Ensure XML overlay is hidden
        shutterButton.visibility = View.GONE // Hide shutter button
        stopScanningAnimation()
        setupCameraPreview() // Re-setup for back camera
        startCameraPreviewOnly()
    }

    private fun setupCameraPreview() {
        // Remove the placeholder if it's still there and a child of cameraPreviewHostFrame
        if (cameraPlaceholder.parent == cameraPreviewHostFrame) {
            cameraPreviewHostFrame.removeView(cameraPlaceholder)
        }

        // Ensure cameraPreviewView is not already added to this specific host frame
        if (cameraPreviewView.parent != null) {
            (cameraPreviewView.parent as ViewGroup).removeView(cameraPreviewView)
        }
        // Add cameraPreviewView at the beginning so qr_scan_box_overlay (from XML) is on top
        cameraPreviewHostFrame.addView(cameraPreviewView, 0)
    }

    private fun startCameraPreviewOnly() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build() // Rebuild for current camera
            val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture // Bind imageCapture here
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed for preview only", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startQrScanner() {
        Log.d(TAG, "startQrScanner called")
        isFrontCamera = false
        setupCameraPreview()

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

            val imageCaptureLocal = ImageCapture.Builder().build()
            imageCapture = imageCaptureLocal // Update global reference

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
                    imageCaptureLocal,
                    imageAnalyzer
                )
                selfieReminder.text = "Scanning for QR codes..."
                Log.d(TAG, "Camera bound successfully for QR scanning.")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanningAnimation() {
        if (scannerOverlay.visibility != View.VISIBLE) {
            scannerOverlay.visibility = View.VISIBLE
        }
        val animation = AlphaAnimation(0.3f, 0.8f).apply { // Slightly more visible max alpha
            duration = 800
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        scannerOverlay.startAnimation(animation)
    }

    private fun stopScanningAnimation() {
        scannerOverlay.clearAnimation()
        // Visibility is handled by GONE now, so alpha reset isn't strictly needed if hidden
    }

    private fun stopScanningOnly() {
        Log.d(TAG, "stopScanningOnly called")
        stopScanningAnimation()
        selfieReminder.text = "Scanning paused. Press button to resume."
        // Unbind analyzer to stop processing, but keep preview
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
            startCameraPreviewOnly() // Restart preview without analyzer
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scanning only: ${e.localizedMessage}")
        }
    }

    private fun stopQrScannerAndAnalysis() { // Renamed for clarity
        Log.d(TAG, "stopQrScannerAndAnalysis called")
        stopScanningAnimation()
        scannerOverlay.visibility = View.GONE // Hide it
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera provider: ${e.localizedMessage}")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageForQrCode(imageProxy: ImageProxy) {
        Log.d(TAG, "processImageForQrCode called. isScanningEnabled: $isScanningEnabled, isQrScanned: $isQrScanned, rotation: ${imageProxy.imageInfo.rotationDegrees}")
        val mediaImage = imageProxy.image

        if (mediaImage != null && isScanningEnabled && !isQrScanned) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "Barcodes detected: ${barcodes.size}")

                    barcodes.forEach {
                        Log.d(TAG, "Found barcode: ${it.rawValue}")
                    }

                    val matchingBarcode = barcodes.firstOrNull {
                        val value = it.rawValue?.trim()
                        value != null // No prefix restriction
                    }

                    if (matchingBarcode != null) {
                        handleQrCodeScanned(matchingBarcode.rawValue!!.trim())
                    } else {
                        Log.d(TAG, "No matching QR code found. All values: ${barcodes.map { it.rawValue }}")
                        runOnUiThread {
                            Toast.makeText(this@TimeInEventActivity, "No matching QR code found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Barcode scanning failed", it)
                    runOnUiThread {
                        Toast.makeText(this@TimeInEventActivity, "Barcode scanning error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            if (mediaImage == null) Log.d(TAG, "mediaImage is null")
            if (!isScanningEnabled) Log.d(TAG, "isScanningEnabled is false")
            if (isQrScanned) Log.d(TAG, "isQrScanned is true")
            imageProxy.close()
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun handleQrCodeScanned(qrContent: String) {
        Log.d(TAG, "handleQrCodeScanned called with: $qrContent")
        if (isQrScanned) return
        vibrate()

        val eventId = qrContent.substringAfterLast("/").trim()
        isQrScanned = true
        scanButton.isEnabled = false
        stopQrScannerAndAnalysis()

        val db = FirebaseFirestore.getInstance()
        val eventDocRef = db.collection("events").document(eventId)

        eventDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val eventName = document.getString("eventName") ?: "Unnamed Event"
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                    selfieReminder.text = "Logging time-in for $eventName..."

                    eventDocRef.collection("attendees")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("type", "event_time_in")
                        .get()
                        .addOnSuccessListener { docs ->
                            if (!docs.isEmpty) {
                                showErrorDialog("You have already timed in for this event.")
                                finish()
                            } else {
                                val record = hashMapOf(
                                    "userId" to userId,
                                    "firstName" to userFirstName,
                                    "email" to userEmail,
                                    "eventId" to eventId,
                                    "eventName" to eventName,
                                    "timestamp" to timestamp,
                                    "selfieUrl" to null,
                                    "type" to "event_time_in",
                                    "hasTimedOut" to false
                                )
                                eventDocRef.collection("attendees")
                                    .add(record)
                                    .addOnSuccessListener {
                                        AlertDialog.Builder(this)
                                            .setTitle("Time-In Successful")
                                            .setMessage("You have timed in for '$eventName'")
                                            .setPositiveButton("OK") { d, _ ->
                                                d.dismiss()
                                                setResult(RESULT_OK)
                                                finish()
                                            }
                                            .setCancelable(false)
                                            .show()
                                    }
                                    .addOnFailureListener {
                                        showErrorDialog("Failed to save time-in: ${it.message}")
                                        isQrScanned = false
                                    }
                            }
                        }
                        .addOnFailureListener {
                            showErrorDialog("Error checking existing records: ${it.message}")
                            isQrScanned = false
                        }
                } else {
                    showErrorDialog("Event not found.")
                    isQrScanned = false
                }
            }
            .addOnFailureListener {
                showErrorDialog("Failed to fetch event info: ${it.message}")
                isQrScanned = false
            }
    }

    private fun switchToSelfieMode() {
        Log.d(TAG, "switchToSelfieMode called")
        isFrontCamera = true
        // shutterButton.visibility = View.VISIBLE; // Already handled
        startCameraPreviewOnly() // This will now use front camera due to isFrontCamera flag
    }

    private fun takeSelfie() {
        Log.d(TAG, "takeSelfie called")
        val imageCapture = this.imageCapture ?: run {
            Toast.makeText(this, "Camera not ready for selfie.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "takeSelfie: imageCapture is null")
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Use external media dir for better accessibility if needed, or app-specific cache
        val photoFile = File(externalMediaDirs.firstOrNull() ?: cacheDir, "event_selfie_${timestamp}.jpg")
        Log.d(TAG, "Selfie will be saved to: ${photoFile.absolutePath}")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(
                        this@TimeInEventActivity,
                        "Selfie capture failed: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    uploadSelfieToFirebase(savedUri, timestamp)
                }
            }
        )
    }

    private fun uploadSelfieToFirebase(photoUri: Uri, timestamp: String) {
        Log.d(TAG, "uploadSelfieToFirebase called with URI: $photoUri")
        val storageRef = FirebaseStorage.getInstance().reference
        val currentUserId = userId ?: run {
            Toast.makeText(this, "User ID missing for upload.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "uploadSelfieToFirebase: userId is null")
            return
        }
        val selfiePath = "selfies_event/$currentUserId/selfie_$timestamp.jpg" // Differentiate event selfies
        val selfieRef = storageRef.child(selfiePath)
        Log.d(TAG, "Uploading selfie to: $selfiePath")

        selfieRef.putFile(photoUri)
            .addOnSuccessListener {
                Log.d(TAG, "Selfie upload successful. Getting download URL.")
                selfieRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d(TAG, "Download URL: $downloadUrl")
                    logTimeInToFirestore(downloadUrl.toString(), timestamp)
                }.addOnFailureListener {
                    Log.e(TAG, "Failed to get download URL", it)
                    Toast.makeText(this, "Failed to get download URL: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Selfie upload failed", it)
                Toast.makeText(this, "Selfie upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logTimeInToFirestore(selfieUrl: String, timestampPhoto: String) {
        Log.d(TAG, "logTimeInToFirestore called. Selfie URL: $selfieUrl")
        // Use stored event name and ID
        val eventNameForLog = currentScannedEventName ?: "Unknown Event"
        val eventIdForLog = currentScannedEventId ?: "UNKNOWN_ID_${timestampPhoto.substring(0,8)}"

        val record = hashMapOf(
            "userId" to userId,
            "firstName" to userFirstName,
            "email" to userEmail,
            "eventId" to eventIdForLog,
            "eventName" to eventNameForLog,
            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "selfieUrl" to selfieUrl,
            "type" to "event_time_in",
            "hasTimedOut" to false
        )
        Log.d(TAG, "Logging to Firestore: $record")

        FirebaseFirestore.getInstance().collection("attendanceRecords")
            .add(record)
            .addOnSuccessListener {
                Log.d(TAG, "Attendance record added to Firestore with ID: ${it.id}")
                AlertDialog.Builder(this)
                    .setTitle("Time-In Recorded")
                    .setMessage("Attendance for event '$eventNameForLog' saved successfully!")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        val resultIntent = Intent()
                        // You can put extras here if HomeActivity needs to know about success
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to save attendance to Firestore", it)
                Toast.makeText(this, "Failed to save attendance: ${it.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        200,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error", e)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            applicationContext, // Using applicationContext
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Call super
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "Permissions granted.")
                setupCameraPreview()
                startCameraPreviewOnly()
                scanButton.text = "Start Scanning"
                selfieReminder.text = "Position QR code within the frame to scan"
                scannerOverlay.visibility = View.GONE
                shutterButton.visibility = View.GONE
            } else {
                Log.e(TAG, "Permissions not granted.")
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
        if (::cameraExecutor.isInitialized) {
            Log.d(TAG, "Shutting down cameraExecutor.")
            cameraExecutor.shutdownNow()
        } else {
            Log.w(TAG, "cameraExecutor was not initialized.")
        }
    }
}