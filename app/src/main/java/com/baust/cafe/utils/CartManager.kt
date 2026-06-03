package com.baust.cafe.utils

import android.util.Log
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
        
        // Force sync whenever we add something
        syncWithFirebase()
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
        return cartItems.sumOf { it.price * it.quantity }
    }

    fun syncWithFirebase() {
        // Prevent saving empty list if we are still waiting for Firebase to load
        // But if we have manually added items, we definitely want to save
        val ref = getDbRef() ?: return
        val sanitized = cartItems.filter { isValid(it) }
        ref.setValue(sanitized).addOnFailureListener {
            Log.e("CartManager", "Sync failed: ${it.message}")
        }
    }

    fun loadFromFirebase(onComplete: () -> Unit) {
        val ref = getDbRef()
        if (ref == null) {
            onComplete()
            return
        }

        ref.get().addOnSuccessListener { snapshot ->
            val cloudItems = mutableListOf<CartItem>()
            if (snapshot.exists()) {
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(CartItem::class.java)
                    if (item != null && isValid(item)) {
                        cloudItems.add(item)
                    }
                }
            }
            
            // Merge logic: Combine what's currently in memory with what's in the cloud
            // This prevents losing items added just before the sync finished
            val combinedMap = mutableMapOf<String, CartItem>()
            
            // Add items from cloud first
            cloudItems.forEach { combinedMap[it.itemId] = it }
            
            // Overwrite/Add items from local memory (most recent additions)
            cartItems.forEach { localItem ->
                val existing = combinedMap[localItem.itemId]
                if (existing != null) {
                    // If both have it, take the higher quantity or merge
                    combinedMap[localItem.itemId] = localItem.copy(quantity = localItem.quantity)
                } else {
                    combinedMap[localItem.itemId] = localItem
                }
            }

            cartItems.clear()
            cartItems.addAll(combinedMap.values)
            isDataLoaded = true
            
            // Clean up duplicates and bad data in the cloud
            syncWithFirebase()

            onComplete()
        }.addOnFailureListener {
            onComplete()
        }
    }
}
