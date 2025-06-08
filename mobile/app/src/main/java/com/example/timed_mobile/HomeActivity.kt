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
import android.view.ViewGroup
import kotlin.math.abs
import androidx.core.view.isVisible
import com.google.firebase.firestore.DocumentReference

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.os.Handler
import androidx.core.app.NotificationCompat
import com.google.android.material.internal.NavigationMenuView

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val TAG_TUTORIAL_NAV = "HomeActivityTutorialNav"

        const val PREFS_TUTORIAL = "TutorialPrefs"

        const val TOTAL_QUICK_TOUR_STEPS = 4
        const val KEY_QUICK_TOUR_COMPLETED = "quickTourCompleted" // Specific completion key
        const val KEY_QUICK_TOUR_CURRENT_STEP = "quickTourCurrentStep"

        const val EXTRA_IS_TUTORIAL_MODE = "is_tutorial_mode"

        const val TOTAL_ATTENDANCE_TUTORIAL_STEPS = 4
        const val KEY_ATTENDANCE_TUTORIAL_COMPLETED = "attendanceTutorialCompleted"
        const val KEY_ATTENDANCE_GUIDE_CURRENT_STEP = "attendanceGuideCurrentStep"
    }

    private var currentTutorialPopupWindow: PopupWindow? = null
    // These will be set when a specific tutorial flow starts
    private lateinit var activeTutorialCompletionKey: String
    private lateinit var activeTutorialStepKey: String
    private var activeTutorialTotalSteps: Int = 0


    private val timeInActivityTutorialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        if (result.resultCode == RESULT_OK) {
            // TimeInActivity was step 1 of Attendance Guide. It's now completed.
            tutorialPrefs.edit().putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 1).apply() // Mark step 1 as done
            Log.d(TAG, "Attendance tutorial: TimeInActivity part (step 1) completed. Saved step 1.")
            // activeTutorialStepKey should be KEY_ATTENDANCE_GUIDE_CURRENT_STEP
            // activeTutorialCompletionKey should be KEY_ATTENDANCE_TUTORIAL_COMPLETED
            // activeTutorialTotalSteps should be TOTAL_ATTENDANCE_TUTORIAL_STEPS
            // Proceed to the next step of the attendance tutorial
            showTimeOutButtonTutorialStep() // This will show step 2
        } else {
            // User cancelled TimeInActivity part of tutorial.
            handleTutorialCancellation() // This will also clear the overlay
            Toast.makeText(this, "Time-In screen guide was not completed.", Toast.LENGTH_SHORT).show()
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
    private lateinit var profileImagePlaceholder: ImageView

    // --- For Tutorial Progress on the RIGHT of Nav Header ---
    private var tutorialProgressOnRightNavHeader: LinearLayout? = null
    private var tutorialProgressBarOnRight: ProgressBar? = null
    private var tutorialTitleTextOnRight: TextView? = null
    private var tutorialPercentageTextOnRight: TextView? = null
    // ---

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
        drawerLayout = findViewById(R.id.drawer_layout) // Initialize drawerLayout
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        val headerView: View? = navigationView.getHeaderView(0)
        if (headerView != null) {
            tutorialProgressOnRightNavHeader = headerView.findViewById(R.id.tutorial_progress_on_right_nav_header)
            tutorialProgressBarOnRight = headerView.findViewById(R.id.tutorial_bar_on_right_nav_header)
            tutorialTitleTextOnRight = headerView.findViewById(R.id.tutorial_title_text)
            tutorialPercentageTextOnRight = headerView.findViewById(R.id.tutorial_percentage_text)
            Log.d(TAG_TUTORIAL_NAV, "Nav Header Tutorial Views Initialized: Container=${tutorialProgressOnRightNavHeader != null}, Bar=${tutorialProgressBarOnRight != null}, Title=${tutorialTitleTextOnRight != null}, Percentage=${tutorialPercentageTextOnRight != null}")

            if (tutorialProgressOnRightNavHeader != null) {
                tutorialProgressOnRightNavHeader?.isClickable = true
                tutorialProgressOnRightNavHeader?.isFocusable = true
                tutorialProgressOnRightNavHeader?.setOnClickListener {
                    val intent = Intent(this, TutorialProgressActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    Log.d(TAG_TUTORIAL_NAV, "Tutorial progress header clicked, launching TutorialProgressActivity.")
                }
            } else {
                Log.e(TAG_TUTORIAL_NAV, "tutorialProgressOnRightNavHeader (LinearLayout) itself was not found in headerView.")
            }

        } else {
            Log.e(TAG_TUTORIAL_NAV, "Navigation headerView is NULL. Cannot find tutorial progress views.")
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
        noEventsMessage = findViewById(R.id.no_events_message)
        profileImagePlaceholder = findViewById(R.id.profile_image_placeholder)
        statusSpinner = findViewById(R.id.status_spinner)
        val statusAdapter = StatusAdapter(this, statusOptions)
        statusSpinner.adapter = statusAdapter

        val greetingCardView = findViewById<androidx.cardview.widget.CardView>(R.id.greeting_card)
        val filterButtonsLayout = findViewById<LinearLayout>(R.id.filter_buttons)
        val attendancePromptView: TextView? = try { findViewById(R.id.attendance_prompt) } catch (e: Exception) { null }
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

        btnHelp.setOnClickListener {
            showTutorialDialog()
        }

        firestore = FirebaseFirestore.getInstance()
        loadUserStatus()

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
                }
                isUserChangingStatus = false
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        recyclerEvents.layoutManager = LinearLayoutManager(this)

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

        swipeRefreshLayout.setColorSchemeResources(R.color.primary_deep_blue, R.color.accent_coral)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeActivity", "Pull-to-refresh triggered")
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
            loadAndStoreEvents()
            swipeRefreshLayout.isRefreshing = false
        }

        // Post the tutorial check to the drawerLayout's message queue
        // to ensure the activity's window is attached before showing PopupWindows.
        drawerLayout.post {
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            val quickTourDone = tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false)
            val attendanceGuideDone = tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)

            if (!quickTourDone && !attendanceGuideDone) {
                // If it's the very first run or both were somehow reset and not completed
                showTutorialDialog() // This shows a Dialog. PopupWindows from its clicks are fine.
            } else if (!quickTourDone) {
                showQuickTour(resetProgress = false) // Resume Quick Tour
            } else if (!attendanceGuideDone) {
                startAttendanceWorkflowTutorial(resetProgress = false) // Resume Attendance Guide
            }
            // If both are done, no tutorial starts automatically. User can re-initiate via help button.
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
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                        if (type == "TimeIn" && timestamp >= todayStart) {
                            callback(true); return
                        }
                    }
                    callback(false)
                }
                override fun onCancelled(error: DatabaseError) { callback(false) }
            })
    }

    private fun showStatusConfirmationDialog(selectedStatus: String) {
        if (selectedStatus == "On Duty") {
            hasTimedInToday { alreadyTimedIn ->
                if (!alreadyTimedIn) {
                    AlertDialog.Builder(this)
                        .setTitle("Time-In Required")
                        .setMessage("You haven't timed in yet. Do you want to time in now?")
                        .setPositiveButton("Yes") { _, _ ->
                            val intent = Intent(this, TimeInActivity::class.java).apply {
                                putExtra("userId", userId); putExtra("email", userEmail ?: ""); putExtra("firstName", userFirstName ?: "User")
                            }
                            timeInLauncher.launch(intent)
                        }
                        .setNegativeButton("Cancel") { _, _ -> loadUserStatus() }
                        .show()
                } else {
                    confirmStatusChange(selectedStatus)
                }
            }
        } else {
            confirmStatusChange(selectedStatus)
        }
    }

    private fun confirmStatusChange(status: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Status Change")
            .setMessage("Are you sure you want to set your status to '$status'?")
            .setPositiveButton("Yes") { _, _ ->
                updateUserStatus(status)
                updateTimeLogsStatus(status)
                if (status == "Off Duty") {
                    handleTimeOutOnOffDuty()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                loadUserStatus()
            }
            .show()
    }

    private fun updateUserStatus(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        firestore.collection("users").document(userId)
            .update("status", status)
            .addOnSuccessListener { Log.d("HomeActivity", "Status updated to $status in Firestore") }
            .addOnFailureListener { Log.e("HomeActivity", "Failed to update status in Firestore: ${it.message}") }
    }

    private fun updateTimeLogsStatus(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val logTimestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        if (logTimestamp >= todayStart) {
                            child.ref.child("status").setValue(status)
                                .addOnSuccessListener { Log.d("HomeActivity", "Status updated to $status in RealtimeDB for log ${child.key}") }
                                .addOnFailureListener { Log.e("HomeActivity", "Failed to update status in RealtimeDB for log ${child.key}: ${it.message}") }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) { Log.e("HomeActivity", "Failed to update timeLogs status: ${error.message}") }
            })
    }

    private fun handleTimeOutOnOffDuty() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val userEmail = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_EMAIL, null)
        val userFirstName = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_FIRST_NAME, "User")
        val intent = Intent(this, TimeOutActivity::class.java).apply {
            putExtra("userId", userId); putExtra("email", userEmail ?: ""); putExtra("firstName", userFirstName ?: "User")
        }
        startActivity(intent)
    }

    private fun loadUserStatus() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val firestoreStatus = doc.getString("status") ?: "Off Duty"
                val index = statusOptions.indexOf(firestoreStatus)
                if (index != -1) {
                    isUserChangingStatus = false
                    statusSpinner.setSelection(index, false)
                }
                val realtimeRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                realtimeRef.orderByChild("timestamp").limitToLast(10)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var latestTimeIn: Long? = null; var latestTimeOut: Long? = null
                            var statusFromLogs = firestoreStatus
                            val todayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java)
                                if (timestamp != null && timestamp >= todayStart) {
                                    if (type == "TimeIn") { if (latestTimeIn == null || timestamp > latestTimeIn) latestTimeIn = timestamp }
                                    else if (type == "TimeOut") { if (latestTimeOut == null || timestamp > latestTimeOut) latestTimeOut = timestamp }
                                }
                            }
                            statusFromLogs = when {
                                latestTimeOut != null && (latestTimeIn == null || latestTimeOut > latestTimeIn) -> "Off Duty"
                                latestTimeIn != null -> "On Duty"
                                else -> firestoreStatus
                            }
                            if (statusFromLogs != firestoreStatus) { updateUserStatus(statusFromLogs) }
                            val finalIndex = statusOptions.indexOf(statusFromLogs)
                            if (finalIndex != -1) {
                                isUserChangingStatus = false
                                statusSpinner.setSelection(finalIndex)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) { Log.e("HomeActivity", "Failed to load user status from RealtimeDB: ${error.message}") }
                    })
            }
            .addOnFailureListener {
                Log.e("HomeActivity", "Failed to load user status from Firestore: ${it.message}")
                val index = statusOptions.indexOf("Off Duty")
                if (index != -1) { isUserChangingStatus = false; statusSpinner.setSelection(index) }
            }
    }

    private fun updateFilterButtonStates(selectedButton: Button) {
        val filterButtons = listOf(btnUpcoming, btnOngoing, btnEnded, btnCancelled)
        filterButtons.forEach { button ->
            button.isSelected = (button.id == selectedButton.id)
            button.backgroundTintList = ContextCompat.getColorStateList(this, if (button.isSelected) R.color.primary_deep_blue else R.color.white)
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
        if (hasTimedOutToday()) {
            profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
            navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image).setImageResource(R.drawable.ic_profile)
        } else {
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
        }
        evaluateAndDisplayAttendanceBadge()
    }

    private fun loadTodayTimeInPhoto() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")
            if (userStatus == "On Duty") {
                FirebaseDatabase.getInstance().getReference("timeLogs").child(userId).orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var foundTodaysTimeIn = false
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                                val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity).load(imageUrl).circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(profileImagePlaceholder)
                                        foundTodaysTimeIn = true; return
                                    }
                                }
                            }
                            if (!foundTodaysTimeIn) {
                                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                                else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                            else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                        }
                    })
            } else {
                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(profileImagePlaceholder)
                else profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
            }
        }.addOnFailureListener { profileImagePlaceholder.setImageResource(R.drawable.ic_profile) }
    }

    private fun updateSidebarProfileImage() {
        val sidebarImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image)
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")
            if (userStatus == "On Duty") {
                FirebaseDatabase.getInstance().getReference("timeLogs").child(userId).orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var foundTodaysTimeIn = false
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                                val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity).load(imageUrl).circleCrop().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(sidebarImage)
                                        foundTodaysTimeIn = true; return
                                    }
                                }
                            }
                            if (!foundTodaysTimeIn) {
                                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                                else sidebarImage.setImageResource(R.drawable.ic_profile)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                            else sidebarImage.setImageResource(R.drawable.ic_profile)
                        }
                    })
            } else {
                if (!profileUrl.isNullOrEmpty()) Glide.with(this@HomeActivity).load(profileUrl).circleCrop().into(sidebarImage)
                else sidebarImage.setImageResource(R.drawable.ic_profile)
            }
        }.addOnFailureListener { sidebarImage.setImageResource(R.drawable.ic_profile) }
    }

    private fun sendEventNotification(title: String, message: String) {
        val channelId = "event_channel_id"
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, HomeActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Event Notifications", NotificationManager.IMPORTANCE_HIGH).apply { description = "Notifications for Timed events" }
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
            FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                val departmentId = userDoc.getString("departmentId")
                if (!departmentId.isNullOrEmpty()) {
                    FirebaseFirestore.getInstance().collection("departments").document(departmentId).get()
                        .addOnSuccessListener { deptDoc -> sidebarDetails.text = "$idNumber • ${deptDoc.getString("abbreviation") ?: "N/A"}" }
                        .addOnFailureListener { sidebarDetails.text = "$idNumber • N/A" }
                } else { sidebarDetails.text = "$idNumber • N/A" }
            }
        } ?: run { sidebarDetails.text = "$idNumber • N/A" }
        sidebarEmail.text = userEmail ?: ""
        (navigationView.getChildAt(0) as? NavigationMenuView)?.isVerticalScrollBarEnabled = false
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.END)
            Handler(mainLooper).postDelayed({
                when (menuItem.itemId) {
                    R.id.nav_home -> {}
                    R.id.nav_event_log -> startActivity(Intent(this, EventLogActivity::class.java).putExtra("userId", userId))
                    R.id.nav_excuse_letter -> startActivity(Intent(this, ExcuseLetterActivity::class.java).apply { putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department) })
                    R.id.nav_excuse_letter_history -> startActivity(Intent(this, ExcuseLetterHistoryActivity::class.java).putExtra("userId", userId))
                    R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java).apply { putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department) })
                    R.id.nav_logout -> showLogoutDialog()
                }
            }, 250)
            true
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this).setTitle("Log Out").setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE).edit().clear().apply() // Clear tutorial progress on logout
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun setupFilterButtons() {
        btnUpcoming.setOnClickListener { updateFilterButtonStates(btnUpcoming); showEventsByStatus("upcoming") }
        btnOngoing.setOnClickListener { updateFilterButtonStates(btnOngoing); showEventsByStatus("ongoing") }
        btnEnded.setOnClickListener { updateFilterButtonStates(btnEnded); showEventsByStatus("ended") }
        btnCancelled.setOnClickListener { updateFilterButtonStates(btnCancelled); showEventsByStatus("cancelled") }
        updateFilterButtonStates(btnUpcoming)
    }

    private fun setupActionButtons() {
        btnTimeIn.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Time - In Confirmation").setMessage("Are you ready to time in for today?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(this, TimeInActivity::class.java).apply { putExtra("userId", userId); putExtra("email", userEmail ?: ""); putExtra("firstName", userFirstName ?: "User") }
                    timeInLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null).show()
        }
        btnTimeOut.setOnClickListener {
            hasTimedInToday { alreadyTimedIn ->
                if (!alreadyTimedIn) {
                    AlertDialog.Builder(this).setTitle("Cannot Time-Out").setMessage("You haven't timed in yet. Please time in first before timing out.").setPositiveButton("OK", null).show()
                } else {
                    AlertDialog.Builder(this).setTitle("Time - Out Confirmation").setMessage("Are you sure you want to time out for today?")
                        .setPositiveButton("Yes") { _, _ ->
                            profileImagePlaceholder.setImageResource(R.drawable.ic_profile)
                            navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image).setImageResource(R.drawable.ic_profile)
                            setTimedOutToday()
                            val intent = Intent(this, TimeOutActivity::class.java).apply { putExtra("userId", userId); putExtra("email", userEmail ?: ""); putExtra("firstName", userFirstName ?: "User") }
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null).show()
                }
            }
        }
    }

    private fun setupExcuseLetterRedirect() {
        excuseLetterText.setOnClickListener {
            startActivity(Intent(this, ExcuseLetterActivity::class.java).apply { putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName); putExtra("idNumber", idNumber); putExtra("department", department) })
        }
    }

    private fun loadAndStoreEvents() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val departmentId: String? = userDoc.getString("departmentId")
            if (departmentId.isNullOrEmpty()) {
                Toast.makeText(this, "No department assigned. Cannot load events.", Toast.LENGTH_SHORT).show()
                showEventsByStatus("upcoming"); updateFilterButtonStates(btnUpcoming); return@addOnSuccessListener
            }
            firestore.collection("events").whereEqualTo("departmentId", departmentId).get()
                .addOnSuccessListener { result ->
                    val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    allEvents.clear(); val nowMillis = System.currentTimeMillis()
                    for (doc in result) {
                        try {
                            val title = doc.getString("eventName") ?: continue
                            val duration = doc.getString("duration") ?: "1:00:00"
                            val date = doc.getTimestamp("date")?.toDate() ?: continue
                            val dateFormatted = formatter.format(date)
                            val statusFromDb = doc.getString("status") ?: "upcoming"
                            val durationParts = duration.split(":")
                            val durationMillis = when (durationParts.size) {
                                3 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L + (durationParts[2].toLongOrNull() ?: 0)*1000L
                                2 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L
                                1 -> (durationParts[0].toLongOrNull() ?: 0)*60000L
                                else -> 3600000L
                            }
                            val eventStartMillis = date.time; val eventEndMillis = eventStartMillis + durationMillis
                            val dynamicStatus = when {
                                statusFromDb.equals("cancelled", ignoreCase = true) -> "cancelled"
                                nowMillis < eventStartMillis -> "upcoming"
                                nowMillis in eventStartMillis..eventEndMillis -> "ongoing"
                                else -> "ended"
                            }
                            if (dynamicStatus == "upcoming" && eventStartMillis - nowMillis in 1..(15 * 60 * 1000)) {
                                sendEventNotification("Event Starting Soon", "\"$title\" starts in ${((eventStartMillis - nowMillis) / 60000).toInt()} minute(s).")
                            }
                            allEvents.add(EventModel(title, duration, dateFormatted, dynamicStatus, rawDate = date))
                        } catch (e: Exception) { Log.e("FirestoreEvents", "Skipping event due to error: ${e.message}", e) }
                    }
                    showEventsByStatus("upcoming"); updateFilterButtonStates(btnUpcoming)
                }
                .addOnFailureListener { Log.e("Firestore", "Failed to load events: ${it.message}", it); Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show() }
        }.addOnFailureListener { Log.e("Firestore", "Failed to fetch user document: ${it.message}", it); Toast.makeText(this, "Failed to load user info.", Toast.LENGTH_SHORT).show() }
    }

    private fun showEventsByStatus(statusFilter: String?) {
        val currentDate = Date()
        val eventsWithDynamicStatus = allEvents.map { event ->
            val eventDate = event.rawDate; val durationParts = event.duration.split(":")
            val durationMillis = when (durationParts.size) {
                3 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L + (durationParts[2].toLongOrNull() ?: 0)*1000L
                2 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L
                1 -> (durationParts[0].toLongOrNull() ?: 0)*60000L; else -> 3600000L
            }
            val eventEndDate = eventDate?.time?.plus(durationMillis)
            val dynamicStatus = if (event.status.equals("cancelled", ignoreCase = true)) "cancelled" else {
                when {
                    eventDate == null || eventEndDate == null -> "unknown"
                    currentDate.time < eventDate.time -> "upcoming"
                    currentDate.time in eventDate.time until eventEndDate -> "ongoing"
                    else -> "ended"
                }
            }
            event.copy(status = dynamicStatus)
        }
        val filtered = if (statusFilter == null) eventsWithDynamicStatus else eventsWithDynamicStatus.filter { it.status.equals(statusFilter, ignoreCase = true) }
        val sorted = filtered.sortedWith(compareBy({ statusOrder(it.status) }, { it.rawDate }))
        recyclerEvents.adapter = EventAdapter(sorted)
        val readableStatus = statusFilter?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Selected"
        noEventsMessage.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        if (sorted.isEmpty()) noEventsMessage.text = "No $readableStatus event/s at the moment."
    }

    private fun statusOrder(status: String): Int = when (status.lowercase(Locale.ROOT)) {
        "upcoming" -> 0; "ongoing" -> 1; "ended" -> 2; "cancelled" -> 3; else -> 4
    }

    // --- TUTORIAL SYSTEM ---
    private fun handleTutorialCancellation() {
        if (tutorialOverlay.visibility == View.VISIBLE) {
            tutorialOverlay.visibility = View.GONE
        }
        Log.d(TAG_TUTORIAL_NAV, "Tutorial CANCELED, nav header progress remains visible showing current step.")
        previousTargetLocationForAnimation = null
        Toast.makeText(this, "Tour cancelled.", Toast.LENGTH_SHORT).show()
        currentTutorialPopupWindow?.dismiss()
        currentTutorialPopupWindow = null
        // Do not reset progress here, user might want to resume later.
        // The nav header will show the progress of the step they cancelled on.
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
        dialog.setCancelable(false) // User must choose an option or explicitly skip

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
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_tutorial_dialog)

        layoutQuickTour.setOnClickListener {
            previousTargetLocationForAnimation = null
            showQuickTour(resetProgress = true) // Reset and start Quick Tour
            dialog.dismiss()
        }

        layoutAttendanceGuide?.setOnClickListener {
            previousTargetLocationForAnimation = null
            startAttendanceWorkflowTutorial(resetProgress = true) // Reset and start Attendance Guide
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            // Mark both as "completed" to prevent auto-start, but steps remain 0.
            // User can still access them via help button, which will reset progress.
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            if (!tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false) ||
                !tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)) {
                tutorialPrefs.edit()
                    .putBoolean(KEY_QUICK_TOUR_COMPLETED, true) // Mark as "skipped" by setting completed
                    .putInt(KEY_QUICK_TOUR_CURRENT_STEP, TOTAL_QUICK_TOUR_STEPS) // Show as 100% if skipped
                    .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, true)
                    .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, TOTAL_ATTENDANCE_TUTORIAL_STEPS)
                    .apply()
                Toast.makeText(this, "Tutorials skipped. You can access them again via the help button.", Toast.LENGTH_LONG).show()
                hideNavHeaderTutorialProgressAfterCompletion() // Hide or update nav header
            }
        }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomTutorialDialog(message: String, targetView: View, currentStepToShow: Int, totalStepsInTutorial: Int, onNext: () -> Unit) {
        tutorialOverlay.visibility = View.VISIBLE

        if (tutorialProgressOnRightNavHeader != null) {
            val percentage = if (totalStepsInTutorial > 0) (currentStepToShow * 100) / totalStepsInTutorial else 0
            tutorialProgressBarOnRight?.max = 100
            tutorialProgressBarOnRight?.progress = percentage
            tutorialPercentageTextOnRight?.text = "$percentage%"
            tutorialTitleTextOnRight?.text = when (activeTutorialStepKey) {
                KEY_QUICK_TOUR_CURRENT_STEP -> "Quick Tour:"
                KEY_ATTENDANCE_GUIDE_CURRENT_STEP -> "Attendance Guide:"
                else -> "Tutorial Progress:"
            }

            if (tutorialProgressOnRightNavHeader?.visibility != View.VISIBLE) {
                tutorialProgressOnRightNavHeader?.visibility = View.VISIBLE
                val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                tutorialProgressOnRightNavHeader?.startAnimation(fadeIn)
            }
            Log.d(TAG_TUTORIAL_NAV, "Showing nav header progress for $activeTutorialStepKey: $percentage% (Step $currentStepToShow/$totalStepsInTutorial).")
        } else {
            Log.e(TAG_TUTORIAL_NAV, "tutorialProgressOnRightNavHeader is NULL in showCustomTutorialDialog.")
        }

        currentTutorialPopupWindow?.dismiss()

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)
        val progressTextView = dialogView.findViewById<TextView>(R.id.tutorial_progress_text)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_tutorial_step)

        progressTextView.text = "Step $currentStepToShow of $totalStepsInTutorial"; messageTextView.text = message
        dialogView.measure(View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST))

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val margin = (16 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val measuredWidth = dialogView.measuredWidth
        val initialDialogWidth = if (measuredWidth > 0) measuredWidth else (screenWidth * 0.8).toInt()
        val upperWidthBound = (screenWidth - 2 * margin).coerceAtLeast(margin * 2)
        val lowerWidthBound = (margin * 2).coerceAtLeast(1)
        val dialogWidth = initialDialogWidth.coerceIn(lowerWidthBound, upperWidthBound)
        val dialogHeight = dialogView.measuredHeight.takeIf { it > 0 } ?: ViewGroup.LayoutParams.WRAP_CONTENT
        var finalDialogX: Int; var finalDialogY: Int
        val currentTargetLocationOnScreen = IntArray(2); targetView.getLocationOnScreen(currentTargetLocationOnScreen)
        val spaceBelow = screenHeight - (currentTargetLocationOnScreen[1] + targetView.height)
        val spaceAbove = currentTargetLocationOnScreen[1]
        val minXPlacement = margin
        val maxXPlacement = screenWidth - dialogWidth - margin
        finalDialogX = (currentTargetLocationOnScreen[0] + targetView.width / 2 - dialogWidth / 2).coerceIn(minXPlacement, maxXPlacement.coerceAtLeast(minXPlacement))
        if (targetView.visibility == View.VISIBLE && targetView.width > 0 && targetView.height > 0 && targetView.isAttachedToWindow) {
            finalDialogY = if (spaceBelow >= dialogHeight + margin / 2) currentTargetLocationOnScreen[1] + targetView.height + margin / 2
            else if (spaceAbove >= dialogHeight + margin / 2) currentTargetLocationOnScreen[1] - dialogHeight - margin / 2
            else (screenHeight - dialogHeight) / 2
        } else {
            finalDialogX = (screenWidth - dialogWidth) / 2
            finalDialogY = (screenHeight - dialogHeight) / 2
        }
        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        currentTutorialPopupWindow = popupWindow
        popupWindow.isFocusable = true; popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)))
        var isProceedingToNextStepOrCompleting = false
        popupWindow.setOnDismissListener { if (!isProceedingToNextStepOrCompleting) handleTutorialCancellation() }
        val animationSet = AnimationSet(true); val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 300
        alphaAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.decelerate_interpolator)
        animationSet.addAnimation(alphaAnimation)
        var startTranslateX = 0f; var startTranslateY = 0f
        if (previousTargetLocationForAnimation != null) {
            val prevDialogEstimateX = previousTargetLocationForAnimation!![0] + targetView.width / 2 - dialogWidth / 2
            val prevDialogEstimateY = previousTargetLocationForAnimation!![1] + targetView.height + margin / 2
            startTranslateX = (prevDialogEstimateX - finalDialogX).toFloat()
            startTranslateY = (prevDialogEstimateY - finalDialogY).toFloat()
        } else {
            startTranslateX = -dialogWidth.toFloat() * 0.2f
            startTranslateY = (20 * resources.displayMetrics.density)
        }
        val translateAnimation = TranslateAnimation(startTranslateX, 0f, startTranslateY, 0f)
        translateAnimation.duration = 450
        translateAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.overshoot_interpolator)
        animationSet.addAnimation(translateAnimation)
        dialogView.startAnimation(animationSet)
        popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, finalDialogX, finalDialogY)
        val currentTargetScreenPos = IntArray(2); targetView.getLocationOnScreen(currentTargetScreenPos)
        previousTargetLocationForAnimation = currentTargetScreenPos

        nextButton.setOnClickListener {
            isProceedingToNextStepOrCompleting = true
            val fadeOutPopup = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOutPopup.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss()
                    // Save the step that was just shown/completed
                    val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
                    tutorialPrefs.edit().putInt(activeTutorialStepKey, currentStepToShow).apply()
                    Log.d(TAG, "Saved $activeTutorialStepKey: $currentStepToShow")

                    if (currentStepToShow == totalStepsInTutorial) { // Tutorial completed
                        tutorialOverlay.visibility = View.GONE
                        Log.d(TAG_TUTORIAL_NAV, "$activeTutorialCompletionKey COMPLETED, nav header progress remains visible at 100%.")
                        previousTargetLocationForAnimation = null
                        currentTutorialPopupWindow = null
                        tutorialPrefs.edit().putBoolean(activeTutorialCompletionKey, true).apply()
                        Log.d(TAG, "$activeTutorialCompletionKey marked as completed.")
                        // Optionally hide or update nav header after a delay or specific message
                        // hideNavHeaderTutorialProgressAfterCompletion()
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOutPopup)
            onNext() // This will call the function for the next step or handle completion
        }

        closeButton.setOnClickListener {
            isProceedingToNextStepOrCompleting = false
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) { popupWindow.dismiss() } // Dismissal triggers handleTutorialCancellation
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }
    }

    private fun hideOverlay() { if (tutorialOverlay.visibility == View.VISIBLE) tutorialOverlay.visibility = View.GONE }

    private fun hideNavHeaderTutorialProgressAfterCompletion() {
        // Example: Fade out the nav header progress after a short delay
        Handler(mainLooper).postDelayed({
            if (tutorialProgressOnRightNavHeader?.visibility == View.VISIBLE) {
                val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(p0: Animation?) {}
                    override fun onAnimationEnd(p0: Animation?) {
                        tutorialProgressOnRightNavHeader?.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(p0: Animation?) {}
                })
                tutorialProgressOnRightNavHeader?.startAnimation(fadeOut)
            }
        }, 1500) // 1.5 second delay
    }


    // --- Quick Tour Steps ---
    private fun showQuickTour(resetProgress: Boolean = false) {
        activeTutorialCompletionKey = KEY_QUICK_TOUR_COMPLETED
        activeTutorialStepKey = KEY_QUICK_TOUR_CURRENT_STEP
        activeTutorialTotalSteps = TOTAL_QUICK_TOUR_STEPS

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        var stepToDisplay = 1

        if (resetProgress) {
            tutorialPrefs.edit()
                .putInt(KEY_QUICK_TOUR_CURRENT_STEP, 0)
                .putBoolean(KEY_QUICK_TOUR_COMPLETED, false)
                .apply()
            Log.d(TAG, "Quick Tour progress reset.")
        } else {
            if (!tutorialPrefs.getBoolean(KEY_QUICK_TOUR_COMPLETED, false)) {
                stepToDisplay = tutorialPrefs.getInt(KEY_QUICK_TOUR_CURRENT_STEP, 0) + 1
                if (stepToDisplay > TOTAL_QUICK_TOUR_STEPS) {
                    Log.w(TAG, "Quick Tour: stepToDisplay ($stepToDisplay) > total. Likely already completed or error. Resetting to 1.")
                    stepToDisplay = 1
                    tutorialPrefs.edit().putInt(KEY_QUICK_TOUR_CURRENT_STEP, 0).apply()
                }
            } else {
                Toast.makeText(this, "Quick Tour already completed. Restarting.", Toast.LENGTH_SHORT).show()
                tutorialPrefs.edit()
                    .putInt(KEY_QUICK_TOUR_CURRENT_STEP, 0)
                    .putBoolean(KEY_QUICK_TOUR_COMPLETED, false)
                    .apply()
                // stepToDisplay remains 1
            }
        }
        Log.d(TAG, "Starting/Resuming Quick Tour. Step to display: $stepToDisplay")

        when (stepToDisplay) {
            1 -> showGreetingCardTourStep()
            2 -> showFilterButtonsTourStep()
            3 -> showEventListTourStep()
            4 -> showAttendanceSectionTourStep()
            else -> {
                Log.e(TAG, "Quick Tour: Invalid step $stepToDisplay. Defaulting to step 1.")
                showGreetingCardTourStep()
            }
        }
    }

    private fun showGreetingCardTourStep() {
        val greetingCard = findViewById<View>(R.id.greeting_card)
        if (greetingCard == null) { Log.e(TAG, "Greeting card view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("Welcome! This is your personalized greeting card, showing your name and details.", greetingCard, 1, TOTAL_QUICK_TOUR_STEPS) {
            showFilterButtonsTourStep()
        }
    }
    private fun showFilterButtonsTourStep() {
        val filterButtons = findViewById<View>(R.id.filter_buttons)
        if (filterButtons == null) { Log.e(TAG, "Filter buttons view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("Here you can filter events: view Upcoming, Ongoing, Ended, or Cancelled events.", filterButtons, 2, TOTAL_QUICK_TOUR_STEPS) {
            showEventListTourStep()
        }
    }
    private fun showEventListTourStep() {
        val eventList = findViewById<View>(R.id.recycler_events)
        if (eventList == null) { Log.e(TAG, "Event list view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("Your selected events will appear here. Scroll to see more if available.", eventList, 3, TOTAL_QUICK_TOUR_STEPS) {
            showAttendanceSectionTourStep()
        }
    }
    private fun showAttendanceSectionTourStep() {
        val attendanceButton = findViewById<View>(R.id.btntime_in)
        if (attendanceButton == null) { Log.e(TAG, "Attendance button view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("Ready for an event? Tap 'Time-In' here. You can also 'Time-Out' or send an excuse.", attendanceButton, TOTAL_QUICK_TOUR_STEPS, TOTAL_QUICK_TOUR_STEPS) {
            Toast.makeText(this@HomeActivity, "Quick Tour Completed! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Attendance Workflow Tutorial Steps ---
    private fun startAttendanceWorkflowTutorial(resetProgress: Boolean = false) {
        activeTutorialCompletionKey = KEY_ATTENDANCE_TUTORIAL_COMPLETED
        activeTutorialStepKey = KEY_ATTENDANCE_GUIDE_CURRENT_STEP
        activeTutorialTotalSteps = TOTAL_ATTENDANCE_TUTORIAL_STEPS

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        var stepToDisplay = 1

        if (resetProgress) {
            tutorialPrefs.edit()
                .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
                .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
                .apply()
            Log.d(TAG, "Attendance Guide progress reset.")
        } else {
            if (!tutorialPrefs.getBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)) {
                stepToDisplay = tutorialPrefs.getInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0) + 1
                if (stepToDisplay > TOTAL_ATTENDANCE_TUTORIAL_STEPS) {
                    Log.w(TAG, "Attendance Guide: stepToDisplay ($stepToDisplay) > total. Likely already completed or error. Resetting to 1.")
                    stepToDisplay = 1
                    tutorialPrefs.edit().putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0).apply()
                }
            } else {
                Toast.makeText(this, "Attendance Guide already completed. Restarting.", Toast.LENGTH_SHORT).show()
                tutorialPrefs.edit()
                    .putInt(KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
                    .putBoolean(KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
                    .apply()
                // stepToDisplay remains 1
            }
        }
        Log.d(TAG, "Starting/Resuming Attendance Guide. Step to display: $stepToDisplay")

        when (stepToDisplay) {
            1 -> showTimeInButtonTutorialStep()
            2 -> showTimeOutButtonTutorialStep()
            3 -> showStatusSpinnerTutorialStep()
            4 -> showExcuseLetterButtonTutorialStep()
            else -> {
                Log.e(TAG, "Attendance Guide: Invalid step $stepToDisplay. Defaulting to step 1.")
                showTimeInButtonTutorialStep()
            }
        }
    }

    private fun showTimeInButtonTutorialStep() {
        val timeInButton = findViewById<View>(R.id.btntime_in)
        if (timeInButton == null) { Log.e(TAG, "Time-In button view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog(
            "This guide focuses on attendance. First, let's see how to 'Time-In'. Tapping this opens the camera for face verification.",
            timeInButton, 1, TOTAL_ATTENDANCE_TUTORIAL_STEPS
        ) {
            val intent = Intent(this, TimeInActivity::class.java).apply {
                putExtra(EXTRA_IS_TUTORIAL_MODE, true); putExtra("userId", userId); putExtra("email", userEmail); putExtra("firstName", userFirstName)
            }
            timeInActivityTutorialLauncher.launch(intent)
        }
    }

    fun showTimeOutButtonTutorialStep() {
        val timeOutButton = findViewById<View>(R.id.btntime_out)
        if (timeOutButton == null) { Log.e(TAG, "Time-Out button view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("After your event or duty, tap 'Time-Out' here to record your end time.", timeOutButton, 2, TOTAL_ATTENDANCE_TUTORIAL_STEPS) {
            showStatusSpinnerTutorialStep()
        }
    }

    private fun showStatusSpinnerTutorialStep() {
        val statusSpinnerView = findViewById<View>(R.id.status_spinner)
        if (statusSpinnerView == null) { Log.e(TAG, "Status spinner view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("You can manually update your current work status (e.g., 'On Break', 'Off Duty') using this dropdown.", statusSpinnerView, 3, TOTAL_ATTENDANCE_TUTORIAL_STEPS) {
            showExcuseLetterButtonTutorialStep()
        }
    }

    private fun showExcuseLetterButtonTutorialStep() {
        val excuseLetterButton = findViewById<View>(R.id.excuse_letter_text_button)
        if (excuseLetterButton == null) { Log.e(TAG, "Excuse letter button view not found for tutorial"); handleTutorialCancellation(); return }
        showCustomTutorialDialog("If you're unable to attend or need to submit an excuse, you can do so by tapping here.", excuseLetterButton, TOTAL_ATTENDANCE_TUTORIAL_STEPS, TOTAL_ATTENDANCE_TUTORIAL_STEPS) {
            Toast.makeText(this@HomeActivity, "Attendance Workflow Guide Completed! 🎉", Toast.LENGTH_SHORT).show()
        }
    }
    // --- END TUTORIAL SYSTEM ---

    private fun updateAttendanceBadge(status: String) {
        attendanceStatusBadge.visibility = View.VISIBLE
        writeAttendanceStatusToRealtime(status)
        when (status.trim().lowercase(Locale.ROOT)) {
            "on time" -> { attendanceStatusBadge.text = "On Time"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_green)); attendanceStatusBadge.background = null }
            "late" -> { attendanceStatusBadge.text = "Late"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_yellow)); attendanceStatusBadge.background = null }
            "absent" -> { attendanceStatusBadge.text = "Absent"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_red)); attendanceStatusBadge.background = null }
            "has not timed-in" -> { attendanceStatusBadge.text = "Has not Timed-In"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray)); attendanceStatusBadge.background = null }
            "timed-out" -> { attendanceStatusBadge.text = "Timed-Out"; attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray)); attendanceStatusBadge.background = null }
            else -> attendanceStatusBadge.visibility = View.GONE
        }
    }

    private fun writeAttendanceStatusToRealtime(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE).getString(LoginActivity.KEY_USER_ID, null) ?: return
        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(10).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var latestTimeInLogKey: String? = null; var latestTimeInTimestamp: Long = 0
                val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                for (child in snapshot.children) {
                    val type = child.child("type").getValue(String::class.java)
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                    if (type == "TimeIn" && timestamp >= todayStart) {
                        if (timestamp > latestTimeInTimestamp) { latestTimeInTimestamp = timestamp; latestTimeInLogKey = child.key }
                    }
                }
                latestTimeInLogKey?.let { key ->
                    ref.child(key).child("attendanceBadge").setValue(status)
                        .addOnSuccessListener { Log.d(TAG, "Attendance badge '$status' written to log $key") }
                        .addOnFailureListener { e -> Log.e(TAG, "Failed to write badge to log $key: ${e.message}") }
                }
            }
            override fun onCancelled(error: DatabaseError) { Log.e("HomeActivity", "Failed to find TimeIn log to write badge: ${error.message}") }
        })
    }

    private fun evaluateAndDisplayAttendanceBadge() {
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
                } else {
                    val todayFormatted = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
                    FirebaseDatabase.getInstance().getReference("excuseLetters").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(excuseSnapshot: DataSnapshot) {
                            var isExcused = false
                            for (doc in excuseSnapshot.children) {
                                if (doc.child("date").getValue(String::class.java) == todayFormatted && doc.child("status").getValue(String::class.java).equals("Approved", ignoreCase = true)) {
                                    isExcused = true; break
                                }
                            }
                            if (isExcused) updateAttendanceBadge("Absent")
                            else if (currentTime > cutoff10am) updateAttendanceBadge("Has not Timed-In")
                            else attendanceStatusBadge.visibility = View.GONE
                        }
                        override fun onCancelled(error: DatabaseError) {
                            if (currentTime > cutoff10am) updateAttendanceBadge("Has not Timed-In") else attendanceStatusBadge.visibility = View.GONE
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                if (Calendar.getInstance().timeInMillis > cutoff10am) updateAttendanceBadge("Has not Timed-In") else attendanceStatusBadge.visibility = View.GONE
            }
        })
    }
}