package com.baust.cafe.models

data class CartItem(
    val itemId: String = "",
    val itemName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
) {
    fun getTotalPrice(): Double = price * quantity
}