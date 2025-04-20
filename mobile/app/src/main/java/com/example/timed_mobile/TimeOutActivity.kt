package com.example.timed_mobile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import java.util.UUID

class TimeOutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_out_page)

        findViewById<Button>(R.id.btntime_out).setOnClickListener {
            showTimeOutSuccessDialog()
            logTimeOutToFirebase()
        }
    }

    private fun showTimeOutSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_out)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        val messageText = dialog.findViewById<TextView>(R.id.popup_message)
        titleText.text = "Successfully Timed - Out"
        messageText.text = "Thank you. It has been recorded."

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun logTimeOutToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs")

        val log = mapOf(
            "userId" to userId,
            "timestamp" to System.currentTimeMillis(),
            "type" to "timeout"
        )

        dbRef.push().setValue(log).addOnSuccessListener {
            Toast.makeText(this, "Time-Out logged", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to log Time-Out", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Firebase Storage Upload Example (Photo Upload)
    private fun uploadImageToFirebase(uri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("uploads/$userId/$filename.jpg")

        storageRef.putFile(uri).addOnSuccessListener {
            Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}


// ✅ ExcuseLetterActivity.kt – Full Firebase integration example
package com.example.timed_mobile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ExcuseLetterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_letter_page)

        val reasonInput = findViewById<EditText>(R.id.reason_input)
        val submitButton = findViewById<Button>(R.id.btn_submit_excuse)

        submitButton.setOnClickListener {
            val reason = reasonInput.text.toString().trim()
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val dbRef = FirebaseDatabase.getInstance().getReference("excuseLetters")

            val excuseData = mapOf(
                "userId" to userId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )

            dbRef.push().setValue(excuseData).addOnSuccessListener {
                Toast.makeText(this, "Excuse submitted", Toast.LENGTH_SHORT).show()
                reasonInput.text.clear()
            }.addOnFailureListener {
                Toast.makeText(this, "Submission failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}