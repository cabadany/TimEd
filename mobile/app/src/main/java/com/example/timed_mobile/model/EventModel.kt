package com.example.timed_mobile.model

import java.util.Date

data class EventModel(
    val title: String = "",
    val duration: String = "",
    val dateFormatted: String = "",
    val status: String = "",
    val rawDate: Date? = null, // <-- NEW FIELD for accurate comparison
    val venue: String? = "N/A"
)