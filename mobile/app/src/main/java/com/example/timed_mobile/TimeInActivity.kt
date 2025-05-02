package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class TimeInActivity : AppCompatActivity() {
    private var previewView: PreviewView? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageUri: Uri? = null
    private lateinit var backButton: ImageView
    private lateinit var faceDetector: FaceDetector // Added
    @Volatile private var isFaceDetected: Boolean = false // Added - volatile for thread safety
    private lateinit var faceBoxOverlay: FaceBoxOverlay // Added
    private var currentLensFacing: Int = CameraSelector.LENS_FACING_FRONT // Added: Track lens facing

    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            // Manifest.permission.WRITE_EXTERNAL_STORAGE // Usually not needed with MediaStore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        backButton = findViewById(R.id.icon_back_button)
        faceBoxOverlay = findViewById(R.id.face_box_overlay) // Initialize overlay

        // Initialize Face Detector
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)

        // Set up camera container
        setupCamera()

        // Set up back button
        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        // Set up QR scanner icon click listener
        findViewById<ImageView>(R.id.icon_qr_scanner).setOnClickListener {
            try {
                val intent = Intent(this@TimeInActivity, TimeInEventActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting TimeInEventActivity", e)
                Toast.makeText(this, "Cannot open QR scanner: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up time-in button - Checks for face
        findViewById<Button>(R.id.btntime_in).setOnClickListener {
            if (isFaceDetected) {
                takePhotoAndShowSuccess()
            } else {
                Toast.makeText(this, "Please position your face in the camera view", Toast.LENGTH_SHORT).show()
            }
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            Handler(Looper.getMainLooper()).postDelayed({
                startCamera()
            }, 500)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    // Setup camera container and add PreviewView
    private fun setupCamera() {
        val container = findViewById<ViewGroup>(R.id.camera_container)
        try {
            previewView = PreviewView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                // Ensure scaleType is FILL_CENTER for the adjustBoundingBox logic below
                scaleType = PreviewView.ScaleType.FILL_CENTER
                visibility = View.INVISIBLE // Start invisible until camera binds
            }
            // Add PreviewView *behind* the overlay in the container
            container.addView(previewView, 0) // Add at index 0
        } catch (e: Exception) {
            Log.e(TAG, "Error creating preview view", e)
            Toast.makeText(this, "Could not initialize camera preview", Toast.LENGTH_SHORT).show()
        }
    }


    // Check if all required permissions are granted
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val cameraPermissionGranted = grantResults.isNotEmpty() &&
                    permissions.indexOf(Manifest.permission.CAMERA) != -1 &&
                    grantResults[permissions.indexOf(Manifest.permission.CAMERA)] == PackageManager.PERMISSION_GRANTED

            if (cameraPermissionGranted) {
                Handler(Looper.getMainLooper()).postDelayed({
                    startCamera()
                }, 500)
            } else {
                Toast.makeText(this, "Camera permissions are required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    // Start camera, bind use cases (Preview, ImageCapture, ImageAnalysis)
    @SuppressLint("UnsafeOptInUsageError") // Needed for ImageAnalysis
    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .setTargetResolution(Size(640, 480))
                        .build()

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetResolution(Size(1280, 720))
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageForFaceDetection(imageProxy)
                            }
                        }

                    val cameraSelector = findAvailableCameraSelector(cameraProvider)
                    currentLensFacing = cameraSelector.lensFacing ?: CameraSelector.LENS_FACING_FRONT

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, imageAnalyzer
                        )

                        previewView?.let {
                            preview.setSurfaceProvider(it.surfaceProvider)
                            it.visibility = View.VISIBLE
                            findViewById<ImageView>(R.id.camera_preview_placeholder).visibility = View.GONE
                        }
                        Log.d(TAG, "Camera started successfully with Face Detection")

                    } catch (e: Exception) {
                        Log.e(TAG, "Use case binding failed", e)
                        Toast.makeText(this, "Camera initialization failed.", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Camera provider error", e)
                    Toast.makeText(this, "Camera system error", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this))

        } catch (e: Exception) {
            Log.e(TAG, "Camera start error", e)
            Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to process image frames for face detection and update overlay
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageForFaceDetection(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // IMPORTANT: Get rotation degrees
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    isFaceDetected = faces.isNotEmpty()

                    if (isFaceDetected) {
                        val face = faces.first()
                        val boundingBox = face.boundingBox

                        // --- Coordinate Transformation ---
                        // Pass rotationDegrees to the adjustment function
                        val transformedBounds = adjustBoundingBox(
                            boundingBox,
                            // Image dimensions depend on rotation. If rotation is 90/270, swap width/height.
                            if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width,
                            if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height,
                            previewView?.width ?: 0,
                            previewView?.height ?: 0,
                            rotationDegrees // Pass rotation
                        )
                        // ---------------------------------

                        runOnUiThread {
                            faceBoxOverlay.updateFaceBox(transformedBounds, true)
                        }
                    } else {
                        runOnUiThread {
                            faceBoxOverlay.updateFaceBox(null, false)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    isFaceDetected = false
                    runOnUiThread { faceBoxOverlay.updateFaceBox(null, false) }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
            isFaceDetected = false
            runOnUiThread { faceBoxOverlay.updateFaceBox(null, false) }
        }
    }


    // REVISED Helper function to transform bounding box coordinates
    private fun adjustBoundingBox(
        boundingBox: Rect,
        imageWidth: Int,    // Width of the image buffer *before* rotation compensation
        imageHeight: Int,   // Height of the image buffer *before* rotation compensation
        viewWidth: Int,
        viewHeight: Int,
        rotationDegrees: Int // Rotation applied to the image buffer to make it upright
    ): RectF {
        val box = RectF(boundingBox)

        if (viewWidth == 0 || viewHeight == 0 || imageWidth == 0 || imageHeight == 0) {
            return RectF() // Avoid division by zero
        }

        // 1. Adjust image dimensions based on rotation for scaling calculation.
        // The ML Kit bounding box is relative to the *upright* image.
        val rotatedImageWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight else imageWidth
        val rotatedImageHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth else imageHeight

        // 2. Calculate scale factors based on FILL_CENTER.
        // How much we need to scale the upright image to fill the view.
        val scaleX = viewWidth.toFloat() / rotatedImageWidth.toFloat()
        val scaleY = viewHeight.toFloat() / rotatedImageHeight.toFloat()
        val scale = max(scaleX, scaleY) // Use the larger scale factor for FILL_CENTER

        // 3. Calculate offset due to centering.
        val offsetX = (viewWidth - rotatedImageWidth * scale) / 2f
        val offsetY = (viewHeight - rotatedImageHeight * scale) / 2f

        // 4. Map bounding box coordinates to the view coordinate system.
        // The bounding box coordinates are relative to the upright image.
        box.left = box.left * scale + offsetX
        box.top = box.top * scale + offsetY
        box.right = box.right * scale + offsetX
        box.bottom = box.bottom * scale + offsetY

        // 5. Mirror the coordinates if using the front camera.
        // This mirroring happens *after* scaling and offsetting.
        if (currentLensFacing == LENS_FACING_FRONT) {
            val originalLeft = box.left
            box.left = viewWidth - box.right
            box.right = viewWidth - originalLeft
        }

        // 6. Clamp to view bounds (optional but good practice)
        // box.left = max(0f, box.left)
        // box.top = max(0f, box.top)
        // box.right = min(viewWidth.toFloat(), box.right)
        // box.bottom = min(viewHeight.toFloat(), box.bottom)
        // Clamping might hide parts of the box if calculations are slightly off near edges.
        // Consider removing clamping if the box seems cut off.

        return box
    }


    // Find the best available camera (prefer front)
    private fun findAvailableCameraSelector(cameraProvider: ProcessCameraProvider): CameraSelector {
        try {
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                Log.d(TAG, "Using front camera")
                return CameraSelector.DEFAULT_FRONT_CAMERA
            }
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                Log.d(TAG, "Front camera not found, using back camera")
                return CameraSelector.DEFAULT_BACK_CAMERA
            }
            Log.d(TAG, "No standard cameras found, using generic selector")
            return CameraSelector.Builder().build()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera availability", e)
            return CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }


    // Take photo, save it, show success dialog, and log to Firebase
    private fun takePhotoAndShowSuccess() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val timeInButton = findViewById<Button>(R.id.btntime_in)
        timeInButton.isEnabled = false
        timeInButton.text = "Processing..."

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "TimEd_${System.currentTimeMillis()}")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TimEd")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        capturedImageUri = output.savedUri
                        Log.d(TAG, "Photo capture succeeded: $capturedImageUri")
                        showFlashEffect()
                        Handler(Looper.getMainLooper()).postDelayed({
                            showSuccessDialog()
                            logTimeInToFirebase()
                        }, 500)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        runOnUiThread {
                            timeInButton.isEnabled = true
                            timeInButton.text = "Time - In"
                            Toast.makeText(baseContext, "Failed to take photo: ${exc.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo setup", e)
            runOnUiThread {
                timeInButton.isEnabled = true
                timeInButton.text = "Time - In"
                Toast.makeText(this, "Error setting up photo capture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Show a brief white flash effect over the camera view
    private fun showFlashEffect() {
        try {
            val cameraContainer = findViewById<ViewGroup>(R.id.camera_container)
            val flashView = View(this)
            flashView.setBackgroundColor(Color.WHITE)
            flashView.alpha = 0.7f
            cameraContainer.addView(flashView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            flashView.bringToFront()
            flashView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    cameraContainer.removeView(flashView)
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing flash effect", e)
        }
    }


    // Show the success dialog after time-in
    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        titleText.text = "Successfully Timed - In"
        messageText.text = "Thank you. It has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            findViewById<Button>(R.id.btntime_in)?.apply {
                isEnabled = true
                text = "Time - In"
            }
            finish()
        }
        dialog.show()
    }


    // Log the time-in event to Firebase Realtime Database
    private fun logTimeInToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in, cannot log time-in")
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs")
        val log = mapOf(
            "userId" to userId,
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeIn"
        )
        dbRef.child(userId).push().setValue(log).addOnSuccessListener {
            Log.d(TAG, "Time-in successfully logged in Firebase")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to log time-in: ${it.message}")
            Toast.makeText(this, "Failed to save time-in record.", Toast.LENGTH_SHORT).show()
        }
    }


    // Shutdown camera executor when activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // faceDetector.close() // Consider closing if needed
        Log.d(TAG, "TimeInActivity destroyed and camera executor shut down.")
    }
}