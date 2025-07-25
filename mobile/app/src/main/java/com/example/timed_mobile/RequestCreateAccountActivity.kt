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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

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

    companion object {
        private const val PREFS_NAME = "TimedAppPrefs"
        private const val KEY_SEEN_REG_GUIDE = "hasSeenRegistrationGuide"
        private const val API_BASE_URL = "https://timed-utd9.onrender.com/api"
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
        val inputPassword = findViewById<EditText>(R.id.input_password)

        submitButton.setOnClickListener {
            val name = inputName.text.toString().trim()
            val idNumber = inputIdNumber.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val department = selectedDepartment?.name ?: ""
            val password = inputPassword.text.toString().trim()

            if (listOf(name, idNumber, email, password).any { it.isEmpty() } || selectedDepartment == null) {
                Toast.makeText(this, "Please fill in all fields and select a department.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Submit request to backend
            submitAccountRequest(name, idNumber, email, department, password)
        }
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
                    Toast.makeText(this@RequestCreateAccountActivity, "Network error. Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(this@RequestCreateAccountActivity, "Account request submitted successfully! Please wait for admin approval.", Toast.LENGTH_LONG).show()
                                    finish()
                                } else {
                                    Toast.makeText(this@RequestCreateAccountActivity, message, Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@RequestCreateAccountActivity, "Account request submitted successfully! Please wait for admin approval.", Toast.LENGTH_LONG).show()
                                finish()
                            }
                        } else {
                            val errorMessage = when (it.code) {
                                400 -> "Invalid request data. Please check all fields."
                                409 -> "An account with this information already exists or is pending."
                                500 -> "Server error. Please try again later."
                                else -> "Failed to submit request. Please try again."
                            }
                            Toast.makeText(this@RequestCreateAccountActivity, errorMessage, Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@RequestCreateAccountActivity, "Failed to load departments. Please check your internet connection.", Toast.LENGTH_LONG).show()
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
                                    Toast.makeText(this@RequestCreateAccountActivity, "Error parsing departments data.", Toast.LENGTH_SHORT).show()
                                    setupFallbackDepartments()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@RequestCreateAccountActivity, "Failed to load departments from server.", Toast.LENGTH_SHORT).show()
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