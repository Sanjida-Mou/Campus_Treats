package com.baust.cafe.utils

import com.baust.cafe.models.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object CartManager {
    private val cartItems = mutableListOf<CartItem>()

    private fun getDbRef() = FirebaseAuth.getInstance().currentUser?.uid?.let {
        FirebaseDatabase.getInstance().getReference("Carts").child(it)
    }

    fun addItem(item: CartItem) {
        val existingItem = cartItems.find { it.itemId == item.itemId }
        if (existingItem != null) {
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + item.quantity)
            cartItems[cartItems.indexOf(existingItem)] = updatedItem
        } else {
            cartItems.add(item)
        }
        syncWithFirebase()
    }

    fun getItems(): MutableList<CartItem> = cartItems

    fun removeItem(item: CartItem) {
        cartItems.removeIf { it.itemId == item.itemId }
        syncWithFirebase()
    }

    fun clearCart() {
        cartItems.clear()
        getDbRef()?.removeValue()
    }

    fun getCartTotal(): Double {
        return cartItems.sumOf { it.price * it.quantity }
    }

    fun syncWithFirebase() {
        getDbRef()?.setValue(cartItems)
    }

    fun loadFromFirebase(onComplete: () -> Unit) {
        getDbRef()?.get()?.addOnSuccessListener { snapshot ->
            cartItems.clear()
            if (snapshot.exists()) {
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItem::class.java)
                    item?.let { cartItems.add(it) }
                }
            }
            onComplete()
        }?.addOnFailureListener {
            onComplete()
        }
    }
}
