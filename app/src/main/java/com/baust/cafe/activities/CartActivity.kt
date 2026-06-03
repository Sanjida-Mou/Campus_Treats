package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.CartAdapter
import java.util.Locale

class CartActivity : BaseUserActivity() {

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

        // Load data from Firebase first
        com.baust.cafe.utils.CartManager.loadFromFirebase {
            setupRecyclerView()
        }

        checkoutButton.setOnClickListener {
            processCheckout()
        }

        setupBottomNavigation(R.id.navCart)
    }

    private fun setupRecyclerView() {
        val cartItems = com.baust.cafe.utils.CartManager.getItems()
        
        adapter = CartAdapter(cartItems, isEditable = true) {
            updateTotalPrice()
            com.baust.cafe.utils.CartManager.syncWithFirebase()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val total = com.baust.cafe.utils.CartManager.getCartTotal()
        totalPriceText.text = String.format(Locale.getDefault(), "Tk. %.2f", total)
    }

    private fun processCheckout() {
        val cartItems = ArrayList(adapter.getItems())
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(this, CheckoutActivity::class.java).apply {
            putExtra("CART_ITEMS", cartItems)
        }
        startActivity(intent)
    }
}
