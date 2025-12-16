package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.timed_mobile.utils.TimeSettingsManager

class TimeOutActivity : WifiSecurityActivity() {

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var isFlexibleTimeOut: Boolean = false

    private lateinit var iconBackButton: ImageView
    private lateinit var titleName: TextView
    private lateinit var timeoutIllustration: ImageView
    private lateinit var timeoutInstruction: TextView
    private lateinit var btnTimeOut: Button

    // --- MODIFIABLE TIME SETTING ---
    // Set the specific time of day when users are allowed to time-out.
    // Use 12-hour format (1-12 for hour).
    companion object {
        // Toggle to enforce the app-level Time-Out window.
        // Now fetched dynamically from Firebase settings/enforceTimeout
        // private const val ENFORCE_TIMEOUT_WINDOW = true (Deprecated, used dynamic fetch)
    }
    // --- END MODIFIABLE TIME SETTING ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.time_out_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")
        isFlexibleTimeOut = intent.getBooleanExtra("isFlexibleTimeOut", false)

        iconBackButton = findViewById(R.id.icon_back_button)
        titleName = findViewById(R.id.titleName)
        timeoutIllustration = findViewById(R.id.timeout_illustration)
        timeoutInstruction = findViewById(R.id.timeout_instruction)
        btnTimeOut = findViewById(R.id.btntime_out)

        var currentDelay = 100L
        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        animateView(iconBackButton, R.anim.fade_in, currentDelay); currentDelay += 100L
        animateView(titleName, R.anim.slide_down_fade_in, currentDelay); currentDelay += 150L
        animateView(timeoutIllustration, R.anim.fade_in, currentDelay); currentDelay += 150L
        animateView(timeoutInstruction, R.anim.slide_up_fade_in_content, currentDelay); currentDelay += 100L
        animateView(btnTimeOut, R.anim.slide_up_fade_in_content, currentDelay)

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

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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
    
    override fun onResume() {
        super.onResume()
        // SECURITY: Re-sync server time to detect device time manipulation
        TimeSettingsManager.refreshServerTimeSync()
    }

    private fun logTimeOutToFirebase() {
        if (userId == null) {
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "User ID missing. Cannot log time-out.")
            return
        }
        
        // SECURITY: Force sync server time and check for manipulation RIGHT NOW
        TimeSettingsManager.refreshServerTimeSync()
        // Give a small delay to let the sync complete, then check
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // Combined check: AUTO_TIME disabled OR server offset too large
            if (TimeSettingsManager.isTimeTampered(this)) {
                val reason = TimeSettingsManager.getTimeTamperingReason(this)
                UiDialogs.showErrorPopup(
                    this,
                    "⚠️ Time Security Violation",
                    "Time tampering detected!\n\n$reason\n\nPlease enable automatic time in your device settings. App will restart for security verification."
                ) {
                    // SECURITY: Reset state and redirect to Splash Screen
                    TimeSettingsManager.resetSecurityState()
                    val intent = android.content.Intent(this, SplashActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                return@postDelayed
            }
            
            // Proceed with actual time-out logic
            performTimeOutCheck()
        }, 500) // 500ms delay to let Firebase sync complete
    }
    
    private fun performTimeOutCheck() {
        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)

        // SECURITY: Use Philippine timezone to prevent device time manipulation
        val todayStart = TimeSettingsManager.getTodayStartInPhilippineTime()

        dbRef.orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    Log.d("TimeOutActivity", "Checking logs for user $userId. Found ${snapshot.childrenCount} entries since midnight.")
                    
                    var latestTimeIn: Long = 0
                    var latestTimeOut: Long = 0

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        
                        if (type == "TimeIn") {
                            if (timestamp > latestTimeIn) latestTimeIn = timestamp
                        } else if (type == "TimeOut") {
                            if (timestamp > latestTimeOut) latestTimeOut = timestamp
                        }
                    }

                    if (latestTimeIn == 0L) {
                        Log.w("TimeOutActivity", "No TimeIn record found for today.")
                        Toast.makeText(this@TimeOutActivity, "You haven't timed in today.", Toast.LENGTH_LONG).show()
                        return
                    }

                    if (latestTimeOut > latestTimeIn) {
                        Log.w("TimeOutActivity", "User already timed out today (latest TimeOut > latest TimeIn).")
                        UiDialogs.showErrorPopup(this@TimeOutActivity, "Already Timed Out", "You have already timed out today.")
                        return
                    }

                    // --- TIME-OUT WINDOW CHECK ---
                    // Skip time restriction if this is a flexible time-out (from Off Duty status)
                    if (!isFlexibleTimeOut && TimeSettingsManager.isTooEarlyToTimeOut()) {
                         val (start, end) = TimeSettingsManager.getTimeWindowString()
                         UiDialogs.showErrorPopup(this@TimeOutActivity, "Too Early to Time-Out", "You cannot time out before $end.\n\nCurrent Time: ${TimeSettingsManager.getCurrentPhilippineTimeFormatted()}")
                         return
                    }
                    
                    // SECURITY: Use Philippine timezone for logging
                    val now = TimeSettingsManager.getNowInPhilippineTime()
                    val log = mapOf(
                        "timestamp" to now.timeInMillis,
                        "type" to "TimeOut",
                        "email" to userEmail,
                        "firstName" to userFirstName,
                        "userId" to userId,
                        "status" to "Off Duty",
                        "attendanceBadge" to "Timed-Out",
                        "timeOutType" to if (isFlexibleTimeOut) "Flexible" else "Regular"
                    )

                    dbRef.push().setValue(log)
                        .addOnSuccessListener {
                            showTimeOutSuccessDialog()
                        }
                        .addOnFailureListener { e ->
                            UiDialogs.showErrorPopup(this@TimeOutActivity, getString(R.string.popup_title_error), "Failed to log Time-Out: ${e.message}")
                        }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    UiDialogs.showErrorPopup(this@TimeOutActivity, getString(R.string.popup_title_error), "Database error: ${error.message}")
                }
            })
    }
}