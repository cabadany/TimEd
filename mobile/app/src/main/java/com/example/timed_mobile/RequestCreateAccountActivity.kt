package com.example.timed_mobile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class RequestCreateAccountActivity : WifiSecurityActivity() {

    // --- Guidance System Members ---
    private lateinit var guidanceOverlay: FrameLayout
    private lateinit var helpButton: ImageButton
    private var guidancePopupWindow: PopupWindow? = null
    private var isGuidanceActive: Boolean = false
    private var previousTargetLocationForAnimation: IntArray? = null

    private data class GuidanceStep(val targetViewId: Int, val message: String)
    private lateinit var guidanceSteps: List<GuidanceStep>
    private var currentGuidanceStepIndex = 0

    companion object {
        private const val PREFS_NAME = "TimedAppPrefs"
        private const val KEY_SEEN_REG_GUIDE = "hasSeenRegistrationGuide"
    }
    // --- End Guidance System Members ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.request_create_account_page)

        initializeGuidanceSteps()
        setupAnimations()

        val backButton = findViewById<ImageView>(R.id.icon_back_button)
        backButton.setOnClickListener { finish() }

        helpButton = findViewById(R.id.btn_help_guidance)
        guidanceOverlay = findViewById(R.id.guidance_overlay)
        helpButton.setOnClickListener { startRegistrationGuidance(it) }

        setupFormSubmission()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SEEN_REG_GUIDE, false)) {
            window.decorView.post { startRegistrationGuidance(helpButton) }
        }
    }

    private fun initializeGuidanceSteps() {
        guidanceSteps = listOf(
            GuidanceStep(R.id.outline_name, "First, please enter your full name as it appears on your official documents."),
            GuidanceStep(R.id.outline_idnumber, "Next, enter your unique ID number provided by the institution."),
            GuidanceStep(R.id.outline_email, "Provide your official institutional email address for verification."),
            GuidanceStep(R.id.outline_department, "Enter the name of your department (e.g., CITE, CBA, CEA)."),
            GuidanceStep(R.id.outline_password, "Choose a strong, secure password of at least 6 characters."),
            GuidanceStep(R.id.btnSubmitAccount, "Once all fields are filled, click here to submit your request.")
        )
    }

    private fun setupAnimations() {
        val title = findViewById<TextView>(R.id.titleCreateAccount)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)
        val formElements = listOf<View>(
            findViewById(R.id.outline_name),
            findViewById(R.id.outline_idnumber),
            findViewById(R.id.outline_email),
            findViewById(R.id.outline_department),
            findViewById(R.id.outline_password),
            findViewById(R.id.btnSubmitAccount)
        )

        val animSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        backButton.startAnimation(animSlideDown)
        title.startAnimation(animSlideDown)

        formElements.forEachIndexed { index, view ->
            val animSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_form_element)
            animSlideUp.startOffset = (index * 100).toLong()
            view.startAnimation(animSlideUp)
        }

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()
    }

    private fun setupFormSubmission() {
        val submitButton = findViewById<Button>(R.id.btnSubmitAccount)
        val inputName = findViewById<EditText>(R.id.input_name)
        val inputIdNumber = findViewById<EditText>(R.id.input_idnumber)
        val inputEmail = findViewById<EditText>(R.id.input_email)
        val inputDepartment = findViewById<EditText>(R.id.input_department)
        val inputPassword = findViewById<EditText>(R.id.input_password)

        submitButton.setOnClickListener {
            val name = inputName.text.toString().trim()
            val idNumber = inputIdNumber.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val department = inputDepartment.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (listOf(name, idNumber, email, department, password).any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subject = "TimEd Account Registration Request"
            val message = "New account registration request:\n\nName: $name\nID Number: $idNumber\nEmail: $email\nDepartment: $department\nPassword: $password\n\nPlease review and approve this request."
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("timedsystems@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                startActivity(Intent.createChooser(intent, "Send email..."))
                Toast.makeText(this, "Request submitted. Please wait for approval.", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- REGISTRATION GUIDANCE SYSTEM ---

    private fun startRegistrationGuidance(anchorView: View) {
        if (isGuidanceActive) return
        isGuidanceActive = true
        currentGuidanceStepIndex = 0
        previousTargetLocationForAnimation = null
        anchorView.getLocationOnScreen(IntArray(2).also { previousTargetLocationForAnimation = it })
        showCurrentGuidanceStep()
    }

    private fun showCurrentGuidanceStep() {
        if (!isGuidanceActive || currentGuidanceStepIndex < 0 || currentGuidanceStepIndex >= guidanceSteps.size) {
            handleGuidanceCancellation()
            return
        }
        val step = guidanceSteps[currentGuidanceStepIndex]
        val targetView = findViewById<View>(step.targetViewId)
        showAnimatedGuidancePopup(targetView, step.message, currentGuidanceStepIndex)
    }

    private fun handleGuidanceCancellation() {
        isGuidanceActive = false
        guidancePopupWindow?.dismiss()
        guidancePopupWindow = null
        guidanceOverlay.visibility = View.GONE
        previousTargetLocationForAnimation = null
        markGuideAsSeen()
    }

    private fun markGuideAsSeen() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEEN_REG_GUIDE, true).apply()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showAnimatedGuidancePopup(targetView: View, message: String, stepIndex: Int) {
        guidancePopupWindow?.dismiss()
        guidanceOverlay.visibility = View.VISIBLE

        val dialogView = LayoutInflater.from(this).inflate(R.layout.guidance_popup, null)
        val titleText = dialogView.findViewById<TextView>(R.id.guidance_title)
        val progressText = dialogView.findViewById<TextView>(R.id.guidance_progress_text)
        val messageText = dialogView.findViewById<TextView>(R.id.guidance_message)
        val nextButton = dialogView.findViewById<Button>(R.id.guidance_next_button)
        val previousButton = dialogView.findViewById<Button>(R.id.guidance_previous_button)
        val closeButton = dialogView.findViewById<Button>(R.id.guidance_close_button)

        titleText.text = "Account Setup Guide"
        progressText.text = "Step ${stepIndex + 1} of ${guidanceSteps.size}"
        messageText.text = message
        previousButton.visibility = if (stepIndex > 0) View.VISIBLE else View.INVISIBLE
        nextButton.text = if (stepIndex == guidanceSteps.size - 1) "Finish" else "Next"

        // --- FIXED: Dynamic Sizing and Positioning Logic ---
        val popupWidth = (resources.displayMetrics.widthPixels * 0.9).toInt()
        val screenHeight = resources.displayMetrics.heightPixels
        val maxHeight = (screenHeight * 0.6).toInt()

        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val desiredHeight = dialogView.measuredHeight
        val finalPopupHeight = if (desiredHeight > maxHeight) maxHeight else ViewGroup.LayoutParams.WRAP_CONTENT

        val popupWindow = PopupWindow(dialogView, popupWidth, finalPopupHeight, true)
        guidancePopupWindow = popupWindow
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var isProceeding = false
        popupWindow.setOnDismissListener { if (!isProceeding) handleGuidanceCancellation() }

        val onNavigate = {
            isProceeding = true
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {}
                override fun onAnimationEnd(p0: Animation?) { popupWindow.dismiss(); showCurrentGuidanceStep() }
                override fun onAnimationRepeat(p0: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }

        nextButton.setOnClickListener { currentGuidanceStepIndex++; onNavigate() }
        previousButton.setOnClickListener { currentGuidanceStepIndex--; onNavigate() }
        closeButton.setOnClickListener { handleGuidanceCancellation() }

        val popupHeightForPositioning = if (finalPopupHeight != ViewGroup.LayoutParams.WRAP_CONTENT) finalPopupHeight else desiredHeight
        val currentTargetScreenPos = IntArray(2)
        targetView.getLocationOnScreen(currentTargetScreenPos)
        val targetY = currentTargetScreenPos[1]
        val targetHeight = targetView.height

        var popupY = targetY + targetHeight + 16
        if (popupY + popupHeightForPositioning > screenHeight) {
            popupY = targetY - popupHeightForPositioning - 16
        }
        val popupX = (resources.displayMetrics.widthPixels - popupWidth) / 2
        // --- End of Sizing and Positioning Logic ---

        val animationSet = AnimationSet(true).apply {
            addAnimation(AlphaAnimation(0.0f, 1.0f).apply { duration = 300 })
            val startY = previousTargetLocationForAnimation?.get(1)?.toFloat() ?: (popupY - 100f)
            val fromYDelta = startY - popupY
            addAnimation(TranslateAnimation(0f, 0f, fromYDelta, 0f).apply {
                duration = 400
                interpolator = AnimationUtils.loadInterpolator(this@RequestCreateAccountActivity, android.R.anim.decelerate_interpolator)
            })
        }

        dialogView.startAnimation(animationSet)
        popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, popupX, popupY)
        previousTargetLocationForAnimation = intArrayOf(popupX, popupY)
    }
}