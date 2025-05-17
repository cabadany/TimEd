package com.example.timed_mobile

data class ExcuseLetterModel(
    val date: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "Pending"
)