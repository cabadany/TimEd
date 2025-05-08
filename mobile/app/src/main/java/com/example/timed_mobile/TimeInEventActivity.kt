package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.timed_mobile.TimeInActivity
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

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    companion object {
        private const val TAG = "TimeInEventActivity"
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

        initializeViews()

        scannerOverlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.scanner_overlay)
            alpha = 0f
        }

        cameraPreviewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        selfieReminder.text = "Position QR code within the frame to scan"
        setupClickListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            setupCameraPreview()
            startCameraPreviewOnly()
            scanButton.text = "Start Scanning"
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initializeViews() {
        cameraContainer = findViewById(R.id.camera_container)
        cameraPlaceholder = findViewById(R.id.camera_preview_placeholder)
        backButton = findViewById(R.id.icon_back_button)
        scanButton = findViewById(R.id.scan_qr_code)
        selfieReminder = findViewById(R.id.qr_scanner_click_reminder)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {

            (it as ImageView).drawable?.let { drawable ->
                if (drawable is AnimatedVectorDrawable) drawable.start()
            }
            Intent(this@TimeInEventActivity, TimeInActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
            }
            it.postDelayed({ finish() }, 50)

        }

        scanButton.setOnClickListener {
            if (isQrScanned) resetForNewScan() else toggleScanState()
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
        setupCameraPreview()
        startCameraPreviewOnly()
    }

    private fun setupCameraPreview() {
        val frameLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        cameraContainer.removeAllViews()
        frameLayout.addView(cameraPreviewView)
        frameLayout.addView(scannerOverlay)
        cameraContainer.addView(frameLayout)
    }

    private fun startCameraPreviewOnly() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startQrScanner() {
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
                    it.setAnalyzer(cameraExecutor) { processImageForQrCode(it) }
                }
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalyzer
            )
            selfieReminder.text = "Scanning for QR codes..."
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanningAnimation() {
        val animation = AlphaAnimation(0.3f, 0.7f).apply {
            duration = 1000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
        scannerOverlay.animation = animation
        scannerOverlay.alpha = 0.3f
        animation.start()
    }

    private fun stopScanningAnimation() {
        scannerOverlay.clearAnimation()
        scannerOverlay.alpha = 0f
    }

    private fun stopScanningOnly() {
        stopScanningAnimation()
        selfieReminder.text = "Scanning paused. Press button to resume."
        startCameraPreviewOnly()
    }

    private fun stopQrScanner() {
        stopScanningAnimation()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    private fun processImageForQrCode(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isScanningEnabled && !isQrScanned) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.rawValue?.startsWith("TIMED:EVENT:") == true }?.rawValue?.let {
                        runOnUiThread { handleQrCodeScanned(it) }
                    }
                }
                .addOnFailureListener { Log.e(TAG, "Barcode scanning failed", it) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun handleQrCodeScanned(qrContent: String) {
        if (isQrScanned) return
        stopScanningAnimation()
        vibrate()

        isQrScanned = true
        isScanningEnabled = false

        val eventData = qrContent.removePrefix("TIMED:EVENT:").split(":")
        if (eventData.size >= 2) {
            val eventId = eventData[0]
            val eventName = eventData[1]
            selfieReminder.text = "Event: $eventName\nPlease take a selfie for verification."
            scanButton.text = "Scan a Different Code"
            stopQrScanner()
            switchToSelfieMode()
        } else {
            showErrorDialog("Invalid QR code format.")
            resetForNewScan()
        }
    }

    private fun switchToSelfieMode() {
        isFrontCamera = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takeSelfie() {
        val imageCapture = imageCapture ?: return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File.createTempFile("selfie_$timestamp", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@TimeInEventActivity,
                        "Selfie capture failed: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    uploadSelfieToFirebase(photoFile, timestamp)
                }
            }
        )
    }

    private fun uploadSelfieToFirebase(photoFile: File, timestamp: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val selfiePath = "selfies/$userId/$timestamp.jpg"
        val selfieRef = storageRef.child(selfiePath)

        val uri = photoFile.toUri()
        selfieRef.putFile(uri)
            .addOnSuccessListener {
                selfieRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    logTimeInToFirestore(downloadUrl.toString(), timestamp)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logTimeInToFirestore(selfieUrl: String, timestamp: String) {
        val eventText = selfieReminder.text.toString()
        val eventName = eventText.substringAfter("Event: ").substringBefore("\n")
        val eventId = "EVENT-${timestamp.take(8)}"

        val record = hashMapOf(
            "userId" to userId,
            "firstName" to userFirstName,
            "email" to userEmail,
            "eventId" to eventId,
            "eventName" to eventName,
            "timestamp" to timestamp,
            "selfieUrl" to selfieUrl
        )

        FirebaseFirestore.getInstance().collection("attendance")
            .add(record)
            .addOnSuccessListener {
                AlertDialog.Builder(this)
                    .setTitle("Time-In Recorded")
                    .setMessage("Attendance saved successfully!")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save attendance: ${it.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

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
            applicationContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            setupCameraPreview()
            startCameraPreviewOnly()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}