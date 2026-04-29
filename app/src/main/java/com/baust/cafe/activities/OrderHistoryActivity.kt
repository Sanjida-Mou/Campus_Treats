package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.OrderAdapter
import com.baust.cafe.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter
    private val ordersList = mutableListOf<Order>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        recyclerView = findViewById(R.id.orderHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = OrderAdapter(ordersList)
        recyclerView.adapter = adapter

        fetchOrderHistory()
    }

    private fun fetchOrderHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("Orders")

        // Query orders where studentId matches current user
        database.orderByChild("studentId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ordersList.clear()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        order?.let { ordersList.add(it) }
                    }
                    // Sort by time descending (newest first)
                    ordersList.sortByDescending { it.orderTime }
                    adapter.updateOrders(ordersList)
                    
                    if (ordersList.isEmpty()) {
                        Toast.makeText(this@OrderHistoryActivity, "No orders found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@OrderHistoryActivity, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}