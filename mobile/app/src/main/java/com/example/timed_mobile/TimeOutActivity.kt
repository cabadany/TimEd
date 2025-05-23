package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
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

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_out_page)

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        findViewById<Button>(R.id.btntime_out).setOnClickListener {
            logTimeOutToFirebase()
            showTimeOutSuccessDialog()
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
        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)

        val log = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "TimeOut",
            "email" to userEmail,
            "firstName" to userFirstName,
            "userId" to userId
        )

        dbRef.push().setValue(log).addOnSuccessListener {
            Toast.makeText(this, "Time-Out logged", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to log Time-Out", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val uid = userId ?: return
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("uploads/$uid/$filename.jpg")

        storageRef.putFile(uri).addOnSuccessListener {
            Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}