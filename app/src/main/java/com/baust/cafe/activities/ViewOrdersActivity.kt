package com.baust.cafe.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.AdminOrderAdapter
import com.baust.cafe.models.Order
import com.baust.cafe.models.User
import com.google.firebase.database.*
import java.util.Locale

class ViewOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminOrderAdapter
    private val ordersList = mutableListOf<Order>()
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_orders)

        database = FirebaseDatabase.getInstance().getReference("Orders")

        recyclerView = findViewById(R.id.ordersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = AdminOrderAdapter(
            ordersList,
            onUpdateStatus = { order -> showUpdateStatusDialog(order) },
            onHandleCancellation = { order -> showHandleCancellationDialog(order) },
            onUserClick = { userId -> showUserDetailsDialog(userId) }
        )
        recyclerView.adapter = adapter

        fetchOrders()
    }

    private fun showUserDetailsDialog(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    AlertDialog.Builder(this)
                        .setTitle("User Details")
                        .setMessage("Name: ${user.name}\n" +
                                "Email: ${user.email}\n" +
                                "Phone: ${user.phoneNumber}\n" +
                                "Student ID: ${user.studentId}\n" +
                                "Location: ${user.location}\n" +
                                "Total Spent: Tk. ${String.format(Locale.getDefault(), "%.2f", user.totalSpent)}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
    }

    private fun showUpdateStatusDialog(order: Order) {
        val statuses = arrayOf("pending", "preparing", "ready", "delivered", "cancelled")
        val currentStatusIndex = statuses.indexOf(order.status.lowercase())
        
        AlertDialog.Builder(this)
            .setTitle("Update Order Status")
            .setSingleChoiceItems(statuses, currentStatusIndex) { dialog, which ->
                val newStatus = statuses[which]
                database.child(order.orderId).child("status").setValue(newStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHandleCancellationDialog(order: Order) {
        // This is no longer needed but kept to avoid breaking the adapter callback for now
    }

    private fun fetchOrders() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                if (snapshot.exists()) {
                    for (orderSnapshot in snapshot.children) {
                        try {
                            val order = orderSnapshot.getValue(Order::class.java)
                            order?.let { ordersList.add(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("ViewOrders", "Error parsing order", e)
                        }
                    }
                    ordersList.sortByDescending { it.orderTime }
                    adapter.updateOrders(ordersList)
                } else {
                    adapter.updateOrders(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewOrdersActivity, "Database Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
