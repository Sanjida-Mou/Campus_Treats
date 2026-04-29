package com.baust.cafe.models

data class Review(
    val reviewId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val rating: Float = 5.0f, // 1.0 to 5.0
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)