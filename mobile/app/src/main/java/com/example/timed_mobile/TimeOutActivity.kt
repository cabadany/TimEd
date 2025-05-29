package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View // Added for View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils // Added for animations
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class TimeOutActivity : AppCompatActivity() {

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    // Declare views for animation
    private lateinit var iconBackButton: ImageView
    private lateinit var titleName: TextView
    private lateinit var timeoutIllustration: ImageView
    private lateinit var timeoutInstruction: TextView
    private lateinit var btnTimeOut: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_out_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")

        // Initialize views
        iconBackButton = findViewById(R.id.icon_back_button)
        titleName = findViewById(R.id.titleName)
        timeoutIllustration = findViewById(R.id.timeout_illustration)
        timeoutInstruction = findViewById(R.id.timeout_instruction)
        btnTimeOut = findViewById(R.id.btntime_out)


        // --- START OF ENTRY ANIMATION CODE ---
        // Load animations
        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        val animSlideUpContent = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_content)

        var currentDelay = 100L

        // Helper to apply animation
        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        // 1. Back Button
        animateView(iconBackButton, R.anim.fade_in, currentDelay)
        currentDelay += 100L

        // 2. Title
        animateView(titleName, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        // 3. Timeout Illustration
        animateView(timeoutIllustration, R.anim.fade_in, currentDelay) // Simple fade, or a custom scale/zoom
        currentDelay += 150L

        // 4. Timeout Instruction Text
        animateView(timeoutInstruction, R.anim.slide_up_fade_in_content, currentDelay)
        currentDelay += 100L

        // 5. Time-Out Button
        animateView(btnTimeOut, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---


        btnTimeOut.setOnClickListener {
            logTimeOutToFirebase()
        }

        iconBackButton.setOnClickListener {
            finish()
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
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)

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

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        dbRef.orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    var latestTimeIn: Long? = null

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)

                        if (type == "TimeIn" && timestamp != null) {
                            if (latestTimeIn == null || timestamp > latestTimeIn) {
                                latestTimeIn = timestamp
                            }
                        }
                    }

                    if (latestTimeIn == null) {
                        Toast.makeText(this@TimeOutActivity, "You haven't timed in today.", Toast.LENGTH_LONG).show()
                        return
                    }

                    val fiveHoursMillis = 5 * 60 * 60 * 1000
                    val now = System.currentTimeMillis()

                    if (now - latestTimeIn < fiveHoursMillis) {
                        val hoursLeft = ((fiveHoursMillis - (now - latestTimeIn)) / (60 * 60 * 1000)).toInt()
                        AlertDialog.Builder(this@TimeOutActivity)
                            .setTitle("Too Early to Time-Out")
                            .setMessage("You can only time-out 5 hours after timing in.\n\nPlease wait about $hoursLeft more hour(s).")
                            .setPositiveButton("OK", null)
                            .show()
                        return
                    }

                    val log = mapOf(
                        "timestamp" to now,
                        "type" to "TimeOut",
                        "email" to userEmail,
                        "firstName" to userFirstName,
                        "userId" to userId,
                        "status" to "Off Duty",
                        "attendanceBadge" to "Timed-Out"
                    )

                    dbRef.push().setValue(log)
                        .addOnSuccessListener {
                            showTimeOutSuccessDialog()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@TimeOutActivity, "Failed to log Time-Out: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Toast.makeText(this@TimeOutActivity, "Database error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}