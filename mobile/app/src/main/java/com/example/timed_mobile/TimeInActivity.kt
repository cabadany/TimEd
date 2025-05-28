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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
    private var isFrontCamera = true
    private var currentLensFacing = CameraSelector.LENS_FACING_FRONT

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var isAuthenticated = false

    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val DB_PATH_TIME_LOGS = "timeLogs"
        private const val DB_PATH_ATTENDANCE = "attendance"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceBoxOverlay = findViewById(R.id.face_box_overlay)
        timeInButton = findViewById(R.id.btntime_in)
        backButton = findViewById(R.id.icon_back_button)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        backButton.setOnClickListener { finish() }

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
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed. Please login again.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@setOnClickListener
            }

            checkAndCapturePhoto(userId ?: user.uid)
        }
    }

    private fun checkAndCapturePhoto(uid: String) {
        checkAlreadyTimedIn(uid) { already ->
            if (already) {
                AlertDialog.Builder(this)
                    .setTitle("Already Timed In")
                    .setMessage("You already timed in today.")
                    .setPositiveButton("OK", null)
                    .show()
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

                if (!userId.isNullOrEmpty()) {
                    // Here you would typically link the anonymous account with the userId
                    // This depends on your authentication setup
                    // For now, we'll just use the new anonymous user ID
                    Log.d(TAG, "New Firebase Auth UID: ${FirebaseAuth.getInstance().currentUser?.uid}")
                }

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
            setupCameraPreview()
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        ProcessCameraProvider.getInstance(this).get().unbindAll()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) startCamera()
    }

    private fun setupCameraPreview() {
        val container = findViewById<ViewGroup>(R.id.camera_container)
        container.removeAllViews()
        findViewById<ImageView>(R.id.camera_preview_placeholder)?.visibility = View.GONE

        previewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        container.addView(previewView)
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
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(640, 480))
                .build().also {
                    it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) }
                }

            try {
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
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
                    adjustBox(it, imageProxy.width, imageProxy.height, previewView.width, previewView.height)
                }
                runOnUiThread {
                    faceBoxOverlay.updateFaceBox(transformed, isFaceDetected)
                }
            }
            .addOnFailureListener {
                isFaceDetected = false
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun adjustBox(box: Rect, imgW: Int, imgH: Int, viewW: Int, viewH: Int): RectF {
        val rect = RectF(box)
        val scaleX = viewW.toFloat() / imgH
        val scaleY = viewH.toFloat() / imgW
        val scale = max(scaleX, scaleY)

        val offsetX = (viewW - imgH * scale) / 2f
        val offsetY = (viewH - imgW * scale) / 2f

        return RectF(
            viewW - (rect.right * scaleX + offsetX),
            rect.top * scaleY + offsetY,
            viewW - (rect.left * scaleX + offsetX),
            rect.bottom * scaleY + offsetY
        )
    }

    private fun checkAlreadyTimedIn(uid: String, callback: (Boolean) -> Unit) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        FirebaseDatabase.getInstance().getReference(DB_PATH_TIME_LOGS).child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val already = snapshot.children.any {
                        val timestamp = it.child("timestamp").getValue(Long::class.java) ?: return@any false
                        val type = it.child("type").getValue(String::class.java) ?: return@any false
                        timestamp >= today && type == "TimeIn"
                    }
                    callback(already)
                }

                override fun onCancelled(error: DatabaseError) {
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

                        // 🔄 FIX ROTATION BASED ON EXIF + FLIP
                        val rotatedBitmap = fixImageOrientationAndFlip(photoFile)

                        // Overwrite file with rotated image
                        FileOutputStream(photoFile).use { out ->
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }

                        uploadImageToFirebase(Uri.fromFile(photoFile), uid)
                    } catch (e: Exception) {
                        Log.e("TimeInActivity", "Failed to rotate image: ${e.message}", e)
                        Toast.makeText(
                            this@TimeInActivity,
                            "Image processing failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        timeInButton.isEnabled = true
                        timeInButton.text = "Time - In"
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@TimeInActivity,
                        "Capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    timeInButton.isEnabled = true
                    timeInButton.text = "Time - In"
                }
            }
        )
    }

    private fun fixImageOrientationAndFlip(photoFile: File): Bitmap {
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

        val matrix = Matrix()
        matrix.postRotate(90f)
        matrix.preScale(-1f, 1f)

        return Bitmap.createBitmap(
            originalBitmap,
            0, 0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
    }

    private fun uploadImageToFirebase(uri: Uri, uid: String) {
        try {
            Log.d(TAG, "Starting upload to Firebase: $uri")
            Log.d(TAG, "Upload for UID: $uid")

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e(TAG, "User is not authenticated")
                Toast.makeText(this, "Authentication error. Please retry.", Toast.LENGTH_SHORT).show()
                timeInButton.isEnabled = true
                timeInButton.text = "Time - In"
                return
            }

            val timestamp = System.currentTimeMillis()
            val fileName = "selfie_$timestamp.jpg"
            val logsRef = FirebaseStorage.getInstance().getReference("timeLogs/$uid/$fileName")

            val file = File(uri.path ?: "")
            if (!file.exists() || !file.canRead()) {
                Log.e(TAG, "File cannot be read: ${file.absolutePath}")
                Toast.makeText(this, "File access error", Toast.LENGTH_SHORT).show()
                timeInButton.isEnabled = true
                timeInButton.text = "Time - In"
                return
            }

            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            logsRef.putFile(uri, metadata)
                .addOnSuccessListener {
                    logsRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        logTimeIn(downloadUrl.toString(), uid)
                        showSuccessDialog()
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get download URL", e)
                        Toast.makeText(this, "Download URL failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        timeInButton.isEnabled = true
                        timeInButton.text = "Time - In"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Upload failed", e)
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    timeInButton.isEnabled = true
                    timeInButton.text = "Time - In"
                }

        } catch (e: Exception) {
            Log.e(TAG, "Upload exception", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            timeInButton.isEnabled = true
            timeInButton.text = "Time - In"
        }
    }

    private fun logTimeIn(imageUrl: String, uid: String) {
        val log = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeIn",
            "imageUrl" to imageUrl,
            "email" to userEmail,
            "firstName" to userFirstName,
            "userId" to uid
        )

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(uid)
        ref.push().setValue(log)
            .addOnSuccessListener {
                Log.d(TAG, "Time-In log successfully written to Realtime Database")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to write Time-In log", e)
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
        faceDetector.close()
    }
}