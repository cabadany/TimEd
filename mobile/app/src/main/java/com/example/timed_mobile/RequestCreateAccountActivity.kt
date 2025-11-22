package com.example.timed_mobile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.util.Locale

class RequestCreateAccountActivity : WifiSecurityActivity() {

    // --- Data Models ---
    data class Department(
        val departmentId: String,
        val name: String,
        val abbreviation: String,
        val numberOfFaculty: Int,
        val offeredPrograms: List<String>
    )

    // --- Guidance System Members ---
    private lateinit var guidanceOverlay: FrameLayout
    private lateinit var helpButton: ImageButton
    private var guidancePopupWindow: PopupWindow? = null
    private var isGuidanceActive: Boolean = false
    private var previousTargetLocationForAnimation: IntArray? = null

    private data class GuidanceStep(val targetViewId: Int, val message: String)
    private lateinit var guidanceSteps: List<GuidanceStep>
    private var currentGuidanceStepIndex = 0

    // --- Department Dropdown ---
    private lateinit var departments: List<Department>
    private lateinit var departmentDropdown: AutoCompleteTextView
    private var selectedDepartment: Department? = null

    // --- Password Strength UI ---
    private lateinit var passwordInput: EditText
    private lateinit var passwordStrengthLabel: TextView
    private lateinit var passwordStrengthBar: ProgressBar
    private lateinit var passwordRequirementIcon: ImageView
    private lateinit var passwordRequirementText: TextView
    private lateinit var passwordRequirementRow: View
    private var passwordMeetsRequirements: Boolean = false
    private var lastRequirementIndex: Int = Int.MIN_VALUE

    companion object {
        private const val PREFS_NAME = "TimedAppPrefs"
        private const val KEY_SEEN_REG_GUIDE = "hasSeenRegistrationGuide"
        private const val API_BASE_URL = "https://timed-utd9.onrender.com/api"
        private val COMMON_PASSWORDS = setOf(
            "password", "123456", "12345678", "123456789", "12345",
            "qwerty", "abc123", "test", "testing", "timed", "admin",
            "letmein", "welcome", "iloveyou", "monkey", "dragon"
        )
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

    setupDepartmentDropdown()
    setupPasswordStrengthChecker()
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
            findViewById(R.id.password_requirements_container),
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

    private fun setupDepartmentDropdown() {
        departmentDropdown = findViewById(R.id.input_department)
        departments = emptyList()
        
        // Load departments from API
        fetchDepartmentsFromAPI()
    }

    private fun setupFormSubmission() {
        val submitButton = findViewById<Button>(R.id.btnSubmitAccount)
        val inputName = findViewById<EditText>(R.id.input_name)
        val inputIdNumber = findViewById<EditText>(R.id.input_idnumber)
        val inputEmail = findViewById<EditText>(R.id.input_email)

        submitButton.setOnClickListener {
            val name = inputName.text.toString().trim()
            val idNumber = inputIdNumber.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val department = selectedDepartment?.name ?: ""
            val password = passwordInput.text.toString()

            if (listOf(name, idNumber, email, password).any { it.isEmpty() } || selectedDepartment == null) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Incomplete Form",
                    message = "Please fill in all fields and select a department."
                )
                return@setOnClickListener
            }

            if (!passwordMeetsRequirements) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Weak Password",
                    message = "Please meet all password requirements before submitting."
                )
                return@setOnClickListener
            }

            // Validate email format
            if (!isValidEmail(email)) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Invalid Email",
                    message = "Please enter a valid email address."
                )
                return@setOnClickListener
            }

            // Submit request to backend
            submitAccountRequest(name, idNumber, email, department, password)
        }
    }

    private fun setupPasswordStrengthChecker() {
        passwordInput = findViewById(R.id.input_password)
        passwordStrengthLabel = findViewById(R.id.password_strength_label)
        passwordStrengthBar = findViewById(R.id.password_strength_bar)
        passwordRequirementIcon = findViewById(R.id.password_requirement_icon)
        passwordRequirementText = findViewById(R.id.password_requirement_text)
        passwordRequirementRow = findViewById(R.id.password_requirement_row)

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrengthUI(s?.toString().orEmpty())
            }
        })

        updatePasswordStrengthUI(passwordInput.text?.toString().orEmpty())
    }

    private fun updatePasswordStrengthUI(password: String) {
        val lengthMet = password.length >= 6
        val upperMet = password.any { it.isUpperCase() }
        val lowerMet = password.any { it.isLowerCase() }
        val numberMet = password.any { it.isDigit() }

        val requirements = listOf(
            lengthMet to "At least 6 characters",
            upperMet to "At least 1 uppercase letter (A-Z)",
            lowerMet to "At least 1 lowercase letter (a-z)",
            numberMet to "At least 1 number (0-9)"
        )

        val focusIndex = requirements.indexOfFirst { !it.first }

        val commonPassword = password.lowercase(Locale.ROOT) in COMMON_PASSWORDS && password.isNotEmpty()

        val displayState = when {
            focusIndex >= 0 -> {
                val error = ContextCompat.getColor(this, R.color.error_red)
                DisplayState(focusIndex, R.drawable.ic_warning, error, requirements[focusIndex].second)
            }
            commonPassword -> {
                val error = ContextCompat.getColor(this, R.color.error_red)
                DisplayState(requirements.size, R.drawable.ic_warning, error, "Avoid common passwords like \"password\" or \"test\"")
            }
            else -> {
                val success = ContextCompat.getColor(this, R.color.success_green)
                DisplayState(requirements.size + 1, R.drawable.ic_check_circle, success, "Password meets all requirements")
            }
        }

        if (displayState.index != lastRequirementIndex) {
            animateRequirementChange(displayState.iconRes, displayState.tintColor, displayState.message)
            lastRequirementIndex = displayState.index
        } else {
            applyRequirementDisplay(displayState.iconRes, displayState.tintColor, displayState.message)
        }

        val metCount = requirements.count { it.first }
        val (labelText, colorRes, progress) = when {
            metCount <= 1 || commonPassword -> Triple("Strength: Weak", R.color.error_red, 25)
            metCount == 2 -> Triple("Strength: Fair", R.color.status_yellow, 50)
            metCount == 3 -> Triple("Strength: Good", R.color.primary_medium_blue, 75)
            else -> Triple("Strength: Strong", R.color.success_green, 100)
        }

        val color = ContextCompat.getColor(this, colorRes)
        passwordStrengthLabel.text = labelText
        passwordStrengthLabel.setTextColor(color)
        passwordStrengthBar.progress = progress
        passwordStrengthBar.progressTintList = ColorStateList.valueOf(color)
        passwordStrengthBar.progressBackgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.primary_light_sky)
        )

        passwordMeetsRequirements = metCount == 4 && !commonPassword
    }

    private data class DisplayState(
        val index: Int,
        val iconRes: Int,
        val tintColor: Int,
        val message: String
    )

    private fun animateRequirementChange(iconRes: Int, tintColor: Int, message: String) {
        passwordRequirementRow.animate().cancel()
        passwordRequirementRow.animate()
            .alpha(0f)
            .translationY(-12f)
            .setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                applyRequirementDisplay(iconRes, tintColor, message)
                passwordRequirementRow.alpha = 0f
                passwordRequirementRow.translationY = 12f
                passwordRequirementRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun applyRequirementDisplay(iconRes: Int, tintColor: Int, message: String) {
        passwordRequirementIcon.setImageResource(iconRes)
        passwordRequirementIcon.imageTintList = ColorStateList.valueOf(tintColor)
        passwordRequirementText.text = message
        passwordRequirementText.setTextColor(tintColor)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun submitAccountRequest(name: String, idNumber: String, email: String, department: String, password: String) {
        // Show loading state
        val submitButton = findViewById<Button>(R.id.btnSubmitAccount)
        submitButton.isEnabled = false
        submitButton.text = "Submitting..."

        // Parse name into first and last name
        val nameParts = name.split(" ", limit = 2)
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""

        // Create request data
        val requestData = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "schoolId" to idNumber,
            "department" to department,
            "password" to password
        )

        // Convert to JSON
        val gson = Gson()
        val jsonBody = gson.toJson(requestData)

        // Create HTTP request to backend API
        val client = OkHttpClient()
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            jsonBody
        )
        
        val request = Request.Builder()
            .url("$API_BASE_URL/account-requests/create")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Request"
                    UiDialogs.showErrorPopup(
                        this@RequestCreateAccountActivity,
                        title = "Network Error",
                        message = "Please check your internet connection and try again."
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    runOnUiThread {
                        submitButton.isEnabled = true
                        submitButton.text = "Submit Request"
                        
                        if (it.isSuccessful) {
                            val responseBody = it.body?.string()
                            try {
                                val responseJson = gson.fromJson(responseBody, Map::class.java)
                                val success = responseJson["success"] as? Boolean ?: false
                                val message = responseJson["message"] as? String ?: "Request submitted successfully"
                                
                                if (success) {
                                    val successMessage = message.ifBlank {
                                        "Your account request has been submitted successfully. Please wait for an administrator to approve it."
                                    }
                                    UiDialogs.showSuccessPopup(
                                        this@RequestCreateAccountActivity,
                                        title = "Request Submitted",
                                        message = successMessage
                                    ) {
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                } else {
                                    UiDialogs.showErrorPopup(
                                        this@RequestCreateAccountActivity,
                                        title = "Submission Failed",
                                        message = message
                                    )
                                }
                            } catch (e: Exception) {
                                UiDialogs.showSuccessPopup(
                                    this@RequestCreateAccountActivity,
                                    title = "Request Submitted",
                                    message = "Your account request has been submitted successfully. Please wait for an administrator to approve it."
                                ) {
                                    setResult(RESULT_OK)
                                    finish()
                                }
                            }
                        } else {
                            val errorMessage = when (it.code) {
                                400 -> "Invalid request data. Please check all fields."
                                409 -> "An account with this information already exists or is pending."
                                500 -> "Server error. Please try again later."
                                else -> "Failed to submit request. Please try again."
                            }
                            UiDialogs.showErrorPopup(
                                this@RequestCreateAccountActivity,
                                title = "Submission Error",
                                message = errorMessage
                            )
                        }
                    }
                }
            }
        })
    }

    private fun fetchDepartmentsFromAPI() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$API_BASE_URL/departments")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    UiDialogs.showErrorPopup(
                        this@RequestCreateAccountActivity,
                        title = "Load Failed",
                        message = "Failed to load departments. Please check your internet connection."
                    )
                    // Set a fallback list of departments
                    setupFallbackDepartments()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            try {
                                val gson = Gson()
                                val departmentListType = object : TypeToken<List<Department>>() {}.type
                                departments = gson.fromJson(responseBody, departmentListType)
                                
                                runOnUiThread {
                                    setupDepartmentAdapter()
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    UiDialogs.showErrorPopup(
                                        this@RequestCreateAccountActivity,
                                        title = "Data Error",
                                        message = "Error parsing departments data."
                                    )
                                    setupFallbackDepartments()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            UiDialogs.showErrorPopup(
                                this@RequestCreateAccountActivity,
                                title = "Server Error",
                                message = "Failed to load departments from server."
                            )
                            setupFallbackDepartments()
                        }
                    }
                }
            }
        })
    }

    private fun setupFallbackDepartments() {
        // Fallback departments in case API fails
        departments = listOf(
            Department("1", "College of Computer Studies", "CCS", 0, emptyList()),
            Department("2", "College of Arts, Sciences, and Education", "CASE", 0, emptyList()),
            Department("3", "College of Engineering and Architecture", "CEA", 0, emptyList()),
            Department("4", "College of Nursing and Allied Health Sciences", "CNAHS", 0, emptyList()),
            Department("5", "Grade 7", "G7", 0, emptyList())
        )
        setupDepartmentAdapter()
    }

    private fun setupDepartmentAdapter() {
        val departmentNames = departments.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, departmentNames)
        departmentDropdown.setAdapter(adapter)
        
        departmentDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedDepartment = departments[position]
            departmentDropdown.setText(departments[position].name, false)
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