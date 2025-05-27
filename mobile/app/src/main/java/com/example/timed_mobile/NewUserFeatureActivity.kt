package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class NewUserFeatureActivity : AppCompatActivity() {

    private var clockAnimation: Animatable? = null
    private lateinit var featureIcon: ImageView // Declare at class level

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_feature_page) // Ensure this layout file name is correct

        val nextButton: Button = findViewById(R.id.btn_next_feature)
        featureIcon = findViewById(R.id.feature_icon) // Initialize here

        // Get the drawable and store it if it's Animatable
        val drawable = featureIcon.drawable
        if (drawable is Animatable) {
            clockAnimation = drawable
        }

        nextButton.setOnClickListener {
            val intent = Intent(this, NewUserFinalStepActivity::class.java).apply {
                // Pass along user data received from NewUserWelcomeActivity
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
        // Start the animation when the activity becomes visible
        clockAnimation?.start()
    }

    override fun onStop() {
        super.onStop()
        // Stop the animation when the activity is no longer visible to save resources
        clockAnimation?.stop()
    }
}