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
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import android.app.ProgressDialog
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ExcuseLetterActivity : AppCompatActivity() {

    private lateinit var datePicker: Button
    private lateinit var reasonSpinner: Spinner
    private lateinit var uploadButton: Button
    private lateinit var uploadedFilename: TextView
    private lateinit var detailsInput: EditText
    private lateinit var submitButton: Button
    private lateinit var backButton: ImageView
    private lateinit var daysAbsentInput: EditText
    private lateinit var excuseLetterTitle: TextView

    private var selectedFileUri: Uri? = null
    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var idNumber: String? = null
    private var department: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_letter_page)

        backButton = findViewById(R.id.icon_back_button)

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener { setupViews() }
                .addOnFailureListener {
                    Toast.makeText(this, "Firebase Auth failed", Toast.LENGTH_SHORT).show()
                    setupViews()
                }
        } else {
            setupViews()
        }
    }

    private fun setupViews() {
        userId = FirebaseAuth.getInstance().currentUser?.uid
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
        daysAbsentInput = findViewById(R.id.edit_text_days_absent)
        excuseLetterTitle = findViewById(R.id.excuse_letter_title)

        datePicker.setOnClickListener { showDatePickerDialog() }

        val reasons = arrayOf("Illness/Medical", "Family Emergency", "Transportation Issue", "Others")
        reasonSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        uploadButton.setOnClickListener { selectFile() }
        submitButton.setOnClickListener { checkAndSubmit() }

        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        val animSlideUpFormElement = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_form_element)
        val animSlideUpButton = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)

        fun animateView(view: View, animationResource: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResource)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        var currentDelay = 50L

        animateView(backButton, R.anim.fade_in, currentDelay)
        currentDelay += 100L

        animateView(excuseLetterTitle, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        val formElementsToAnimate = listOf(
            datePicker,
            daysAbsentInput,
            reasonSpinner,
            detailsInput,
            uploadButton
        )

        formElementsToAnimate.forEach { view ->
            animateView(view, R.anim.slide_up_fade_in_form_element, currentDelay)
            currentDelay += 100L
        }

        animateView(submitButton, R.anim.slide_up_fade_in_bottom, currentDelay)
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

    private fun checkAndSubmit() {
        val date = datePicker.text.toString()
        val reason = reasonSpinner.selectedItem.toString()
        val details = detailsInput.text.toString().trim()
        val daysAbsent = daysAbsentInput.text.toString().trim()

        if (date == "Select date" || details.isBlank() || daysAbsent.isBlank()) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // ✨ Block past dates
        val dateParts = date.split("/")
        if (dateParts.size != 3) {
            Toast.makeText(this, "Invalid date format.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCalendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
            set(Calendar.MONTH, dateParts[1].toInt() - 1)
            set(Calendar.YEAR, dateParts[2].toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedCalendar.before(today)) {
            Toast.makeText(this, "Cannot submit an excuse for a past date.", Toast.LENGTH_SHORT).show()
            return
        }

        if (reason == "Others" && details.isBlank()) {
            Toast.makeText(this, "Please specify the reason in details.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId.isNullOrBlank() || department.isNullOrBlank()) {
            Toast.makeText(this, "Missing user information.", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = ProgressDialog(this).apply {
            setMessage("Submitting, please wait...")
            setCancelable(false)
            show()
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("excuseLetters").child(userId!!)

        dbRef.orderByChild("date").equalTo(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        progress.dismiss()
                        Toast.makeText(this@ExcuseLetterActivity, "You’ve already submitted for this date.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (selectedFileUri != null) {
                            uploadFileToFirebase(date, reason, details, daysAbsent, progress)
                        } else {
                            saveExcuseToRealtimeDB(date, reason, details, daysAbsent, null, progress)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progress.dismiss()
                    Log.e("ExcuseLetter", "Firebase DB error: ${error.message}", error.toException())
                    Toast.makeText(this@ExcuseLetterActivity, "Error checking previous submissions.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun uploadFileToFirebase(date: String, reason: String, details: String, daysAbsent: String, progress: ProgressDialog) {
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().reference.child("excuse_documents/$userId/$filename")

        selectedFileUri?.let { uri ->
            storageRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    saveExcuseToRealtimeDB(date, reason, details, daysAbsent, downloadUri.toString(), progress)
                }
                .addOnFailureListener {
                    progress.dismiss()
                    Toast.makeText(this, "File upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveExcuseToRealtimeDB(date: String, reason: String, details: String, daysAbsent: String, fileUrl: String?, progress: ProgressDialog) {
        val excuse = mapOf(
            "userId" to userId,
            "email" to userEmail,
            "firstName" to userFirstName,
            "idNumber" to idNumber,
            "department" to department,
            "date" to date,
            "reason" to reason,
            "details" to details,
            "daysAbsent" to daysAbsent,
            "attachmentUrl" to (fileUrl ?: ""),
            "status" to "Pending",
            "submittedAt" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("excuseLetters")
            .child(userId!!)
            .push()
            .setValue(excuse)
            .addOnSuccessListener {
                progress.dismiss()
                showSuccessDialog()
            }
            .addOnFailureListener {
                progress.dismiss()
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