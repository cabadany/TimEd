package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.View // Added for animation
import android.view.animation.AnimationUtils // Added for animation
import android.widget.TextView // Added for TextViews

class NewUserWelcomeActivity : WifiSecurityActivity() {

    companion object {
        const val PREFS_ONBOARDING = "OnboardingPrefs"
        const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
        const val EXTRA_USER_ID = "USER_ID"
        const val EXTRA_USER_EMAIL = "USER_EMAIL"
        const val EXTRA_USER_FIRST_NAME = "USER_FIRST_NAME"
        const val EXTRA_USER_DEPARTMENT = "USER_DEPARTMENT"
        const val EXTRA_ID_NUMBER = "ID_NUMBER"
    }

    private lateinit var welcomeIcon: ImageView
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeMessage: TextView
    private lateinit var getStartedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_welcome_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        // Initialize views for animation and functionality
        welcomeIcon = findViewById(R.id.welcome_icon)
        welcomeTitle = findViewById(R.id.welcome_title)
        welcomeMessage = findViewById(R.id.welcome_message)
        getStartedButton = findViewById(R.id.btn_get_started)

        // --- START OF ENTRY ANIMATION CODE ---
        var currentDelay = 150L

        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
            view.visibility = View.VISIBLE
        }

        welcomeIcon.visibility = View.INVISIBLE
        welcomeTitle.visibility = View.INVISIBLE
        welcomeMessage.visibility = View.INVISIBLE
        getStartedButton.visibility = View.INVISIBLE

        // 1. Welcome Icon
        animateView(welcomeIcon, R.anim.fade_in, currentDelay) // Or a custom scale/pop animation
        currentDelay += 200L

        // 2. Welcome Title
        animateView(welcomeTitle, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        // 3. Welcome Message
        animateView(welcomeMessage, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 200L

        // 4. Get Started Button
        animateView(getStartedButton, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---


        getStartedButton.setOnClickListener {
            val intent = Intent(this, NewUserFeatureActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, getIntent().getStringExtra(EXTRA_USER_ID))
                putExtra(EXTRA_USER_EMAIL, getIntent().getStringExtra(EXTRA_USER_EMAIL))
                putExtra(EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(EXTRA_USER_FIRST_NAME))
                putExtra(EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(EXTRA_USER_DEPARTMENT))
                putExtra(EXTRA_ID_NUMBER, getIntent().getStringExtra(EXTRA_ID_NUMBER))
            }
            startActivity(intent)
        }
    }
}