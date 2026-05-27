package com.baust.cafe.models

data class UserNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // "stock", "order_status"
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)