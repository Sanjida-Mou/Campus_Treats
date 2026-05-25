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
        val cartItems = com.baust.cafe.utils.CartManager.getItems()
        
        adapter = CartAdapter(cartItems) {
            updateTotalPrice()
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