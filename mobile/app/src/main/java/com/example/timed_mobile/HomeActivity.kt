package com.example.timed_mobile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.adapter.EventAdapter
import com.example.timed_mobile.model.EventModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerEvents: RecyclerView
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnAll: Button
    private lateinit var btnUpcoming: Button
    private lateinit var btnOngoing: Button
    private lateinit var btnEnded: Button

    private val allEvents = mutableListOf<EventModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        // Greeting
        val name = intent.getStringExtra("name") ?: "User"
        val idNumber = intent.getStringExtra("idNumber") ?: "N/A"
        val department = intent.getStringExtra("department") ?: "N/A"

        findViewById<TextView>(R.id.greeting_name).text = "Hi, $name 👋"
        findViewById<TextView>(R.id.home_greeting).text = "ID: $idNumber • $department"

        // RecyclerView
        recyclerEvents = findViewById(R.id.recycler_events)
        recyclerEvents.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()

        // Filters
        btnAll = findViewById(R.id.btn_filter_all)
        btnUpcoming = findViewById(R.id.btn_filter_upcoming)
        btnOngoing = findViewById(R.id.btn_filter_ongoing)
        btnEnded = findViewById(R.id.btn_filter_ended)

        setupFilterButtons()
        loadAndStoreEvents()
    }

    private fun setupFilterButtons() {
        btnAll.setOnClickListener { showEventsByStatus(null) }
        btnUpcoming.setOnClickListener { showEventsByStatus("upcoming") }
        btnOngoing.setOnClickListener { showEventsByStatus("ongoing") }
        btnEnded.setOnClickListener { showEventsByStatus("ended") }
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

                showEventsByStatus(null) // Show all by default
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load events: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEventsByStatus(statusFilter: String?) {
        val formatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

        val filtered = if (statusFilter == null) {
            allEvents
        } else {
            allEvents.filter { it.status.equals(statusFilter, ignoreCase = true) }
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
}