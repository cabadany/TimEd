package com.example.timed_mobile.model

data class EventLogModel(
    val eventId: String,
    val attendeeDocId: String, // ADDED: This is crucial to identify which document to update for time-out.
    val eventName: String,
    val timeInTimestamp: String,
    val status: String,
    val showTimeOutButton: Boolean = false,
    val checkinMethod: Boolean = false, // false = QR (default), true = Manual
    val userId: String
)