package com.example.timed_mobile

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class ExcuseLetterActivity : AppCompatActivity() {

    private lateinit var datePicker: Button
    private lateinit var reasonSpinner: Spinner
    private lateinit var uploadButton: Button
    private lateinit var uploadedFilename: TextView
    private lateinit var detailsInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_letter_page)

        // Start top wave animation
        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Initialize views
        datePicker = findViewById(R.id.btn_date_picker)
        reasonSpinner = findViewById(R.id.spinner_reason)
        uploadButton = findViewById(R.id.btn_upload)
        uploadedFilename = findViewById(R.id.text_uploaded_filename)
        detailsInput = findViewById(R.id.edit_text_details)
        submitButton = findViewById(R.id.btn_submit_excuse)

        // Set up back button
        findViewById<ImageView>(R.id.icon_back_button).setOnClickListener {
            finish()
        }

        // Set up date picker
        datePicker.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up reason spinner
        val reasons = arrayOf("Illness/Medical", "Family Emergency", "Transportation Issue", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reasonSpinner.adapter = adapter

        // Set up upload button
        uploadButton.setOnClickListener {
            // Add file picker code
            selectFile()
        }

        // Set up submit button
        submitButton.setOnClickListener {
            submitExcuseLetter()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the date
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                datePicker.text = selectedDate
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"  // All file types
        startActivityForResult(intent, 1)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Get file name
                val cursor = contentResolver.query(uri, null, null, null, null)
                val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor?.moveToFirst()
                val fileName = nameIndex?.let { cursor.getString(it) } ?: "File selected"
                cursor?.close()

                // Display file name
                uploadedFilename.text = fileName
                uploadedFilename.visibility = View.VISIBLE
            }
        }
    }

    private fun submitExcuseLetter() {
        // Validate form
        val date = datePicker.text.toString()
        if (date == "Select date") {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val reason = reasonSpinner.selectedItem.toString()
        val details = detailsInput.text.toString().trim()

        if (details.isBlank()) {
            Toast.makeText(this, "Please provide details", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Create data to store in Firebase
        val excuseData = mapOf(
            "userId" to userId,
            "date" to date,
            "reason" to reason,
            "details" to details,
            "hasAttachment" to (uploadedFilename.visibility == View.VISIBLE),
            "timestamp" to System.currentTimeMillis()
        )

        // Save to Firebase
        val dbRef = FirebaseDatabase.getInstance().getReference("excuseLetters")
        dbRef.push().setValue(excuseData)
            .addOnSuccessListener {
                // Show success message and return to home page
                showSuccessDialog()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Submission failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.successs_popup_excuse_letter)
        dialog.setCancelable(false)

        // Set transparent background
        if (dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        // Set success message
        val message = dialog.findViewById<TextView>(R.id.popup_message)
        message.text = "Your excuse letter has been submitted successfully!"

        // Set up OK button
        val okButton = dialog.findViewById<Button>(R.id.btn_ok)
        okButton.setOnClickListener {
            dialog.dismiss()
            finish()  // Return to previous screen
        }

        dialog.show()
    }
}