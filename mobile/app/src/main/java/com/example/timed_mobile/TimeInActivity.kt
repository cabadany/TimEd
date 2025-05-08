package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class TimeInActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceBoxOverlay: FaceBoxOverlay
    private lateinit var faceDetector: FaceDetector
    private lateinit var timeInButton: Button
    private var isFaceDetected = false
    private var currentLensFacing: Int =
        CameraSelector.LENS_FACING_BACK // Default to back, will be updated

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var isFrontCamera = true // Assume front first, will be updated by startCamera

    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        // Get user data from intent
        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        // Initialize UI components
        // previewView will be initialized in setupCameraPreview
        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceBoxOverlay = findViewById(R.id.face_box_overlay)
        timeInButton = findViewById(R.id.btntime_in)

        // Set up back button
        findViewById<ImageView>(R.id.icon_back_button).setOnClickListener {
            val drawable = it.background // Assuming background is an AnimatedVectorDrawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            it.postDelayed({ finish() }, 50) // Delay for animation
        }

        findViewById<ImageView>(R.id.icon_qr_scanner).setOnClickListener {
            try {
                val intent = Intent(this@TimeInActivity, TimeInEventActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("email", userEmail)
                    putExtra("firstName", userFirstName)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting TimeInEventActivity", e)
                Toast.makeText(this, "Cannot open QR scanner: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Handle Firebase authentication
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            if (userId != null) {
                Log.d(TAG, "Using provided userId: $userId")
            } else {
                auth.signInAnonymously()
                    .addOnSuccessListener { Log.d(TAG, "Signed in anonymously") }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Anonymous auth failed: ${e.message}")
                        Toast.makeText(
                            this,
                            "Authentication failed. Try again later.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        // Setup face detection
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)

        // Initial setup of camera preview structure. Camera itself will start in onResume.
        // Calling setupCameraPreview here ensures the PreviewView object is created.
        setupCameraPreview() // Call this before startCamera

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        timeInButton.setOnClickListener {
            if (!isFaceDetected) {
                Toast.makeText(this, "Please position your face properly.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: userId
            if (currentUserId == null) {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkAlreadyTimedIn(currentUserId) { already ->
                if (already) {
                    AlertDialog.Builder(this)
                        .setTitle("Already Timed In")
                        .setMessage("You already timed in today.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    capturePhotoAndUpload()
                }
            }
        }

        // Request permissions if not already granted.
        // The camera will be started in onResume after permissions are confirmed.
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        // If permissions are granted, set up the preview and start the camera.
        // This ensures the camera starts/restarts every time the activity comes to the foreground.
        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions granted, setting up preview and starting camera in onResume")
            setupCameraPreview() // Re-run to ensure PreviewView is correctly configured and added
            startCamera()
        } else {
            Log.d(TAG, "Permissions not granted in onResume. Camera will not start.")
            // Optionally, ensure a placeholder is visible if the camera isn't going to start
            findViewById<ViewGroup>(R.id.camera_container)?.removeAllViews() // Clear any old preview
            findViewById<ImageView>(R.id.camera_preview_placeholder)?.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        // Release the camera when the activity is paused.
        // This is crucial for CameraX to work correctly when resuming.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll() // Unbinds all use cases from the lifecycle.
                Log.d(TAG, "Camera unbound successfully in onPause.")
            } catch (e: Exception) {
                Log.e(TAG, "Could not unbind camera in onPause", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // Permissions granted. onResume will be called next and will handle starting the camera.
                Log.d(TAG, "Permissions granted via dialog.")
                // No need to call startCamera() here directly, onResume will take care of it.
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
                finish() // Or handle UI to show permission is denied
            }
        }
    }

    private fun setupCameraPreview() {
        val container = findViewById<ViewGroup>(R.id.camera_container)

        // First remove any existing views to avoid duplicates
        container.removeAllViews()

        // Hide the placeholder image that might be blocking the camera
        findViewById<ImageView>(R.id.camera_preview_placeholder)?.visibility = View.GONE

        // Important: Make sure previewView is initialized correctly
        previewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        // Add the preview view to container
        container.addView(previewView)

        // Get the class-level faceBoxOverlay (don't create a new local variable)
        // and only add if it's not null
        // Ensure faceBoxOverlay is initialized before this call (it is in onCreate)
        if (faceBoxOverlay.parent != null) {
            (faceBoxOverlay.parent as? ViewGroup)?.removeView(faceBoxOverlay)
        }
        container.addView(faceBoxOverlay)


        Log.d(TAG, "Camera preview setup complete")
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Use addListener with a weak reference to this activity to prevent leaks
        cameraProviderFuture.addListener({
            // Check if activity is still valid
            if (isFinishing || isDestroyed) {
                Log.w(TAG, "Activity is finishing or destroyed, skipping camera initialization")
                return@addListener
            }

            try {
                val provider = cameraProviderFuture.get()

                // Log available cameras for debugging
                val availableCameras = provider.availableCameraInfos
                Log.d(TAG, "Available cameras: ${availableCameras.size}")

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(640, 480))  // Lower resolution for faster processing
                    .build().also {
                        it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) }
                    }

                // Start with front camera
                var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    // Unbind use cases before rebinding
                    provider.unbindAll()

                    // Try to bind with front camera first
                    val camera = provider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture, imageAnalyzer
                    )

                    // Successfully using front camera
                    Log.d(TAG, "Using front camera")
                    isFrontCamera = true
                    currentLensFacing = CameraSelector.LENS_FACING_FRONT


                } catch (e: Exception) {
                    // Check again if activity is still valid
                    if (isFinishing || isDestroyed) {
                        Log.w(TAG, "Activity is finishing, aborting camera setup")
                        return@addListener
                    }

                    // If front camera fails, try back camera
                    try {
                        Log.w(TAG, "Front camera binding failed, trying back camera", e)
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, imageAnalyzer
                        )

                        // Successfully using back camera
                        isFrontCamera = false
                        currentLensFacing = CameraSelector.LENS_FACING_BACK
                        Toast.makeText(
                            this,
                            "Using back camera - please center your face in the frame",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to bind any camera", e2)
                        Toast.makeText(this, "Unable to access camera", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera provider failed to initialize", e)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                isFaceDetected = faces.isNotEmpty()
                val bounds = faces.firstOrNull()?.boundingBox
                val transformed = bounds?.let {
                    adjustBox(
                        it,
                        imageProxy.width,
                        imageProxy.height,
                        previewView.width,
                        previewView.height
                    )
                }
                runOnUiThread {
                    faceBoxOverlay.updateFaceBox(transformed, isFaceDetected)
                }
            }
            .addOnFailureListener {
                isFaceDetected = false
                Log.e(TAG, "Face detection failed: ${it.message}")
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun adjustBox(box: Rect, imgW: Int, imgH: Int, viewW: Int, viewH: Int): RectF {
        val rect = RectF(box)
        val scaleX = viewW.toFloat() / imgH // Swapped imgW and imgH for portrait
        val scaleY = viewH.toFloat() / imgW // Swapped imgW and imgH for portrait

        val scale = max(scaleX, scaleY) // Use max to fill the view

        val offsetX = (viewW - imgH * scale) / 2f
        val offsetY = (viewH - imgW * scale) / 2f

        // Adjust coordinates based on image rotation and preview scaling
        val mappedBox = RectF()
        mappedBox.left = rect.left * scaleX + offsetX
        mappedBox.right = rect.right * scaleX + offsetX
        mappedBox.top = rect.top * scaleY + offsetY
        mappedBox.bottom = rect.bottom * scaleY + offsetY


        if (isFrontCamera) { // Mirror for front camera
            val originalLeft = mappedBox.left
            mappedBox.left = viewW - mappedBox.right
            mappedBox.right = viewW - originalLeft
        }

        return mappedBox
    }

    private fun checkAlreadyTimedIn(uid: String, callback: (Boolean) -> Unit) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        FirebaseDatabase.getInstance().getReference("timeLogs").child(uid)
            .orderByChild("timestamp").startAt(today.toDouble())
            .get()
            .addOnSuccessListener { snapshot ->
                val already = snapshot.children.any {
                    it.child("type").value == "TimeIn"
                }
                callback(already)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking time-in status: ${e.message}")
                Toast.makeText(this, "Could not verify time-in status", Toast.LENGTH_SHORT).show()
                callback(false) // Assume not timed in on error to allow attempt
            }
    }

    private fun capturePhotoAndUpload() {
        timeInButton.isEnabled = false
        timeInButton.text = "Processing..."

        val filename = "Timed_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/TimedApp"
                )
            }
        }

        val imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
            Toast.makeText(this, "Failed to create file URI", Toast.LENGTH_SHORT).show()
            timeInButton.isEnabled = true
            timeInButton.text = "Time - In"
            return
        }

        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(contentResolver, imageUri, contentValues).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Get user ID - try Firebase first, then intent
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: userId

                    if (currentUserId == null) {
                        Toast.makeText(
                            this@TimeInActivity,
                            "User ID not available",
                            Toast.LENGTH_SHORT
                        ).show()
                        timeInButton.isEnabled = true
                        timeInButton.text = "Time - In"
                        return
                    }

                    uploadImageToFirebase(imageUri, currentUserId) { imageUrl ->
                        if (imageUrl != null) {
                            logTimeIn(imageUrl, currentUserId)
                            showSuccessDialog()
                        } else {
                            Toast.makeText(
                                this@TimeInActivity,
                                "Upload failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            timeInButton.isEnabled = true
                            timeInButton.text = "Time - In"
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(this@TimeInActivity, "Photo capture failed.", Toast.LENGTH_SHORT)
                        .show()
                    timeInButton.isEnabled = true
                    timeInButton.text = "Time - In"
                }
            })
    }

    private fun uploadImageToFirebase(imageUri: Uri, uid: String, callback: (String?) -> Unit) {
        val filename = "Timed_${System.currentTimeMillis()}.jpg"
        val ref = FirebaseStorage.getInstance().getReference("uploads/$uid/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get image URL: ${e.message}")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Image upload failed: ${e.message}")
                callback(null)
            }
    }

    private fun logTimeIn(imageUrl: String, uid: String) {
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(uid)
        val log = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeIn",
            "imageUrl" to imageUrl,
            "email" to userEmail, // Make sure userEmail is not null
            "firstName" to userFirstName // Make sure userFirstName is not null
        )

        ref.push().setValue(log)
            .addOnSuccessListener { Log.d(TAG, "Time-in logged successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Log failed: ${e.message}") }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<TextView>(R.id.popup_title).text = "Successfully Timed - In"
        dialog.findViewById<TextView>(R.id.popup_message).text =
            "Your attendance has been recorded."
        dialog.findViewById<Button>(R.id.popup_close_button).setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK, Intent().putExtra("TIMED_IN_SUCCESS", true))
            finish()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        // It's good practice to release the FaceDetector client when no longer needed
        faceDetector.close()
    }
}