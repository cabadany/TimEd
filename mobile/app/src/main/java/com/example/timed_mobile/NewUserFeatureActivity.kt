/*
package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class NewUserFeatureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_user_feature_page) // Make sure this matches your XML file name

        val nextButton: Button = findViewById(R.id.btn_next_feature)
        val featureIcon: ImageView = findViewById(R.id.feature_icon)

        // Start the animation for the clock icon
        val drawable = featureIcon.drawable
        if (drawable is Animatable) {
            (drawable as Animatable).start()
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
            // Not finishing here to allow back navigation within the onboarding flow
        }
    }
}*/
