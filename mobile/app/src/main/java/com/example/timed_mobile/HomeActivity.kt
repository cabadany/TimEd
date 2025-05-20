package com.example.timed_mobile

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
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TOTAL_QUICK_TOUR_STEPS = 4
    }

    private var previousTargetLocation: IntArray? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var greetingName: TextView
    private lateinit var greetingDetails: TextView
    private lateinit var recyclerEvents: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnCancelled: Button
    private lateinit var btnUpcoming: Button
    private lateinit var btnOngoing: Button
    private lateinit var btnEnded: Button
    private lateinit var btnTimeIn: Button
    private lateinit var btnTimeOut: Button
    private lateinit var excuseLetterText: TextView

    private lateinit var greetingCardNavIcon: ImageView
    private lateinit var tutorialOverlay: FrameLayout

    private val allEvents = mutableListOf<EventModel>()
    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var idNumber: String? = null
    private var department: String? = null

    private val timeInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isTimedIn = result.data?.getBooleanExtra("TIMED_IN_SUCCESS", false) ?: false
                if (isTimedIn) {
                    Toast.makeText(this, "Time-In recorded successfully!", Toast.LENGTH_LONG).show()
                    loadTodayTimeInPhoto()
                    updateSidebarProfileImage()
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        tutorialOverlay = findViewById(R.id.tutorial_overlay)
        tutorialOverlay.setOnTouchListener { v, event ->
            if (event?.action == MotionEvent.ACTION_UP) v?.performClick()
            true
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        greetingCardNavIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
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

        recyclerEvents.layoutManager = LinearLayoutManager(this)
        firestore = FirebaseFirestore.getInstance()

        val sharedPrefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
        userId = sharedPrefs.getString(LoginActivity.KEY_USER_ID, null)
        userEmail = sharedPrefs.getString(LoginActivity.KEY_EMAIL, null)
        userFirstName = sharedPrefs.getString(LoginActivity.KEY_FIRST_NAME, null)
        idNumber = sharedPrefs.getString(LoginActivity.KEY_ID_NUMBER, "N/A")
        department = sharedPrefs.getString(LoginActivity.KEY_DEPARTMENT, "N/A")

        greetingName.text = "Hi, $userFirstName"
        greetingDetails.text = "$idNumber • $department"

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

        loadAndStoreEvents()
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
        }
    }

    private fun loadTodayTimeInPhoto() {
        val profileImageView = findViewById<ImageView>(R.id.profile_image_placeholder)
        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

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
                    profileImageView.setImageResource(R.drawable.ic_profile)
                }

                override fun onCancelled(error: DatabaseError) {
                    profileImageView.setImageResource(R.drawable.ic_profile)
                }
            })
    }

    private fun updateSidebarProfileImage() {
        val headerView = navigationView.getHeaderView(0)
        val sidebarImage = headerView.findViewById<ImageView>(R.id.sidebar_profile_image)

        val userId = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
            .getString(LoginActivity.KEY_USER_ID, null) ?: return

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
                    sidebarImage.setImageResource(R.drawable.ic_profile)
                }

                override fun onCancelled(error: DatabaseError) {
                    sidebarImage.setImageResource(R.drawable.ic_profile)
                }
            })
    }

    private fun setupNavigationDrawer() {
        val headerView = navigationView.getHeaderView(0)
        val sidebarName = headerView.findViewById<TextView>(R.id.sidebar_user_name)
        val sidebarDetails = headerView.findViewById<TextView>(R.id.sidebar_user_details)
        val sidebarEmail = headerView.findViewById<TextView>(R.id.sidebar_user_email)

        sidebarName.text = userFirstName ?: "User"
        sidebarDetails.text = "$idNumber • $department"
        sidebarEmail.text = userEmail ?: ""

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_event_log -> {
                    val intent = Intent(this, EventLogActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
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
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_excuse_letter_history -> {
                    val intent = Intent(this, ExcuseLetterHistoryActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
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
                    drawerLayout.closeDrawer(GravityCompat.START)
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

        // Step 1: Get the user's departmentId from nested "department" object
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val department = userDoc.get("department")
                Log.d("DEBUG", "Raw department field: $department")
                Log.d("DEBUG", "Full user document: ${userDoc.data}")

                val departmentId = if (department is Map<*, *>) {
                    department["departmentId"].toString()
                } else null

                if (departmentId.isNullOrEmpty()) {
                    Toast.makeText(this, "No departmentId found for user.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                Log.d("HomeActivity", "User's departmentId: $departmentId")

                // Step 2: Now load events for this departmentId
                firestore.collection("events")
                    .whereEqualTo("departmentId", departmentId)
                    .get()
                    .addOnSuccessListener { result ->
                        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        allEvents.clear()

                        for (doc in result) {
                            try {
                                val title = doc.getString("eventName") ?: continue
                                val statusRaw = doc.getString("status") ?: "unknown"
                                val status = statusRaw.lowercase(Locale.ROOT)
                                val date = doc.getTimestamp("date")?.toDate() ?: continue
                                val formattedDate = formatter.format(date)

                                allEvents.add(EventModel(title, status, formattedDate))
                            } catch (e: Exception) {
                                Log.e("FirestoreEvents", "Skipping event due to error: ${e.message}", e)
                            }
                        }

                        showEventsByStatus("upcoming")
                        updateFilterButtonStates(btnUpcoming)
                    }
                    .addOnFailureListener {
                        Log.e("Firestore", "Failed to load events: ${it.message}", it)
                        Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch user document: ${it.message}", it)
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
                        val statusRaw = doc.getString("status") ?: "unknown"
                        val status = statusRaw.lowercase(Locale.ROOT)
                        val date = doc.getTimestamp("date")?.toDate() ?: continue
                        val formattedDate = formatter.format(date)

                        allEvents.add(EventModel(title, status, formattedDate))
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
        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val currentDate = Date()

        val eventsWithDynamicStatus = allEvents.map { event ->
            val eventDate = try {
                formatter.parse(event.dateFormatted)
            } catch (e: Exception) {
                null
            }
            val dynamicStatus = when {
                eventDate == null -> "unknown"
                // For "upcoming", check if eventDate is strictly after current time
                eventDate.after(currentDate) && !event.status.equals(
                    "ongoing",
                    ignoreCase = true
                ) -> "upcoming"
                // For "ongoing", explicitly trust the status if it's "ongoing"
                event.status.equals("ongoing", ignoreCase = true) -> "ongoing"
                // For "ended", check if eventDate is before current time and not "ongoing"
                eventDate.before(currentDate) && !event.status.equals(
                    "ongoing",
                    ignoreCase = true
                ) -> "ended"
                // Fallback to original status if none of the above
                else -> event.status
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
                {
                    try {
                        formatter.parse(it.dateFormatted)
                    } catch (e: Exception) {
                        null
                    }
                }
            ))

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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun showTutorialDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_tutorial_options)

        ContextCompat.getColor(this, android.R.color.transparent)

        // Adjust dialog width to be responsive
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)

            // Set dialog width to 90% of screen width
            val displayMetrics = resources.displayMetrics
            layoutParams.width = (displayMetrics.widthPixels * 0.90).toInt()
            // You can also set a maxHeight if needed, e.g.,
            // layoutParams.height = (displayMetrics.heightPixels * 0.85).toInt();
            // For now, let's keep height as wrap_content by not setting it explicitly here,
            // as the XML root is android:layout_height="wrap_content"

            window.attributes = layoutParams
        }


        val layoutQuickTour = dialog.findViewById<LinearLayout>(R.id.layout_quick_tour)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel_tutorial_dialog)

        layoutQuickTour.setOnClickListener {
            previousTargetLocation = null // Reset for a new tour
            showQuickTour()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCustomTutorialDialog(
        message: String,
        targetView: View,
        currentStep: Int,
        totalSteps: Int,
        onNext: () -> Unit
    ) {
        tutorialOverlay.visibility = View.VISIBLE // Make sure overlay is visible

        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)
        // ... (findViewById for progressText, messageText, nextButton, closeButton)
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
        // ... (Your existing robust dialogX, dialogY calculation logic from previous step) ...
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
            val showBelow = spaceBelow >= dialogHeight + 24
            finalDialogY = if (showBelow) {
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
        // End of dialogX, dialogY calculation


        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        popupWindow.isOutsideTouchable = false
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent))

        // ... (Animation setup as in the previous correct step) ...
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
        // End of animation setup


        popupWindow.showAtLocation(targetView.rootView, Gravity.NO_GRAVITY, finalDialogX, finalDialogY)

        val currentTargetScreenPos = IntArray(2)
        targetView.getLocationOnScreen(currentTargetScreenPos)
        previousTargetLocation = currentTargetScreenPos

        val dismissPopupAndOverlay = {
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    popupWindow.dismiss()
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
                    popupWindow.dismiss()
                    if (currentStep == totalSteps) {
                        tutorialOverlay.visibility = View.GONE
                        previousTargetLocation = null
                    }
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            dialogView.startAnimation(fadeOut)
            onNext()
        }

        closeButton.setOnClickListener {
            dismissPopupAndOverlay()
            Toast.makeText(this, "Tour cancelled.", Toast.LENGTH_SHORT).show()
        }
    }


    // Function to hide the overlay
    private fun hideOverlay() {
        tutorialOverlay.visibility = View.GONE
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
}