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
import androidx.cardview.widget.CardView

class TutorialProgressActivity : AppCompatActivity() {

    private lateinit var tvQuickTourStatus: TextView
    private lateinit var pbQuickTour: ProgressBar
    private lateinit var tvAttendanceGuideStatus: TextView
    private lateinit var pbAttendanceGuide: ProgressBar
    private lateinit var tvNoActiveTutorial: TextView // Will be managed based on actual progress

    private lateinit var cardQuickTour: CardView
    private lateinit var cardAttendanceGuide: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tutorial_progress_page)

        val toolbar: Toolbar = findViewById(R.id.toolbar_tutorial_progress)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        tvQuickTourStatus = findViewById(R.id.tv_quick_tour_status)
        pbQuickTour = findViewById(R.id.pb_quick_tour)
        tvAttendanceGuideStatus = findViewById(R.id.tv_attendance_guide_status)
        pbAttendanceGuide = findViewById(R.id.pb_attendance_guide)
        tvNoActiveTutorial = findViewById(R.id.tv_no_active_tutorial) // Keep for now

        cardQuickTour = findViewById(R.id.card_quick_tour)
        cardAttendanceGuide = findViewById(R.id.card_attendance_guide)

        loadTutorialProgress()

        // Apply animations
        val animSlideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn.startOffset = 100
        cardQuickTour.startAnimation(animSlideUpFadeIn)

        val animSlideUpFadeIn2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn2.startOffset = 300
        cardAttendanceGuide.startAnimation(animSlideUpFadeIn2)
    }

    private fun loadTutorialProgress() {
        val tutorialPrefs = getSharedPreferences(HomeActivity.PREFS_TUTORIAL, Context.MODE_PRIVATE)

        // --- Quick Tour Progress ---
        val quickTourIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_QUICK_TOUR_COMPLETED, false)
        val quickTourSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_QUICK_TOUR_CURRENT_STEP, 0)

        if (quickTourIsCompleted) {
            tvQuickTourStatus.text = "Completed"
            pbQuickTour.progress = 100
        } else if (quickTourSavedStep > 0) {
            val progressPercentage = (quickTourSavedStep * 100) / HomeActivity.TOTAL_QUICK_TOUR_STEPS
            pbQuickTour.progress = progressPercentage
            tvQuickTourStatus.text = "In Progress (${quickTourSavedStep}/${HomeActivity.TOTAL_QUICK_TOUR_STEPS})"
        } else {
            tvQuickTourStatus.text = "Not Started"
            pbQuickTour.progress = 0
        }

        // --- Attendance Workflow Guide Progress ---
        val attendanceGuideIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
        val attendanceGuideSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)

        if (attendanceGuideIsCompleted) {
            tvAttendanceGuideStatus.text = "Completed"
            pbAttendanceGuide.progress = 100
        } else if (attendanceGuideSavedStep > 0) {
            val progressPercentage = (attendanceGuideSavedStep * 100) / HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS
            pbAttendanceGuide.progress = progressPercentage
            tvAttendanceGuideStatus.text = "In Progress (${attendanceGuideSavedStep}/${HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS})"
        } else {
            tvAttendanceGuideStatus.text = "Not Started"
            pbAttendanceGuide.progress = 0
        }

        // Manage visibility of "No active tutorial" text
        // If both are "Not Started" (step 0) AND not marked as completed (which they wouldn't be if step is 0),
        // then it might be relevant. However, "Not Started" is a valid status on the card.
        // For now, let's assume the cards are always shown and display their status.
        // If you want to hide cards for "Not Started" and show "No active tutorial", this logic would change.
        val anyTutorialEverStartedOrCompleted = quickTourIsCompleted || quickTourSavedStep > 0 ||
                attendanceGuideIsCompleted || attendanceGuideSavedStep > 0

        if (anyTutorialEverStartedOrCompleted) {
            tvNoActiveTutorial.visibility = View.GONE
        } else {
            // This means both tutorials are at step 0 and not marked completed.
            // You could show "No active tutorial" or let the cards show "Not Started".
            // Let's hide it if cards show "Not Started".
            tvNoActiveTutorial.visibility = View.GONE
        }
        // Ensure cards are visible by default in XML or set them visible here if needed.
        // cardQuickTour.visibility = View.VISIBLE
        // cardAttendanceGuide.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}