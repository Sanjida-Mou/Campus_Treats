package com.baust.cafe.models

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val userType: String = "student",
    val phoneNumber: String = "",
    val location: String = "",
    val profileImage: String = "",
    val totalSpent: Double = 0.0
)
