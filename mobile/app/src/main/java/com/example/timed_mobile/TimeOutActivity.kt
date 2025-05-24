package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity // <<< ADD THIS IMPORT
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class TimeOutActivity : AppCompatActivity() {

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_out_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        findViewById<Button>(R.id.btntime_out).setOnClickListener {
            logTimeOutToFirebase()
        }

        findViewById<ImageView>(R.id.icon_back_button).setOnClickListener {
            finish()
        }
    }

    private fun showTimeOutSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_time_out)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, // Dialog window takes full width
            ViewGroup.LayoutParams.WRAP_CONTENT  // Height wraps content
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Makes XML background visible
        dialog.window?.setGravity(Gravity.CENTER) // Ensure it's centered
        dialog.window?.setWindowAnimations(R.style.DialogAnimation) // Apply the animation

        // TextViews text is set in XML, no need to set them here unless dynamic
        // val titleText = dialog.findViewById<TextView>(R.id.popup_title)
        // val messageText = dialog.findViewById<TextView>(R.id.popup_message)

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
            }
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun logTimeOutToFirebase() {
        if (userId == null) {
            Toast.makeText(this, "User ID missing. Cannot log time-out.", Toast.LENGTH_LONG).show()
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

        dbRef.push().setValue(log)
            .addOnSuccessListener {
                showTimeOutSuccessDialog()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to log Time-Out: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

}