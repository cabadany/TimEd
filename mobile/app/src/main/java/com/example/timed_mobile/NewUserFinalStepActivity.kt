package com.example.timed_mobile

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.animation.AnimationUtils

class NewUserFinalStepActivity : AppCompatActivity() {

    private lateinit var completionIcon: ImageView
    private lateinit var completionTitle: TextView
    private lateinit var completionMessage: TextView
    private lateinit var finishButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_final_step_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        // Initialize views for animation and functionality
        completionIcon = findViewById(R.id.final_step_icon) // Corrected ID
        completionTitle = findViewById(R.id.final_step_title) // Corrected ID
        completionMessage = findViewById(R.id.final_step_message) // Corrected ID
        finishButton = findViewById(R.id.btn_finish_onboarding)

        // --- START OF ENTRY ANIMATION CODE ---
        var currentDelay = 150L

        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
            view.visibility = View.VISIBLE
        }

        completionIcon.visibility = View.INVISIBLE
        completionTitle.visibility = View.INVISIBLE
        completionMessage.visibility = View.INVISIBLE
        finishButton.visibility = View.INVISIBLE // Make button invisible before animation

        // 1. Completion Icon
        animateView(completionIcon, R.anim.fade_in, currentDelay)
        currentDelay += 200L

        // 2. Completion Title
        animateView(completionTitle, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        // 3. Completion Message
        animateView(completionMessage, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 200L

        // 4. Finish Button
        animateView(finishButton, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---

        finishButton.setOnClickListener {
            val userId = getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_ID)

            if (userId == null) {
                Toast.makeText(this, "Error: User ID not found. Cannot save onboarding status.", Toast.LENGTH_LONG).show()
            } else {
                val sharedPreferences = getSharedPreferences(NewUserWelcomeActivity.PREFS_ONBOARDING, Context.MODE_PRIVATE)
                val userSpecificOnboardingKey = "${NewUserWelcomeActivity.KEY_ONBOARDING_COMPLETED}_$userId"
                with(sharedPreferences.edit()) {
                    putBoolean(userSpecificOnboardingKey, true)
                    apply()
                }
            }

            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, userId)
                putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT))
                putExtra(NewUserWelcomeActivity.EXTRA_ID_NUMBER, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_ID_NUMBER))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }
}