package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TimeInEventActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraContainer: CardView
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var cameraPlaceholder: ImageView
    private lateinit var backButton: ImageView
    private lateinit var scanButton: Button
    private lateinit var shutterButton: ImageView
    private lateinit var selfieReminder: TextView
    private lateinit var scannerOverlay: View

    private var imageCapture: ImageCapture? = null
    private var isScanningEnabled = false
    private var isQrScanned = false
    private var isFrontCamera = false

    companion object {
        private const val TAG = "TimeInEventActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_event_page)

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Initialize UI components
        initializeViews()

        // Create scanner overlay for visual feedback
        scannerOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundResource(R.drawable.scanner_overlay)
            alpha = 0f
        }

        // Create a preview view for the camera
        cameraPreviewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        // Set up QR scanner options (focus on QR codes)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Set initial visibility for reminders
        selfieReminder.text = "Position QR code within the frame to scan"

        // Set click listeners
        setupClickListeners()

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permissions
        if (allPermissionsGranted()) {
            setupCameraPreview()
            // Start camera preview only (no QR scanning yet)
            startCameraPreviewOnly()
            scanButton.text = "Start Scanning"
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun initializeViews() {
        cameraContainer = findViewById(R.id.camera_container)
        cameraPlaceholder = findViewById(R.id.camera_preview_placeholder)
        backButton = findViewById(R.id.icon_back_button)
        scanButton = findViewById(R.id.scan_qr_code)
        shutterButton = findViewById(R.id.icon_shutter_camera)
        selfieReminder = findViewById(R.id.qr_scanner_click_reminder)
    }

    private fun setupClickListeners() {

        backButton.setOnClickListener { view ->
            // Start animation if the drawable is an AnimatedVectorDrawable
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            // Add a small delay before finishing to allow animation to be seen
            view.postDelayed({
                finish()
            }, 50) // Match animation duration
        }

        scanButton.setOnClickListener {
            if (isQrScanned) {
                // Reset the scan state and UI
                resetForNewScan()
            } else {
                toggleScanState()
            }
        }

        shutterButton.setOnClickListener {
            if (isQrScanned && isFrontCamera) {
                takeSelfie()
            } else if (!isQrScanned) {
                // Do nothing when scanning - the button is just for taking selfies
                Toast.makeText(this, "Waiting for QR code...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleScanState() {
        if (!isScanningEnabled) {
            startQrScanner()
            scanButton.text = "Stop Scanning"
            isScanningEnabled = true
        } else {
            stopScanningOnly()
            scanButton.text = "Start Scanning"
            isScanningEnabled = false
        }
    }

    private fun resetForNewScan() {
        isQrScanned = false
        isScanningEnabled = false
        isFrontCamera = false
        scanButton.text = "Start Scanning"
        selfieReminder.text = "Position QR code within the frame to scan"

        // Restart camera preview only (no scanning)
        setupCameraPreview()
        startCameraPreviewOnly()
    }

    private fun setupCameraPreview() {
        try {
            // Create FrameLayout to hold both camera preview and overlay
            val frameLayout = FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // Replace placeholder with camera preview
            cameraContainer.removeAllViews()

            // Add camera preview to frame layout
            frameLayout.addView(cameraPreviewView)

            // Add scanner overlay to frame layout
            frameLayout.addView(scannerOverlay)

            // Add frame layout to camera container
            cameraContainer.addView(frameLayout)

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up camera preview", e)
            Toast.makeText(this, "Failed to set up camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCameraPreviewOnly() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Configure camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Set up preview only
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                    }

                // Set up image capture for later use
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Unbind all use cases and rebind only preview
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Error starting camera preview", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startQrScanner() {
        // Show scanning animation
        showScanningAnimation()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Configure camera with extended range if available
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Set up preview with proper resolution
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                    }

                // Improved image analysis configuration
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageForQrCode(imageProxy)
                        }
                    }

                // Set up image capture for selfies
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()

                // Unbind and bind use cases
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )

                // Show scanning message
                selfieReminder.text = "Scanning for QR codes..."

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Failed to start camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanningAnimation() {
        val animation = AlphaAnimation(0.3f, 0.7f)
        animation.duration = 1000
        animation.repeatMode = Animation.REVERSE
        animation.repeatCount = Animation.INFINITE
        scannerOverlay.animation = animation
        scannerOverlay.alpha = 0.3f
        animation.start()
    }

    private fun stopScanningAnimation() {
        scannerOverlay.clearAnimation()
        scannerOverlay.alpha = 0f
    }

    // Stop scanning but keep camera preview
    private fun stopScanningOnly() {
        stopScanningAnimation()
        selfieReminder.text = "Scanning paused. Press button to resume."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Configure camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Set up preview only
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                    }

                // Set up image capture for later use
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Unbind all use cases and rebind only preview
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Error stopping scanner", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Stop QR scanner completely (unbinds all camera)
    private fun stopQrScanner() {
        stopScanningAnimation()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }

    private fun processImageForQrCode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningEnabled && !isQrScanned) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // Process the image for QR codes
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_URL, Barcode.TYPE_TEXT -> {
                                val qrContent = barcode.rawValue ?: ""
                                if (qrContent.isNotEmpty()) {
                                    // Process the QR code content
                                    runOnUiThread {
                                        handleQrCodeScanned(qrContent)
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun handleQrCodeScanned(qrContent: String) {
        if (isQrScanned) return  // Prevent processing same QR code multiple times

        // Stop scanning animation
        stopScanningAnimation()

        // Vibrate to give feedback
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ (API 26+) - Use VibrationEffect
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // This won't be used since min SDK is 27, but keeping for completeness
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            // Handle vibration error silently
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }

        isQrScanned = true
        isScanningEnabled = false

        // Try to parse event information from QR
        try {
            if (qrContent.startsWith("TIMED:EVENT:")) {
                // Extract event data from QR code
                val eventData = qrContent.replace("TIMED:EVENT:", "").split(":")
                if (eventData.size >= 2) {
                    val eventId = eventData[0]
                    val eventName = eventData[1]

                    // Show success dialog
                    showSuccessDialog(eventName, eventId)

                    // Change UI to selfie mode
                    selfieReminder.text = "Event: $eventName\nPlease take a selfie for verification."
                    scanButton.text = "Scan a Different Code"

                    // Stop QR scanning and prepare for selfie
                    stopQrScanner()
                    switchToSelfieMode()
                } else {
                    showErrorDialog("Invalid QR format")
                    resetForNewScan()
                }
            } else {
                // Show error for invalid format
                showErrorDialog("Not a valid TimEd event QR code:\n$qrContent")
                resetForNewScan()
            }
        } catch (e: Exception) {
            showErrorDialog("Error: ${e.message}")
            resetForNewScan()
        }
    }

    private fun switchToSelfieMode() {
        // Switch camera to front-facing
        isFrontCamera = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Selfie mode setup failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSelfie() {
        // This is a placeholder - in a real implementation you would save the photo
        Toast.makeText(this, "Selfie captured for verification!", Toast.LENGTH_LONG).show()

        // Here you would typically:
        // 1. Actually capture the image using imageCapture
        // 2. Save it to storage
        // 3. Upload it along with the event check-in data

        // For now, we'll just simulate completion
        AlertDialog.Builder(this)
            .setTitle("Time-In Successful!")
            .setMessage("Your attendance has been recorded.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()  // Return to previous screen
            }
            .setCancelable(false)
            .show()
    }

    private fun showSuccessDialog(eventName: String, eventId: String) {
        Toast.makeText(
            this,
            "QR Code scanned successfully!\nEvent: $eventName",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Scan Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(applicationContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupCameraPreview()
                startCameraPreviewOnly()
                scanButton.text = "Start Scanning"
            } else {
                Toast.makeText(this,
                    "Camera permission is required for QR scanning.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}