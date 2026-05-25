package com.baust.cafe.models

import java.io.Serializable

data class CartItem(
    val itemId: String = "",
    val itemName: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
) : Serializable {
    fun getTotalPrice(): Double = price * quantity
}