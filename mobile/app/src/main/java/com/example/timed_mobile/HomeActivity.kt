package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var greetingCardNavIcon: ImageView // Declared

    private val allEvents = mutableListOf<EventModel>()

    private val timeInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val isTimedIn = result.data?.getBooleanExtra("TIMED_IN_SUCCESS", false) ?: false
            if (isTimedIn) {
                Toast.makeText(this, "✅ Time-In recorded successfully!", Toast.LENGTH_LONG).show()
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

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        greetingCardNavIcon = findViewById(R.id.greeting_card_nav_icon) // Initialize

        // Set OnClickListener for the new icon in the greeting card
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
            Toast.makeText(this, "User session error. Please log in again.", Toast.LENGTH_LONG).show()
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
                    val sharedPreferences = getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)
                        remove(LoginActivity.KEY_USER_ID)
                        remove(LoginActivity.KEY_EMAIL)
                        remove(LoginActivity.KEY_FIRST_NAME)
                        remove(LoginActivity.KEY_ID_NUMBER)
                        remove(LoginActivity.KEY_DEPARTMENT)
                        // or clear()
                        apply()
                    }
                    FirebaseAuth.getInstance().signOut() // Good practice

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

        setupFilterButtons()
        setupActionButtons()
        setupExcuseLetterRedirect()
        loadAndStoreEvents()
    }

    private fun setupFilterButtons() {
        btnAll.setOnClickListener { showEventsByStatus(null) }
        btnUpcoming.setOnClickListener { showEventsByStatus("upcoming") }
        btnOngoing.setOnClickListener { showEventsByStatus("ongoing") }
        btnEnded.setOnClickListener { showEventsByStatus("ended") }
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
                showEventsByStatus(null)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEventsByStatus(statusFilter: String?) {
        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
        val currentDate = Date()

        val eventsWithDynamicStatus = allEvents.map { event ->
            val eventDate = formatter.parse(event.dateFormatted)
            val dynamicStatus = when {
                eventDate == null -> "unknown"
                eventDate.after(currentDate) -> "upcoming"
                event.status.equals("ongoing", ignoreCase = true) -> "ongoing"
                eventDate.before(currentDate) -> "ended"
                else -> event.status
            }
            event.copy(status = dynamicStatus)
        }


        val filtered = if (statusFilter == null) {
            eventsWithDynamicStatus
        } else {
            eventsWithDynamicStatus.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        val sorted = filtered.sortedWith(compareBy(
            { statusOrder(it.status) },
            { formatter.parse(it.dateFormatted) }
        ))

        recyclerEvents.adapter = EventAdapter(sorted)
    }

    private fun statusOrder(status: String): Int {
        return when (status.lowercase(Locale.ROOT)) {
            "upcoming" -> 0
            "ongoing" -> 1
            "ended" -> 2
            else -> 3
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}