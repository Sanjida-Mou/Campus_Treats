package com.baust.cafe.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baust.cafe.R
import com.baust.cafe.adapters.AdminOrderAdapter
import com.baust.cafe.models.Order
import com.baust.cafe.models.User
import com.baust.cafe.models.UserNotification
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import java.util.Locale

class ViewOrdersActivity : BaseAdminActivity() {

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
            onVerifyPayment = { order -> verifyPayment(order) },
            onRejectPayment = { order -> rejectPayment(order) },
            onUserClick = { userId -> showUserDetailsDialog(userId) }
        )
        recyclerView.adapter = adapter

        fetchOrders()
        setupBottomNavigation(0)
    }

    private fun verifyPayment(order: Order) {
        val updates = HashMap<String, Any>()
        updates["paymentStatus"] = "verified"
        updates["status"] = "pending"

        database.child(order.orderId).updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Payment Verified!", Toast.LENGTH_SHORT).show()
            
            // NOTIFY USER
            pushUserNotification(order.studentId, "Order Confirmed!", "Your payment has been verified and order is now confirmed.", "order_status")
        }
    }

    private fun rejectPayment(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Reject Payment")
            .setMessage("Are you sure this is a fake transaction? The order will be cancelled.")
            .setPositiveButton("Reject & Cancel") { _, _ ->
                val updates = HashMap<String, Any>()
                updates["paymentStatus"] = "rejected"
                updates["status"] = "cancelled"
                
                database.child(order.orderId).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Order Rejected due to fake payment", Toast.LENGTH_SHORT).show()
                    
                    // NOTIFY USER
                    pushUserNotification(order.studentId, "Order Rejected", "Your payment verification failed. Order has been cancelled.", "order_status")
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun pushUserNotification(userId: String, title: String, message: String, type: String) {
        val userNotifyDb = FirebaseDatabase.getInstance().getReference("UserNotifications").child(userId)
        val id = userNotifyDb.push().key ?: return
        val notification = UserNotification(id, title, message, type, System.currentTimeMillis(), false)
        userNotifyDb.child(id).setValue(notification)
    }

    private fun showUserDetailsDialog(userId: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                if (user != null) {
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null)
                    val dialog = AlertDialog.Builder(this).setView(dialogView).create()
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                    val userImage = dialogView.findViewById<ImageView>(R.id.dialogUserImage)
                    val userName = dialogView.findViewById<TextView>(R.id.dialogUserName)
                    val userDetails = dialogView.findViewById<TextView>(R.id.dialogUserDetails)
                    val okBtn = dialogView.findViewById<Button>(R.id.dialogOkButton)

                    userName.text = user.name
                    val totalSpentFormatted = String.format(Locale.getDefault(), "%.2f", user.totalSpent)
                    userDetails.text = "Email:  ${user.email}\n" +
                            "Phone:  ${user.phoneNumber}\n" +
                            "Student ID:  ${user.studentId}\n" +
                            "Location:  ${user.location}\n" +
                            "Total Spent:  Tk. $totalSpentFormatted"

                    val imageUrl = user.profileImage
                    if (imageUrl.isNotEmpty() && imageUrl != "null") {
                        if (imageUrl.startsWith("http")) {
                            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_person).into(userImage)
                        } else {
                            try {
                                val imageBytes = android.util.Base64.decode(imageUrl, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                userImage.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                userImage.setImageResource(R.drawable.ic_person)
                            }
                        }
                    }
                    okBtn.setOnClickListener { dialog.dismiss() }
                    dialog.show()
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
                database.child(order.orderId).child("status").setValue(newStatus).addOnSuccessListener {
                    // NOTIFY USER
                    when (newStatus) {
                        "delivered" -> pushUserNotification(order.studentId, "Order Delivered!", "Our order is delivered from BAUST Cafe. Enjoy!", "order_status")
                        "ready" -> pushUserNotification(order.studentId, "Order Ready!", "Your food is ready for pickup!", "order_status")
                        "preparing" -> pushUserNotification(order.studentId, "Order Preparing", "Your food is being prepared in the kitchen.", "order_status")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHandleCancellationDialog(order: Order) {
        // No longer used, but kept for callback compatibility
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
