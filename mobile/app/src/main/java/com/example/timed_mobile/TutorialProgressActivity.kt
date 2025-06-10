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
    private lateinit var tvNoActiveTutorial: TextView

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
        tvNoActiveTutorial = findViewById(R.id.tv_no_active_tutorial)

        cardQuickTour = findViewById(R.id.card_quick_tour)
        cardAttendanceGuide = findViewById(R.id.card_attendance_guide)

        loadTutorialProgress()

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
        // Uses keys that HomeActivity should now consistently use
        val quickTourIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_QUICK_TOUR_COMPLETED, false)
        val quickTourSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_QUICK_TOUR_CURRENT_STEP, 0) // Last completed step

        pbQuickTour.max = HomeActivity.TOTAL_QUICK_TOUR_STEPS // Ensure max is set for accurate percentage
        if (quickTourIsCompleted) {
            tvQuickTourStatus.text = "Completed"
            pbQuickTour.progress = HomeActivity.TOTAL_QUICK_TOUR_STEPS
        } else if (quickTourSavedStep > 0) {
            pbQuickTour.progress = quickTourSavedStep
            tvQuickTourStatus.text = "In Progress (${quickTourSavedStep}/${HomeActivity.TOTAL_QUICK_TOUR_STEPS})"
        } else {
            tvQuickTourStatus.text = "Not Started"
            pbQuickTour.progress = 0
        }

        // --- Attendance Workflow Guide Progress ---
        // Uses keys that HomeActivity should now consistently use
        val attendanceGuideIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
        val attendanceGuideSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0) // Last completed step

        pbAttendanceGuide.max = HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS // Ensure max is set
        if (attendanceGuideIsCompleted) {
            tvAttendanceGuideStatus.text = "Completed"
            pbAttendanceGuide.progress = HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS
        } else if (attendanceGuideSavedStep > 0) {
            pbAttendanceGuide.progress = attendanceGuideSavedStep
            tvAttendanceGuideStatus.text = "In Progress (${attendanceGuideSavedStep}/${HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS})"
        } else {
            tvAttendanceGuideStatus.text = "Not Started"
            pbAttendanceGuide.progress = 0
        }

        val anyTutorialEverStartedOrCompleted = quickTourIsCompleted || quickTourSavedStep > 0 ||
                attendanceGuideIsCompleted || attendanceGuideSavedStep > 0

        if (anyTutorialEverStartedOrCompleted) {
            tvNoActiveTutorial.visibility = View.GONE
        } else {
            tvNoActiveTutorial.visibility = View.VISIBLE // Show if nothing has ever been touched
            // Optionally hide cards if you prefer "No active tutorial" to dominate
            // cardQuickTour.visibility = View.GONE
            // cardAttendanceGuide.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}