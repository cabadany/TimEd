package com.example.timed_mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NewUserWelcomeActivity : AppCompatActivity() {

    companion object {
        // SharedPreferences constants for onboarding status
        const val PREFS_ONBOARDING = "OnboardingPrefs" // Separate prefs file for onboarding
        const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"

        // Intent extras keys for passing user data through onboarding
        // These should align with what LoginActivity sends and HomeActivity might expect
        const val EXTRA_USER_ID = "USER_ID" // Using consistent naming
        const val EXTRA_USER_EMAIL = "USER_EMAIL"
        const val EXTRA_USER_FIRST_NAME = "USER_FIRST_NAME"
        const val EXTRA_USER_DEPARTMENT = "USER_DEPARTMENT"
        // Add any other keys LoginActivity might send and HomeActivity might need
        const val EXTRA_ID_NUMBER = "ID_NUMBER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_welcome_page)

        val getStartedButton: Button = findViewById(R.id.btn_get_started)

        getStartedButton.setOnClickListener {
            val intent = Intent(this, NewUserFeatureActivity::class.java).apply {
                // Pass along any user data received from LoginActivity
                putExtra(EXTRA_USER_ID, getIntent().getStringExtra(EXTRA_USER_ID))
                putExtra(EXTRA_USER_EMAIL, getIntent().getStringExtra(EXTRA_USER_EMAIL))
                putExtra(EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(EXTRA_USER_FIRST_NAME))
                putExtra(EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(EXTRA_USER_DEPARTMENT))
                putExtra(EXTRA_ID_NUMBER, getIntent().getStringExtra(EXTRA_ID_NUMBER))
            }
            startActivity(intent)
            // Optional: finish() if you don't want users to go back to this specific step.
            // For now, let's not finish, allowing back navigation within onboarding.
        }
    }
}