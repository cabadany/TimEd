package com.example.timed_mobile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private val TAG = "ExcuseHistory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excuse_history)

        recyclerView = findViewById(R.id.recycler_excuses)
        emptyText = findViewById(R.id.text_empty)
        backButton = findViewById(R.id.icon_back_button)

        recyclerView.layoutManager = LinearLayoutManager(this)
        excuseList = mutableListOf()
        adapter = ExcuseLetterAdapter(excuseList)
        recyclerView.adapter = adapter

        backButton.setOnClickListener {
            finish()
        }

        fetchExcuseLetters()
    }

    private fun fetchExcuseLetters() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        if (uid.isNullOrEmpty()) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Logged in UID: $uid")

        val databaseRef = FirebaseDatabase.getInstance().getReference("excuseLetters")
        val query = databaseRef.orderByChild("userId").equalTo(uid)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                excuseList.clear()

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val excuse = child.getValue(ExcuseLetterModel::class.java)
                        if (excuse != null) {
                            excuseList.add(excuse)
                        }
                    }

                    adapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                } else {
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    Log.d(TAG, "No excuse letters found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(this@ExcuseLetterHistoryActivity, "Failed to load data.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}