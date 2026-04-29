package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.CartAdapter
import com.baust.cafe.models.Order
import com.baust.cafe.models.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class CartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter
    private lateinit var totalPriceText: TextView
    private lateinit var checkoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.cartRecyclerView)
        totalPriceText = findViewById(R.id.totalPriceText)
        checkoutButton = findViewById(R.id.checkoutButton)

        setupRecyclerView()

        checkoutButton.setOnClickListener {
            processCheckout()
        }
    }

    private fun setupRecyclerView() {
        // In a real app, you'd get this from a CartManager or ViewModel
        val dummyCartItems = mutableListOf<com.baust.cafe.models.CartItem>()
        
        adapter = CartAdapter(dummyCartItems) {
            updateTotalPrice()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val total = adapter.getItems().sumOf { it.price * it.quantity }
        totalPriceText.text = String.format(Locale.getDefault(), "$%.2f", total)
    }

    private fun processCheckout() {
        val cartItems = adapter.getItems()
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Student"

        val database = FirebaseDatabase.getInstance().getReference("Orders")
        val orderId = database.push().key ?: return

        val orderItems = cartItems.map {
            OrderItem(
                itemId = it.itemId,
                itemName = it.itemName,
                quantity = it.quantity,
                price = it.price
            )
        }

        val totalAmount = cartItems.sumOf { it.price * it.quantity }

        val order = Order(
            orderId = orderId,
            studentId = userId,
            studentName = userName,
            items = orderItems,
            totalAmount = totalAmount,
            status = "pending"
        )

        database.child(orderId).setValue(order)
            .addOnSuccessListener {
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show()
                // Clear cart logic would go here
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to place order: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}