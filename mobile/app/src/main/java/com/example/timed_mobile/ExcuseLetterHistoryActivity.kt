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
    private val FADE_DURATION = 300L // Duration for fade animations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_history)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) topDrawable.start()

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        recyclerView = findViewById(R.id.recycler_excuses)
        emptyText = findViewById(R.id.text_empty)
        backButton = findViewById(R.id.icon_back_button)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        excuseList = mutableListOf()
        adapter = ExcuseLetterAdapter(excuseList) // Make sure ExcuseLetterAdapter exists and is correctly implemented
        recyclerView.adapter = adapter

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.maroon, R.color.yellow_gold) // Customize colors
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe to refresh triggered.")
            fetchExcuseLetters(isRefreshing = true)
        }

        // Initial fetch of excuse letters
        fetchExcuseLetters(isRefreshing = false)
    }

    private fun fetchExcuseLetters(isRefreshing: Boolean = false) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            if (isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            } else {
                progressBar.visibility = View.GONE
            }
            // Show empty text with an appropriate message
            emptyText.text = "User not authenticated. Please log in."
            emptyText.animate()
                .alpha(1f)
                .setDuration(FADE_DURATION)
                .setListener(object : AnimatorListenerAdapter() {
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
            emptyText.alpha = 0f // Reset alpha for potential fade-in
        }

        val databaseRef = FirebaseDatabase.getInstance().getReference("excuseLetters")
        val query = databaseRef.orderByChild("userId").equalTo(uid)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                excuseList.clear()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val excuse = child.getValue(ExcuseLetterModel::class.java)
                        if (excuse != null) {
                            // If your ExcuseLetterModel needs an ID from the Firebase key:
                            // excuse.id = child.key
                            excuseList.add(excuse)
                        }
                    }
                    // Optionally sort the list, e.g., by date if your model has a timestamp or date field
                    // excuseList.sortByDescending { it.timestamp } // Example

                    adapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                    emptyText.alpha = 0f
                    // RecyclerView's layout animation is set in XML, so it should run automatically
                } else {
                    recyclerView.visibility = View.GONE
                    emptyText.text = "No excuse letters submitted." // Set default empty message
                    emptyText.animate()
                        .alpha(1f)
                        .setDuration(FADE_DURATION)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator) {
                                emptyText.visibility = View.VISIBLE
                            }
                        })
                    Log.d(TAG, "No excuse letters found for UID: $uid")
                }

                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(this@ExcuseLetterHistoryActivity, "Failed to load data: ${error.message}", Toast.LENGTH_LONG).show()

                if (isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                } else {
                    progressBar.visibility = View.GONE
                }

                emptyText.text = "Failed to load data. Swipe to try again."
                emptyText.animate()
                    .alpha(1f)
                    .setDuration(FADE_DURATION)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            emptyText.visibility = View.VISIBLE
                        }
                    })
                recyclerView.visibility = View.GONE
            }
        })
    }
}