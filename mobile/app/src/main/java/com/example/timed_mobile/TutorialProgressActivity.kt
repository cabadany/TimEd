package com.example.timed_mobile

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView // Added for CardView

class TutorialProgressActivity : AppCompatActivity() {

    private lateinit var tvQuickTourStatus: TextView
    private lateinit var pbQuickTour: ProgressBar
    private lateinit var tvAttendanceGuideStatus: TextView
    private lateinit var pbAttendanceGuide: ProgressBar
    private lateinit var tvNoActiveTutorial: TextView

    // CardView declarations for animation
    private lateinit var cardQuickTour: CardView
    private lateinit var cardAttendanceGuide: CardView

    // private var userId: String? = null // Uncomment if you pass and use userId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure this matches your XML file name: tutorial_progress_page.xml
        setContentView(R.layout.tutorial_progress_page)

        val toolbar: Toolbar = findViewById(R.id.toolbar_tutorial_progress)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // userId = intent.getStringExtra("userId") // Uncomment if needed

        tvQuickTourStatus = findViewById(R.id.tv_quick_tour_status)
        pbQuickTour = findViewById(R.id.pb_quick_tour)
        tvAttendanceGuideStatus = findViewById(R.id.tv_attendance_guide_status)
        pbAttendanceGuide = findViewById(R.id.pb_attendance_guide)
        tvNoActiveTutorial = findViewById(R.id.tv_no_active_tutorial)

        // Initialize CardViews (ensure these IDs exist in your tutorial_progress_page.xml)
        cardQuickTour = findViewById(R.id.card_quick_tour)
        cardAttendanceGuide = findViewById(R.id.card_attendance_guide)

        loadTutorialProgress()

        // Apply animations
        val animSlideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn.startOffset = 100 // Optional: delay for the first card
        cardQuickTour.startAnimation(animSlideUpFadeIn)

        val animSlideUpFadeIn2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn2.startOffset = 300 // Optional: slightly longer delay for the second card
        cardAttendanceGuide.startAnimation(animSlideUpFadeIn2)
    }

    private fun loadTutorialProgress() {
        val tutorialPrefs = getSharedPreferences(HomeActivity.PREFS_TUTORIAL, Context.MODE_PRIVATE)

        val quickTourCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_TUTORIAL_COMPLETED, false)
        val attendanceGuideCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)

        var anyTutorialActiveOrCompleted = false

        if (quickTourCompleted) {
            tvQuickTourStatus.text = "Completed"
            pbQuickTour.progress = 100
            anyTutorialActiveOrCompleted = true
        } else {
            tvQuickTourStatus.text = "Not Started"
            pbQuickTour.progress = 0
            // If you want to show the card even if not started, ensure anyTutorialActiveOrCompleted can be true
            // For example, if the tutorial is available to be started.
            // For now, this logic means if neither is completed, "no active tutorial" might show.
            // Consider if "Not Started" tutorials should still make the cards visible.
            // If so, you might always set anyTutorialActiveOrCompleted = true here,
            // or have a more complex check for tutorial availability.
        }

        if (attendanceGuideCompleted) {
            tvAttendanceGuideStatus.text = "Completed"
            pbAttendanceGuide.progress = 100
            anyTutorialActiveOrCompleted = true
        } else {
            tvAttendanceGuideStatus.text = "Not Started"
            pbAttendanceGuide.progress = 0
            // Similar consideration as above for "Not Started" state.
        }

        // This logic will hide the "No active tutorial" text if EITHER tutorial is completed.
        // If both are "Not Started", "No active tutorial" will be shown.
        // The cards themselves are not explicitly hidden here, their visibility depends on the XML default
        // and whether the "No active tutorial" text overlays them or if the parent layout adjusts.
        if (!anyTutorialActiveOrCompleted) {
            tvNoActiveTutorial.visibility = View.VISIBLE
            // You might also want to explicitly hide the cards if no tutorials are active/completed:
            // cardQuickTour.visibility = View.GONE
            // cardAttendanceGuide.visibility = View.GONE
        } else {
            tvNoActiveTutorial.visibility = View.GONE
            // And ensure cards are visible if tutorials are active/completed:
            // cardQuickTour.visibility = View.VISIBLE
            // cardAttendanceGuide.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Goes back to the previous activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}