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
import android.view.Gravity // Added for tutorial
import android.view.LayoutInflater // Added for tutorial
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager // Added for tutorial
import android.view.animation.AlphaAnimation // Added for tutorial
import android.view.animation.AnimationSet // Added for tutorial
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation // Added for tutorial
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
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
import kotlin.math.abs // Added for tutorial
import kotlin.math.max

import androidx.exifinterface.media.ExifInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

class TimeInActivity : WifiSecurityActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceBoxOverlay: FaceBoxOverlay
    private lateinit var faceDetector: FaceDetector
    private lateinit var timeInButton: Button
    private lateinit var backButton: ImageView
    private var isFaceDetected = false
    private var isFrontCamera = true
    private var currentLensFacing = CameraSelector.LENS_FACING_FRONT

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var isAuthenticated = false

    private lateinit var titleName: TextView
    private lateinit var positionCameraViewText: TextView
    private lateinit var cameraContainerCard: CardView
    private lateinit var iconQrScanner: ImageView
    private lateinit var qrScannerClickReminderText: TextView

    // --- Tutorial Members ---
    private var isInTutorialMode: Boolean = false
    private var currentTutorialPopupWindow: PopupWindow? = null
    private lateinit var tutorialOverlay: FrameLayout // IMPORTANT: Add this ID to your time_in_page.xml
    private var previousTargetLocationForAnimation: IntArray? = null
    private var currentTutorialStepForActivity: Int = 0
    // --- End Tutorial Members ---


    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val DB_PATH_TIME_LOGS = "timeLogs"

        // --- Tutorial Constants ---
        private const val TOTAL_TIME_IN_ACTIVITY_TUTORIAL_STEPS = 3 // Preview, Face Position, Button
        // --- End Tutorial Constants ---
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")
        isInTutorialMode = intent.getBooleanExtra(HomeActivity.EXTRA_IS_TUTORIAL_MODE, false)


        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceBoxOverlay = findViewById(R.id.face_box_overlay)

        backButton = findViewById(R.id.icon_back_button)
        timeInButton = findViewById(R.id.btntime_in)
        titleName = findViewById(R.id.titleName)
        positionCameraViewText = findViewById(R.id.position_camera_view)
        cameraContainerCard = findViewById(R.id.camera_container)
        iconQrScanner = findViewById(R.id.icon_qr_scanner)
        qrScannerClickReminderText = findViewById(R.id.qr_scanner_click_reminder)

        // --- Tutorial Overlay Initialization ---
        // IMPORTANT: Ensure you have a FrameLayout with id "time_in_tutorial_overlay" in your time_in_page.xml
        tutorialOverlay = findViewById(R.id.time_in_tutorial_overlay)
        // --- End Tutorial Overlay Initialization ---

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        // --- START OF ENTRY ANIMATION CODE ---
        if (!isInTutorialMode) { // Optionally skip entry animations if in tutorial mode
            var currentDelay = 100L
            fun animateView(view: View, animationResId: Int, delay: Long) {
                val anim = AnimationUtils.loadAnimation(view.context, animationResId)
                anim.startOffset = delay
                view.startAnimation(anim)
            }
            animateView(backButton, R.anim.fade_in, currentDelay); currentDelay += 100L
            animateView(titleName, R.anim.slide_down_fade_in, currentDelay); currentDelay += 100L
            animateView(positionCameraViewText, R.anim.slide_down_fade_in, currentDelay); currentDelay += 150L
            animateView(cameraContainerCard, R.anim.fade_in, currentDelay); currentDelay += 200L
            animateView(iconQrScanner, R.anim.fade_in, currentDelay); currentDelay += 100L
            animateView(qrScannerClickReminderText, R.anim.slide_up_fade_in_content, currentDelay); currentDelay += 100L
            animateView(timeInButton, R.anim.slide_up_fade_in_content, currentDelay)
        }
        // --- END OF ENTRY ANIMATION CODE ---

        backButton.setOnClickListener { finish() }

        iconQrScanner.setOnClickListener {
            val intent = Intent(this, TimeInEventActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("email", userEmail)
            intent.putExtra("firstName", userFirstName)
            startActivity(intent)
        }
        qrScannerClickReminderText.setOnClickListener {
            iconQrScanner.performClick()
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            isAuthenticated = true
        } else {
            signInAnonymouslyForStorage { success -> isAuthenticated = success }
        }

        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)

        setupCameraPreview()

        if (allPermissionsGranted()) {
            startCamera()
            if (isInTutorialMode) {
                // Delay tutorial start slightly to ensure camera preview is active
                // and entry animations (if not skipped) are less likely to interfere.
                Handler(Looper.getMainLooper()).postDelayed({
                    startTimeInActivityTutorial()
                }, if (isInTutorialMode) 800L else 0L) // Longer delay if animations run
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        timeInButton.setOnClickListener {
            if (isInTutorialMode) { // If in tutorial, button click might be part of tutorial
                // Let tutorial handler manage it, or if it's the final step, it will proceed.
                // For now, assume tutorial handler will call `setResult` and `finish`.
                // If this button is the target of the *last* tutorial step, its `onNext`
                // in `showCustomTutorialDialogInTimeIn` should handle completion.
                // We prevent normal time-in logic during tutorial.
                Toast.makeText(this, "Tutorial in progress. Follow guide.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isFaceDetected) {
                Toast.makeText(this, "Please position your face properly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            timeInButton.isEnabled = false
            timeInButton.text = "Processing..."
            Handler(Looper.getMainLooper()).postDelayed({
                val currentAuthInner = FirebaseAuth.getInstance()
                val userInner = currentAuthInner.currentUser
                if (userInner == null) {
                    signInAnonymouslyForStorage { success ->
                        if (success) {
                            val firebaseUser = FirebaseAuth.getInstance().currentUser
                            if (firebaseUser != null) checkAndCapturePhoto(userId ?: firebaseUser.uid)
                            else { Toast.makeText(this, "Auth failed.", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
                        } else { Toast.makeText(this, "Auth failed.", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
                    }
                } else {
                    checkAndCapturePhoto(userId ?: userInner.uid)
                }
            }, 2000)
        }
    }

    private fun checkAndCapturePhoto(uid: String) {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        // val currentMinute = now.get(Calendar.MINUTE) // Not used in original logic
        // val isAllowed = (currentHour == 9) || (currentHour == 10 && currentMinute == 0) // Original logic
        val isAllowed = currentHour in 0..23 // Allowing for testing, revert to original if needed

        if (!isAllowed) {
            AlertDialog.Builder(this)
                .setTitle("Time-In Not Allowed")
                .setMessage("You can only Time-In between 9:00 AM and 10:00 AM.") // Update if time changes
                .setPositiveButton("OK", null)
                .show()
            resetTimeInButton() // Reset button since action is disallowed
            return
        }
        checkAlreadyTimedIn(uid) { already ->
            if (already) {
                AlertDialog.Builder(this)
                    .setTitle("Already Timed In")
                    .setMessage("You already timed in today.")
                    .setPositiveButton("OK", null)
                    .show()
                resetTimeInButton()
            } else {
                capturePhotoAndUpload(uid)
            }
        }
    }

    private fun signInAnonymouslyForStorage(callback: ((Boolean) -> Unit)? = null) {
        // ... (existing code - no changes needed for tutorial) ...
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
        if (allPermissionsGranted() && !isInTutorialMode) { // Don't restart camera if tutorial is active and might be mid-flow
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isInTutorialMode) { // Don't unbind if tutorial popup might be showing over it
            try {
                ProcessCameraProvider.getInstance(this).get().unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding camera provider: ${e.message}")
            }
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
                if (isInTutorialMode) {
                    Handler(Looper.getMainLooper()).postDelayed({ // Delay to ensure camera is ready
                        startTimeInActivityTutorial()
                    }, 500L)
                }
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
                if (isInTutorialMode) {
                    Toast.makeText(this, "Tutorial cannot proceed without camera.", Toast.LENGTH_LONG).show()
                    setResult(RESULT_CANCELED) // Indicate tutorial couldn't complete
                    finish()
                } else {
                    finish()
                }
            }
        }
    }

    private fun setupCameraPreview() {
        // ... (existing code - no changes needed for tutorial) ...
        val container = findViewById<ViewGroup>(R.id.camera_container)
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
        container.addView(previewView, 0)
        (faceBoxOverlay.parent as? ViewGroup)?.removeView(faceBoxOverlay)
        container.addView(faceBoxOverlay)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        // ... (existing code - no changes needed for tutorial) ...
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(640, 480))
                .build().also {
                    it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) }
                }
            try {
                val cameraSelector = CameraSelector.Builder().requireLensFacing(currentLensFacing).build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        // ... (existing code - no changes needed for tutorial) ...
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                isFaceDetected = faces.isNotEmpty()
                val bounds = faces.firstOrNull()?.boundingBox
                val transformed = bounds?.let {
                    if (previewView.width > 0 && previewView.height > 0) {
                        adjustBox(it, imageProxy.width, imageProxy.height, previewView.width, previewView.height, rotation)
                    } else null
                }
                runOnUiThread { faceBoxOverlay.updateFaceBox(transformed, isFaceDetected) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Face detection failure: ", e); isFaceDetected = false; runOnUiThread { faceBoxOverlay.updateFaceBox(null, false) } }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun adjustBox(box: Rect, imgW: Int, imgH: Int, viewW: Int, viewH: Int, rotationDegrees: Int): RectF {
        // ... (existing code - no changes needed for tutorial) ...
        val rect = RectF(box)
        val matrix = Matrix()
        val imageActualRatio = if (rotationDegrees == 90 || rotationDegrees == 270) imgH.toFloat() / imgW else imgW.toFloat() / imgH
        val previewRatio = viewW.toFloat() / viewH
        var scaleX = 1f; var scaleY = 1f
        if (previewRatio > imageActualRatio) {
            scaleY = viewH.toFloat() / (if (rotationDegrees == 90 || rotationDegrees == 270) imgW.toFloat() else imgH.toFloat())
            scaleX = scaleY
        } else {
            scaleX = viewW.toFloat() / (if (rotationDegrees == 90 || rotationDegrees == 270) imgH.toFloat() else imgW.toFloat())
            scaleY = scaleX
        }
        matrix.postScale(scaleX, scaleY)
        val scaledImgW = (if (rotationDegrees == 90 || rotationDegrees == 270) imgH else imgW) * scaleX
        val scaledImgH = (if (rotationDegrees == 90 || rotationDegrees == 270) imgW else imgH) * scaleY
        val dx = (viewW - scaledImgW) / 2f
        val dy = (viewH - scaledImgH) / 2f
        matrix.postTranslate(dx, dy)
        if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            matrix.postScale(-1f, 1f, viewW / 2f, viewH / 2f)
        }
        val newRect = RectF(); matrix.mapRect(newRect, rect)
        return newRect
    }

    private fun checkAlreadyTimedIn(uid: String, callback: (Boolean) -> Unit) {
        // ... (existing code - no changes needed for tutorial) ...
        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        FirebaseDatabase.getInstance().getReference(DB_PATH_TIME_LOGS).child(uid)
            .orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val already = snapshot.children.any { it.child("type").getValue(String::class.java) == "TimeIn" }
                    callback(already)
                }
                override fun onCancelled(error: DatabaseError) { callback(false) }
            })
    }

    private fun capturePhotoAndUpload(uid: String) {
        // ... (existing code - no changes needed for tutorial) ...
        timeInButton.isEnabled = false; timeInButton.text = "Processing..."
        val file = File(externalCacheDir, "Timed_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val photoFile = File(output.savedUri?.path ?: file.path)
                    val rotatedBitmap = fixImageOrientationAndFlip(photoFile)
                    FileOutputStream(photoFile).use { out -> rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                    uploadImageToFirebase(Uri.fromFile(photoFile), uid)
                } catch (e: Exception) { Log.e(TAG, "Failed to process image: ${e.message}", e); Toast.makeText(this@TimeInActivity, "Image processing failed.", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
            }
            override fun onError(exc: ImageCaptureException) { Log.e(TAG, "Image capture error: ${exc.message}", exc); Toast.makeText(this@TimeInActivity, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
        })
    }

    private fun fixImageOrientationAndFlip(photoFile: File): Bitmap {
        // ... (existing code - no changes needed for tutorial) ...
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val matrix = Matrix()
        val exif = ExifInterface(photoFile.inputStream())
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        Log.d(TAG, "EXIF Orientation: $orientation")
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            matrix.postScale(-1f, 1f, originalBitmap.width / 2f, originalBitmap.height / 2f)
        }
        return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
    }

    private fun uploadImageToFirebase(uri: Uri, uid: String) {
        // ... (existing code - no changes needed for tutorial) ...
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) { Toast.makeText(this, "Auth error.", Toast.LENGTH_SHORT).show(); resetTimeInButton(); return }
            val timestamp = System.currentTimeMillis(); val fileName = "selfie_$timestamp.jpg"
            val storageUid = currentUser.uid; val logsRef = FirebaseStorage.getInstance().getReference("timeLogs/$storageUid/$fileName")
            val file = File(uri.path ?: ""); if (!file.exists() || !file.canRead()) { Toast.makeText(this, "File access error.", Toast.LENGTH_SHORT).show(); resetTimeInButton(); return }
            val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()
            logsRef.putFile(uri, metadata)
                .addOnSuccessListener { taskSnapshot ->
                    logsRef.downloadUrl.addOnSuccessListener { downloadUrl -> logTimeIn(downloadUrl.toString(), uid); showSuccessDialog() }
                        .addOnFailureListener { e -> Log.e(TAG, "Failed to get download URL", e); Toast.makeText(this, "Upload failed (URL): ${e.message}", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
                }
                .addOnFailureListener { e -> Log.e(TAG, "Upload failed (PUT)", e); Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
        } catch (e: Exception) { Log.e(TAG, "Upload exception", e); Toast.makeText(this, "Error during upload: ${e.message}", Toast.LENGTH_SHORT).show(); resetTimeInButton() }
    }

    private fun logTimeIn(imageUrl: String, studentUid: String) {
        // ... (existing code - no changes needed for tutorial) ...
        val log = mapOf("timestamp" to System.currentTimeMillis(), "type" to "TimeIn", "imageUrl" to imageUrl, "email" to userEmail, "firstName" to userFirstName, "userId" to studentUid, "status" to "On Duty")
        val ref = FirebaseDatabase.getInstance().getReference(DB_PATH_TIME_LOGS).child(studentUid)
        ref.push().setValue(log)
            .addOnSuccessListener { Log.d(TAG, "Time-In log successfully written for UID: $studentUid") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to write Time-In log for UID: $studentUid", e); Toast.makeText(this, "Failed to record time-in: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun showSuccessDialog() {
        // ... (existing code - no changes needed for tutorial) ...
        val dialog = Dialog(this); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in); dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.findViewById<Button>(R.id.popup_close_button).setOnClickListener { dialog.dismiss(); setResult(RESULT_OK, Intent().putExtra("TIMED_IN_SUCCESS", true)); finish() }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        try { if (::faceDetector.isInitialized) faceDetector.close() } catch (e: Exception) { Log.e(TAG, "Error closing face detector: ${e.message}") }
        // --- Tutorial Cleanup ---
        currentTutorialPopupWindow?.dismiss() // Dismiss if activity is destroyed
        currentTutorialPopupWindow = null
        // --- End Tutorial Cleanup ---
    }

    private fun resetTimeInButton() {
        timeInButton.isEnabled = true
        timeInButton.text = getString(R.string.button_time_in)
    }

    // --- TUTORIAL METHODS ---
    private fun startTimeInActivityTutorial() {
        currentTutorialStepForActivity = 0 // Reset
        if (!::tutorialOverlay.isInitialized) {
            Log.e(TAG, "Tutorial overlay not initialized! Cannot start tutorial.")
            setResult(RESULT_CANCELED) // Indicate tutorial couldn't run
            finish()
            return
        }
        tutorialOverlay.visibility = View.VISIBLE
        previousTargetLocationForAnimation = null // Reset for this activity's tutorial
        showCameraPreviewTutorialStep()
    }

    private fun showCameraPreviewTutorialStep() {
        currentTutorialStepForActivity = 1
        val cameraContainer = findViewById<View>(R.id.camera_container) // The CardView
        showCustomTutorialDialogInTimeIn(
            "This is the camera preview. Your face will appear here once the camera starts.",
            cameraContainer,
            currentTutorialStepForActivity,
            TOTAL_TIME_IN_ACTIVITY_TUTORIAL_STEPS
        ) {
            showFacePositioningTutorialStep()
        }
    }

    private fun showFacePositioningTutorialStep() {
        currentTutorialStepForActivity = 2
        // Target the text view above the camera or the camera container itself
        val instructionText = findViewById<View>(R.id.position_camera_view)
        showCustomTutorialDialogInTimeIn(
            "Position your face within the highlighted box that will appear. Ensure good lighting for accurate detection.",
            instructionText, // Or cameraContainerCard
            currentTutorialStepForActivity,
            TOTAL_TIME_IN_ACTIVITY_TUTORIAL_STEPS
        ) {
            showActualTimeInButtonTutorialStep()
        }
    }

    private fun showActualTimeInButtonTutorialStep() {
        currentTutorialStepForActivity = 3
        val timeInButtonView = findViewById<View>(R.id.btntime_in)
        showCustomTutorialDialogInTimeIn(
            "Once your face is detected and positioned correctly, this button will become active. Tap it to capture your Time-In photo.",
            timeInButtonView,
            currentTutorialStepForActivity,
            TOTAL_TIME_IN_ACTIVITY_TUTORIAL_STEPS
        ) {
            // Last step for TimeInActivity's tutorial part
            if (::tutorialOverlay.isInitialized) tutorialOverlay.visibility = View.GONE
            previousTargetLocationForAnimation = null
            Toast.makeText(this, "Time-In screen guide finished.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK) // Signal completion to HomeActivity
            finish() // Return to HomeActivity
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomTutorialDialogInTimeIn(
        message: String,
        targetView: View,
        currentStep: Int,
        totalSteps: Int,
        onNext: () -> Unit
    ) {
        currentTutorialPopupWindow?.dismiss() // Dismiss any existing popup

        if (!::tutorialOverlay.isInitialized) {
            Log.e(TAG, "Tutorial overlay not initialized in showCustomTutorialDialogInTimeIn!")
            onNext() // Skip step
            return
        }
        tutorialOverlay.visibility = View.VISIBLE // Ensure overlay is visible

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null) // Ensure this layout is available
        val progressTextView = dialogView.findViewById<TextView>(R.id.tutorial_progress_text)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_tutorial_step)

        progressTextView.text = "Step $currentStep of $totalSteps (Time-In Screen)"
        messageTextView.text = message

        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val dialogWidth = dialogView.measuredWidth.takeIf { it > 0 } ?: (resources.displayMetrics.widthPixels * 0.8).toInt()
        val dialogHeight = dialogView.measuredHeight.takeIf { it > 0 } ?: ViewGroup.LayoutParams.WRAP_CONTENT

        var finalDialogX: Int
        var finalDialogY: Int
        val currentTargetLocationOnScreen = IntArray(2)
        targetView.getLocationOnScreen(currentTargetLocationOnScreen)

        val margin = (16 * resources.displayMetrics.density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // Simplified positioning: Try below, then above, then center.
        finalDialogY = currentTargetLocationOnScreen[1] + targetView.height + margin / 2
        if (finalDialogY + dialogHeight > screenHeight - margin) { // Not enough space below
            finalDialogY = currentTargetLocationOnScreen[1] - dialogHeight - margin / 2
            if (finalDialogY < margin) { // Not enough space above
                finalDialogY = (screenHeight - dialogHeight) / 2 // Center vertically
            }
        }
        finalDialogX = currentTargetLocationOnScreen[0] + targetView.width / 2 - dialogWidth / 2
        if (finalDialogX < margin) finalDialogX = margin
        if (finalDialogX + dialogWidth > screenWidth - margin) finalDialogX = screenWidth - dialogWidth - margin


        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        currentTutorialPopupWindow = popupWindow
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true // Allow dismissing by touching outside
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // For rounded corners if dialog bg is shaped

        var isProceeding = false
        popupWindow.setOnDismissListener {
            if (!isProceeding) {
                handleTutorialCancellationInTimeIn()
            }
        }

        // Animation (adapted from HomeActivity)
        val animationSet = AnimationSet(true)
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 300
        animationSet.addAnimation(alphaAnimation)

        var startTranslateX = 0f
        var startTranslateY = 0f
        if (previousTargetLocationForAnimation != null) {
            val prevDialogEstimateX = previousTargetLocationForAnimation!![0] + (targetView.width / 2) - (dialogWidth / 2)
            val prevDialogEstimateY = previousTargetLocationForAnimation!![1] + targetView.height + margin / 2
            startTranslateX = (prevDialogEstimateX - finalDialogX).toFloat()
            startTranslateY = (prevDialogEstimateY - finalDialogY).toFloat()
        } else {
            startTranslateX = -dialogWidth.toFloat() * 0.2f
            startTranslateY = (20 * resources.displayMetrics.density)
        }
        val translateAnimation = TranslateAnimation(startTranslateX, 0f, startTranslateY, 0f)
        translateAnimation.duration = 450
        translateAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.overshoot_interpolator)
        animationSet.addAnimation(translateAnimation)
        dialogView.startAnimation(animationSet)

        popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, finalDialogX, finalDialogY)
        previousTargetLocationForAnimation = intArrayOf(finalDialogX, finalDialogY) // Store dialog's own location

        nextButton.setOnClickListener {
            isProceeding = true
            popupWindow.dismiss()
            onNext()
        }
        closeButton.setOnClickListener {
            isProceeding = true // To prevent double call from onDismissListener
            popupWindow.dismiss()
            handleTutorialCancellationInTimeIn()
        }
    }

    private fun handleTutorialCancellationInTimeIn() {
        if (::tutorialOverlay.isInitialized) tutorialOverlay.visibility = View.GONE
        currentTutorialPopupWindow?.dismiss() // Ensure it's dismissed
        currentTutorialPopupWindow = null
        previousTargetLocationForAnimation = null
        Toast.makeText(this, "Time-In guide cancelled.", Toast.LENGTH_SHORT).show()
        setResult(RESULT_CANCELED) // Signal cancellation to HomeActivity
        finish()
    }

    override fun onBackPressed() {
        if (isInTutorialMode && currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing) {
            handleTutorialCancellationInTimeIn()
        } else {
            super.onBackPressed()
        }
    }
}