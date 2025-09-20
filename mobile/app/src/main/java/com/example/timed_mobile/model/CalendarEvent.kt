package com.example.timed_mobile.model

import java.util.Date

data class CalendarEvent(
    val title: String,
    val date: Date,
    val status: String,
    val timeLabel: String
)