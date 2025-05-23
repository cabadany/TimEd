package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NewUserFinalStepActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_final_step_page)

        val finishButton: Button = findViewById(R.id.btn_finish_onboarding)

        finishButton.setOnClickListener {
            val userId = getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_ID)

            if (userId == null) {
                // This should ideally not happen if data is passed correctly
                Toast.makeText(this, "Error: User ID not found. Cannot save onboarding status.", Toast.LENGTH_LONG).show()
                // Optionally, still navigate to HomeActivity or handle error appropriately
                // For now, let's prevent setting the flag and proceed to HomeActivity
            } else {
                // Mark onboarding as completed for this specific user
                val sharedPreferences = getSharedPreferences(NewUserWelcomeActivity.PREFS_ONBOARDING, Context.MODE_PRIVATE)
                val userSpecificOnboardingKey = "${NewUserWelcomeActivity.KEY_ONBOARDING_COMPLETED}_$userId"
                with(sharedPreferences.edit()) {
                    putBoolean(userSpecificOnboardingKey, true)
                    apply()
                }
            }

            // Navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java).apply {
                // Pass user data to HomeActivity
                putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, userId) // userId might be null here if check above failed
                putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT))
                putExtra(NewUserWelcomeActivity.EXTRA_ID_NUMBER, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_ID_NUMBER)) // Added ID_NUMBER

                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }
}