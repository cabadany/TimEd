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
import androidx.core.app.NotificationCompat

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TOTAL_QUICK_TOUR_STEPS = 4
        const val PREFS_TUTORIAL = "TutorialPrefs"
        const val KEY_TUTORIAL_COMPLETED = "tutorialCompleted"
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

    private val timeInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isTimedIn = result.data?.getBooleanExtra("TIMED_IN_SUCCESS", false) ?: false
                if (isTimedIn) {
                    Toast.makeText(this, "Time-In recorded successfully!", Toast.LENGTH_LONG).show()

                    // ✅ Automatically update status to On Duty
                    updateUserStatus("On Duty")
                    updateTimeLogsStatus("On Duty")

                    // ✅ Refresh visual indicators
                    loadTodayTimeInPhoto()
                    updateSidebarProfileImage()
                    evaluateAndDisplayAttendanceBadge()

                    // ✅ Force update spinner visually
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
        tutorialOverlay.setOnTouchListener { _, _ ->
            // Consume the touch event if the overlay is visible AND a tutorial popup is active.
            // This prevents the overlay from blocking touches if it was somehow left visible
            // when no tutorial step is active.
            val isTutorialStepActive = currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing
            val shouldConsumeTouch = tutorialOverlay.isVisible && isTutorialStepActive

            if (shouldConsumeTouch) {
                // Optionally, you could give a subtle feedback like a quick "shake" animation
                // to the currentTutorialPopupWindow to indicate the user should interact with it.
                // For now, just consuming the touch is the main goal.
            }
            shouldConsumeTouch
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        greetingCardNavIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) drawerLayout.closeDrawer(GravityCompat.END)
            else drawerLayout.openDrawer(GravityCompat.END)
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

        btnHelp = findViewById(R.id.btn_help) // Initialize btnHelp

        statusSpinner = findViewById(R.id.status_spinner)
        val statusAdapter = StatusAdapter(this, statusOptions)
        statusSpinner.adapter = statusAdapter

        // Setup for btn_help
        btnHelp.setOnClickListener {
            showTutorialDialog()
        }

        firestore = FirebaseFirestore.getInstance()

        loadUserStatus()

        statusSpinner.setOnTouchListener { _, _ ->
            isUserChangingStatus = true // Set flag when user touches the spinner
            false
        }

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true // Skip automatic first trigger
                    return
                }

                val selectedStatus = statusOptions[position]

                if (isUserChangingStatus) {
                    // ✅ Show confirmation if user changed manually
                    showStatusConfirmationDialog(selectedStatus)
                } else {
                    // ✅ Silent update if programmatic
                    updateUserStatus(selectedStatus)
                    updateTimeLogsStatus(selectedStatus)
                }

                isUserChangingStatus = false
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        recyclerEvents.layoutManager = LinearLayoutManager(this)
        firestore = FirebaseFirestore.getInstance()

        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
        userEmail = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
        userFirstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)
        idNumber = sharedPrefs.getString(LoginActivity.KEY_ID_NUMBER, "N/A")
        department = sharedPrefs.getString(LoginActivity.KEY_DEPARTMENT, "N/A")

        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
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

        loadTodayTimeInPhoto()
        updateSidebarProfileImage()
        setupNavigationDrawer()
        setupFilterButtons()
        setupActionButtons()
        setupExcuseLetterRedirect()

        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("HomeActivity", "Pull-to-refresh triggered")
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
            loadAndStoreEvents()
            swipeRefreshLayout.isRefreshing = false
        }

        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
        if (!tutorialPrefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)) {
            showTutorialDialog()
            tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
        }

        loadAndStoreEvents()
    }

    private fun hasTimedInToday(callback: (Boolean) -> Unit) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return callback(false)

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue

                        if (type == "TimeIn" && timestamp >= todayStart) {
                            callback(true)
                            return
                        }
                    }
                    callback(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
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
                                putExtra("userId", userId)
                                putExtra("email", userEmail ?: "")
                                putExtra("firstName", userFirstName ?: "User")
                            }
                            timeInLauncher.launch(intent)
                        }
                        .setNegativeButton("Cancel", null)
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
            .addOnSuccessListener {
                Log.d("HomeActivity", "Status updated to $status")
            }
            .addOnFailureListener {
                Log.e("HomeActivity", "Failed to update status: ${it.message}")
            }
    }

    private fun updateTimeLogsStatus(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.child("status").setValue(status)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeActivity", "Failed to update timeLogs status: ${error.message}")
                }
            })
    }

    private fun handleTimeOutOnOffDuty() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return
        val userEmail = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_EMAIL, null)
        val userFirstName = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_FIRST_NAME, "User")

        val intent = Intent(this, TimeOutActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("email", userEmail ?: "")
            putExtra("firstName", userFirstName ?: "User")
        }
        startActivity(intent)
    }

    private fun loadUserStatus() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val realtimeRef = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)

        realtimeRef.orderByChild("timestamp").limitToLast(10) // grab last few records just in case
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestTimeIn: Long? = null
                    var latestTimeOut: Long? = null

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)

                        if (type == "TimeIn" && timestamp != null) {
                            if (latestTimeIn == null || timestamp > latestTimeIn) {
                                latestTimeIn = timestamp
                            }
                        } else if (type == "TimeOut" && timestamp != null) {
                            if (latestTimeOut == null || timestamp > latestTimeOut) {
                                latestTimeOut = timestamp
                            }
                        }
                    }

                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val statusFromLogs: String = when {
                        latestTimeOut != null && latestTimeOut >= todayStart &&
                                (latestTimeIn == null || latestTimeOut > latestTimeIn) -> "Off Duty"
                        latestTimeIn != null && latestTimeIn >= todayStart -> "On Duty"
                        else -> "Off Duty"
                    }

                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { doc ->
                            val currentStatus = doc.getString("status") ?: statusFromLogs

                            // Update Firestore if not yet stored
                            firestore.collection("users").document(userId)
                                .update("status", statusFromLogs)

                            val index = statusOptions.indexOf(statusFromLogs)
                            if (index != -1) {
                                isUserChangingStatus = false
                                statusSpinner.setSelection(index)
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeActivity", "Failed to load user status: ${error.message}")
                }
            })
    }

    private fun updateFilterButtonStates(selectedButton: Button) {
        val filterButtons = listOf(btnUpcoming, btnOngoing, btnEnded, btnCancelled)

        filterButtons.forEach { button ->
            if (button.id == selectedButton.id) {
                button.isSelected = true
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.maroon)
            } else {
                button.isSelected = false
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
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

    override fun onResume() {
        super.onResume()

        val profileImageView = findViewById<ImageView>(R.id.profile_image_placeholder)

        if (hasTimedOutToday()) {
            profileImageView.setImageResource(R.drawable.ic_profile)
            val sidebarImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image)
            sidebarImage.setImageResource(R.drawable.ic_profile)
        } else {
            loadTodayTimeInPhoto()
            updateSidebarProfileImage()
            evaluateAndDisplayAttendanceBadge()
        }
    }

    private fun loadTodayTimeInPhoto() {
        val profileImageView = findViewById<ImageView>(R.id.profile_image_placeholder)
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        usersRef.get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")

            if (userStatus == "On Duty") {
                val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                ref.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                                val todayStart = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity)
                                            .load(imageUrl)
                                            .circleCrop()
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .into(profileImageView)
                                        return
                                    }
                                }
                            }
                            // fallback to profile picture if no time-in image found
                            if (!profileUrl.isNullOrEmpty()) {
                                Glide.with(this@HomeActivity)
                                    .load(profileUrl)
                                    .circleCrop()
                                    .into(profileImageView)
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_profile)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            profileImageView.setImageResource(R.drawable.ic_profile)
                        }
                    })
            } else {
                // User is Off Duty – show profilePictureUrl
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this@HomeActivity)
                        .load(profileUrl)
                        .circleCrop()
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_profile)
                }
            }
        }.addOnFailureListener {
            profileImageView.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun updateSidebarProfileImage() {
        val headerView = navigationView.getHeaderView(0)
        val sidebarImage = headerView.findViewById<ImageView>(R.id.sidebar_profile_image)

        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        usersRef.get().addOnSuccessListener { document ->
            val userStatus = document.getString("status") ?: "Off Duty"
            val profileUrl = document.getString("profilePictureUrl")

            if (userStatus == "On Duty") {
                val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
                ref.orderByChild("timestamp").limitToLast(1)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (child in snapshot.children) {
                                val type = child.child("type").getValue(String::class.java)
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                                val todayStart = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis

                                if (type == "TimeIn" && timestamp >= todayStart) {
                                    val imageUrl = child.child("imageUrl").getValue(String::class.java)
                                    if (!imageUrl.isNullOrEmpty()) {
                                        Glide.with(this@HomeActivity)
                                            .load(imageUrl)
                                            .circleCrop()
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .into(sidebarImage)
                                        return
                                    }
                                }
                            }

                            // fallback to profilePictureUrl
                            if (!profileUrl.isNullOrEmpty()) {
                                Glide.with(this@HomeActivity)
                                    .load(profileUrl)
                                    .circleCrop()
                                    .into(sidebarImage)
                            } else {
                                sidebarImage.setImageResource(R.drawable.ic_profile)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            sidebarImage.setImageResource(R.drawable.ic_profile)
                        }
                    })
            } else {
                // Off Duty - use profilePictureUrl
                if (!profileUrl.isNullOrEmpty()) {
                    Glide.with(this@HomeActivity)
                        .load(profileUrl)
                        .circleCrop()
                        .into(sidebarImage)
                } else {
                    sidebarImage.setImageResource(R.drawable.ic_profile)
                }
            }
        }.addOnFailureListener {
            sidebarImage.setImageResource(R.drawable.ic_profile)
        }
    }

    private fun sendEventNotification(title: String, message: String) {
        val channelId = "event_channel_id"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Create this icon in drawable
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
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
        val usersRef = FirebaseFirestore.getInstance().collection("users").document(userId!!)
        usersRef.get().addOnSuccessListener { userDoc ->
            val departmentId = userDoc.getString("departmentId")
            if (!departmentId.isNullOrEmpty()) {
                FirebaseFirestore.getInstance().collection("departments")
                    .document(departmentId)
                    .get()
                    .addOnSuccessListener { deptDoc ->
                        val abbreviation = deptDoc.getString("abbreviation") ?: "N/A"
                        sidebarDetails.text = "$idNumber • $abbreviation"
                    }
                    .addOnFailureListener {
                        sidebarDetails.text = "$idNumber • N/A"
                    }
            } else {
                sidebarDetails.text = "$idNumber • N/A"
            }
        }
        sidebarEmail.text = userEmail ?: ""

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_event_log -> {
                    val sharedPref = getSharedPreferences("TimedAppPrefs", Context.MODE_PRIVATE)
                    val userId = sharedPref.getString("userId", null)

                    val intent = Intent(this, EventLogActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_excuse_letter -> {
                    val intent = Intent(this, ExcuseLetterActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("email", userEmail)
                        putExtra("firstName", userFirstName)
                        putExtra("idNumber", idNumber)
                        putExtra("department", department)
                    }
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_excuse_letter_history -> {
                    val intent = Intent(this, ExcuseLetterHistoryActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("email", userEmail)
                        putExtra("firstName", userFirstName)
                        putExtra("idNumber", idNumber)
                        putExtra("department", department)
                    }
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.END)
                    true
                }
                R.id.nav_logout -> {
                    showLogoutDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                val prefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupFilterButtons() {
        btnUpcoming.setOnClickListener {
            updateFilterButtonStates(btnUpcoming)
            showEventsByStatus("upcoming")
        }
        btnOngoing.setOnClickListener {
            updateFilterButtonStates(btnOngoing)
            showEventsByStatus("ongoing")
        }
        btnEnded.setOnClickListener {
            updateFilterButtonStates(btnEnded)
            showEventsByStatus("ended")
        }
        btnCancelled.setOnClickListener {
            updateFilterButtonStates(btnCancelled)
            showEventsByStatus("cancelled")
        }
    }

    private fun setupActionButtons() {
        btnTimeIn.setOnClickListener {
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
            hasTimedInToday { alreadyTimedIn ->
                if (!alreadyTimedIn) {
                    AlertDialog.Builder(this)
                        .setTitle("Cannot Time-Out")
                        .setMessage("You haven't timed in yet. Please time in first before timing out.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Time - Out Confirmation")
                        .setMessage("Are you sure you want to time out for today?")
                        .setPositiveButton("Yes") { _, _ ->
                            val profileImageView = findViewById<ImageView>(R.id.profile_image_placeholder)
                            profileImageView.setImageResource(R.drawable.ic_profile)
                            val sidebarImage = navigationView.getHeaderView(0).findViewById<ImageView>(R.id.sidebar_profile_image)
                            sidebarImage.setImageResource(R.drawable.ic_profile)
                            setTimedOutToday()

                            val intent = Intent(this, TimeOutActivity::class.java).apply {
                                putExtra("userId", userId)
                                putExtra("email", userEmail ?: "")
                                putExtra("firstName", userFirstName ?: "User")
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
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
                putExtra("idNumber", idNumber)
                putExtra("department", department)
            }
            startActivity(intent)
        }
    }

    private fun loadAndStoreEvents() {
        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null) ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val department = userDoc.get("department")
                val departmentId: String? = if (department is Map<*, *>) department["departmentId"]?.toString() else null
                if (departmentId.isNullOrEmpty()) {
                    Toast.makeText(this, "No departmentId found for user.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                firestore.collection("events")
                    .whereEqualTo("departmentId", departmentId)
                    .get()
                    .addOnSuccessListener { result ->
                        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        allEvents.clear()

                        for (doc in result) {
                            try {
                                val title = doc.getString("eventName") ?: continue
                                val duration = doc.getString("duration") ?: "1:00:00"
                                val date = doc.getTimestamp("date")?.toDate() ?: continue
                                val dateFormatted = formatter.format(date)
                                val status = doc.getString("status") ?: "upcoming"

                                // 🔔 Notifications
                                val now = Date()
                                val timeDiff = date.time - now.time
                                if (status.equals("upcoming", ignoreCase = true) && timeDiff in 1..(24 * 60 * 60 * 1000)) {
                                    sendEventNotification("Upcoming Event", "There's an upcoming event on $dateFormatted.")
                                }
                                if (status.equals("cancelled", ignoreCase = true)) {
                                    sendEventNotification("Event Cancelled", "Event on $dateFormatted has been cancelled.")
                                }

                                allEvents.add(EventModel(title, duration, dateFormatted, status, rawDate = date))
                            } catch (e: Exception) {
                                Log.e("FirestoreEvents", "Skipping event due to error: \${e.message}", e)
                            }
                        }

                        showEventsByStatus("upcoming")
                        updateFilterButtonStates(btnUpcoming)
                    }
                    .addOnFailureListener {
                        Log.e("Firestore", "Failed to load events: \${it.message}", it)
                        Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch user document: \${it.message}", it)
                Toast.makeText(this, "Failed to load user info.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchEventsByDepartmentId(departmentId: String) {
        firestore.collection("events")
            .whereEqualTo("departmentId", departmentId)
            .get()
            .addOnSuccessListener { result ->
                val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                allEvents.clear()

                for (doc in result) {
                    try {
                        val title = doc.getString("eventName") ?: continue
                        val duration = doc.getString("duration") ?: "1:00:00"
                        val date = doc.getTimestamp("date")?.toDate() ?: continue
                        val dateFormatted = formatter.format(date)

                        allEvents.add(EventModel(title, duration, dateFormatted))
                    } catch (e: Exception) {
                        Log.e("EventParse", "Skipping bad event: ${e.message}", e)
                    }
                }

                showEventsByStatus("upcoming")
                updateFilterButtonStates(btnUpcoming)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEventsByStatus(statusFilter: String?) {
        val currentDate = Date()

        val eventsWithDynamicStatus = allEvents.map { event ->
            val eventDate = event.rawDate

            val durationParts = event.duration.split(":")
            val durationMillis = when (durationParts.size) {
                3 -> {
                    val hours = durationParts[0].toIntOrNull() ?: 0
                    val minutes = durationParts[1].toIntOrNull() ?: 0
                    val seconds = durationParts[2].toIntOrNull() ?: 0
                    (hours * 3600 + minutes * 60 + seconds) * 1000L
                }
                2 -> {
                    val hours = durationParts[0].toIntOrNull() ?: 0
                    val minutes = durationParts[1].toIntOrNull() ?: 0
                    (hours * 3600 + minutes * 60) * 1000L
                }
                1 -> {
                    val minutes = durationParts[0].toIntOrNull() ?: 0
                    (minutes * 60) * 1000L
                }
                else -> 3600000L
            }

            val eventEndDate = eventDate?.time?.plus(durationMillis)

            val dynamicStatus = when {
                event.status.equals("cancelled", ignoreCase = true) -> "cancelled"
                eventDate == null || eventEndDate == null -> "unknown"
                currentDate.time < eventDate.time -> "upcoming"
                currentDate.time in eventDate.time until eventEndDate -> "ongoing"
                else -> "ended"
            }

            event.copy(status = dynamicStatus)
        }

        val filtered = if (statusFilter == null) {
            eventsWithDynamicStatus
        } else {
            eventsWithDynamicStatus.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        val sorted = filtered.sortedWith(
            compareBy(
                { statusOrder(it.status) },
                { it.rawDate }
            )
        )

        recyclerEvents.adapter = EventAdapter(sorted)
    }

    private fun statusOrder(status: String): Int {
        return when (status.lowercase(Locale.ROOT)) {
            "upcoming" -> 0
            "ongoing" -> 1
            "ended" -> 2
            else -> 3 // unknown or other statuses
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else if (currentTutorialPopupWindow != null && currentTutorialPopupWindow!!.isShowing) {
            // If a tutorial step popup is showing, handle back press to cancel the tour
            currentTutorialPopupWindow?.dismiss() // Dismiss the popup (will trigger its OnDismissListener)

            // Explicitly hide overlay and update state here for immediate effect
            tutorialOverlay.visibility = View.GONE
            previousTargetLocation = null

            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
            Toast.makeText(this, "Tour cancelled.", Toast.LENGTH_SHORT).show()
            // currentTutorialPopupWindow is set to null in its OnDismissListener
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
        }

        val layoutQuickTour = dialog.findViewById<LinearLayout>(R.id.layout_quick_tour)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_tutorial_dialog)

        layoutQuickTour.setOnClickListener {
            previousTargetLocation = null
            showQuickTour()
            dialog.dismiss()
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
        }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomTutorialDialog(
        message: String,
        targetView: View,
        currentStep: Int,
        totalSteps: Int,
        onNext: () -> Unit
    ) {
        tutorialOverlay.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)

        val progressTextView = dialogView.findViewById<TextView>(R.id.tutorial_progress_text)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.btn_close_tutorial_step)

        progressTextView.text = "Step $currentStep of $totalSteps"
        messageTextView.text = message

        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val dialogWidth = dialogView.measuredWidth
        val dialogHeight = dialogView.measuredHeight

        var finalDialogX: Int
        var finalDialogY: Int

        val currentTargetLocationOnScreen = IntArray(2)
        targetView.getLocationOnScreen(currentTargetLocationOnScreen)
        if (targetView.visibility == View.VISIBLE && targetView.width > 0 && targetView.height > 0) {
            val spaceBelow = resources.displayMetrics.heightPixels - (currentTargetLocationOnScreen[1] + targetView.height)
            val spaceAbove = currentTargetLocationOnScreen[1]
            val margin = (16 * resources.displayMetrics.density).toInt()
            val maxX = resources.displayMetrics.widthPixels - dialogWidth - margin
            val minX = margin
            finalDialogX = when {
                maxX < minX -> margin
                else -> (currentTargetLocationOnScreen[0] + targetView.width / 2 - dialogWidth / 2).coerceIn(minX, maxX)
            }
            finalDialogY = if (spaceBelow >= dialogHeight + 24) {
                currentTargetLocationOnScreen[1] + targetView.height + 16
            } else if (spaceAbove >= dialogHeight + 24) {
                currentTargetLocationOnScreen[1] - dialogHeight - 16
            } else {
                (resources.displayMetrics.heightPixels - dialogHeight) / 2
            }
        } else {
            finalDialogX = (resources.displayMetrics.widthPixels - dialogWidth) / 2
            finalDialogY = (resources.displayMetrics.heightPixels - dialogHeight) / 2
        }

        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        popupWindow.isOutsideTouchable = false
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)))

        currentTutorialPopupWindow = popupWindow // Track the current popup
        popupWindow.setOnDismissListener {
            currentTutorialPopupWindow = null // Clear reference when dismissed
        }

        val animationSet = AnimationSet(true)
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 400
        alphaAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.decelerate_interpolator)
        animationSet.addAnimation(alphaAnimation)
        var startTranslateX = 0f
        var startTranslateY = 0f
        if (previousTargetLocation != null) {
            val prevTargetCenterX = previousTargetLocation!![0] + targetView.width / 2
            val prevTargetCenterY = previousTargetLocation!![1] + targetView.height / 2
            val deltaX = (prevTargetCenterX - (finalDialogX + dialogWidth / 2)).toFloat()
            val deltaY = (prevTargetCenterY - (finalDialogY + dialogHeight / 2)).toFloat()
            if (abs(deltaX) > abs(deltaY)) {
                startTranslateX = deltaX; startTranslateY = 0f
            } else {
                startTranslateX = 0f; startTranslateY = deltaY
            }
        } else {
            startTranslateX = -dialogWidth.toFloat() * 1.2f; startTranslateY = 0f
        }
        val translateAnimation = TranslateAnimation(startTranslateX, 0f, startTranslateY, 0f)
        translateAnimation.duration = 600
        translateAnimation.interpolator = AnimationUtils.loadInterpolator(this, android.R.anim.anticipate_overshoot_interpolator)
        animationSet.addAnimation(translateAnimation)
        dialogView.startAnimation(animationSet)

        popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, finalDialogX, finalDialogY)

        val currentTargetScreenPos = IntArray(2)
        targetView.getLocationOnScreen(currentTargetScreenPos)
        previousTargetLocation = currentTargetScreenPos

        val dismissPopupAndHideOverlay = {
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss() // This will trigger OnDismissListener
                    tutorialOverlay.visibility = View.GONE
                    previousTargetLocation = null
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
        }

        nextButton.setOnClickListener {
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss() // This will trigger OnDismissListener
                    if (currentStep == totalSteps) {
                        tutorialOverlay.visibility = View.GONE
                        previousTargetLocation = null
                        val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
                        tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
            onNext()
        }

        closeButton.setOnClickListener {
            dismissPopupAndHideOverlay() // This handles dismissing popup and hiding overlay
            Toast.makeText(this, "Tour cancelled.", Toast.LENGTH_SHORT).show()
            val tutorialPrefs = getSharedPreferences(PREFS_TUTORIAL, Context.MODE_PRIVATE)
            tutorialPrefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, true).apply()
        }
    }


    private fun hideOverlay() { // This function might be redundant if direct visibility changes are used
        if (tutorialOverlay.visibility == View.VISIBLE) {
            tutorialOverlay.visibility = View.GONE
        }
    }

    private fun showQuickTour() {
        val greetingCard = findViewById<View>(R.id.greeting_card)
        // previousTargetLocation is null or set from previous tour
        showCustomTutorialDialog(
            message = "Welcome! This is your personalized greeting card, showing your name and details.",
            targetView = greetingCard,
            currentStep = 1,
            totalSteps = TOTAL_QUICK_TOUR_STEPS
        ) { showFilterButtonsTour() }
    }

    private fun showFilterButtonsTour() {
        val filterButtons = findViewById<View>(R.id.filter_buttons)
        showCustomTutorialDialog(
            message = "Here you can filter events: view All, Upcoming, Ongoing, or past Ended events.",
            targetView = filterButtons,
            currentStep = 2,
            totalSteps = TOTAL_QUICK_TOUR_STEPS
        ) { showEventListTour() }
    }

    private fun showEventListTour() {
        val eventList = findViewById<View>(R.id.recycler_events)
        showCustomTutorialDialog(
            message = "Your selected events will appear here. Scroll to see more if available.",
            targetView = eventList,
            currentStep = 3,
            totalSteps = TOTAL_QUICK_TOUR_STEPS
        ) { showAttendanceSectionTour() }
    }

    private fun showAttendanceSectionTour() {
        val attendanceButton = findViewById<View>(R.id.btntime_in)
        showCustomTutorialDialog(
            message = "Ready for an event? Tap 'Time-In' here. You can also 'Time-Out' or send an excuse.",
            targetView = attendanceButton,
            currentStep = 4,
            totalSteps = TOTAL_QUICK_TOUR_STEPS,
            onNext = {
                Toast.makeText(this@HomeActivity, "Quick Tour Completed! 🎉", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateAttendanceBadge(status: String) {
        attendanceStatusBadge.visibility = View.VISIBLE
        writeAttendanceStatusToRealtime(status) // ✅ Only this, no recursion

        when (status.trim().lowercase()) {
            "on time" -> {
                attendanceStatusBadge.text = "On Time"
                attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_green))
                attendanceStatusBadge.background = null
            }
            "late" -> {
                attendanceStatusBadge.text = "Late"
                attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_yellow))
                attendanceStatusBadge.background = null
            }
            "absent" -> {
                attendanceStatusBadge.text = "Absent"
                attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.attendance_red))
                attendanceStatusBadge.background = null
            }
            "not timed-in" -> {
                attendanceStatusBadge.text = "Has not Timed-In"
                attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray))
                attendanceStatusBadge.background = null
            }
            "timed-out" -> {
                attendanceStatusBadge.text = "Timed-Out"
                attendanceStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.medium_gray))
                attendanceStatusBadge.background = null
            }
            else -> attendanceStatusBadge.visibility = View.GONE
        }
    }

    private fun writeAttendanceStatusToRealtime(status: String) {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)

        ref.orderByChild("timestamp").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        if (timestamp >= todayStart) {
                            child.ref.child("attendanceBadge").setValue(status)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeActivity", "Failed to write badge to Realtime DB: ${error.message}")
                }
            })
    }

    private fun evaluateAndDisplayAttendanceBadge() {
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

        val ref = FirebaseDatabase.getInstance().getReference("timeLogs").child(userId)
        val now = Calendar.getInstance()
        val currentTime = now.timeInMillis

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cutoff9am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val cutoff10am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        ref.orderByChild("timestamp").startAt(todayStart.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var timeInTimestamp: Long? = null
                    var timeOutTimestamp: Long? = null
                    var timeInSnapshot: DataSnapshot? = null
                    var timeOutSnapshot: DataSnapshot? = null

                    for (child in snapshot.children) {
                        val type = child.child("type").getValue(String::class.java)
                        val timestamp = child.child("timestamp").getValue(Long::class.java)

                        if (type == "TimeIn" && timestamp != null) {
                            timeInTimestamp = timestamp
                            timeInSnapshot = child
                        } else if (type == "TimeOut" && timestamp != null) {
                            timeOutTimestamp = timestamp
                            timeOutSnapshot = child
                        }
                    }

                    if (timeOutTimestamp != null && (timeInTimestamp == null || timeOutTimestamp > timeInTimestamp)) {
                        updateUserStatus("Off Duty")

                        var badge: String? = null
                        for (child in snapshot.children) {
                            val type = child.child("type").getValue(String::class.java)
                            val badgeValue = child.child("attendanceBadge").getValue(String::class.java)
                            if ((type == "TimeIn" || type == "TimeOut") && !badgeValue.isNullOrEmpty()) {
                                badge = badgeValue
                                break
                            }
                        }

                        if (!badge.isNullOrEmpty()) {
                            updateAttendanceBadge(badge)
                        } else if (timeInTimestamp != null) {
                            val fallback = when {
                                timeInTimestamp < cutoff9am -> "On Time"
                                timeInTimestamp < cutoff10am -> "Late"
                                else -> "Absent"
                            }
                            updateAttendanceBadge(fallback)
                            timeInSnapshot?.ref?.child("attendanceBadge")?.setValue(fallback)
                        } else {
                            attendanceStatusBadge.visibility = View.GONE
                        }
                    } else if (timeInTimestamp != null) {
                        val badge = timeInSnapshot?.child("attendanceBadge")?.getValue(String::class.java)
                        if (!badge.isNullOrEmpty()) {
                            updateAttendanceBadge(badge)
                        } else {
                            val fallback = when {
                                timeInTimestamp < cutoff9am -> "On Time"
                                timeInTimestamp < cutoff10am -> "Late"
                                else -> "Absent"
                            }
                            updateAttendanceBadge(fallback)
                            timeInSnapshot?.ref?.child("attendanceBadge")?.setValue(fallback)
                        }
                    } else {
                        val todayFormatted = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
                        val excuseRef = FirebaseDatabase.getInstance().getReference("excuseLetters").child(userId)

                        excuseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(excuseSnapshot: DataSnapshot) {
                                var matched = false
                                for (doc in excuseSnapshot.children) {
                                    val date = doc.child("date").getValue(String::class.java)
                                    val status = doc.child("status").getValue(String::class.java)
                                    if (date == todayFormatted && status.equals("Approved", ignoreCase = true)) {
                                        matched = true
                                        break
                                    }
                                }

                                if (matched) {
                                    updateAttendanceBadge("Absent")
                                    // Optional: Write this badge to the latest excuse-linked node if needed
                                } else if (currentTime > cutoff10am) {
                                    updateAttendanceBadge("Has not Timed-In")
                                } else {
                                    attendanceStatusBadge.visibility = View.GONE
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                updateAttendanceBadge("Has not Timed-In")
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    updateAttendanceBadge("Has not Timed-In")
                }
            })
    }
}