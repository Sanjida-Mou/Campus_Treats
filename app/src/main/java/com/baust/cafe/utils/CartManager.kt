package com.baust.cafe.utils

import com.baust.cafe.models.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object CartManager {
    private val cartItems = mutableListOf<CartItem>()
    private var isDataLoaded = false

    private fun getDbRef() = FirebaseAuth.getInstance().currentUser?.uid?.let {
        FirebaseDatabase.getInstance().getReference("Carts").child(it)
    }

    private fun isValid(item: CartItem): Boolean {
        return item.itemId.isNotEmpty() && item.itemName.isNotEmpty() && item.price > 0
    }

    fun addItem(item: CartItem) {
        if (!isValid(item)) return

        val existingItem = cartItems.find { it.itemId == item.itemId }
        if (existingItem != null) {
            val updatedItem = existingItem.copy(quantity = existingItem.quantity + item.quantity)
            cartItems[cartItems.indexOf(existingItem)] = updatedItem
        } else {
            cartItems.add(item)
        }
        
        if (isDataLoaded) {
            syncWithFirebase()
        }
    }

    fun getItems(): MutableList<CartItem> {
        return cartItems
    }

    fun removeItem(item: CartItem) {
        cartItems.removeIf { it.itemId == item.itemId }
        syncWithFirebase()
    }

    fun clearCart() {
        cartItems.clear()
        getDbRef()?.removeValue()
    }

    fun clearLocalData() {
        cartItems.clear()
        isDataLoaded = false
    }

    fun getCartTotal(): Double {
        // Simple sum of current items to ensure UI matches Total
        return cartItems.sumOf { it.price * it.quantity }
    }

    fun syncWithFirebase() {
        // Only save valid, non-duplicate items back to Firebase
        val sanitized = cartItems.filter { isValid(it) }
        getDbRef()?.setValue(sanitized)
    }

    fun loadFromFirebase(onComplete: () -> Unit) {
        val ref = getDbRef()
        if (ref == null) {
            onComplete()
            return
        }

        ref.get().addOnSuccessListener { snapshot ->
            val tempItems = mutableListOf<CartItem>()
            if (snapshot.exists()) {
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItem::class.java)
                    if (item != null && isValid(item)) {
                        tempItems.add(item)
                    }
                }
            }
            
            // DEDUPLICATION: If Firebase had multiple entries for the same ID, merge them
            val mergedItems = tempItems.groupBy { it.itemId }.map { entry ->
                val group = entry.value
                val first = group[0]
                if (group.size > 1) {
                    first.copy(quantity = group.sumOf { it.quantity })
                } else {
                    first
                }
            }

            cartItems.clear()
            cartItems.addAll(mergedItems)
            isDataLoaded = true
            
            // Clean up the database if we found bad data
            if (tempItems.size != mergedItems.size) {
                syncWithFirebase()
            }

            onComplete()
        }.addOnFailureListener {
            onComplete()
        }
    }
}
