package com.example.timed_mobile

import android.app.Dialog
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import android.util.Log

class ChangePasswordActivity : WifiSecurityActivity() {
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var reenterPasswordInput: TextInputEditText
    private lateinit var updatePasswordButton: Button
    private lateinit var newPasswordLayout: TextInputLayout
    private lateinit var reenterPasswordLayout: TextInputLayout
    private lateinit var backButton: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var teacherName: TextView
    private lateinit var teacherId: TextView
    private lateinit var currentPasswordInput: TextInputEditText
    private lateinit var changePasswordTitle: TextView
    private lateinit var currentPasswordLayout: TextInputLayout
    private lateinit var changePasswordContentContainer: RelativeLayout
    private lateinit var changePasswordSkeletonContainer: LinearLayout


    private var userId: String? = null
    private val skeletonTimeoutHandler = Handler(Looper.getMainLooper())
    private val skeletonTimeoutRunnable = Runnable {
        if (this::changePasswordSkeletonContainer.isInitialized && changePasswordSkeletonContainer.visibility == View.VISIBLE) {
            changePasswordSkeletonContainer.clearAnimation()
            changePasswordSkeletonContainer.visibility = View.GONE
            if (this::changePasswordContentContainer.isInitialized) changePasswordContentContainer.visibility =
                View.VISIBLE
            setupEntryAnimations()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Initialize Views
        newPasswordInput = findViewById(R.id.new_password_input)
        reenterPasswordInput = findViewById(R.id.reenter_password_input)
        updatePasswordButton = findViewById(R.id.btn_update_password)
        newPasswordLayout = findViewById(R.id.new_password_layout)
        reenterPasswordLayout = findViewById(R.id.reenter_password_layout)
        backButton = findViewById(R.id.icon_back_button)
        profileImage = findViewById(R.id.profile_image)
        teacherName = findViewById(R.id.teacher_name)
        teacherId = findViewById(R.id.teacher_id)
        currentPasswordInput = findViewById(R.id.current_password_input)
        changePasswordTitle = findViewById(R.id.change_password_title)
        currentPasswordLayout = findViewById(R.id.current_password_layout)
        changePasswordContentContainer = findViewById(R.id.change_password_content_container)
        changePasswordSkeletonContainer = findViewById(R.id.change_password_skeleton_container)


        updatePasswordButton.setOnClickListener {
            updatePassword()
        }

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        // Show skeleton until data loads
        changePasswordSkeletonContainer.visibility = View.VISIBLE
        changePasswordSkeletonContainer.startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.shimmer_pulse
            )
        )
        changePasswordContentContainer.visibility = View.GONE

        // Load data
        loadCurrentUserInfo()

        // Safety fallback: reveal content after 5s if still loading
        skeletonTimeoutHandler.postDelayed(skeletonTimeoutRunnable, 5000)
    }

    private fun setupEntryAnimations() {
        // Animation for elements sliding down from the top
        val animSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        backButton.startAnimation(animSlideDown)
        changePasswordTitle.startAnimation(animSlideDown)

        // List of elements to slide up from the bottom with a stagger
        val formElements = listOf<View>(
            profileImage,
            teacherName,
            teacherId,
            currentPasswordLayout,
            newPasswordLayout,
            reenterPasswordLayout,
            updatePasswordButton
        )

        formElements.forEachIndexed { index, view ->
            val animSlideUp =
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_form_element)
            // Stagger the start of each animation
            animSlideUp.startOffset = (index * 100).toLong()
            view.startAnimation(animSlideUp)
        }
    }

    private fun loadCurrentUserInfo() {
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)

        if (userId.isNullOrEmpty()) {
            UiDialogs.showErrorPopup(
                this,
                title = "Not Logged In",
                message = "Please log in to change your password."
            )
            changePasswordSkeletonContainer.visibility = View.GONE
            changePasswordContentContainer.visibility = View.VISIBLE
            setupEntryAnimations()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .document(userId!!)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Profile Not Found",
                        message = "We couldn't find your profile. Please try again or contact support."
                    )
                    changePasswordSkeletonContainer.clearAnimation()
                    changePasswordSkeletonContainer.visibility = View.GONE
                    changePasswordContentContainer.visibility = View.VISIBLE
                    setupEntryAnimations()
                    return@addOnSuccessListener
                }

                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val schoolId = document.getString("schoolId") ?: ""
                val profilePictureUrl = document.getString("profilePictureUrl")

                teacherName.text = "$firstName $lastName"
                teacherId.text = schoolId

                val timeLogsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("timeLogs").child(userId!!)

                timeLogsRef.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object :
                        com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            var timeInImageUrl: String? = null
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp =
                                    child.child("timestamp").getValue(Long::class.java) ?: 0L

                                val todayStart = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    set(java.util.Calendar.MINUTE, 0)
                                    set(java.util.Calendar.SECOND, 0)
                                    set(java.util.Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    timeInImageUrl =
                                        child.child("imageUrl").getValue(String::class.java)
                                    break
                                }
                            }

                            val imageToLoad = when {
                                !timeInImageUrl.isNullOrEmpty() -> timeInImageUrl
                                !profilePictureUrl.isNullOrEmpty() -> profilePictureUrl
                                else -> null
                            }

                            if (!imageToLoad.isNullOrEmpty()) {
                                Glide.with(this@ChangePasswordActivity)
                                    .load(imageToLoad)
                                    .placeholder(R.drawable.profile_placeholder)
                                    .into(profileImage)
                            } else {
                                profileImage.setImageResource(R.drawable.profile_placeholder)
                            }

                            // Reveal content and animate after data populates
                            changePasswordSkeletonContainer.clearAnimation()
                            changePasswordSkeletonContainer.visibility = View.GONE
                            changePasswordContentContainer.visibility = View.VISIBLE
                            setupEntryAnimations()
                        }

                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            profileImage.setImageResource(R.drawable.profile_placeholder)
                            UiDialogs.showErrorPopup(
                                this@ChangePasswordActivity,
                                title = "Load Error",
                                message = "Couldn't load recent time-in image. Please try again later."
                            )
                        }
                    })
            }
            .addOnFailureListener {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Load Error",
                    message = "Failed to load user info. Please check your connection and try again."
                )
                changePasswordSkeletonContainer.clearAnimation()
                changePasswordSkeletonContainer.visibility = View.GONE
                changePasswordContentContainer.visibility = View.VISIBLE
                setupEntryAnimations()
            }
    }

    private fun updatePassword() {
        val currentPassword = currentPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = reenterPasswordInput.text.toString().trim()

        newPasswordLayout.error = null
        reenterPasswordLayout.error = null

        if (currentPassword.isEmpty()) {
            UiDialogs.showErrorPopup(
                this,
                title = "Missing Current Password",
                message = "Please enter your current password to proceed."
            )
            return
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout.error = "Enter a new password"
            return
        }

        if (newPassword.length < 6) {
            newPasswordLayout.error = "Password must be at least 6 characters"
            return
        }

        if (confirmPassword != newPassword) {
            reenterPasswordLayout.error = "Passwords do not match"
            return
        }

        if (userId == null) {
            UiDialogs.showErrorPopup(
                this,
                title = "Missing Session",
                message = "Your session is missing or expired. Please log in again."
            )
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId!!)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val storedHash = document.getString("password")
                if (storedHash.isNullOrEmpty()) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Password Error",
                        message = "No existing password found for this account."
                    )
                    return@addOnSuccessListener
                }

                val passwordMatch = org.mindrot.jbcrypt.BCrypt.checkpw(currentPassword, storedHash)
                if (!passwordMatch) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Incorrect Password",
                        message = "The current password you entered is incorrect."
                    )
                    return@addOnSuccessListener
                }

                val newHashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(
                    newPassword,
                    org.mindrot.jbcrypt.BCrypt.gensalt()
                )
                userDocRef.update("password", newHashedPassword)
                    .addOnSuccessListener {
                        showSuccessDialog()
                    }
                    .addOnFailureListener {
                        UiDialogs.showErrorPopup(
                            this,
                            title = "Update Failed",
                            message = "Couldn't update your password. Please try again."
                        )
                    }
            } else {
                UiDialogs.showErrorPopup(
                    this,
                    title = "User Not Found",
                    message = "We couldn't find your account. Please re-login and try again."
                )
            }
        }.addOnFailureListener {
            UiDialogs.showErrorPopup(
                this,
                title = "Database Error",
                message = "Error accessing your account data. Please try again later."
            )
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.success_popup_changepassword)

        val closeButton = dialog.findViewById<Button>(R.id.popup_close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        skeletonTimeoutHandler.removeCallbacks(skeletonTimeoutRunnable)
    }
}