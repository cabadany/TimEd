package com.example.timed_mobile.model

data class EventLogModel(
    val eventId: String,
    val eventName: String,
    val timeInTimestamp: String,
    val status: String,
    val showTimeOutButton: Boolean = false,
    val userId: String
)