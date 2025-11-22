package com.example.timed_mobile.tutorial

import android.content.Context
import android.content.SharedPreferences
import com.example.timed_mobile.HomeActivity
import com.example.timed_mobile.model.EventModel

object EventTutorialState {
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(HomeActivity.PREFS_TUTORIAL, Context.MODE_PRIVATE)

    private const val KEY_SELECTED_EVENT_TITLE = "eventTutorialSelectedTitle"
    private const val KEY_SELECTED_EVENT_DATE = "eventTutorialSelectedDate"
    private const val KEY_SELECTED_EVENT_STATUS = "eventTutorialSelectedStatus"
    private const val KEY_SELECTED_EVENT_VENUE = "eventTutorialSelectedVenue"

    fun setActive(context: Context, isActive: Boolean) {
        prefs(context).edit().putBoolean(HomeActivity.KEY_EVENT_TUTORIAL_ACTIVE, isActive).apply()
        if (!isActive) {
            setExpectedAction(context, null)
            clearSelectedEvent(context)
        }
    }

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean(HomeActivity.KEY_EVENT_TUTORIAL_ACTIVE, false)

    fun setExpectedAction(context: Context, action: String?) {
        prefs(context).edit().apply {
            if (action == null) remove(HomeActivity.KEY_EVENT_TUTORIAL_EXPECTED_ACTION)
            else putString(HomeActivity.KEY_EVENT_TUTORIAL_EXPECTED_ACTION, action)
        }.apply()
    }

    fun getExpectedAction(context: Context): String? =
        prefs(context).getString(HomeActivity.KEY_EVENT_TUTORIAL_EXPECTED_ACTION, null)

    fun completeStep(context: Context, step: Int, markCompleted: Boolean = false) {
        prefs(context).edit().apply {
            putInt(HomeActivity.KEY_EVENT_TUTORIAL_CURRENT_STEP, step)
            if (markCompleted) putBoolean(HomeActivity.KEY_EVENT_TUTORIAL_COMPLETED, true)
        }.apply()
    }

    fun getCurrentStep(context: Context): Int =
        prefs(context).getInt(HomeActivity.KEY_EVENT_TUTORIAL_CURRENT_STEP, 0)

    fun rememberSelectedEvent(context: Context, event: EventModel) {
        prefs(context).edit().apply {
            putString(KEY_SELECTED_EVENT_TITLE, event.title)
            putString(KEY_SELECTED_EVENT_DATE, event.dateFormatted)
            putString(KEY_SELECTED_EVENT_STATUS, event.status)
            putString(KEY_SELECTED_EVENT_VENUE, event.venue)
        }.apply()
    }

    fun readSelectedEvent(context: Context): EventModel? {
        val title = prefs(context).getString(KEY_SELECTED_EVENT_TITLE, null) ?: return null
        val date = prefs(context).getString(KEY_SELECTED_EVENT_DATE, "") ?: ""
        val status = prefs(context).getString(KEY_SELECTED_EVENT_STATUS, "") ?: ""
        val venue = prefs(context).getString(KEY_SELECTED_EVENT_VENUE, null)
        return EventModel(title = title, dateFormatted = date, status = status, venue = venue)
    }

    fun clearSelectedEvent(context: Context) {
        prefs(context).edit().apply {
            remove(KEY_SELECTED_EVENT_TITLE)
            remove(KEY_SELECTED_EVENT_DATE)
            remove(KEY_SELECTED_EVENT_STATUS)
            remove(KEY_SELECTED_EVENT_VENUE)
        }.apply()
    }
}
