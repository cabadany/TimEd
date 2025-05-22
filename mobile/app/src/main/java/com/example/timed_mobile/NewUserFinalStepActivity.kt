/*
package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NewUserFinalStepActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_final_step_page) // Make sure this matches your XML file name

        val finishButton: Button = findViewById(R.id.btn_finish_onboarding)

        finishButton.setOnClickListener {
            // Mark onboarding as completed
            // Using constants from NewUserWelcomeActivity for SharedPreferences
            val sharedPreferences = getSharedPreferences(NewUserWelcomeActivity.PREFS_ONBOARDING, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putBoolean(NewUserWelcomeActivity.KEY_ONBOARDING_COMPLETED, true)
                apply()
            }

            // Navigate to HomeActivity
            val intent = Intent(this, HomeActivity::class.java).apply {
                // Pass user data to HomeActivity
                putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_ID))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT))

                // Clear the task stack and start HomeActivity as a new task
                // This prevents the user from going back to the onboarding flow
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity() // Finishes this activity and all parent activities in the task.
            // This is a robust way to clear the onboarding stack.
        }
    }
}*/
