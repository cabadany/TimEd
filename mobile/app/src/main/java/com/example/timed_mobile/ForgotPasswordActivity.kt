package com.example.timed_mobile

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.view.animation.AnimationUtils

class ForgotPasswordActivity : WifiSecurityActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password_page)
        setupAnimations()

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val bottomWave = findViewById<ImageView>(R.id.bottom_wave_animation)
        (bottomWave.drawable as? AnimatedVectorDrawable)?.start()

        val emailInput = findViewById<EditText>(R.id.input_email)
        val sendButton = findViewById<Button>(R.id.btnSendReset)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)

        backButton.setOnClickListener {
            finish()
        }

        // Design-only: no backend, just show a confirmation popup
        sendButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim().orEmpty()
            if (email.isEmpty()) {
                UiDialogs.showErrorPopup(
                    this,
                    title = "Missing Email",
                    message = "Please enter your email to continue."
                )
            } else {
                UiDialogs.showForgotPasswordSuccess(this, email) {
                    // Close the forgot password screen after user acknowledges
                    finish()
                }
            }
        }
    }

    private fun setupAnimations() {
        val title = findViewById<TextView>(R.id.titleForgot)
        val backButton = findViewById<ImageView>(R.id.icon_back_button)
        val formElements = listOf<View>(
            findViewById(R.id.outline_email),
            findViewById(R.id.btnSendReset)
        )

        val animSlideDown = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_in)
        backButton.startAnimation(animSlideDown)
        title.startAnimation(animSlideDown)

        formElements.forEachIndexed { index, view ->
            val animSlideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_form_element)
            animSlideUp.startOffset = (index * 100).toLong()
            view.startAnimation(animSlideUp)
        }

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()
    }
}
