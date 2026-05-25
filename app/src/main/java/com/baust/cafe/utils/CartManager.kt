package com.baust.cafe.utils

import com.baust.cafe.models.CartItem

object CartManager {
    private val cartItems = mutableListOf<CartItem>()

    fun addItem(item: CartItem) {
        val existingItem = cartItems.find { it.itemId == item.itemId }
        if (existingItem != null) {
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + item.quantity)
            cartItems[cartItems.indexOf(existingItem)] = updatedItem
        } else {
            cartItems.add(item)
        }
    }

    fun getItems(): MutableList<CartItem> = cartItems

    fun removeItem(item: CartItem) {
        cartItems.removeIf { it.itemId == item.itemId }
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun getCartTotal(): Double {
        return cartItems.sumOf { it.price * it.quantity }
    }
}
