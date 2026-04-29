package com.baust.cafe.models

data class MenuItem(
    val itemId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "", // e.g., "Beverages", "Snacks", "Meals"
    val isAvailable: Boolean = true,
    val imageUrl: String = "",
    val preparationTime: Int = 15 // in minutes
)