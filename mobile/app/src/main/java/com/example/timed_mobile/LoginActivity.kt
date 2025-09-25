package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : WifiSecurityActivity() {

    private lateinit var inputIdNumber: EditText
    private lateinit var inputPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

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
        firebaseAuth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {
            val idNumber = inputIdNumber.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (idNumber.isEmpty() || password.isEmpty()) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Missing Credentials",
                    message = "Please enter your ID number and password."
                )
                return@setOnClickListener
            }

            loginUser(idNumber, password)
        }

        // Navigate to design-only Forgot Password screen
        val forgotText = findViewById<TextView>(R.id.highlight_forgotPassword)
        forgotText.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
        }

        // --- This block makes "Create Account" a clickable link ---
        val createAccountText = findViewById<TextView>(R.id.highlight_createAccount)
        val fullText = "Don't have an account? Create Account"
        val target = "Create Account"
        val start = fullText.indexOf(target)
        val end = start + target.length
        val spannable = SpannableString(fullText)

        createAccountText.isClickable = false
        createAccountText.isFocusable = false
        createAccountText.setOnClickListener(null)

        if (start >= 0) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@LoginActivity, RequestCreateAccountActivity::class.java))
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = Color.parseColor("#3538CD")
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Optional distinct color (already same in updateDrawState—can omit)
            // spannable.setSpan(ForegroundColorSpan(Color.parseColor("#3538CD")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val color = Color.parseColor("#3538CD") // Your primary button color
            spannable.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }



            createAccountText.text = spannable
            createAccountText.movementMethod = LinkMovementMethod.getInstance()
            createAccountText.highlightColor = Color.TRANSPARENT // Removes the highlight on click
        }


    private fun loginUser(idNumber: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("schoolId", idNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "User Not Found",
                        message = "No account matches the provided ID number."
                    )
                    return@addOnSuccessListener
                }

                val userDoc = documents.first()
                val dbPassword = userDoc.getString("password") ?: ""
                val role = userDoc.getString("role")?.uppercase() ?: ""
                val verified = userDoc.getBoolean("verified") ?: false

                if (role != "USER" && role != "FACULTY") {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Unsupported Role",
                        message = "Only USER/FACULTY accounts can log in on mobile."
                    )
                    return@addOnSuccessListener
                }

                // Check if user account is verified
                if (!verified) {
                    UiDialogs.showErrorPopup(
                        this,
                        title = "Account Not Verified",
                        message = "Your account must be verified by an admin. Please contact the administrator."
                    )
                    return@addOnSuccessListener
                }

                // Get user data first
                val userId = userDoc.id // This is the crucial Firebase document ID
                val firstName = userDoc.getString("firstName") ?: ""
                val email = userDoc.getString("email") ?: ""
                val department = when (val dep = userDoc.get("department")) {
                    is Map<*, *> -> dep["abbreviation"]?.toString() ?: "N/A"
                    else -> "N/A"
                }
                val schoolIdValue = userDoc.getString("schoolId") ?: idNumber // Use schoolId from doc or fallback

                // Try Firebase Auth first (for users who reset their password)
                // If that fails, fall back to bcrypt verification
                attemptFirebaseAuthLogin(email, password, userDoc, userId, firstName, department, schoolIdValue)
            }
            .addOnFailureListener { e ->
                Log.e("LOGIN", "Firestore error", e)
                UiDialogs.showErrorPopup(
                    this,
                    title = "Login Failed",
                    message = "${e.message ?: "Unexpected error occurred while logging in."}"
                )
            }
    }

    private fun attemptFirebaseAuthLogin(
        email: String, 
        password: String, 
        userDoc: com.google.firebase.firestore.QueryDocumentSnapshot,
        userId: String, 
        firstName: String, 
        department: String, 
        schoolIdValue: String
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase Auth login successful - user has reset their password
                    Log.d("LOGIN", "Firebase Auth login successful - new password used")
                    proceedWithLogin(userId, firstName, email, department, schoolIdValue)
                } else {
                    // Check if user exists in Firebase Auth (meaning they've reset their password)
                    checkIfUserHasFirebaseAccount(email, password, userDoc, userId, firstName, department, schoolIdValue)
                }
            }
    }

    private fun checkIfUserHasFirebaseAccount(
        email: String, 
        password: String, 
        userDoc: com.google.firebase.firestore.QueryDocumentSnapshot,
        userId: String, 
        firstName: String, 
        department: String, 
        schoolIdValue: String
    ) {
        // Try to fetch the user by email to see if they exist in Firebase Auth
        firebaseAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods ?: emptyList()
                    if (signInMethods.isNotEmpty()) {
                        // User exists in Firebase Auth but password is wrong
                        Log.d("LOGIN", "User has Firebase Auth account but wrong password")
                        UiDialogs.showErrorPopup(
                            this,
                            title = "Incorrect Password",
                            message = "You have reset your password. Please use your new password to log in."
                        )
                    } else {
                        // User doesn't exist in Firebase Auth, try bcrypt (original password)
                        Log.d("LOGIN", "User doesn't have Firebase Auth account, trying bcrypt")
                        attemptBcryptLogin(password, userDoc, userId, firstName, email, department, schoolIdValue)
                    }
                } else {
                    // Error checking Firebase Auth, fallback to bcrypt
                    Log.d("LOGIN", "Error checking Firebase Auth, trying bcrypt")
                    attemptBcryptLogin(password, userDoc, userId, firstName, email, department, schoolIdValue)
                }
            }
    }

    private fun attemptBcryptLogin(
        password: String, 
        userDoc: com.google.firebase.firestore.QueryDocumentSnapshot,
        userId: String, 
        firstName: String, 
        email: String, 
        department: String, 
        schoolIdValue: String
    ) {
        val dbPassword = userDoc.getString("password") ?: ""
        val result = BCrypt.verifyer().verify(password.toCharArray(), dbPassword)
        
        if (result.verified) {
            // bcrypt verification successful
            Log.d("LOGIN", "Bcrypt verification successful")
            proceedWithLogin(userId, firstName, email, department, schoolIdValue)
        } else {
            // Both Firebase Auth and bcrypt failed
            UiDialogs.showErrorPopup(
                this,
                title = "Incorrect Password",
                message = "The password you entered is incorrect."
            )
        }
    }

    private fun proceedWithLogin(
        userId: String, 
        firstName: String, 
        email: String, 
        department: String, 
        schoolIdValue: String
    ) {
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
}