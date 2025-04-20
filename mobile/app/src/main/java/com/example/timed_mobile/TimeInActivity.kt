package com.example.timed_mobile

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TimeInActivity : AppCompatActivity() {
    private var previewView: PreviewView? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageUri: Uri? = null

    companion object {
        private const val TAG = "TimeInActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_in_page)

        cameraExecutor = Executors.newSingleThreadExecutor()
        setupCamera()

        findViewById<ImageView>(R.id.icon_back_button).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btntime_in).setOnClickListener {
            takePhotoAndShowSuccess()
        }

        if (allPermissionsGranted()) {
            Handler(Looper.getMainLooper()).postDelayed({
                startCamera()
            }, 500)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permissions are required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupCamera() {
        val container = findViewById<ViewGroup>(R.id.camera_container)
        previewView = PreviewView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            visibility = View.INVISIBLE
        }
        container.addView(previewView)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                previewView?.let {
                    preview.setSurfaceProvider(it.surfaceProvider)
                    it.visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.camera_preview_placeholder).visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Camera setup failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndShowSuccess() {
        val imageCapture = imageCapture ?: return

        val timeInButton = findViewById<Button>(R.id.btntime_in)
        timeInButton.isEnabled = false
        timeInButton.text = "Processing..."

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "TimEd_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImageUri = output.savedUri
                    showFlashEffect()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showSuccessDialog()
                        logTimeInToFirebase()
                    }, 500)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    timeInButton.isEnabled = true
                    timeInButton.text = "Time - In"
                    Toast.makeText(baseContext, "Failed to take photo", Toast.LENGTH_SHORT).show()
                }
            })
    }

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
            flashView.animate().alpha(0f).setDuration(300).withEndAction {
                cameraContainer.removeView(flashView)
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "Flash effect error", e)
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_in)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        titleText.text = "Successfully Timed - In"
        messageText.text = "Thank you. It has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun logTimeInToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs")

        val log = mapOf(
            "userId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        dbRef.push().setValue(log).addOnSuccessListener {
            Log.d(TAG, "Time-in successfully logged in Firebase")
        }.addOnFailureListener {
            Log.e(TAG, "Failed to log time-in: ${it.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}