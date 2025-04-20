package com.example.timed_mobile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ExcuseLetterActivity : AppCompatActivity() {

    private lateinit var reasonInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.excuse_letter_page)

        reasonInput = findViewById(R.id.reason_input)
        submitButton = findViewById(R.id.btn_submit_excuse)

        submitButton.setOnClickListener {
            val reason = reasonInput.text.toString().trim()

            if (reason.isEmpty()) {
                Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val excuseData = mapOf(
                "userId" to userId,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )

            val dbRef = FirebaseDatabase.getInstance().getReference("excuseLetters")

            dbRef.push().setValue(excuseData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Excuse submitted", Toast.LENGTH_SHORT).show()
                    reasonInput.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Submission failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}