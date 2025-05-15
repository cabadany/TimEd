package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.adapter.EventAdapter
import com.example.timed_mobile.model.EventModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var greetingName: TextView
    private lateinit var greetingDetails: TextView
    private lateinit var recyclerEvents: RecyclerView
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnAll: Button
    private lateinit var btnUpcoming: Button
    private lateinit var btnOngoing: Button
    private lateinit var btnEnded: Button
    private lateinit var btnTimeIn: Button
    private lateinit var btnTimeOut: Button
    private lateinit var excuseLetterText: TextView

    private lateinit var greetingCardNavIcon: ImageView

    private lateinit var tutorialOverlay: FrameLayout
    private lateinit var highlightView: View

    private val allEvents = mutableListOf<EventModel>()

    private val timeInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val isTimedIn = result.data?.getBooleanExtra("TIMED_IN_SUCCESS", false) ?: false
                if (isTimedIn) {
                    Toast.makeText(this, "✅ Time-In recorded successfully!", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

    private var userId: String? = null
    private var userEmail: String? = null
    private var userFirstName: String? = null
    private var idNumber: String? = null
    private var department: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Initialize the overlay and highlight view
        tutorialOverlay = findViewById(R.id.tutorial_overlay)

        // Help button functionality
        val helpButton = findViewById<ImageView>(R.id.btn_help)
        helpButton.setOnClickListener {
            showTutorialDialog()
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon)

        greetingCardNavIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        greetingName = findViewById(R.id.greeting_name)
        greetingDetails = findViewById(R.id.home_greeting)
        recyclerEvents = findViewById(R.id.recycler_events)
        btnAll = findViewById(R.id.btn_filter_all)
        btnUpcoming = findViewById(R.id.btn_filter_upcoming)
        btnOngoing = findViewById(R.id.btn_filter_ongoing)
        btnEnded = findViewById(R.id.btn_filter_ended)
        btnTimeIn = findViewById(R.id.btntime_in)
        btnTimeOut = findViewById(R.id.btntime_out)
        excuseLetterText = findViewById(R.id.excuse_letter_text_button)

        recyclerEvents.layoutManager = LinearLayoutManager(this)
        firestore = FirebaseFirestore.getInstance()

        userId = intent.getStringExtra("userId")
        userEmail = intent.getStringExtra("email")
        userFirstName = intent.getStringExtra("firstName")
        idNumber = intent.getStringExtra("idNumber") ?: "N/A"
        department = intent.getStringExtra("department") ?: "N/A"

        if (userId == null) {
            Toast.makeText(this, "User session error. Please log in again.", Toast.LENGTH_LONG)
                .show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        greetingName.text = "Hi, $userFirstName 👋"
        greetingDetails.text = "ID: $idNumber • $department"

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java).apply {
                        putExtra("userId", userId)
                        putExtra("email", userEmail)
                        putExtra("firstName", userFirstName)
                        putExtra("idNumber", idNumber)
                        putExtra("department", department)
                    }
                    startActivity(intent)
                }

                R.id.nav_schedule -> {
                    val intent = Intent(this, ScheduleActivity::class.java).apply {
                        putExtra("userId", userId)
                    }
                    startActivity(intent)
                }

                R.id.nav_logout -> {
                    val sharedPreferences =
                        getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                        remove(LoginActivity.KEY_USER_ID)
                        remove(LoginActivity.KEY_EMAIL)
                        remove(LoginActivity.KEY_FIRST_NAME)
                        remove(LoginActivity.KEY_ID_NUMBER)
                        remove(LoginActivity.KEY_DEPARTMENT)
                        apply()
                    }
                    FirebaseAuth.getInstance().signOut()

                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupFilterButtons() // This will set up the listeners correctly
        setupActionButtons()
        setupExcuseLetterRedirect()
        loadAndStoreEvents() // This will call showEventsByStatus(null) and update button state

        // The initial state for btnAll is now handled within loadAndStoreEvents's success block
    }

    private fun updateFilterButtonStates(selectedButton: Button) {
        val filterButtons = listOf(btnAll, btnUpcoming, btnOngoing, btnEnded)

        filterButtons.forEach { button ->
            if (button.id == selectedButton.id) {
                button.isSelected = true // For text color selector and StateListAnimator
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.maroon)
            } else {
                button.isSelected = false // For text color selector and StateListAnimator
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
        }
    }

    private fun setupFilterButtons() {
        btnAll.setOnClickListener {
            updateFilterButtonStates(btnAll)
            showEventsByStatus(null)
        }
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
            val intent = Intent(this, TimeOutActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail ?: "")
                putExtra("firstName", userFirstName ?: "User")
            }
            startActivity(intent)
        }
    }

    private fun setupExcuseLetterRedirect() {
        excuseLetterText.setOnClickListener {
            val intent = Intent(this, ExcuseLetterActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("email", userEmail)
                putExtra("firstName", userFirstName)
                putExtra("idNumber", idNumber)
                putExtra("department", department)
            }
            startActivity(intent)
        }
    }

    private fun loadAndStoreEvents() {
        firestore.collection("events").get()
            .addOnSuccessListener { result ->
                val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                allEvents.clear()

                for (doc in result) {
                    val title = doc.getString("eventName") ?: continue
                    val status = doc.getString("status") ?: "unknown"
                    val date = doc.getTimestamp("date")?.toDate() ?: continue
                    val formattedDate = formatter.format(date)

                    allEvents.add(EventModel(title, status, formattedDate))
                }
                showEventsByStatus(null) // Display all events initially
                updateFilterButtonStates(btnAll) // Set "All" button as selected after data load
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events: ${it.message}", Toast.LENGTH_SHORT)
                    .show()
                // Even on failure, ensure a default button state
                updateFilterButtonStates(btnAll)
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
        val options = arrayOf("Quick Tour", "Detailed Tour", "Feature-Specific Tutorials")
        AlertDialog.Builder(this)
            .setTitle("Interactive Guide")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showQuickTour() // Start the Quick Tour
                    1 -> showFilterButtonsTour()
                    2 -> showEventListTour()
                    3 -> showAttendanceSectionTour()


                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showCustomTutorialDialog(message: String, targetView: View, onNext: () -> Unit) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.custom_tutorial_dialog, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tutorial_message)
        val nextButton = dialogView.findViewById<Button>(R.id.tutorial_next_button)
        messageTextView.text = message

        // Apply fade-in animation
        val fadeIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in)
        dialogView.startAnimation(fadeIn)

        // Measure dialog size before showing
        dialogView.measure(
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.heightPixels, View.MeasureSpec.AT_MOST)
        )
        val dialogWidth = dialogView.measuredWidth
        val dialogHeight = dialogView.measuredHeight

        val popupWindow = PopupWindow(dialogView, dialogWidth, dialogHeight, true)
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent))

        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val targetX = location[0]
        val targetY = location[1]
        val targetWidth = targetView.width
        val targetHeight = targetView.height

        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels

        val spaceBelow = screenHeight - (targetY + targetHeight)
        val spaceAbove = targetY

        // Center horizontally over the target, but clamp to screen edges with 16dp margin
        val margin = (16 * resources.displayMetrics.density).toInt()
        val maxX = screenWidth - dialogWidth - margin
        val minX = margin
        val dialogX = when {
            maxX < minX -> margin // fallback if dialog is too wide
            else -> (targetX + targetWidth / 2 - dialogWidth / 2).coerceIn(minX, maxX)
        }

        val showBelow = spaceBelow >= dialogHeight + 24
        val dialogY = if (showBelow) {
            targetY + targetHeight + 16
        } else if (spaceAbove >= dialogHeight + 24) {
            targetY - dialogHeight - 16
        } else {
            (screenHeight - dialogHeight) / 2
        }

        popupWindow.showAtLocation(targetView, Gravity.NO_GRAVITY, dialogX, dialogY)

        nextButton.setOnClickListener {
            popupWindow.dismiss()
            onNext()
        }
    }


    // Function to hide the overlay
    private fun hideOverlay() {
        tutorialOverlay.visibility = View.GONE
        highlightView.visibility = View.GONE
    }

    private fun showQuickTour() {
        val greetingCard = findViewById<View>(R.id.greeting_card)
        showCustomTutorialDialog(
            message = "This is your personalized greeting card. It shows your name, ID, and department.",
            targetView = greetingCard
        ) { showFilterButtonsTour() }
    }

    private fun showFilterButtonsTour() {
        val filterButtons = findViewById<View>(R.id.filter_buttons)
        showCustomTutorialDialog(
            message = "Use these buttons to filter events by category: All, Upcoming, Ongoing, or Ended.",
            targetView = filterButtons
        ) { showEventListTour() }
    }

    private fun showEventListTour() {
        val eventList = findViewById<View>(R.id.recycler_events)
        showCustomTutorialDialog(
            message = "This is the event list. It shows all the events based on your selected filter.",
            targetView = eventList
        ) { showAttendanceSectionTour() }
    }

    private fun showAttendanceSectionTour() {
        val attendanceSection = findViewById<View>(R.id.btntime_in)
        showCustomTutorialDialog(
            message = "Use the Time-In and Time-Out buttons to mark your attendance.",
            targetView = attendanceSection
        ) {
            Toast.makeText(this, "Quick Tour Completed! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

}
