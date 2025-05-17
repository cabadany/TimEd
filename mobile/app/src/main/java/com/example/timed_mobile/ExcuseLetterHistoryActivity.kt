package com.example.timed_mobile

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.adapter.ExcuseLetterAdapter
import com.example.timed_mobile.model.ExcuseLetterModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ExcuseLetterHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExcuseLetterAdapter
    private val excuseList = mutableListOf<ExcuseLetterModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excuse_history)

        recyclerView = findViewById(R.id.recycler_excuses)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExcuseLetterAdapter(excuseList)
        recyclerView.adapter = adapter

        fetchExcuses()
    }

    private fun fetchExcuses() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("excuseLetters")
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt") // optional
            .get()
            .addOnSuccessListener { result ->
                excuseList.clear()
                for (doc in result) {
                    val date = doc.getString("date") ?: "N/A"
                    val reason = doc.getString("reason") ?: "N/A"
                    val status = doc.getString("status") ?: "Pending"
                    excuseList.add(ExcuseLetterModel(date, reason, status))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load excuses", Toast.LENGTH_SHORT).show()
                Log.e("ExcuseHistory", "Fetch failed", it)
            }
    }
}