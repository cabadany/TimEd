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
    private lateinit var tvEventTutorialStatus: TextView
    private lateinit var pbEventTutorial: ProgressBar
    private lateinit var tvNoActiveTutorial: TextView

    private lateinit var cardQuickTour: CardView
    private lateinit var cardAttendanceGuide: CardView
    private lateinit var cardEventTutorial: CardView

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
        tvEventTutorialStatus = findViewById(R.id.tv_event_tutorial_status)
        pbEventTutorial = findViewById(R.id.pb_event_tutorial)
        tvNoActiveTutorial = findViewById(R.id.tv_no_active_tutorial)

        cardQuickTour = findViewById(R.id.card_quick_tour)
        cardAttendanceGuide = findViewById(R.id.card_attendance_guide)
        cardEventTutorial = findViewById(R.id.card_event_tutorial)

        loadTutorialProgress()

        // Staggered animation for cards
        val animSlideUpFadeIn = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn.startOffset = 100
        cardQuickTour.startAnimation(animSlideUpFadeIn)

        val animSlideUpFadeIn2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn2.startOffset = 300
        cardAttendanceGuide.startAnimation(animSlideUpFadeIn2)
        
        val animSlideUpFadeIn3 = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in_bottom)
        animSlideUpFadeIn3.startOffset = 500
        cardEventTutorial.startAnimation(animSlideUpFadeIn3)
    }

    private fun loadTutorialProgress() {
        val tutorialPrefs = getSharedPreferences(HomeActivity.PREFS_TUTORIAL, Context.MODE_PRIVATE)

        // --- Quick Tour Progress ---
        val quickTourIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_QUICK_TOUR_COMPLETED, false)
        var quickTourSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_QUICK_TOUR_CURRENT_STEP, 0)
        val quickTourTotalSteps = HomeActivity.TOTAL_QUICK_TOUR_STEPS

        pbQuickTour.max = quickTourTotalSteps
        if (quickTourIsCompleted) {
            tvQuickTourStatus.text = "Completed ✓"
            pbQuickTour.progress = quickTourTotalSteps
            setStatusBadge(tvQuickTourStatus, StatusType.COMPLETED)
        } else {
            if (quickTourSavedStep > quickTourTotalSteps) {
                quickTourSavedStep = quickTourTotalSteps
            }

            if (quickTourSavedStep > 0 && quickTourTotalSteps > 0) {
                val percentage = (quickTourSavedStep * 100) / quickTourTotalSteps
                pbQuickTour.progress = quickTourSavedStep
                tvQuickTourStatus.text = "${percentage}%"
                setStatusBadge(tvQuickTourStatus, StatusType.IN_PROGRESS)
            } else {
                tvQuickTourStatus.text = "Not Started"
                pbQuickTour.progress = 0
                setStatusBadge(tvQuickTourStatus, StatusType.NOT_STARTED)
            }
        }

        // --- Attendance Workflow Guide Progress ---
        val attendanceGuideIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_ATTENDANCE_TUTORIAL_COMPLETED, false)
        var attendanceGuideSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_ATTENDANCE_GUIDE_CURRENT_STEP, 0)
        val attendanceGuideTotalSteps = HomeActivity.TOTAL_ATTENDANCE_TUTORIAL_STEPS

        pbAttendanceGuide.max = attendanceGuideTotalSteps
        if (attendanceGuideIsCompleted) {
            tvAttendanceGuideStatus.text = "Completed ✓"
            pbAttendanceGuide.progress = attendanceGuideTotalSteps
            setStatusBadge(tvAttendanceGuideStatus, StatusType.COMPLETED)
        } else {
            if (attendanceGuideSavedStep > attendanceGuideTotalSteps) {
                attendanceGuideSavedStep = attendanceGuideTotalSteps
            }

            if (attendanceGuideSavedStep > 0 && attendanceGuideTotalSteps > 0) {
                val percentage = (attendanceGuideSavedStep * 100) / attendanceGuideTotalSteps
                pbAttendanceGuide.progress = attendanceGuideSavedStep
                tvAttendanceGuideStatus.text = "${percentage}%"
                setStatusBadge(tvAttendanceGuideStatus, StatusType.IN_PROGRESS)
            } else {
                tvAttendanceGuideStatus.text = "Not Started"
                pbAttendanceGuide.progress = 0
                setStatusBadge(tvAttendanceGuideStatus, StatusType.NOT_STARTED)
            }
        }

        // --- Event Tutorial Progress ---
        val eventTutorialIsCompleted = tutorialPrefs.getBoolean(HomeActivity.KEY_EVENT_TUTORIAL_COMPLETED, false)
        var eventTutorialSavedStep = tutorialPrefs.getInt(HomeActivity.KEY_EVENT_TUTORIAL_CURRENT_STEP, 0)
        val eventTutorialTotalSteps = HomeActivity.TOTAL_EVENT_TUTORIAL_STEPS

        pbEventTutorial.max = eventTutorialTotalSteps
        if (eventTutorialIsCompleted) {
            tvEventTutorialStatus.text = "Completed ✓"
            pbEventTutorial.progress = eventTutorialTotalSteps
            setStatusBadge(tvEventTutorialStatus, StatusType.COMPLETED)
        } else {
            if (eventTutorialSavedStep > eventTutorialTotalSteps) {
                eventTutorialSavedStep = eventTutorialTotalSteps
            }

            if (eventTutorialSavedStep > 0 && eventTutorialTotalSteps > 0) {
                val percentage = (eventTutorialSavedStep * 100) / eventTutorialTotalSteps
                pbEventTutorial.progress = eventTutorialSavedStep
                tvEventTutorialStatus.text = "${percentage}%"
                setStatusBadge(tvEventTutorialStatus, StatusType.IN_PROGRESS)
            } else {
                tvEventTutorialStatus.text = "Not Started"
                pbEventTutorial.progress = 0
                setStatusBadge(tvEventTutorialStatus, StatusType.NOT_STARTED)
            }
        }

        // Determine if any tutorial has ever been started or completed to hide the "No active tutorial" message
        val anyQuickTourProgress = quickTourIsCompleted || quickTourSavedStep > 0
        val anyAttendanceGuideProgress = attendanceGuideIsCompleted || attendanceGuideSavedStep > 0
        val anyEventTutorialProgress = eventTutorialIsCompleted || eventTutorialSavedStep > 0

        if (anyQuickTourProgress || anyAttendanceGuideProgress || anyEventTutorialProgress) {
            tvNoActiveTutorial.visibility = View.GONE
        } else {
            tvNoActiveTutorial.visibility = View.VISIBLE
        }
    }
    
    private enum class StatusType {
        COMPLETED, IN_PROGRESS, NOT_STARTED
    }
    
    private fun setStatusBadge(textView: TextView, status: StatusType) {
        val (backgroundRes, textColorRes) = when (status) {
            StatusType.COMPLETED -> Pair(R.drawable.bg_status_completed, R.color.status_green)
            StatusType.IN_PROGRESS -> Pair(R.drawable.bg_status_in_progress, R.color.status_orange)
            StatusType.NOT_STARTED -> Pair(R.drawable.bg_status_not_started, R.color.neutral_text_gray)
        }
        textView.setBackgroundResource(backgroundRes)
        textView.setTextColor(androidx.core.content.ContextCompat.getColor(this, textColorRes))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Go back to the previous activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}