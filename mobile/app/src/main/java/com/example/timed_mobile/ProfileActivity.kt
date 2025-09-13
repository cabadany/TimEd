package com.example.timed_mobile

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils // Added for animations
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : WifiSecurityActivity() {

    private lateinit var changePasswordButton: Button
    private lateinit var attendanceSheetButton: Button
    private lateinit var logoutText: TextView
    private lateinit var teacherName: TextView // This is profile_name in XML
    private lateinit var teacherId: TextView // This is profile_id_number in XML
    private lateinit var backButton: ImageView
    private lateinit var profileEmail: TextView
    private lateinit var profileDepartment: TextView
    private lateinit var profileImage: CircleImageView
    private lateinit var profileTitle: TextView // Added for animation
    private lateinit var profileInfoContainer: LinearLayout // Added for animation
    private lateinit var profileContentContainer: RelativeLayout
    private lateinit var profileSkeletonContainer: LinearLayout
    private val skeletonTimeoutHandler = Handler(Looper.getMainLooper())
    private val skeletonTimeoutRunnable = Runnable {
        if (this::profileSkeletonContainer.isInitialized && profileSkeletonContainer.visibility == View.VISIBLE) {
            profileSkeletonContainer.clearAnimation()
            profileSkeletonContainer.visibility = View.GONE
            if (this::profileContentContainer.isInitialized) profileContentContainer.visibility = View.VISIBLE
            playEntryAnimations()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

    // Initialize all views
        profileTitle = findViewById(R.id.profile_title) // Initialize title
        backButton = findViewById(R.id.icon_back_button)
        profileImage = findViewById(R.id.profile_image)
        teacherName = findViewById(R.id.profile_name)
        teacherId = findViewById(R.id.profile_id_number)
        profileInfoContainer = findViewById(R.id.profile_info_container) // Initialize container
        profileEmail = findViewById(R.id.profile_email) // Already part of profileInfoContainer, but can be animated individually if needed
        profileDepartment = findViewById(R.id.profile_department) // Same as above
        changePasswordButton = findViewById(R.id.btn_change_password)
        attendanceSheetButton = findViewById(R.id.btn_attendance_sheet)
        logoutText = findViewById(R.id.logout_text)
    profileContentContainer = findViewById(R.id.profile_content_container)
    profileSkeletonContainer = findViewById(R.id.profile_skeleton_container)


        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

    // Initially show skeleton, hide content
    profileSkeletonContainer.visibility = View.VISIBLE
    profileSkeletonContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shimmer_pulse))
        profileContentContainer.visibility = View.GONE


        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        changePasswordButton.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        attendanceSheetButton.setOnClickListener {
            // startActivity(Intent(this, AttendanceSheetActivity::class.java)) // Original
            showAttendanceDownloadDialog() // As per your existing code logic
        }

        logoutText.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        loadUserProfile()
        // Safety fallback: reveal content after 5s if still loading
        skeletonTimeoutHandler.postDelayed(skeletonTimeoutRunnable, 5000)
    }

    private fun loadUserProfile() {
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "User not logged in")
            // Consider hiding animated views or showing an error state if profile can't load
            profileSkeletonContainer.visibility = View.GONE
            profileContentContainer.visibility = View.VISIBLE
            playEntryAnimations()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Profile not found.")
                    profileSkeletonContainer.clearAnimation()
                    profileSkeletonContainer.visibility = View.GONE
                    profileContentContainer.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val departmentId = document.getString("departmentId") ?: ""
                val idNumber = document.getString("schoolId") ?: "N/A" // schoolId from XML, idNumber in code
                val email = document.getString("email") ?: "No email"
                val profilePictureUrl = document.getString("profilePictureUrl")

                teacherName.text = "$firstName $lastName"
                teacherId.text = idNumber // Matches profile_id_number
                profileEmail.text = email

                if (departmentId.isNotEmpty()) {
                    firestore.collection("departments")
                        .document(departmentId)
                        .get()
                        .addOnSuccessListener { deptDoc ->
                            if (deptDoc.exists()) {
                                profileDepartment.text = deptDoc.getString("name") ?: "N/A"
                            } else {
                                profileDepartment.text = "N/A"
                            }
                        }
                        .addOnFailureListener {
                            profileDepartment.text = "N/A"
                        }
                } else {
                    profileDepartment.text = "N/A"
                }

                val timeLogsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("timeLogs").child(userId)

                timeLogsRef.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            var timeInImageUrl: String? = null
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                                val todayStart = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    timeInImageUrl = child.child("imageUrl").getValue(String::class.java)
                                    break
                                }
                            }

                            val imageToLoad = when {
                                !timeInImageUrl.isNullOrEmpty() -> timeInImageUrl
                                !profilePictureUrl.isNullOrEmpty() -> profilePictureUrl
                                else -> null
                            }

                            if (!imageToLoad.isNullOrEmpty()) {
                                Glide.with(this@ProfileActivity)
                                    .load(imageToLoad)
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(profileImage)
                            } else {
                                profileImage.setImageResource(R.drawable.profile_placeholder)
                            }

                            // Reveal content and play entry animations after data has been applied
                            profileSkeletonContainer.clearAnimation()
                            profileSkeletonContainer.visibility = View.GONE
                            profileContentContainer.visibility = View.VISIBLE
                            playEntryAnimations()
                        }

                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            Log.e("ProfileActivity", "Firebase DB error: ${error.message}")
                            profileImage.setImageResource(R.drawable.profile_placeholder) // Ensure this drawable exists
                        }
                    })
            }
            .addOnFailureListener {
                UiDialogs.showErrorPopup(this, getString(R.string.popup_title_error), "Failed to load profile")
                Log.e("ProfileActivity", "Firestore error: ", it)
                profileSkeletonContainer.clearAnimation()
                profileSkeletonContainer.visibility = View.GONE
                profileContentContainer.visibility = View.VISIBLE
            }
    }

    private fun playEntryAnimations() {
        val animFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        var currentDelay = 100L

        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
        }

        animateView(backButton, R.anim.fade_in, currentDelay)
        currentDelay += 100L
        animateView(profileTitle, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L
        animateView(profileImage, R.anim.fade_in, currentDelay)
        currentDelay += 150L
        animateView(teacherName, R.anim.slide_up_fade_in, currentDelay)
        currentDelay += 100L
        animateView(teacherId, R.anim.slide_up_fade_in, currentDelay)
        currentDelay += 100L
        animateView(profileInfoContainer, R.anim.slide_up_fade_in, currentDelay)
        currentDelay += 150L
        animateView(changePasswordButton, R.anim.slide_up_fade_in, currentDelay)
        currentDelay += 100L
        animateView(attendanceSheetButton, R.anim.slide_up_fade_in, currentDelay)
        currentDelay += 100L
        animateView(logoutText, R.anim.slide_up_fade_in, currentDelay)
    }

    private fun showAttendanceDownloadDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.attendance_download_dialog) // Ensure this layout exists
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val noButton = dialog.findViewById<Button>(R.id.btn_no)
        val yesButton = dialog.findViewById<Button>(R.id.btn_yes)

        noButton.setOnClickListener { dialog.dismiss() }
        yesButton.setOnClickListener {
            dialog.dismiss()
            // Simulate download
            Handler(Looper.getMainLooper()).postDelayed({
                showDownloadSuccessDialog()
            }, 1500)
        }
        dialog.show()
    }

    private fun showDownloadSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.success_popup_attendance_download) // Ensure this layout exists
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val okButton = dialog.findViewById<Button>(R.id.btn_ok)
        okButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showLogoutConfirmationDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.logout_confirmation_dialog) // Ensure this layout exists
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.5f)

        val cancelButton = dialog.findViewById<Button>(R.id.btn_cancel)
        val logoutButton = dialog.findViewById<Button>(R.id.btn_logout)

        cancelButton.setOnClickListener { dialog.dismiss() }
        logoutButton.setOnClickListener {
            getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finish ProfileActivity after logout
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        skeletonTimeoutHandler.removeCallbacks(skeletonTimeoutRunnable)
    }
}