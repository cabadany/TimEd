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
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeOutActivity : WifiSecurityActivity() {

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null

    private lateinit var iconBackButton: ImageView
    private lateinit var titleName: TextView
    private lateinit var timeoutIllustration: ImageView
    private lateinit var timeoutInstruction: TextView
    private lateinit var btnTimeOut: Button

    // --- MODIFIABLE TIME SETTING ---
    // Set the specific time of day when users are allowed to time-out.
    // Use 12-hour format (1-12 for hour).
    companion object {
        private const val TIMEOUT_HOUR = 2 // e.g., 5 for 5 o'clock
        private const val TIMEOUT_MINUTE = 0 // e.g., 0 for on the hour
        private const val TIMEOUT_AM_PM = Calendar.PM // Use Calendar.AM or Calendar.PM
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

    private fun logTimeOutToFirebase() {
        if (userId == null) {
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "User ID missing. Cannot log time-out.")
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
                    val hasTimedInToday = snapshot.children.any {
                        it.child("type").getValue(String::class.java) == "TimeIn"
                    }

                    if (!hasTimedInToday) {
                        Toast.makeText(this@TimeOutActivity, "You haven't timed in today.", Toast.LENGTH_LONG).show()
                        return
                    }

                    // --- MODIFIED TIME CHECK WITH SPECIFIC TIME OF DAY (AM/PM) ---
                    val targetTimeOut = Calendar.getInstance().apply {
                        // Use HOUR for 12-hour format. Calendar treats 12 AM/PM as 0 in this context.
                        set(Calendar.HOUR, if (TIMEOUT_HOUR == 12) 0 else TIMEOUT_HOUR)
                        set(Calendar.MINUTE, TIMEOUT_MINUTE)
                        set(Calendar.AM_PM, TIMEOUT_AM_PM)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val now = Calendar.getInstance()

                    if (now.before(targetTimeOut)) {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val formattedTimeOut = timeFormat.format(targetTimeOut.time)
                        UiDialogs.showErrorPopup(this@TimeOutActivity, "Too Early to Time-Out", "You can only time-out after $formattedTimeOut.")
                        return
                    }
                    // --- END MODIFIED TIME CHECK ---

                    val log = mapOf(
                        "timestamp" to now.timeInMillis,
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
                            UiDialogs.showErrorPopup(this@TimeOutActivity, getString(R.string.popup_title_error), "Failed to log Time-Out: ${e.message}")
                        }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    UiDialogs.showErrorPopup(this@TimeOutActivity, getString(R.string.popup_title_error), "Database error: ${error.message}")
                }
            })
    }
}