package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.OrderAdapter
import com.baust.cafe.models.Order
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderHistoryActivity : BaseUserActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter
    private val ordersList = mutableListOf<Order>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        database = FirebaseDatabase.getInstance().getReference("Orders")

        recyclerView = findViewById(R.id.orderHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = OrderAdapter(ordersList) { order ->
            handleOrderAction(order)
        }
        recyclerView.adapter = adapter

        fetchOrderHistory()
        setupNavigation()
        
        setupBottomNavigation(R.id.navCart) // History is mapped to Cart icon in some layouts? Wait, history icon exists.
    }

    private fun handleOrderAction(order: Order) {
        when (order.status.lowercase()) {
            "pending", "preparing", "pending_verification" -> {
                showCancelOrderDialog(order)
            }
            "ready" -> {
                Toast.makeText(this, "Food is ongoing/ready. Cannot cancel anymore.", Toast.LENGTH_LONG).show()
            }
            "delivered", "cancelled" -> {
                showDeleteHistoryDialog(order)
            }
        }
    }

    private fun showCancelOrderDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order? Admin will be notified.")
            .setPositiveButton("Cancel Order") { _, _ ->
                database.child(order.orderId).child("status").setValue("cancelled")
                    .addOnSuccessListener {
                        saveAdminNotification("Order Cancelled", "@${order.studentName} has cancelled their order", "cancellation")
                        Toast.makeText(this, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveAdminNotification(title: String, message: String, type: String) {
        val notifyDb = FirebaseDatabase.getInstance().getReference("AdminNotifications")
        val id = notifyDb.push().key ?: return
        val notification = com.baust.cafe.models.AdminNotification(
            id = id,
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        notifyDb.child(id).setValue(notification)
    }

    private fun showDeleteHistoryDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Delete History")
            .setMessage("Are you sure you want to delete this order from your history?")
            .setPositiveButton("Delete") { _, _ ->
                database.child(order.orderId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Order history deleted", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupNavigation() {
        findViewById<android.widget.ImageView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navBookmark).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
        }

        findViewById<android.widget.ImageView>(R.id.navNotifications).setOnClickListener {
            Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.ImageView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, UserDashboardActivity::class.java))
            finish()
        }
    }

    private fun fetchOrderHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.orderByChild("studentId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ordersList.clear()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        order?.let { ordersList.add(it) }
                    }
                    ordersList.sortByDescending { it.orderTime }
                    adapter.updateOrders(ordersList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@OrderHistoryActivity, "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
