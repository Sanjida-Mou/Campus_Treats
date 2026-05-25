package com.baust.cafe.models

data class Review(
    val reviewId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val rating: Float = 5.0f,
    val comment: String = "",
    val reply: String = "", // Added for Admin reply
    val timestamp: Long = System.currentTimeMillis()
)
