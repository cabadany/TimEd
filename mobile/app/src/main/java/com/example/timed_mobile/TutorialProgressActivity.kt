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

class TutorialProgressActivity : WifiSecurityActivity() {

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
        val quickTourIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_QUICK_TOUR_COMPLETED, false)
        var quickTourSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_QUICK_TOUR_CURRENT_STEP, 0)
        val quickTourTotalSteps = HomeActivity.TOTAL_QUICK_TOUR_STEPS

        pbQuickTour.max = quickTourTotalSteps
        if (quickTourIsCompleted) {
            tvQuickTourStatus.text = "Status: Completed (100%)"
            pbQuickTour.progress = quickTourTotalSteps
        } else {
            // Defensively cap savedStep for display if it somehow exceeds total steps
            if (quickTourSavedStep > quickTourTotalSteps) {
                quickTourSavedStep = quickTourTotalSteps
            }

            if (quickTourSavedStep > 0 && quickTourTotalSteps > 0) {
                val percentage = (quickTourSavedStep * 100) / quickTourTotalSteps
                pbQuickTour.progress = quickTourSavedStep
                tvQuickTourStatus.text = "Status: In Progress - ${percentage}% (${quickTourSavedStep}/${quickTourTotalSteps})"
            } else {
                tvQuickTourStatus.text = "Status: Not Started (0%)"
                pbQuickTour.progress = 0
            }
        }

        // --- Attendance Workflow Guide Progress ---
        val attendanceGuideIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
        var attendanceGuideSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
        val attendanceGuideTotalSteps = HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS

        pbAttendanceGuide.max = attendanceGuideTotalSteps
        if (attendanceGuideIsCompleted) {
            tvAttendanceGuideStatus.text = "Status: Completed (100%)"
            pbAttendanceGuide.progress = attendanceGuideTotalSteps
        } else {
            // Defensively cap savedStep for display
            if (attendanceGuideSavedStep > attendanceGuideTotalSteps) {
                attendanceGuideSavedStep = attendanceGuideTotalSteps
            }

            if (attendanceGuideSavedStep > 0 && attendanceGuideTotalSteps > 0) {
                val percentage = (attendanceGuideSavedStep * 100) / attendanceGuideTotalSteps
                pbAttendanceGuide.progress = attendanceGuideSavedStep
                tvAttendanceGuideStatus.text = "Status: In Progress - ${percentage}% (${attendanceGuideSavedStep}/${attendanceGuideTotalSteps})"
            } else {
                tvAttendanceGuideStatus.text = "Status: Not Started (0%)"
                pbAttendanceGuide.progress = 0
            }
        }

        // Determine if any tutorial has ever been started or completed to hide the "No active tutorial" message
        val anyQuickTourProgress = quickTourIsCompleted || quickTourSavedStep > 0
        val anyAttendanceGuideProgress = attendanceGuideIsCompleted || attendanceGuideSavedStep > 0

        if (anyQuickTourProgress || anyAttendanceGuideProgress) {
            tvNoActiveTutorial.visibility = View.GONE
        } else {
            tvNoActiveTutorial.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Go back to the previous activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}