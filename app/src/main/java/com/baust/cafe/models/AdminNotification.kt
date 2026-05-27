package com.baust.cafe.models

data class AdminNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "order", "review"
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)