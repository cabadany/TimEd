package com.example.timed_mobile

import android.content.Context
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View

class TutorialProgressActivity : AppCompatActivity() {

    private lateinit var tvQuickTourStatus: TextView
    private lateinit var pbQuickTour: ProgressBar
    private lateinit var tvAttendanceGuideStatus: TextView
    private lateinit var pbAttendanceGuide: ProgressBar
    private lateinit var tvNoActiveTutorial: TextView

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

        loadTutorialProgress()
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
        }

        if (attendanceGuideCompleted) {
            tvAttendanceGuideStatus.text = "Completed"
            pbAttendanceGuide.progress = 100
            anyTutorialActiveOrCompleted = true
        } else {
            tvAttendanceGuideStatus.text = "Not Started"
            pbAttendanceGuide.progress = 0
        }

        if (!anyTutorialActiveOrCompleted) {
            // This logic might need refinement based on how you define "active"
            tvNoActiveTutorial.visibility = View.VISIBLE
        } else {
            tvNoActiveTutorial.visibility = View.GONE
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