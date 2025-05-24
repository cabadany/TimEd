package com.example.timed_mobile

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.timed_mobile.adapter.ExcuseLetterAdapter
import com.example.timed_mobile.model.ExcuseLetterModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ExcuseLetterHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExcuseLetterAdapter
    private lateinit var excuseList: MutableList<ExcuseLetterModel>
    private lateinit var emptyText: TextView
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val TAG = "ExcuseHistory"
    private val FADE_DURATION = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_history)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) topDrawable.start()

        backButton = findViewById(R.id.icon_back_button)

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({ finish() }, 50)
        }

        recyclerView = findViewById(R.id.recycler_excuses)
        emptyText = findViewById(R.id.text_empty)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        excuseList = mutableListOf()
        adapter = ExcuseLetterAdapter(excuseList)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe to refresh triggered.")
            fetchExcuseLetters(isRefreshing = true)
        }

        fetchExcuseLetters(isRefreshing = false)
    }

    private fun fetchExcuseLetters(isRefreshing: Boolean = false) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid
        monitorExcuseLetterStatusChanges()

        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            if (isRefreshing) swipeRefreshLayout.isRefreshing = false else progressBar.visibility = View.GONE
            emptyText.text = "User not authenticated. Please log in."
            emptyText.animate().alpha(1f).setDuration(FADE_DURATION).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    emptyText.visibility = View.VISIBLE
                }
            })
            recyclerView.visibility = View.GONE
            return
        }

        Log.d(TAG, "Fetching excuse letters for UID: $uid")

        if (!isRefreshing) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.GONE
            emptyText.alpha = 0f
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("excuseLetters").child(uid)

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                excuseList.clear()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val excuse = child.getValue(ExcuseLetterModel::class.java)
                        excuse?.let {
                            excuseList.add(it)
                        }
                    }
                    adapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                    emptyText.alpha = 0f
                } else {
                    recyclerView.visibility = View.GONE
                    emptyText.text = "No excuse letters submitted."
                    emptyText.animate().alpha(1f).setDuration(FADE_DURATION).setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            emptyText.visibility = View.VISIBLE
                        }
                    })
                    Log.d(TAG, "No excuse letters found for UID: $uid")
                }

                if (isRefreshing) swipeRefreshLayout.isRefreshing = false else progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(this@ExcuseLetterHistoryActivity, "Failed to load data: ${error.message}", Toast.LENGTH_LONG).show()

                if (isRefreshing) swipeRefreshLayout.isRefreshing = false else progressBar.visibility = View.GONE

                emptyText.text = "Failed to load data. Swipe to try again."
                emptyText.animate().alpha(1f).setDuration(FADE_DURATION).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        emptyText.visibility = View.VISIBLE
                    }
                })
                recyclerView.visibility = View.GONE
            }
        })
    }

    private var lastKnownStatuses: MutableMap<String, String> = mutableMapOf()

    private fun monitorExcuseLetterStatusChanges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("excuseLetters").child(uid)

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val status = snapshot.child("status").getValue(String::class.java) ?: return
                lastKnownStatuses[id] = status
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val newStatus = snapshot.child("status").getValue(String::class.java) ?: return
                val oldStatus = lastKnownStatuses[id]

                if (oldStatus != null && newStatus != oldStatus) {
                    lastKnownStatuses[id] = newStatus
                    val title = "Excuse Letter Update"
                    val message = "Your excuse letter has been $newStatus."
                    com.example.timed_mobile.utils.NotificationUtils.showNotification(this@ExcuseLetterHistoryActivity, title, message)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}