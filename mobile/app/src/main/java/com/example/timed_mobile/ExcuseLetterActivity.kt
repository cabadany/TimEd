package com.example.timed_mobile

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ExcuseLetterActivity : AppCompatActivity() {

    private lateinit var datePicker: Button
    private lateinit var reasonSpinner: Spinner
    private lateinit var uploadButton: Button
    private lateinit var uploadedFilename: TextView
    private lateinit var detailsInput: EditText
    private lateinit var submitButton: Button
    private lateinit var backButton: ImageView

    private var selectedFileUri: Uri? = null
    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var idNumber: String? = null
    private var department: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_letter_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")
        idNumber = intent.getStringExtra("idNumber")
        department = intent.getStringExtra("department")

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        datePicker = findViewById(R.id.btn_date_picker)
        reasonSpinner = findViewById(R.id.spinner_reason)
        uploadButton = findViewById(R.id.btn_upload)
        uploadedFilename = findViewById(R.id.text_uploaded_filename)
        detailsInput = findViewById(R.id.edit_text_details)
        submitButton = findViewById(R.id.btn_submit_excuse)
        backButton = findViewById(R.id.icon_back_button)

        backButton.setOnClickListener {
            (it as? ImageView)?.drawable?.let { drawable ->
                if (drawable is AnimatedVectorDrawable) drawable.start()
            }
            it.postDelayed({ finish() }, 50)
        }

        datePicker.setOnClickListener { showDatePickerDialog() }

        val reasons = arrayOf("Illness/Medical", "Family Emergency", "Transportation Issue", "Others")
        reasonSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        uploadButton.setOnClickListener { selectFile() }
        submitButton.setOnClickListener { submitExcuseLetter() }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            datePicker.text = "$d/${m + 1}/$y"
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            selectedFileUri = data?.data
            selectedFileUri?.let { uri ->
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    it.moveToFirst()
                    uploadedFilename.text = it.getString(nameIndex)
                    uploadedFilename.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun submitExcuseLetter() {
        val date = datePicker.text.toString()
        val reason = reasonSpinner.selectedItem.toString()
        val details = detailsInput.text.toString().trim()

        if (date == "Select date" || details.isBlank()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId.isNullOrBlank()) {
            Toast.makeText(this, "Missing User ID", Toast.LENGTH_SHORT).show()
            return
        }

        if (department.isNullOrBlank()) {
            Toast.makeText(this, "Missing Department ID", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedFileUri != null) {
            uploadFileToFirebase(date, reason, details)
        } else {
            saveExcuseToFirestore(date, reason, details, null)
        }
    }

    private fun uploadFileToFirebase(date: String, reason: String, details: String) {
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().reference.child("excuse_documents/$filename")

        selectedFileUri?.let { uri ->
            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    saveExcuseToFirestore(date, reason, details, downloadUri.toString())
                }
                .addOnFailureListener {
                    Log.e("ExcuseLetter", "Upload failed", it)
                    Toast.makeText(this, "File upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveExcuseToFirestore(date: String, reason: String, details: String, fileUrl: String?) {
        val excuse = hashMapOf(
            "userId" to userId,
            "email" to userEmail,
            "firstName" to userFirstName,
            "idNumber" to idNumber,
            "department" to department,
            "date" to date,
            "reason" to reason,
            "details" to details,
            "attachmentUrl" to (fileUrl ?: ""),
            "status" to "Pending",
            "submittedAt" to System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId!!)
            .collection("excuseLetters")
            .add(excuse)
            .addOnSuccessListener {
                showSuccessDialog()
            }
            .addOnFailureListener {
                Log.e("ExcuseLetter", "Submit failed", it)
                Toast.makeText(this, "Failed to submit: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.successs_popup_excuse_letter)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<TextView>(R.id.popup_message).text = "Your excuse letter has been submitted successfully!"
        dialog.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}