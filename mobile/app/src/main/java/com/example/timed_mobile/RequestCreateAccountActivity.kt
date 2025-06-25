package com.example.timed_mobile

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RequestCreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.request_create_account_page)

        val backButton = findViewById<ImageView>(R.id.icon_back_button)
        backButton.setOnClickListener {
            finish() // Closes the current activity
        }

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        (topWave.drawable as? AnimatedVectorDrawable)?.start()

        val inputName = findViewById<EditText>(R.id.input_name)
        val inputIdNumber = findViewById<EditText>(R.id.input_idnumber)
        val inputEmail = findViewById<EditText>(R.id.input_email)
        val inputDepartment = findViewById<EditText>(R.id.input_department)
        val inputPassword = findViewById<EditText>(R.id.input_password)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitAccount)

        btnSubmit.setOnClickListener {
            val name = inputName.text.toString().trim()
            val idNumber = inputIdNumber.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val department = inputDepartment.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (name.isEmpty() || idNumber.isEmpty() || email.isEmpty() || department.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val subject = "TimEd Account Registration Request"
            val message = """
                New account registration request:
                
                Name: $name
                ID Number: $idNumber
                Email: $email
                Department: $department
                Password: $password
                
                Please review and approve this request.
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("timedsystems@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
            }
            try {
                startActivity(Intent.createChooser(intent, "Send email..."))
                Toast.makeText(this, "Request submitted. Please wait for approval.", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}