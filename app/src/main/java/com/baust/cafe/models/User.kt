package com.baust.cafe.models

// "data class" means Kotlin automatically generates useful methods
data class User(
    val userId: String = "",      // Unique ID from Firebase
    val name: String = "",         // Student's full name
    val email: String = "",        // Email for login
    val studentId: String = "",    // BAUST student ID
    val userType: String = "student", // "student" or "owner" - who is this?
    val phoneNumber: String = ""   // Contact number
)