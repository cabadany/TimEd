package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.View // Added for animation
import android.view.animation.AnimationUtils // Added for animation
import android.widget.TextView // Added for feature_title and feature_message

class NewUserFeatureActivity : WifiSecurityActivity() {

    private var clockAnimation: Animatable? = null
    private lateinit var featureIcon: ImageView
    private lateinit var featureTitle: TextView // Declare feature_title
    private lateinit var featureMessage: TextView // Declare feature_message
    private lateinit var nextButton: Button // Declare nextButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_feature_page)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        // Initialize views for animation and functionality
        featureIcon = findViewById(R.id.feature_icon)
        featureTitle = findViewById(R.id.feature_title)
        featureMessage = findViewById(R.id.feature_message)
        nextButton = findViewById(R.id.btn_next_feature)


        // Get the drawable and store it if it's Animatable (for the clock ticking)
        val drawable = featureIcon.drawable
        if (drawable is Animatable) {
            clockAnimation = drawable
        }

        // --- START OF ENTRY ANIMATION CODE ---
        var currentDelay = 150L // Start with a slightly longer initial delay if desired

        fun animateView(view: View, animationResId: Int, delay: Long) {
            val anim = AnimationUtils.loadAnimation(view.context, animationResId)
            anim.startOffset = delay
            view.startAnimation(anim)
            view.visibility = View.VISIBLE // Ensure view is visible before animation starts
        }

        // Make views initially invisible if they are to fade/slide in
        featureIcon.visibility = View.INVISIBLE
        featureTitle.visibility = View.INVISIBLE
        featureMessage.visibility = View.INVISIBLE
        nextButton.visibility = View.INVISIBLE

        // 1. Feature Icon (Fade In or a custom scale-up if you have one)
        animateView(featureIcon, R.anim.fade_in, currentDelay) // Using fade_in for simplicity
        currentDelay += 200L // Icon can take a bit longer to appear

        // 2. Feature Title (Slide Down)
        animateView(featureTitle, R.anim.slide_down_fade_in, currentDelay)
        currentDelay += 150L

        // 3. Feature Message (Slide Down or Fade In)
        animateView(featureMessage, R.anim.slide_down_fade_in, currentDelay) // Or R.anim.fade_in
        currentDelay += 200L

        // 4. Next Button (Slide Up)
        animateView(nextButton, R.anim.slide_up_fade_in_content, currentDelay)
        // --- END OF ENTRY ANIMATION CODE ---

        nextButton.setOnClickListener {
            val intent = Intent(this, NewUserFinalStepActivity::class.java).apply {
                putExtra(NewUserWelcomeActivity.EXTRA_USER_ID, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_ID))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_EMAIL))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_FIRST_NAME))
                putExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT, getIntent().getStringExtra(NewUserWelcomeActivity.EXTRA_USER_DEPARTMENT))
            }
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // Start the clock ticking animation when the activity becomes visible
        clockAnimation?.start()
    }

    override fun onStop() {
        super.onStop()
        // Stop the clock ticking animation when the activity is no longer visible
        clockAnimation?.stop()
    }
}