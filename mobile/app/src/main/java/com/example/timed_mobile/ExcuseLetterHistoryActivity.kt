package com.example.timed_mobile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

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

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("excuseLetters")
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt")
            .get()
            .addOnSuccessListener { result ->
                excuseList.clear()
                for (doc in result) {
                    val model = doc.toObject(ExcuseLetterModel::class.java)
                    excuseList.add(model)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}