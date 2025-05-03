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
    private var currentLensFacing: Int = CameraSelector.LENS_FACING_FRONT

    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        previewView = PreviewView(this)
        imageCapture = ImageCapture.Builder().build()
        cameraExecutor = Executors.newSingleThreadExecutor()
        faceBoxOverlay = findViewById(R.id.face_box_overlay)
        timeInButton = findViewById(R.id.btntime_in)

        // 🔐 Ensure Firebase Auth is signed in
        if (FirebaseAuth.getInstance().currentUser == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener { Log.d(TAG, "Signed in anonymously") }
                .addOnFailureListener {
                    Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        // Animate back button
        val backBtn = findViewById<ImageView>(R.id.icon_back_button)
        backBtn.setOnClickListener {
            val drawable = backBtn.drawable
            if (drawable is AnimatedVectorDrawable) drawable.start()
            it.postDelayed({ finish() }, 50)
        }

        // Setup face detection
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(faceOptions)

        setupCameraPreview()

        timeInButton.setOnClickListener {
            if (!isFaceDetected) {
                Toast.makeText(this, "Please position your face properly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            checkAlreadyTimedIn(userId) { already ->
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

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupCameraPreview() {
        val container = findViewById<ViewGroup>(R.id.camera_container)
        previewView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        container.addView(previewView, 0)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor) { proxy -> processImage(proxy) }
                }

            val selector = CameraSelector.DEFAULT_FRONT_CAMERA
            currentLensFacing = selector.lensFacing ?: CameraSelector.LENS_FACING_FRONT

            provider.unbindAll()
            provider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalyzer)
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
            .addOnFailureListener { isFaceDetected = false }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun adjustBox(box: Rect, imgW: Int, imgH: Int, viewW: Int, viewH: Int): RectF {
        val rect = RectF(box)
        val scale = max(viewW.toFloat() / imgW, viewH.toFloat() / imgH)
        val offsetX = (viewW - imgW * scale) / 2f
        val offsetY = (viewH - imgH * scale) / 2f
        rect.left = rect.left * scale + offsetX
        rect.top = rect.top * scale + offsetY
        rect.right = rect.right * scale + offsetX
        rect.bottom = rect.bottom * scale + offsetY

        if (currentLensFacing == CameraSelector.LENS_FACING_FRONT) {
            val originalLeft = rect.left
            rect.left = viewW - rect.right
            rect.right = viewW - originalLeft
        }

        return rect
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
            .addOnFailureListener {
                Toast.makeText(this, "Could not verify time-in status", Toast.LENGTH_SHORT).show()
                callback(false)
            }
    }

    private fun capturePhotoAndUpload() {
        timeInButton.isEnabled = false
        timeInButton.text = "Processing..."

        val filename = "Timed_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
            Toast.makeText(this, "Failed to create file URI", Toast.LENGTH_SHORT).show()
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, imageUri, contentValues).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    uploadImageToFirebase(imageUri) { imageUrl ->
                        if (imageUrl != null) {
                            logTimeIn(imageUrl)
                            showSuccessDialog()
                        } else {
                            Toast.makeText(this@TimeInActivity, "Upload failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@TimeInActivity, "Photo capture failed.", Toast.LENGTH_SHORT).show()
                    timeInButton.isEnabled = true
                    timeInButton.text = "Time - In"
                }
            })
    }

    private fun uploadImageToFirebase(imageUri: Uri, callback: (String?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(null)
        val filename = "Timed_${System.currentTimeMillis()}.jpg"
        val ref = FirebaseStorage.getInstance().getReference("uploads/$uid/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener {
                    Log.e(TAG, "Failed to get image URL: ${it.message}")
                    callback(null)
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Image upload failed: ${it.message}")
                callback(null)
            }
    }

    private fun logTimeIn(imageUrl: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(uid)
        val log = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeIn",
            "imageUrl" to imageUrl
        )

        ref.push().setValue(log)
            .addOnSuccessListener { Log.d(TAG, "Logged with image") }
            .addOnFailureListener { Log.e(TAG, "Log failed: ${it.message}") }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<TextView>(R.id.popup_title).text = "Successfully Timed - In"
        dialog.findViewById<TextView>(R.id.popup_message).text = "Your attendance has been recorded."
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
    }
}