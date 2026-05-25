package com.baust.cafe.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.baust.cafe.R

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize Firebase and start listening for orders
        database = FirebaseDatabase.getInstance().getReference("Orders")
        setupOrderListener()

        val manageMenuCard = findViewById<CardView>(R.id.manageMenuCard)
        // ...
        val viewOrdersCard = findViewById<CardView>(R.id.viewOrdersCard)
        val viewReviewsCard = findViewById<CardView>(R.id.viewReviewsCard)
        val adminLogoutCard = findViewById<CardView>(R.id.adminLogoutCard)

        manageMenuCard.setOnClickListener {
            startActivity(Intent(this, ManageMenuActivity::class.java))
        }

        viewOrdersCard.setOnClickListener {
            startActivity(Intent(this, ViewOrdersActivity::class.java))
        }

        viewReviewsCard.setOnClickListener {
            startActivity(Intent(this, ViewReviewsActivity::class.java))
        }

        adminLogoutCard.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
            finish()
        }
    }

    private fun setupOrderListener() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!isFirstLoad) {
                    val customerName = snapshot.child("studentName").value.toString()
                    val total = snapshot.child("totalAmount").value.toString()
                    showNotification("New Order Received!", "Customer: $customerName - Tk $total")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                if (!isFirstLoad) {
                    val status = snapshot.child("status").value.toString()
                    val customerName = snapshot.child("studentName").value.toString()
                    if (status == "cancelled") {
                        showNotification("Order Cancelled", "$customerName has cancelled their order")
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // Skip existing orders on first load
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isFirstLoad = false
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "admin_orders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Order Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ViewOrdersActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}