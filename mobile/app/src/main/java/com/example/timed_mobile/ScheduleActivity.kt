package com.example.timed_mobile

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScheduleActivity : AppCompatActivity() {


    private lateinit var dateLabel: TextView
    private lateinit var backButton: ImageView

    private val classes = listOf(
        ClassInfo("IT332", "Web Development", "Room 301", "10:30 am - 12:00 pm", R.drawable.schedule_circle_orange),
        ClassInfo("IT342", "Database Management", "Room 405", "3:30 pm - 5:00 pm", R.drawable.schedule_circle_maroon),
        ClassInfo("CSIT335", "Advanced Programming", "Room 209", "7:30 pm - 9:00 pm", R.drawable.schedule_circle_yellow)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.schedule_page)

        dateLabel = findViewById(R.id.date_label)
        backButton = findViewById(R.id.icon_back_button)

        val topWave = findViewById<ImageView>(R.id.top_wave_animation)
        val topDrawable = topWave.drawable
        if (topDrawable is AnimatedVectorDrawable) {
            topDrawable.start()
        }

        backButton.setOnClickListener { view ->
            val drawable = (view as ImageView).drawable
            if (drawable is AnimatedVectorDrawable) {
                drawable.start()
            }
            view.postDelayed({
                finish()
            }, 50)
        }

        updateDateLabel()

        setupClassCards()
    }

    private fun setupAnimatedClickListener(view: View, onClickAction: () -> Unit) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.85f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.85f)
        scaleDownX.duration = 150
        scaleDownY.duration = 150
        scaleDownX.interpolator = AccelerateDecelerateInterpolator()
        scaleDownY.interpolator = AccelerateDecelerateInterpolator()

        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)
        scaleUpX.duration = 150
        scaleUpY.duration = 150
        scaleUpX.interpolator = AccelerateDecelerateInterpolator()
        scaleUpY.interpolator = AccelerateDecelerateInterpolator()

        val scaleDown = AnimatorSet()
        scaleDown.play(scaleDownX).with(scaleDownY)

        val scaleUp = AnimatorSet()
        scaleUp.play(scaleUpX).with(scaleUpY)

        view.setOnClickListener {
            scaleDown.start()
            view.postDelayed({
                scaleUp.start()
                onClickAction()
            }, 150)
        }
    }

    private fun updateDateLabel() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val dateString = dateFormat.format(calendar.time)
        dateLabel.text = "$dateString Class"
    }

    private fun setupClassCards() {
        val card1: CardView = findViewById(R.id.class_card_1)
        val card2: CardView = findViewById(R.id.class_card_2)
        val card3: CardView = findViewById(R.id.class_card_3)

        card1.setOnClickListener { showClassDetails(classes[0]) }
        card2.setOnClickListener { showClassDetails(classes[1]) }
        card3.setOnClickListener { showClassDetails(classes[2]) }

        val subjectCode1: TextView = findViewById(R.id.subject_code1)
        val subjectTime1: TextView = findViewById(R.id.subject_time1)
        val subjectCode2: TextView = findViewById(R.id.subject_code2)
        val subjectTime2: TextView = findViewById(R.id.subject_time2)
        val subjectCode3: TextView = findViewById(R.id.subject_code3)
        val subjectTime3: TextView = findViewById(R.id.subject_time3)

        subjectCode1.text = "${classes[0].code}\n${classes[0].name}\n${classes[0].room}"
        subjectTime1.text = classes[0].time

        subjectCode2.text = "${classes[1].code}\n${classes[1].name}\n${classes[1].room}"
        subjectTime2.text = classes[1].time

        subjectCode3.text = "${classes[2].code}\n${classes[2].name}\n${classes[2].room}"
        subjectTime3.text = classes[2].time
    }

    private fun showClassDetails(classInfo: ClassInfo) {
        val message = "${classInfo.code}: ${classInfo.name} at ${classInfo.time} in ${classInfo.room}"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    data class ClassInfo(
        val code: String,
        val name: String,
        val room: String,
        val time: String,
        val colorResource: Int
    )
}