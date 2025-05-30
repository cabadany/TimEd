package com.example.timed_mobile

import com.example.timed_mobile.adapter.StatusAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.timed_mobile.adapter.EventAdapter
import com.example.timed_mobile.model.EventModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

import android.graphics.drawable.ColorDrawable
import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlin.math.abs
import androidx.core.view.isVisible
import com.google.firebase.firestore.FieldValue

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.material.internal.NavigationMenuView
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TOTAL_QUICK_TOUR_STEPS = 4
        const val PREFS_TUTORIAL = "TutorialPrefs"

        // New keys for enhanced tutorial management
        const val KEY_TUTORIAL_OVERALL_COMPLETED_OR_SKIPPED = "tutorialOverallCompletedOrSkipped"
        const val KEY_TUTORIAL_CURRENT_STEP_IF_PAUSED = "tutorialCurrentStepIfPaused"
        const val KEY_TUTORIAL_IS_PAUSED = "tutorialIsPaused"
        const val KEY_TUTORIAL_STEP_COMPLETED_PREFIX = "tutorial_step_" // e.g., tutorial_step_1_completed
        const val KEY_TUTORIAL_LAST_FIRESTORE_SYNC = "tutorialLastFirestoreSync"
    }

    private var previousTargetLocation: IntArray? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var greetingName: TextView
    private lateinit var greetingDetails: TextView
    private lateinit var recyclerEvents: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var firestore: FirebaseFirestore
    private lateinit var statusSpinner: Spinner

    private lateinit var btnCancelled: Button
    private lateinit var btnUpcoming: Button
    private lateinit var btnOngoing: Button
    private lateinit var btnEnded: Button
    private lateinit var btnTimeIn: Button
    private lateinit var btnTimeOut: Button
    private lateinit var excuseLetterText: TextView

    private lateinit var greetingCardNavIcon: ImageView
    private lateinit var tutorialOverlay: FrameLayout
    private lateinit var noEventsMessage: TextView

    private var currentTutorialPopupWindow: PopupWindow? = null
    private val allEvents = mutableListOf<EventModel>()
    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var idNumber: String? = null
    private var department: String? = null
    private val statusOptions = listOf("On Duty", "On Break", "Off Duty")
    private var isSpinnerInitialized = false
    private var isUserChangingStatus = false

    private lateinit var attendanceStatusBadge: TextView

    private lateinit var btnHelp: ImageView
    private lateinit var profileImagePlaceholder: ImageView

    // Tutorial State Variables
    private var currentTutorialActualStep: Int = 1
    private var tutorialIsPaused: Boolean = false
    private var tutorialOverallCompletedOrSkipped: Boolean = false
    private var tutorialCompletedStepsMap = mutableMapOf<Int, Boolean>()


    private val timeInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isTimedIn = result.data?.getBooleanExtra("TIMED_IN_SUCCESS", false) ?: false
                if (isTimedIn) {
                    Toast.makeText(this, "Time-In recorded successfully!", Toast.LENGTH_LONG).show()
                    updateUserStatus("On Duty")
                    updateTimeLogsStatus("On Duty")
                    loadTodayTimeInPhoto()
                    updateSidebarProfileImage()
                    evaluateAndDisplayAttendanceBadge()
                    val index = statusOptions.indexOf("On Duty")
                    if (index != -1) {
                        isUserChangingStatus = false
                        statusSpinner.setSelection(index)
                    }
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        attendanceStatusBadge = findViewById(R.id.attendance_status_badge)

        tutorialOverlay = findViewById(R.id.tutorial_overlay)
        tutorialOverlay.setOnTouchListener { _, motionEvent -> // view, event
            val isTutorialStepActive = currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing

            if (tutorialOverlay.isVisible && isTutorialStepActive) {
                // User clicked outside the tutorial dialog, on the overlay
                currentTutorialPopupWindow?.dismiss() // Dismiss the popup
                currentTutorialPopupWindow = null     // Nullify the reference
                tutorialOverlay.visibility = View.GONE // Hide the overlay
                previousTargetLocation = null          // Reset the previous target location

                // Mark tutorial as completed
                val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
                tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()

                Toast.makeText(this, "Quick Tour ended.", Toast.LENGTH_SHORT).show()

                true // Event consumed
            } else {
                // If tutorial is not active or overlay is not meant to block, don't consume
                false
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        greetingCardNavIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        greetingName = findViewById(R.id.greeting_name)
        greetingDetails = findViewById(R.id.home_greeting)
        recyclerEvents = findViewById(R.id.recycler_events)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        btnCancelled = findViewById(R.id.btn_filter_cancelled)
        btnUpcoming = findViewById(R.id.btn_filter_upcoming)
        btnOngoing = findViewById(R.id.btn_filter_ongoing)
        btnEnded = findViewById(R.id.btn_filter_ended)
        btnTimeIn = findViewById(R.id.btntime_in)
        btnTimeOut = findViewById(R.id.btntime_out)
        excuseLetterText = findViewById(R.id.excuse_letter_text_button)

        btnHelp = findViewById(R.id.btn_help)
        noEventsMessage = findViewById(R.id.no_events_message)
        profileImagePlaceholder = findViewById(R.id.profile_image_placeholder)

        statusSpinner = findViewById(R.id.status_spinner)
        val statusAdapter = StatusAdapter(this, statusOptions)
        statusSpinner.adapter = statusAdapter

        val greetingCardView = findViewById<androidx.cardview.widget.CardView>(R.id.greeting_card)
        val filterButtonsLayout = findViewById<LinearLayout>(R.id.filter_buttons)
        val attendancePromptView: TextView? = try {
            findViewById(R.id.attendance_prompt)
        } catch (e: Exception) {
            null
        }

        val animSlideDownFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        greetingCardView.startAnimation(animSlideDownFadeIn)

        val animSlideDownFilters = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideDownFilters.startOffset = 200L
        filterButtonsLayout.startAnimation(animSlideDownFilters)

        val animFadeInRecycler = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        animFadeInRecycler.startOffset = 400L
        recyclerEvents.startAnimation(animFadeInRecycler)

        val baseDelayBottom = 600L
        attendancePromptView?.let {
            val animSlideUpPrompt = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
            animSlideUpPrompt.startOffset = baseDelayBottom
            it.startAnimation(animSlideUpPrompt)
        }
        val animSlideUpTimeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpTimeIn.startOffset = baseDelayBottom + (if (attendancePromptView != null) 100L else 0L)
        btnTimeIn.startAnimation(animSlideUpTimeIn)

        val animSlideUpTimeOut = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpTimeOut.startOffset = baseDelayBottom + (if (attendancePromptView != null) 200L else 100L)
        btnTimeOut.startAnimation(animSlideUpTimeOut)

        val animSlideUpExcuseLetter = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpExcuseLetter.startOffset = baseDelayBottom + (if (attendancePromptView != null) 300L else 200L)
        excuseLetterText.startAnimation(animSlideUpExcuseLetter)


        firestore = FirebaseFirestore.getInstance()
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
        userEmail = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
        userFirstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)
        idNumber = sharedPrefs.getString(LoginActivity.KEY_ID_NUMBER, "N/A")
        department = sharedPrefs.getString(LoginActivity.KEY_DEPARTMENT, "N/A")

        loadUserStatus()

        // Initialize and load tutorial state
        loadTutorialStateFromPreferences()
        userId?.let {
            loadTutorialStateFromFirestore() // Sync/load from Firestore after local
        }

        // Decide whether to start or resume tutorial AFTER views are likely ready
        // Post to message queue to ensure views are laid out, preventing BadTokenException
        window.decorView.post {
            if (!isFinishing && !isDestroyed) {
                if (!tutorialOverallCompletedOrSkipped) {
                    if (tutorialIsPaused) {
                        showResumeRestartSkipDialog()
                    } else {
                        val firstUncompletedStep = (1..TOTAL_QUICK_TOUR_STEPS).firstOrNull { !isTutorialStepCompleted(it) } ?: 1
                        startTutorialFromStep(firstUncompletedStep)
                    }
                }
            }
        }

        btnHelp.setOnClickListener {
            showRestartTutorialConfirmationDialog()
        }

        statusSpinner.setOnTouchListener { _, _ ->
            isUserChangingStatus = true
            false
        }
        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                val selectedStatus = statusOptions[position]
                if (isUserChangingStatus) {
                    showStatusConfirmationDialog(selectedStatus)
                } else {
                    // This case should ideally not be hit if isUserChangingStatus is managed correctly
                    // updateUserStatus(selectedStatus)
                    // updateTimeLogsStatus(selectedStatus)
                }
                isUserChangingStatus = false // Reset flag after handling
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        recyclerEvents.layoutManager = LinearLayoutManager(this)

        userId?.let { uid ->
            val usersRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            usersRef.get().addOnSuccessListener { doc ->
                val fullName = userFirstName ?: "User"
                greetingName.text = "Hi, $fullName"
                val departmentId = doc.getString("departmentId")
                if (!departmentId.isNullOrEmpty()) {
                    FirebaseFirestore.getInstance().collection("departments")
                        .document(departmentId)
                        .get()
                        .addOnSuccessListener { deptDoc ->
                            val abbreviation = deptDoc.getString("abbreviation") ?: "N/A"
                            greetingDetails.text = "$idNumber • $abbreviation"
                        }
                        .addOnFailureListener {
                            greetingDetails.text = "$idNumber • N/A"
                        }
                } else {
                    greetingDetails.text = "$idNumber • N/A"
                }
            }
        } ?: run {
            greetingName.text = "Hi, User"
            greetingDetails.text = "N/A • N/A"
        }


        loadTodayTimeInPhoto()
        updateSidebarProfileImage()
        setupNavigationDrawer()
        setupFilterButtons()
        setupActionButtons()
        setupExcuseLetterRedirect()

        val profileClickListener = View.OnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
                putExtra("idNumber", idNumber)
                putExtra("department", department)
            }
            startActivity(intent)
        }
        profileImagePlaceholder.setOnClickListener(profileClickListener)
        greetingName.setOnClickListener(profileClickListener)

        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeActivity", "Pull-to-refresh triggered")
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
            loadAndStoreEvents() // Reload events
            evaluateAndDisplayAttendanceBadge() // Re-evaluate badge
            loadUserStatus() // Reload user status for spinner
            swipeRefreshLayout.isRefreshing = false
        }
        loadAndStoreEvents()
    }

    // --- Enhanced Tutorial Management Functions ---

    private fun loadTutorialStateFromPreferences() {
        val prefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        tutorialOverallCompletedOrSkipped = prefs.getBoolean(KEY_TUTORIAL_OVERALL_COMPLETED_OR_SKIPPED, false)
        tutorialIsPaused = prefs.getBoolean(KEY_TUTORIAL_IS_PAUSED, false)
        currentTutorialActualStep = prefs.getInt(KEY_TUTORIAL_CURRENT_STEP_IF_PAUSED, 1)
        tutorialCompletedStepsMap.clear()
        for (i in 1..TOTAL_QUICK_TOUR_STEPS) {
            tutorialCompletedStepsMap[i] = prefs.getBoolean("$KEY_TUTORIAL_STEP_COMPLETED_PREFIX$i", false)
        }
        Log.d("Tutorial", "Loaded from Prefs: overallSkipped=$tutorialOverallCompletedOrSkipped, paused=$tutorialIsPaused, currentStep=$currentTutorialActualStep, completedSteps=$tutorialCompletedStepsMap")
    }

    private fun saveTutorialStateToPreferences() {
        val prefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_TUTORIAL_OVERALL_COMPLETED_OR_SKIPPED, tutorialOverallCompletedOrSkipped)
            putBoolean(KEY_TUTORIAL_IS_PAUSED, tutorialIsPaused)
            putInt(KEY_TUTORIAL_CURRENT_STEP_IF_PAUSED, currentTutorialActualStep)
            tutorialCompletedStepsMap.forEach { (step, completed) ->
                putBoolean("$KEY_TUTORIAL_STEP_COMPLETED_PREFIX$step", completed)
            }
            putLong(KEY_TUTORIAL_LAST_FIRESTORE_SYNC, System.currentTimeMillis())
            apply()
        }
        Log.d("Tutorial", "Saved to Prefs: overallSkipped=$tutorialOverallCompletedOrSkipped, paused=$tutorialIsPaused, currentStep=$currentTutorialActualStep")
    }

    private fun loadTutorialStateFromFirestore() {
        userId ?: return
        val docRef = firestore.collection("users").document(userId!!)
            .collection("tutorialState").document("progress")

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val remoteOverallCompleted = document.getBoolean("overallCompletedOrSkipped") ?: false
                val remoteIsPaused = document.getBoolean("isPaused") ?: false
                val remoteCurrentStep = (document.getLong("currentStepIfPaused") ?: 1L).toInt()
                @Suppress("UNCHECKED_CAST")
                val remoteCompletedStepsFirestore = document.get("completedSteps") as? Map<String, Boolean> ?: emptyMap()
                val remoteCompletedSteps = remoteCompletedStepsFirestore.mapKeys { it.key.toIntOrNull() ?: -1 }.filterKeys { it != -1 }


                // Basic sync: Firestore data is considered more authoritative if present.
                tutorialOverallCompletedOrSkipped = remoteOverallCompleted
                tutorialIsPaused = remoteIsPaused
                currentTutorialActualStep = remoteCurrentStep
                tutorialCompletedStepsMap.clear()
                for (i in 1..TOTAL_QUICK_TOUR_STEPS) {
                    tutorialCompletedStepsMap[i] = remoteCompletedSteps[i] ?: false
                }
                saveTutorialStateToPreferences() // Update local prefs with Firestore data
                Log.d("TutorialFirestore", "Tutorial state loaded from Firestore. overallSkipped=$tutorialOverallCompletedOrSkipped, paused=$tutorialIsPaused, currentStep=$currentTutorialActualStep, completedSteps=$tutorialCompletedStepsMap")

                // Re-evaluate if tutorial should run after loading from Firestore,
                // This might be redundant if already handled by the post in onCreate, but good for direct calls.
                // Ensure not to create dialog loops.
                if (!isFinishing && !isDestroyed) {
                    if (!tutorialOverallCompletedOrSkipped && (currentTutorialPopupWindow == null || !currentTutorialPopupWindow!!.isShowing)) {
                        if (tutorialIsPaused) {
                            showResumeRestartSkipDialog()
                        } else {
                            val firstUncompletedStep = (1..TOTAL_QUICK_TOUR_STEPS).firstOrNull { !isTutorialStepCompleted(it) } ?: 1
                            startTutorialFromStep(firstUncompletedStep)
                        }
                    }
                }
            } else {
                Log.d("TutorialFirestore", "No tutorial state found in Firestore. Using local or default. Saving current local state to Firestore.")
                saveTutorialStateToFirestore() // Save local (possibly default) state to Firestore if not present
            }
        }.addOnFailureListener { e ->
            Log.e("TutorialFirestore", "Error loading tutorial state from Firestore", e)
        }
    }

    private fun saveTutorialStateToFirestore() {
        userId ?: return
        val completedStepsForFirestore = tutorialCompletedStepsMap.mapKeys { it.key.toString() }

        val tutorialState = hashMapOf(
            "overallCompletedOrSkipped" to tutorialOverallCompletedOrSkipped,
            "isPaused" to tutorialIsPaused,
            "currentStepIfPaused" to currentTutorialActualStep,
            "completedSteps" to completedStepsForFirestore,
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId!!)
            .collection("tutorialState").document("progress")
            .set(tutorialState)
            .addOnSuccessListener { Log.d("TutorialFirestore", "Tutorial state saved to Firestore.") }
            .addOnFailureListener { e -> Log.e("TutorialFirestore", "Error saving tutorial state to Firestore", e) }
    }


    private fun isTutorialStepCompleted(step: Int): Boolean {
        return tutorialCompletedStepsMap[step] ?: false
    }

    private fun markTutorialStepAsCompleted(step: Int) {
        tutorialCompletedStepsMap[step] = true
        // currentTutorialActualStep will be incremented by the calling logic or set if tour ends
        saveTutorialStateToPreferences()
        saveTutorialStateToFirestore()
    }

    private fun showResumeRestartSkipDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Tutorial Paused")
            .setMessage("You have a paused tutorial. What would you like to do?")
            .setPositiveButton("Resume") { _, _ ->
                tutorialIsPaused = false
                saveTutorialStateToPreferences()
                saveTutorialStateToFirestore()
                startTutorialFromStep(currentTutorialActualStep)
            }
            .setNegativeButton("Restart") { _, _ ->
                resetTutorialProgress()
                startTutorialFromStep(1)
            }
            .setNeutralButton("Skip Tour") { _, _ ->
                skipEntireTutorial()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRestartTutorialConfirmationDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Restart Tutorial?")
            .setMessage("Would you like to restart the quick tour from the beginning?")
            .setPositiveButton("Restart") { _, _ ->
                resetTutorialProgress()
                startTutorialFromStep(1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun startTutorialFromStep(step: Int) {
        if (isFinishing || isDestroyed) {
            Log.w("Tutorial", "Activity finishing/destroyed, cannot start tutorial step $step.")
            return
        }

        if (tutorialOverallCompletedOrSkipped && step <= TOTAL_QUICK_TOUR_STEPS) {
            tutorialIsPaused = false // Allow replaying
        } else if (tutorialOverallCompletedOrSkipped) {
            Log.d("Tutorial", "Tutorial overall completed or skipped, not starting step $step unless replaying.")
            return
        }

        currentTutorialActualStep = step
        tutorialIsPaused = false
        saveTutorialStateToPreferences()
        // saveTutorialStateToFirestore() // Firestore saved on mark step completed or pause/skip

        val targetView: View
        val message: String

        currentTutorialPopupWindow?.dismiss()
        tutorialOverlay.visibility = View.GONE

        when (step) {
            1 -> {
                targetView = findViewById(R.id.greeting_card)
                message = "Welcome! This is your personalized greeting card, showing your name and details."
            }
            2 -> {
                targetView = findViewById(R.id.filter_buttons)
                message = "Here you can filter events: view Upcoming, Ongoing, Ended, or Cancelled events."
            }
            3 -> {
                targetView = findViewById(R.id.recycler_events)
                message = "Your selected events will appear here. Scroll to see more if available."
            }
            4 -> {
                targetView = findViewById(R.id.btntime_in) // Assuming btntime_in is the main action button for this step
                message = "Ready for an event? Tap 'Time-In' here. You can also 'Time-Out' or send an excuse."
            }
            else -> {
                completeEntireTutorial()
                return
            }
        }

        targetView.post { // Ensure view is ready
            if (!isFinishing && !isDestroyed) {
                showCustomTutorialDialog(message, targetView, step, TOTAL_QUICK_TOUR_STEPS)
            }
        }
    }

    private fun completeEntireTutorial() {
        tutorialOverallCompletedOrSkipped = true
        tutorialIsPaused = false
        for (i in 1..TOTAL_QUICK_TOUR_STEPS) {
            tutorialCompletedStepsMap[i] = true
        }
        currentTutorialActualStep = TOTAL_QUICK_TOUR_STEPS
        saveTutorialStateToPreferences()
        saveTutorialStateToFirestore()

        currentTutorialPopupWindow?.dismiss()
        currentTutorialPopupWindow = null
        tutorialOverlay.visibility = View.GONE
        previousTargetLocation = null
        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this@HomeActivity, "Quick Tour Completed! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseTutorialSession() {
        tutorialIsPaused = true
        // currentTutorialActualStep is already set to the step being paused
        saveTutorialStateToPreferences()
        saveTutorialStateToFirestore()

        currentTutorialPopupWindow?.dismiss()
        currentTutorialPopupWindow = null
        tutorialOverlay.visibility = View.GONE
        previousTargetLocation = null
        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this, "Tour paused. You can resume it later.", Toast.LENGTH_LONG).show()
        }
    }

    private fun skipEntireTutorial() {
        tutorialOverallCompletedOrSkipped = true
        tutorialIsPaused = false
        for (i in 1..TOTAL_QUICK_TOUR_STEPS) {
            tutorialCompletedStepsMap[i] = true
        }
        currentTutorialActualStep = TOTAL_QUICK_TOUR_STEPS
        saveTutorialStateToPreferences()
        saveTutorialStateToFirestore()

        currentTutorialPopupWindow?.dismiss()
        currentTutorialPopupWindow = null
        tutorialOverlay.visibility = View.GONE
        previousTargetLocation = null
        if (!isFinishing && !isDestroyed) {
            Toast.makeText(this, "Tour skipped.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetTutorialProgress() {
        tutorialOverallCompletedOrSkipped = false
        tutorialIsPaused = false
        currentTutorialActualStep = 1
        tutorialCompletedStepsMap.clear()
        for (i in 1..TOTAL_QUICK_TOUR_STEPS) {
            tutorialCompletedStepsMap[i] = false
        }
        saveTutorialStateToPreferences()
        saveTutorialStateToFirestore()
        Log.d("Tutorial", "Tutorial progress has been reset.")
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomTutorialDialog(message: String, targetView: View, currentStepNum: Int, totalSteps: Int) {
        if (isFinishing || isDestroyed) {
            tutorialOverlay.visibility = View.GONE
            return
        }

        tutorialOverlay.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)

        val progressTextView = dialogView.findViewById<TextView>(R.id.tutorial_progress_text)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_tutorial_step)

        progressTextView.text = "Step $currentStepNum of $totalSteps"
        messageTextView.text = message
        nextButton.text = if (currentStepNum == totalSteps) "Finish" else "Next"

        dialogView.measure(View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST))
        val dialogWidth = dialogView.measuredWidth; val dialogHeight = dialogView.measuredHeight
        var finalDialogX: Int; var finalDialogY: Int
        val currentTargetLocationOnScreen = IntArray(2); targetView.getLocationOnScreen(currentTargetLocationOnScreen)

        if (targetView.visibility == View.VISIBLE && targetView.width > 0 && targetView.height > 0) {
            val spaceBelow = resources.displayMetrics.heightPixels - (currentTargetLocationOnScreen[1] + targetView.height)
            val spaceAbove = currentTargetLocationOnScreen[1]; val margin = (16 * resources.displayMetrics.density).toInt()
            val maxX = resources.displayMetrics.widthPixels - dialogWidth - margin; val minX = margin
            finalDialogX = when { maxX < minX -> margin; else -> (currentTargetLocationOnScreen[0] + targetView.width / 2 - dialogWidth / 2).coerceIn(minX, maxX) }
            finalDialogY = if (spaceBelow >= dialogHeight + 24) currentTargetLocationOnScreen[1] + targetView.height + 16
            else if (spaceAbove >= dialogHeight + 24) currentTargetLocationOnScreen[1] - dialogHeight - 16
            else (resources.displayMetrics.heightPixels - dialogHeight) / 2
        } else {
            finalDialogX = (resources.displayMetrics.widthPixels - dialogWidth) / 2
            finalDialogY = (resources.displayMetrics.heightPixels - dialogHeight) / 2
        }

        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        popupWindow.isOutsideTouchable = false; popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)))

        currentTutorialPopupWindow?.dismiss() // Dismiss any old one
        currentTutorialPopupWindow = popupWindow

        popupWindow.setOnDismissListener {
            // currentTutorialPopupWindow is nulled by explicit actions like pause, complete, skip
            // or when a new popup replaces it.
        }

        val animationSet = AnimationSet(true); val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 400; alphaAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.decelerate_interpolator)
        animationSet.addAnimation(alphaAnimation)
        var startTranslateX = 0f; var startTranslateY = 0f
        if (previousTargetLocation != null && previousTargetLocation!!.size == 2) {
            val prevTargetCenterX = previousTargetLocation!![0] + targetView.width / 2
            val prevTargetCenterY = previousTargetLocation!![1] + targetView.height / 2
            val deltaX = (prevTargetCenterX - (finalDialogX + dialogWidth / 2)).toFloat()
            val deltaY = (prevTargetCenterY - (finalDialogY + dialogHeight / 2)).toFloat()
            if (abs(deltaX) > abs(deltaY)) { startTranslateX = deltaX; startTranslateY = 0f } else { startTranslateX = 0f; startTranslateY = deltaY }
        } else { startTranslateX = -dialogWidth.toFloat() * 1.2f; startTranslateY = 0f }
        val translateAnimation = TranslateAnimation(startTranslateX, 0f, startTranslateY, 0f)
        translateAnimation.duration = 600; translateAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.anticipate_overshoot_interpolator)
        animationSet.addAnimation(translateAnimation); dialogView.startAnimation(animationSet)

        if (!isFinishing && !isDestroyed) {
            popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, finalDialogX, finalDialogY)
            val currentTargetScreenPos = IntArray(2); targetView.getLocationOnScreen(currentTargetScreenPos)
            previousTargetLocation = currentTargetScreenPos
        }


        nextButton.setOnClickListener {
            markTutorialStepAsCompleted(currentStepNum)
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    if (popupWindow == currentTutorialPopupWindow) popupWindow.dismiss()
                    if (currentStepNum < totalSteps) {
                        startTutorialFromStep(currentStepNum + 1)
                    } else {
                        completeEntireTutorial()
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }

        closeButton.setOnClickListener {
            showPauseOrExitTutorialDialog()
        }

        pauseButton.setOnClickListener {
            if (popupWindow == currentTutorialPopupWindow) pauseTutorialSession()
        }

        replayButton.setOnClickListener {
            if (popupWindow == currentTutorialPopupWindow) popupWindow.dismiss()
            startTutorialFromStep(currentStepNum)
        }

        skipStepButton.setOnClickListener {
            markTutorialStepAsCompleted(currentStepNum)
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    if (popupWindow == currentTutorialPopupWindow) popupWindow.dismiss()
                    if (currentStepNum < totalSteps) {
                        startTutorialFromStep(currentStepNum + 1)
                    } else {
                        completeEntireTutorial()
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }
    }

    private fun showPauseOrExitTutorialDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Exit Tutorial?")
            .setMessage("What would you like to do?")
            .setPositiveButton("Pause & Exit") { _, _ ->
                pauseTutorialSession()
            }
            .setNegativeButton("Skip Rest of Tour") { _, _ ->
                skipEntireTutorial()
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    // --- End Enhanced Tutorial Management Functions ---

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else if (currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing) {
            showPauseOrExitTutorialDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasTimedOutToday()) {
            profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
            val sidebarImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image)
            sidebarImage.setImageResource(R.drawable.ic_profile)
        } else {
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
        }
        evaluateAndDisplayAttendanceBadge()
        loadUserStatus() // Refresh status spinner

        // Check tutorial state on resume
        if (!tutorialOverallCompletedOrSkipped && tutorialIsPaused) {
            if (currentTutorialPopupWindow == null || !currentTutorialPopupWindow!!.isShowing) {
                showResumeRestartSkipDialog()
            }
        }
    }

    private fun hasTimedInToday(callback: (Boolean) -> Unit) {
        userId ?: return callback(false)
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
        ref.orderByChild("timestamp").limitToLast(1) // Check only the very last log
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        callback(false)
                        return
                    }
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    // Check the single latest log
                    val child = snapshot.children.firstOrNull()
                    if (child != null) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        if (type == "TimeIn" && timestamp >= todayStart) {
                            callback(true)
                            return
                        }
                    }
                    callback(false)
                }
                override fun onCancelled(error: DatabaseError) { callback(false) }
            })
    }

    private fun showStatusConfirmationDialog(selectedStatus: String) {
        if (isFinishing || isDestroyed) return

        if (selectedStatus == "On Duty") {
            hasTimedInToday { alreadyTimedIn ->
                if (!alreadyTimedIn) {
                    AlertDialog.Builder(this)
                        .setTitle("Time-In Required")
                        .setMessage("You haven't timed in yet for today. Do you want to time in now?")
                        .setPositiveButton("Yes") { _, _ ->
                            val intent = Intent(this, TimeInActivity::class.java).apply {
                                putExtra("userId", userId)
                                putExtra("email", userEmail ?: "")
                                putExtra("firstName", userFirstName ?: "User")
                            }
                            timeInLauncher.launch(intent)
                        }
                        .setNegativeButton("Cancel") { _, _ -> loadUserStatus() } // Reset spinner to actual status
                        .show()
                } else { // Already timed in, but wants to set to "On Duty" (e.g. from "On Break")
                    confirmStatusChange(selectedStatus)
                }
            }
        } else {
            confirmStatusChange(selectedStatus)
        }
    }

    private fun confirmStatusChange(status: String) {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Confirm Status Change")
            .setMessage("Are you sure you want to set your status to '$status'?")
            .setPositiveButton("Yes") { _, _ ->
                updateUserStatus(status)
                updateTimeLogsStatus(status) // Also update the status in the latest timeLog
                if (status == "Off Duty") {
                    handleTimeOutOnOffDuty()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> loadUserStatus() } // Reset spinner to actual status
            .show()
    }

    private fun updateUserStatus(status: String) {
        userId ?: return
        firestore.collection("users").document(userId!!)
            .update("status", status)
            .addOnSuccessListener { Log.d("HomeActivity", "Firestore status updated to $status") }
            .addOnFailureListener { Log.e("HomeActivity", "Failed to update Firestore status: ${it.message}") }
    }

    private fun updateTimeLogsStatus(status: String) {
        userId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.ref?.child("status")?.setValue(status)
                        ?.addOnSuccessListener { Log.d("HomeActivity", "TimeLog status updated to $status") }
                        ?.addOnFailureListener { Log.e("HomeActivity", "Failed to update TimeLog status: ${it.message}") }
                }
                override fun onCancelled(error: DatabaseError) { Log.e("HomeActivity", "Failed to query TimeLog for status update: ${error.message}") }
            })
    }


    private fun handleTimeOutOnOffDuty() {
        // This is called when status is set to "Off Duty" via spinner
        // The TimeOutActivity itself handles creating the TimeOut log.
        // We just need to ensure the UI reflects this possibility.
        val intent = Intent(this, TimeOutActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("email", userEmail ?: "")
            putExtra("firstName", userFirstName ?: "User")
        }
        startActivity(intent) // User will complete time-out there
    }

    private fun loadUserStatus() {
        userId ?: return
        val realtimeRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
        realtimeRef.orderByChild("timestamp").limitToLast(10) // Check recent logs
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestTimeIn: Long? = null
                    var latestTimeOut: Long? = null
                    var statusFromLatestLog: String? = null

                    // Iterate to find the absolute latest TimeIn or TimeOut and its status
                    var mostRecentTimestamp: Long = 0
                    var finalDeterminedStatus = "Off Duty" // Default

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val logStatus = child.child("status").getValue(String::class.java)

                        if (timestamp > mostRecentTimestamp) {
                            mostRecentTimestamp = timestamp
                            if (!logStatus.isNullOrEmpty()) {
                                statusFromLatestLog = logStatus
                            } else { // Fallback if status field is missing in log
                                statusFromLatestLog = if (type == "TimeIn") "On Duty" else "Off Duty"
                            }
                        }

                        if (type == "TimeIn") {
                            if (latestTimeIn == null || timestamp > latestTimeIn) latestTimeIn = timestamp
                        } else if (type == "TimeOut") {
                            if (latestTimeOut == null || timestamp > latestTimeOut) latestTimeOut = timestamp
                        }
                    }

                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    if (statusFromLatestLog != null && mostRecentTimestamp >= todayStart) {
                        finalDeterminedStatus = statusFromLatestLog!!
                    } else { // Fallback if no recent log with status or no logs today
                        finalDeterminedStatus = if (latestTimeOut != null && latestTimeOut >= todayStart && (latestTimeIn == null || latestTimeOut > latestTimeIn)) {
                            "Off Duty"
                        } else if (latestTimeIn != null && latestTimeIn >= todayStart) {
                            "On Duty"
                        } else {
                            "Off Duty"
                        }
                    }

                    // Update Firestore user status based on the determined status
                    firestore.collection("users").document(userId!!)
                        .update("status", finalDeterminedStatus)
                        .addOnSuccessListener {
                            Log.d("HomeActivity", "User status synced from RTDB to Firestore: $finalDeterminedStatus")
                            val index = statusOptions.indexOf(finalDeterminedStatus)
                            if (index != -1 && !isUserChangingStatus) { // Avoid loop if user is interacting
                                statusSpinner.setSelection(index, false) // false to prevent re-triggering onItemSelected
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("HomeActivity", "Failed to sync user status to Firestore: ${e.message}")
                            // Fallback to setting spinner from logs anyway if Firestore update fails
                            val index = statusOptions.indexOf(finalDeterminedStatus)
                            if (index != -1 && !isUserChangingStatus) {
                                statusSpinner.setSelection(index, false)
                            }
                        }
                }
                override fun onCancelled(error: DatabaseError) { Log.e("HomeActivity", "Failed to load user status from RTDB: ${error.message}") }
            })
    }


    private fun updateFilterButtonStates(selectedButton: Button) {
        val filterButtons = listOf(btnUpcoming, btnOngoing, btnEnded, btnCancelled)
        filterButtons.forEach { button ->
            if (button.id == selectedButton.id) {
                button.isSelected = true
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.maroon)
                button.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                button.isSelected = false
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray) // Or your default unselected background
                button.setTextColor(ContextCompat.getColor(this, R.color.dark_gray)) // Or your default unselected text color
            }
        }
    }

    private fun hasTimedOutToday(): Boolean {
        val prefs = getSharedPreferences("TimeOutPrefs", MODE_PRIVATE)
        val lastTimedOutDate = prefs.getString("lastTimedOutDate", null)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return lastTimedOutDate == today
    }

    private fun setTimedOutToday() {
        val prefs = getSharedPreferences("TimeOutPrefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit().putString("lastTimedOutDate", today).apply()
    }


    private fun loadTodayTimeInPhoto() {
        userId ?: return
        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
        usersRef.get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")

            if (userStatus == "On Duty") {
                val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
                ref.orderByChild("timestamp").limitToLast(1) // Get the latest log
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var foundTodaysTimeIn = false
                            snapshot.children.firstOrNull()?.let { child -> // Only check the latest log
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                                val todayStart = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity).load(imageUrl).circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(profileImagePlaceholder)
                                        foundTodaysTimeIn = true
                                    }
                                }
                            }
                            if (!foundTodaysTimeIn) { // If no specific time-in photo for today from latest log, or log not TimeIn
                                if (!profileUrl.isNullOrEmpty()) {
                                    Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                                } else {
                                    profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            if (!profileUrl.isNullOrEmpty()) {
                                Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                            } else {
                                profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                            }
                        }
                    })
            } else { // Off Duty or other status, or user has timed out
                if (!profileUrl.isNullOrEmpty() && !hasTimedOutToday()) { // Show profile pic if not timed out
                    Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                } else { // Show placeholder if timed out or no profile pic
                    profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                }
            }
        }.addOnFailureListener { profileImagePlaceholder.setImageResource(R.drawable.ic_profile) }
    }

    private fun updateSidebarProfileImage() {
        val headerView = navigationView.getHeaderView(0)
        val sidebarImage = headerView.findViewById<ImageView>(R.id.sidebar_profile_image)
        userId ?: return sidebarImage.setImageResource(R.drawable.ic_profile)

        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
        usersRef.get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")

            if (userStatus == "On Duty") {
                val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
                ref.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var foundTodaysTimeInSide = false
                            snapshot.children.firstOrNull()?.let { child ->
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                                val todayStart = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity).load(imageUrl).circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(sidebarImage)
                                        foundTodaysTimeInSide = true
                                    }
                                }
                            }
                            if (!foundTodaysTimeInSide) {
                                if (!profileUrl.isNullOrEmpty()) {
                                    Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                                } else {
                                    sidebarImage.setImageResource(R.drawable.ic_profile)
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            if (!profileUrl.isNullOrEmpty()) {
                                Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                            } else {
                                sidebarImage.setImageResource(R.drawable.ic_profile)
                            }
                        }
                    })
            } else { // Off Duty or timed out
                if (!profileUrl.isNullOrEmpty() && !hasTimedOutToday()) {
                    Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                } else {
                    sidebarImage.setImageResource(R.drawable.ic_profile)
                }
            }
        }.addOnFailureListener { sidebarImage.setImageResource(R.drawable.ic_profile) }
    }

    private fun sendEventNotification(title: String, message: String) {
        val channelId = "event_channel_id"
        val notificationId = System.currentTimeMillis().toInt() // Unique ID for each notification
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Event Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for upcoming, ongoing, or cancelled events."
            }
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(notificationId, builder.build())
    }

    private fun setupNavigationDrawer() {
        val headerView = navigationView.getHeaderView(0)
        val sidebarName = headerView.findViewById<TextView>(R.id.sidebar_user_name)
        val sidebarDetails = headerView.findViewById<TextView>(R.id.sidebar_user_details)
        val sidebarEmail = headerView.findViewById<TextView>(R.id.sidebar_user_email)

        sidebarName.text = userFirstName ?: "User"
        userId?.let { uid ->
            val usersRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            usersRef.get().addOnSuccessListener { userDoc ->
                val currentIdNumber = userDoc.getString("idNumber") ?: idNumber // Prefer Firestore if available
                val departmentId = userDoc.getString("departmentId")
                if (!departmentId.isNullOrEmpty()) {
                    FirebaseFirestore.getInstance().collection("departments")
                        .document(departmentId)
                        .get()
                        .addOnSuccessListener { deptDoc ->
                            val abbreviation = deptDoc.getString("abbreviation") ?: "N/A"
                            sidebarDetails.text = "$currentIdNumber • $abbreviation"
                        }
                        .addOnFailureListener { sidebarDetails.text = "$currentIdNumber • N/A" }
                } else {
                    sidebarDetails.text = "$currentIdNumber • N/A"
                }
            }
        } ?: run {
            sidebarDetails.text = "$idNumber • N/A"
        }
        sidebarEmail.text = userEmail ?: ""


        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.END) // Close drawer first
            // Use a small delay to allow drawer to close before navigating
            Handler(Looper.getMainLooper()).postDelayed({
                when (menuItem.itemId) {
                    R.id.nav_home -> { /* Already home or handled by closing drawer */ }
                    R.id.nav_event_log -> {
                        val intent = Intent(this, EventLogActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                    }
                    R.id.nav_excuse_letter -> {
                        val intent = Intent(this, ExcuseLetterActivity::class.java).apply {
                            putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department)
                        }
                        startActivity(intent)
                    }
                    R.id.nav_excuse_letter_history -> {
                        val intent = Intent(this, ExcuseLetterHistoryActivity::class.java)
                        intent.putExtra("userId", userId)
                        startActivity(intent)
                    }
                    R.id.nav_profile -> {
                        val intent = Intent(this, ProfileActivity::class.java).apply {
                            putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department)
                        }
                        startActivity(intent)
                    }
                    R.id.nav_logout -> { showLogoutDialog() }
                }
            }, 250) // 250ms delay
            true // Indicate item is handled
        }
    }

    private fun showLogoutDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                val loginPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                loginPrefs.edit().clear().apply()
                // Also clear tutorial prefs on logout
                getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE).edit().clear().apply()
                // Clear other specific preferences if any (e.g., TimeOutPrefs)
                getSharedPreferences("TimeOutPrefs", Context.MODE_PRIVATE).edit().clear().apply()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFilterButtons() {
        btnUpcoming.setOnClickListener { updateFilterButtonStates(btnUpcoming); showEventsByStatus("upcoming") }
        btnOngoing.setOnClickListener { updateFilterButtonStates(btnOngoing); showEventsByStatus("ongoing") }
        btnEnded.setOnClickListener { updateFilterButtonStates(btnEnded); showEventsByStatus("ended") }
        btnCancelled.setOnClickListener { updateFilterButtonStates(btnCancelled); showEventsByStatus("cancelled") }

        // Set default selection
        updateFilterButtonStates(btnUpcoming)
    }

    private fun setupActionButtons() {
        btnTimeIn.setOnClickListener {
            if (isFinishing || isDestroyed) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Time - In Confirmation")
                .setMessage("Are you ready to time in for today?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(this, TimeInActivity::class.java)
                    intent.putExtra("userId", userId)
                    intent.putExtra("email", userEmail ?: "")
                    intent.putExtra("firstName", userFirstName ?: "User")
                    timeInLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        btnTimeOut.setOnClickListener {
            if (isFinishing || isDestroyed) return@setOnClickListener
            hasTimedInToday { alreadyTimedIn ->
                if (!alreadyTimedIn) {
                    AlertDialog.Builder(this)
                        .setTitle("Cannot Time-Out")
                        .setMessage("You haven't timed in yet for today. Please time in first before timing out.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Time - Out Confirmation")
                        .setMessage("Are you sure you want to time out for today?")
                        .setPositiveButton("Yes") { _, _ ->
                            setTimedOutToday() // Mark that user has initiated time-out
                            loadTodayTimeInPhoto() // This will now show placeholder due to hasTimedOutToday()
                            updateSidebarProfileImage() // Same for sidebar

                            val intent = Intent(this, TimeOutActivity::class.java).apply {
                                putExtra("userId", userId); putExtra("email", userEmail ?: ""); putExtra("firstName", userFirstName ?: "User")
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }

    private fun setupExcuseLetterRedirect() {
        excuseLetterText.setOnClickListener {
            val intent = Intent(this, ExcuseLetterActivity::class.java).apply {
                putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department)
            }
            startActivity(intent)
        }
    }

    private fun loadAndStoreEvents() {
        userId ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId!!).get()
            .addOnSuccessListener { userDoc ->
                val departmentId: String? = userDoc.getString("departmentId")
                if (departmentId.isNullOrEmpty()) {
                    if (!isFinishing) Toast.makeText(this, "No department assigned. Cannot load events.", Toast.LENGTH_LONG).show()
                    allEvents.clear()
                    showEventsByStatus("upcoming") // Show empty state
                    updateFilterButtonStates(btnUpcoming) // Default filter button state
                    return@addOnSuccessListener
                }

                firestore.collection("events").whereEqualTo("departmentId", departmentId).get()
                    .addOnSuccessListener { result ->
                        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        allEvents.clear()
                        val nowMillis = System.currentTimeMillis()
                        val previouslyNotifiedEvents = getSharedPreferences("EventNotificationsPrefs", Context.MODE_PRIVATE)


                        for (doc in result) {
                            try {
                                val eventId = doc.id
                                val title = doc.getString("eventName") ?: continue
                                val durationStr = doc.getString("duration") ?: "1:00:00"
                                val date = doc.getTimestamp("date")?.toDate() ?: continue
                                val dateFormatted = formatter.format(date)
                                val statusFromDb = doc.getString("status") ?: "upcoming"

                                val durationParts = durationStr.split(":")
                                val durationMillis = when (durationParts.size) {
                                    3 -> (durationParts[0].toLongOrNull() ?: 0) * 3600000L +
                                            (durationParts[1].toLongOrNull() ?: 0) * 60000L +
                                            (durationParts[2].toLongOrNull() ?: 0) * 1000L
                                    2 -> (durationParts[0].toLongOrNull() ?: 0) * 3600000L +
                                            (durationParts[1].toLongOrNull() ?: 0) * 60000L
                                    1 -> (durationParts[0].toLongOrNull() ?: 0) * 60000L // Assume minutes
                                    else -> 3600000L // Default 1 hour
                                }

                                val eventStartMillis = date.time
                                val eventEndMillis = eventStartMillis + durationMillis

                                val dynamicStatus = when {
                                    statusFromDb.equals("cancelled", ignoreCase = true) -> "cancelled"
                                    nowMillis < eventStartMillis -> "upcoming"
                                    nowMillis in eventStartMillis..eventEndMillis -> "ongoing"
                                    else -> "ended"
                                }

                                if (dynamicStatus != statusFromDb && !statusFromDb.equals("cancelled", ignoreCase = true)) {
                                    doc.reference.update("status", dynamicStatus)
                                        .addOnSuccessListener { Log.d("EventSync", "Event '$title' status updated to $dynamicStatus in Firestore.") }
                                        .addOnFailureListener { e -> Log.e("EventSync", "Failed to update event status for '$title'", e)}
                                }

                                // Notification Logic
                                val notifiedKey = "notified_${eventId}_${dynamicStatus}"
                                if (!previouslyNotifiedEvents.getBoolean(notifiedKey, false)) {
                                    var shouldNotify = false
                                    var notifTitle = ""
                                    var notifMsg = ""

                                    when (dynamicStatus) {
                                        "upcoming" -> {
                                            val timeToStart = eventStartMillis - nowMillis
                                            if (timeToStart in (1L)..(15 * 60 * 1000L)) { // 0-15 mins
                                                shouldNotify = true
                                                notifTitle = "Event Starting Soon!"
                                                notifMsg = "'$title' is starting in about ${timeToStart / 60000} minutes."
                                            } else if (timeToStart in (15 * 60 * 1000L + 1)..(60 * 60 * 1000L)) { // 15-60 mins
                                                shouldNotify = true
                                                notifTitle = "Upcoming Event Reminder"
                                                notifMsg = "'$title' is scheduled for today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}."
                                            }
                                        }
                                        "ongoing" -> {
                                            shouldNotify = true
                                            notifTitle = "Event Started!"
                                            notifMsg = "'$title' is now ongoing."
                                        }
                                        "cancelled" -> { // Only notify if it just got cancelled
                                            if (statusFromDb != "cancelled") { // Check if it wasn't already cancelled
                                                shouldNotify = true
                                                notifTitle = "Event Cancelled"
                                                notifMsg = "The event '$title' has been cancelled."
                                            }
                                        }
                                        // "ended" notifications are usually not sent unless specifically required.
                                    }
                                    if (shouldNotify && !isFinishing) {
                                        sendEventNotification(notifTitle, notifMsg)
                                        previouslyNotifiedEvents.edit().putBoolean(notifiedKey, true).apply()
                                    }
                                }
                                allEvents.add(EventModel(title, durationStr, dateFormatted, dynamicStatus, rawDate = date))
                            } catch (e: Exception) {
                                Log.e("FirestoreEvents", "Skipping event due to error: ${e.message}", e)
                            }
                        }
                        // Determine which filter is currently selected to refresh its view
                        val currentSelectedFilterButton = listOf(btnUpcoming, btnOngoing, btnEnded, btnCancelled).firstOrNull { it.isSelected }
                        val filterTag = currentSelectedFilterButton?.tag?.toString() ?: "upcoming" // Default to upcoming
                        showEventsByStatus(filterTag)
                        // Ensure the correct button state if it was defaulted
                        if (currentSelectedFilterButton == null) updateFilterButtonStates(btnUpcoming)


                    }
                    .addOnFailureListener {
                        Log.e("Firestore", "Failed to load events: ${it.message}", it)
                        if (!isFinishing) Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch user document: ${it.message}", it)
                if (!isFinishing) Toast.makeText(this, "Failed to load user info for events.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showEventsByStatus(statusFilter: String?) {
        val filtered = if (statusFilter == null) {
            allEvents
        } else {
            allEvents.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        val sorted = filtered.sortedWith(compareBy({ statusOrder(it.status) }, { it.rawDate }))
        recyclerEvents.adapter = EventAdapter(sorted)

        val readableStatus = statusFilter?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Selected"
        if (sorted.isEmpty()) {
            noEventsMessage.visibility = View.VISIBLE
            noEventsMessage.text = "No $readableStatus event/s at the moment."
        } else {
            noEventsMessage.visibility = View.GONE
        }
    }

    private fun statusOrder(status: String): Int {
        return when (status.lowercase(Locale.ROOT)) {
            "upcoming" -> 0
            "ongoing" -> 1
            "ended" -> 2
            "cancelled" -> 3
            else -> 4
        }
    }

    private fun updateAttendanceBadge(status: String) {
        if (isFinishing || isDestroyed) return
        attendanceStatusBadge.visibility = View.VISIBLE
        when (status.trim().lowercase()) {
            "on time" -> { attendanceStatusBadge.text = "On Time"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_green)); attendanceStatusBadge.background = null }
            "late" -> { attendanceStatusBadge.text = "Late"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_yellow)); attendanceStatusBadge.background = null }
            "absent" -> { attendanceStatusBadge.text = "Absent"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_red)); attendanceStatusBadge.background = null }
            "not timed-in" -> { attendanceStatusBadge.text = "Not Timed-In"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray)); attendanceStatusBadge.background = null }
            "timed-out" -> { attendanceStatusBadge.text = "Timed-Out"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray)); attendanceStatusBadge.background = null }
            else -> attendanceStatusBadge.visibility = View.GONE
        }
    }

    private fun writeAttendanceStatusToRealtime(logId: String, badgeStatus: String) {
        userId ?: return
        FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!).child(logId)
            .child("attendanceBadge").setValue(badgeStatus)
            .addOnSuccessListener { Log.d("BadgeWrite", "Successfully wrote badge '$badgeStatus' to log $logId") }
            .addOnFailureListener { e -> Log.e("BadgeWrite", "Failed to write badge to log $logId", e) }
    }


    private fun evaluateAndDisplayAttendanceBadge() {
        userId ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId!!)
        val now = Calendar.getInstance()
        val currentTime = now.timeInMillis
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cutoff9am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val cutoff10am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

        ref.orderByChild("timestamp").startAt(todayStart.toDouble()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var latestTimeInLog: DataSnapshot? = null
                var latestTimeOutLog: DataSnapshot? = null

                for (child in snapshot.children) {
                    val type = child.child("type").getValue(String::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    if (timestamp >= todayStart) { // Only consider logs from today
                        if (type == "TimeIn") {
                            if (latestTimeInLog == null || timestamp > (latestTimeInLog.child("timestamp").getValue(Long::class.java) ?: 0L) ) {
                                latestTimeInLog = child
                            }
                        } else if (type == "TimeOut") {
                            if (latestTimeOutLog == null || timestamp > (latestTimeOutLog.child("timestamp").getValue(Long::class.java) ?: 0L) ) {
                                latestTimeOutLog = child
                            }
                        }
                    }
                }

                val timeInTimestamp = latestTimeInLog?.child("timestamp")?.getValue(Long::class.java)
                val timeOutTimestamp = latestTimeOutLog?.child("timestamp")?.getValue(Long::class.java)

                if (timeOutTimestamp != null && (timeInTimestamp == null || timeOutTimestamp > timeInTimestamp)) {
                    // User has timed out today as the latest action
                    val badgeFromLog = latestTimeOutLog?.child("attendanceBadge")?.getValue(String::class.java)
                    updateAttendanceBadge(badgeFromLog ?: "Timed-Out") // Use logged badge or default to "Timed-Out"
                    if (badgeFromLog == null && latestTimeOutLog?.key != null) { // If no badge, set it
                        writeAttendanceStatusToRealtime(latestTimeOutLog.key!!, "Timed-Out")
                    }
                } else if (timeInTimestamp != null) {
                    // User has timed in, and it's the latest action or no time-out yet today
                    var badge = latestTimeInLog?.child("attendanceBadge")?.getValue(String::class.java)
                    if (badge.isNullOrEmpty()) { // Badge not set, determine it
                        badge = when {
                            timeInTimestamp < cutoff9am -> "On Time"
                            timeInTimestamp < cutoff10am -> "Late"
                            else -> "Absent" // Considered absent if timed in very late
                        }
                        if (latestTimeInLog?.key != null) {
                            writeAttendanceStatusToRealtime(latestTimeInLog.key!!, badge)
                        }
                    }
                    updateAttendanceBadge(badge)
                } else {
                    // No TimeIn or TimeOut logs for today
                    val todayFormatted = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
                    val excuseRef = FirebaseDatabase.getInstance().getReference("excuseLetters").child(userId!!)
                    excuseRef.orderByChild("date").equalTo(todayFormatted)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(excuseSnapshot: DataSnapshot) {
                                var isExcusedApproved = false
                                for (doc in excuseSnapshot.children) {
                                    val status = doc.child("status").getValue(String::class.java)
                                    if (status.equals("Approved", ignoreCase = true)) {
                                        isExcusedApproved = true
                                        break
                                    }
                                }

                                if (isExcusedApproved) {
                                    updateAttendanceBadge("Absent") // Excused absence
                                } else if (currentTime > cutoff10am) {
                                    updateAttendanceBadge("Not Timed-In")
                                } else {
                                    attendanceStatusBadge.visibility = View.GONE
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("ExcuseCheck", "Error checking excuse letters: ${error.message}")
                                if (currentTime > cutoff10am) updateAttendanceBadge("Not Timed-In")
                                else attendanceStatusBadge.visibility = View.GONE
                            }
                        })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("BadgeEval", "Failed to evaluate attendance badge: ${error.message}")
                updateAttendanceBadge("Not Timed-In")
            }
        })
    }
}