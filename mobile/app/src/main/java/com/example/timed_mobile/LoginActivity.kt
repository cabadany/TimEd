package com.example.timed_mobile

import android.content.Context // Added import
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var inputIdNumber: EditText
    private lateinit var inputPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var firestore: FirebaseFirestore

    // SharedPreferences constants
    companion object {
        const val PREFS_NAME = "TimedAppPrefs"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_EMAIL = "email"
        const val KEY_FIRST_NAME = "firstName"
        const val KEY_ID_NUMBER = "idNumber" // School ID
        const val KEY_DEPARTMENT = "department"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        // Start bottom wave animation
        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        val bottomDrawable = bottomWave.drawable
        if (bottomDrawable is AnimatedVectorDrawable) {
            bottomDrawable.start()
        }

        inputIdNumber = findViewById(R.id.input_idnumber)
        inputPassword = findViewById(R.id.input_Password)
        loginButton = findViewById(R.id.btnLogin)
        firestore = FirebaseFirestore.getInstance()

        loginButton.setOnClickListener {
            val idNumber = inputIdNumber.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (idNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter ID number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithSchoolId(idNumber, password)
        }
    }



    private fun loginWithSchoolId(idNumber: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("schoolId", idNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.firstOrNull()
                    if (document != null) {
                        val dbPassword = document.getString("password") ?: ""
                        val role = document.getString("role") ?: "USER"

                        if (role.uppercase() != "USER") {
                            Toast.makeText(this, "Only USER accounts can log in on mobile.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        if (dbPassword == password) {
                            val userId = document.id // This is the Firestore document ID
                            val firstName = document.getString("firstName") ?: ""
                            val lastName = document.getString("lastName") ?: ""
                            val name = "$firstName $lastName"
                            val email = document.getString("email") ?: ""
                            val department = when (val dep = document.get("department")) {
                                is Map<*, *> -> dep["abbreviation"]?.toString() ?: "N/A"
                                is String -> dep
                                else -> "N/A"
                            }

                            // Save login state and user details
                            val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putBoolean(KEY_IS_LOGGED_IN, true)
                                putString(KEY_USER_ID, userId)
                                putString(KEY_EMAIL, email)
                                putString(KEY_FIRST_NAME, firstName)
                                putString(KEY_ID_NUMBER, idNumber) // This is the schoolId used for login
                                putString(KEY_DEPARTMENT, department)
                                apply()
                            }

                            Toast.makeText(this, "Welcome $name!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, HomeActivity::class.java).apply {
                                putExtra("userId", userId)
                                putExtra("email", email)
                                putExtra("firstName", firstName)
                                putExtra("idNumber", idNumber)
                                putExtra("department", department)
                            }

                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Unexpected error: user record is missing.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("LOGIN", "Firestore error", it)
                Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}