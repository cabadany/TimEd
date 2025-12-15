package com.example.timed_mobile

import com.example.timed_mobile.adapter.StatusAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater

import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.animation.ObjectAnimator
import android.animation.AnimatorListenerAdapter
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.timed_mobile.EventDetailActivity
import com.example.timed_mobile.adapter.EventAdapter
import com.example.timed_mobile.model.EventModel
import com.example.timed_mobile.tutorial.EventTutorialState
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

import android.graphics.drawable.ColorDrawable
import android.app.Dialog
import android.view.ViewGroup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import androidx.core.app.NotificationCompat
import android.Manifest
import com.example.timed_mobile.utils.TimeSettingsManager


class HomeActivity : WifiSecurityActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val TAG_TUTORIAL_NAV = "HomeActivityTutorialNav"

        const val PREFS_TUTORIAL = "TutorialPrefs"

        // --- Quick Tour Keys ---
        const val TOTAL_QUICK_TOUR_STEPS = 4
        const val KEY_QUICK_TOUR_COMPLETED =
            "quickTourCompleted" // Renamed from KEY_TUTORIAL_COMPLETED
        const val KEY_QUICK_TOUR_CURRENT_STEP =
            "quickTourCurrentStep" // Added for progress tracking

        const val EXTRA_IS_TUTORIAL_MODE = "is_tutorial_mode"

        // --- Attendance Workflow Guide Keys ---
        const val TOTAL_ATTENDANCE_TUTORIAL_STEPS = 6
        const val KEY_ATTENDANCE_TUTORIAL_COMPLETED = "attendanceTutorialCompleted"
        const val KEY_ATTENDANCE_GUIDE_CURRENT_STEP =
            "attendanceGuideCurrentStep" // Added for progress tracking

        const val TOTAL_EVENT_TUTORIAL_STEPS = 4
        const val KEY_EVENT_TUTORIAL_COMPLETED = "eventTutorialCompleted"
        const val KEY_EVENT_TUTORIAL_CURRENT_STEP = "eventTutorialCurrentStep"
        const val KEY_EVENT_TUTORIAL_EXPECTED_ACTION = "eventTutorialExpectedAction"
        const val KEY_EVENT_TUTORIAL_ACTIVE = "eventTutorialActive"

        // --- Interactive Tutorial Action Constants ---
        private const val TUTORIAL_NAME_QUICK_TOUR = "TUTORIAL_NAME_QUICK_TOUR"
        private const val TUTORIAL_NAME_ATTENDANCE = "TUTORIAL_NAME_ATTENDANCE"
        private const val TUTORIAL_NAME_EVENT = "TUTORIAL_NAME_EVENT"
        private const val ACTION_USER_CLICK_TIME_IN = "ACTION_USER_CLICK_TIME_IN"
        private const val ACTION_USER_PERFORMED_TIME_IN = "ACTION_USER_PERFORMED_TIME_IN"

        // TODO: Add more for other interactive steps:
        // private const val ACTION_USER_CLICK_TIME_OUT = "ACTION_USER_CLICK_TIME_OUT"
        // private const val ACTION_USER_PERFORMED_TIME_OUT = "ACTION_USER_PERFORMED_TIME_OUT"
        // private const val ACTION_USER_SELECT_STATUS = "ACTION_USER_SELECT_STATUS"
        // private const val ACTION_USER_PERFORMED_STATUS_SELECT = "ACTION_USER_PERFORMED_STATUS_SELECT"
        // private const val ACTION_USER_CLICK_EXCUSE_LETTER = "ACTION_USER_CLICK_EXCUSE_LETTER"
        // private const val ACTION_USER_PERFORMED_EXCUSE_LETTER = "ACTION_USER_PERFORMED_EXCUSE_LETTER"
        const val ACTION_EVENT_OPEN_CALENDAR = "ACTION_EVENT_OPEN_CALENDAR"
        const val ACTION_EVENT_SELECT_EVENT = "ACTION_EVENT_SELECT_EVENT"
        const val ACTION_EVENT_TIME_IN = "ACTION_EVENT_TIME_IN"
        const val ACTION_EVENT_TIME_OUT = "ACTION_EVENT_TIME_OUT"
    }

    private var currentTutorialPopupWindow: PopupWindow? = null

    // These will be set when a specific tutorial flow starts.
    private var activeTutorialCompletionKey: String = KEY_QUICK_TOUR_COMPLETED
    private var activeTutorialStepKey: String = KEY_QUICK_TOUR_CURRENT_STEP
    private var activeTutorialTotalSteps: Int = TOTAL_QUICK_TOUR_STEPS


    // New state variables for managing interactive tutorials
    private var isInteractiveTutorialActive: Boolean = false
    private var currentInteractiveTutorialName: String? = null
    private var expectedInteractiveTutorialAction: String? = null

    private val timeInActivityTutorialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        if (result.resultCode == RESULT_OK) {
            if (isInteractiveTutorialActive &&
                currentInteractiveTutorialName == TUTORIAL_NAME_ATTENDANCE &&
                expectedInteractiveTutorialAction == ACTION_USER_PERFORMED_TIME_IN
            ) {

                Log.d(TAG, "Attendance Tutorial: TimeInActivity successful (Step 1).")
                tutorialPrefs.edit().putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 1).apply()
                updateNavHeaderTutorialProgress() // Update nav header after step completion
                Log.d(
                    TAG,
                    "Attendance tutorial: TimeInActivity part (step 1) completed. Saved step 1."
                )

                // Proceed to the next tutorial step instruction
                showEventCalendarTutorialStep()
                
            } else {
                // TimeInActivity was successful, but not as part of the expected tutorial action
                Log.w(
                    TAG,
                    "TimeInActivity (tutorial mode) returned OK, but not as expected by interactive tutorial state ($expectedInteractiveTutorialAction). Current tutorial: $currentInteractiveTutorialName"
                )
                // Handle normal successful time-in UI updates
                Toast.makeText(this, "Time-In recorded.", Toast.LENGTH_SHORT).show()
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
                if (isInteractiveTutorialActive) handleTutorialCancellation() // Cancel tutorial if state is unexpected
            }
        } else { // Result was not OK (e.g., user cancelled TimeInActivity)
            if (isInteractiveTutorialActive &&
                currentInteractiveTutorialName == TUTORIAL_NAME_ATTENDANCE &&
                expectedInteractiveTutorialAction == ACTION_USER_PERFORMED_TIME_IN
            ) {

                Log.d(
                    TAG,
                    "Attendance Tutorial: TimeInActivity cancelled or failed by user (Step 1)."
                )
                Toast.makeText(
                    this,
                    "Time-In was not completed. Please tap the 'Time-In' button again to try.",
                    Toast.LENGTH_LONG
                ).show()
                expectedInteractiveTutorialAction =
                    ACTION_USER_CLICK_TIME_IN // Reset to expect click again
                hideOverlay()
                updateNavHeaderTutorialProgress() // Reflect current (unchanged) progress
            } else {
                Log.w(
                    TAG,
                    "TimeInActivity (tutorial mode) cancelled, but not as expected by interactive tutorial state ($expectedInteractiveTutorialAction). Current tutorial: $currentInteractiveTutorialName"
                )
                if (isInteractiveTutorialActive) handleTutorialCancellation()
                else Toast.makeText(this, "Time-In was not completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var previousTargetLocationForAnimation: IntArray? = null

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
    private var tutorialCalendarSheet: com.example.timed_mobile.calendar.EventCalendarBottomSheet? =
        null
    private lateinit var noEventsMessage: TextView

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
    private lateinit var btnCalendar: ImageView
    private lateinit var profileImagePlaceholder: ImageView

    private var tutorialProgressOnRightNavHeader: LinearLayout? = null

    // Department id cached for calendar and event queries
    private var userDepartmentId: String? = null
    private var tutorialProgressBarOnRight: ProgressBar? = null
    private var tutorialTitleTextOnRight: TextView? = null
    private var tutorialPercentageTextOnRight: TextView? = null

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

    // --- Notification Permission Handling ---
    private var pendingNotification: Pair<String, String>? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Notification permission granted by user.")
                pendingNotification?.let {
                    showNotification(it.first, it.second)
                    pendingNotification = null
                }
            } else {
                Log.d(TAG, "Notification permission denied by user.")
                UiDialogs.showErrorPopup(
                    this,
                    title = "Permission Denied",
                    message = "Event notifications will not be shown without notification permission."
                )
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        // Initialize TimeSettingsManager
        TimeSettingsManager.initialize()

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        attendanceStatusBadge = findViewById(R.id.attendance_status_badge)
        tutorialOverlay = findViewById(R.id.tutorial_overlay)
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setScrimColor(0x44000000) // Semi-transparent black overlay
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        val headerView: View? = navigationView.getHeaderView(0)
        if (headerView != null) {
            tutorialProgressOnRightNavHeader =
                headerView.findViewById(R.id.tutorial_progress_on_right_nav_header)
            tutorialProgressBarOnRight =
                headerView.findViewById(R.id.tutorial_bar_on_right_nav_header)
            tutorialTitleTextOnRight = headerView.findViewById(R.id.tutorial_title_text)
            tutorialPercentageTextOnRight = headerView.findViewById(R.id.tutorial_percentage_text)
            Log.d(
                TAG_TUTORIAL_NAV,
                "Nav Header Tutorial Views Initialized: Container=${tutorialProgressOnRightNavHeader != null}, Bar=${tutorialProgressBarOnRight != null}, Title=${tutorialTitleTextOnRight != null}, Percentage=${tutorialPercentageTextOnRight != null}"
            )

            if (tutorialProgressOnRightNavHeader != null) {
                tutorialProgressOnRightNavHeader?.isClickable = true
                tutorialProgressOnRightNavHeader?.isFocusable = true
                tutorialProgressOnRightNavHeader?.setOnClickListener {
                    // Button-like press animation on the container (scale down then back) and then open the activity.
                    val container = tutorialProgressOnRightNavHeader
                    val intent = Intent(
                        this,
                        TutorialProgressActivity::class.java
                    ).apply { putExtra("userId", userId) }

                    if (container != null) {
                        try {
                            // Prevent double-tap while animating
                            if (!container.isClickable) return@setOnClickListener
                            container.isClickable = false

                            // Press in
                            container.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100)
                                .setInterpolator(DecelerateInterpolator()).withEndAction {
                                    // Release back to normal
                                    container.animate().scaleX(1f).scaleY(1f).setDuration(140)
                                        .setInterpolator(DecelerateInterpolator()).withEndAction {
                                            container.isClickable = true
                                            // Launch the activity after the visual feedback completes
                                            startActivity(intent)
                                            Log.d(
                                                TAG_TUTORIAL_NAV,
                                                "Tutorial progress header clicked, launching TutorialProgressActivity after container press animation."
                                            )
                                        }
                                }.start()
                        } catch (e: Exception) {
                            // Fallback: if animation fails, re-enable and launch immediately
                            container.isClickable = true
                            startActivity(intent)
                            Log.w(
                                TAG_TUTORIAL_NAV,
                                "Container animation failed, launching TutorialProgressActivity immediately: ${e.message}"
                            )
                        }
                    } else {
                        // If container is null, fallback to immediate launch
                        startActivity(intent)
                        Log.d(
                            TAG_TUTORIAL_NAV,
                            "Tutorial progress header clicked, launching TutorialProgressActivity (container missing)."
                        )
                    }
                }
            } else {
                Log.e(
                    TAG_TUTORIAL_NAV,
                    "tutorialProgressOnRightNavHeader (LinearLayout) itself was not found in headerView."
                )
            }

        } else {
            Log.e(
                TAG_TUTORIAL_NAV,
                "Navigation headerView is NULL. Cannot find tutorial progress views."
            )
        }

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
        btnCalendar = findViewById(R.id.btn_calendar)
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
        val animSlideDownFilters =
            AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideDownFilters.startOffset = 200L
        filterButtonsLayout.startAnimation(animSlideDownFilters)
        val animFadeInRecycler = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        animFadeInRecycler.startOffset = 400L
        recyclerEvents.startAnimation(animFadeInRecycler)
        val baseDelayBottom = 600L
        attendancePromptView?.let {
            val animSlideUpPrompt =
                AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
            animSlideUpPrompt.startOffset = baseDelayBottom
            it.startAnimation(animSlideUpPrompt)
        }
        val animSlideUpTimeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpTimeIn.startOffset =
            baseDelayBottom + (if (attendancePromptView != null) 100L else 0L)
        btnTimeIn.startAnimation(animSlideUpTimeIn)
        val animSlideUpTimeOut = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpTimeOut.startOffset =
            baseDelayBottom + (if (attendancePromptView != null) 200L else 100L)
        btnTimeOut.startAnimation(animSlideUpTimeOut)
        val animSlideUpExcuseLetter =
            AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpExcuseLetter.startOffset =
            baseDelayBottom + (if (attendancePromptView != null) 300L else 200L)
        excuseLetterText.startAnimation(animSlideUpExcuseLetter)

        btnHelp.setOnClickListener {
            showTutorialDialog()
        }
        btnCalendar.setOnClickListener {
            val deptId = userDepartmentId
            if (deptId.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "No department assigned. Cannot open calendar.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val sheet =
                    com.example.timed_mobile.calendar.EventCalendarBottomSheet.newInstance(deptId)
                tutorialCalendarSheet = sheet
                sheet.onDismissed = {
                    if (tutorialCalendarSheet === sheet) {
                        tutorialCalendarSheet = null
                    }
                }
                sheet.show(supportFragmentManager, "EventCalendar")

                if (isInteractiveTutorialActive &&
                    currentInteractiveTutorialName == TUTORIAL_NAME_EVENT
                ) {
                    if (expectedInteractiveTutorialAction == ACTION_EVENT_SELECT_EVENT) {
                        tutorialProgressOnRightNavHeader?.post { showEventGuideSelectEventStep() }
                    }
                }
            }
        }

        firestore = FirebaseFirestore.getInstance()
        loadUserStatus()

        statusSpinner.setOnTouchListener { _, _ ->
            isUserChangingStatus = true
            false
        }
        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    return
                }
                val selectedStatus = statusOptions[position]
                if (isUserChangingStatus) {
                    // Check security rules before allowing status change
                    checkStatusChangeAllowed(selectedStatus)
                }
                isUserChangingStatus = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        recyclerEvents.layoutManager = LinearLayoutManager(this)
        recyclerEvents.adapter = EventAdapter(mutableListOf(), ::onEventItemClicked)

        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
        userEmail = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
        userFirstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)
        idNumber = sharedPrefs.getString(LoginActivity.KEY_ID_NUMBER, "N/A")
        department = sharedPrefs.getString(LoginActivity.KEY_DEPARTMENT, "N/A")

        userId?.let { uid ->
            val usersRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            usersRef.get().addOnSuccessListener { doc ->
                val fullName = userFirstName ?: "User"
                greetingName.text = "Hi, $fullName"
                val departmentId = doc.getString("departmentId")
                userDepartmentId = departmentId
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

        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_deep_blue,
            R.color.primary_medium_blue
        )
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeActivity", "Pull-to-refresh triggered")
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
            loadAndStoreEvents()
            swipeRefreshLayout.isRefreshing = false
        }

        // --- Tutorial Auto-Start Logic ---
        drawerLayout.post {
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            val quickTourDone = tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false)
            val attendanceGuideDone =
                tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)

            if (!quickTourDone && !attendanceGuideDone) { // Neither completed, show dialog
                showTutorialDialog()
            } else if (!quickTourDone) { // Quick tour not done, resume it
                showQuickTour(resetProgress = false)
            } else if (!attendanceGuideDone) { // Attendance guide not done, resume it
                startAttendanceWorkflowTutorial(resetProgress = false)
            }
            // If both are done, do nothing here. Nav header progress will be hidden or show last state.
        }


        loadAndStoreEvents()
        evaluateAndDisplayAttendanceBadge()
    }

    private fun hasTimedInToday(callback: (Boolean) -> Unit) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return callback(false)

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
                        Calendar.SECOND,
                        0
                    ); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp =
                            child.child("timestamp").getValue(Long::class.java) ?: continue
                        if (type == "TimeIn" && timestamp >= todayStart) {
                            callback(true); return
                        }
                    }
                    callback(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

    /**
     * Checks if status change is allowed based on security rules:
     * 1. Must have a TimeIn record for today
     * 2. If already Off Duty (has TimeOut today), status is locked until next day
     */
    private fun checkStatusChangeAllowed(newStatus: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val dbRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        dbRef.orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasTimeInToday = false
                    var hasTimeOutToday = false
                    
                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        if (type == "TimeIn") hasTimeInToday = true
                        if (type == "TimeOut") hasTimeOutToday = true
                    }
                    
                    when {
                        // Rule 1: Must have TimeIn today
                        !hasTimeInToday -> {
                            UiDialogs.showInfoDialog(
                                this@HomeActivity,
                                title = "Time-In Required",
                                message = "Please Time-In first before changing your status."
                            )
                            loadUserStatus() // Reset spinner to current status
                        }
                        // Rule 2: Off Duty locks status until next day
                        hasTimeOutToday -> {
                            UiDialogs.showInfoDialog(
                                this@HomeActivity,
                                title = "Status Locked",
                                message = "You have set Off Duty today. Status changes are locked until tomorrow."
                            )
                            loadUserStatus() // Reset spinner to current status
                        }
                        // All checks passed - allow status change
                        else -> {
                            showStatusConfirmationDialog(newStatus)
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeActivity", "Failed to check status change allowed: ${error.message}")
                    // On error, still allow status change (fail-open)
                    showStatusConfirmationDialog(newStatus)
                }
            })
    }

    private fun showStatusConfirmationDialog(selectedStatus: String) {
        confirmStatusChange(selectedStatus)
    }

        private fun confirmStatusChange(status: String) {
            UiDialogs.showConfirmationDialog(
                this,
                title = "Confirm Status Change",
                message = "Are you sure you want to set your status to '$status'?",
                positiveText = "Yes",
                negativeText = "Cancel",
                onPositive = {
                    updateUserStatus(status)
                    updateTimeLogsStatus(status, createStatusChangeLog = true)
                    if (status == "Off Duty") {
                        handleTimeOutOnOffDuty()
                    }
                },
                onNegative = {
                    loadUserStatus()
                }
            )
        }

    private fun updateUserStatus(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        firestore.collection("users").document(userId)
            .update("status", status)
            .addOnSuccessListener {
                Log.d(
                    "HomeActivity",
                    "Status updated to $status in Firestore"
                )
            }
            .addOnFailureListener {
                Log.e(
                    "HomeActivity",
                    "Failed to update status in Firestore: ${it.message}"
                )
            }
    }

    private fun updateTimeLogsStatus(status: String, createStatusChangeLog: Boolean = false) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        
        // First, get the previous status from the latest log
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var previousStatus = "Unknown"
                    
                    for (child in snapshot.children) {
                        val logTimestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(
                            Calendar.MINUTE,
                            0
                        ); set(
                            Calendar.SECOND,
                            0
                        ); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        if (logTimestamp >= todayStart) {
                            // Get previous status for audit trail
                            previousStatus = child.child("status").getValue(String::class.java) ?: "Unknown"
                            
                            // Only update existing log's status if NOT creating a separate StatusChange log
                            // This prevents duplicate entries in the timeline
                            if (!createStatusChangeLog) {
                                child.ref.child("status").setValue(status)
                                    .addOnSuccessListener {
                                        Log.d(
                                            "HomeActivity",
                                            "Status updated to $status in RealtimeDB for log ${child.key}"
                                        )
                                    }
                                    .addOnFailureListener {
                                        Log.e(
                                            "HomeActivity",
                                            "Failed to update status in RealtimeDB for log ${child.key}: ${it.message}"
                                        )
                                    }
                            }
                        }
                    }
                    
                    // Only create StatusChange log when explicitly requested (manual spinner changes)
                    if (createStatusChangeLog) {
                        val now = Calendar.getInstance()
                        val statusChangeLog = mapOf(
                            "timestamp" to now.timeInMillis,
                            "type" to "StatusChange",
                            "status" to status,
                            "previousStatus" to previousStatus,
                            "firstName" to userFirstName,
                            "email" to userEmail,
                            "userId" to userId
                        )
                        
                        ref.push().setValue(statusChangeLog)
                            .addOnSuccessListener {
                                Log.d("HomeActivity", "Status change logged: $previousStatus → $status")
                            }
                            .addOnFailureListener {
                                Log.e("HomeActivity", "Failed to log status change: ${it.message}")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeActivity", "Failed to update timeLogs status: ${error.message}")
                }
            })
    }

    private fun handleTimeOutOnOffDuty() {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val userEmail = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_EMAIL, null)
        val userFirstName = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_FIRST_NAME, "User")
        val intent = Intent(this, TimeOutActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("email", userEmail ?: "")
            putExtra("firstName", userFirstName ?: "User")
            putExtra("isFlexibleTimeOut", true) // Bypass time restriction for Off Duty status
        }
        startActivity(intent)
    }

    private fun loadUserStatus() {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val firestoreStatus = doc.getString("status") ?: "Off Duty"
                val index = statusOptions.indexOf(firestoreStatus)
                if (index != -1) {
                    isUserChangingStatus = false
                    statusSpinner.setSelection(index, false)
                }
                val realtimeRef =
                    FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                realtimeRef.orderByChild("timestamp").limitToLast(10)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var latestTimeIn: Long? = null;
                            var latestTimeOut: Long? = null
                            var statusFromLogs = firestoreStatus
                            val todayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(
                                Calendar.MINUTE,
                                0
                            ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java)
                                if (timestamp != null && timestamp >= todayStart) {
                                    if (type == "TimeIn") {
                                        if (latestTimeIn == null || timestamp > latestTimeIn) latestTimeIn =
                                            timestamp
                                    } else if (type == "TimeOut") {
                                        if (latestTimeOut == null || timestamp > latestTimeOut) latestTimeOut =
                                            timestamp
                                    }
                                }
                            }
                            statusFromLogs = when {
                                latestTimeOut != null && (latestTimeIn == null || latestTimeOut > latestTimeIn) -> "Off Duty"
                                latestTimeIn != null -> "On Duty"
                                else -> firestoreStatus
                            }
                            if (statusFromLogs != firestoreStatus) {
                                updateUserStatus(statusFromLogs)
                            }
                            val finalIndex = statusOptions.indexOf(statusFromLogs)
                            if (finalIndex != -1) {
                                isUserChangingStatus = false
                                statusSpinner.setSelection(finalIndex)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "HomeActivity",
                                "Failed to load user status from RealtimeDB: ${error.message}"
                            )
                        }
                    })
            }
            .addOnFailureListener {
                Log.e("HomeActivity", "Failed to load user status from Firestore: ${it.message}")
                val index = statusOptions.indexOf("Off Duty")
                if (index != -1) {
                    isUserChangingStatus = false; statusSpinner.setSelection(index)
                }
            }
    }

    private fun updateFilterButtonStates(selectedButton: Button) {
        val filterButtons = listOf(btnUpcoming, btnOngoing, btnEnded, btnCancelled)
        filterButtons.forEach { button ->
            button.isSelected = (button.id == selectedButton.id)
            button.backgroundTintList = ContextCompat.getColorStateList(
                this,
                if (button.isSelected) R.color.primary_deep_blue else R.color.white
            )
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

    override fun onResume() {
        super.onResume()
        loadUserStatus()
        // Always try to load the photo. loadTodayTimeInPhoto checks status internally.
        loadTodayTimeInPhoto()
        updateSidebarProfileImage()
        evaluateAndDisplayAttendanceBadge()
        syncEventTutorialStateFromPrefs()
    }

    private fun loadTodayTimeInPhoto() {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val userStatus = document.getString("status") ?: "Off Duty"
                val profileUrl = document.getString("profilePictureUrl")
                if (userStatus == "On Duty") {
                    FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                        .orderByChild("timestamp").limitToLast(1)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var foundTodaysTimeIn = false
                                for (child in snapshot.children) {
                                    val type = child.child("type").getValue(String::class.java)
                                    val timestamp =
                                        child.child("timestamp").getValue(Long::class.java) ?: 0L
                                    val todayStart = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(
                                        Calendar.MINUTE,
                                        0
                                    ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    if (type == "TimeIn" && timestamp >= todayStart) {
                                        val imageUrl =
                                            child.child("imageUrl").getValue(String::class.java)
                                        if (!imageUrl.isNullOrEmpty()) {
                                            Glide.with(this@HomeActivity).load(imageUrl)
                                                .circleCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true).into(profileImagePlaceholder)
                                            foundTodaysTimeIn = true; return
                                        }
                                    }
                                }
                                if (!foundTodaysTimeIn) {
                                    if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity)
                                        .load(profileUrl).circleCrop().into(profileImagePlaceholder)
                                    else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity)
                                    .load(profileUrl).circleCrop().into(profileImagePlaceholder)
                                else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                            }
                        })
                } else {
                    if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl)
                        .circleCrop().into(profileImagePlaceholder)
                    else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                }
            }
            .addOnFailureListener { profileImagePlaceholder.setImageResource(R.drawable.ic_profile) }
    }

    private fun updateSidebarProfileImage() {
        val sidebarImage =
            navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image)
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val userStatus = document.getString("status") ?: "Off Duty"
                val profileUrl = document.getString("profilePictureUrl")
                if (userStatus == "On Duty") {
                    FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                        .orderByChild("timestamp").limitToLast(1)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var foundTodaysTimeIn = false
                                for (child in snapshot.children) {
                                    val type = child.child("type").getValue(String::class.java)
                                    val timestamp =
                                        child.child("timestamp").getValue(Long::class.java) ?: 0L
                                    val todayStart = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 0); set(
                                        Calendar.MINUTE,
                                        0
                                    ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    if (type == "TimeIn" && timestamp >= todayStart) {
                                        val imageUrl =
                                            child.child("imageUrl").getValue(String::class.java)
                                        if (!imageUrl.isNullOrEmpty()) {
                                            Glide.with(this@HomeActivity).load(imageUrl)
                                                .circleCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true).into(sidebarImage)
                                            foundTodaysTimeIn = true; return
                                        }
                                    }
                                }
                                if (!foundTodaysTimeIn) {
                                    if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity)
                                        .load(profileUrl).circleCrop().into(sidebarImage)
                                    else sidebarImage.setImageResource(R.drawable.ic_profile)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity)
                                    .load(profileUrl).circleCrop().into(sidebarImage)
                                else sidebarImage.setImageResource(R.drawable.ic_profile)
                            }
                        })
                } else {
                    if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl)
                        .circleCrop().into(sidebarImage)
                    else sidebarImage.setImageResource(R.drawable.ic_profile)
                }
            }.addOnFailureListener { sidebarImage.setImageResource(R.drawable.ic_profile) }
    }

    private fun sendEventNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                showNotification(title, message)
            } else {
                // Permission is not granted. Request it.
                pendingNotification = title to message
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // No runtime permission needed for older versions
            showNotification(title, message)
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "event_channel_id"
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(
            this,
            HomeActivity::class.java
        ).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for Timed events" }
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
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val departmentId = userDoc.getString("departmentId")
                    if (!departmentId.isNullOrEmpty()) {
                        FirebaseFirestore.getInstance().collection("departments")
                            .document(departmentId).get()
                            .addOnSuccessListener { deptDoc ->
                                sidebarDetails.text =
                                    "$idNumber • ${deptDoc.getString("abbreviation") ?: "N/A"}"
                            }
                            .addOnFailureListener { sidebarDetails.text = "$idNumber • N/A" }
                    } else {
                        sidebarDetails.text = "$idNumber • N/A"
                    }
                }
        } ?: run { sidebarDetails.text = "$idNumber • N/A" }
        sidebarEmail.text = userEmail ?: ""
        // The problematic line accessing NavigationMenuView has been removed.
        // To hide the scrollbar, add `android:scrollbars="none"` to the
        // <com.google.android.material.navigation.NavigationView> in your home_page.xml file.
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Smooth fade-in effect
                drawerView.alpha = slideOffset
                // Subtle elevation animation
                drawerView.translationX = -30f * (1 - slideOffset)
            }
            override fun onDrawerOpened(drawerView: View) {
                drawerView.alpha = 1f
                drawerView.translationX = 0f
            }
            override fun onDrawerClosed(drawerView: View) {
                drawerView.alpha = 1f
                drawerView.translationX = 0f
            }
            override fun onDrawerStateChanged(newState: Int) {}
        })
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.END)
            Handler(mainLooper).postDelayed({
                when (menuItem.itemId) {
                    R.id.nav_home -> {}
                    R.id.nav_event_log -> startActivity(
                        Intent(
                            this,
                            EventLogActivity::class.java
                        ).putExtra("userId", userId)
                    )

                    R.id.nav_excuse_letter -> startActivity(
                        Intent(
                            this,
                            ExcuseLetterActivity::class.java
                        ).apply {
                            putExtra("userId", userId); putExtra(
                            "email",
                            userEmail
                        ); putExtra("firstName", userFirstName); putExtra(
                            "idNumber",
                            idNumber
                        ); putExtra("department", department)
                        })

                    R.id.nav_excuse_letter_history -> startActivity(
                        Intent(
                            this,
                            ExcuseLetterHistoryActivity::class.java
                        ).putExtra("userId", userId)
                    )

                    R.id.nav_profile -> startActivity(
                        Intent(
                            this,
                            ProfileActivity::class.java
                        ).apply {
                            putExtra("userId", userId); putExtra(
                            "email",
                            userEmail
                        ); putExtra("firstName", userFirstName); putExtra(
                            "idNumber",
                            idNumber
                        ); putExtra("department", department)
                        })

                    R.id.nav_logout -> showLogoutDialog()
                }
            }, 250)
            true
        }
    }

    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            dialog.dismiss()
            getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE).edit().clear()
                .apply()
            getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
        
        dialog.show()
    }

    private fun setupFilterButtons() {
        btnUpcoming.setOnClickListener { updateFilterButtonStates(btnUpcoming); showEventsByStatus("upcoming") }
        btnOngoing.setOnClickListener { updateFilterButtonStates(btnOngoing); showEventsByStatus("ongoing") }
        btnEnded.setOnClickListener { updateFilterButtonStates(btnEnded); showEventsByStatus("ended") }
        btnCancelled.setOnClickListener { updateFilterButtonStates(btnCancelled); showEventsByStatus("cancelled") }
    }

    private fun setupActionButtons() {
        btnTimeIn.setOnClickListener {
            // Guard Clause: Check if Time-In is allowed (outside time window - before 7 AM)
            // Note: TimeIn during break is allowed, status will be set to "On Break"
            if (!TimeSettingsManager.isTimeInAllowed() && !TimeSettingsManager.isInBreak()) {
                val (start, end) = TimeSettingsManager.getTimeWindowString()
                UiDialogs.showInfoDialog(
                    this,
                    title = "Outside Time-In Window",
                    message = "You can only time in between $start and $end."
                )
                return@setOnClickListener
            }

            // Interactive Tutorial Logic
            if (isInteractiveTutorialActive &&
                currentInteractiveTutorialName == TUTORIAL_NAME_ATTENDANCE &&
                expectedInteractiveTutorialAction == ACTION_USER_CLICK_TIME_IN
            ) {
                expectedInteractiveTutorialAction = ACTION_USER_PERFORMED_TIME_IN
                hideOverlay()
            }

            showDefaultTimeInDialog()
        }

        btnTimeOut.setOnClickListener {
            // Guard Clause: Check if it's too early to time out
            if (TimeSettingsManager.isTooEarlyToTimeOut()) {
                val (start, end) = TimeSettingsManager.getTimeWindowString()
                UiDialogs.showInfoDialog(
                    this,
                    title = "Too Early to Time Out",
                    message = "You cannot time out before $end."
                )
                return@setOnClickListener
            }

            UiDialogs.showConfirmationDialog(
                this,
                title = "Time-Out Confirmation",
                message = "Are you sure you want to time out for today?",
                positiveText = "Yes",
                negativeText = "Cancel",
                onPositive = {
                    profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                    navigationView.getHeaderView(0)
                        .findViewById<ImageView>(R.id.sidebar_profile_image)
                        .setImageResource(R.drawable.ic_profile)
                    // setTimedOutToday() // Removed to allow multiple shifts
                    val intent = Intent(this, TimeOutActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("email", userEmail ?: "")
                        putExtra("firstName", userFirstName ?: "User")
                    }
                    startActivity(intent)
                }
            )
        }
    }

    private fun showDefaultTimeInDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Configure dialog
        dialogView.findViewById<ImageView>(R.id.dialog_icon).apply {
            setImageResource(R.drawable.ic_clock)
            setColorFilter(ContextCompat.getColor(this@HomeActivity, R.color.brand_indigo))
        }
        dialogView.findViewById<TextView>(R.id.dialog_title).text = "Time-In Confirmation"
        dialogView.findViewById<TextView>(R.id.dialog_message).text = "Are you ready to time in for today?"
        dialogView.findViewById<Button>(R.id.btn_negative).text = "Cancel"
        dialogView.findViewById<Button>(R.id.btn_positive).apply {
            text = "Time In"
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@HomeActivity, R.color.brand_indigo))
        }
        
        dialogView.findViewById<Button>(R.id.btn_negative).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<Button>(R.id.btn_positive).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, TimeInActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail ?: "")
                putExtra("firstName", userFirstName ?: "User")
            }
            timeInLauncher.launch(intent)
        }
        
        dialog.show()
    }


    private fun setupExcuseLetterRedirect() {
        excuseLetterText.setOnClickListener {
            // TODO: Implement interactive tutorial logic if this is a tutorial step
            startActivity(Intent(this, ExcuseLetterActivity::class.java).apply {
                putExtra(
                    "userId",
                    userId
                ); putExtra("email", userEmail); putExtra(
                "firstName",
                userFirstName
            ); putExtra("idNumber", idNumber); putExtra("department", department)
            })
        }
    }

    private fun loadAndStoreEvents() {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val departmentId: String? = userDoc.getString("departmentId")
                if (departmentId.isNullOrEmpty()) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Missing Department",
                        message = "No department assigned. Cannot load events."
                    )
                    showEventsByStatus("upcoming"); updateFilterButtonStates(btnUpcoming); return@addOnSuccessListener
                }
                firestore.collection("events").whereEqualTo("departmentId", departmentId).get()
                    .addOnSuccessListener { result ->
                        val fullFormatter =
                            SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        val shortDateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())
                        allEvents.clear()

                        // --- Time Window Setup for Notifications ---
                        val now = Calendar.getInstance()
                        val nowMillis = now.timeInMillis
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
                            Calendar.SECOND,
                            0
                        ); set(Calendar.MILLISECOND, 0)
                        }
                        val todayStartMillis = todayStart.timeInMillis
                        val yesterdayStart =
                            (todayStart.clone() as Calendar).apply { add(Calendar.DATE, -1) }
                        val yesterdayStartMillis = yesterdayStart.timeInMillis
                        // --- End Time Window Setup ---

                        val notifiedPrefs =
                            getSharedPreferences("EventNotificationPrefs", Context.MODE_PRIVATE)

                        for (doc in result) {
                            try {
                                val title = doc.getString("eventName") ?: continue
                                val duration = doc.getString("duration") ?: "1:00:00"
                                val date = doc.getTimestamp("date")?.toDate() ?: continue
                                val venue = doc.getString("venue") ?: "N/A"
                                val dateFormatted = fullFormatter.format(date)
                                val statusFromDb = doc.getString("status") ?: "upcoming"
                                val durationParts = duration.split(":")
                                val durationMillis = when (durationParts.size) {
                                    3 -> (durationParts[0].toLongOrNull()
                                        ?: 0) * 3600000L + (durationParts[1].toLongOrNull()
                                        ?: 0) * 60000L + (durationParts[2].toLongOrNull()
                                        ?: 0) * 1000L

                                    2 -> (durationParts[0].toLongOrNull()
                                        ?: 0) * 3600000L + (durationParts[1].toLongOrNull()
                                        ?: 0) * 60000L

                                    1 -> (durationParts[0].toLongOrNull() ?: 0) * 60000L
                                    else -> 3600000L
                                }
                                val eventStartMillis = date.time;
                                val eventEndMillis = eventStartMillis + durationMillis
                                val dynamicStatus = when {
                                    statusFromDb.equals(
                                        "cancelled",
                                        ignoreCase = true
                                    ) -> "cancelled"

                                    statusFromDb.equals("ongoing", ignoreCase = true) -> "ongoing"
                                    statusFromDb.equals("ended", ignoreCase = true) -> "ended"
                                    // If status is "upcoming" or not present, calculate based on time
                                    else -> when {
                                        nowMillis < eventStartMillis -> "upcoming"
                                        nowMillis in eventStartMillis..eventEndMillis -> "ongoing"
                                        else -> "ended"
                                    }
                                }

                                // Notification for upcoming event (within 15 mins)
                                if (dynamicStatus == "upcoming" && eventStartMillis - nowMillis in 1..(15 * 60 * 1000)) {
                                    val notificationKey = "notified_upcoming_${doc.id}"
                                    if (!notifiedPrefs.getBoolean(notificationKey, false)) {
                                        sendEventNotification(
                                            "Event Starting Soon",
                                            "\"$title\" starts in ${((eventStartMillis - nowMillis) / 60000).toInt()} minute(s)."
                                        )
                                        notifiedPrefs.edit().putBoolean(notificationKey, true)
                                            .apply()
                                    }
                                }
                                // Notification for when an event becomes ongoing
                                else if (dynamicStatus == "ongoing") {
                                    val notificationKey = "notified_ongoing_${doc.id}"
                                    if (!notifiedPrefs.getBoolean(notificationKey, false)) {
                                        sendEventNotification(
                                            "Event is Ongoing",
                                            "\"$title\" has started."
                                        )
                                        notifiedPrefs.edit().putBoolean(notificationKey, true)
                                            .apply()
                                    }
                                }
                                // Notification for events that ended yesterday or today
                                else if (dynamicStatus == "ended") {
                                    val notificationKey = "notified_ended_${doc.id}"
                                    val endedWithinWindow =
                                        eventEndMillis >= yesterdayStartMillis && eventEndMillis <= nowMillis
                                    if (endedWithinWindow && !notifiedPrefs.getBoolean(
                                            notificationKey,
                                            false
                                        )
                                    ) {
                                        sendEventNotification(
                                            "Event Ended",
                                            "The event \"$title\" has now ended."
                                        )
                                        notifiedPrefs.edit().putBoolean(notificationKey, true)
                                            .apply()
                                    }
                                }
                                // Notification for events scheduled for yesterday or today that are cancelled
                                else if (dynamicStatus == "cancelled") {
                                    val notificationKey = "notified_cancelled_${doc.id}"
                                    val scheduledForYesterdayOrToday =
                                        eventStartMillis >= yesterdayStartMillis && eventStartMillis < (todayStartMillis + 24 * 60 * 60 * 1000)
                                    if (scheduledForYesterdayOrToday && !notifiedPrefs.getBoolean(
                                            notificationKey,
                                            false
                                        )
                                    ) {
                                        val scheduledDateStr = shortDateFormatter.format(date)
                                        sendEventNotification(
                                            "Event Cancelled",
                                            "The event \"$title\" (for $scheduledDateStr) has been cancelled."
                                        )
                                        notifiedPrefs.edit().putBoolean(notificationKey, true)
                                            .apply()
                                    }
                                }

                                allEvents.add(
                                    EventModel(
                                        title,
                                        duration,
                                        dateFormatted,
                                        dynamicStatus,
                                        rawDate = date,
                                        venue = venue
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "FirestoreEvents",
                                    "Skipping event due to error: ${e.message}",
                                    e
                                )
                            }
                        }

                        // --- FIX: This logic now runs *after* allEvents is populated ---
                        val defaultFilter =
                            if (allEvents.any { it.status.equals("ongoing", ignoreCase = true) }) {
                                "ongoing"
                            } else {
                                "upcoming"
                            }
                        showEventsByStatus(defaultFilter)
                        val buttonToSelect =
                            if (defaultFilter == "ongoing") btnOngoing else btnUpcoming
                        updateFilterButtonStates(buttonToSelect)
                    }
                    .addOnFailureListener {
                        Log.e(
                            "Firestore",
                            "Failed to load events: ${it.message}",
                            it
                        ); UiDialogs.showErrorPopup(
                        this,
                        title = "Load Error",
                        message = "Failed to load events."
                    )
                    }
            }.addOnFailureListener {
                Log.e(
                    "Firestore",
                    "Failed to fetch user document: ${it.message}",
                    it
                ); UiDialogs.showErrorPopup(
                this,
                title = "Load Error",
                message = "Failed to load user info."
            )
            }
    }

    private fun showEventsByStatus(statusFilter: String?) {
        // --- FIX: Simplified Filtering Logic ---
        // The status for each event is now correctly determined in `loadAndStoreEvents`.
        // This function should ONLY filter and sort the existing `allEvents` list.
        // The redundant and buggy re-calculation logic has been removed.
        val filtered = if (statusFilter == null) {
            allEvents
        } else {
            allEvents.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }
        val sorted = filtered.sortedWith(compareBy({ statusOrder(it.status) }, { it.rawDate }))

        (recyclerEvents.adapter as? EventAdapter)?.updateData(sorted)
            ?: run {
                recyclerEvents.adapter = EventAdapter(sorted.toMutableList(), ::onEventItemClicked)
            }


        val readableStatus =
            statusFilter?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?: "Selected"
        noEventsMessage.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        if (sorted.isEmpty()) noEventsMessage.text = "No $readableStatus event/s at the moment."
    }

    private fun onEventItemClicked(event: EventModel): Boolean {
        if (isInteractiveTutorialActive &&
            currentInteractiveTutorialName == TUTORIAL_NAME_EVENT &&
            expectedInteractiveTutorialAction == ACTION_EVENT_SELECT_EVENT
        ) {
            handleEventSelectionForTutorial(event, advanceStep = true)
            return true
        }
        return false
    }

    private fun openCalendarForEventGuide() {
        val calendarButton = findViewById<View>(R.id.btn_calendar)
            ?: run {
                Log.e(
                    TAG,
                    "Calendar button missing while trying to auto-open calendar for tutorial"
                )
                handleTutorialCancellation()
                return
            }

        val deptId = userDepartmentId
        if (deptId.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "No department assigned. Cannot open calendar.",
                Toast.LENGTH_SHORT
            ).show()
            handleTutorialCancellation()
            return
        }

        calendarButton.post {
            calendarButton.performClick()
        }
    }

    private fun openFirstEventForEventGuide() {
        val adapter = recyclerEvents.adapter as? EventAdapter
        val firstEvent = adapter?.getEventAt(0)
        if (firstEvent == null) {
            val sampleEvent = EventModel(
                title = "Sample Event",
                duration = "08:00 - 09:00",
                dateFormatted = "Today",
                status = "Ongoing",
                rawDate = Date(),
                venue = "Main Hall"
            )

            Toast.makeText(
                this,
                "No events found. Using a sample event so the tutorial can continue.",
                Toast.LENGTH_LONG
            ).show()
            handleEventSelectionForTutorial(sampleEvent, advanceStep = true)
            return
        }

        recyclerEvents.smoothScrollToPosition(0)
        Handler(mainLooper).postDelayed({
            handleEventSelectionForTutorial(firstEvent, advanceStep = true)
        }, 350)
    }

    private fun openStoredEventForEventGuide() {
        val storedEvent = EventTutorialState.readSelectedEvent(this)
        if (storedEvent != null) {
            Handler(mainLooper).postDelayed({
                val nextAction = expectedInteractiveTutorialAction
                handleEventSelectionForTutorial(
                    storedEvent,
                    advanceStep = false,
                    nextExpectedAction = nextAction
                )
            }, 150)
        } else {
            val sampleEvent = EventModel(
                title = "Sample Event",
                duration = "08:00 - 09:00",
                dateFormatted = "Today",
                status = "Ongoing",
                rawDate = Date(),
                venue = "Main Hall"
            )

            Toast.makeText(
                this,
                "Reopening a sample event so we can finish the guide.",
                Toast.LENGTH_SHORT
            ).show()
            handleEventSelectionForTutorial(sampleEvent, advanceStep = false, nextExpectedAction = expectedInteractiveTutorialAction)
        }
    }

    private fun closeTutorialCalendarSheetIfOpen() {
        val sheet = tutorialCalendarSheet
        if (sheet != null) {
            try {
                sheet.dismissAllowingStateLoss()
            } catch (ex: IllegalStateException) {
                Log.w(TAG, "Failed to dismiss tutorial calendar sheet cleanly: ${ex.message}")
            }
            tutorialCalendarSheet = null
        }
    }

    private fun safeEventTitle(event: EventModel?): String {
        val candidate = event?.title?.trim()
        return if (candidate.isNullOrEmpty()) "Event" else candidate
    }

    private fun handleEventSelectionForTutorial(
        event: EventModel,
        advanceStep: Boolean,
        nextExpectedAction: String? = ACTION_EVENT_TIME_IN
    ) {
        if (!isInteractiveTutorialActive || currentInteractiveTutorialName != TUTORIAL_NAME_EVENT) {
            launchEventDetail(event, tutorialMode = false)
            return
        }

        if (advanceStep) {
            completeEventGuideStep(2)
        }

        EventTutorialState.rememberSelectedEvent(this, event)
        nextExpectedAction?.let { setEventTutorialExpectedAction(it) }

        if (tutorialOverlay.visibility == View.VISIBLE) {
            tutorialOverlay.visibility = View.GONE
        }

        Handler(mainLooper).postDelayed({
            launchEventDetail(event, tutorialMode = true)
        }, if (advanceStep) 220 else 120)
    }

    private fun launchEventDetail(event: EventModel, tutorialMode: Boolean) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra("eventTitle", safeEventTitle(event))
            putExtra("eventDate", event.dateFormatted)
            putExtra("eventStatus", event.status)
            putExtra("eventVenue", event.venue ?: "N/A")
            if (tutorialMode) {
                putExtra(EXTRA_IS_TUTORIAL_MODE, true)
            }
        }
        startActivity(intent)
    }

    private fun statusOrder(status: String): Int = when (status.lowercase(Locale.ROOT)) {
        "upcoming" -> 0; "ongoing" -> 1; "ended" -> 2; "cancelled" -> 3; else -> 4
    }

    // --- TUTORIAL SYSTEM ---
    private fun handleTutorialCancellation() {
        if (tutorialOverlay.visibility == View.VISIBLE) {
            tutorialOverlay.visibility = View.GONE
        }
        closeTutorialCalendarSheetIfOpen()
        isInteractiveTutorialActive = false
        currentInteractiveTutorialName = null
        setEventTutorialExpectedAction(null)
        markEventTutorialActive(false)

        Log.d(TAG_TUTORIAL_NAV, "Tutorial CANCELED.")
        updateNavHeaderTutorialProgress() // Update nav header to show progress at cancellation
        previousTargetLocationForAnimation = null
        Toast.makeText(this, "Tour cancelled.", Toast.LENGTH_SHORT).show()
        currentTutorialPopupWindow?.dismiss()
        currentTutorialPopupWindow = null
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else if (currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing) {
            currentTutorialPopupWindow?.dismiss()
        } else {
            super.onBackPressed()
        }
    }

    private fun showTutorialDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_tutorial_options)
        dialog.setCancelable(false)

        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            val displayMetrics = resources.displayMetrics
            layoutParams.width = (displayMetrics.widthPixels * 0.90).toInt()
            window.attributes = layoutParams
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        val layoutQuickTour = dialog.findViewById<LinearLayout>(R.id.layout_quick_tour)
        val layoutAttendanceGuide = dialog.findViewById<LinearLayout?>(R.id.layout_attendance_guide)
        val layoutEventGuide = dialog.findViewById<LinearLayout?>(R.id.layout_event_guide)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_tutorial_dialog)

        layoutQuickTour.setOnClickListener {
            previousTargetLocationForAnimation = null
            showQuickTour(resetProgress = true)
            dialog.dismiss()
        }

        layoutAttendanceGuide?.setOnClickListener {
            previousTargetLocationForAnimation = null
            startAttendanceWorkflowTutorial(resetProgress = true)
            dialog.dismiss()
        }

        layoutEventGuide?.setOnClickListener {
            previousTargetLocationForAnimation = null
            startEventWorkflowTutorial(resetProgress = true)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            if (!tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false) ||
                !tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
            ) {
                tutorialPrefs.edit()
                    .putBoolean(KEY_QUICK_TOUR_COMPLETED, true)
                    .putInt(KEY_QUICK_TOUR_CURRENT_STEP, TOTAL_QUICK_TOUR_STEPS)
                    .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, true)
                    .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, TOTAL_ATTENDANCE_TUTORIAL_STEPS)
                    .apply()
                Toast.makeText(
                    this,
                    "Tutorials skipped. You can access them again via the help button.",
                    Toast.LENGTH_LONG
                ).show()
                // Temporarily set active keys to update and then hide one of the bars
                activeTutorialCompletionKey = KEY_QUICK_TOUR_COMPLETED
                activeTutorialStepKey = KEY_QUICK_TOUR_CURRENT_STEP
                activeTutorialTotalSteps = TOTAL_QUICK_TOUR_STEPS
                updateNavHeaderTutorialProgress() // Show 100%
                hideNavHeaderTutorialProgressAfterCompletion()
            }
        }
        dialog.show()
    }

    private fun updateNavHeaderTutorialProgress() {
        if (tutorialProgressOnRightNavHeader == null) {
            Log.e(
                TAG_TUTORIAL_NAV,
                "Nav header tutorial views not initialized. Cannot update progress."
            )
            return
        }

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        val currentStep = tutorialPrefs.getInt(activeTutorialStepKey, 0)
        val isCompleted = tutorialPrefs.getBoolean(activeTutorialCompletionKey, false)
        val totalSteps = activeTutorialTotalSteps

        var percentage = 0
        if (totalSteps > 0) {
            percentage = if (isCompleted) 100 else (currentStep * 100) / totalSteps
        }
        if (percentage > 100) percentage = 100


        tutorialProgressBarOnRight?.max = 100
        tutorialProgressBarOnRight?.progress = percentage
        tutorialPercentageTextOnRight?.text = "$percentage%"

        tutorialTitleTextOnRight?.text = when (activeTutorialStepKey) {
            KEY_QUICK_TOUR_CURRENT_STEP -> "Quick Tour:"
            KEY_ATTENDANCE_GUIDE_CURRENT_STEP -> "Attendance Guide:"
            else -> "Tutorial:"
        }

        if (tutorialProgressOnRightNavHeader?.visibility != View.VISIBLE && !isFinishing && !isDestroyed) {
            tutorialProgressOnRightNavHeader?.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            tutorialProgressOnRightNavHeader?.startAnimation(fadeIn)
        }
        Log.d(
            TAG_TUTORIAL_NAV,
            "Updated nav header progress for $activeTutorialStepKey: $percentage% (Step $currentStep/$totalSteps). Completed: $isCompleted. Visible: true"
        )
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomTutorialDialog(
        message: String,
        targetView: View,
        currentStepToShow: Int,
        totalStepsInTutorial: Int,
        onNext: () -> Unit
    ) {
        tutorialOverlay.visibility = View.VISIBLE

        if (tutorialProgressOnRightNavHeader != null) {
            tutorialTitleTextOnRight?.text = when (activeTutorialStepKey) {
                KEY_QUICK_TOUR_CURRENT_STEP -> "Quick Tour:"
                KEY_ATTENDANCE_GUIDE_CURRENT_STEP -> "Attendance Guide:"
                else -> "Tutorial Progress:"
            }
            // Ensure visibility if it's supposed to be shown (updateNavHeaderTutorialProgress handles actual percentage)
            if (tutorialProgressOnRightNavHeader?.visibility != View.VISIBLE && (activeTutorialStepKey == KEY_QUICK_TOUR_CURRENT_STEP || activeTutorialStepKey == KEY_ATTENDANCE_GUIDE_CURRENT_STEP) && !isFinishing && !isDestroyed) {
                tutorialProgressOnRightNavHeader?.visibility = View.VISIBLE
            }
        } else {
            Log.e(
                TAG_TUTORIAL_NAV,
                "tutorialProgressOnRightNavHeader is NULL in showCustomTutorialDialog."
            )
        }

        currentTutorialPopupWindow?.dismiss()

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)
        val progressTextView = dialogView.findViewById<TextView>(R.id.tutorial_progress_text)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_tutorial_step)

        progressTextView.text =
            "Step $currentStepToShow of $totalStepsInTutorial"; messageTextView.text = message

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density
        val margin = (16 * density).toInt().coerceAtLeast(1)

        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        )

        val measuredWidth = dialogView.measuredWidth.takeIf { it > 0 }
            ?: (screenWidth * 0.8f).toInt()
        val maxDialogWidth = (screenWidth * 0.92f).toInt().coerceAtLeast(margin * 2)
        val dialogWidth = measuredWidth.coerceAtMost(maxDialogWidth)

        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(dialogWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(screenHeight - margin * 2, View.MeasureSpec.AT_MOST)
        )

        val measuredHeight = dialogView.measuredHeight.takeIf { it > 0 }
            ?: (screenHeight * 0.4f).toInt().coerceAtLeast(margin * 4)
        val maxDialogHeight = (screenHeight * 0.7f).toInt().coerceAtLeast(margin * 6)
        val dialogHeight = measuredHeight.coerceAtMost(maxDialogHeight)

        var finalDialogX: Int;
        var finalDialogY: Int
        val currentTargetLocationOnScreen = IntArray(2); targetView.getLocationOnScreen(
            currentTargetLocationOnScreen
        )
        val spaceBelow = screenHeight - (currentTargetLocationOnScreen[1] + targetView.height)
        val spaceAbove = currentTargetLocationOnScreen[1]
        val minXPlacement = margin
        val maxXPlacement = screenWidth - dialogWidth - margin
        finalDialogX =
            (currentTargetLocationOnScreen[0] + targetView.width / 2 - dialogWidth / 2).coerceIn(
                minXPlacement,
                maxXPlacement.coerceAtLeast(minXPlacement)
            )
        if (targetView.visibility == View.VISIBLE && targetView.width > 0 && targetView.height > 0 && targetView.isAttachedToWindow) {
            finalDialogY =
                if (spaceBelow >= dialogHeight + margin / 2) currentTargetLocationOnScreen[1] + targetView.height + margin / 2
                else if (spaceAbove >= dialogHeight + margin / 2) currentTargetLocationOnScreen[1] - dialogHeight - margin / 2
                else (screenHeight - dialogHeight) / 2
        } else {
            finalDialogX = (screenWidth - dialogWidth) / 2
            finalDialogY = (screenHeight - dialogHeight) / 2
        }

        finalDialogY = finalDialogY.coerceIn(margin, screenHeight - dialogHeight - margin)

        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        currentTutorialPopupWindow = popupWindow
        popupWindow.isFocusable = true; popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    this,
                    android.R.color.transparent
                )
            )
        )
        var isProceedingToNextStepOrCompleting = false
        popupWindow.setOnDismissListener { if (!isProceedingToNextStepOrCompleting) handleTutorialCancellation() }
        val animationSet = AnimationSet(true);
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 300
        alphaAnimation.interpolator =
            AnimationUtils.loadInterpolator(this, android.R.anim.decelerate_interpolator)
        animationSet.addAnimation(alphaAnimation)
        var startTranslateX = 0f;
        var startTranslateY = 0f
        if (previousTargetLocationForAnimation != null) {
            val prevDialogEstimateX =
                previousTargetLocationForAnimation!![0] + targetView.width / 2 - dialogWidth / 2
            val prevDialogEstimateY =
                previousTargetLocationForAnimation!![1] + targetView.height + margin / 2
            startTranslateX = (prevDialogEstimateX - finalDialogX).toFloat()
            startTranslateY = (prevDialogEstimateY - finalDialogY).toFloat()
        } else {
            startTranslateX = -dialogWidth.toFloat() * 0.2f
            startTranslateY = (20 * resources.displayMetrics.density)
        }
        val translateAnimation = TranslateAnimation(startTranslateX, 0f, startTranslateY, 0f)
        translateAnimation.duration = 450
        translateAnimation.interpolator =
            AnimationUtils.loadInterpolator(this, android.R.anim.overshoot_interpolator)
        animationSet.addAnimation(translateAnimation)
        dialogView.startAnimation(animationSet)
        popupWindow.showAtLocation(
            targetView.rootView,
            Gravity.NO_GRAVITY,
            finalDialogX,
            finalDialogY
        )
        val currentTargetScreenPos = IntArray(2); targetView.getLocationOnScreen(
            currentTargetScreenPos
        )
        previousTargetLocationForAnimation = currentTargetScreenPos

        nextButton.setOnClickListener {
            isProceedingToNextStepOrCompleting = true
            val fadeOutPopup = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOutPopup.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss()
                    val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)

                    if (!isInteractiveTutorialActive) { // Passive tutorial step advancement
                        // currentStepToShow is the step whose instruction was just shown.
                        // So, we save this as the completed step.
                        tutorialPrefs.edit().putInt(activeTutorialStepKey, currentStepToShow)
                            .apply()
                        updateNavHeaderTutorialProgress() // Reflect this completed step
                    }
                    // For interactive tutorials, progress is saved upon action completion, not here.

                    if (currentStepToShow == totalStepsInTutorial) { // Last instruction of a tutorial
                        if (!isInteractiveTutorialActive) { // Passive tutorial completion
                            tutorialPrefs.edit().putBoolean(activeTutorialCompletionKey, true)
                                .apply()
                            Log.d(
                                TAG,
                                "$activeTutorialCompletionKey marked as completed (passive)."
                            )
                            updateNavHeaderTutorialProgress() // Show 100%
                        } else { // Interactive tutorial - last instruction shown
                            Log.d(
                                TAG,
                                "Last instruction for interactive tutorial $activeTutorialCompletionKey shown. Completion depends on final action."
                            )
                            // Completion and final nav header update will be handled by the action's callback
                        }
                        // Common cleanup for last step of any tutorial type if not handled by action
                        if (tutorialOverlay.visibility == View.VISIBLE && !isInteractiveTutorialActive) {
                            tutorialOverlay.visibility = View.GONE
                        }
                        previousTargetLocationForAnimation = null
                        currentTutorialPopupWindow = null
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOutPopup)
            onNext() // Call the lambda to proceed (e.g., show next step's dialog or launch activity)
        }

        closeButton.setOnClickListener {
            isProceedingToNextStepOrCompleting = false
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss()
                }

                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }
    }

    private fun hideOverlay() {
        if (tutorialOverlay.visibility == View.VISIBLE) tutorialOverlay.visibility = View.GONE
    }

    private fun hideNavHeaderTutorialProgressAfterCompletion() {
        Handler(mainLooper).postDelayed({
            currentInteractiveTutorialName = null
        }, 1500)
    }


    // --- Quick Tour Steps ---
    private fun showQuickTour(resetProgress: Boolean = false) {
        activeTutorialCompletionKey = KEY_QUICK_TOUR_COMPLETED
        activeTutorialStepKey = KEY_QUICK_TOUR_CURRENT_STEP
        activeTutorialTotalSteps = TOTAL_QUICK_TOUR_STEPS

        isInteractiveTutorialActive = false
        currentInteractiveTutorialName = TUTORIAL_NAME_QUICK_TOUR

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        var stepToStartFrom = 0 // Represents the number of steps already completed

        if (resetProgress) {
            tutorialPrefs.edit()
                .putInt(KEY_QUICK_TOUR_CURRENT_STEP, 0)
                .putBoolean(KEY_QUICK_TOUR_COMPLETED, false)
                .apply()
            Log.d(TAG, "Quick Tour progress reset.")
        } else {
            if (!tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false)) {
                stepToStartFrom = tutorialPrefs.getInt(KEY_QUICK_TOUR_CURRENT_STEP, 0)
            } else { // Already completed, but resetProgress was false (e.g. user chose from dialog again)
                Toast.makeText(
                    this,
                    "Quick Tour already completed. Restarting.",
                    Toast.LENGTH_SHORT
                ).show()
                tutorialPrefs.edit()
                    .putInt(KEY_QUICK_TOUR_CURRENT_STEP, 0)
                    .putBoolean(KEY_QUICK_TOUR_COMPLETED, false)
                    .apply()
                // stepToStartFrom remains 0 for a fresh start
            }
        }

        updateNavHeaderTutorialProgress() // Update nav header based on current/reset progress

        val stepToDisplayInstructionFor =
            stepToStartFrom + 1 // We show instruction for step N, meaning N-1 are done.
        Log.d(
            TAG,
            "Starting/Resuming Quick Tour. Will show instruction for step: $stepToDisplayInstructionFor (Saved completed steps: $stepToStartFrom)"
        )

        when (stepToDisplayInstructionFor) {
            1 -> showGreetingCardTourStep()
            2 -> showFilterButtonsTour()
            3 -> showEventListTour()
            4 -> showAttendanceSectionTour()
            else -> { // Handles if already completed (stepToStartFrom = TOTAL_QUICK_TOUR_STEPS) or error
                if (tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false)) {
                    Log.d(
                        TAG,
                        "Quick Tour was already completed. Not restarting automatically unless reset."
                    )
                    Toast.makeText(this, "Quick Tour already completed!", Toast.LENGTH_SHORT).show()
                    hideNavHeaderTutorialProgressAfterCompletion()
                } else {
                    Log.e(
                        TAG,
                        "Quick Tour: Invalid step to display $stepToDisplayInstructionFor. Defaulting to step 1."
                    )
                    showGreetingCardTourStep() // Should ideally not happen if logic is correct
                }
            }
        }
    }

    private fun showGreetingCardTourStep() { // Instruction for step 1
        val greetingCard = findViewById<View>(R.id.greeting_card)
        if (greetingCard == null) {
            Log.e(
                TAG,
                "Greeting card view not found for tutorial"
            ); handleTutorialCancellation(); return
        }
        showCustomTutorialDialog(
            "Welcome! This is your personalized greeting card, showing your name and details.",
            greetingCard,
            1,
            TOTAL_QUICK_TOUR_STEPS
        ) {
            // Passive: On "Next", step 1 is done. showCustomTutorialDialog's listener saves step 1 and updates nav header.
            showFilterButtonsTour() // Show instruction for step 2
        }
    }

    private fun showFilterButtonsTour() { // Instruction for step 2
        val filterButtons = findViewById<View>(R.id.filter_buttons)
        if (filterButtons == null) {
            Log.e(
                TAG,
                "Filter buttons view not found for tutorial"
            ); handleTutorialCancellation(); return
        }
        showCustomTutorialDialog(
            "Here you can filter events: view Upcoming, Ongoing, Ended, or Cancelled events.",
            filterButtons,
            2,
            TOTAL_QUICK_TOUR_STEPS
        ) {
            showEventListTour() // Show instruction for step 3
        }
    }

    private fun showEventListTour() { // Instruction for step 3
        val eventList = findViewById<View>(R.id.recycler_events)
        if (eventList == null) {
            Log.e(
                TAG,
                "Event list view not found for tutorial"
            ); handleTutorialCancellation(); return
        }
        showCustomTutorialDialog(
            "Your selected events will appear here. Scroll to see more if available.",
            eventList,
            3,
            TOTAL_QUICK_TOUR_STEPS
        ) {
            showAttendanceSectionTour() // Show instruction for step 4
        }
    }

    private fun showAttendanceSectionTour() { // Instruction for step 4 (Last step of Quick Tour)
        val attendanceButton = findViewById<View>(R.id.btntime_in)
        if (attendanceButton == null) {
            Log.e(
                TAG,
                "Attendance button view not found for tutorial"
            ); handleTutorialCancellation(); return
        }
        showCustomTutorialDialog(
            "Ready for an event? Tap 'Time-In' here. You can also 'Time-Out' or send an excuse.",
            attendanceButton,
            TOTAL_QUICK_TOUR_STEPS,
            TOTAL_QUICK_TOUR_STEPS
        ) {
            // Passive: On "Next", step 4 is done. showCustomTutorialDialog's listener saves step 4, marks completed, updates nav header.
            Toast.makeText(this@HomeActivity, "Quick Tour Completed! 🎉", Toast.LENGTH_SHORT).show()
            hideNavHeaderTutorialProgressAfterCompletion()
        }
    }

    private fun pulseView(target: View?) {
        target ?: return
        target.animate().cancel()
        target.scaleX = 1f
        target.scaleY = 1f
        target.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(180)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun setEventTutorialExpectedAction(action: String?) {
        if (expectedInteractiveTutorialAction == action) return
        expectedInteractiveTutorialAction = action
        EventTutorialState.setExpectedAction(this, action)
    }

    private fun markEventTutorialActive(isActive: Boolean) {
        EventTutorialState.setActive(this, isActive)
        if (!isActive) {
            expectedInteractiveTutorialAction = null
        }
    }

    private fun syncEventTutorialStateFromPrefs() {
        val eventGuideActive = EventTutorialState.isActive(this)
        if (eventGuideActive) {
            isInteractiveTutorialActive = true
            currentInteractiveTutorialName = TUTORIAL_NAME_EVENT
            activeTutorialCompletionKey = KEY_EVENT_TUTORIAL_COMPLETED
            activeTutorialStepKey = KEY_EVENT_TUTORIAL_CURRENT_STEP
            activeTutorialTotalSteps = TOTAL_EVENT_TUTORIAL_STEPS
            setEventTutorialExpectedAction(EventTutorialState.getExpectedAction(this))
            updateNavHeaderTutorialProgress()

            when (expectedInteractiveTutorialAction) {
                ACTION_EVENT_SELECT_EVENT -> tutorialProgressOnRightNavHeader?.post { showEventGuideSelectEventStep() }
                ACTION_EVENT_TIME_IN -> tutorialProgressOnRightNavHeader?.post { showEventGuideCheckInStep() }
                ACTION_EVENT_TIME_OUT -> tutorialProgressOnRightNavHeader?.post { showEventGuideCheckOutStep() }
            }
        } else if (currentInteractiveTutorialName == TUTORIAL_NAME_EVENT) {
            isInteractiveTutorialActive = false
            currentInteractiveTutorialName = null
            setEventTutorialExpectedAction(null)
            updateNavHeaderTutorialProgress()
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            if (tutorialPrefs.getBoolean(KEY_EVENT_TUTORIAL_COMPLETED, false)) {
                hideNavHeaderTutorialProgressAfterCompletion()
            }
        }
    }

    private fun completeEventGuideStep(step: Int, markCompleted: Boolean = false) {
        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        tutorialPrefs.edit().apply {
            putInt(KEY_EVENT_TUTORIAL_CURRENT_STEP, step)
            if (markCompleted) {
                putBoolean(KEY_EVENT_TUTORIAL_COMPLETED, true)
            }
        }.apply()
        if (markCompleted) {
            markEventTutorialActive(false)
            setEventTutorialExpectedAction(null)
        } else {
            markEventTutorialActive(true)
        }
        updateNavHeaderTutorialProgress()
    }


    // --- Event Check-In Tutorial Steps ---
    private fun startEventWorkflowTutorial(resetProgress: Boolean = false) {
        activeTutorialCompletionKey = KEY_EVENT_TUTORIAL_COMPLETED
        activeTutorialStepKey = KEY_EVENT_TUTORIAL_CURRENT_STEP
        activeTutorialTotalSteps = TOTAL_EVENT_TUTORIAL_STEPS

        isInteractiveTutorialActive = true
        currentInteractiveTutorialName = TUTORIAL_NAME_EVENT
        markEventTutorialActive(true)
        setEventTutorialExpectedAction(null)
        closeTutorialCalendarSheetIfOpen()

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        var stepToStartFrom = 0

        if (resetProgress) {
            tutorialPrefs.edit()
                .putInt(KEY_EVENT_TUTORIAL_CURRENT_STEP, 0)
                .putBoolean(KEY_EVENT_TUTORIAL_COMPLETED, false)
                .apply()
            Log.d(TAG, "Event guide progress reset.")
            EventTutorialState.clearSelectedEvent(this)
            setEventTutorialExpectedAction(null)
        } else {
            if (!tutorialPrefs.getBoolean(KEY_EVENT_TUTORIAL_COMPLETED, false)) {
                stepToStartFrom = tutorialPrefs.getInt(KEY_EVENT_TUTORIAL_CURRENT_STEP, 0)
            } else {
                Toast.makeText(
                    this,
                    "Event check-in guide already completed. Restarting.",
                    Toast.LENGTH_SHORT
                ).show()
                tutorialPrefs.edit()
                    .putInt(KEY_EVENT_TUTORIAL_CURRENT_STEP, 0)
                    .putBoolean(KEY_EVENT_TUTORIAL_COMPLETED, false)
                    .apply()
                EventTutorialState.clearSelectedEvent(this)
                setEventTutorialExpectedAction(null)
            }
        }

        updateNavHeaderTutorialProgress()

        val stepToDisplayInstructionFor = stepToStartFrom + 1
        Log.d(
            TAG,
            "Starting/Resuming Event guide. Will show instruction for step: $stepToDisplayInstructionFor (Saved completed steps: $stepToStartFrom)"
        )

        when (stepToDisplayInstructionFor) {
            1 -> showEventGuideCalendarStep()
            2 -> showEventGuideSelectEventStep()
            3 -> showEventGuideCheckInStep()
            4 -> showEventGuideCheckOutStep()
            else -> {
                if (tutorialPrefs.getBoolean(KEY_EVENT_TUTORIAL_COMPLETED, false)) {
                    Toast.makeText(
                        this,
                        "Event check-in guide already completed!",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideNavHeaderTutorialProgressAfterCompletion()
                } else {
                    Log.e(
                        TAG,
                        "Event guide: Invalid step to display $stepToDisplayInstructionFor. Defaulting to step 1."
                    )
                    showEventGuideCalendarStep()
                }
            }
        }
    }

    private fun showEventGuideCalendarStep() {
        val calendarButton = findViewById<View>(R.id.btn_calendar)
        if (calendarButton == null) {
            Log.e(
                TAG,
                "Calendar button view not found for event guide"
            ); handleTutorialCancellation(); return
        }

        setEventTutorialExpectedAction(null)
        pulseView(calendarButton)

        showCustomTutorialDialog(
            "Step 1/4: Open Calendar. Tap Continue to open the Event Calendar. You'll pick the event from the list or use the QR code on-site.",
            calendarButton,
            1,
            TOTAL_EVENT_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            completeEventGuideStep(1)
            setEventTutorialExpectedAction(ACTION_EVENT_SELECT_EVENT)
            openCalendarForEventGuide()
        }
    }

    private fun showEventGuideSelectEventStep() {
        closeTutorialCalendarSheetIfOpen()
        val eventList = findViewById<View>(R.id.recycler_events)
        if (eventList == null) {
            Log.e(
                TAG,
                "Event list view not found for event guide"
            ); handleTutorialCancellation(); return
        }

        setEventTutorialExpectedAction(ACTION_EVENT_SELECT_EVENT)
        pulseView(eventList)

        showCustomTutorialDialog(
            "Step 2/4: Pick the event. We'll open the first available event so you can see check-in. You can switch to the correct one in the list or by scanning its QR.",
            eventList,
            2,
            TOTAL_EVENT_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            openFirstEventForEventGuide()
        }
    }

    private fun showEventGuideCheckInStep() {
        closeTutorialCalendarSheetIfOpen()
        val anchorView = tutorialProgressOnRightNavHeader ?: findViewById(R.id.recycler_events)
        if (anchorView == null) {
            Log.e(
                TAG,
                "No anchor view found for check-in step"
            ); handleTutorialCancellation(); return
        }

        setEventTutorialExpectedAction(ACTION_EVENT_TIME_IN)
        pulseView(anchorView)

        val storedEvent = EventTutorialState.readSelectedEvent(this)
        val isSampleEvent = storedEvent?.title?.equals("Sample Event", ignoreCase = true) == true
        val message = storedEvent?.let {
            val safeTitle = safeEventTitle(it)
            "Step 3/4: Time-In. I've opened \"$safeTitle\". Scan its QR (or use Manual Code), then take the selfie when prompted. Stay on this screen until you see the success message."
        }
            ?: "Step 3/4: Time-In. Scan the event QR or use Manual Code, then take the selfie when prompted. Stay until you see the success message. Hit Continue to reopen the event if needed."

        showCustomTutorialDialog(
            message,
            anchorView,
            3,
            TOTAL_EVENT_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            if (isSampleEvent) {
                completeEventGuideStep(3)
                setEventTutorialExpectedAction(ACTION_EVENT_TIME_OUT)
                val intent = Intent(this, TimeInEventActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("email", userEmail)
                    putExtra("firstName", userFirstName)
                    putExtra("isTutorialSampleEvent", true)
                }
                startActivity(intent)
            } else {
                openStoredEventForEventGuide()
            }
        }
    }

    private fun showEventGuideCheckOutStep() {
        closeTutorialCalendarSheetIfOpen()
        val anchorView = tutorialProgressOnRightNavHeader ?: findViewById(R.id.recycler_events)
        if (anchorView == null) {
            Log.e(
                TAG,
                "No anchor view found for check-out step"
            ); handleTutorialCancellation(); return
        }

        setEventTutorialExpectedAction(ACTION_EVENT_TIME_OUT)
        pulseView(anchorView)

        val storedEvent = EventTutorialState.readSelectedEvent(this)
        val message = storedEvent?.let {
            val safeTitle = safeEventTitle(it)
            "Step 4/4: Time-Out. When \"$safeTitle\" ends, reopen it and tap Time-Out. Need me to bring it back up? Press Continue."
        }
            ?: "Step 4/4: Time-Out. After the event, reopen its details and tap Time-Out. Want me to reopen it? Press Continue."

        showCustomTutorialDialog(
            message,
            anchorView,
            TOTAL_EVENT_TUTORIAL_STEPS,
            TOTAL_EVENT_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            openStoredEventForEventGuide()
        }
    }


    // --- Attendance Workflow Tutorial Steps ---
    private fun startAttendanceWorkflowTutorial(resetProgress: Boolean = false) {
        activeTutorialCompletionKey = KEY_ATTENDANCE_TUTORIAL_COMPLETED
        activeTutorialStepKey = KEY_ATTENDANCE_GUIDE_CURRENT_STEP
        activeTutorialTotalSteps = TOTAL_ATTENDANCE_TUTORIAL_STEPS

        isInteractiveTutorialActive = true
        currentInteractiveTutorialName = TUTORIAL_NAME_ATTENDANCE

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        var stepToStartFrom = 0 // Actual completed step count

        if (resetProgress) {
            tutorialPrefs.edit()
                .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
                .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
                .apply()
            Log.d(TAG, "Attendance Guide progress reset.")
        } else {
            if (!tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)) {
                stepToStartFrom = tutorialPrefs.getInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
            } else { // Already completed, but resetProgress was false
                Toast.makeText(
                    this,
                    "Attendance Guide already completed. Restarting.",
                    Toast.LENGTH_SHORT
                ).show()
                tutorialPrefs.edit()
                    .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
                    .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
                    .apply()
            }
        }

        updateNavHeaderTutorialProgress() // Update nav header based on current/reset progress

        val stepToDisplayInstructionFor = stepToStartFrom + 1
        Log.d(
            TAG,
            "Starting/Resuming Interactive Attendance Guide. Will show instruction for step: $stepToDisplayInstructionFor (Saved completed steps: $stepToStartFrom)"
        )

        when (stepToDisplayInstructionFor) {
            1 -> showTimeInButtonTutorialStep_New()
            2 -> showEventCalendarTutorialStep()
            3 -> showEventTimeInTutorialStep()
            4 -> showTimeOutButtonTutorialStep()
            5 -> showStatusSpinnerTutorialStep_New()
            6 -> showExcuseLetterButtonTutorialStep_New()
            else -> {
                if (tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)) {
                    Log.d(
                        TAG,
                        "Attendance Guide was already completed. Not restarting automatically unless reset."
                    )
                    Toast.makeText(this, "Attendance Guide already completed!", Toast.LENGTH_SHORT)
                        .show()
                    hideNavHeaderTutorialProgressAfterCompletion()
                } else {
                    Log.e(
                        TAG,
                        "Attendance Guide: Invalid step to display $stepToDisplayInstructionFor. Defaulting to step 1."
                    )
                    showTimeInButtonTutorialStep_New()
                }
            }
        }
    }

    private fun showTimeInButtonTutorialStep_New() { // Instruction for Step 1 of Attendance Guide (Interactive)
        val timeInButton = findViewById<View>(R.id.btntime_in)
        if (timeInButton == null) {
            Log.e(
                TAG,
                "Time-In button view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        expectedInteractiveTutorialAction = ACTION_USER_CLICK_TIME_IN
        Log.d(
            TAG,
            "Attendance Tutorial: Expecting user to click Time-In button (Instruction for step 1)."
        )
        // Nav header progress already updated by startAttendanceWorkflowTutorial to reflect 0/4 (or current saved step)

        showCustomTutorialDialog(
            "This guide focuses on attendance. First, let's Time-In. Please tap the 'Time-In' button now.",
            timeInButton,
            1,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS // currentStepToShow is 1 (instruction for step 1)
        ) {
            // This onNext is for the tutorial dialog's "Next" button.
            // For interactive step, we just hide overlay to allow user to click the actual UI element.
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            Log.d(
                TAG,
                "Attendance Tutorial: Time-In prompt shown. User should click the actual Time-In button."
            )
            // Actual progress update for step 1 happens in timeInActivityTutorialLauncher
        }
    }

    private fun showEventCalendarTutorialStep() { // Instruction for Step 2 – Event Calendar overview
        val calendarButton = findViewById<View>(R.id.btn_calendar)
        if (calendarButton == null) {
            Log.e(
                TAG,
                "Calendar button view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        showCustomTutorialDialog(
            "Need to browse upcoming events? Tap here to open the Event Calendar and see what's scheduled for your department.",
            calendarButton,
            2,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            showEventTimeInTutorialStep()
        }
    }

    private fun showEventTimeInTutorialStep() { // Instruction for Step 3 – Event Time-In guidance
        val eventListView = findViewById<View>(R.id.recycler_events)
        if (eventListView == null) {
            Log.e(
                TAG,
                "Event list view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        showCustomTutorialDialog(
            "After opening the calendar, pick an event to view its details. Use the Time-In option there to log your attendance before it starts.",
            eventListView,
            3,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            showTimeOutButtonTutorialStep()
        }
    }

    fun showTimeOutButtonTutorialStep() { // Instruction for Step 2 - TODO: Make fully interactive
        val timeOutButton = findViewById<View>(R.id.btntime_out)
        if (timeOutButton == null) {
            Log.e(
                TAG,
                "Time-Out button view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        // TODO: Set expectedInteractiveTutorialAction = ACTION_USER_CLICK_TIME_OUT
        Log.d(
            TAG,
            "Attendance Tutorial: Now showing Time-Out step instruction (placeholder for full interactive logic)."
        )
        // Nav header should reflect progress once previous steps are completed.

        showCustomTutorialDialog(
            "After wrapping up your event, tap 'Time-Out' here so the system knows you're done.",
            timeOutButton,
            4,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            Toast.makeText(this, "Click Time-Out button now.", Toast.LENGTH_SHORT).show()

            // TODO: Click Time-Out button now.
            // For now, to simulate progress for testing and move to next instruction:
            // This would be removed once Time-Out is fully interactive.
            // getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE).edit().putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 2).apply()
            // updateNavHeaderTutorialProgress()
            showStatusSpinnerTutorialStep_New() // Continue tutorial flow
        }
    }

    private fun showStatusSpinnerTutorialStep_New() { // Instruction for Step 5 - TODO: Make fully interactive
        val statusSpinnerView = findViewById<View>(R.id.status_spinner)
        if (statusSpinnerView == null) {
            Log.e(
                TAG,
                "Status spinner view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        Log.d(
            TAG,
            "Attendance Tutorial: Now showing Status Spinner step instruction (placeholder for full interactive logic)."
        )
        // TODO: Set expectedInteractiveTutorialAction = ACTION_USER_SELECT_STATUS

        showCustomTutorialDialog(
            "You can manually update your current work status (e.g., 'On Break', 'Off Duty') using this dropdown. (TODO: Make this interactive)",
            statusSpinnerView,
            5,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            Toast.makeText(this, "Select a status now.", Toast.LENGTH_SHORT).show()

            // TODO: Select a status now.
            // For now, to simulate progress for testing:
            // getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE).edit().putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 3).apply()
            // updateNavHeaderTutorialProgress()
            // showExcuseLetterButtonTutorialStep_New() // Manually call next instruction
            showExcuseLetterButtonTutorialStep_New()
        }
    }

    private fun showExcuseLetterButtonTutorialStep_New() { // Instruction for Step 6 - TODO: Make fully interactive
        val excuseLetterButton = findViewById<View>(R.id.excuse_letter_text_button)
        if (excuseLetterButton == null) {
            Log.e(
                TAG,
                "Excuse letter button view not found for tutorial"
            ); handleTutorialCancellation(); return
        }

        Log.d(
            TAG,
            "Attendance Tutorial: Now showing Excuse Letter step instruction (placeholder for full interactive logic)."
        )
        // TODO: Set expectedInteractiveTutorialAction = ACTION_USER_CLICK_EXCUSE_LETTER

        showCustomTutorialDialog(
            "If you're unable to attend or need to submit an excuse, you can do so by tapping here. (TODO: Make this interactive)",
            excuseLetterButton,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS,
            TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            if (tutorialOverlay.visibility == View.VISIBLE) {
                tutorialOverlay.visibility = View.GONE
            }
            // This is the last step's instruction.
            // Actual completion and 100% update would happen after user performs the excuse letter action.
            // For now, to simulate completion for testing:
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            tutorialPrefs.edit()
                .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, TOTAL_ATTENDANCE_TUTORIAL_STEPS)
                .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, true)
                .apply()
            updateNavHeaderTutorialProgress() // Show 100%

            Toast.makeText(
                this@HomeActivity,
                "Attendance Workflow Guide Completed! 🎉 (Partially interactive)",
                Toast.LENGTH_SHORT
            ).show()
            isInteractiveTutorialActive = false
            currentInteractiveTutorialName = null
            expectedInteractiveTutorialAction = null
            hideNavHeaderTutorialProgressAfterCompletion()
        }
    }

    // --- END TUTORIAL SYSTEM ---

    private fun updateAttendanceBadge(status: String) {
        attendanceStatusBadge.visibility = View.VISIBLE
        writeAttendanceStatusToRealtime(status)
        when (status.trim().lowercase(Locale.ROOT)) {
            "on time" -> {
                attendanceStatusBadge.text = "On Time"; attendanceStatusBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.attendance_green)
                ); attendanceStatusBadge.background = null
            }

            "late" -> {
                attendanceStatusBadge.text = "Late"; attendanceStatusBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.attendance_yellow)
                ); attendanceStatusBadge.background = null
            }

            "absent" -> {
                attendanceStatusBadge.text = "Absent"; attendanceStatusBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.attendance_red)
                ); attendanceStatusBadge.background = null
            }

            "has not timed-in" -> {
                attendanceStatusBadge.text = "Has not Timed-In"; attendanceStatusBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.medium_gray)
                ); attendanceStatusBadge.background = null
            }

            "timed-out" -> {
                attendanceStatusBadge.text = "Timed-Out"; attendanceStatusBadge.setTextColor(
                    ContextCompat.getColor(this, R.color.medium_gray)
                ); attendanceStatusBadge.background = null
            }

            else -> attendanceStatusBadge.visibility = View.GONE
        }
    }

    private fun writeAttendanceStatusToRealtime(status: String) {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(10)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestTimeInLogKey: String? = null;
                    var latestTimeInTimestamp: Long = 0
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(
                        Calendar.MINUTE,
                        0
                    ); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)
                        if (type == "TimeIn" && timestamp != null) {
                            if (timestamp > latestTimeInTimestamp) {
                                latestTimeInTimestamp = timestamp; latestTimeInLogKey = child.key
                            }
                        }
                    }
                    latestTimeInLogKey?.let { key ->
                        ref.child(key).child("attendanceBadge").setValue(status)
                            .addOnSuccessListener {
                                Log.d(
                                    TAG,
                                    "Attendance badge '$status' written to log $key"
                                )
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    TAG,
                                    "Failed to write badge to log $key: ${e.message}"
                                )
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "HomeActivity",
                        "Failed to find TimeIn log to write badge: ${error.message}"
                    )
                }
            })
    }

    /*private fun evaluateAndDisplayAttendanceBadge() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        val now = Calendar.getInstance(); val currentTime = now.timeInMillis
        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val cutoff9am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val cutoff10am = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis

        ref.orderByChild("timestamp").startAt(todayStart.toDouble()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var timeInTimestamp: Long? = null; var timeOutTimestamp: Long? = null
                var timeInLogSnapshot: DataSnapshot? = null
                for (child in snapshot.children) {
                    val type = child.child("type").getValue(String::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java)
                    if (type == "TimeIn" && timestamp != null) { if (timeInTimestamp == null || timestamp > timeInTimestamp) { timeInTimestamp = timestamp; timeInLogSnapshot = child } }
                    else if (type == "TimeOut" && timestamp != null) { if (timeOutTimestamp == null || timestamp > timeOutTimestamp) timeOutTimestamp = timestamp }
                }

                if (timeOutTimestamp != null && (timeInTimestamp == null || timeOutTimestamp > timeInTimestamp)) {
                    updateUserStatus("Off Duty")
                    val badgeFromLog = timeInLogSnapshot?.child("attendanceBadge")?.getValue(String::class.java)
                    if (!badgeFromLog.isNullOrEmpty()) updateAttendanceBadge(badgeFromLog) else updateAttendanceBadge("Timed-Out")
                } else if (timeInTimestamp != null) {
                    val existingBadge = timeInLogSnapshot?.child("attendanceBadge")?.getValue(String::class.java)
                    if (!existingBadge.isNullOrEmpty()) updateAttendanceBadge(existingBadge)
                    else {
                        val determinedBadge = when { timeInTimestamp < cutoff9am -> "On Time"; timeInTimestamp < cutoff10am -> "Late"; else -> "Absent" }
                        updateAttendanceBadge(determinedBadge)
                        timeInLogSnapshot?.ref?.child("attendanceBadge")?.setValue(determinedBadge)
                    }
                } else */
// Modified
    private fun evaluateAndDisplayAttendanceBadge() {
        val userId = getSharedPreferences(
            LoginActivity.PREFS_NAME,
            MODE_PRIVATE
        ).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        val now = Calendar.getInstance();
        val currentTime = now.timeInMillis
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val cutoff9am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val cutoff10am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        ref.orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var timeInTimestamp: Long? = null;
                    var timeOutTimestamp: Long? = null
                    var timeInLogSnapshot: DataSnapshot? = null
                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)
                        if (type == "TimeIn" && timestamp != null) {
                            if (timeInTimestamp == null || timestamp > timeInTimestamp) {
                                timeInTimestamp = timestamp; timeInLogSnapshot = child
                            }
                        } else if (type == "TimeOut" && timestamp != null) {
                            if (timeOutTimestamp == null || timestamp > timeOutTimestamp) timeOutTimestamp =
                                timestamp
                        }
                    }
                    if (timeOutTimestamp != null && (timeInTimestamp == null || timeOutTimestamp > timeInTimestamp)) {
                        updateUserStatus("Off Duty")
                        val badgeFromLog = timeInLogSnapshot?.child("attendanceBadge")
                            ?.getValue(String::class.java)
                        if (!badgeFromLog.isNullOrEmpty()) updateAttendanceBadge(badgeFromLog) else updateAttendanceBadge(
                            "Timed-Out"
                        )
                    } else if (timeInTimestamp != null) {
                        val existingBadge = timeInLogSnapshot?.child("attendanceBadge")
                            ?.getValue(String::class.java)
                        if (!existingBadge.isNullOrEmpty()) updateAttendanceBadge(existingBadge)
                        else {
                            // --- DEMO MODE: Force "On Time" ---
                            // The original logic is commented out to ensure any time-in is marked as "On Time".
                            /*
                            val determinedBadge = when { timeInTimestamp < cutoff9am -> "On Time"; timeInTimestamp < cutoff10am -> "Late"; else -> "Absent" }
                            */
                            val determinedBadge = "On Time" // Always set to "On Time" for the demo

                            updateAttendanceBadge(determinedBadge)
                            timeInLogSnapshot?.ref?.child("attendanceBadge")
                                ?.setValue(determinedBadge)
                        }
                        //MODIFIED
                    } else {
                        val todayFormatted =
                            SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
                        FirebaseDatabase.getInstance().getReference("excuseLetters").child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(excuseSnapshot: DataSnapshot) {
                                    var isExcused = false
                                    for (doc in excuseSnapshot.children) {
                                        if (doc.child("date")
                                                .getValue(String::class.java) == todayFormatted && doc.child(
                                                "status"
                                            ).getValue(String::class.java)
                                                .equals("Approved", ignoreCase = true)
                                        ) {
                                            isExcused = true; break
                                        }
                                    }
                                    if (isExcused) updateAttendanceBadge("Absent")
                                    else if (currentTime > cutoff10am) updateAttendanceBadge("Has not Timed-In")
                                    else attendanceStatusBadge.visibility = View.GONE
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    if (currentTime > cutoff10am) updateAttendanceBadge("Has not Timed-In") else attendanceStatusBadge.visibility =
                                        View.GONE
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (Calendar.getInstance().timeInMillis > cutoff10am) updateAttendanceBadge("Has not Timed-In") else attendanceStatusBadge.visibility =
                        View.GONE
                }
            })
    }
}