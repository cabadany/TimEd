package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : WifiSecurityActivity() {

    private lateinit var inputIdNumber: EditText
    private lateinit var inputPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var firestore: FirebaseFirestore

    companion object {
        const val PREFS_NAME = "TimedAppPrefs"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_EMAIL = "email"
        const val KEY_FIRST_NAME = "firstName"
        const val KEY_ID_NUMBER = "idNumber"
        const val KEY_DEPARTMENT = "department"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        inputIdNumber = findViewById(R.id.input_idnumber)
        inputPassword = findViewById(R.id.input_Password)
        loginButton = findViewById(R.id.btnLogin)
        firestore = FirebaseFirestore.getInstance()

        loginButton.setOnClickListener {
            val idNumber = inputIdNumber.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (idNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter ID number and password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            loginUser(idNumber, password)
        }

        // --- This block makes "Create Account" a clickable link ---
        val createAccountText = findViewById<TextView>(R.id.highlight_createAccount)
        val fullText = createAccountText.text.toString()
        val spannable = SpannableString(fullText)

        val clickablePart = "Create Account"
        val start = fullText.indexOf(clickablePart)
        if (start != -1) { // Check if the text exists to avoid errors
            val end = start + clickablePart.length

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(this@LoginActivity, RequestCreateAccountActivity::class.java)
                    startActivity(intent)
                }
            }

            // Make the text clickable
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Change the color of the clickable text
            val color = Color.parseColor("#3538CD") // Your primary button color
            spannable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            createAccountText.text = spannable
            createAccountText.movementMethod = LinkMovementMethod.getInstance()
            createAccountText.highlightColor = Color.TRANSPARENT // Removes the highlight on click
        }
    }

    private fun loginUser(idNumber: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("schoolId", idNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userDoc = documents.first()
                val dbPassword = userDoc.getString("password") ?: ""
                val role = userDoc.getString("role")?.uppercase() ?: ""
                val verified = userDoc.getBoolean("verified") ?: false

                if (role != "USER" && role != "FACULTY") {
                    Toast.makeText(
                        this,
                        "Only USER/FACULTY accounts can log in on mobile.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

/*                // Check if user account is verified
                if (!verified) {
                    Toast.makeText(
                        this,
                        "Your account needs to be verified by an admin first. Please contact the administrator.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }*/

                val result = BCrypt.verifyer().verify(password.toCharArray(), dbPassword)
                if (!result.verified) {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val userId = userDoc.id // This is the crucial Firebase document ID
                val firstName = userDoc.getString("firstName") ?: ""
                val email = userDoc.getString("email") ?: ""
                val department = when (val dep = userDoc.get("department")) {
                    is Map<*, *> -> dep["abbreviation"]?.toString() ?: "N/A"
                    else -> "N/A"
                }
                val schoolIdValue = userDoc.getString("schoolId") ?: idNumber // Use schoolId from doc or fallback

                // Save login session
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(prefs.edit()) {
                    putBoolean(KEY_IS_LOGGED_IN, true)
                    putString(KEY_USER_ID, userId) // Storing Firebase document ID
                    putString(KEY_FIRST_NAME, firstName)
                    putString(KEY_EMAIL, email)
                    putString(KEY_ID_NUMBER, schoolIdValue) // Storing schoolId
                    putString(KEY_DEPARTMENT, department)
                    apply()
                }

                Toast.makeText(this, "Welcome $firstName!", Toast.LENGTH_SHORT).show()

                // Check if onboarding is completed for this specific user
                val onboardingPrefs = getSharedPreferences(
                    NewUserWelcomeActivity.PREFS_ONBOARDING,
                    Context.MODE_PRIVATE
                )
                // Construct the user-specific key
                val userSpecificOnboardingKey = "${NewUserWelcomeActivity.KEY_ONBOARDING_COMPLETED}_$userId"
                val isOnboardingCompleted = onboardingPrefs.getBoolean(
                    userSpecificOnboardingKey, // Check user-specific key
                    false // Default to false if no entry found for this user
                )

                if (!isOnboardingCompleted) {
                    // THIS IS FOR "NEW USERS" (to the onboarding flow)
                    // Start Onboarding Flow
                    val intent = Intent(this, NewUserWelcomeActivity::class.java).apply {
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, userId)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, email)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, firstName)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, department)
                    }
                    startActivity(intent)
                } else {
                    // THIS IS FOR "EXISTING USERS" (who have completed onboarding)
                    // Go to Home Activity
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, userId)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, email)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, firstName)
                        putExtra(NewUserWelcomeActivity.EXTRA_ID_NUMBER, schoolIdValue)
                        putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, department)
                    }
                    startActivity(intent)
                }
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("LOGIN", "Firestore error", e)
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}