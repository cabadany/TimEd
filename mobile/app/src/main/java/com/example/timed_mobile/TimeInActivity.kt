package com.example.timed_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils // Added for animations
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView // Added for CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

import androidx.exifinterface.media.ExifInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

class TimeInActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceBoxOverlay: FaceBoxOverlay
    private lateinit var faceDetector: FaceDetector
    private lateinit var timeInButton: Button
    private lateinit var backButton: ImageView
    private var isFaceDetected = false
    private var isFrontCamera = true // Assuming front camera is default
    private var currentLensFacing = CameraSelector.LENS_FACING_FRONT

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var isAuthenticated = false

    // Declare views for animation
    private lateinit var titleName: TextView
    private lateinit var positionCameraViewText: TextView
    private lateinit var cameraContainerCard: CardView
    private lateinit var iconQrScanner: ImageView
    private lateinit var qrScannerClickReminderText: TextView


    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val DB_PATH_TIME_LOGS = "timeLogs"
        // private const val DB_PATH_ATTENDANCE = "attendance" // Not used in this file
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        // Initialize views for functionality
        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceBoxOverlay = findViewById(R.id.face_box_overlay)
        // Views for animation are initialized below

        // Initialize views for animation
        backButton = findViewById(R.id.icon_back_button)
        timeInButton = findViewById(R.id.btntime_in)
        titleName = findViewById(R.id.titleName)
        positionCameraViewText = findViewById(R.id.position_camera_view)
        cameraContainerCard = findViewById(R.id.camera_container) // This is the CardView
        iconQrScanner = findViewById(R.id.icon_qr_scanner)
        qrScannerClickReminderText = findViewById(R.id.qr_scanner_click_reminder)


        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()


        // --- START OF ENTRY ANIMATION CODE ---
        // Load animations
        // val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in) // Not used directly, individual instances created
        // val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in) // Not used directly
        // val animSlideUpContent = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_content) // Not used directly

        var currentDelay = 100L

        // Helper to apply animation
        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        // 1. Back Button
        animateView(backButton, R.anim.fade_in, currentDelay)
        currentDelay += 100L

        // 2. Title "WILDTIMed"
        animateView(titleName, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 100L

        // 3. Instruction Text "Position your face..."
        animateView(positionCameraViewText, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        // 4. Camera Container Card
        animateView(cameraContainerCard, R.anim.fade_in, currentDelay)
        currentDelay += 200L

        // 5. QR Scanner Icon
        animateView(iconQrScanner, R.anim.fade_in, currentDelay)
        currentDelay += 100L

        // 6. QR Scanner Reminder Text
        animateView(qrScannerClickReminderText, R.anim.slide_up_fade_in_content, currentDelay)
        currentDelay += 100L

        // 7. Time-In Button
        animateView(timeInButton, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---


        backButton.setOnClickListener { finish() }

        // --- START OF QR SCANNER ICON CLICK LISTENER ---
        iconQrScanner.setOnClickListener {
            val intent = Intent(this, TimeInEventActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("email", userEmail)
            intent.putExtra("firstName", userFirstName)
            startActivity(intent)
        }
        // Also make the reminder text clickable for better UX
        qrScannerClickReminderText.setOnClickListener {
            iconQrScanner.performClick() // Programmatically click the icon
        }
        // --- END OF QR SCANNER ICON CLICK LISTENER ---

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d(TAG, "Firebase Auth: User IS authenticated. UID: ${currentUser.uid}")
            isAuthenticated = true
        } else {
            Log.e(TAG, "Firebase Auth: User is NOT authenticated")
            signInAnonymouslyForStorage { success ->
                if (success) {
                    isAuthenticated = true
                } else {
                    isAuthenticated = false
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Log.d(TAG, "Authentication state - isAuthenticated: $isAuthenticated, userId: $userId, " +
                "firebaseUID: ${currentUser?.uid}")

        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)

        setupCameraPreview()

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        timeInButton.setOnClickListener {
            if (!isFaceDetected) {
                Toast.makeText(this, "Please position your face properly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            timeInButton.isEnabled = false
            timeInButton.text = "Processing..."

            Handler(Looper.getMainLooper()).postDelayed({
                val currentAuth = FirebaseAuth.getInstance()
                val user = currentAuth.currentUser

                if (user == null) {
                    signInAnonymouslyForStorage { success ->
                        if (success) {
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            if (firebaseUser != null) {
                                checkAndCapturePhoto(userId ?: firebaseUser.uid)
                            } else {
                                Toast.makeText(this, "Authentication failed. Please login again.", Toast.LENGTH_SHORT).show()
                                resetTimeInButton()
                            }
                        } else {
                            Toast.makeText(this, "Authentication failed. Please login again.", Toast.LENGTH_SHORT).show()
                            resetTimeInButton()
                        }
                    }
                } else {
                    checkAndCapturePhoto(userId ?: user.uid)
                }
            }, 2000)
        }
    }

    private fun checkAndCapturePhoto(uid: String) {
        // Time check: Only allow between 9:00 AM and 10:00 AM
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val isAllowed = (currentHour == 9) || (currentHour == 10 && currentMinute == 0)

        if (!isAllowed) {
            AlertDialog.Builder(this)
                .setTitle("Time-In Not Allowed")
                .setMessage("You can only Time-In between 9:00 AM and 10:00 AM.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        checkAlreadyTimedIn(uid) { already ->
            if (already) {
                AlertDialog.Builder(this)
                    .setTitle("Already Timed In")
                    .setMessage("You already timed in today.")
                    .setPositiveButton("OK", null)
                    .show()
                // Re-enable button if already timed in and no action is taken
                timeInButton.isEnabled = true
                timeInButton.text = getString(R.string.button_time_in)
            } else {
                capturePhotoAndUpload(uid)
            }
        }
    }

    private fun signInAnonymouslyForStorage(callback: ((Boolean) -> Unit)? = null) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Authenticating")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener {
                Log.d(TAG, "Anonymous auth success")
                isAuthenticated = true

                // if (!userId.isNullOrEmpty()) { // userId is the student's ID, not Firebase UID
                //     Log.d(TAG, "New Firebase Auth UID: ${FirebaseAuth.getInstance().currentUser?.uid}")
                // }

                progressDialog.dismiss()
                callback?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous auth failed", e)
                progressDialog.dismiss()
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                callback?.invoke(false)
            }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            // setupCameraPreview() // Called in onCreate, might not be needed here unless view is destroyed and recreated
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding camera provider: ${e.message}")
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to Time-In.", Toast.LENGTH_LONG).show()
                finish() // Or handle more gracefully
            }
        }
    }

    private fun setupCameraPreview() {
        val container = findViewById<ViewGroup>(R.id.camera_container) // This is the CardView
        // Clear previous previewView if any, to avoid multiple previews
        container.findViewWithTag<PreviewView>("cameraPreviewTag")?.let {
            container.removeView(it)
        }
        findViewById<ImageView>(R.id.camera_preview_placeholder)?.visibility = View.GONE

        previewView = PreviewView(this).apply {
            tag = "cameraPreviewTag"
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        container.addView(previewView, 0) // Add previewView at index 0

        // Ensure faceBoxOverlay is on top of previewView
        (faceBoxOverlay.parent as? ViewGroup)?.removeView(faceBoxOverlay)
        container.addView(faceBoxOverlay)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                // .setTargetRotation(previewView.display.rotation) // Let CameraX handle rotation
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(640, 480)) // Adjust as needed
                .build().also {
                    it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) }
                }

            try {
                val cameraSelector = CameraSelector.Builder().requireLensFacing(currentLensFacing).build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
                Toast.makeText(this, "Camera initialization failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    if (previewView.width > 0 && previewView.height > 0) { // Ensure view is laid out
                        adjustBox(it, imageProxy.width, imageProxy.height, previewView.width, previewView.height, rotation)
                    } else null
                }
                runOnUiThread {
                    faceBoxOverlay.updateFaceBox(transformed, isFaceDetected)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failure: ", e)
                isFaceDetected = false
                runOnUiThread { faceBoxOverlay.updateFaceBox(null, false) }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun adjustBox(box: Rect, imgW: Int, imgH: Int, viewW: Int, viewH: Int, rotationDegrees: Int): RectF {
        val rect = RectF(box)

        // Transformation logic depends heavily on camera sensor orientation,
        // image rotation, preview scaling, and whether it's front/back camera.
        // This is a common source of issues.

        val matrix = Matrix()

        // 1. Scale the image to fit the view (considering FILL_CENTER)
        val imageAspectRatio = imgW.toFloat() / imgH.toFloat()
        val viewAspectRatio = viewW.toFloat() / viewH.toFloat()

        var scaleFactor: Float
        var postScaleWidth: Float
        var postScaleHeight: Float

        if (imageAspectRatio > viewAspectRatio) { // Image is wider than view (letterboxed if FIT_CENTER)
            scaleFactor = viewW.toFloat() / imgW.toFloat()
            postScaleWidth = viewW.toFloat()
            postScaleHeight = imgH * scaleFactor
        } else { // Image is taller than view (pillarboxed if FIT_CENTER)
            scaleFactor = viewH.toFloat() / imgH.toFloat()
            postScaleWidth = imgW * scaleFactor
            postScaleHeight = viewH.toFloat()
        }
        // For FILL_CENTER, the scale is such that the smaller dimension of the image fits the view,
        // and the larger dimension is cropped. Or rather, it fills the view and maintains aspect ratio.
        // Let's use the logic for FILL_CENTER:
        val previewRatio = viewW.toFloat() / viewH
        val imageActualRatio = if (rotationDegrees == 90 || rotationDegrees == 270) imgH.toFloat() / imgW else imgW.toFloat() / imgH

        var scaleX = 1f
        var scaleY = 1f

        if (previewRatio > imageActualRatio) { // Preview is wider than image
            scaleY = viewH.toFloat() / (if (rotationDegrees == 90 || rotationDegrees == 270) imgW.toFloat() else imgH.toFloat())
            scaleX = scaleY
        } else { // Preview is narrower or same aspect ratio
            scaleX = viewW.toFloat() / (if (rotationDegrees == 90 || rotationDegrees == 270) imgH.toFloat() else imgW.toFloat())
            scaleY = scaleX
        }

        matrix.postScale(scaleX, scaleY)


        // 2. Center the scaled image in the view
        val scaledImgW = (if (rotationDegrees == 90 || rotationDegrees == 270) imgH else imgW) * scaleX
        val scaledImgH = (if (rotationDegrees == 90 || rotationDegrees == 270) imgW else imgH) * scaleY

        val dx = (viewW - scaledImgW) / 2f
        val dy = (viewH - scaledImgH) / 2f
        matrix.postTranslate(dx, dy)


        // 3. If front camera, mirror the coordinates horizontally
        if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            matrix.postScale(-1f, 1f, viewW / 2f, viewH / 2f) // Mirror around view center
        }

        val newRect = RectF()
        matrix.mapRect(newRect, rect)
        return newRect
    }


    private fun checkAlreadyTimedIn(uid: String, callback: (Boolean) -> Unit) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        FirebaseDatabase.getInstance().getReference(DB_PATH_TIME_LOGS).child(uid)
            .orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val already = snapshot.children.any {
                        val type = it.child("type").getValue(String::class.java)
                        type == "TimeIn" // Timestamp already filtered by query
                    }
                    callback(already)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "checkAlreadyTimedIn onCancelled: ${error.message}")
                    callback(false)
                }
            })
    }

    private fun capturePhotoAndUpload(uid: String) {
        timeInButton.isEnabled = false
        timeInButton.text = "Processing..."

        val file = File(externalCacheDir, "Timed_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val photoFile = File(output.savedUri?.path ?: file.path)
                        val rotatedBitmap = fixImageOrientationAndFlip(photoFile) // Apply custom rotation/flip
                        FileOutputStream(photoFile).use { out ->
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // Compress after rotation
                        }
                        uploadImageToFirebase(Uri.fromFile(photoFile), uid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process image: ${e.message}", e)
                        Toast.makeText(this@TimeInActivity, "Image processing failed.", Toast.LENGTH_SHORT).show()
                        timeInButton.isEnabled = true
                        timeInButton.text = getString(R.string.button_time_in)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Image capture error: ${exc.message}", exc)
                    Toast.makeText(this@TimeInActivity, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                    timeInButton.isEnabled = true
                    timeInButton.text = getString(R.string.button_time_in)
                }
            }
        )
    }

    private fun fixImageOrientationAndFlip(photoFile: File): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val matrix = Matrix()

        // CameraX should provide correctly oriented images based on targetRotation or EXIF.
        // However, front camera often needs horizontal flipping for a "mirror" effect.
        // If CameraX output is already upright for the display, only flipping might be needed.

        // Attempt to read EXIF orientation
        val exif = ExifInterface(photoFile.inputStream())
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        Log.d(TAG, "EXIF Orientation: $orientation")

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            // Handle mirrored orientations if necessary
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            // More complex mirrored rotations can be added here
        }

        // If it's the front camera, apply a horizontal flip *after* EXIF rotation correction,
        // unless EXIF already handled mirroring.
        if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            // Check if EXIF was a mirrored orientation. If so, this might double-flip.
            // This is tricky. A common approach is to ensure upright and then flip.
            // If EXIF based rotation made it upright, then:
            matrix.postScale(-1f, 1f, originalBitmap.width / 2f, originalBitmap.height / 2f)
        }


        return Bitmap.createBitmap(
            originalBitmap, 0, 0,
            originalBitmap.width, originalBitmap.height, matrix, true
        )
    }


    private fun uploadImageToFirebase(uri: Uri, uid: String) {
        try {
            Log.d(TAG, "Starting upload to Firebase: $uri for UID: $uid")
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User is not authenticated for upload.")
                Toast.makeText(this, "Authentication error. Please retry.", Toast.LENGTH_SHORT).show()
                timeInButton.isEnabled = true; timeInButton.text = getString(R.string.button_time_in)
                return
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "selfie_$timestamp.jpg"
            val storageUid = currentUser.uid // Store under Firebase Auth UID for security rules
            val logsRef = FirebaseStorage.getInstance().getReference("timeLogs/$storageUid/$fileName")

            val file = File(uri.path ?: "")
            if (!file.exists() || !file.canRead()) {
                Log.e(TAG, "File cannot be read: ${file.absolutePath}")
                Toast.makeText(this, "File access error.", Toast.LENGTH_SHORT).show()
                timeInButton.isEnabled = true; timeInButton.text = getString(R.string.button_time_in)
                return
            }

            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg").build()

            logsRef.putFile(uri, metadata)
                .addOnSuccessListener {
                    logsRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        logTimeIn(downloadUrl.toString(), uid) // Log with the student's uid
                        showSuccessDialog()
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL", e)
                        Toast.makeText(this, "Upload failed (URL): ${e.message}", Toast.LENGTH_SHORT).show()
                        timeInButton.isEnabled = true; timeInButton.text = getString(R.string.button_time_in)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Upload failed (PUT)", e)
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    timeInButton.isEnabled = true; timeInButton.text = getString(R.string.button_time_in)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            Toast.makeText(this, "Error during upload: ${e.message}", Toast.LENGTH_SHORT).show()
            timeInButton.isEnabled = true; timeInButton.text = getString(R.string.button_time_in)
        }
    }

    private fun logTimeIn(imageUrl: String, studentUid: String) { // Renamed uid to studentUid for clarity
        val log = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeIn",
            "imageUrl" to imageUrl,
            "email" to userEmail,
            "firstName" to userFirstName,
            "userId" to studentUid, // This is the student's ID from intent
            "status" to "On Duty"
        )

        // Log under the student's ID in Realtime Database
        val ref = FirebaseDatabase.getInstance().getReference(DB_PATH_TIME_LOGS).child(studentUid)
        ref.push().setValue(log)
            .addOnSuccessListener {
                Log.d(TAG, "Time-In log successfully written to Realtime Database for UID: $studentUid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to write Time-In log for UID: $studentUid", e)
                Toast.makeText(this, "Failed to record time-in: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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
        try { // faceDetector might not be initialized if permissions were denied early
            if (::faceDetector.isInitialized) {
                faceDetector.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing face detector: ${e.message}")
        }
    }

    private fun resetTimeInButton() {
        timeInButton.isEnabled = true
        timeInButton.text = getString(R.string.button_time_in)
    }
}