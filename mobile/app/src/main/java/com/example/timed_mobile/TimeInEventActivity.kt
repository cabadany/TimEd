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


    companion object {
        private const val TAG = "TimeInEventActivity" // Ensure this TAG is used for Logcat filtering
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing user session. Please log in again.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        initializeViews() // scannerOverlay will be initialized here to the XML one

        // cameraPreviewView is created programmatically
        cameraPreviewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE // Recommended for CameraX
            scaleType = PreviewView.ScaleType.FILL_CENTER // Or FIT_CENTER depending on desired preview
        }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            setupCameraPreview() // Sets up the cameraPreviewView in the FrameLayout
            startCameraPreviewOnly()
            scanButton.text = "Start Scanning"
            selfieReminder.text = "Position QR code within the frame to scan"
            scannerOverlay.visibility = View.GONE // Ensure XML overlay is initially hidden
            shutterButton.visibility = View.GONE // Hide shutter initially
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
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
        scannerOverlay = findViewById(R.id.qr_scan_box_overlay) // Use the View from XML
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            // StateListAnimator on XML handles press feedback.
            val intent = Intent(this@TimeInEventActivity, TimeInActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
                // Consider flags if you want to go to an existing instance and clear stack above it
                // flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent) // Start the activity
            finish() // Finish current activity
        }

        scanButton.setOnClickListener {
            if (isQrScanned) { // If QR already scanned, button might be for "Take Selfie" or "Scan Different"
                if (scanButton.text.toString().equals("Take Selfie", ignoreCase = true) && isFrontCamera) {
                    takeSelfie()
                } else if (scanButton.text.toString().equals("Take Selfie", ignoreCase = true) && !isFrontCamera) {
                    Toast.makeText(this, "Please wait, switching to selfie camera.", Toast.LENGTH_SHORT).show()
                    switchToSelfieMode()
                }
                else { // If button is not "Take Selfie" (e.g. "Scan Different Event")
                    resetForNewScan()
                }
            } else {
                toggleScanState()
            }
        }

        shutterButton.setOnClickListener {
            if (isFrontCamera && isQrScanned) { // Only allow selfie if QR scanned and in selfie mode
                takeSelfie()
            } else if (!isQrScanned) {
                Toast.makeText(this, "Please scan a QR code first.", Toast.LENGTH_SHORT).show()
            } else if (!isFrontCamera) {
                Toast.makeText(this, "Switching to selfie mode...", Toast.LENGTH_SHORT).show()
                switchToSelfieMode() // Or just enable selfie mode if QR is already scanned
            }
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
        isFrontCamera = false // Ensure we are using back camera for QR
        setupCameraPreview() // Ensure preview is set up for back camera
        if (scannerOverlay.visibility != View.VISIBLE) { // Ensure visible before animation
            scannerOverlay.visibility = View.VISIBLE
        }
        showScanningAnimation()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ::processImageForQrCode)
                }
            // imageCapture is already initialized in startCameraPreviewOnly or setupCameraPreview
            // No need to re-initialize imageCapture here unless changing its config

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA, // Explicitly use back camera
                    preview,
                    imageCapture, // Use the existing imageCapture instance
                    imageAnalyzer
                )
                selfieReminder.text = "Scanning for QR codes..."
                Log.d(TAG, "Camera bound for QR scanning.")
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed for QR Scanner", exc)
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
            Log.d(TAG, "Processing image. Format: ${mediaImage.format}, Size: ${mediaImage.width}x${mediaImage.height}")
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Log.d(TAG, "Barcodes detected: ${barcodes.size}")
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            Log.d(TAG, "Barcode raw value: ${barcode.rawValue}, format: ${barcode.format}, type: ${barcode.valueType}")
                        }
                    }

                    if (isScanningEnabled && !isQrScanned) { // Double check state
                        val matchingBarcode = barcodes.firstOrNull {
                            Log.d(TAG, "Checking barcode for prefix: '${it.rawValue}'")
                            it.rawValue?.startsWith("TIMED:EVENT:") == true
                        }

                        if (matchingBarcode != null) {
                            Log.d(TAG, "Matching QR code found: ${matchingBarcode.rawValue}")
                            runOnUiThread { handleQrCodeScanned(matchingBarcode.rawValue!!) }
                        } else {
                            Log.d(TAG, "No QR code with 'TIMED:EVENT:' prefix found in this batch.")
                        }
                    } else {
                        Log.d(TAG, "State changed during barcode processing. isScanningEnabled: $isScanningEnabled, isQrScanned: $isQrScanned")
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    // Log.d(TAG, "Image processing complete, closing proxy.") // Can be noisy
                    imageProxy.close()
                }
        } else {
            if (mediaImage == null) Log.d(TAG, "mediaImage is null in processImageForQrCode")
            if (!isScanningEnabled) Log.d(TAG, "isScanningEnabled is false in processImageForQrCode (state: $isScanningEnabled)")
            if (isQrScanned) Log.d(TAG, "isQrScanned is true in processImageForQrCode (state: $isQrScanned)")
            imageProxy.close() // Always close the proxy
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun handleQrCodeScanned(qrContent: String) {
        Log.d(TAG, "handleQrCodeScanned called with: $qrContent")
        if (isQrScanned) { // Check if already processed or in verification
            Log.d(TAG, "handleQrCodeScanned: Already processed or verifying, returning.")
            return
        }
        vibrate()

        // isScanningEnabled = false; // This is now set before calling this if verification is involved
        // stopQrScannerAndAnalysis(); // This is also called before if verification is involved

        val eventData = qrContent.removePrefix("TIMED:EVENT:").split(":")
        if (eventData.size >= 2) {
            currentScannedEventId = eventData[0]
            currentScannedEventName = eventData[1]
            Log.d(TAG, "Parsed Event ID: $currentScannedEventId, Name: $currentScannedEventName")

            // --- SIMULATED VERIFICATION ---
            selfieReminder.text = "Verifying QR Code for ${currentScannedEventName}..."
            scanButton.isEnabled = false // Disable button during verification

            Handler(Looper.getMainLooper()).postDelayed({
                scanButton.isEnabled = true // Re-enable button
                val isVerified = Random().nextBoolean() // Randomly succeed or fail
                Log.d(TAG, "Simulated verification result: $isVerified")

                if (isVerified) {
                    AlertDialog.Builder(this)
                        .setTitle("Verification Successful")
                        .setMessage("QR Code for '${currentScannedEventName}' verified. Please proceed to take a selfie.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            isQrScanned = true // Mark as scanned only after successful verification
                            selfieReminder.text = "Event: ${currentScannedEventName}\nPlease take a selfie for verification."
                            scanButton.text = "Take Selfie"
                            scanButton.setOnClickListener { // Update listener for "Take Selfie"
                                if (isFrontCamera) {
                                    takeSelfie()
                                } else {
                                    Toast.makeText(this, "Please wait, switching to selfie camera.", Toast.LENGTH_SHORT).show()
                                    switchToSelfieMode()
                                }
                            }
                            shutterButton.visibility = View.VISIBLE
                            switchToSelfieMode()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Verification Failed")
                        .setMessage("The scanned QR code for '${currentScannedEventName}' could not be verified. It might be invalid or expired. Please try again.")
                        .setPositiveButton("Try Again") { dialog, _ ->
                            dialog.dismiss()
                            resetForNewScan() // Allow user to scan again
                        }
                        .setCancelable(false)
                        .show()
                }
            }, 2000) // 2-second delay for simulation

        } else {
            Log.e(TAG, "Invalid QR code format after prefix removal: $qrContent")
            showErrorDialog("Invalid QR code format.")
            resetForNewScan()
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
        Log.d(TAG, "onDestroy called, shutting down cameraExecutor.")
        cameraExecutor.shutdownNow() // Use shutdownNow for quicker termination
    }
}